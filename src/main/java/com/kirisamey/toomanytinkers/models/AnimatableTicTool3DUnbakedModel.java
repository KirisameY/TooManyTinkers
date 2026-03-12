package com.kirisamey.toomanytinkers.models;

import com.ibm.icu.impl.Pair;
import com.mojang.datafixers.types.Func;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class AnimatableTicTool3DUnbakedModel implements IUnbakedGeometry<AnimatableTicTool3DUnbakedModel> {

    private final List<AnimatableTicTool3DModelData.UnbakedPart> parts;
    @Getter private final AnimatableTicTool3DModelData.UnbakedBone skeleton;
    @Getter private final ItemTransforms transforms;
    @Getter private final boolean largeTex;


    @Override
    public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter, IGeometryBakingContext context) {
        parts.forEach(p -> modelGetter.apply(p.model()));
    }

    @Override
    public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides, ResourceLocation modelLocation) {
        //noinspection Convert2MethodRef
        var bakedParts = parts.stream().map(p -> {
            var model = baker.bake(p.model(), modelState, spriteGetter);
            return new AnimatableTicTool3DModelData.BakedPart(p.id(), model, p.renderType(), p.toolPart(), p.origin(), p.offset());
        }).collect(Collectors.toMap(p -> p.id(), p -> p));

        Function<AnimatableTicTool3DModelData.UnbakedBone, List<AnimatableTicTool3DModelData.BakedPart>> getParts = b ->
                b.parts().stream().map(bakedParts::get).toList();
        Queue<Pair<AnimatableTicTool3DModelData.BakedBone, AnimatableTicTool3DModelData.UnbakedBone>> bakeQueue = new LinkedList<>();
        var bakedSkeleton = new AnimatableTicTool3DModelData.BakedBone(skeleton.id(), getParts.apply(skeleton), new ArrayList<>());
        bakeQueue.add(Pair.of(bakedSkeleton, skeleton));
        while (!bakeQueue.isEmpty()) {
            var p = bakeQueue.remove();
            var bb = p.first;
            var ub = p.second;
            ub.bones().forEach(b -> {
                var newBone = new AnimatableTicTool3DModelData.BakedBone(b.id(), getParts.apply(b), new ArrayList<>());
                bb.bones().add(newBone);
                bakeQueue.add(Pair.of(newBone, b));
            });
        }

        return new AnimatableTicTool3DOriginalBakedModel(bakedSkeleton, transforms, largeTex);
    }

    @Override public Set<String> getConfigurableComponentNames() {
        return IUnbakedGeometry.super.getConfigurableComponentNames();
    }

}
