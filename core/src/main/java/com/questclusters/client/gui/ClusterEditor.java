package com.questclusters.client.gui;

import com.questclusters.client.ClientClusters;
import com.questclusters.client.ClusterViewState;
import com.questclusters.data.QuestCluster;
import com.questclusters.mixin.client.QuestScreenAccessor;
import com.questclusters.net.ClusterPayloads;
import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.ui.EditConfigScreen;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.ContextMenuItem;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import dev.ftb.mods.ftbquests.net.DeleteObjectMessage;
import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.Movable;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.theme.property.ThemeProperties;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Editor actions. Every change is a request to the server, which validates it and broadcasts the result;
 * the client never edits its replica directly.
 */
public final class ClusterEditor {
    private ClusterEditor() {
    }

    private static MutableComponent tr(String key, Object... args) {
        return Component.translatable("quest_clusters.menu." + key, args);
    }

    // ------------------------------------------------------------------ menu entries added to FTB Quests' menus

    /** Right-click on a single quest, quest link or chapter image. */
    public static void addObjectMenuItems(List<ContextMenuItem> menu, QuestScreen screen, Movable movable) {
        if (!ClusterWidgets.canEdit() || movable.getChapter() == null) {
            return;
        }
        long id = movable.getMovableID();
        Chapter chapter = movable.getChapter();
        Optional<QuestCluster> owner = ClientClusters.store().clusterOfMember(id);
        ScoringEditor.addQuestItems(menu, screen, movable);

        menu.add(ContextMenuItem.SEPARATOR);
        if (owner.isPresent()) {
            QuestCluster c = owner.get();
            menu.add(new ContextMenuItem(tr("remove_from", ClusterText.title(c)), ThemeProperties.DELETE_ICON.get(),
                    b -> setMembers(c, without(c.members(), Set.of(id)))));
        } else {
            menu.add(new ContextMenuItem(tr("create"), ThemeProperties.ADD_ICON.get(),
                    b -> promptCreate(screen, chapter, List.of(id))));
            List<ContextMenuItem> targets = new ArrayList<>();
            for (QuestCluster c : ClientClusters.store().byChapter(chapter.id)) {
                targets.add(new ContextMenuItem(ClusterText.title(c), Icons.ADD,
                        b -> setMembers(c, with(c.members(), List.of(id)))));
            }
            if (!targets.isEmpty()) {
                menu.add(ContextMenuItem.subMenu(tr("add_to"), Icons.ADD, targets));
            }
        }
    }

    /** Right-click with several objects selected. */
    public static void addSelectionMenuItems(List<ContextMenuItem> menu, QuestScreen screen, List<Movable> selected, Chapter chapter) {
        if (!ClusterWidgets.canEdit() || selected.isEmpty() || chapter == null) {
            return;
        }
        List<Long> ids = selected.stream().filter(m -> m.getChapter() == chapter).map(Movable::getMovableID).toList();
        if (ids.isEmpty()) {
            return;
        }
        menu.add(ContextMenuItem.SEPARATOR);
        ScoringEditor.addSelectionItems(menu, screen, selected);
        menu.add(new ContextMenuItem(tr("create_from_selection", ids.size()), ThemeProperties.ADD_ICON.get(),
                b -> promptCreate(screen, chapter, ids)));
        List<ContextMenuItem> targets = new ArrayList<>();
        for (QuestCluster c : ClientClusters.store().byChapter(chapter.id)) {
            targets.add(new ContextMenuItem(ClusterText.title(c), Icons.ADD,
                    b -> setMembers(c, with(c.members(), ids))));
        }
        if (!targets.isEmpty()) {
            menu.add(ContextMenuItem.subMenu(tr("add_selection_to"), Icons.ADD, targets));
        }
        boolean anyOwned = ids.stream().anyMatch(id -> ClientClusters.store().clusterOfMember(id).isPresent());
        if (anyOwned) {
            menu.add(new ContextMenuItem(tr("remove_selection"), ThemeProperties.DELETE_ICON.get(), b -> {
                Set<Long> drop = Set.copyOf(ids);
                for (QuestCluster c : ClientClusters.store().byChapter(chapter.id)) {
                    if (c.members().stream().anyMatch(drop::contains)) {
                        setMembers(c, without(c.members(), drop));
                    }
                }
            }));
        }
    }

