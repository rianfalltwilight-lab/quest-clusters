package com.questclusters.editor.client.desc;

import com.questclusters.editor.PersistentData;
import com.questclusters.editor.net.EditorNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Client cache of persistent-data values shown by description placeholders. */
public final class ClientPersistentData {
    private static final Map<String, Tag> FORGE = new HashMap<>(), KUBE = new HashMap<>();
    private static long lastRequest;

    private ClientPersistentData() {
    }

    /** Asks the server for these keys (at most once a second). */
    public static void request(Set<String> keys) {
        long now = System.currentTimeMillis();
        if (keys.isEmpty() || now - lastRequest < 1000L || Minecraft.getInstance().getConnection() == null) return;
        lastRequest = now;
        PacketDistributor.sendToServer(new EditorNetwork.PersistentRequest(List.copyOf(keys).subList(0, Math.min(64, keys.size()))));
    }

    public static void receive(List<String> keys, CompoundTag forge, CompoundTag kube) {
        boolean changed = false;
        for (String k : keys) {
            changed |= put(FORGE, k, forge.get(k));
            changed |= put(KUBE, k, kube.get(k));
        }
        // refresh the quest screen actually shown (an older one may still be registered with FTB Quests)
        if (changed && Minecraft.getInstance().screen instanceof dev.ftb.mods.ftblibrary.ui.ScreenWrapper w
                && w.getGui() instanceof dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen qs && qs.isViewingQuest()) {
            qs.viewQuestPanel.refreshWidgets();
        }
    }

    private static boolean put(Map<String, Tag> m, String k, Tag v) {
        Tag old = v == null ? m.remove(k) : m.put(k, v.copy());
        return v == null ? old != null : !v.equals(old);
    }

    /** Replaces every placeholder in a component tree by its value (unchanged if there is none). */
    public static Component substitute(Component c) {
        if (DescriptionComponents.placeholderKey(c) != null) return value(c);
        if (c.getSiblings().isEmpty() || !hasPlaceholder(c)) return c;
        MutableComponent out = c.plainCopy().withStyle(c.getStyle());
        c.getSiblings().forEach(s -> out.append(substitute(s)));
        return out;
    }

    private static boolean hasPlaceholder(Component c) {
        if (DescriptionComponents.placeholderKey(c) != null) return true;
        for (Component s : c.getSiblings()) if (hasPlaceholder(s)) return true;
        return false;
    }

    /** Value of a placeholder: server data, then this client's copy, then KubeJS (server, client); empty if none. */
    public static Component value(Component placeholder) {
        String key = DescriptionComponents.placeholderKey(placeholder);
        Tag t = FORGE.get(key);
        var player = Minecraft.getInstance().player;
        if (t == null && player != null) t = player.getPersistentData().get(key);
        if (t == null) t = KUBE.get(key);
        if (t == null && player != null) {
            CompoundTag k = PersistentData.kubejs(player);
            if (k != null) t = k.get(key);
        }
        if (t == null) return Component.empty();
        String v = t.getAsString();
        MutableComponent c = DescriptionComponents.placeholderI18n(placeholder) ? Component.translatableWithFallback(v, v) : Component.literal(v);
        return c.withStyle(placeholder.getStyle());
    }
}
