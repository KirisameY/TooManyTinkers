package com.kirisamey.toomanytinkers.models;

import com.kirisamey.toomanytinkers.models.pose.IAnimatableTicTool3DBoneController;
import io.vavr.collection.Map;
import io.vavr.collection.Vector;
import io.vavr.control.Option;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import slimeknights.tconstruct.library.modifiers.ModifierId;

import java.util.List;

@SuppressWarnings("ClassCanBeRecord")
@RequiredArgsConstructor
public class AnimatableTicTool3DBakedModifier implements BakedModel {
    @Getter private final ModifierId id;
    @Getter private final int minLevel;

    @Getter private final Option<IAnimatableTicTool3DBoneController> controller;
    @Getter private final ItemTransforms transforms;
    @Getter private final Vector<AnimatableTicTool3DModelData.BakedBoneModifier> mods;
    @Getter private final Map<String, Vector3f> marks;


    @Override
    public @NonNull List<BakedQuad> getQuads(@Nullable BlockState p_235039_, @Nullable Direction p_235040_, @NonNull RandomSource p_235041_) {
        return List.of();
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
        return false;
    }

    @Override public @NonNull TextureAtlasSprite getParticleIcon() {
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(MissingTextureAtlasSprite.getLocation());
    }

    @Override public @NonNull ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }
}
