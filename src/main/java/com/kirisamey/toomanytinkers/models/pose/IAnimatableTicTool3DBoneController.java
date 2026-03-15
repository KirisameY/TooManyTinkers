package com.kirisamey.toomanytinkers.models.pose;

import com.kirisamey.toomanytinkers.models.AnimatableTicTool3DModelData;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

public interface IAnimatableTicTool3DBoneController {
    default AnimatableTicTool3DModelData.PosedBone pose(ItemStack itemStack, AnimatableTicTool3DModelData.BakedBone bone) {
        return pose(itemStack, bone, new Matrix4f());
    }

    AnimatableTicTool3DModelData.PosedBone pose(ItemStack itemStack, AnimatableTicTool3DModelData.BakedBone bone, Matrix4f transform);
}
