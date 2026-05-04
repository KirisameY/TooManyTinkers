package com.kirisamey.toomanytinkers.models;

import com.ibm.icu.impl.Pair;
import io.vavr.collection.Stream;
import io.vavr.collection.Vector;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.function.Supplier;


public class AnimatableTicTool3DModelData {

    public record UnbakedPart(
            String id, ResourceLocation model, Supplier<RenderType> renderType,
            int toolPart, Vector3f origin) {
    }

    public record BakedPart(
            String id, BakedModel model, Supplier<RenderType> renderType,
            int toolPart, Vector3f offset) {
    }

    public record PosedPart(
            String id, BakedModel model, Supplier<RenderType> renderType,
            int toolPart, Matrix4f transform
    ) {
    }

    public record UnbakedBone(
            String id, Vector<Pair<String, Vector3f>> parts, Vector<UnbakedBone> bones) {
        public Stream<UnbakedBone> enumBones() {
            return Stream.concat(
                    Stream.of(this),
                    bones.toStream().flatMap(UnbakedBone::enumBones)
            );
        }
    }

    public record BakedBone(
            String id, Vector<BakedPart> parts, Vector<BakedBone> bones) {
        public Stream<BakedBone> enumBones() {
            return Stream.concat(
                    Stream.of(this),
                    bones.flatMap(BakedBone::enumBones)
            );
        }
    }

    public record PosedBone(
            String id, Vector<BakedPart> parts, Vector<PosedBone> bones, Matrix4f transform) {
        public Stream<PosedPart> enumParts() {
            return enumParts(new Matrix4f());
        }

        public Stream<PosedPart> enumParts(Matrix4f alreadyTrans) {
            var nowTrans = alreadyTrans.mul(this.transform, new Matrix4f());
            return Stream.concat(parts.toStream().map(part -> {
                var t = new Matrix4f(nowTrans).translate(part.offset);
                return new PosedPart(part.id, part.model, part.renderType, part.toolPart, t);
            }), bones.toStream().flatMap(b -> b.enumParts(nowTrans)));
        }
    }


    public record UnbakedBoneModifier(
            String boneId, Vector<Pair<String, Vector3f>> newParts, Vector<String> removeParts
    ) {
    }

    public record BakedBoneModifier(
            String boneId, Vector<BakedPart> newParts, Vector<String> removeParts
    ) {
    }

}
