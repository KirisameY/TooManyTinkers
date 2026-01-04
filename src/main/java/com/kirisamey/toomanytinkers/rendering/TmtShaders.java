package com.kirisamey.toomanytinkers.rendering;

import com.kirisamey.toomanytinkers.TooManyTinkers;
import com.kirisamey.toomanytinkers.rendering.materialmap.events.MaterialMappingUpdatedEvent;
import com.kirisamey.toomanytinkers.rendering.materialmap.MaterialMapTextureManager;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;

public class TmtShaders {

    // <editor-fold desc="TinkerMapping">

    private static ShaderInstance tinkerMappingShader;

    @Mod.EventBusSubscriber(modid = TooManyTinkers.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class TinkerMappingUniforms {
        static float atlasWidth;
        static float atlasHeight;

        static float mapWidth;
        static float mapHeight;

        @SubscribeEvent
        public static void updateUniforms(MaterialMappingUpdatedEvent e) {
            LogUtils.getLogger().debug("Now Update Uniform for TinkerMappingShader");

            var atlasTex = Minecraft.getInstance().getTextureManager().getTexture(InventoryMenu.BLOCK_ATLAS);

            // 获取纹理尺寸
            if (atlasTex instanceof TextureAtlas tex) {
                LogUtils.getLogger().debug("Seems like that BLOCK_ATLAS is a TextureAtlas, now doing...");
                atlasWidth = tex.getWidth();
                atlasHeight = tex.getHeight();
            } else {
                LogUtils.getLogger().error("Seems like that BLOCK_ATLAS is not a TextureAtlas?! but, why?");
                LogUtils.getLogger().error("This should not happened! Please contact KirisameY");
            }
            LogUtils.getLogger().debug("Set AtlasSize: ({}, {})", atlasWidth, atlasHeight);

            // 获取映射尺寸
            mapWidth = MaterialMapTextureManager.getTexWidth() * MaterialMapTextureManager.TEX_UNIT;
            mapHeight = MaterialMapTextureManager.getTexHeigh() * MaterialMapTextureManager.TEX_UNIT;
            LogUtils.getLogger().debug("Set MapSize: ({}, {})", mapWidth, mapHeight);
        }
    }

    public static ShaderInstance setUpTinkerMappingShader() {
        var atlasSize = tinkerMappingShader.getUniform("AtlasSize");
        if (atlasSize != null) atlasSize.set(TinkerMappingUniforms.atlasWidth, TinkerMappingUniforms.atlasHeight);
        var mapSize = tinkerMappingShader.getUniform("MapSize");
        if (mapSize != null) mapSize.set(TinkerMappingUniforms.mapWidth, TinkerMappingUniforms.mapHeight);
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

