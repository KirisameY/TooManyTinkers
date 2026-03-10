package com.kirisamey.toomanytinkers;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.NewRegistryEvent;
import net.minecraftforge.registries.RegistryBuilder;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = TooManyTinkers.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class TmtRegistries {
    public static final ResourceKey<Registry<Supplier<RenderType>>> RENDER_TYPE_GETTERS_REGKEY = key("render_type_getters");
    public static Supplier<IForgeRegistry<Supplier<RenderType>>> RENDER_TYPE_GETTERS;


    @SuppressWarnings("SameParameterValue")
    private static <T> ResourceKey<Registry<T>> key(String name) {
        return ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(TooManyTinkers.MODID, name));
    }

    @SubscribeEvent
    public static void onNewRegistry(NewRegistryEvent event) {
        RegistryBuilder<Supplier<RenderType>> builder = new RegistryBuilder<Supplier<RenderType>>()
                .setName(RENDER_TYPE_GETTERS_REGKEY.location())
                .setMaxID(Integer.MAX_VALUE - 1);
        //.allowModification();
        RENDER_TYPE_GETTERS = event.create(builder);
    }
}
