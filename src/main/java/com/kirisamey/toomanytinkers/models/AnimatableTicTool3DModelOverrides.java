package com.kirisamey.toomanytinkers.models;

import com.google.common.collect.ImmutableMap;
import com.ibm.icu.impl.Pair;
import com.kirisamey.toomanytinkers.rendering.materialmap.MaterialMapsManager;
import com.kirisamey.toomanytinkers.utils.TmtLookupUtils;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import io.vavr.collection.Map;
import io.vavr.collection.Stream;
import io.vavr.collection.Vector;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector4f;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.item.ModifiableItem;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;

@Log4j2 @RequiredArgsConstructor
public class AnimatableTicTool3DModelOverrides extends ItemOverrides {

    private final AnimatableTicTool3DOriginalBakedModel original;

    private final HashMap<OverrideKey, AnimatableTicTool3DFinalBakedModel> cache = new HashMap<>();

    @Override
    public @Nullable BakedModel resolve(@NotNull BakedModel model, @NotNull ItemStack itemStack, @Nullable ClientLevel level,
                                        @Nullable LivingEntity livingEntity, int seed) {
        if (!(itemStack.getItem() instanceof ModifiableItem))
            return super.resolve(model, itemStack, level, livingEntity, seed);

        var tool = ToolStack.from(itemStack);
        var mats = Vector.ofAll(tool.getMaterials());
        var mods = Vector.ofAll(tool.getModifiers()); // todo
        var key = new OverrideKey(mats, mods);
        return findOrCreateModel(key);
    }

    private AnimatableTicTool3DFinalBakedModel findOrCreateModel(OverrideKey key) {
        var result = cache.get(key);
        if (result == null) {

            var modT = key.modifiers.map(entry -> {
                var modStream = original.getAvailableMods().toStream()
                        .filter(mod -> entry.matches(mod.getId()) &&
                                entry.getLevel() >= mod.getMinLevel())
                        .sortBy(AnimatableTicTool3DBakedModifier::getMinLevel);
                return modStream.map(mod -> Tuple.of(new ModifierEntry(mod.getId(), mod.getMinLevel()), mod));
            }).flatMap(s -> s).unzip(t -> t);

            var subKey = new OverrideKey(key.partMats, modT._1);
            result = cache.get(subKey);
            if (result == null) {

                result = createNewModel(key.partMats, modT._2);
                cache.put(subKey, result);
            }

            cache.put(key, result);
        }
        return result;
    }

