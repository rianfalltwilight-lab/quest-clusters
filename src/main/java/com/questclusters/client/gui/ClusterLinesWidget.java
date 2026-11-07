package com.questclusters.client.gui;

import com.questclusters.client.ClientClusters;
import com.questclusters.client.ClusterViewState;
import com.questclusters.compat.QuestEnhanceCompat;
import com.questclusters.data.ClusterEdges;
import com.questclusters.data.LineRule;
import com.questclusters.data.QuestCluster;
import com.questclusters.mixin.client.QuestButtonAccessor;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestButton;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestPanel;
import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.Movable;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.QuestObject;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.theme.property.ThemeProperties;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;

/**
 * Draws the dependency lines that start or end at a collapsed cluster box. Lines between two visible quests are
 * still drawn by FTB Quests (or whichever addon replaces its line renderer); lines to hidden members are skipped
 * there automatically because hidden buttons report {@code shouldDraw() == false}.
 * <p>
 * Lines are drawn straight, without FTB Quests' animated texture or bezier curves.
 */
public final class ClusterLinesWidget extends Widget {
    private final QuestPanel questPanel;
    private final Map<Long, QuestButton> buttons = new HashMap<>();
    private final Map<Long, ClusterBoxWidget> boxes = new HashMap<>();
    private Map<ClusterEdges.Edge, List<ClusterEdges.Pair>> edges = Map.of();
    private int edgesVersion = Integer.MIN_VALUE;
    private long edgesTime;
    /** For the dev self-test: lines drawn in the last frame, and how many in the "hidden" edit colour. */
    public int lastDrawn, lastDrawnHidden;

    ClusterLinesWidget(QuestPanel panel) {
        super(panel);
        this.questPanel = panel;
        setDrawLayer(Widget.DrawLayer.BACKGROUND);
    }

    void align() {
        buttons.clear();
        boxes.clear();
        for (Widget w : questPanel.getWidgets()) {
            if (w instanceof QuestButton qb) {
                buttons.put(qb.moveAndDeleteFocus().getMovableID(), qb);
            } else if (w instanceof ClusterBoxWidget b) {
                boxes.put(b.cluster.id(), b);
            }
        }
        edgesVersion = Integer.MIN_VALUE;
        setPosAndSize(0, 0, 1, 1);
    }

    @Override
    public boolean collidesWith(int x, int y, int w, int h) {
        return true; // spans the whole canvas; never culled
    }

    @Override
    public boolean checkMouseOver(int mouseX, int mouseY) {
        return false;
    }

    @Override
    public boolean shouldAddMouseOverText() {
        return false;
    }

    @Override
    public void addMouseOverText(TooltipList list) {
    }

    private Map<ClusterEdges.Edge, List<ClusterEdges.Pair>> edges() {
        int v = ClusterViewState.state().version();
        long now = System.currentTimeMillis();
        // visibility depends on progress, so refresh now and then even without expand/collapse changes
        if (v != edgesVersion || now - edgesTime > 1000L) {
            edgesVersion = v;
            edgesTime = now;
            edges = compute();
        }
        return edges;
    }

    private Map<ClusterEdges.Edge, List<ClusterEdges.Pair>> compute() {
        ClientQuestFile file = ClientQuestFile.INSTANCE;
        if (file == null || file.selfTeamData == null || boxes.isEmpty()) {
            return Map.of();
        }
        boolean edit = file.canEdit();
        List<ClusterEdges.Node> nodes = new ArrayList<>(buttons.size());
        for (Map.Entry<Long, QuestButton> e : buttons.entrySet()) {
            Quest q = ((QuestButtonAccessor) e.getValue()).quest_clusters$getQuest();
            QuestObject shown = (QuestObject) e.getValue().moveAndDeleteFocus();
            Set<Long> deps = new HashSet<>();
            q.streamDependencies().forEach(d -> {
                if (d.isValid() && d instanceof Quest) {
                    deps.add(d.id);
                }
            });
            nodes.add(new ClusterEdges.Node(e.getKey(), q.id, deps,
                    edit || shown.isVisible(file.selfTeamData),
                    q.shouldHideDependencyLines(), q.shouldHideDependentLines()));
        }
        return ClusterEdges.computeWithPairs(nodes, id -> {
            var c = ClientClusters.store().clusterOfMember(id);
            return c.isPresent() && !ClusterViewState.isExpanded(c.get()) && boxes.containsKey(c.get().id())
                    ? OptionalLong.of(c.get().id()) : OptionalLong.empty();
        });
    }

