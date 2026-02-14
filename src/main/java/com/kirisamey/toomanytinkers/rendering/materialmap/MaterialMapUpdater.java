package com.kirisamey.toomanytinkers.rendering.materialmap;

import com.kirisamey.toomanytinkers.TooManyTinkers;
import com.kirisamey.toomanytinkers.rendering.materialmap.events.MaterialMappingRestartEvent;
import com.kirisamey.toomanytinkers.rendering.materialmap.events.MaterialMappingUpdatedEvent;
import com.mojang.logging.LogUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class MaterialMapUpdater extends SimplePreparableReloadListener<MaterialMapUpdater.MaterialMapUpdatingContext> {
    @Override
    protected @NotNull MaterialMapUpdatingContext prepare(@NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        // fire the event
        LogUtils.getLogger().debug("Fire the MaterialMappingRestartEvent");
        MinecraftForge.EVENT_BUS.post(new MaterialMappingRestartEvent());

        var matInfos = MaterialMapsManager.remapData(resourceManager, profilerFiller);
        return new MaterialMapUpdatingContext(
                matInfos
        );
    }

    @Override
    protected void apply(@NotNull MaterialMapUpdatingContext ctx, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        var materialInfos = ctx.matInfos();
        MaterialMapsManager.addMatsMap(materialInfos, resourceManager, profilerFiller);
        MaterialMapTextureManager.remapTexture(materialInfos, resourceManager, profilerFiller);

        // fire the event
        LogUtils.getLogger().debug("Fire the MaterialMappingUpdatedEvent");
        MinecraftForge.EVENT_BUS.post(new MaterialMappingUpdatedEvent());
    }

    @Override public @NotNull String getName() {
        return "TMT Material Palette Update Listener";
    }


    @Mod.EventBusSubscriber(modid = TooManyTinkers.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEventSubscriber {
        @SubscribeEvent
        public static void onRegisterReloadListeners(@NotNull RegisterClientReloadListenersEvent event) {
            event.registerReloadListener(new MaterialMapUpdater());
        }
    }

    @Mod.EventBusSubscriber(modid = TooManyTinkers.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class MaterialMapTextureInitializedResender {
        @SubscribeEvent()
        public static void onClientTick(TickEvent.ClientTickEvent e) {
            MinecraftForge.EVENT_BUS.unregister(MaterialMapTextureInitializedResender.class);
            // 补发第一次事件
            LogUtils.getLogger().debug("Refire the first MaterialMappingUpdatedEvent");
            MinecraftForge.EVENT_BUS.post(new MaterialMappingUpdatedEvent());
        }
    }


    // <editor-fold desc="Data Class">

    public record MaterialMapUpdatingContext(
            List<MatInfo> matInfos
    ) {
    }

    public static class MatInfo {
        @AllArgsConstructor
        public static class M1D extends MatInfo {
            @Getter private final ResourceLocation location;
            @Getter private final List<ColorMap> colorMaps;

            public record ColorMap(int color, int grey) {
            }
        }

        @AllArgsConstructor
        public static class M3D extends MatInfo {
            @Getter private final ResourceLocation location;
            @Getter private final List<SpriteMap> spriteMaps;
            @Getter private final int frame;

            public record SpriteMap(int color, Optional<ResourceLocation> texture, int grey) {
            }
        }

        @AllArgsConstructor
        public static class M4D extends MatInfo {
            @Getter private final ResourceLocation location;
            @Getter private final ResourceLocation meta;
            @Getter private final List<MatInfo> frames;
        }
    }

    // </editor-fold>
}
