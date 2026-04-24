package com.kirisamey.toomanytinkers.models.pose;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class EmptyTmtAnimationController implements ITmtAnimationController {

    private EmptyTmtAnimationController() {
    }

    public static final EmptyTmtAnimationController INSTANCE = new EmptyTmtAnimationController();

    @Override public Tuple2<String, Float> getPose(ItemStack stack, @NotNull ItemDisplayContext itemDisplayContext) {
        return Tuple.of("idle", 0f);
    }
}
