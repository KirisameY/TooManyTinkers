package com.kirisamey.toomanytinkers.models;

import com.ibm.icu.impl.Pair;
import com.kirisamey.toomanytinkers.models.pose.IAnimatableTicTool3DBoneController;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import io.vavr.collection.Map;
import io.vavr.collection.Vector;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;
import org.joml.Vector3f;


import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.function.Function;

@RequiredArgsConstructor
public class AnimatableTicTool3DUnbakedModel implements IUnbakedGeometry<AnimatableTicTool3DUnbakedModel> {

    private final Vector<AnimatableTicTool3DModelData.UnbakedPart> parts;
    @Getter private final AnimatableTicTool3DModelData.UnbakedBone skeleton;
    @Getter private final IAnimatableTicTool3DBoneController controller;
    @Getter private final ItemTransforms transforms;
    @Getter private final boolean largeTex;
    @Getter private final Map<String, Vector3f> marks;


    @Override
    public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter, IGeometryBakingContext context) {
        parts.forEach(p -> modelGetter.apply(p.model()));
    }

    @Override
    public BakedModel bake(IGeometryBakingContext context, ModelBaker baker,
                           Function<Material, TextureAtlasSprite> spriteGetter,
                           ModelState modelState, ItemOverrides overrides, ResourceLocation modelLocation) {

        var halfBakedParts = parts.map(p -> {
            var model = baker.bake(p.model(), modelState, spriteGetter);
            return Pair.of(p, Objects.requireNonNull(model));
        }).toMap(p -> p.first.id(), p -> p);

        Function<AnimatableTicTool3DModelData.UnbakedBone, io.vavr.collection.Vector<AnimatableTicTool3DModelData.BakedPart>> getParts = b -> {
            return b.parts().map(p -> {
                var pm = halfBakedParts.get(p.first).get();
                var offset = new Vector3f(p.second);
                var unb = pm.first;
                var model = pm.second;
                offset = offset.sub(unb.origin());
                return new AnimatableTicTool3DModelData.BakedPart(unb.id(), model, unb.renderType(), unb.toolPart(), offset);
            });
        };

        Stack<Tuple2<
                ArrayList<AnimatableTicTool3DModelData.BakedBone>,
                AnimatableTicTool3DModelData.UnbakedBone
                >> pushStack = new Stack<>();
        Stack<Tuple3<
                ArrayList<AnimatableTicTool3DModelData.BakedBone>,
                AnimatableTicTool3DModelData.UnbakedBone,
                ArrayList<AnimatableTicTool3DModelData.BakedBone>
                >> bakeStack = new Stack<>();

        ArrayList<AnimatableTicTool3DModelData.BakedBone> finalList = new ArrayList<>();
        pushStack.push(Tuple.of(finalList, skeleton));
        while (!pushStack.empty()) {
            var tuple = pushStack.pop();
            var parentList = tuple._1;
            var unbaked = tuple._2;
            var selfList = new ArrayList<AnimatableTicTool3DModelData.BakedBone>();
            bakeStack.push(Tuple.of(parentList, unbaked, selfList));
            unbaked.bones().forEach(b -> pushStack.push(Tuple.of(selfList, b)));
        }
        while (!bakeStack.empty()) {
            var tuple = bakeStack.pop();
            var parentList = tuple._1;
            var unbaked = tuple._2;
            var selfList = tuple._3;
            var newBone = new AnimatableTicTool3DModelData.BakedBone(unbaked.id(), getParts.apply(unbaked), Vector.ofAll(selfList));
            parentList.add(newBone);
        }

        var bakedSkeleton = finalList.get(0);

        return new AnimatableTicTool3DOriginalBakedModel(bakedSkeleton, controller, transforms, largeTex, marks);
    }

    @Override public Set<String> getConfigurableComponentNames() {
        return IUnbakedGeometry.super.getConfigurableComponentNames();
    }

}