    /** Right-click on a cluster box or frame title strip. */
    public static List<ContextMenuItem> clusterMenu(QuestScreen screen, QuestCluster c) {
        List<ContextMenuItem> menu = new ArrayList<>();
        menu.add(new ContextMenuItem(Component.literal("\"").append(ClusterText.title(c)).append("\""), Color4I.empty(), null).setCloseMenu(false));
        menu.add(ContextMenuItem.SEPARATOR);
        menu.add(new ContextMenuItem(tr("edit"), ThemeProperties.EDIT_ICON.get(), b -> openEditor(screen, c)));
        menu.add(new ContextMenuItem(tr("background"), Icons.ART, b -> openBackgroundEditor(screen, c)));
        menu.add(new ContextMenuItem(ClusterViewState.isExpanded(c) ? tr("collapse") : tr("expand"), Icons.INFO,
                b -> ClusterViewState.toggle(c)));
        menu.add(new ContextMenuItem(tr("select_members"), Icons.ADD, b -> selectMembers(screen, c)));
        // selected quests, links and images of this chapter that are not members yet (images need no quest with them)
        List<Long> joining = selectedNonMembers(screen, c);
        if (!joining.isEmpty()) {
            menu.add(new ContextMenuItem(tr("add_selected_here", joining.size()), ThemeProperties.ADD_ICON.get(),
                    b -> addToCluster(c, joining)));
        }
        menu.add(new ContextMenuItem(tr("fit_box"), Icons.SETTINGS, b -> update(fitBoxToMembers(c))));
        // whole-cluster prerequisites, done with FTB Quests' own dependencies on every member quest
        List<Quest> prereqs = selectedPrerequisites(screen, c);
        if (!prereqs.isEmpty()) {
            menu.add(new ContextMenuItem(tr("prereq_add", prereqs.size()), ThemeProperties.ADD_ICON.get(),
                    b -> editMemberDependencies(screen, c, prereqs, true)));
            menu.add(new ContextMenuItem(tr("prereq_remove", prereqs.size()), ThemeProperties.DELETE_ICON.get(),
                    b -> editMemberDependencies(screen, c, prereqs, false)));
        } else {
            menu.add(new ContextMenuItem(tr("prereq_hint").withStyle(ChatFormatting.GRAY), Icons.INFO, null).setCloseMenu(false));
        }
        menu.add(ContextMenuItem.SEPARATOR);
        menu.add(new ContextMenuItem(tr("dissolve"), ThemeProperties.DELETE_ICON.get(),
                b -> PacketDistributor.sendToServer(new ClusterPayloads.Dissolve(c.id())))
                .setYesNoText(tr("dissolve.confirm", ClusterText.title(c))));
        menu.add(new ContextMenuItem(tr("delete_all", c.members().size()).withStyle(ChatFormatting.RED),
                ThemeProperties.DELETE_ICON.get(),
                // FTB Quests' own delete (undoable via its history); the server then drops the empty cluster
                b -> NetworkManager.sendToServer(new DeleteObjectMessage(List.copyOf(c.members()))))
                .setYesNoText(tr("delete_all.confirm", c.members().size(), ClusterText.title(c))));
        return menu;
    }

    // ------------------------------------------------------------------ actions

    static void promptCreate(QuestScreen screen, Chapter chapter, List<Long> members) {
        String[] title = {Component.translatable("quest_clusters.default_title").getString()};
        ConfigGroup group = new ConfigGroup("quest_clusters", accepted -> {
            if (accepted && !title[0].isBlank()) {
                PacketDistributor.sendToServer(new ClusterPayloads.Create(chapter.id, title[0].strip(), List.copyOf(members)));
            }
            reopen(screen);
        });
        group.addString("title", title[0], v -> title[0] = v, title[0]);
        new EditConfigScreen(group).openGui();
    }

