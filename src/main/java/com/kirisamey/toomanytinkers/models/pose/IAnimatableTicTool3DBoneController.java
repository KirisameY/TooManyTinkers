package com.kirisamey.toomanytinkers.models.pose;

import com.kirisamey.toomanytinkers.models.AnimatableTicTool3DModelData;
import net.minecraft.world.item.ItemStack;

public interface IAnimatableTicTool3DBoneController {
    AnimatableTicTool3DModelData.PosedBone pose(ItemStack itemStack, AnimatableTicTool3DModelData.BakedBone bone);
}
