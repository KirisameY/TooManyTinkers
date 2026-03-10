package com.kirisamey.toomanytinkers.models.rendering;

import com.kirisamey.toomanytinkers.models.AnimatableTicTool3DFinalBakedModel;
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
        var buffer = multiBufferSource.getBuffer(TmtRenderTypes.getTinkerMapping()); // todo: 允许更多RenderType
//        var buffer = multiBufferSource.getBuffer(RenderType.cutout());
        var rgbaColors = model.getToolPartRgbaColors();

        model.getParts().forEach(p -> {
            var matNo = p.toolPart();
            var rgba = matNo >= 0 && matNo < rgbaColors.length ? rgbaColors[matNo] : new Vector4f(1);
            var quads = p.model().getQuads(null, null, rd, ModelData.EMPTY, null);
            for (var quad : quads) {
                buffer.putBulkData(poseStack.last(), quad, rgba.x, rgba.y, rgba.z, rgba.w, packedLight, packedOverlay, false);
            }
        });
    }
}
