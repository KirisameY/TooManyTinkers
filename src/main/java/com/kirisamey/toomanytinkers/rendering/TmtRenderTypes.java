package com.kirisamey.toomanytinkers.rendering;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
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
                        // 使用 Supplier 引用 Shader，这样即使 RenderType 先创建，Shader 后加载也没问题
                        .setShaderState(new RenderStateShard.ShaderStateShard(() -> TmtShaders.TinkerMappingShader))
                        // ... 设置你的 TextureState (绑定 Sampler1) ...
                        .setTextureState(new RenderStateShard.EmptyTextureStateShard(() -> {
                            // Setup logic
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
