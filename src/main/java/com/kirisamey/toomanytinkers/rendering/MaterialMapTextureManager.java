package com.kirisamey.toomanytinkers.rendering;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.kirisamey.toomanytinkers.TooManyTinkers;
import com.kirisamey.toomanytinkers.rendering.events.MaterialMapTextureUpdatedEvent;
import com.kirisamey.toomanytinkers.utils.TmtColorUtils;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.FastColor;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import oshi.util.tuples.Pair;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfoLoader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MaterialMapTextureManager {

    // <editor-fold desc="Texture">

    public static final int TEX_UNIT = 256;
    public static final ResourceLocation MAT_TEX_ID = ResourceLocation.fromNamespaceAndPath(TooManyTinkers.MODID, "dynamic/mat_map");
    @Getter private static int texWidth = 1;
    @Getter private static int texHeigh = 1;
    private static DynamicTexture mapTex;

    // </editor-fold>


    // <editor-fold desc="Material Maps">

    private static final List<ResourceLocation> MAT1D_LST = new ArrayList<>();
    private static final Map<ResourceLocation, Integer> MAT1D_MAP = new HashMap<>();

    private static final List<ResourceLocation> MAT3D_LST = new ArrayList<>();
    private static final Map<ResourceLocation, Integer> MAT3D_MAP = new HashMap<>();

    private static int unitsFor1D = 1;

    private static void clearMaps() {
        MAT1D_LST.clear();
        MAT1D_MAP.clear();
    }

    private static int tryAddMat1DMap(ResourceLocation location) {
        var index = MAT1D_LST.size();
        if (MAT1D_MAP.putIfAbsent(location, index) != null) return -1;
        MAT1D_LST.add(location);
        return index;
    }

    public static @NotNull MatType getTexInfo(ResourceLocation location) {
        var id = MAT1D_MAP.getOrDefault(location, -1);
        if (id >= 0) return new MatType.Mat1D(id);

        id = MAT3D_MAP.getOrDefault(location, -1);
        if (id >= 0) return new MatType.Mat3D(id + unitsFor1D);

        return new MatType.MatNotFound();
    }

    // </editor-fold>


    // <editor-fold desc="Data Parsing">

    private static @NotNull MaterialInfos prepare(@NotNull ResourceManager resourceManager,
                                                  @NotNull ProfilerFiller profilerFiller) {
        clearMaps();

        // "first, we need to fetch all relevant JSON files"
        //                                          —— Slime Knights
        // So, let's do this
        Map<ResourceLocation, JsonElement> jsons = new HashMap<>();
        SimpleJsonResourceReloadListener.scanDirectory(resourceManager, MaterialRenderInfoLoader.FOLDER, JsonHelper.DEFAULT_GSON, jsons);

        List<Mat1DInfo> mat1DInfos = new ArrayList<>();
        List<Mat3DInfo> mat3DInfos = new ArrayList<>();
        List<MatInheritedInfo> matInheritedInfos = new ArrayList<>();

        for (var entry : jsons.entrySet()) {
            var location = entry.getKey();

            try {
                var json = GsonHelper.convertToJsonObject(entry.getValue(), location.toString());

                // none or inherited
                if (!json.has("generator")) {
                    if (json.has("parent")) {
                        var parent = ResourceLocation.tryParse(json.get("parent").getAsString());
                        matInheritedInfos.add(new MatInheritedInfo(location, parent));
                        LogUtils.getLogger().info("Read Inherited material info {} succeed!", location);
                        continue;
                    }
                    LogUtils.getLogger().warn("Material {} has neither 'generator' nor 'parent'", location);
                    continue;
                }

                var generator = json.get("generator");
                if (!generator.isJsonObject()) {
                    LogUtils.getLogger().error("'generator' field of material {} is not an object", location);
                    continue;
                }
                var transformerJs = generator.getAsJsonObject().get("transformer");
                if (transformerJs == null) {
                    LogUtils.getLogger().error("Material {} has a 'generator' but has no 'generator.transformer'", location);
                    continue;
                }
                if (!transformerJs.isJsonObject()) {
                    LogUtils.getLogger().error("'generator.transformer' field of material {} is not an object", location);
                    continue;
                }
                var transformer = transformerJs.getAsJsonObject();
                var type = transformer.get("type").getAsString();
                switch (type) {
                    case "tconstruct:recolor_sprite":
                        var info1 = getMat1DInfo(transformer, location);
                        if (info1 != null) mat1DInfos.add(info1);
                        break;
                    case "tconstruct:grey_to_sprite":
                        var info3n = getDefaultMat3DInfo(transformer, location);
                        if (info3n != null) mat3DInfos.add(info3n);
                        break;
                    case "tconstruct:animated_sprite":
                        var info3a = getAnimMat3DInfo(transformer, location);
                        if (info3a != null) mat3DInfos.add(info3a);
                        break;
                    default:
                        LogUtils.getLogger().error("Material {} has a unknown generator transformer type: {}", location, type);
                        break;
                }

            } catch (IllegalStateException e) {
                LogUtils.getLogger().error("Failed to get material generator info for {}", location, e);
            } catch (JsonSyntaxException e) {
                LogUtils.getLogger().error("Failed to read material generator info for {}", location, e);
            }
        }

        // calculate size of tex
        var count1D = mat1DInfos.size();
        unitsFor1D = (int) Math.ceil(((double) count1D) / TEX_UNIT);
        var unitsFor3D = mat3DInfos.stream().mapToInt(Mat3DInfo::frames).sum();
        var totalUnits = unitsFor1D + unitsFor3D;
        {
            var sqrtCeil = Math.ceil(Math.sqrt(totalUnits));
            var size = 1;
            while (size < sqrtCeil) {
                size *= 2;
            }
            texHeigh = size;
            texWidth = size * size / 2 >= totalUnits ? size / 2 : size;
            LogUtils.getLogger().info("MatMap texture size set to {}, {} ({}, {})",
                    texWidth, texHeigh, texWidth * TEX_UNIT, texHeigh * TEX_UNIT);
        }

        return new MaterialInfos(mat1DInfos, mat3DInfos, matInheritedInfos);
    }

    private static @Nullable Mat1DInfo getMat1DInfo(JsonObject transformer, ResourceLocation location) {
        try {
            var colorMapping = transformer.getAsJsonObject("color_mapping");
            var type = colorMapping.get("type").getAsString();
            if (!type.equals("tconstruct:grey_to_color")) {
                LogUtils.getLogger().error("Unrecognized color_mapping type: {} in Material {}", type, location);
                return null;
            }

            var palette = colorMapping.getAsJsonArray("palette");
            var colorMaps = palette.asList().stream().map(p -> {
                var cm = p.getAsJsonObject();
                var c = cm.get("color").getAsString();
                var grey = cm.get("grey").getAsInt();
                if (c.length() == 6) c = "ff" + c; // someone write material json with RGB instead of ARGB, unite them
                var color = Integer.parseUnsignedInt(c, 16);
                return new Mat1DInfo.ColorMap(color, grey);
            }).collect(Collectors.toCollection(ArrayList::new));

            // try to add to map
            var index = tryAddMat1DMap(location);
            var mat = new Mat1DInfo(location, index, colorMaps);

            if (index < 0) {
                LogUtils.getLogger().error("1D Material {} did not added to map, there is duplicated already exists? But why?", mat.location());
                return null;
            }

            // validate
            if (mat.colorMaps().isEmpty()) {
                LogUtils.getLogger().warn("1D Material {} has an empty color map!", mat.location());
                mat.colorMaps.add(new Mat1DInfo.ColorMap(0xffffffff, 0));
            }

            // append last map if it needs
            var lastMap = mat.colorMaps.get(mat.colorMaps.size() - 1);
            if (lastMap.grey < 255) {
                mat.colorMaps.add(new Mat1DInfo.ColorMap(lastMap.color, 255));
            }

            LogUtils.getLogger().info("Read 1D material info {} succeed!", location);
            return mat;
        } catch (IllegalStateException | NullPointerException | UnsupportedOperationException |
                 ClassCastException | NumberFormatException e) {
            LogUtils.getLogger().error("Failed to read 1D material generator transformer info for {}", location, e);
            return null;
        }
    }

    @SuppressWarnings("LoggingSimilarMessage")
    private static @Nullable Mat3DInfo getDefaultMat3DInfo(JsonObject transformer, ResourceLocation location) {
        LogUtils.getLogger().error("3D Info has not implemented yet, Material {} will not be registered", location);
        return null;
    }

    private static @Nullable Mat3DInfo getAnimMat3DInfo(JsonObject transformer, ResourceLocation location) {
        LogUtils.getLogger().error("3D Info has not implemented yet, Material {} will not be registered", location);
        return null;
    }

    // </editor-fold>


    // <editor-fold desc="Update Texture">

    // todo: 在这之前的任意时机，我们要开一个lifetime事件，让其他人能向我们这里注册材质。
    //       或者不用事件，我们提供一个接口让用户实现然后由我们在prepare之后调度也可。
    private static void apply(@NotNull MaterialInfos materialInfos,
                              @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {

        // initialize buffer
        var buffer = new NativeImage(texWidth * TEX_UNIT, texHeigh * TEX_UNIT, true);
        LogUtils.getLogger().info("MatMap texture initialized with size {}, {} ({}, {})",
                texWidth, texHeigh, texWidth * TEX_UNIT, texHeigh * TEX_UNIT);

        for /* those for 1D */ (var mat1 : materialInfos.mat1DInfos()) {
            // calculate position
            var index = mat1.index();
            var row = index % (texHeigh * TEX_UNIT);
            var col = index / (texHeigh * TEX_UNIT);

            var first = mat1.colorMaps().get(0);
            Mat1DInfo.ColorMap lastColor = first.grey == 0 ? null : new Mat1DInfo.ColorMap(first.color, 0);
            for (var colorMap : mat1.colorMaps()) {
                final var last = lastColor;
                if (lastColor != null) {
                    var d = colorMap.grey - lastColor.grey;
                    if (d <= 0) continue;
                    IntStream.rangeClosed(lastColor.grey, colorMap.grey).forEach(grey -> {
                        if (grey < 0 || grey > 255) return;
                        //noinspection UnnecessaryLocalVariable
                        var y = row;
                        var x = col * TEX_UNIT + grey;
                        var lerp = ((float) (grey - last.grey)) / d;
                        var color = FastColor.ARGB32.lerp(lerp, last.color, colorMap.color);
                        buffer.setPixelRGBA(x, y, TmtColorUtils.Argb2Abgr(color));
                    });
                }
                lastColor = colorMap;
            }

            LogUtils.getLogger().info("1D Material {} mapped successfully as 1D no.{}", mat1.location(), index);
        }

        if (mapTex != null) mapTex.close();
        mapTex = new DynamicTexture(buffer);
        Minecraft.getInstance().getTextureManager().release(MAT_TEX_ID);
        Minecraft.getInstance().getTextureManager().register(MAT_TEX_ID, mapTex);
        mapTex.upload();
        // mojang 扫码了，不换tex只换掉image的话根本没法upload到gpu，我觉得我不得不创建锌纹理然后重新绑定了，妈的不早说

        {
            // for test
            var runDir = Minecraft.getInstance().gameDirectory;
            var testDir = new File(runDir, "debug");
            if (testDir.exists() || testDir.mkdirs()) {
                var outFile = new File(testDir, "tmt_map.png");
                try {
                    if (mapTex.getPixels() != null) {
                        mapTex.getPixels().writeToFile(outFile);
                    }
                } catch (IOException e) {
                    LogUtils.getLogger().error("test image save error", e);
                }
            }
        }

        // fire the event
        LogUtils.getLogger().debug("Fire the MaterialMapTextureUpdatedEvent");
        MinecraftForge.EVENT_BUS.post(new MaterialMapTextureUpdatedEvent());
    }

    // </editor-fold>


    // <editor-fold desc="Data Class">

    public record Mat1DInfo(ResourceLocation location, int index, List<ColorMap> colorMaps) {
        public record ColorMap(int color, int grey) {

        }
    }

    public record Mat3DInfo(ResourceLocation location, int index, int frames) {
    }

    public record MatInheritedInfo(ResourceLocation mat, ResourceLocation parent) {

    }

    public record MaterialInfos(List<Mat1DInfo> mat1DInfos, List<Mat3DInfo> mat3DInfos,
                                List<MatInheritedInfo> matInheritedInfos) {
    }

    // </editor-fold>


    // <editor-fold desc="Listening">

    public static class ReloadListener extends SimplePreparableReloadListener<MaterialInfos> {

        @Override
        protected @NotNull MaterialInfos prepare(@NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
            return MaterialMapTextureManager.prepare(resourceManager, profilerFiller);
        }

        @Override
        protected void apply(@NotNull MaterialInfos materialInfos, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
            MaterialMapTextureManager.apply(materialInfos, resourceManager, profilerFiller);
        }

        @Override public @NotNull String getName() {
            return "TMT Material Palette Update Listener";
        }
    }

    // </editor-fold>


    // <editor-fold desc="Event Handlers">

    @Mod.EventBusSubscriber(modid = TooManyTinkers.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEventSubscriber {
        @SubscribeEvent
        public static void onRegisterReloadListeners(@NotNull RegisterClientReloadListenersEvent event) {
            event.registerReloadListener(new MaterialMapTextureManager.ReloadListener());
        }
    }

    @Mod.EventBusSubscriber(modid = TooManyTinkers.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class MaterialMapTextureInitializedResender {
        @SubscribeEvent()
        public static void onClientTick(TickEvent.ClientTickEvent e) {
            MinecraftForge.EVENT_BUS.unregister(MaterialMapTextureInitializedResender.class);
            // 补发第一次事件
            MinecraftForge.EVENT_BUS.post(new MaterialMapTextureUpdatedEvent());
        }
    }

    // </editor-fold>


    // <editor-fold desc="MatType">

//    public enum MatType {
//        NotFound, Mat1D, Mat3D,
//    }

    public static abstract class MatType {
        public static class MatNotFound extends MatType {
        }

        @AllArgsConstructor
        public static class Mat1D extends MatType {
            @Getter private int id;
        }

        @AllArgsConstructor
        public static class Mat3D extends MatType {
            @Getter private int id;
        }
    }

    // </editor-fold>
}
