package com.questclusters.client.gui;

import com.questclusters.compat.CqaCompat;

/** Value that eases towards a target over a fixed time. Easing follows CQA's "smooth animations" setting. */
final class Anim {
    private final float durationMs;
    private float from, to, value;
    private long start;

    Anim(float initial, float durationMs) {
        this.durationMs = durationMs;
        this.from = this.to = this.value = initial;
    }

    float update(float target, boolean animate) {
        long now = System.currentTimeMillis();
        if (!animate) {
            from = to = value = target;
            return value;
        }
        if (target != to) {
            from = value;
            to = target;
            start = now;
        }
        float t = Math.min(1F, (now - start) / durationMs);
        value = from + (to - from) * ease(t);
        return value;
    }

    /** 0..1 progress of a transition that started at {@code startMs}. */
    static float progress(long startMs, float durationMs) {
        if (startMs <= 0) {
            return 1F;
        }
        return ease(Math.min(1F, (System.currentTimeMillis() - startMs) / durationMs));
    }

    static float ease(float t) {
        return CqaCompat.smooth() ? t * t * (3F - 2F * t) : t;
    }
}
