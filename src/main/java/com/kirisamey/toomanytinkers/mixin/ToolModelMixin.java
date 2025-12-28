package com.kirisamey.toomanytinkers.mixin;

import com.kirisamey.toomanytinkers.rendering.MaterialMapTextureManager;
import com.kirisamey.toomanytinkers.rendering.TmtRenderTypes;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.logging.LogUtils;
import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
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
        // todo: 搞个排除特定ID物品的配置表，我不想看到手杖和打火石
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
        var matLocation = material.getLocation('_');
        var info = MaterialMapTextureManager.getTexInfo(matLocation);

        var vtx = -1;
        if (info instanceof MaterialMapTextureManager.MatType.Mat1D m1d) {
            vtx = tooManyTinkers$getVertex(m1d.getId(), false, false);
        } else if (info instanceof MaterialMapTextureManager.MatType.Mat3D m3d) {
            vtx = tooManyTinkers$getVertex(m3d.getId(), true, false);
        } else {
            return original.call(spriteGetter, texture, material);
        }

        var spr = spriteGetter.apply(texture);

        LogUtils.getLogger().debug("TMT: replaced sprite for tex: {}, mat: {}", texture.texture(), matLocation);

        return new MaterialRenderInfo.TintedSprite(spr, vtx, 0);
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
    private static List<BakedQuad> replaceLargeQuards(
            Function<Material, TextureAtlasSprite> spriteGetter, Material texture, MaterialVariantId material,
            int tintIndex, Transformation transformation, @Nullable ItemLayerPixels pixels,
            Operation<List<BakedQuad>> original) {

        // rewrite logic of getMaterialSprite
        MaterialRenderInfo.TintedSprite sprite;
        {
            var matLocation = material.getLocation('_');
            var info = MaterialMapTextureManager.getTexInfo(matLocation);

            if (info instanceof MaterialMapTextureManager.MatType.Mat1D m1d) {
                var vtx = tooManyTinkers$getVertex(m1d.getId(), false, true);
                var spr = spriteGetter.apply(texture);
                sprite = new MaterialRenderInfo.TintedSprite(spr, vtx, 0);
            } else if (info instanceof MaterialMapTextureManager.MatType.Mat3D m3d) {
                var vtx = tooManyTinkers$getVertex(m3d.getId(), true, true);
                var spr = spriteGetter.apply(texture);
                sprite = new MaterialRenderInfo.TintedSprite(spr, vtx, 0);
            } else {
                // origin
                sprite = MaterialModel.getMaterialSprite(spriteGetter, texture, material);
            }
        }

        return MantleItemLayerModel.getQuadsForSprite(sprite.color(), tintIndex, sprite.sprite(), transformation, sprite.emissivity(), pixels);
    }


//    @WrapOperation(
//            method = "bakeInternal",
//            at = @At(
//                    value = "INVOKE",
//                    target = "Lslimeknights/tconstruct/library/client/model/tools/MaterialModel;" +
//                            "getQuadsForMaterial(" +
//                            "Ljava/util/function/Function;" +
//                            "Lnet/minecraft/client/resources/model/Material;" +
//                            "Lslimeknights/tconstruct/library/materials/definition/MaterialVariantId;" +
//                            "ILcom/mojang/math/Transformation;" +
//                            "Lslimeknights/mantle/util/ItemLayerPixels;)" +
//                            "Ljava/util/List;"),
//            remap = false
//    )
//    private static List<BakedQuad> replaceLargeColor(
//            Function<Material, TextureAtlasSprite> spriteGetter, Material texture,
//            MaterialVariantId material, int tintIndex, Transformation transformation,
//            @Nullable ItemLayerPixels pixels, Operation<List<BakedQuad>> original) {
//        var origin = original.call(spriteGetter, texture, material, tintIndex, transformation, pixels);
//        return origin.stream().map(quad -> {
//            var c = quad.getTintIndex();
//            // todo
//            return quad;
//        }).toList();
//    }


    @Unique
    private static int tooManyTinkers$getVertex(
            int id, boolean is3D, @SuppressWarnings("SameParameterValue") boolean isLarge) {
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

}
