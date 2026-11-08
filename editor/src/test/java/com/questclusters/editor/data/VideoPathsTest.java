package com.questclusters.editor.data;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VideoPathsTest {
    @Test
    void normalises() {
        assertEquals("intro/start.mp4", VideoPaths.normalise(" \\intro\\.\\start.mp4 "));
        assertEquals("a/c.mp4", VideoPaths.normalise("a/b/../c.mp4"));
        assertNull(VideoPaths.normalise("../secret.mp4"));
        assertNull(VideoPaths.normalise("C:/x.mp4"));
        assertNull(VideoPaths.normalise(""));
        assertTrue(VideoPaths.isBundled(VideoPaths.normalise("assets/clips/a.webm")));
        assertTrue(VideoPaths.isVideoFile("A.MP4"));
    }

    @Test
    void markupRoundTrip() {
        String line = VideoPaths.descriptionLine("intro/开场.mp4", "Play {now} 100%");
        assertTrue(line.startsWith("{quest_enhance_video:"));
        String inner = line.substring(1, line.length() - 1);
        String b64 = inner.substring(inner.indexOf(':') + 1, inner.indexOf(' '));
        assertEquals("intro/开场.mp4", VideoPaths.decode(b64));
        // FTB splits on spaces and turns %20 into spaces before we see the label
        String rawLabel = inner.substring(inner.indexOf("quest_enhance_video_label:") + 26).replace("%20", " ");
        assertEquals("Play {now} 100%", VideoPaths.label(Map.of(VideoPaths.LABEL, rawLabel), "x"));
        assertNull(VideoPaths.decode("!!!"));
    }
}
