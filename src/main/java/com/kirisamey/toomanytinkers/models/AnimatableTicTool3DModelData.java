package com.kirisamey.toomanytinkers.models;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

import java.util.function.Supplier;


public class AnimatableTicTool3DModelData {

    public record UnbakedPart(
            String id, ResourceLocation model, Supplier<RenderType> renderType,
            int toolPart, Vector3f shift) {
    }

    public record BakedPart(
            String id, BakedModel model, Supplier<RenderType> renderType,
            int toolPart, Vector3f shift) {
    }
}
