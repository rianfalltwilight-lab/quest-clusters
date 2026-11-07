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

    public record HiddenLine(String source, String dependency, boolean revealOnHover) {
        public HiddenLine {
            Objects.requireNonNull(source);
            Objects.requireNonNull(dependency);
        }
    }

    public boolean isEmpty() {
        return background.isEmpty() && prerequisites.isEmpty() && decorativeLines.isEmpty() && hiddenLines.isEmpty();
    }

    public ChapterExtras copy() {
        ChapterExtras c = new ChapterExtras();
        c.background = background;
        c.prerequisites.addAll(prerequisites);
        decorativeLines.forEach(l -> c.decorativeLines.add(new ArrayList<>(l)));
        c.hiddenLines.addAll(hiddenLines);
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
