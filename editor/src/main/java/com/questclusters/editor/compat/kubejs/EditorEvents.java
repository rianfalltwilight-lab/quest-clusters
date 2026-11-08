package com.questclusters.editor.compat.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.latvian.mods.kubejs.script.ScriptType;

/** Client event group {@code QuestEnhanceEvents} (name kept for existing scripts). */
public final class EditorEvents {
    public static final EventGroup GROUP = EventGroup.of("QuestEnhanceEvents");
    public static final EventHandler CLICK = GROUP.client("click", () -> TextClickEvent.class);

    private EditorEvents() {
    }

    /** A player (not editing) clicked a canvas text element. */
    public static final class TextClickEvent implements KubeEvent {
        private final String text, chapterId;
        private final double x, y, width, height;

        TextClickEvent(String text, long chapterId, double x, double y, double width, double height) {
            this.text = text;
            this.chapterId = Long.toUnsignedString(chapterId); // decimal, as Quest Enhance passed it
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public String getText() {
            return text;
        }

        public String getChapterId() {
            return chapterId;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getWidth() {
            return width;
        }

        public double getHeight() {
            return height;
        }
    }

    static void postClick(String text, long chapterId, double x, double y, double w, double h) {
        if (CLICK.hasListeners()) {
            CLICK.post(ScriptType.CLIENT, new TextClickEvent(text, chapterId, x, y, w, h));
        }
    }
}
