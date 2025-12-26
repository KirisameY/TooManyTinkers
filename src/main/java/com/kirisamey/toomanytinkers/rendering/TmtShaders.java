package com.kirisamey.toomanytinkers.rendering;

import com.kirisamey.toomanytinkers.TooManyTinkers;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;

@Mod.EventBusSubscriber(modid = TooManyTinkers.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class TmtShaders {

    public static ShaderInstance TinkerMappingShader;

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(new ShaderInstance(
                            event.getResourceProvider(),
                            ResourceLocation.fromNamespaceAndPath(TooManyTinkers.MODID, "tinker_map"),
                            DefaultVertexFormat.NEW_ENTITY), // 物品渲染通常使用 NEW_ENTITY 格式
                    shaderInstance -> TinkerMappingShader = shaderInstance
            );
        } catch (IOException e) {
            throw new RuntimeException("Could not load shader 'tinker_map'", e);
        }
    }
}
