package com.kirisamey.toomanytinkers.rendering.materialmap;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.logging.LogUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfoLoader;

import java.util.*;
import java.util.stream.Collectors;

import static com.kirisamey.toomanytinkers.rendering.materialmap.MaterialMapUpdater.*;

public class MaterialMapsManager {

    // <editor-fold desc="Material Maps">
    private static int mat1dNext = 0;
    private static final Map<ResourceLocation, Integer> MAT1D_MAP = new HashMap<>();

    private static int mat3dNext = 0;
    private static final Map<ResourceLocation, Integer> MAT3D_MAP = new HashMap<>();

    private static final Map<ResourceLocation, ResourceLocation> MAT_INHERITED_MAP = new HashMap<>();

    private static final Map<ResourceLocation, Integer> MAT_EMISSIVE_MAP = new HashMap<>();

    @Getter private static int unitsFor1D = 1;

    private static void clearMaps() {
        mat1dNext = 0;
        MAT1D_MAP.clear();

        mat3dNext = 0;
        MAT3D_MAP.clear();

        MAT_EMISSIVE_MAP.clear();
    }

    private static int tryAddMat1DMap(ResourceLocation location) {
        var index = mat1dNext;
        if (MAT1D_MAP.putIfAbsent(location, index) != null) return -1;
        mat1dNext++;
        return index;
    }

    private static int tryAddMat3DMap(ResourceLocation location) {
        var index = mat3dNext;
        if (MAT3D_MAP.putIfAbsent(location, index) != null) return -1;
        mat3dNext++;
        return index;
    }

    public static @NotNull MatType getTexInfo(ResourceLocation location) {
        var locin = MAT_INHERITED_MAP.getOrDefault(location, null);
        if (locin != null) location = locin;

        var emissivity = MAT_EMISSIVE_MAP.getOrDefault(location, 0);

        var id = MAT1D_MAP.getOrDefault(location, -1);
        if (id >= 0) return new MatType.Mat1D(id, emissivity);

        id = MAT3D_MAP.getOrDefault(location, -1);
        if (id >= 0) return new MatType.Mat3D(id + unitsFor1D, emissivity);

        // todo: 4D get

        return new MatType.MatNotFound();
    }

    public static int getTintIfIs4D(ResourceLocation location) {
        // todo: 4D get
        return -1;
    }

    // </editor-fold>


    // <editor-fold desc="Data Parsing">

    static @NotNull MaterialInfos remapData(
            @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {

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
                        MAT_INHERITED_MAP.putIfAbsent(location, parent);
                        matInheritedInfos.add(new MatInheritedInfo(location, parent));
                        LogUtils.getLogger().info("Read Inherited material info {} succeed!", location);
                        continue;
                    }
                    LogUtils.getLogger().warn("Material {} has neither 'generator' nor 'parent'", location);
                    continue;
                }

