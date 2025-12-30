package com.kirisamey.toomanytinkers.mixin;

import com.kirisamey.toomanytinkers.configs.TmtExcludes;
import com.kirisamey.toomanytinkers.rendering.MaterialMapTextureManager;
import com.kirisamey.toomanytinkers.rendering.TmtRenderTypes;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.datafixers.util.Function3;
import com.mojang.logging.LogUtils;
import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.util.FastColor;
import net.minecraftforge.client.RenderTypeGroup;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import slimeknights.mantle.client.model.util.MantleItemLayerModel;
import slimeknights.mantle.util.ItemLayerPixels;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfo;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfoLoader;
import slimeknights.tconstruct.library.client.model.tools.MaterialModel;
import slimeknights.tconstruct.library.client.model.tools.ToolModel;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

@Mixin(ToolModel.class)
public class ToolModelMixin {


    @WrapOperation(
            method = "makeModelBuilder",
            at = @At(
                    value = "INVOKE",
                    target = "Lslimeknights/mantle/client/model/util/MantleItemLayerModel;" +
                            "getDefaultRenderType(" +
                            "Lnet/minecraftforge/client/model/geometry/IGeometryBakingContext;)" +
                            "Lnet/minecraftforge/client/RenderTypeGroup;"),
            remap = false
    )
    private static RenderTypeGroup overrideRenderType(IGeometryBakingContext ctx, Operation<RenderTypeGroup> original) {
        // todo: 这里得加个针对物品不使用shader的配置项
        return TmtRenderTypes.getTinkerMappingGroup();
    }


    @WrapOperation(
            method = "bakeInternal",
            at = @At(
                    value = "INVOKE",
                    target = "Lslimeknights/tconstruct/library/client/model/tools/MaterialModel;" +
                            "getMaterialSprite(" +
                            "Ljava/util/function/Function;" +
                            "Lnet/minecraft/client/resources/model/Material;" +
                            "Lslimeknights/tconstruct/library/materials/definition/MaterialVariantId;)" +
                            "Lslimeknights/tconstruct/library/client/materials/MaterialRenderInfo$TintedSprite;"),
            remap = false
    )
    private static MaterialRenderInfo.TintedSprite replaceMatSprite(
            Function<Material, TextureAtlasSprite> spriteGetter, Material texture, MaterialVariantId material,
            Operation<MaterialRenderInfo.TintedSprite> original) {
//        var matLocation = material.getLocation('_');
//        var info = MaterialMapTextureManager.getTexInfo(matLocation);
//
//        if (TmtExcludes.isExcluded(texture.texture(), matLocation)) //exclude
//            info = new MaterialMapTextureManager.MatType.MatNotFound();
//
//        var vtx = -1;
//        if (info instanceof MaterialMapTextureManager.MatType.Mat1D m1d) {
//            vtx = tooManyTinkers$getVertex(m1d.getId(), false, false);
//        } else if (info instanceof MaterialMapTextureManager.MatType.Mat3D m3d) {
//            vtx = tooManyTinkers$getVertex(m3d.getId(), true, false);
//        } else {
//            LogUtils.getLogger().debug("[TMT] pair <{}, {}> has been excluded or not mapped, replace cancelled, now getting fallback",
//                    texture.texture(), matLocation);
//            var result = original.call(spriteGetter, texture, material);
//            LogUtils.getLogger().debug("[TMT] got fallback for tex: {}, part: {};", matLocation, texture.texture());
//            LogUtils.getLogger().debug("[TMT] sprite: {}, color: {}, emissivity: {}",
//                    result.sprite(), Integer.toHexString(result.color()), result.emissivity());
//            if (FastColor.ARGB32.alpha(result.color()) <= 0x7f) {
//                var fixedColor = FastColor.ARGB32.color(
//                        0x80,
//                        FastColor.ARGB32.red(result.color()),
//                        FastColor.ARGB32.green(result.color()),
//                        FastColor.ARGB32.blue(result.color())
//                );
//                result = new MaterialRenderInfo.TintedSprite(result.sprite(), fixedColor, result.emissivity());
//                LogUtils.getLogger().warn("[TMT] mat: {} has default alpha less then 0x7f, make it 0x80 (color: {}) for part: {}",
//                        matLocation, Integer.toHexString(fixedColor), texture.texture());
//            }
//            return result;
//        }
//
//        var spr = spriteGetter.apply(texture);
//
//        LogUtils.getLogger().debug("[TMT] replaced sprite for part: {}, mat: {}", texture.texture(), matLocation);
//
//        return new MaterialRenderInfo.TintedSprite(spr, vtx, 0);
        return tooManyTinkers$fixedGetMaterialSprite(spriteGetter, texture, material, false, original::call, "getSmallMatSprite");
    }


