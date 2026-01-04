package com.kirisamey.toomanytinkers.rendering.materialmap.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraftforge.eventbus.api.Event;

@AllArgsConstructor
public class MaterialAnimFrameUpdatedEvent extends Event {
    @Getter private int id;
    @Getter private int frame;
    @Getter private int vertexColor;
    @Getter private int largeVertexColor;
}