                JsonElement emissivityJson = null;
                int emissivity = 0;
                if (json.has("emissivity")) emissivityJson = json.get("emissivity"); // 未雨绸缪
                if (json.has("luminosity")) emissivityJson = json.get("luminosity");
                if (emissivityJson != null) emissivity = emissivityJson.getAsInt();

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
                    case "tconstruct:frames":
                        LogUtils.getLogger().error("Transformer 'tconstruct:frames' has not supported yet, material {} skipped", location);
                        break;
                    default:
                        LogUtils.getLogger().error("Material {} has a unknown generator transformer type: {}", location, type);
                        break;
                }
                MAT_EMISSIVE_MAP.put(location, emissivity);

            } catch (IllegalStateException e) {
                LogUtils.getLogger().error("Failed to get material generator info for {}", location, e);
            } catch (JsonSyntaxException e) {
                LogUtils.getLogger().error("Failed to read material generator info for {}", location, e);
            }
        }

        // todo: 在这里，我们要开一个lifetime事件，让其他人能向我们这里注册材质。
        //       或者不用事件，我们提供一个接口让用户实现然后由我们在此调度也可。

        // calculate size of tex
        var count1D = mat1DInfos.size();
        unitsFor1D = (int) Math.ceil(((double) count1D) / MaterialMapTextureManager.TEX_UNIT);
        var unitsFor3D = mat3DInfos.size();
        var totalUnits = unitsFor1D + unitsFor3D;
        {
            var sqrtCeil = Math.ceil(Math.sqrt(totalUnits));
            var size = 1;
            while (size < sqrtCeil) {
                size *= 2;
            }
            var texHeigh = size;
            var texWidth = size * size / 2 >= totalUnits ? size / 2 : size;
            MaterialMapTextureManager.setTexHeigh(texHeigh);
            MaterialMapTextureManager.setTexWidth(texWidth);
            LogUtils.getLogger().info("MatMap texture size set to {}, {} ({}, {})",
                    texWidth, texHeigh, texWidth * MaterialMapTextureManager.TEX_UNIT, texHeigh * MaterialMapTextureManager.TEX_UNIT);
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
                if (c.length() == 6) c = "ff" + c; // someone write material.json with RGB instead of ARGB, unite them
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
                mat.colorMaps().add(new Mat1DInfo.ColorMap(0xffffffff, 0));
            }

            // append last map if it needs
            var lastMap = mat.colorMaps().get(mat.colorMaps().size() - 1);
            if (lastMap.grey() < 255) {
                mat.colorMaps().add(new Mat1DInfo.ColorMap(lastMap.color(), 255));
            }

            LogUtils.getLogger().info("Read 1D material info {} succeed!", location);
            return mat;
        } catch (IllegalStateException | NullPointerException | UnsupportedOperationException |
                 ClassCastException | NumberFormatException e) {
            LogUtils.getLogger().error("Failed to read 1D material generator transformer info for {}", location, e);
            return null;
        }
    }

    private static @Nullable Mat3DInfo getDefaultMat3DInfo(JsonObject transformer, ResourceLocation location) {
        try {
            var palette = transformer.getAsJsonArray("palette");
            var spriteMaps = palette.asList().stream().map(p -> {
                var cm = p.getAsJsonObject();
                var c = Optional.ofNullable(cm.get("color")).map(JsonElement::getAsString).orElse("ffffffff");
                var grey = cm.get("grey").getAsInt();
                var path = cm.get("path");
                if (c.length() == 6) c = "ff" + c; // someone write material.json with RGB instead of ARGB, unite them
                var color = Integer.parseUnsignedInt(c, 16);
                Optional<ResourceLocation> tex = path == null ? Optional.empty() :
                        Optional.ofNullable(ResourceLocation.tryParse(path.getAsString()));
                tex = tex.map(l -> l.withPath("textures/" + l.getPath() + ".png"));
                return new Mat3DInfo.SpriteMap(color, tex, grey);
            }).collect(Collectors.toCollection(ArrayList::new));

            // try to add to map
            var index = tryAddMat3DMap(location);
            var mat = new Mat3DInfo(location, index, spriteMaps, 0);

            if (index < 0) {
                LogUtils.getLogger().error("3D Material {} did not added to map, there is duplicated already exists? But why?", mat.location());
                return null;
            }

            // validate
            if (mat.spriteMaps().isEmpty()) {
                LogUtils.getLogger().warn("3D Material {} has an empty color map!", mat.location());
                mat.spriteMaps().add(new Mat3DInfo.SpriteMap(0xffffffff, Optional.empty(), 0));
            }

            // append last map if it needs
            var lastMap = mat.spriteMaps().get(mat.spriteMaps().size() - 1);
            if (lastMap.grey() < 255) {
                mat.spriteMaps().add(new Mat3DInfo.SpriteMap(lastMap.color(), lastMap.texture(), 255));
            }

            LogUtils.getLogger().info("Read 3D material info {} succeed!", location);
            return new Mat3DInfo(location, index, spriteMaps, 0);
        } catch (IllegalStateException | NullPointerException | UnsupportedOperationException |
                 ClassCastException | NumberFormatException e) {
            LogUtils.getLogger().error("Failed to read 3D material generator transformer info for {}", location, e);
            return null;
        }
    }

    private static @Nullable Mat3DInfo getAnimMat3DInfo(JsonObject transformer, ResourceLocation location) {
        LogUtils.getLogger().error("4D Info has not implemented yet, Material {} will not be registered", location);
        return null;
    }

    // </editor-fold>


    // <editor-fold desc="MatType">

    public static abstract class MatType {
        public static class MatNotFound extends MatType {
        }

        @AllArgsConstructor
        public static class Mat1D extends MatType {
            @Getter private int id;
            @Getter private int emissivity;
        }

        @AllArgsConstructor
        public static class Mat3D extends MatType {
            @Getter private int id;
            @Getter private int emissivity;
        }

        public static class Mat4D extends MatType {
            @Getter private int tint;
            @Getter private int emissivity;
        }
    }

    // </editor-fold>
}
