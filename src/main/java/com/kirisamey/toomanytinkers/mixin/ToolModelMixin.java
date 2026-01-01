package com.kirisamey.toomanytinkers.mixin;

import com.kirisamey.toomanytinkers.rendering.materialmap.MaterialMapsManager;
import com.kirisamey.toomanytinkers.rendering.TmtRenderTypes;
import com.kirisamey.toomanytinkers.utils.TmtLookupUtils;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraftforge.client.RenderTypeGroup;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;
import slimeknights.mantle.client.model.util.MantleItemLayerModel;
import slimeknights.mantle.util.ItemLayerPixels;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfo;
import slimeknights.tconstruct.library.client.model.tools.MaterialModel;
import slimeknights.tconstruct.library.client.model.tools.ToolModel;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;

import javax.annotation.Nullable;
import java.util.List;
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
    private static MaterialRenderInfo.TintedSprite replaceNormalMatSprite(
            Function<Material, TextureAtlasSprite> spriteGetter, Material texture, MaterialVariantId material,
            Operation<MaterialRenderInfo.TintedSprite> original) {
        return TmtLookupUtils.fixedGetMaterialSprite(
                spriteGetter, texture, material, false, original::call, "getSmallMatSprite"
        ).first;
    }


    @WrapOperation(
            method = "bakeInternal",
            at = @At(
                    value = "INVOKE",
                    target = "Lslimeknights/mantle/client/model/util/MantleItemLayerModel;" +
                            "getQuadsForSprite(" +
                            "II" +
                            "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;" +
                            "Lcom/mojang/math/Transformation;" +
                            "I" +
                            "Lslimeknights/mantle/util/ItemLayerPixels;)" +
                            "Ljava/util/List;"
            ),
            slice = @Slice(
                    to = @At(
                            value = "INVOKE",
                            target = "Lslimeknights/tconstruct/library/client/model/tools/MaterialModel;" +
                                    "getQuadsForMaterial(" +
                                    "Ljava/util/function/Function;Lnet/minecraft/client/resources/model/Material;" +
                                    "Lslimeknights/tconstruct/library/materials/definition/MaterialVariantId;" +
                                    "I" +
                                    "Lcom/mojang/math/Transformation;" +
                                    "Lslimeknights/mantle/util/ItemLayerPixels;)" +
                                    "Ljava/util/List;"
                    )
            ),
            remap = false
    )
    private static List<BakedQuad> replaceNormalQuadTints(
            int color, int tint, TextureAtlasSprite sprite, Transformation transform, int emissivity,
            @Nullable ItemLayerPixels pixels, @NotNull Operation<List<BakedQuad>> original,
            @Local(name = "material") MaterialVariantId materialCapture) {
        var t = MaterialMapsManager.getTintIfIs4D(materialCapture.getLocation('_'));
        if (t >= 0) tint = t;
        return original.call(color, tint, sprite, transform, emissivity, pixels);
    }


    @WrapOperation(
            method = "bakeInternal",
            at = @At(
                    value = "INVOKE",
                    target = "Lslimeknights/tconstruct/library/client/model/tools/MaterialModel;" +
                            "getQuadsForMaterial(" +
                            "Ljava/util/function/Function;Lnet/minecraft/client/resources/model/Material;" +
                            "Lslimeknights/tconstruct/library/materials/definition/MaterialVariantId;" +
                            "I" +
                            "Lcom/mojang/math/Transformation;" +
                            "Lslimeknights/mantle/util/ItemLayerPixels;)" +
                            "Ljava/util/List;"),
            remap = false
    )
    private static List<BakedQuad> replaceLargeQuads(
            Function<Material, TextureAtlasSprite> spriteGetter, Material texture, MaterialVariantId material,
            int tintIndex, Transformation transformation, @Nullable ItemLayerPixels pixels,
            Operation<List<BakedQuad>> original) {
        var pair = TmtLookupUtils.fixedGetMaterialSprite(spriteGetter, texture, material, true,
                MaterialModel::getMaterialSprite, "getLargeMatSprite");
        var sprite = pair.first;
        var tint = pair.second;
        if (tint >= 0) tintIndex = tint;

        return MantleItemLayerModel.getQuadsForSprite(sprite.color(), tintIndex, sprite.sprite(), transformation, sprite.emissivity(), pixels);
    }
}
