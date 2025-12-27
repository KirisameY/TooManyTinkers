package com.kirisamey.toomanytinkers.mixin;

import com.kirisamey.toomanytinkers.rendering.MaterialMapTextureManager;
import com.kirisamey.toomanytinkers.rendering.TmtRenderTypes;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraftforge.client.RenderTypeGroup;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
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
    private static MaterialRenderInfo.TintedSprite replaceMatSprite(
            Function<Material, TextureAtlasSprite> spriteGetter, Material texture, MaterialVariantId material,
            Operation<MaterialRenderInfo.TintedSprite> original) {
        var matLocation = material.getLocation('_');
        var info = MaterialMapTextureManager.getTexInfo(matLocation);
//        LogUtils.getLogger().debug("TMT: solving material sprite of material {}", matLocation);
//        LogUtils.getLogger().debug("TMT: result: {}, id {}", info.getA(), info.getB());
        var vtx = -1;
        if (info.getA() != MaterialMapTextureManager.MatType.NotFound) {
            var is3D = info.getA() == MaterialMapTextureManager.MatType.Mat3D;
            vtx = tooManyTinkers$getVertex(info.getB(), is3D, false);
        }

        var spr = spriteGetter.apply(texture);
        return new MaterialRenderInfo.TintedSprite(spr, vtx, 0);
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

        var h = MaterialMapTextureManager.getInstance().getTexHeigh();

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
