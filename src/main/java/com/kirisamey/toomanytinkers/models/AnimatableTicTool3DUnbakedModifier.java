package com.kirisamey.toomanytinkers.models;

import com.ibm.icu.impl.Pair;
import com.kirisamey.toomanytinkers.models.pose.IAnimatableTicTool3DBoneController;
import io.vavr.collection.Map;
import io.vavr.collection.Vector;
import io.vavr.control.Option;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;
import org.joml.Vector3f;
import slimeknights.tconstruct.library.modifiers.ModifierId;

import java.util.Objects;
import java.util.function.Function;

@SuppressWarnings("ClassCanBeRecord")
@RequiredArgsConstructor
public class AnimatableTicTool3DUnbakedModifier implements IUnbakedGeometry<AnimatableTicTool3DUnbakedModifier> {
    @Getter private final ModifierId id;
    @Getter private final int minLevel;

    @Getter private final Vector<AnimatableTicTool3DModelData.UnbakedPart> parts;
    @Getter private final Option<IAnimatableTicTool3DBoneController> controller;
    @Getter private final ItemTransforms transforms;
    @Getter private final Vector<AnimatableTicTool3DModelData.UnbakedBoneModifier> mods;
    @Getter private final Map<String, Vector3f> marks;

    @Override
    public BakedModel bake(
            IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter,
            ModelState modelState, ItemOverrides overrides, ResourceLocation modelLocation) {

        var halfBakedModParts = parts.map(p -> {
            var model = baker.bake(p.model(), modelState, spriteGetter);
            return Pair.of(p, Objects.requireNonNull(model));
        }).toMap(p -> p.first.id(), p -> p);

        return new AnimatableTicTool3DBakedModifier(
                id,
                minLevel,
                controller,
                transforms,
                mods.map(boneMod -> new AnimatableTicTool3DModelData.BakedBoneModifier(
                        boneMod.boneId(),
                        boneMod.newParts().map(partPair -> {
                            var partId = partPair.first;
                            var partOff = partPair.second;
                            var pm = halfBakedModParts.get(partId).getOrElseThrow(NullPointerException::new);
                            var unb = pm.first;
                            var model = pm.second;
                            var offset = partOff.sub(unb.origin(), new Vector3f());
                            return new AnimatableTicTool3DModelData.BakedPart(unb.id(), model, unb.renderType(), unb.toolPart(), offset);
                        }),
                        boneMod.removeParts()
                )),
                marks
        );
    }
}
