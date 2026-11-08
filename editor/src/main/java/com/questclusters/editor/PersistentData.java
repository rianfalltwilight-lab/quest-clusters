package com.questclusters.editor;

import com.questclusters.editor.net.EditorNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Server side of the player persistent-data placeholder: answers with only the requested keys. */
public final class PersistentData {
    private PersistentData() {
    }

    public static void handleRequest(ServerPlayer player, List<String> keys) {
        Set<String> valid = new LinkedHashSet<>();
        for (String k : keys) {
            if (valid.size() < 64 && k != null && !k.isEmpty() && k.length() <= 128 && !k.matches(".*[\\s{}].*")) valid.add(k);
        }
        if (valid.isEmpty()) return;
        CompoundTag forge = subset(player.getPersistentData(), valid);
        CompoundTag kube = subset(kubejs(player), valid);
        EditorNetwork.sendPersistent(player, List.copyOf(valid), forge, kube);
    }

    static CompoundTag subset(CompoundTag from, Set<String> keys) {
        CompoundTag out = new CompoundTag();
        if (from != null) {
            for (String k : keys) {
                Tag t = from.get(k);
                if (t != null) out.put(k, t.copy());
            }
        }
        return out;
    }

    /** KubeJS's own per-player data, if KubeJS is installed (read reflectively). */
    public static CompoundTag kubejs(Player player) {
        try {
            Object o = player.getClass().getMethod("kjs$getPersistentData").invoke(player);
            return o instanceof CompoundTag t ? t : null;
        } catch (Throwable t) {
            return null;
        }
    }
}
