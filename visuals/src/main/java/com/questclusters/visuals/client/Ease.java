package com.questclusters.visuals.client;

/** Value easing towards a target over a fixed time; ease-out cubic when smooth animations are on. */
public final class Ease {
    private final float seconds;
    private float from, to, value;
    private long start;

    public Ease(float initial, float seconds) {
        this.seconds = seconds;
        this.from = this.to = this.value = initial;
    }

    /** Animate towards target; {@code restartFrom} non-null restarts from that value. */
    public void set(float target, Float restartFrom) {
        if (target == to && restartFrom == null) {
            return;
        }
        from = restartFrom != null ? restartFrom : value;
        to = target;
        start = System.nanoTime();
    }

    public void set(float target) {
        set(target, null);
    }

    public float get() {
        float t = Math.min(1F, (System.nanoTime() - start) / 1e9F / seconds);
        float k = VisualsConfig.SMOOTH_ANIMATIONS.get() ? 1F - (1F - t) * (1F - t) * (1F - t) : t;
        value = from + (to - from) * k;
        return value;
    }

    public float target() {
        return to;
    }

    public boolean animating() {
        return (System.nanoTime() - start) / 1e9F < seconds && from != to;
    }
}
