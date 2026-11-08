package com.questclusters.editor.client;

import java.util.List;

/** Loaded font ids (shared by canvas text and description components). */
public final class CanvasEditFonts {
    private CanvasEditFonts() {
    }

    public static List<String> fonts() {
        return CanvasEdit.fonts();
    }
}
