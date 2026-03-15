package com.kirisamey.toomanytinkers.models.pose;

import io.vavr.Tuple2;
import net.minecraft.world.item.ItemStack;

public interface ITmtAnimationController {
    Tuple2<String, Float> getPose(ItemStack stack);
}
