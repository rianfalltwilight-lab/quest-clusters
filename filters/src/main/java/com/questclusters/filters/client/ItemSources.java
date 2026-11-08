package com.questclusters.filters.client;

import com.questclusters.filters.filter.ItemListFilter;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Item lists for the pickers (creative inventory contents or the player's inventory) and searching them. */
public final class ItemSources {
    /** Data that changes with use or is stored content: not part of what "kind" of item a stack is. */
    static final Set<DataComponentType<?>> NOT_VARIANT = Set.of(DataComponents.DAMAGE, DataComponents.REPAIR_COST,
            DataComponents.CHARGED_PROJECTILES, DataComponents.WRITABLE_BOOK_CONTENT, DataComponents.CONTAINER,
            DataComponents.BUNDLE_CONTENTS, DataComponents.BLOCK_ENTITY_DATA, DataComponents.BEES,
            DataComponents.CONTAINER_LOOT, DataComponents.DEBUG_STICK_STATE);

    /** A picker entry: a display stack and the filter entry text it stands for. */
    public record Choice(ItemStack stack, String text, String searchName, String searchId) {
    }

    private static List<Choice> all, allVariants;
    private static Object cacheKey;

    private ItemSources() {
    }

    public static void clearCache() {
        all = allVariants = null;
    }

    public static DataComponentPatch variantPatch(ItemStack stack) {
        DataComponentPatch.Builder b = DataComponentPatch.builder();
        for (Map.Entry<DataComponentType<?>, java.util.Optional<?>> e : stack.getComponentsPatch().entrySet()) {
            if (NOT_VARIANT.contains(e.getKey()) || e.getValue().isEmpty()) continue;
            set(b, e.getKey(), e.getValue().get());
        }
        return b.build();
    }

    @SuppressWarnings("unchecked")
    private static <T> void set(DataComponentPatch.Builder b, DataComponentType<T> type, Object v) {
        b.set(type, (T) v);
    }

    static Choice choice(ItemStack stack, boolean variant) {
        var mc = Minecraft.getInstance();
        var reg = mc.level.registryAccess();
        Item item = stack.getItem();
        String text = variant ? ItemListFilter.textOf(item, variantPatch(stack), reg) : BuiltInRegistries.ITEM.getKey(item).toString();
        ItemStack shown = variant ? stack.copyWithCount(1) : new ItemStack(item);
        return new Choice(shown, text, shown.getHoverName().getString().toLowerCase(Locale.ROOT), text.toLowerCase(Locale.ROOT));
    }

    /** Everything in the creative inventory ("search" tab), one per item or one per variant. */
    public static List<Choice> all(boolean variants) {
        var mc = Minecraft.getInstance();
        Object key = mc.level;
        if (key != cacheKey) {
            clearCache();
            cacheKey = key;
        }
        if ((variants ? allVariants : all) == null) {
            var player = mc.player;
            CreativeModeTabs.tryRebuildTabContents(player.connection.enabledFeatures(),
                    player.canUseGameMasterBlocks() && mc.options.operatorItemsTab().get(), mc.level.registryAccess());
            Map<String, Choice> out = new LinkedHashMap<>();
            for (ItemStack s : CreativeModeTabs.searchTab().getDisplayItems()) {
                Choice c = choice(s, variants);
                if (c.text().length() <= ItemListFilter.MAX_ENTRY_LENGTH) out.putIfAbsent(c.text(), c);
            }
            if (variants) allVariants = new ArrayList<>(out.values());
            else all = new ArrayList<>(out.values());
        }
        return variants ? allVariants : all;
    }

    public static List<Choice> inventory(boolean variants) {
        Map<String, Choice> out = new LinkedHashMap<>();
        var inv = Minecraft.getInstance().player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty()) {
                Choice c = choice(s, variants);
                out.putIfAbsent(c.text(), c);
            }
        }
        return new ArrayList<>(out.values());
    }

    // ------------------------------------------------------------------ search

    private static Method pinyin;
    private static boolean pinyinChecked;

    /** Name or id contains the query; "@mod" limits to a namespace; pinyin too when Just Enough Characters is installed. */
    public static boolean matches(Choice c, String query) {
        String q = query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) return true;
        String mod = null;
        if (q.startsWith("@")) {
            int sp = q.indexOf(' ');
            mod = sp < 0 ? q.substring(1) : q.substring(1, sp);
            q = sp < 0 ? "" : q.substring(sp + 1).trim();
            if (!c.searchId().startsWith(mod)) return false;
        }
        if (q.isEmpty()) return true;
        if (c.searchName().contains(q) || c.searchId().contains(q)) return true;
        return pinyin(c.searchName(), q);
    }

    private static boolean pinyin(String name, String q) {
        if (!pinyinChecked) {
            pinyinChecked = true;
            try {
                pinyin = Class.forName("me.towdium.jecharacters.utils.Match").getMethod("contains", String.class, CharSequence.class);
            } catch (Throwable ignored) {
            }
        }
        if (pinyin == null) return false;
        try {
            return (Boolean) pinyin.invoke(null, name, q);
        } catch (Throwable t) {
            pinyin = null;
            return false;
        }
    }
}
