package com.kirisamey.toomanytinkers.rendering.materialmap;

import com.kirisamey.toomanytinkers.TooManyTinkers;
import com.kirisamey.toomanytinkers.rendering.materialmap.events.MaterialMapTextureUpdatedEvent;
import com.kirisamey.toomanytinkers.utils.TmtColorUtils;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.FastColor;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;

import static com.kirisamey.toomanytinkers.rendering.materialmap.MaterialMapUpdater.*;

public class MaterialMapTextureManager {

    // <editor-fold desc="texture">

    public static final int TEX_UNIT = 256;
    public static final ResourceLocation MAT_TEX_ID = ResourceLocation.fromNamespaceAndPath(TooManyTinkers.MODID, "dynamic/mat_map");
    @Getter @Setter(AccessLevel.PACKAGE) private static int texWidth = 1;
    @Getter @Setter(AccessLevel.PACKAGE) private static int texHeigh = 1;
    private static DynamicTexture mapTex;

    // </editor-fold>


    // <editor-fold desc="Update texture">

    static void remapTexture(@NotNull List<MatInfo> materialInfos,
                             @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {

        // initialize buffer
        var buffer = new NativeImage(texWidth * TEX_UNIT, texHeigh * TEX_UNIT, true);
        LogUtils.getLogger().info("MatMap texture initialized with size {}, {} ({}, {})",
                texWidth, texHeigh, texWidth * TEX_UNIT, texHeigh * TEX_UNIT);

        for (var mat : materialInfos) {
            if /* those for 1D */ (mat instanceof MatInfo.M1D mat1) {
                // calculate position
                var index = mat1.getIndex();
                var row = index % (texHeigh * TEX_UNIT);
                var col = index / (texHeigh * TEX_UNIT);

                var first = mat1.getColorMaps().get(0);
                MatInfo.M1D.ColorMap lastColor = first.grey() <= 0 ? null : new MatInfo.M1D.ColorMap(first.color(), 0);
                for (var colorMap : mat1.getColorMaps()) {
                    final var last = lastColor;
                    if (lastColor != null) {
                        var d = colorMap.grey() - lastColor.grey();
                        if (d <= 0) continue;
                        IntStream.rangeClosed(lastColor.grey(), colorMap.grey()).forEach(grey -> {
                            if (grey < 0 || grey > 255) return;
                            //noinspection UnnecessaryLocalVariable
                            var y = row;
                            var x = col * TEX_UNIT + grey;
                            var lerp = ((float) (grey - last.grey())) / d;
                            var color = FastColor.ARGB32.lerp(lerp, last.color(), colorMap.color());
                            buffer.setPixelRGBA(x, y, TmtColorUtils.Argb2Abgr(color));
                        });
                    }
                    lastColor = colorMap;
                }

                LogUtils.getLogger().info("1D Material {} mapped successfully as 1D no.{}", mat1.getLocation(), index);
            } else if /* those for 3D */ (mat instanceof MatInfo.M3D mat3) {
                // calculate position
                var index = mat3.getIndex() + MaterialMapsManager.getUnitsFor1D();
                var gridY = (index % texHeigh) * TEX_UNIT;
                var gridX = (index / texHeigh) * TEX_UNIT;

                var first = mat3.getSpriteMaps().get(0);
                MatInfo.M3D.SpriteMap lastSprite = first.grey() <= 0 ? null :
                        new MatInfo.M3D.SpriteMap(first.color(), first.texture(), 0);
                for (var sprMap : mat3.getSpriteMaps()) {
                    final var last = lastSprite;
                    if (lastSprite != null) {
                        var d = sprMap.grey() - lastSprite.grey();
                        if (d <= 0) continue;

                        final Function<Resource, Optional<NativeImage>> openImg = res -> {
                            try (var str = res.open()) {
                                return Optional.of(NativeImage.read(str));
                            } catch (IOException e) {
                                return Optional.empty();
                            }
                        };

                        var texLst = lastSprite.texture().flatMap(resourceManager::getResource).flatMap(openImg);
                        var texNew = sprMap.texture().flatMap(resourceManager::getResource).flatMap(openImg);

                        IntStream.range(0, 256).forEach(i -> {
                            var innerY = i % 16;
                            var innerX = i / 16;

                            final var frame = mat3.getFrame();
                            final Function<NativeImage, Integer> readColor = img -> {
                                var width = img.getWidth();
                                var height = img.getHeight();
                                var texX = innerX % width;
                                var texY = innerY;
                                if (frame < 0) {
                                    texY %= height;
                                } else {
                                    texY = ((texY % width) + (frame * width)) % height;
                                }
                                return img.getPixelRGBA(texX, texY);
                            };

                            int colorLst = texLst.map(readColor).orElse(0xffffffff);
                            int colorNew = texNew.map(readColor).orElse(0xffffffff);
                            var colorLstF = FastColor.ARGB32.multiply(colorLst, TmtColorUtils.Argb2Abgr(last.color()));
                            var colorNewF = FastColor.ARGB32.multiply(colorNew, TmtColorUtils.Argb2Abgr(sprMap.color()));

                            IntStream.rangeClosed(last.grey(), sprMap.grey()).forEach(grey -> {
                                if (grey < 0 || grey > 255) return;

                                var lerp = ((float) (grey - last.grey())) / d;
                                var color = FastColor.ARGB32.lerp(lerp, colorLstF, colorNewF);

                                var frameY = gridY + (grey % 16) * 16 + innerY;
                                var frameX = gridX + (grey / 16) * 16 + innerX;

                                buffer.setPixelRGBA(frameX, frameY, color);
                            });
                        });

                        texLst.ifPresent(NativeImage::close);
                        texNew.ifPresent(NativeImage::close);
                    }
                    lastSprite = sprMap;
                }

                LogUtils.getLogger().info("3D Material {} mapped successfully as 3D no.{}", mat3.getLocation(), index);
            }
        }

        if (mapTex != null) mapTex.close();
        mapTex = new DynamicTexture(buffer);
        Minecraft.getInstance().getTextureManager().release(MAT_TEX_ID);
        Minecraft.getInstance().getTextureManager().register(MAT_TEX_ID, mapTex);
        mapTex.upload();
        // mojang 扫码了，不换tex只换掉image的话根本没法upload到gpu，我觉得我不得不创建锌纹理然后重新绑定了，妈的不早说

        {
            // for test
            var runDir = Minecraft.getInstance().gameDirectory;
            var testDir = new File(runDir, "debug");
            if (testDir.exists() || testDir.mkdirs()) {
                var outFile = new File(testDir, "tmt_map.png");
                try {
                    if (mapTex.getPixels() != null) {
                        mapTex.getPixels().writeToFile(outFile);
                    }
                } catch (IOException e) {
                    LogUtils.getLogger().error("test image save error", e);
                }
            }
        }

        // fire the event
        LogUtils.getLogger().debug("Fire the MaterialMapTextureUpdatedEvent");
        MinecraftForge.EVENT_BUS.post(new MaterialMapTextureUpdatedEvent());
    }

    // </editor-fold>


    // <editor-fold desc="Event Handlers">

    @Mod.EventBusSubscriber(modid = TooManyTinkers.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class MaterialMapTextureInitializedResender {
        @SubscribeEvent()
        public static void onClientTick(TickEvent.ClientTickEvent e) {
            MinecraftForge.EVENT_BUS.unregister(MaterialMapTextureInitializedResender.class);
            // 补发第一次事件
            MinecraftForge.EVENT_BUS.post(new MaterialMapTextureUpdatedEvent());
        }
    }

    // </editor-fold>
}
