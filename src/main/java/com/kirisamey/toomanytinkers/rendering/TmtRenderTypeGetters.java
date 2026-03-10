package com.kirisamey.toomanytinkers.rendering;

import com.kirisamey.toomanytinkers.TmtRegistries;
import com.kirisamey.toomanytinkers.TooManyTinkers;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class TmtRenderTypeGetters {
    private static final DeferredRegister<Supplier<RenderType>> REGISTER =
            DeferredRegister.create(TmtRegistries.RENDER_TYPE_GETTERS_REGKEY, TooManyTinkers.MODID);

    public static final RegistryObject<Supplier<RenderType>> TINKER_MAPPING =
            REGISTER.register("tinker_mapping", () -> TmtRenderTypes::getTinkerMapping);

    public static void init(IEventBus bus) {
        REGISTER.register(bus);
    }
}