    public static void openEditor(QuestScreen screen, QuestCluster c) {
        QuestCluster[] e = {c};
        boolean[] autoBorder = {c.style().borderColor() == 0};
        boolean[] boxFrame = {c.style().frameColor() == 0};
        int[] border = {c.style().borderColor() != 0 ? c.style().borderColor() : 0xFFFFFFFF};
        int[] frame = {c.style().frameColor() != 0 ? c.style().frameColor() : c.color()};
        ConfigGroup group = new ConfigGroup("quest_clusters", accepted -> {
            reopen(screen);
            if (!accepted) {
                return;
            }
            QuestCluster out = e[0].withStyle(e[0].style()
                    .withBorder(autoBorder[0] ? 0 : border[0])
                    .withFrame(boxFrame[0] ? 0 : frame[0]));
            if (!out.equals(c)) {
                update(out);
            }
        });
        int o = 0;
        group.addString("title", c.title(), v -> e[0] = e[0].withText(v, e[0].subtitle()), c.title()).setOrder(o++);
        group.addString("subtitle", c.subtitle(), v -> e[0] = e[0].withText(e[0].title(), v), "").setOrder(o++);
        group.addColor("color", Color4I.rgba(c.color()), v -> e[0] = e[0].withColor(v.rgba() | 0xFF000000),
                Color4I.rgba(QuestCluster.DEFAULT_COLOR)).setOrder(o++);
        group.addColor("title_color", Color4I.rgba(c.style().titleOr()),
                v -> e[0] = e[0].withStyle(e[0].style().withTitle(v.rgba() | 0xFF000000)), Color4I.WHITE).setOrder(o++);
        group.addColor("subtitle_color", Color4I.rgba(c.style().subtitleOr()),
                v -> e[0] = e[0].withStyle(e[0].style().withSubtitle(v.rgba() | 0xFF000000)), Color4I.rgba(com.questclusters.data.ClusterStyle.DEFAULT_SUBTITLE)).setOrder(o++);
        group.addBool("border_auto", autoBorder[0], v -> autoBorder[0] = v, true).setOrder(o++);
        group.addColor("border_color", Color4I.rgba(border[0]), v -> border[0] = v.rgba() | 0xFF000000, Color4I.WHITE).setOrder(o++);
        group.addBool("frame_from_box", boxFrame[0], v -> boxFrame[0] = v, true).setOrder(o++);
        group.addColor("frame_color", Color4I.rgba(frame[0]), v -> frame[0] = v.rgba() | 0xFF000000,
                Color4I.rgba(QuestCluster.DEFAULT_COLOR)).setOrder(o++);
        o = addBackgroundFields(group, e, o);
        group.addBool("default_expanded", c.defaultExpanded(), v -> e[0] = e[0].withDefaultExpanded(v), true).setOrder(o++);
        group.addBool("free_box", c.style().freeBox(), v -> e[0] = e[0].withStyle(e[0].style().withFreeBox(v)), false).setOrder(o++);
        group.addDouble("width", c.width(), v -> e[0] = e[0].withBoxSize(v, e[0].height()), 3D,
                QuestCluster.MIN_BOX_SIZE, QuestCluster.MAX_BOX_SIZE).setOrder(o++);
        group.addDouble("height", c.height(), v -> e[0] = e[0].withBoxSize(e[0].width(), v), 1D,
                QuestCluster.MIN_BOX_SIZE, QuestCluster.MAX_BOX_SIZE).setOrder(o++);
        new EditConfigScreen(group).openGui();
    }

