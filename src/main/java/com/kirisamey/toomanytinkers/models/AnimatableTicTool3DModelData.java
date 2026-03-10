package com.kirisamey.toomanytinkers.models;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;


public class AnimatableTicTool3DModelData {

    public record UnbakedPart(String id, ResourceLocation model, int toolPart, Vector3f shift) {
    }

    public record BakedPart(String id, BakedModel model, int toolPart, Vector3f shift) {
    }
}
