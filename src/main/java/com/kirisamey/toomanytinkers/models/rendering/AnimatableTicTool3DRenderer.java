package com.kirisamey.toomanytinkers.models.rendering;

import com.kirisamey.toomanytinkers.models.AnimatableTicTool3DFinalBakedModel;
import com.kirisamey.toomanytinkers.models.AnimatableTicTool3DModelData;
import com.kirisamey.toomanytinkers.rendering.TmtRenderTypeGetters;
import com.kirisamey.toomanytinkers.rendering.TmtRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector4f;
import slimeknights.tconstruct.library.tools.item.ModifiableItem;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import java.util.stream.Collector;
import java.util.stream.Collectors;

public class AnimatableTicTool3DRenderer extends BlockEntityWithoutLevelRenderer {
    public AnimatableTicTool3DRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet modelSet) {
        super(dispatcher, modelSet);
    }


    private final ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();


    @Override
    public void renderByItem(@NotNull ItemStack itemStack, @NotNull ItemDisplayContext itemDisplayContext,
                             @NotNull PoseStack poseStack, @NotNull MultiBufferSource multiBufferSource,
                             int packedLight, int packedOverlay) {

        if (!(itemStack.getItem() instanceof ModifiableItem)) {
            super.renderByItem(itemStack, itemDisplayContext, poseStack, multiBufferSource, packedLight, packedOverlay);
            return;
        }
        var tool = ToolStack.from(itemStack);
        var itemModel = itemRenderer.getModel(itemStack, null, null, 0);
        if (!(itemModel instanceof AnimatableTicTool3DFinalBakedModel model)) {
            super.renderByItem(itemStack, itemDisplayContext, poseStack, multiBufferSource, packedLight, packedOverlay);
            return;
        }

        var rd = RandomSource.create();

        var rgbaColors = model.getToolPartRgbaColors();

        // todo: 引入位姿调整
        var allParts = model.getSkeleton().enumBones().flatMap(AnimatableTicTool3DModelData.BakedBone::parts);
        var grouped = allParts.collect(Collectors.groupingBy(AnimatableTicTool3DModelData.BakedPart::renderType));
        grouped.forEach((rtGetter, partList) -> {
            var renderType = rtGetter.get();
            var buffer = multiBufferSource.getBuffer(renderType);
            var tinkerMapping = TmtRenderTypeGetters.TINKER_MAPPING.get();

            partList.forEach(p -> {
                var rgba = new Vector4f(1);
                if (p.renderType() == tinkerMapping) {
                    var matNo = p.toolPart();
                    if (matNo >= 0 && matNo < rgbaColors.length) rgba = rgbaColors[matNo];
                }
                var quads = p.model().getQuads(null, null, rd, ModelData.EMPTY, null);
                for (var quad : quads) {
                    buffer.putBulkData(poseStack.last(), quad, rgba.x, rgba.y, rgba.z, rgba.w, packedLight, packedOverlay, false);
                }
            });
        });
    }
}