    /** Image picker (FTB Quests' own, as for chapter images), fill mode, nine-slice border, frame toggle. */
    private static int addBackgroundFields(ConfigGroup group, QuestCluster[] e, int o) {
        var st = e[0].style();
        net.minecraft.resources.ResourceLocation current = st.hasBackground()
                ? net.minecraft.resources.ResourceLocation.tryParse(st.background()) : null;
        group.add("background", new dev.ftb.mods.ftblibrary.config.ImageResourceConfig(),
                current == null ? dev.ftb.mods.ftblibrary.config.ImageResourceConfig.NONE : current,
                v -> e[0] = e[0].withStyle(e[0].style().withBackground(
                        v == null || v.equals(dev.ftb.mods.ftblibrary.config.ImageResourceConfig.NONE) ? "" : v.toString(),
                        e[0].style().backgroundMode(), e[0].style().sliceBorder(), e[0].style().backgroundOnFrame())),
                dev.ftb.mods.ftblibrary.config.ImageResourceConfig.NONE).setOrder(o++);
        group.addEnum("background_mode", BackgroundMode.values()[st.backgroundMode()],
                v -> e[0] = e[0].withStyle(e[0].style().withBackground(e[0].style().background(), v.ordinal(),
                        e[0].style().sliceBorder(), e[0].style().backgroundOnFrame())),
                BackgroundMode.NAME_MAP).setOrder(o++);
        group.addInt("slice_border", st.sliceBorder(),
                v -> e[0] = e[0].withStyle(e[0].style().withBackground(e[0].style().background(), e[0].style().backgroundMode(),
                        v, e[0].style().backgroundOnFrame())), 4, 0, 64).setOrder(o++);
        group.addBool("background_on_frame", st.backgroundOnFrame(),
                v -> e[0] = e[0].withStyle(e[0].style().withBackground(e[0].style().background(), e[0].style().backgroundMode(),
                        e[0].style().sliceBorder(), v)), false).setOrder(o++);
        return o;
    }

    enum BackgroundMode {
        STRETCH, TILE, NINE_SLICE;
        static final dev.ftb.mods.ftblibrary.config.NameMap<BackgroundMode> NAME_MAP = dev.ftb.mods.ftblibrary.config.NameMap
                .of(STRETCH, values()).id(m -> m.name().toLowerCase(java.util.Locale.ROOT))
                .name(m -> Component.translatable("quest_clusters.background_mode." + m.name().toLowerCase(java.util.Locale.ROOT))).create();
    }

    /** Right click → "Set background image…": only the background fields. */
    public static void openBackgroundEditor(QuestScreen screen, QuestCluster c) {
        QuestCluster[] e = {c};
        ConfigGroup group = new ConfigGroup("quest_clusters", accepted -> {
            reopen(screen);
            if (accepted && !e[0].equals(c)) {
                update(e[0]);
            }
        });
        addBackgroundFields(group, e, 0);
        new EditConfigScreen(group).openGui();
    }

    /**
     * Back to the quest screen we came from. Re-opening that same screen keeps its "previous screen" (the game),
     * so Esc / the inventory key then close it normally instead of returning to the config screen.
     */
    private static void reopen(QuestScreen screen) {
        if (screen != null) {
            screen.openGui();
        } else {
            ClientQuestFile.openGui();
        }
    }

    static void setMembers(QuestCluster c, List<Long> members) {
        if (members.isEmpty()) {
            PacketDistributor.sendToServer(new ClusterPayloads.Dissolve(c.id()));
        } else {
            update(c.withMembers(members));
        }
    }

    static void update(QuestCluster c) {
        PacketDistributor.sendToServer(new ClusterPayloads.Update(c));
    }

    /**
     * A drag of the box ({@code fromBox}) or the expanded frame by (dx, dy) canvas units. Normally box and members
     * move together; with a free box, the box drag moves only the box and the frame drag only the members.
     * Member moves go through FTB Quests (undoable).
     */
    static void moveBy(QuestScreen screen, QuestCluster c, double dx, double dy, boolean fromBox) {
        if (dx == 0 && dy == 0) {
            return;
        }
        if (c.style().freeBox() && fromBox) {
            update(c.withBoxPosition(c.x() + dx, c.y() + dy));
        } else {
            PacketDistributor.sendToServer(new ClusterPayloads.Move(c.id(), dx, dy, c.style().freeBox()));
        }
    }

    static void selectMembers(QuestScreen screen, QuestCluster c) {
        ClusterViewState.setExpanded(c, true); // hidden objects can't be shown as selected
        ClientQuestFile file = ClientQuestFile.INSTANCE;
        List<Movable> selected = ((QuestScreenAccessor) screen).quest_clusters$getSelectedObjects();
        for (long id : c.members()) {
            if (file.getBase(id) instanceof Movable m && !selected.contains(m)) {
                screen.toggleSelected(m);
            }
        }
    }

