package com.kirisamey.toomanytinkers.rendering.materialmap;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfoLoader;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.kirisamey.toomanytinkers.rendering.materialmap.MaterialMapUpdater.*;

public class MaterialMapsManager {

    // <editor-fold desc="Material Maps">
    private static int mat1dNext = 0;
    private static final Map<ResourceLocation, Integer> MAT1D_MAP = new HashMap<>();

    private static int mat3dNext = 0;
    private static final Map<ResourceLocation, Integer> MAT3D_MAP = new HashMap<>();

    private static final Map<ResourceLocation, ResourceLocation> MAT_INHERITED_MAP = new HashMap<>();

    private static final Map<ResourceLocation, Integer> MAT_EMISSIVE_MAP = new HashMap<>();

    private static final Map<ResourceLocation, List<String>> MAT_FALLBACKS_MAP = new HashMap<>();

    private static final List<Boolean> MAT3D_IS32_LST = new ArrayList<>();

    @Getter private static int unitsFor1D = 1;

    private static void clearMaps() {
        mat1dNext = 0;
        MAT1D_MAP.clear();

        mat3dNext = 0;
        MAT3D_MAP.clear();

        MAT_INHERITED_MAP.clear();
        MAT_EMISSIVE_MAP.clear();
        MAT_FALLBACKS_MAP.clear();

        MAT3D_IS32_LST.clear();

        MaterialAnimMapManager.clear();
    }

    private static int tryAddMat1DMap(ResourceLocation location) {
        var index = mat1dNext;
        if (MAT1D_MAP.putIfAbsent(location, index) != null) return -1;
        mat1dNext++;
        return index;
    }

    private static int tryAddMat3DMap(ResourceLocation location, boolean is32x) {
        var index = mat3dNext;
        if (MAT3D_MAP.putIfAbsent(location, index) != null) return -1;
        mat3dNext++;
        if (is32x) {
            while (MAT3D_IS32_LST.size() <= index) {
                MAT3D_IS32_LST.add(false);
            }
            MAT3D_IS32_LST.set(index, true);
        }
        return index;
    }

    public static @NotNull MatType getTexInfo(ResourceLocation location) {
        ResourceLocation locin = location;
        while (locin != null) {
            location = locin;
            locin = MAT_INHERITED_MAP.getOrDefault(location, null);
        }

        var emissivity = MAT_EMISSIVE_MAP.getOrDefault(location, 0);
        var fallbacks = MAT_FALLBACKS_MAP.getOrDefault(location, List.of());

        var id = MAT1D_MAP.getOrDefault(location, -1);
        if (id >= 0) return new MatType.Mat1D(id, emissivity, fallbacks);

        id = MAT3D_MAP.getOrDefault(location, -1);
        if (id >= 0) {
            var is32x = MAT3D_IS32_LST.size() > id ? MAT3D_IS32_LST.get(id) : false;
            return new MatType.Mat3D(id + unitsFor1D, is32x, emissivity, fallbacks);
        }

        id = MaterialAnimMapManager.tryGetAnimIndex(location);
        if (id >= 0) return new MatType.Mat4D(id, emissivity, fallbacks);

        return new MatType.MatNotFound();
    }

    public static int tryGetAnimId(ResourceLocation location) {
        return MaterialAnimMapManager.tryGetAnimIndex(location);
    }

    // </editor-fold>


    // <editor-fold desc="Data Parsing">

