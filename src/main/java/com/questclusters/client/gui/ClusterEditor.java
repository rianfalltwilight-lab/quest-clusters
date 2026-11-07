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

        menu.add(ContextMenuItem.SEPARATOR);
        if (owner.isPresent()) {
            QuestCluster c = owner.get();
            menu.add(new ContextMenuItem(tr("remove_from", ClusterText.title(c)), ThemeProperties.DELETE_ICON.get(),
                    b -> setMembers(c, without(c.members(), Set.of(id)))));
        } else {
            menu.add(new ContextMenuItem(tr("create"), ThemeProperties.ADD_ICON.get(),
                    b -> promptCreate(chapter, List.of(id))));
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
        menu.add(new ContextMenuItem(tr("create_from_selection", ids.size()), ThemeProperties.ADD_ICON.get(),
                b -> promptCreate(chapter, ids)));
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
        menu.add(new ContextMenuItem(tr("edit"), ThemeProperties.EDIT_ICON.get(), b -> openEditor(c)));
        menu.add(new ContextMenuItem(ClusterViewState.isExpanded(c) ? tr("collapse") : tr("expand"), Icons.INFO,
                b -> ClusterViewState.toggle(c)));
        menu.add(new ContextMenuItem(tr("select_members"), Icons.ADD, b -> selectMembers(screen, c)));
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

    static void promptCreate(Chapter chapter, List<Long> members) {
        String[] title = {Component.translatable("quest_clusters.default_title").getString()};
        ConfigGroup group = new ConfigGroup("quest_clusters", accepted -> {
            if (accepted && !title[0].isBlank()) {
                PacketDistributor.sendToServer(new ClusterPayloads.Create(chapter.id, title[0].strip(), List.copyOf(members)));
            }
            reopenQuestScreen();
        });
        group.addString("title", title[0], v -> title[0] = v, title[0]);
        new EditConfigScreen(group).openGui();
    }

    public static void openEditor(QuestCluster c) {
        QuestCluster[] edited = {c};
        ConfigGroup group = new ConfigGroup("quest_clusters", accepted -> {
            if (accepted && !edited[0].equals(c)) {
                update(edited[0]);
            }
            reopenQuestScreen();
        });
        group.addString("title", c.title(), v -> edited[0] = copy(edited[0], v, null, null, null, null, null), c.title()).setOrder(0);
        group.addString("subtitle", c.subtitle(), v -> edited[0] = copy(edited[0], null, v, null, null, null, null), "").setOrder(1);
        group.addColor("color", Color4I.rgba(c.color()), v -> edited[0] = copy(edited[0], null, null, v.rgba(), null, null, null),
                Color4I.rgba(QuestCluster.DEFAULT_COLOR)).setOrder(2);
        group.addBool("default_expanded", c.defaultExpanded(), v -> edited[0] = copy(edited[0], null, null, null, v, null, null), true).setOrder(3);
        group.addDouble("width", c.width(), v -> edited[0] = copy(edited[0], null, null, null, null, v, null), 3D,
                QuestCluster.MIN_BOX_SIZE, QuestCluster.MAX_BOX_SIZE).setOrder(4);
        group.addDouble("height", c.height(), v -> edited[0] = copy(edited[0], null, null, null, null, null, v), 1D,
                QuestCluster.MIN_BOX_SIZE, QuestCluster.MAX_BOX_SIZE).setOrder(5);
        new EditConfigScreen(group).openGui();
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

    /** Moves the box and every member by (dx, dy) canvas units. Member moves go through FTB Quests (undoable). */
    static void moveBy(QuestScreen screen, QuestCluster c, double dx, double dy) {
        if (dx != 0 || dy != 0) {
            PacketDistributor.sendToServer(new ClusterPayloads.Move(c.id(), dx, dy));
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
        return new QuestCluster(c.id(), c.chapterId(), c.title(), c.subtitle(), c.color(), c.defaultExpanded(),
                (minX + maxX) / 2, (minY + maxY) / 2, c.width(), c.height(), c.members());
    }

    private static void reopenQuestScreen() {
        ClientQuestFile.openGui();
    }

    private static QuestCluster copy(QuestCluster c, String title, String subtitle, Integer color, Boolean defExp,
                                     Double width, Double height) {
        return new QuestCluster(c.id(), c.chapterId(),
                title != null ? title : c.title(),
                subtitle != null ? subtitle : c.subtitle(),
                color != null ? color : c.color(),
                defExp != null ? defExp : c.defaultExpanded(),
                c.x(), c.y(),
                width != null ? width : c.width(),
                height != null ? height : c.height(),
                c.members());
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
