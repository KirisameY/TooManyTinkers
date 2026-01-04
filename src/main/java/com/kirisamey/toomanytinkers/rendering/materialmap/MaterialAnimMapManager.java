package com.kirisamey.toomanytinkers.rendering.materialmap;

import com.ibm.icu.impl.Pair;
import com.kirisamey.toomanytinkers.TooManyTinkers;
import com.kirisamey.toomanytinkers.lib.ListMap;
import com.kirisamey.toomanytinkers.rendering.materialmap.events.MaterialAnimFrameUpdatedEvent;
import com.kirisamey.toomanytinkers.utils.TmtColorUtils;
import com.kirisamey.toomanytinkers.utils.TmtLookupUtils;
import com.mojang.logging.LogUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jline.utils.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MaterialAnimMapManager {

    private static final ListMap<ResourceLocation> ANIM_MAP = new ListMap<>();
    private static final List<AnimInfo> ANIM_INFOS = new ArrayList<>();


    // <editor-fold desc="Map">

    static void clear() {
        ANIM_MAP.clear();
        ANIM_INFOS.clear();
    }

    static Optional<AnimMatWriter> startAddMat(ResourceLocation materialId, ResourceLocation meta) {
        var metaData = getAnimMatInfo(meta);
        if (metaData.isEmpty()) return Optional.empty();
        var animId = ANIM_MAP.tryAdd(materialId);
        if (animId < 0) return Optional.empty();
        var animInfo = new AnimInfo(animId, metaData.get());
        ANIM_INFOS.add(animInfo);
        return metaData.map(section -> new AnimMatWriter(animInfo.frames));
    }

    private static Optional<AnimationMetadataSection> getAnimMatInfo(ResourceLocation meta) {
        var resourceManager = Minecraft.getInstance().getResourceManager();
        var res = resourceManager.getResource(meta);
        if (res.isEmpty()) {
            LogUtils.getLogger().error("Failed to get meta {}: did not found", meta);
            return Optional.empty();
        }
        try {
            return res.get().metadata().getSection(AnimationMetadataSection.SERIALIZER);
        } catch (IOException | ClassCastException e) {
            throw new RuntimeException(e);
        }
    }

    static int tryGetAnimIndex(ResourceLocation material) {
        return ANIM_MAP.getIndex(material);

    }

    // </editor-fold>


    // <editor-fold desc="Update">

    private static void update() {
        ANIM_INFOS.forEach(AnimInfo::update);
    }

    // </editor-fold>


    // <editor-fold desc="Event">

    @Mod.EventBusSubscriber(modid = TooManyTinkers.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class EventSubscriber {
        @SubscribeEvent
        public static void OnClientTick(TickEvent.ClientTickEvent e) {
            update();
        }
    }

    // </editor-fold>


    // <editor-fold desc="SubClass">

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class AnimMatWriter {
        private final List<Pair<Boolean, Integer>> list;

        public void addFrame(boolean is3D, int index) {
            list.add(Pair.of(is3D, index));
        }
    }

    @RequiredArgsConstructor
    private static class AnimInfo {
        private final int id;
        private final List<Pair<Boolean, Integer>> frames = new ArrayList<>();
        private final AnimationMetadataSection meta;

        private int currentFrame = 0;
        private int currentFrameTime = 0;

        void update() {

            currentFrameTime++;

            Pair<Boolean, Integer> newFrameInfo;
            var defaultFrameTime = meta.getDefaultFrameTime();

            if (meta.frames.isEmpty()) {
                if (currentFrameTime < defaultFrameTime) return;
                currentFrameTime = 0;
                currentFrame = (currentFrame + 1) % frames.size();
                newFrameInfo = frames.get(currentFrame);
            } else {
                var frame = meta.frames.get(currentFrame);
                int frameTime = frame.getTime(defaultFrameTime);
                if (currentFrameTime < frameTime) return;
                currentFrameTime = 0;
                currentFrame = (currentFrame + 1) % meta.frames.size();

                var newFrame = meta.frames.get(currentFrame);
                var newFrameIndex = newFrame.getIndex();
                if (frames.size() < newFrameIndex) {
                    LogUtils.getLogger().error("Anim Material {} have only {} frames, but frame {} required by meta",
                            ANIM_MAP.getValue(id), frames.size(), newFrameIndex);
                    MinecraftForge.EVENT_BUS.post(new MaterialAnimFrameUpdatedEvent(id, currentFrame, 0xffffffff, 0xffffffff));
                    return;
                }
                newFrameInfo = frames.get(newFrameIndex);
            }

            var is3d = newFrameInfo.first;
            var index = is3d ? newFrameInfo.second + MaterialMapsManager.getUnitsFor1D() : newFrameInfo.second;
            var color = TmtLookupUtils.getVertexColor(index, is3d, false);
            color = TmtColorUtils.Argb2Abgr(color);
            var largeColor = TmtLookupUtils.getVertexColor(index, is3d, true);
            largeColor = TmtColorUtils.Argb2Abgr(largeColor);
            MinecraftForge.EVENT_BUS.post(new MaterialAnimFrameUpdatedEvent(id, currentFrame, color, largeColor));
        }
    }

    // </editor-fold>
}
