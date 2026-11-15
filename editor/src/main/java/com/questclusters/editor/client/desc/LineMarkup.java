package com.questclusters.editor.client.desc;

import com.questclusters.platform.Rl;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Line markup for quest descriptions, following the syntax of Misanthropy's FTB Quest Additions (MQA). Markers are
 * read at the start of a description line (after colour codes are applied) and removed from what is shown:
 * <ul>
 * <li>{@code ---} / {@code ===} alone on a line: divider / double divider</li>
 * <li>{@code >} / {@code <} / {@code ><}: blue bar on the left / right / both sides</li>
 * <li>{@code [spoiler]}: covered until clicked</li>
 * <li>{@code `}: code block (a closing backtick is dropped too)</li>
 * <li>{@code [gap=N]} alone on a line: N pixels of space, -5 to 100 (negative pulls the next line up)</li>
 * <li>{@code #} / {@code ##} / {@code ###}: large / medium / small heading</li>
 * <li>{@code [!info]} {@code [!note]}, {@code [!tip]} {@code [!success]}, {@code [!warn]} {@code [!warning]},
 * {@code [!danger]} {@code [!error]}: callout box</li>
 * <li>{@code - } and {@code 1. }: bullet / numbered list item; two leading spaces per extra level</li>
 * <li>{@code [center]}: centred line (our marker; MQA's Shift+O marker is not documented)</li>
 * <li>{@code \\} (or a single {@code \}): show the rest literally, no markup</li>
 * </ul>
 * Bold ({@code &l}) on a divider or bar line makes it thicker.
 */
public final class LineMarkup {
    public enum Kind { TEXT, DIVIDER, DOUBLE_DIVIDER, GAP }

    public enum Callout {
        INFO(0xFF3B82F6), TIP(0xFF22C55E), WARN(0xFFF59E0B), DANGER(0xFFEF4444);

        public final int color;

        Callout(int color) {
            this.color = color;
        }

        static Callout of(String name) {
            return switch (name.toLowerCase(Locale.ROOT)) {
                case "info", "note" -> INFO;
                case "tip", "success" -> TIP;
                case "warn", "warning" -> WARN;
                case "danger", "error" -> DANGER;
                default -> null;
            };
        }
    }

    /**
     * How one line is shown. {@code text} is the line without its markers.
     *
     * @param heading 0 none, 1-3 for # ## ###
     * @param bullet  "" none, "•" or "N." for list items
     * @param level   list nesting level (0 = top)
     */
    public record Line(Kind kind, Component text, boolean center, boolean barLeft, boolean barRight, boolean thick,
                        boolean spoiler, boolean code, int heading, Callout callout, String bullet, int level, int gap) {
        public boolean isPlain() {
            return kind == Kind.TEXT && !center && !barLeft && !barRight && !spoiler && !code && heading == 0
                    && callout == null && bullet.isEmpty();
        }

        public float scale() {
            return switch (heading) {
                case 1 -> 2F;
                case 2 -> 1.5F;
                case 3 -> 1.25F;
                default -> 1F;
            };
        }
    }

    private static final Pattern GAP = Pattern.compile("\\[gap=(-?\\d{1,3})]");
    private static final Pattern CALLOUT = Pattern.compile("\\[!([a-zA-Z]+)]\\s?");
    private static final Pattern NUMBER = Pattern.compile("(\\d{1,3})\\.\\s");

    private LineMarkup() {
    }

    public static Line parse(Component line, boolean centerAll) {
        String s = line.getString();
        Component text = line;
        int cut = 0;
        // literal: no markup at all
        if (s.startsWith("\\")) {
            int n = s.startsWith("\\\\") ? 2 : 1;
            return new Line(Kind.TEXT, drop(line, n, 0), centerAll, false, false, false, false, false, 0, null, "", 0, 0);
        }
        String trimmed = s.strip();
        boolean bold = anyBold(line);
        if (trimmed.equals("---")) {
            return block(Kind.DIVIDER, bold, centerAll, 0);
        }
        if (trimmed.equals("===")) {
            return block(Kind.DOUBLE_DIVIDER, bold, centerAll, 0);
        }
        Matcher gap = GAP.matcher(trimmed);
        if (gap.matches()) {
            int n = Math.max(-5, Math.min(100, Integer.parseInt(gap.group(1))));
            return block(Kind.GAP, false, centerAll, n);
        }
        boolean center = centerAll, barLeft = false, barRight = false, spoiler = false, code = false;
        int heading = 0;
        Callout callout = null;
        // prefixes may be combined, e.g. "[center]# Title" or "> [spoiler]text"
        for (boolean more = true; more; ) {
            more = false;
            String rest = s.substring(cut);
            if (rest.startsWith("[center]")) {
                center = true;
                cut += 8;
                more = true;
            } else if (rest.startsWith("[spoiler]")) {
                spoiler = true;
                cut += 9;
                more = true;
            } else if (rest.startsWith("><")) {
                barLeft = barRight = true;
                cut += skipSpace(rest, 2);
                more = true;
            } else if (rest.startsWith(">") && !barLeft) {
                barLeft = true;
                cut += skipSpace(rest, 1);
                more = true;
            } else if (rest.startsWith("<") && !barRight) {
                barRight = true;
                cut += skipSpace(rest, 1);
                more = true;
            } else if (callout == null && CALLOUT.matcher(rest).lookingAt()) {
                Matcher m = CALLOUT.matcher(rest);
                m.lookingAt();
                Callout c = Callout.of(m.group(1));
                if (c != null) {
                    callout = c;
                    cut += m.end();
                    more = true;
                }
            } else if (heading == 0 && (rest.startsWith("### ") || rest.startsWith("## ") || rest.startsWith("# "))) {
                heading = rest.startsWith("### ") ? 3 : rest.startsWith("## ") ? 2 : 1;
                cut += heading + 1;
                more = true;
            }
        }
        String rest = s.substring(cut);
        int tail = 0;
        if (rest.startsWith("`")) {
            code = true;
            cut += 1;
            if (rest.length() > 1 && rest.endsWith("`")) tail = 1;
            rest = rest.substring(1);
        }
        String bullet = "";
        int level = 0;
        if (!code) {
            int spaces = 0;
            while (spaces < rest.length() && rest.charAt(spaces) == ' ') spaces++;
            String item = rest.substring(spaces);
            Matcher num = NUMBER.matcher(item);
            if (item.startsWith("- ") || item.startsWith("* ")) {
                bullet = "•";
                level = Math.min(6, spaces / 2);
                cut += spaces + 2;
            } else if (num.lookingAt()) {
                bullet = num.group(1) + ".";
                level = Math.min(6, spaces / 2);
                cut += spaces + num.end();
            }
        }
        if (cut > 0 || tail > 0) {
            text = drop(line, cut, tail);
        }
        if (code) {
            text = Component.empty().append(text).withStyle(st -> st.withFont(Rl.mc("uniform")));
        }
        boolean thick = bold && (barLeft || barRight);
        return new Line(Kind.TEXT, text, center, barLeft, barRight, thick, spoiler, code, heading, callout, bullet, level, 0);
    }

    private static Line block(Kind kind, boolean thick, boolean center, int gap) {
        return new Line(kind, Component.empty(), center, false, false, thick, false, false, 0, null, "", 0, gap);
    }

    private static int skipSpace(String s, int from) {
        return from < s.length() && s.charAt(from) == ' ' ? from + 1 : from;
    }

    private static boolean anyBold(Component c) {
        boolean[] bold = {false};
        c.visit((style, text) -> {
            if (!text.isEmpty() && style.isBold()) bold[0] = true;
            return Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        return bold[0];
    }

    /** The component without its first {@code head} and last {@code tail} characters, styles and click events kept. */
    public static Component drop(Component c, int head, int tail) {
        int total = c.getString().length();
        int end = Math.max(head, total - tail);
        MutableComponent out = Component.empty();
        int[] pos = {0};
        c.visit((style, text) -> {
            int start = pos[0], stop = start + text.length();
            pos[0] = stop;
            int a = Math.max(start, head), b = Math.min(stop, end);
            if (a < b) out.append(Component.literal(text.substring(a - start, b - start)).withStyle(style));
            return Optional.<Object>empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        return out;
    }
}