    @WrapOperation(
            method = "bakeInternal",
            at = @At(
                    value = "INVOKE",
                    target = "Lslimeknights/tconstruct/library/client/model/tools/MaterialModel;" +
                            "getQuadsForMaterial(" +
                            "Ljava/util/function/Function;Lnet/minecraft/client/resources/model/Material;" +
                            "Lslimeknights/tconstruct/library/materials/definition/MaterialVariantId;" +
                            "ILcom/mojang/math/Transformation;" +
                            "Lslimeknights/mantle/util/ItemLayerPixels;)" +
                            "Ljava/util/List;"),
            remap = false
    )
    private static List<BakedQuad> replaceLargeQuads(
            Function<Material, TextureAtlasSprite> spriteGetter, Material texture, MaterialVariantId material,
            int tintIndex, Transformation transformation, @Nullable ItemLayerPixels pixels,
            Operation<List<BakedQuad>> original) {

//        // rewrite logic of getMaterialSprite
//        MaterialRenderInfo.TintedSprite sprite;
//        {
//            var matLocation = material.getLocation('_');
//            var info = MaterialMapTextureManager.getTexInfo(matLocation);
//
//            if (info instanceof MaterialMapTextureManager.MatType.Mat1D m1d) {
//                var vtx = tooManyTinkers$getVertex(m1d.getId(), false, true);
//                var spr = spriteGetter.apply(texture);
//                sprite = new MaterialRenderInfo.TintedSprite(spr, vtx, 0);
//            } else if (info instanceof MaterialMapTextureManager.MatType.Mat3D m3d) {
//                var vtx = tooManyTinkers$getVertex(m3d.getId(), true, true);
//                var spr = spriteGetter.apply(texture);
//                sprite = new MaterialRenderInfo.TintedSprite(spr, vtx, 0);
//            } else {
//                // origin
//                sprite = MaterialModel.getMaterialSprite(spriteGetter, texture, material);
//            }
//        }
        var sprite = tooManyTinkers$fixedGetMaterialSprite(spriteGetter, texture, material, true,
                MaterialModel::getMaterialSprite, "getLargeMatSprite");

        return MantleItemLayerModel.getQuadsForSprite(sprite.color(), tintIndex, sprite.sprite(), transformation, sprite.emissivity(), pixels);
    }


    @Unique
    private static int tooManyTinkers$getVertex(
            int id, boolean is3D, boolean isLarge) {
        int a = 0x7f;
        if (is3D) a -= 0x40;
        if (isLarge) a -= 0x20;

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

        return b + (g << 8) + (r << 16) + (a << 24);
    }

    @Unique
    private static MaterialRenderInfo.TintedSprite tooManyTinkers$fixedGetMaterialSprite(
            Function<Material, TextureAtlasSprite> spriteGetter, Material texture, MaterialVariantId material, Boolean isLarge,
            Function3<Function<Material, TextureAtlasSprite>, Material, MaterialVariantId, MaterialRenderInfo.TintedSprite> fallback,
            String logStage) {
        var matLocation = material.getLocation('_');
        var info = MaterialMapTextureManager.getTexInfo(matLocation);

        if (TmtExcludes.isExcluded(texture.texture(), matLocation)) //exclude
            info = new MaterialMapTextureManager.MatType.MatNotFound();

        var vtx = -1;
        if (info instanceof MaterialMapTextureManager.MatType.Mat1D m1d) {
            vtx = tooManyTinkers$getVertex(m1d.getId(), false, isLarge);
        } else if (info instanceof MaterialMapTextureManager.MatType.Mat3D m3d) {
            vtx = tooManyTinkers$getVertex(m3d.getId(), true, isLarge);
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
            return result;
        }

        var spr = spriteGetter.apply(texture);

        LogUtils.getLogger().debug("[TMT/{}] replaced sprite for part: {}, mat: {}", logStage, texture.texture(), matLocation);
        return new MaterialRenderInfo.TintedSprite(spr, vtx, 0);
    }

}
