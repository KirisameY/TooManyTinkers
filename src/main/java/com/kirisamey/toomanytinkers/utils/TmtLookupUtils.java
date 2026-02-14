package com.kirisamey.toomanytinkers.utils;

import com.ibm.icu.impl.Pair;
import com.kirisamey.toomanytinkers.configs.TmtExcludes;
import com.kirisamey.toomanytinkers.rendering.materialmap.MaterialMapTextureManager;
import com.kirisamey.toomanytinkers.rendering.materialmap.MaterialMapsManager;
import com.mojang.datafixers.util.Function3;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.util.FastColor;
import net.minecraft.world.inventory.InventoryMenu;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector4f;
import org.joml.Vector4i;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfo;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;

import java.util.List;
import java.util.function.Function;

public class TmtLookupUtils {

    public static @NotNull Pair<MaterialRenderInfo.TintedSprite, Integer> fixedGetMaterialSprite(
            Function<Material, TextureAtlasSprite> spriteGetter, @NotNull Material texture, @NotNull MaterialVariantId material, Boolean isLarge,
            Function3<Function<Material, TextureAtlasSprite>, Material, MaterialVariantId, MaterialRenderInfo.TintedSprite> fallback,
            String logStage) {
        var matLocation = material.getLocation('_');
        var info = MaterialMapsManager.getTexInfo(matLocation);

        if (TmtExcludes.isExcluded(texture.texture(), matLocation)) //exclude
            info = new MaterialMapsManager.MatType.MatNotFound();

        var vtx = -1;
        var emissivity = 0;
        var anim = -1;
        List<String> fallbacks = List.of();
        if (info instanceof MaterialMapsManager.MatType.Mat1D m1d) {
            vtx = getVertexColor(m1d.getId(), false, isLarge, false);
            emissivity = m1d.getEmissivity();
            fallbacks = m1d.getFallbacks();
        } else if (info instanceof MaterialMapsManager.MatType.Mat3D m3d) {
            vtx = getVertexColor(m3d.getId(), true, isLarge, m3d.is32x());
            emissivity = m3d.getEmissivity();
            fallbacks = m3d.getFallbacks();
        } else if (info instanceof MaterialMapsManager.MatType.Mat4D m4d) {
            anim = m4d.getAnim();
            emissivity = m4d.getEmissivity();
            fallbacks = m4d.getFallbacks();
        } else {
            LogUtils.getLogger().debug("[TMT/{}] pair <{}, {}> has been excluded or not mapped, replace cancelled, now getting fallback",
                    logStage, texture.texture(), matLocation);
            var result = fallback.apply(spriteGetter, texture, material);
            LogUtils.getLogger().debug("[TMT/{}] got fallback for tex: {}, part: {};", logStage, matLocation, texture.texture());
            LogUtils.getLogger().debug("[TMT/{}] sprite: {}, color: {}, emissivity: {}",
                    logStage, result.sprite(), Integer.toHexString(result.color()), result.emissivity());
            if (FastColor.ARGB32.alpha(result.color()) <= 0x7f) {
                var fixedColor = FastColor.ARGB32.color(
                        0x80,
                        FastColor.ARGB32.red(result.color()),
                        FastColor.ARGB32.green(result.color()),
                        FastColor.ARGB32.blue(result.color())
                );
                result = new MaterialRenderInfo.TintedSprite(result.sprite(), fixedColor, result.emissivity());
                LogUtils.getLogger().warn("[TMT/{}] mat: {} has default alpha less then 0x7f, make it 0x80 (color: {}) for part: {}",
                        logStage, matLocation, Integer.toHexString(fixedColor), texture.texture());
            }
            return Pair.of(result, -1);
        }

        TextureAtlasSprite spr = spriteGetter.apply(texture);

        var modelManager = Minecraft.getInstance().getModelManager();
        for (var matFallback : fallbacks) {
            var partId = texture.texture().withSuffix("_" + matFallback);
            var tex = modelManager.getAtlas(InventoryMenu.BLOCK_ATLAS).getSprite(partId);
            if (tex.contents().name() != MissingTextureAtlasSprite.getLocation()) {
                spr = tex;
                break;
            }
        }

        LogUtils.getLogger().debug("[TMT/{}] replaced sprite for part: {}, mat: {}, emissivity: {}", logStage, texture.texture(), matLocation, emissivity);
        return Pair.of(new MaterialRenderInfo.TintedSprite(spr, vtx, emissivity), anim);
    }

    public static Vector4i getVertexColorRgba(int id, boolean is3D, boolean isLarge, boolean isTex32) {
        if (id < 0) return new Vector4i(0xff);

        int a = 0x7f;
        if (is3D) a -= 0x40;
        if (isLarge) a -= 0x20;
        if (isTex32) a -= 0x10;

        var h = MaterialMapTextureManager.getTexHeigh();

        int r, g, b;
        if (!is3D) {
            b = id % 256;
            g = (id / 256) % h;
            r = (id / 256) / h;
        } else {
            b = 0;
            g = id % h;
            r = id / h;
        }

        return new Vector4i(r, g, b, a);
    }

    public static int getVertexColor(int id, boolean is3D, boolean isLarge, boolean isTex32) {
        var rgba = getVertexColorRgba(id, is3D, isLarge, isTex32);
        return rgba.z + (rgba.y << 8) + (rgba.x << 16) + (rgba.w << 24);
    }

    public static Vector4f getVertexColorRgbaF(int id, boolean is3D, boolean isLarge, boolean isTex32) {
        var rgba = getVertexColorRgba(id, is3D, isLarge, isTex32);
        return new Vector4f(rgba).div(255f);
    }
}
