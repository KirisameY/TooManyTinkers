package com.kirisamey.toomanytinkers;

import com.kirisamey.toomanytinkers.configs.TmtConfig;
import com.kirisamey.toomanytinkers.models.animating.TmtAnimation;
import com.kirisamey.toomanytinkers.models.pose.TmtAnimationControllers;
import com.kirisamey.toomanytinkers.rendering.TmtRenderTypeGetters;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import javax.swing.*;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(TooManyTinkers.MODID)
public class TooManyTinkers {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "toomanytinkers";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();


    public TooManyTinkers(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        context.registerConfig(ModConfig.Type.COMMON, TmtConfig.SPEC);

        TmtRenderTypeGetters.init(modEventBus);
        TmtAnimationControllers.init(modEventBus);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("Tooooooo Many Tinkers: Common Setup");

        if (ModList.get().isLoaded("oculus")) {
            JOptionPane.showMessageDialog(null,
                    "警告：Too many Tinkers 在加载了自定义光影包时可能出现兼容性问题。" +
                            "如果发现匠魂工具渲染异常，请考虑在配置中禁用 Too many Tinkers 的原版注入功能并重启游戏。\n" +
                            "Warning: Too many Tinkers may experience compatibility issues " +
                            "when a custom shader pack is loaded." +
                            "If you find that the TiC tools is rendering incorrectly, " +
                            "please consider disabling vanilla injection of Too many Tinkers in configs " +
                            "and restart game.",
                    "Warning",
                    JOptionPane.WARNING_MESSAGE);
        }
    }
}
