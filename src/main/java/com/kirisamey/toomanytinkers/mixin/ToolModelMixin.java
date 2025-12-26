package com.kirisamey.toomanytinkers.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfo;
import slimeknights.tconstruct.library.client.model.tools.ToolModel;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;

import java.util.function.Function;

@Mixin(ToolModel.class)
public class ToolModelMixin {

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
        return new MaterialRenderInfo.TintedSprite(spriteGetter.apply(texture), -1, 0);
    }
}
