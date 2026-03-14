package com.kirisamey.toomanytinkers.models.animating;

import io.vavr.collection.List;
import io.vavr.collection.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@SuppressWarnings("ClassCanBeRecord")
@RequiredArgsConstructor
public class TmtAnimation {
    @Getter private final LoopMode loopMode;
    @Getter private final double length;
    @Getter private final Map<String, TmtAnimationBoneEntry> bones;

    enum LoopMode {
        No, Loop, HoldOnLast
    }
}
