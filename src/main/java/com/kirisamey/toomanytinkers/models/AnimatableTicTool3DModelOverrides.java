package com.kirisamey.toomanytinkers.models;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.ibm.icu.impl.Pair;
import com.kirisamey.toomanytinkers.rendering.materialmap.MaterialMapsManager;
import com.kirisamey.toomanytinkers.utils.TmtLookupUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4f;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.tools.item.ModifiableItem;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@Log4j2 @RequiredArgsConstructor
public class AnimatableTicTool3DModelOverrides extends ItemOverrides {

    private final AnimatableTicTool3DOriginalBakedModel original;

    private final Map<OverrideKey, AnimatableTicTool3DFinalBakedModel> cache = new HashMap<>();

    @Override
    public @Nullable BakedModel resolve(@NotNull BakedModel model, @NotNull ItemStack itemStack, @Nullable ClientLevel level,
                                        @Nullable LivingEntity livingEntity, int seed) {
        if (!(itemStack.getItem() instanceof ModifiableItem))
            return super.resolve(model, itemStack, level, livingEntity, seed);

        var tool = ToolStack.from(itemStack);
        var mats = tool.getMaterials().getList();
        var key = new OverrideKey(ImmutableList.copyOf(mats));
        var result = cache.get(key);
        if (result == null) {
            result = createNewModel(key, mats);
            cache.put(key, result);
        }
        return result;
    }

    private AnimatableTicTool3DFinalBakedModel createNewModel(OverrideKey key, List<MaterialVariant> mats) {
        List<Pair<Integer, Integer>> partAnimPairs = new ArrayList<>();
        AtomicInteger index = new AtomicInteger(0);
        var isLarge = original.isLargeTex();

        var argbColors = mats.stream().map(mv -> {
            var i = index.getAndAdd(1);
            var matId = mv.getVariant().getLocation('_');
            var info = MaterialMapsManager.getTexInfo(matId);

            Vector4f color = new Vector4f(1);
            if (info instanceof MaterialMapsManager.MatType.Mat1D m1d) {
                color = TmtLookupUtils.getVertexColorRgbaF(m1d.getId(), false, isLarge, true);
            } else if (info instanceof MaterialMapsManager.MatType.Mat3D m3d) {
                color = TmtLookupUtils.getVertexColorRgbaF(m3d.getId(), true, isLarge, true);
            } else if (info instanceof MaterialMapsManager.MatType.Mat4D m4d) {
                partAnimPairs.add(Pair.of(i, m4d.getAnim()));
            }

            return color;
        }).toArray(Vector4f[]::new);

        return new AnimatableTicTool3DFinalBakedModel(original.getSkeleton(), argbColors, partAnimPairs, original.getTransforms(), isLarge);
    }

    record OverrideKey(ImmutableList<MaterialVariant> partMats) {
        // todo: 加入词条
    }
}
