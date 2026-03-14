package com.kirisamey.toomanytinkers.models.animating;

import io.vavr.collection.Map;

public record TmtAnimationSet(Map<String, TmtAnimation> animations) {
}
