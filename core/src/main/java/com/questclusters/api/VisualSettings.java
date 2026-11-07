package com.questclusters.api;

/**
 * Animation settings shared by Quest Clusters modules. The visuals module installs a provider; without it, core
 * falls back to Certain Questing Additions' settings (if installed) or defaults.
 */
public interface VisualSettings {
    boolean questHover();

    int questHoverGlowAlpha();

    boolean smoothAnimations();

    final class Holder {
        private static volatile VisualSettings provider;

        private Holder() {
        }

        public static VisualSettings get() {
            return provider;
        }

        public static void set(VisualSettings p) {
            provider = p;
        }
    }
}
