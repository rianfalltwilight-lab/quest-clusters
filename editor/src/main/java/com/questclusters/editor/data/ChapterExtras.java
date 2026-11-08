package com.questclusters.editor.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Per-chapter editor data. Mutable; owned by {@link BookExtras}. */
public final class ChapterExtras {
    /** Background image resource location, or "" for none. */
    public String background = "";
    /** Quest ids that must all be completed before the chapter shows (insertion order kept). */
    public final List<Long> prerequisites = new ArrayList<>();
    /** Each decorative line as node keys (see {@link NodeKeys}). */
    public final List<List<String>> decorativeLines = new ArrayList<>();
    public final List<HiddenLine> hiddenLines = new ArrayList<>();
    /** Default decorative line preset of this chapter ("" = the built-in look). */
    public String linePreset = "";
    /** Node key → preset of the decorative lines that start at that node (overrides the chapter default). */
    public final java.util.Map<String, String> nodeLinePresets = new java.util.LinkedHashMap<>();

    /** The preset id for a line starting at {@code node}: the node's own, else the chapter default ("" = built-in). */
    public String presetFor(String node) {
        String own = nodeLinePresets.get(node);
        return own != null ? own : linePreset;
    }

    public record HiddenLine(String source, String dependency, boolean revealOnHover) {
        public HiddenLine {
            Objects.requireNonNull(source);
            Objects.requireNonNull(dependency);
        }
    }

    public boolean isEmpty() {
        return background.isEmpty() && prerequisites.isEmpty() && decorativeLines.isEmpty() && hiddenLines.isEmpty()
                && linePreset.isEmpty() && nodeLinePresets.isEmpty();
    }

    public ChapterExtras copy() {
        ChapterExtras c = new ChapterExtras();
        c.background = background;
        c.prerequisites.addAll(prerequisites);
        decorativeLines.forEach(l -> c.decorativeLines.add(new ArrayList<>(l)));
        c.hiddenLines.addAll(hiddenLines);
        c.linePreset = linePreset;
        c.nodeLinePresets.putAll(nodeLinePresets);
        return c;
    }

    /** Dedupe and drop blanks; null if fewer than 2 nodes remain (Quest Enhance's rule). */
    public static List<String> normaliseLine(List<String> nodes) {
        List<String> out = new ArrayList<>();
        for (String n : nodes) {
            if (n != null && !n.isBlank() && !out.contains(n)) {
                out.add(n);
            }
        }
        return out.size() < 2 ? null : out;
    }

    public java.util.Optional<HiddenLine> hiddenLine(String source, String dependency) {
        for (HiddenLine h : hiddenLines) {
            if (h.source.equals(source) && h.dependency.equals(dependency)) {
                return java.util.Optional.of(h);
            }
        }
        return java.util.Optional.empty();
    }
}
