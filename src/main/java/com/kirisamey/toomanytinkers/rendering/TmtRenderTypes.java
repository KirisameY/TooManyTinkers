package com.kirisamey.toomanytinkers.rendering;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.client.RenderTypeGroup;

public class TmtRenderTypes extends RenderType {

    public TmtRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                          boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    private static RenderType TinkerMapping;
    private static RenderTypeGroup TinkerMappingGroup;

    public static RenderType getTinkerMapping() {
        if (TinkerMapping == null) TinkerMapping = create(
                "tinker_map",
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                256,
                true,
                true,
                RenderType.CompositeState.builder()
                        .setShaderState(new RenderStateShard.ShaderStateShard(() -> TmtShaders.TinkerMappingShader))
                        .setTextureState(new RenderStateShard.TextureStateShard(
                                InventoryMenu.BLOCK_ATLAS,
                                false, false
                        ))
                        // binding sampler
                        .setTextureState(new RenderStateShard.EmptyTextureStateShard(() -> {
                            // Setup logic
                            RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
                            RenderSystem.bindTexture(RenderSystem.getShaderTexture(0));

                            RenderSystem.activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE1);
                            ResourceLocation mapTexId = MaterialPaletteManager.MAT_TEX_ID;
                            RenderSystem.setShaderTexture(1, mapTexId);
                            RenderSystem.bindTexture(RenderSystem.getShaderTexture(1));

                            RenderSystem.activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);

                        }, () -> {
                            // Clear logic
                        }))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .setLightmapState(LIGHTMAP)
                        .setOverlayState(OVERLAY)
                        .createCompositeState(true)
        );
        return TinkerMapping;
    }

    public static RenderTypeGroup getTinkerMappingGroup() {
        if (TinkerMappingGroup == null)
            TinkerMappingGroup = new RenderTypeGroup(RenderType.translucent(), getTinkerMapping());
        return TinkerMappingGroup;
    }
}