    @Override
    public void draw(GuiGraphics g, Theme theme, int x, int y, int w, int h) {
        Map<ClusterEdges.Edge, List<ClusterEdges.Pair>> list = edges();
        if (list.isEmpty()) {
            lastDrawn = lastDrawnHidden = 0;
            return;
        }
        Chapter chapter = ClusterWidgets.chapter(questPanel);
        ClientQuestFile file = ClientQuestFile.INSTANCE;
        TeamData td = file.selfTeamData;
        boolean edit = file.canEdit();
        float half = (float) (ClusterWidgets.zoom(questPanel) * ThemeProperties.DEPENDENCY_LINE_THICKNESS.get(chapter) / 4D * 3D);

        int drawn = 0, drawnHidden = 0;
        var texture = ThemeProperties.DEPENDENCY_LINE_TEXTURE.get(chapter);
        double t = -(System.currentTimeMillis() * 0.001D);
        float scrollNormal = (float) ((t * ThemeProperties.DEPENDENCY_LINE_UNSELECTED_SPEED.get(chapter)) % 1D);
        float scrollSelected = (float) ((t * ThemeProperties.DEPENDENCY_LINE_SELECTED_SPEED.get(chapter)) % 1D);
        for (Map.Entry<ClusterEdges.Edge, List<ClusterEdges.Pair>> entry : list.entrySet()) {
            ClusterEdges.Edge e = entry.getKey();
            Widget from = widget(e.dependent());
            Widget to = widget(e.dependency());
            if (from == null || to == null || !from.shouldDraw() || !to.shouldDraw()) {
                continue;
            }
            int color = lineColor(e.dependent(), from, chapter, td);
            float scroll = scrollNormal;

            LineRule.Visibility vis = visibility(file, chapter, entry.getValue());
            boolean hidden = false;
            if (vis != LineRule.Visibility.SHOW) {
                if (edit) {
                    color = QuestEnhanceCompat.HIDDEN_LINE_EDIT_COLOR;
                    hidden = true;
                } else if (vis == LineRule.Visibility.HIDDEN || !(from.isMouseOver() || to.isMouseOver())) {
                    continue;
                }
            }
            // hover highlight, like FTB Quests: what the hovered end requires / is required for
            if (!hidden && from.isMouseOver()) {
                color = ThemeProperties.DEPENDENCY_LINE_REQUIRES_COLOR.get(chapter).rgba();
                scroll = scrollSelected;
            } else if (!hidden && to.isMouseOver()) {
                color = ThemeProperties.DEPENDENCY_LINE_REQUIRED_FOR_COLOR.get(chapter).rgba();
                scroll = scrollSelected;
            }
            ClusterRender.texturedLine(g,
                    from.getX() + from.width / 2D, from.getY() + from.height / 2D,
                    to.getX() + to.width / 2D, to.getY() + to.height / 2D,
                    half, ClusterRender.darker(color), color, scroll, texture);
            drawn++;
            if (hidden) drawnHidden++;
        }
        lastDrawn = drawn;
        lastDrawnHidden = drawnHidden;
    }

    /** Quest Enhance can hide single dependency lines; a merged line is hidden only if all lines behind it are. */
    private static LineRule.Visibility visibility(ClientQuestFile file, Chapter chapter, List<ClusterEdges.Pair> pairs) {
        if (!QuestEnhanceCompat.active()) {
            return LineRule.Visibility.SHOW;
        }
        int hidden = 0, reveal = 0;
        for (ClusterEdges.Pair p : pairs) {
            if (file.getBase(p.dependentMovable()) instanceof Movable a && file.getBase(p.dependencyMovable()) instanceof Movable b) {
                var setting = QuestEnhanceCompat.hiddenLine(chapter, a, b);
                if (setting.isPresent()) {
                    hidden++;
                    if (setting.get().revealOnHover()) {
                        reveal++;
                    }
                }
            }
        }
        return LineRule.decide(pairs.size(), hidden, reveal);
    }

    private Widget widget(ClusterEdges.End end) {
        return end.cluster() ? boxes.get(end.id()) : buttons.get(end.id());
    }

    /** Coloured by the dependent end, like FTB Quests. */
    private static int lineColor(ClusterEdges.End dependent, Widget w, Chapter chapter, TeamData td) {
        boolean complete, unavailable;
        if (w instanceof ClusterBoxWidget box) {
            ClusterStatus s = box.status();
            complete = s.allComplete();
            unavailable = !complete && !s.startable() && s.completed() == 0;
        } else {
            Quest q = ((QuestButtonAccessor) w).quest_clusters$getQuest();
            unavailable = !td.canStartTasks(q);
            complete = !unavailable && td.isCompleted(q);
        }
        Color4I c = complete ? ThemeProperties.DEPENDENCY_LINE_COMPLETED_COLOR.get(chapter)
                : unavailable ? ThemeProperties.DEPENDENCY_LINE_UNAVAILABLE_COLOR.get(chapter)
                : ThemeProperties.DEPENDENCY_LINE_UNCOMPLETED_COLOR.get(chapter);
        return c.rgba();
    }
}