    static @NotNull List<MatInfo> remapData(
            @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {

        clearMaps();

        // "first, we need to fetch all relevant JSON files"
        //                                          —— Slime Knights
        // So, let's do this
        Map<ResourceLocation, JsonElement> jsons = new HashMap<>();
        SimpleJsonResourceReloadListener.scanDirectory(resourceManager, MaterialRenderInfoLoader.FOLDER, JsonHelper.DEFAULT_GSON, jsons);

        List<MatInfo> matInfos = new ArrayList<>();

        for (var entry : jsons.entrySet()) {
            var location = entry.getKey();
            var mats = getMatInfo(entry.getValue(), location);
            matInfos.addAll(mats);
        }

        // todo: 在这里，我们要开一个lifetime事件，让其他人能向我们这里注册材质。
        //       或者不用事件，我们提供一个接口让用户实现然后由我们在此调度也可。

        return matInfos.stream().toList();
    }

    private static List<MatInfo> getMatInfo(JsonElement entry, ResourceLocation location) {
        try {
            var json = GsonHelper.convertToJsonObject(entry, location.toString());

            // none or inherited
            if (!json.has("generator")) {
                if (json.has("parent")) {
                    var parent = ResourceLocation.tryParse(json.get("parent").getAsString());
                    MAT_INHERITED_MAP.putIfAbsent(location, parent);
                    LogUtils.getLogger().info("Read Inherited material info {} succeed!", location);
                    return List.of();
                }
                LogUtils.getLogger().warn("Material {} has neither 'generator' nor 'parent'", location);
                return List.of();
            }

            if (json.has("fallbacks")) {
                var fallbacksJ = json.get("fallbacks");
                if (fallbacksJ.isJsonArray()) {
                    var fallbacks = fallbacksJ.getAsJsonArray()
                            .asList().stream()
                            .map(JsonElement::getAsString)
                            .toList();
                    MAT_FALLBACKS_MAP.put(location, fallbacks);
                }
            }

            JsonElement emissivityJson = null;
            int emissivity = 0;
            if (json.has("emissivity")) emissivityJson = json.get("emissivity"); // 未雨绸缪
            if (json.has("luminosity")) emissivityJson = json.get("luminosity");
            if (emissivityJson != null) emissivity = emissivityJson.getAsInt();

            var generator = json.get("generator");
            if (!generator.isJsonObject()) {
                LogUtils.getLogger().error("'generator' field of material {} is not an object", location);
                return List.of();
            }
            var transformerJs = generator.getAsJsonObject().get("transformer");
            if (transformerJs == null) {
                LogUtils.getLogger().error("Material {} has a 'generator' but has no 'generator.transformer'", location);
                return List.of();
            }
            if (!transformerJs.isJsonObject()) {
                LogUtils.getLogger().error("'generator.transformer' field of material {} is not an object", location);
                return List.of();
            }
            var transformer = transformerJs.getAsJsonObject();
            return getMatInfoFromTransformer(transformer, location, emissivity, -1);

        } catch (IllegalStateException e) {
            LogUtils.getLogger().error("Failed to get material generator info for {}", location, e);
        } catch (JsonSyntaxException e) {
            LogUtils.getLogger().error("Failed to read material generator info for {}", location, e);
        }

        return List.of();
    }

    private static @NotNull List<MatInfo> getMatInfoFromTransformer(JsonObject transformer, ResourceLocation location, int emissivity, int frame) {
        var type = transformer.get("type").getAsString();
        List<MatInfo> result = List.of();
        switch (type) {
            case "tconstruct:recolor_sprite":
                result = getRecolorSpriteMatInfo(transformer, location);
                break;
            case "tconstruct:grey_to_sprite":
                result = getGreyToSpriteMatInfo(transformer, location, frame);
                break;
            case "tconstruct:animated_sprite":
                result = getAnimatedSpriteMatInfo(transformer, location);
                break;
            case "tconstruct:frames":
                result = getFramesMatInfo(transformer, location);
                break;
            default:
                LogUtils.getLogger().error("Material {} has a unknown generator transformer type: {}", location, type);
                break;
        }
        MAT_EMISSIVE_MAP.put(location, emissivity);
        return result;
    }

    private static @NotNull List<MatInfo> getRecolorSpriteMatInfo(JsonObject transformer, ResourceLocation location) {
        try {
            var colorMapping = transformer.getAsJsonObject("color_mapping");
            var type = colorMapping.get("type").getAsString();
            if (!type.equals("tconstruct:grey_to_color")) {
                LogUtils.getLogger().error("Unrecognized color_mapping type: {} in Material {}", type, location);
                return List.of();
            }

            var palette = colorMapping.getAsJsonArray("palette");
            var colorMaps = palette.asList().stream().map(p -> {
                var cm = p.getAsJsonObject();
                var c = cm.get("color").getAsString();
                var grey = cm.get("grey").getAsInt();
                if (c.length() == 6) c = "ff" + c; // someone write material.json with RGB instead of ARGB, unite them
                var color = Integer.parseUnsignedInt(c, 16);
                return new MatInfo.M1D.ColorMap(color, grey);
            }).collect(Collectors.toCollection(ArrayList::new));

            var mat = new MatInfo.M1D(location, colorMaps);

            // validate
            if (mat.getColorMaps().isEmpty()) {
                LogUtils.getLogger().warn("1D Material {} has an empty color map!", mat.getLocation());
                mat.getColorMaps().add(new MatInfo.M1D.ColorMap(0xffffffff, 0));
            }

            // append last map if it needs
            var lastMap = mat.getColorMaps().get(mat.getColorMaps().size() - 1);
            if (lastMap.grey() < 255) {
                mat.getColorMaps().add(new MatInfo.M1D.ColorMap(lastMap.color(), 255));
            }

            LogUtils.getLogger().info("Read 1D material info {} succeed!", location);
            return List.of(mat);
        } catch (IllegalStateException | NullPointerException | UnsupportedOperationException |
                 ClassCastException | NumberFormatException e) {
            LogUtils.getLogger().error("Failed to read 1D material generator transformer info for {}", location, e);
            return List.of();
        }
    }

    private static @NotNull List<MatInfo> getGreyToSpriteMatInfo(JsonObject transformer, ResourceLocation location, int frame) {
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
                return new MatInfo.M3D.SpriteMap(color, tex, grey);
            }).collect(Collectors.toCollection(ArrayList::new));

