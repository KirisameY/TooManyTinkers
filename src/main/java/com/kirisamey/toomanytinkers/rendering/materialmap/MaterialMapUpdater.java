package com.kirisamey.toomanytinkers.rendering.materialmap;

import com.kirisamey.toomanytinkers.TooManyTinkers;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class MaterialMapUpdater extends SimplePreparableReloadListener<List<MaterialMapUpdater.MatInfo>> {
    @Override
    protected @NotNull List<MatInfo> prepare(@NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        return MaterialMapsManager.remapData(resourceManager, profilerFiller);
    }

    @Override
    protected void apply(@NotNull List<MatInfo> materialInfos, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        MaterialMapTextureManager.remapTexture(materialInfos, resourceManager, profilerFiller);
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


    // <editor-fold desc="Data Class">
    public static class MatInfo {
        @AllArgsConstructor
        public static class M1D extends MatInfo {
            @Getter private final ResourceLocation location;
            @Getter private final int index;
            @Getter private final List<ColorMap> colorMaps;

            public record ColorMap(int color, int grey) {
            }
        }

        @AllArgsConstructor
        public static class M3D extends MatInfo {
            @Getter private final ResourceLocation location;
            @Getter private final int index;
            @Getter private final List<SpriteMap> spriteMaps;
            @Getter private final int frame;

            public record SpriteMap(int color, Optional<ResourceLocation> texture, int grey) {
            }
        }
    }

    // </editor-fold>
}
