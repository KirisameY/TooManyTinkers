package com.kirisamey.toomanytinkers.models;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;


public class AnimatableTicTool3DModelData {

    public record UnbakedPart(
            String id, ResourceLocation model, Supplier<RenderType> renderType,
            int toolPart, Vector3f origin, Vector3f offset) {
    }

    public record BakedPart(
            String id, BakedModel model, Supplier<RenderType> renderType,
            int toolPart, Vector3f origin, Vector3f offset) {
    }

    public record UnbakedBone(
            String id, List<String> parts, List<UnbakedBone> bones) {
        public Stream<UnbakedBone> enumBones() {
            return Stream.concat(
                    Stream.of(this),
                    bones.stream().flatMap(UnbakedBone::enumBones)
            );
        }
    }

    public record BakedBone(
            String id, List<BakedPart> parts, List<BakedBone> bones) {
        public Stream<BakedBone> enumBones() {
            return Stream.concat(
                    Stream.of(this),
                    bones.stream().flatMap(BakedBone::enumBones)
            );
        }
    }
}
