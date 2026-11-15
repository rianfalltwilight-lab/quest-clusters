package com.questclusters.editor.client.gui;

import com.questclusters.client.Textures;
import com.questclusters.editor.client.ClientEditor;
import com.questclusters.editor.data.BookCheck;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.SimpleTextButton;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.input.Key;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.ui.misc.AbstractButtonListScreen;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.Movable;
import dev.ftb.mods.ftbquests.quest.Quest;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Edit mode: the book check's findings; click one to go to it. */
public final class BookCheckScreen extends AbstractButtonListScreen {
    private final QuestScreen questScreen;
    private List<BookCheck.Issue> issues;

    public BookCheckScreen(QuestScreen questScreen) {
        this.questScreen = questScreen;
        setHasSearchBox(true);
        setBorder(1, 1, 1);
        rerun();
    }

    /** Runs the check with the client's resources (so textures are checked too). */
    public static List<BookCheck.Issue> runClient() {
        BookCheck.animationProblem = tex -> {
            var rl = Textures.file(tex);
            return rl == null ? null : Textures.animationProblem(rl);
        };
        return BookCheck.run(ClientQuestFile.INSTANCE, ClientEditor.book(), BookCheckScreen::textureExists);
    }

    static boolean textureExists(String tex) {
        try {
            var rl = Textures.file(tex);
            if (rl == null) return false;
            var mc = Minecraft.getInstance();
            // pasted images are registered textures, not (yet) resource-pack files
            return mc.getResourceManager().getResource(rl).isPresent() || mc.getTextureManager().getTexture(rl, null) != null;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private void rerun() {
        issues = runClient();
        long errors = issues.stream().filter(i -> i.level() == BookCheck.Level.ERROR).count();
        long warns = issues.stream().filter(i -> i.level() == BookCheck.Level.WARN).count();
        setTitle(issues.isEmpty() ? Component.translatable("quest_clusters_editor.check.none")
                : Component.translatable("quest_clusters_editor.check.title", errors, warns, issues.size() - errors - warns));
    }

    public List<BookCheck.Issue> issues() {
        return issues;
    }

    @Override
    public void addButtons(Panel panel) {
        for (BookCheck.Issue i : issues) panel.add(new IssueButton(panel, i));
    }

    @Override
    public boolean onInit() {
        setWidth(Math.min(360, getGui().getScreen().getGuiScaledWidth() - 20));
        setHeight(getGui().getScreen().getGuiScaledHeight() * 4 / 5);
        return true;
    }

    @Override
    public boolean onClosedByKey(Key key) {
        if (super.onClosedByKey(key)) {
            doCancel();
            return true;
        }
        return false;
    }

    @Override
    protected void doCancel() {
        questScreen.openGui();
    }

    @Override
    protected void doAccept() {
        questScreen.openGui();
    }

    /** Opens the quest, or selects the chapter and scrolls to the object. */
    public static void jump(QuestScreen screen, BookCheck.Issue i) {
        ClientQuestFile file = ClientQuestFile.INSTANCE;
        screen.openGui();
        var base = i.object() == 0 ? null : file.getBase(i.object());
        if (base instanceof Quest q) {
            screen.open(q, false);
            return;
        }
        Chapter ch = i.chapter() == 0 ? null : file.getChapter(i.chapter());
        if (ch == null) return;
        screen.selectChapter(ch);
        if (base instanceof Movable m) screen.questPanel.scrollTo(m.getX(), m.getY());
    }

    private static Icon icon(BookCheck.Level l) {
        return switch (l) {
            case ERROR -> Icons.CANCEL;
            case WARN -> Icons.INFO;
            case INFO -> Icons.NOTES;
        };
    }

    private class IssueButton extends SimpleTextButton {
        private final BookCheck.Issue issue;

        IssueButton(Panel panel, BookCheck.Issue issue) {
            super(panel, issue.text().copy().withStyle(switch (issue.level()) {
                case ERROR -> ChatFormatting.RED;
                case WARN -> ChatFormatting.YELLOW;
                case INFO -> ChatFormatting.GRAY;
            }), icon(issue.level()));
            this.issue = issue;
            setHeight(16);
        }

        @Override
        public void onClicked(MouseButton button) {
            playClickSound();
            jump(questScreen, issue);
        }

        @Override
        public void addMouseOverText(TooltipList list) {
            list.add(issue.text());
            list.add(Component.translatable("quest_clusters_editor.check.level." + issue.level().name().toLowerCase(java.util.Locale.ROOT))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }

        @Override
        public void drawBackground(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            if (isMouseOver) Color4I.WHITE.withAlpha(30).draw(graphics, x, y, w, h);
        }
    }
}
