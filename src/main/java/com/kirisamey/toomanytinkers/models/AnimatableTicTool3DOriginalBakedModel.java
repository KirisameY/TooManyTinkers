package com.kirisamey.toomanytinkers.models;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class AnimatableTicTool3DOriginalBakedModel implements BakedModel {

    @Getter private final AnimatableTicTool3DModelData.BakedBone skeleton;
    @Getter private final ItemTransforms transforms;
    @Getter private final boolean largeTex;


    private final AnimatableTicTool3DModelOverrides overrides = new AnimatableTicTool3DModelOverrides(this);

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
        return overrides;
    }


    @Override
    public @NotNull BakedModel applyTransform(@NotNull ItemDisplayContext transformType, @NotNull PoseStack poseStack, boolean applyLeftHandTransform) {
        return BakedModel.super.applyTransform(transformType, poseStack, applyLeftHandTransform);
    }

    @Override public @NotNull List<RenderType> getRenderTypes(@NotNull ItemStack itemStack, boolean fabulous) {
        return BakedModel.super.getRenderTypes(itemStack, fabulous);
    }

    @Override public @NotNull List<BakedModel> getRenderPasses(@NotNull ItemStack itemStack, boolean fabulous) {
        return BakedModel.super.getRenderPasses(itemStack, fabulous);
    }
}
