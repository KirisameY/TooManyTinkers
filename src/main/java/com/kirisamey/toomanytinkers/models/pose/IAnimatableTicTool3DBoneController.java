package com.kirisamey.toomanytinkers.models.pose;

import com.kirisamey.toomanytinkers.models.AnimatableTicTool3DModelData;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

public interface IAnimatableTicTool3DBoneController {
    default AnimatableTicTool3DModelData.PosedBone pose(
            ItemStack itemStack, AnimatableTicTool3DModelData.BakedBone bone,
            @NotNull ItemDisplayContext itemDisplayContext) {
        return pose(itemStack, bone, itemDisplayContext, new Matrix4f());
    }

    AnimatableTicTool3DModelData.PosedBone pose(
            ItemStack itemStack, AnimatableTicTool3DModelData.BakedBone bone,
            @NotNull ItemDisplayContext itemDisplayContext, Matrix4f transform
    );
}
