package com.kirisamey.toomanytinkers.rendering;

import com.kirisamey.toomanytinkers.TooManyTinkers;
import com.kirisamey.toomanytinkers.rendering.materialmap.events.MaterialAnimFrameUpdatedEvent;
import com.kirisamey.toomanytinkers.rendering.materialmap.events.MaterialMappingRestartEvent;
import com.kirisamey.toomanytinkers.rendering.materialmap.events.MaterialMappingUpdatedEvent;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraftforge.client.model.IQuadTransformer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

public class TmtAnimColorBakedQuad extends BakedQuad {
    public TmtAnimColorBakedQuad(int[] pVertices, int pTintIndex, Direction pDirection, TextureAtlasSprite pSprite,
                                 boolean pShade, boolean hasAmbientOcclusion, int animId, boolean isLarge) {
        super(pVertices, pTintIndex, pDirection, pSprite, pShade, hasAmbientOcclusion);
        if (animId >= 0) {
            while (INSTANCES.size() <= animId) INSTANCES.add(new ArrayList<>());
            INSTANCES.get(animId).add(this);
        }
        this.isLarge = isLarge;
    }

    public static TmtAnimColorBakedQuad fromBakedQuad(BakedQuad origin, int animId, boolean isLarge) {
        return new TmtAnimColorBakedQuad(
                origin.getVertices(), origin.getTintIndex(), origin.getDirection(), origin.getSprite(),
                origin.isShade(), origin.hasAmbientOcclusion(), animId, isLarge
        );
    }

    private final boolean isLarge;

    private static final List<List<TmtAnimColorBakedQuad>> INSTANCES = new ArrayList<>();

    private void updateVertex(int newColor) {
        int i = 0;
        var cPos = IQuadTransformer.COLOR;
        while (cPos < vertices.length) {
            vertices[cPos] = newColor;
            i++;
            cPos = i * IQuadTransformer.STRIDE + IQuadTransformer.COLOR;
        }
    }


    @Mod.EventBusSubscriber(modid = TooManyTinkers.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)

    public static class EventSubscriber {
        @SubscribeEvent
        public static void OnMaterialMappingRestart(MaterialMappingRestartEvent e) {
            INSTANCES.clear();
        }

        @SubscribeEvent
        public static void OnMaterialMappingUpdated(MaterialMappingUpdatedEvent e) {
        }

        @SubscribeEvent
        public static void OnMaterialAnimFrameUpdated(MaterialAnimFrameUpdatedEvent e) {
            var id = e.getId();
            if (INSTANCES.size() <= id) return;
            INSTANCES.get(id).forEach(q -> {
                if (q.isLarge) q.updateVertex(e.getLargeVertexColor());
                else q.updateVertex(e.getVertexColor());
            });
        }
    }
}
