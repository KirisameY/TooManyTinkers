package com.kirisamey.toomanytinkers.models;

import com.ibm.icu.impl.Pair;
import com.kirisamey.toomanytinkers.TooManyTinkers;
import com.kirisamey.toomanytinkers.rendering.materialmap.events.MaterialAnimFrameUpdatedEvent;
import com.kirisamey.toomanytinkers.rendering.materialmap.events.MaterialMappingRestartEvent;
import com.kirisamey.toomanytinkers.utils.TmtColorUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class AnimatableTicTool3DFinalBakedModel implements BakedModel {

    public AnimatableTicTool3DFinalBakedModel(AnimatableTicTool3DModelData.BakedBone skeleton, Vector4f[] partArgbColors,
                                              List<Pair<Integer, Integer>> partAnimPairs, ItemTransforms transforms, boolean largeTex) {
        this.skeleton = skeleton;
        this.toolPartRgbaColors = partArgbColors;
        this.transforms = transforms;

        for (var pair : partAnimPairs) {
            while (ANIM_MAT_COLOR_UPDATE_LIST.size() <= pair.second) {
                ANIM_MAT_COLOR_UPDATE_LIST.add(new ArrayList<>());
            }
            ANIM_MAT_COLOR_UPDATE_LIST.get(pair.second).add(Pair.of(this, pair.first));
        }

        this.largeTex = largeTex;
    }

    private static final ArrayList<ArrayList<Pair<AnimatableTicTool3DFinalBakedModel, Integer>>> ANIM_MAT_COLOR_UPDATE_LIST = new ArrayList<>();

    @Getter private final AnimatableTicTool3DModelData.BakedBone skeleton;
    @Getter private final Vector4f[] toolPartRgbaColors;
    @Getter private final ItemTransforms transforms;
    @Getter private final boolean largeTex;

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState blockState, @Nullable Direction direction, @NotNull RandomSource randomSource) {
        //noinspection deprecation
        return skeleton.enumBones().toJavaStream().flatMap(
                b -> b.parts().toJavaStream()
        ).flatMap(
                p -> p.model().getQuads(blockState, direction, randomSource).stream()
        ).toList();
    }

    @Override public boolean useAmbientOcclusion() {
        return false;
    }

    @Override public boolean isGui3d() {
        return false;
    }

    @Override public boolean usesBlockLight() {
        return false;
    }

    @Override public boolean isCustomRenderer() {
        return true;
    }

    @Override public @NotNull TextureAtlasSprite getParticleIcon() {
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(MissingTextureAtlasSprite.getLocation());
    }

    @Override public @NotNull ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }


    @Override
    public @NotNull BakedModel applyTransform(@NotNull ItemDisplayContext transformType, @NotNull PoseStack poseStack, boolean applyLeftHandTransform) {
        var tr = transforms.getTransform(transformType);
        tr.apply(applyLeftHandTransform, poseStack);
        return this;
    }

    @Override public @NotNull List<RenderType> getRenderTypes(@NotNull ItemStack itemStack, boolean fabulous) {
        return BakedModel.super.getRenderTypes(itemStack, fabulous);
    }

    @Override public @NotNull List<BakedModel> getRenderPasses(@NotNull ItemStack itemStack, boolean fabulous) {
        return BakedModel.super.getRenderPasses(itemStack, fabulous);
    }


    @Mod.EventBusSubscriber(modid = TooManyTinkers.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class AnimationToolPartColorUpdater {
        @SubscribeEvent
        public static void OnMaterialMappingRestart(MaterialMappingRestartEvent e) {
            ANIM_MAT_COLOR_UPDATE_LIST.clear();
        }

        @SubscribeEvent
        public static void OnMaterialAnimFrameUpdated(MaterialAnimFrameUpdatedEvent e) {
            var id = e.getId();
            if (ANIM_MAT_COLOR_UPDATE_LIST.size() <= id) return;

            ANIM_MAT_COLOR_UPDATE_LIST.get(id).forEach(q -> {
                var isLarge = q.first.isLargeTex();
                var color = isLarge ? e.getLargeVertexColor() : e.getVertexColor();
                q.first.toolPartRgbaColors[q.second] = TmtColorUtils.Argb2RgbaF(color);
            });
        }
    }
}
