package com.kirisamey.toomanytinkers.mixin;

import com.kirisamey.toomanytinkers.configs.TmtExcludes;
import com.kirisamey.toomanytinkers.rendering.TmtAnimColorBakedQuad;
import com.kirisamey.toomanytinkers.rendering.TmtRenderTypes;
import com.kirisamey.toomanytinkers.rendering.materialmap.MaterialMapsManager;
import com.kirisamey.toomanytinkers.utils.TmtLookupUtils;
import com.kirisamey.toomanytinkers.utils.TmtMixinUtils;
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
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfo;
import slimeknights.tconstruct.library.client.model.tools.MaterialModel;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;

import java.util.List;
import java.util.function.Function;


@Mixin(MaterialModel.class)
public class PartModelMixin {

    @WrapOperation(
            method = "bakeInternal",
            at = @At(
                    value = "INVOKE",
                    target = "Lslimeknights/mantle/client/model/util/MantleItemLayerModel;" +
                            "getDefaultRenderType(" +
                            "Lnet/minecraftforge/client/model/geometry/IGeometryBakingContext;)" +
                            "Lnet/minecraftforge/client/RenderTypeGroup;"
            ),
            remap = false
    )
    private static RenderTypeGroup overrideRenderType(IGeometryBakingContext ctx, Operation<RenderTypeGroup> original) {
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
    private static MaterialRenderInfo.TintedSprite replaceSprite(
            Function<Material, TextureAtlasSprite> spriteGetter, Material texture, MaterialVariantId material,
            Operation<MaterialRenderInfo.TintedSprite> original) {
        var result = TmtLookupUtils.fixedGetMaterialSprite(spriteGetter, texture, material, false, original::call, "getToolPartSprite");
        TmtMixinUtils.PartModel.animForQuads = result.second;
        TmtMixinUtils.PartModel.excludedForQuads = TmtExcludes.isExcluded(texture.texture(), material.getLocation('_'));
        return result.first;
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
                            "I)" +
                            "Ljava/util/List;"
            ),
            remap = false
    )
    private static List<BakedQuad> replaceQuadTints(
            int color, int tint, TextureAtlasSprite sprite, Transformation transform, int emissivity,
            @NotNull Operation<List<BakedQuad>> original) {
        var anim = TmtMixinUtils.PartModel.animForQuads;
        var excluded = TmtMixinUtils.PartModel.excludedForQuads;
        TmtMixinUtils.PartModel.resetForQuads();

        var result = original.call(color, tint, sprite, transform, emissivity);
        if (excluded) return result; //exclude
        if (anim >= 0) result = result.stream()
                .map(q -> (BakedQuad) TmtAnimColorBakedQuad.fromBakedQuad(q, anim, false))
                .toList();
        return result;
    }

}
