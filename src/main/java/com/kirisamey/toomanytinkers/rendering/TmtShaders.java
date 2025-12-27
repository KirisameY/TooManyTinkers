package com.kirisamey.toomanytinkers.rendering;

import com.kirisamey.toomanytinkers.TooManyTinkers;
import com.kirisamey.toomanytinkers.rendering.events.MaterialMapTextureUpdatedEvent;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.logging.LogUtils;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector2f;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.Objects;

public class TmtShaders {

    // <editor-fold desc="TinkerMapping">

    private static ShaderInstance tinkerMappingShader;

    @Mod.EventBusSubscriber(modid = TooManyTinkers.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class TinkerMappingUniforms {
        static float atlasWidth;
        static float atlasHeight;

        @SubscribeEvent
        public static void updateUniforms(MaterialMapTextureUpdatedEvent e) {
            LogUtils.getLogger().debug("Now Update Uniform for TinkerMappingShader");

            var atlasTex = Minecraft.getInstance().getTextureManager().getTexture(InventoryMenu.BLOCK_ATLAS);

            // 获取纹理尺寸
            if (atlasTex instanceof TextureAtlas tex) {
                LogUtils.getLogger().debug("Seems like that is a TextureAtlas, now doing...");
                atlasWidth = tex.getWidth();
                atlasHeight = tex.getHeight();
            }

//            RenderSystem.recordRenderCall(() -> {
//                RenderSystem.bindTexture(atlasTex.getId());
//                int w = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
//                int h = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
//                atlasWidth = (float) w;
//                atlasHeight = (float) h;
//            });

            LogUtils.getLogger().debug("Set AtlasSize: ({}, {})", atlasWidth, atlasHeight);
        }
    }

    public static ShaderInstance setUpTinkerMappingShader() {
        Objects.requireNonNull(tinkerMappingShader.getUniform("AtlasSize"))
                .set(TinkerMappingUniforms.atlasWidth, TinkerMappingUniforms.atlasHeight);
        // LogUtils.getLogger().debug("Set AtlasSize: ({}, {})", TinkerMappingUniforms.atlasWidth, TinkerMappingUniforms.atlasHeight);
        return tinkerMappingShader;
    }


    // </editor-fold>

    @Mod.EventBusSubscriber(modid = TooManyTinkers.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ShadersRegister {
        @SubscribeEvent
        public static void registerShaders(RegisterShadersEvent event) {
            try {
                event.registerShader(new ShaderInstance(
                                event.getResourceProvider(),
                                ResourceLocation.fromNamespaceAndPath(TooManyTinkers.MODID, "tinker_map"),
                                DefaultVertexFormat.NEW_ENTITY), // 物品渲染通常使用 NEW_ENTITY 格式
                        shaderInstance -> tinkerMappingShader = shaderInstance
                );
            } catch (IOException e) {
                throw new RuntimeException("Could not load shader 'tinker_map'", e);
            }
        }
    }

}