            var mat = new MatInfo.M3D(location, spriteMaps, frame);

            // validate
            if (mat.getSpriteMaps().isEmpty()) {
                LogUtils.getLogger().warn("3D Material {} has an empty color map!", mat.getLocation());
                mat.getSpriteMaps().add(new MatInfo.M3D.SpriteMap(0xffffffff, Optional.empty(), 0));
            }

            // append last map if it needs
            var lastMap = mat.getSpriteMaps().get(mat.getSpriteMaps().size() - 1);
            if (lastMap.grey() < 255) {
                mat.getSpriteMaps().add(new MatInfo.M3D.SpriteMap(lastMap.color(), lastMap.texture(), 255));
            }

            LogUtils.getLogger().info("Read 3D material info {} succeed!", location);
            return List.of(mat);
        } catch (IllegalStateException | NullPointerException | UnsupportedOperationException |
                 ClassCastException | NumberFormatException e) {
            LogUtils.getLogger().error("Failed to read 3D material generator transformer info for {}", location, e);
            return List.of();
        }
    }

    private static @NotNull List<MatInfo> getAnimatedSpriteMatInfo(JsonObject transformer, ResourceLocation location) {
        try {
            var metaPath = transformer.get("meta").getAsString();
            var meta = ResourceLocation.parse(metaPath + ".png");
            meta = meta.withPrefix("textures/");
//            var animWriter = MaterialAnimMapManager.startAddMat(location, meta);
//            if (animWriter.isEmpty()) {
//                LogUtils.getLogger().error("4D Material {} did not added to map, there is duplicated already exists? But why?", location);
//                return List.of();
//            }
            List<MatInfo> frameMats = new ArrayList<>();
            var frames = transformer.get("frames").getAsInt();
            for (int frame = 0; frame < frames; frame++) {
                var local = location.withSuffix("/%d".formatted(frame));
                var mats = getGreyToSpriteMatInfo(transformer, local, frame);
                frameMats.addAll(mats);

//                if (mats.isEmpty()) {
//                    animWriter.get().addFrame(false, -1);
//                } else if (mats.get(0) instanceof MatInfo.M1D) {
//                    animWriter.get().addFrame(false, mat1dNext - 1);
//                } else if (mats.get(0) instanceof MatInfo.M3D) {
//                    animWriter.get().addFrame(true, mat3dNext - 1);
//                } else {
//                    LogUtils.getLogger().error("WTF? Frame {} in 4D material {} parsed as neither Mat1D nor Mat3D",
//                            frame, location);
//                    return List.of();
//                }
            }
            LogUtils.getLogger().info("Read 4D material info {} succeed!", location);
            return List.of(new MatInfo.M4D(location, meta, frameMats));
        } catch (IllegalStateException | NullPointerException | UnsupportedOperationException |
                 ClassCastException | NumberFormatException e) {
            LogUtils.getLogger().error("Failed to read 4D material generator transformer info for {}", location, e);
            return List.of();
        }
    }

    private static @NotNull List<MatInfo> getFramesMatInfo(JsonObject transformer, ResourceLocation location) {
        try {
            var metaPath = transformer.get("meta").getAsString();
            var meta = ResourceLocation.parse(metaPath + ".png");
            meta = meta.withPrefix("textures/");
//            var animWriter = MaterialAnimMapManager.startAddMat(location, meta);
//            if (animWriter.isEmpty()) {
//                LogUtils.getLogger().error("4D(frames) Material {} did not added to map, there is duplicated already exists? But why?", location);
//                return List.of();
//            }
            List<MatInfo> frameMats = new ArrayList<>();
            var frames = transformer.getAsJsonArray("frames");
            for (int frame = 0; frame < frames.size(); frame++) {
                var trans = frames.get(frame).getAsJsonObject();
                var local = location.withSuffix("/%d".formatted(frame));
                var mats = getMatInfoFromTransformer(trans, local, 0, frame);
                frameMats.addAll(mats);

//                if (mats.isEmpty()) {
//                    animWriter.get().addFrame(false, -1);
//                } else if (mats.get(0) instanceof MatInfo.M1D) {
//                    animWriter.get().addFrame(false, mat1dNext - 1);
//                } else if (mats.get(0) instanceof MatInfo.M3D) {
//                    animWriter.get().addFrame(true, mat3dNext - 1);
//                } else {
//                    LogUtils.getLogger().error("WTF? Frame {} in 4D(frames) material {} parsed as neither Mat1D nor Mat3D",
//                            frame, location);
//                    return List.of();
//                }
            }
            LogUtils.getLogger().info("Read 4D(frames) material info {} succeed!", location);
            return List.of(new MatInfo.M4D(location, meta, frameMats));
        } catch (IllegalStateException | NullPointerException | UnsupportedOperationException |
                 ClassCastException | NumberFormatException e) {
            LogUtils.getLogger().error("Failed to read frames material generator transformer info for {}", location, e);
            return List.of();
        }
    }

    // </editor-fold>


    //<editor-fold desc="Data Mapping">

    static void addMatsMap(List<MatInfo> matInfos,
                           @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {

        for (var info : matInfos) {
            addMap(info, resourceManager);
        }

        // calculate size of tex
        int count1D = 0, unitsFor3D = 0;
        for (var info : matInfos) {
            if (info instanceof MatInfo.M1D) count1D++;
            else if (info instanceof MatInfo.M3D) unitsFor3D++;
        }
        unitsFor1D = (int) Math.ceil(((double) count1D) / MaterialMapTextureManager.TEX_UNIT);
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
    }

    private static Optional<MaterialAnimMapManager.FrameInfo> addMap(MatInfo matInfo, @NotNull ResourceManager resourceManager) {
        if (matInfo instanceof MatInfo.M1D m1) {
            var index = tryAddMat1DMap(m1.getLocation());
            if (index < 0) {
                LogUtils.getLogger().error("1D Material {} did not added to map, there is duplicated already exists? But why?", m1.getLocation());
                return Optional.empty();
            }
            return Optional.of(new MaterialAnimMapManager.FrameInfo(index, false, false));

        } else if (matInfo instanceof MatInfo.M3D m3) {
            var imgs = m3.getSpriteMaps().stream().flatMap(
                    s -> s.texture().flatMap(resourceManager::getResource).flatMap(res -> {
                        try (var str = res.open()) {
                            return Optional.of(Stream.of(NativeImage.read(str)));
                        } catch (IOException e) {
                            return Optional.empty();
                        }
                    }).orElse(Stream.empty())
            ).toList();
            var is32x = imgs.stream().anyMatch(img -> img.getWidth() >= 32 && img.getHeight() >= 32);
            var index = tryAddMat3DMap(m3.getLocation(), is32x);
            if (index < 0) {
                LogUtils.getLogger().error("3D Material {} did not added to map, there is duplicated already exists? But why?", m3.getLocation());
                return Optional.empty();
            }
            return Optional.of(new MaterialAnimMapManager.FrameInfo(index, true, is32x));

        } else if (matInfo instanceof MatInfo.M4D m4) {
            var animWriter = MaterialAnimMapManager.startAddMat(m4.getLocation(), m4.getMeta());
            if (animWriter.isEmpty()) {
                LogUtils.getLogger().error("4D Material {} did not added to map, there is duplicated already exists? But why?", m4.getLocation());
                return Optional.empty();
            }
            for (var fm : m4.getFrames()) {
                var frame = addMap(fm, resourceManager);
                frame.ifPresent(
                        f -> animWriter.get().addFrame(f.is3D(), f.getId(), f.is32x())
                );
            }
            return Optional.empty();
        }

        LogUtils.getLogger().error("Not recognized mat info found: {}", matInfo);
        return Optional.empty();
    }

    //</editor-fold>


    // <editor-fold desc="MatType">

    public static abstract class MatType {
        public static class MatNotFound extends MatType {
        }

        @AllArgsConstructor
        public static class Mat1D extends MatType {
            @Getter private final int id;
            @Getter private final int emissivity;
            @Getter private final List<String> fallbacks;
        }

        @AllArgsConstructor
        public static class Mat3D extends MatType {
            @Getter private final int id;
            @Getter private final boolean is32x;
            @Getter private final int emissivity;
            @Getter private final List<String> fallbacks;
        }

        @AllArgsConstructor
        public static class Mat4D extends MatType {
            @Getter private final int anim;
            @Getter private final int emissivity;
            @Getter private final List<String> fallbacks;
        }
    }

    // </editor-fold>
}
