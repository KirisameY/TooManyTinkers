package com.kirisamey.toomanytinkers.models.pose;

import com.kirisamey.toomanytinkers.TmtRegistries;
import com.kirisamey.toomanytinkers.TooManyTinkers;
import com.kirisamey.toomanytinkers.rendering.TmtRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class TmtAnimationControllers {
    private static final DeferredRegister<IAnimatableTicTool3DBoneController> BONE_CONTROLLERS =
            DeferredRegister.create(TmtRegistries.BONE_CONTROLLERS_REGKEY, TooManyTinkers.MODID);

    private static final DeferredRegister<ITmtAnimationController> ANIM_CONTROLLERS =
            DeferredRegister.create(TmtRegistries.ANIM_CONTROLLERS_REGKEY, TooManyTinkers.MODID);

    public static final RegistryObject<IAnimatableTicTool3DBoneController> EMPTY_BONE_CONTROLLER =
            BONE_CONTROLLERS.register("empty", () -> EmptyTmtBoneController.INSTANCE);

    public static final RegistryObject<ITmtAnimationController> EMPTY_ANIM_CONTROLLER =
            ANIM_CONTROLLERS.register("empty", () -> EmptyTmtAnimationController.INSTANCE);

    public static void init(IEventBus bus) {
        BONE_CONTROLLERS.register(bus);
        ANIM_CONTROLLERS.register(bus);
    }
}
