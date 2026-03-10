package com.kirisamey.toomanytinkers.models.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public class AnimatableTicTool3DClientItemExtensions implements IClientItemExtensions {
    AnimatableTicTool3DRenderer renderer = null;

    @Override public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        if (renderer == null) {
            var mc = Minecraft.getInstance();
            renderer = new AnimatableTicTool3DRenderer(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
        }
        return renderer;
    }

    @Override
    public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm, ItemStack itemInHand, float partialTick, float equipProcess, float swingProcess) {
        return IClientItemExtensions.super.applyForgeHandTransform(poseStack, player, arm, itemInHand, partialTick, equipProcess, swingProcess);
    }
}
