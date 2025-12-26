package com.kirisamey.toomanytinkers.mixin;

import com.kirisamey.toomanytinkers.rendering.TmtRenderTypes;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraftforge.client.RenderTypeGroup;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfo;
import slimeknights.tconstruct.library.client.model.tools.ToolModel;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;

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
    private static MaterialRenderInfo.TintedSprite forceGrayMaterialSprite(
            Function<Material, TextureAtlasSprite> spriteGetter, Material texture, MaterialVariantId material,
            Operation<MaterialRenderInfo.TintedSprite> original) {
        var vtx = 0xffff8800; // todo: 替换为相应数据
        return new MaterialRenderInfo.TintedSprite(spriteGetter.apply(texture), vtx, 0);
    }

}
