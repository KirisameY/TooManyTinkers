package com.kirisamey.toomanytinkers.models;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

@RequiredArgsConstructor
public class AnimatableTicTool3DUnbakedModel implements IUnbakedGeometry<AnimatableTicTool3DUnbakedModel> {

    private final List<AnimatableTicTool3DModelData.UnbakedPart> parts;
    @Getter private final ItemTransforms transforms;
    @Getter private final boolean largeTex;


    @Override
    public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter, IGeometryBakingContext context) {
        parts.forEach(p -> modelGetter.apply(p.model()));
    }

    @Override
    public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides, ResourceLocation modelLocation) {
        var bakedParts = parts.stream().map(p -> {
            var model = baker.bake(p.model(), modelState, spriteGetter);
            return new AnimatableTicTool3DModelData.BakedPart(p.id(), model, p.toolPart(), p.shift());
        }).toList();
        return new AnimatableTicTool3DOriginalBakedModel(bakedParts, transforms, largeTex);
    }

    @Override public Set<String> getConfigurableComponentNames() {
        return IUnbakedGeometry.super.getConfigurableComponentNames();
    }


}
