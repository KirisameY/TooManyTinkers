package com.kirisamey.toomanytinkers.rendering;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.kirisamey.toomanytinkers.TooManyTinkers;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfoLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MaterialPaletteManager extends SimplePreparableReloadListener<MaterialPaletteManager.MaterialInfos> {

    // Singleton
    private static MaterialPaletteManager instance;

    public static MaterialPaletteManager getInstance() {
        LogUtils.getLogger().info("MaterialPaletteManager startup...");
        if (instance == null) instance = new MaterialPaletteManager();
        return instance;
    }


    private MaterialPaletteManager() {
        Minecraft.getInstance().getTextureManager().register(MAT_TEX_ID, MAT_TEX);
    }


    // <editor-fold desc="Texture">

    public static final int TEX_WIDTH = 8192;
    public static final int TEX_HEIGH = 8192;
    public static final ResourceLocation MAT_TEX_ID = ResourceLocation.fromNamespaceAndPath(TooManyTinkers.MODID, "dynamic/mat_map");
    private static final DynamicTexture MAT_TEX = new DynamicTexture(TEX_WIDTH, TEX_HEIGH, true);

    // </editor-fold>


    // <editor-fold desc="Listening">

    @Override
    protected @NotNull MaterialInfos prepare(@NotNull ResourceManager resourceManager,
                                             @NotNull ProfilerFiller profilerFiller) {
        // "first, we need to fetch all relevant JSON files"
        //                                          —— Slime Knights
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
                var color = Integer.parseUnsignedInt(c, 16);
                return new Mat1DInfo.ColorMap(color, grey);
            }).toList();

            LogUtils.getLogger().info("Read 1D material info {} succeed!", location);
            return new Mat1DInfo(colorMaps);

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

    @Override
    protected void apply(@NotNull MaterialInfos materialInfos,
                         @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {

    }

    // </editor-fold>


    // <editor-fold desc="Event">

    @Mod.EventBusSubscriber(modid = TooManyTinkers.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class EventSubscriber {
        @SubscribeEvent
        public static void onRegisterReloadListeners(@NotNull RegisterClientReloadListenersEvent event) {
            event.registerReloadListener(MaterialPaletteManager.getInstance());
        }
    }

    // </editor-fold>


    // <editor-fold desc="Data Class">

    public record Mat1DInfo(List<ColorMap> colorMaps) {
        public record ColorMap(int color, int grey) {

        }
    }

    public record Mat3DInfo() {
    }

    public record MatInheritedInfo(ResourceLocation mat, ResourceLocation parent) {

    }

    public record MaterialInfos(List<Mat1DInfo> mat1DInfos, List<Mat3DInfo> mat3DInfos,
                                List<MatInheritedInfo> matInheritedInfos) {
    }

    // </editor-fold>
}