    /** Selected objects of the cluster's chapter that are not its members yet. */
    static List<Long> selectedNonMembers(QuestScreen screen, QuestCluster c) {
        List<Long> out = new ArrayList<>();
        for (Movable m : ((QuestScreenAccessor) screen).quest_clusters$getSelectedObjects()) {
            if (m.getChapter() != null && m.getChapter().id == c.chapterId() && !c.members().contains(m.getMovableID())
                    && !out.contains(m.getMovableID())) {
                out.add(m.getMovableID());
            }
        }
        return out;
    }

    /** Adds objects to a cluster; ones in another cluster of the chapter leave it first (an object has one cluster). */
    static void addToCluster(QuestCluster c, List<Long> ids) {
        Set<Long> moving = Set.copyOf(ids);
        for (QuestCluster other : ClientClusters.store().byChapter(c.chapterId())) {
            if (other.id() != c.id() && other.members().stream().anyMatch(moving::contains)) {
                setMembers(other, without(other.members(), moving));
            }
        }
        setMembers(c, with(c.members(), ids));
    }

    /** Selected quests outside the cluster (any chapter) are candidate prerequisites. */
    static List<Quest> selectedPrerequisites(QuestScreen screen, QuestCluster c) {
        List<Quest> out = new ArrayList<>();
        for (Movable m : ((QuestScreenAccessor) screen).quest_clusters$getSelectedObjects()) {
            Quest q = m instanceof Quest quest ? quest : m instanceof dev.ftb.mods.ftbquests.quest.QuestLink l ? l.getQuest().orElse(null) : null;
            if (q != null && !c.members().contains(q.id) && !out.contains(q)) {
                out.add(q);
            }
        }
        return out;
    }

    /**
     * Adds (or removes) the given quests as dependencies of every quest in the cluster, exactly like FTB Quests'
     * own "add dependencies" action: per quest, reject loops, then send the edited quest to the server.
     */
    public static int editMemberDependencies(QuestScreen screen, QuestCluster c, List<Quest> deps, boolean add) {
        ClientQuestFile file = ClientQuestFile.INSTANCE;
        int changed = 0, loops = 0;
        for (long id : c.members()) {
            if (!(file.getBase(id) instanceof Quest quest)) {
                continue;
            }
            List<dev.ftb.mods.ftbquests.quest.QuestObject> before = quest.streamDependencies().toList();
            boolean dirty = false;
            for (Quest dep : deps) {
                if (dep == quest || add == quest.hasDependency(dep)) {
                    continue;
                }
                if (add) {
                    quest.addDependency(dep);
                } else {
                    quest.removeDependency(dep);
                }
                dirty = true;
            }
            if (!dirty) {
                continue;
            }
            quest.removeInvalidDependencies();
            if (quest.verifyDependencies(false)) {
                NetworkManager.sendToServer(dev.ftb.mods.ftbquests.net.EditObjectMessage.forQuestObject(quest));
                changed++;
            } else {
                quest.clearDependencies();
                before.forEach(quest::addDependency);
                loops++;
            }
        }
        if (loops > 0) {
            QuestScreen.displayError(Component.translatable("ftbquests.gui.looping_dependencies"));
        }
        screen.questPanel.refreshWidgets();
        return changed;
    }

    static QuestCluster fitBoxToMembers(QuestCluster c) {
        ClientQuestFile file = ClientQuestFile.INSTANCE;
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (long id : c.members()) {
            if (file.getBase(id) instanceof Movable m) {
                minX = Math.min(minX, m.getX() - m.getWidth() / 2);
                maxX = Math.max(maxX, m.getX() + m.getWidth() / 2);
                minY = Math.min(minY, m.getY() - m.getHeight() / 2);
                maxY = Math.max(maxY, m.getY() + m.getHeight() / 2);
            }
        }
        if (minX == Double.MAX_VALUE) {
            return c;
        }
        return c.withBoxPosition((minX + maxX) / 2, (minY + maxY) / 2);
    }

    static List<Long> with(List<Long> members, List<Long> add) {
        Set<Long> s = new LinkedHashSet<>(members);
        s.addAll(add);
        return List.copyOf(s);
    }

    static List<Long> without(List<Long> members, Set<Long> remove) {
        return members.stream().filter(m -> !remove.contains(m)).toList();
    }
}
