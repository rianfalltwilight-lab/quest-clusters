package com.questclusters.editor.client.desc;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LineMarkupTest {
    private static LineMarkup.Line p(String s) {
        return LineMarkup.parse(Component.literal(s), false);
    }

    @Test
    void blocks() {
        assertEquals(LineMarkup.Kind.DIVIDER, p("---").kind());
        assertEquals(LineMarkup.Kind.DOUBLE_DIVIDER, p(" === ").kind());
        assertEquals(LineMarkup.Kind.GAP, p("[gap=12]").kind());
        assertEquals(12, p("[gap=12]").gap());
        assertEquals(-5, p("[gap=-9]").gap(), "clamped");
        assertEquals(100, p("[gap=500]").gap(), "clamped");
        assertTrue(LineMarkup.parse(Component.literal("---").withStyle(ChatFormatting.BOLD), false).thick(), "&l makes it thick");
    }

    @Test
    void prefixesAreStripped() {
        var bars = p("><Both");
        assertTrue(bars.barLeft() && bars.barRight());
        assertEquals("Both", bars.text().getString());
        assertEquals("Left", p("> Left").text().getString());
        assertTrue(p("< R").barRight());
        var h = p("## Title");
        assertEquals(2, h.heading());
        assertEquals(1.5F, h.scale());
        assertEquals("Title", h.text().getString());
        var c = p("[!warning] Careful");
        assertEquals(LineMarkup.Callout.WARN, c.callout());
        assertEquals("Careful", c.text().getString());
        assertNull(p("[!unknown] x").callout(), "unknown callouts stay text");
        var code = p("`x = 1`");
        assertTrue(code.code());
        assertEquals("x = 1", code.text().getString());
        var sp = p("[spoiler]Hidden");
        assertTrue(sp.spoiler());
        assertEquals("Hidden", sp.text().getString());
    }

    @Test
    void lists() {
        var b = p("- item");
        assertEquals("•", b.bullet());
        assertEquals(0, b.level());
        assertEquals("item", b.text().getString());
        assertEquals(1, p("  - nested").level());
        var n = p("12. twelfth");
        assertEquals("12.", n.bullet());
        assertEquals("twelfth", n.text().getString());
        assertTrue(p("-not a list").isPlain());
    }

    @Test
    void combinedAndLiteral() {
        var m = p("[center]> [spoiler]x");
        assertTrue(m.center() && m.barLeft() && m.spoiler());
        assertEquals("x", m.text().getString());
        var lit = p("\\\\# not a heading");
        assertTrue(lit.isPlain());
        assertEquals("# not a heading", lit.text().getString());
        assertTrue(LineMarkup.parse(Component.literal("plain"), true).center(), "centre-all option");
        assertTrue(p("Plain text").isPlain());
    }

    @Test
    void stylesSurviveStripping() {
        Component line = Component.literal("> ").append(Component.literal("red").withStyle(ChatFormatting.RED));
        var m = LineMarkup.parse(line, false);
        assertEquals("red", m.text().getString());
        boolean[] red = {false};
        m.text().visit((style, text) -> {
            if (text.equals("red") && style.getColor() != null && style.getColor().getValue() == 0xFF5555) red[0] = true;
            return java.util.Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        assertTrue(red[0]);
    }
}
