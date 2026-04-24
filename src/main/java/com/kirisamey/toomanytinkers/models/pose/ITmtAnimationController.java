package com.kirisamey.toomanytinkers.models.pose;

import io.vavr.Tuple2;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface ITmtAnimationController {
    Tuple2<String, Float> getPose(ItemStack stack, @NotNull ItemDisplayContext itemDisplayContext);
}
