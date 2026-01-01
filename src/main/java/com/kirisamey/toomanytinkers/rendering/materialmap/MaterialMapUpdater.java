package com.kirisamey.toomanytinkers.rendering.materialmap;

import com.kirisamey.toomanytinkers.TooManyTinkers;
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

public class MaterialMapUpdater extends SimplePreparableReloadListener<MaterialMapUpdater.MaterialInfos> {
    @Override
    protected @NotNull MaterialInfos prepare(@NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        return MaterialMapsManager.remapData(resourceManager, profilerFiller);
    }

    @Override
    protected void apply(@NotNull MaterialInfos materialInfos, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
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

    public record Mat1DInfo(ResourceLocation location, int index, List<ColorMap> colorMaps) {
        public record ColorMap(int color, int grey) {
        }
    }

    public record Mat3DInfo(ResourceLocation location, int index, List<SpriteMap> spriteMaps, int frame) {
        public record SpriteMap(int color, Optional<ResourceLocation> texture, int grey) {
        }
    }

    public record MatInheritedInfo(ResourceLocation mat, ResourceLocation parent) {
    }

    public record MaterialInfos(List<Mat1DInfo> mat1DInfos, List<Mat3DInfo> mat3DInfos,
                                List<MatInheritedInfo> matInheritedInfos) {
    }

    // </editor-fold>
}
