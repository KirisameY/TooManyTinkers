package com.kirisamey.toomanytinkers;

import com.kirisamey.toomanytinkers.models.pose.IAnimatableTicTool3DBoneController;
import com.kirisamey.toomanytinkers.models.pose.ITmtAnimationController;
import com.kirisamey.toomanytinkers.models.pose.TmtAnimationBoneController;
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

    public static final ResourceKey<Registry<IAnimatableTicTool3DBoneController>> BONE_CONTROLLERS_REGKEY = key("bone_controllers");
    public static Supplier<IForgeRegistry<IAnimatableTicTool3DBoneController>> BONE_CONTROLLERS;

    public static final ResourceKey<Registry<ITmtAnimationController>> ANIM_CONTROLLERS_REGKEY = key("anim_controllers");
    public static Supplier<IForgeRegistry<ITmtAnimationController>> ANIM_CONTROLLERS;

    @SuppressWarnings("SameParameterValue")
    private static <T> ResourceKey<Registry<T>> key(String name) {
        return ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(TooManyTinkers.MODID, name));
    }

    @SubscribeEvent
    public static void onNewRegistry(NewRegistryEvent event) {
        RENDER_TYPE_GETTERS = addRegistry(event, RENDER_TYPE_GETTERS_REGKEY);
        BONE_CONTROLLERS = addRegistry(event, BONE_CONTROLLERS_REGKEY);
        ANIM_CONTROLLERS = addRegistry(event, ANIM_CONTROLLERS_REGKEY);
    }

    private static <T> Supplier<IForgeRegistry<T>> addRegistry(NewRegistryEvent event, ResourceKey<Registry<T>> regkey) {
        RegistryBuilder<T> boneControllersBuilder = new RegistryBuilder<T>()
                .setName(regkey.location())
                .setMaxID(Integer.MAX_VALUE - 1);
        //.allowModification();
        return event.create(boneControllersBuilder);
    }
}