    private AnimatableTicTool3DFinalBakedModel createNewModel(
            Vector<MaterialVariant> mats, Vector<AnimatableTicTool3DBakedModifier> mods) {

        ArrayList<Pair<Integer, Integer>> partAnimPairs = new ArrayList<>();
        AtomicInteger index = new AtomicInteger(0);
        var isLarge = original.isLargeTex();

        var argbColors = mats.map(mv -> {
            var i = index.getAndAdd(1);
            var matId = mv.getVariant().getLocation('_');
            var info = MaterialMapsManager.getTexInfo(matId);

            Vector4f color = new Vector4f(1);
            if (info instanceof MaterialMapsManager.MatType.Mat1D m1d) {
                color = TmtLookupUtils.getVertexColorRgbaF(m1d.getId(), false, isLarge, true);
                log.debug("got vertex color for mat[{}] (1d): {}", i, matId);
            } else if (info instanceof MaterialMapsManager.MatType.Mat3D m3d) {
                color = TmtLookupUtils.getVertexColorRgbaF(m3d.getId(), true, isLarge, true);
                log.debug("got vertex color for mat[{}] (3d): {}", i, matId);
            } else if (info instanceof MaterialMapsManager.MatType.Mat4D m4d) {
                partAnimPairs.add(Pair.of(i, m4d.getAnim()));
                log.debug("got vertex color for mat[{}] (4d): {}", i, matId);
            } else {
                log.warn("failed got vertex color for mat[{}]: {}, got {}", i, matId, info);
            }

            return color;
        }).toJavaArray(Vector4f[]::new);


        Stack<Tuple2<
                ArrayList<AnimatableTicTool3DModelData.BakedBone>,
                AnimatableTicTool3DModelData.BakedBone
                >> pushStack = new Stack<>();
        Stack<Tuple3<
                ArrayList<AnimatableTicTool3DModelData.BakedBone>,
                AnimatableTicTool3DModelData.BakedBone,
                ArrayList<AnimatableTicTool3DModelData.BakedBone>
                >> bakeStack = new Stack<>();

        ArrayList<AnimatableTicTool3DModelData.BakedBone> finalList = new ArrayList<>();
        pushStack.push(Tuple.of(finalList, original.getSkeleton()));
        //noinspection DuplicatedCode
        while (!pushStack.empty()) {
            var tuple = pushStack.pop();
            var parentList = tuple._1;
            var unmodded = tuple._2;
            var selfList = new ArrayList<AnimatableTicTool3DModelData.BakedBone>();
            bakeStack.push(Tuple.of(parentList, unmodded, selfList));
            unmodded.bones().forEach(b -> pushStack.push(Tuple.of(selfList, b)));
        }
        while (!bakeStack.empty()) {
            var tuple = bakeStack.pop();
            var parentList = tuple._1;
            var unmodded = tuple._2;
            var selfList = tuple._3;
            var moddedParts = mods.toStream()
                    .flatMap(AnimatableTicTool3DBakedModifier::getMods)
                    .filter(m -> m.boneId().equals(unmodded.id()))
                    .foldLeft(unmodded.parts().toStream(), (partsAcc, boneMod) -> {
                        return Stream.concat(
                                partsAcc.toStream().filter(part -> !boneMod.removeParts().contains(part.id())),
                                boneMod.newParts()
                        ).distinctByKeepLast(AnimatableTicTool3DModelData.BakedPart::id);
                    }).toVector();
            var newBone = new AnimatableTicTool3DModelData.BakedBone(unmodded.id(), moddedParts, Vector.ofAll(selfList));
            parentList.add(newBone);
        }
        var skeleton = finalList.get(0);

        var controller = mods.lastOption().flatMap(AnimatableTicTool3DBakedModifier::getController).getOrElse(original.getController());

        var transforms = mods.foldLeft(original.getTransforms(), (transAcc, mod) -> {
            var transMod = mod.getTransforms();
            if (transMod == ItemTransforms.NO_TRANSFORMS) return transAcc;

            var tpL = transAcc.thirdPersonLeftHand;
            var tpR = transAcc.thirdPersonRightHand;
            var fpL = transAcc.firstPersonLeftHand;
            var fpR = transAcc.firstPersonRightHand;
            var head = transAcc.head;
            var gui = transAcc.gui;
            var gnd = transAcc.ground;
            var fixed = transAcc.fixed;
            var modded = transMod.moddedTransforms;

            if (transMod.thirdPersonLeftHand != ItemTransform.NO_TRANSFORM) tpL = transMod.thirdPersonLeftHand;
            if (transMod.thirdPersonRightHand != ItemTransform.NO_TRANSFORM) tpR = transMod.thirdPersonRightHand;
            if (transMod.firstPersonLeftHand != ItemTransform.NO_TRANSFORM) fpL = transMod.firstPersonLeftHand;
            if (transMod.firstPersonRightHand != ItemTransform.NO_TRANSFORM) fpR = transMod.firstPersonRightHand;
            if (transMod.head != ItemTransform.NO_TRANSFORM) head = transMod.head;
            if (transMod.gui != ItemTransform.NO_TRANSFORM) gui = transMod.gui;
            if (transMod.ground != ItemTransform.NO_TRANSFORM) gnd = transMod.ground;
            if (transMod.fixed != ItemTransform.NO_TRANSFORM) fixed = transMod.fixed;
            if (!transAcc.moddedTransforms.isEmpty()) {
                modded = ImmutableMap.copyOf(io.vavr.collection.HashMap.ofAll(transAcc.moddedTransforms)
                        .merge(io.vavr.collection.HashMap.ofAll(modded))
                        .toJavaMap()
                );
            }

            return new ItemTransforms(tpL, tpR, fpL, fpR, head, gui, gnd, fixed, modded);
        });

        var marks = mods.foldLeft(original.getMarks(), (marksAcc, mod) -> {
            return mod.getMarks().merge(marksAcc);
        });

        return new AnimatableTicTool3DFinalBakedModel(
                skeleton, controller, argbColors, partAnimPairs, transforms, isLarge, marks
        );
    }

    private record OverrideKey(Vector<MaterialVariant> partMats, Vector<ModifierEntry> modifiers) {
    }
}
