package com.questclusters.mixin.forge;

import com.questclusters.compat.image.ImageId;
import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.ChapterImage;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Persistent ids for chapter images on 1.20.1: saved with the image under {@code id} in FTB Quests' own format (a
 * 16-digit hex string, which FTB Quests for 1.21.1 reads as the image's id, so books keep their ids either way),
 * sent with it to clients, and handed out by the server (from the quest file's id pool) to images that have none.
 * {@code getMovableID()} returns it too, as it does on 1.21.1.
 */
@Mixin(value = ChapterImage.class, remap = false)
public abstract class ChapterImageIdMixin implements ImageId {
    @Unique
    private static final String quest_clusters$KEY = "id";

    @Shadow
    private Chapter chapter;

    @Unique
    private long quest_clusters$id;

    @Override
    public long quest_clusters$id() {
        if (quest_clusters$id == 0L && chapter != null && chapter.file instanceof ServerQuestFile file) {
            quest_clusters$id = file.newID();
            file.markDirty();
        }
        return quest_clusters$id;
    }

    @Override
    public void quest_clusters$setId(long id) {
        quest_clusters$id = id;
    }

    @Inject(method = "getMovableID", at = @At("HEAD"), cancellable = true)
    private void quest_clusters$movableId(CallbackInfoReturnable<Long> cir) {
        cir.setReturnValue(quest_clusters$id());
    }

    @Inject(method = "writeData", at = @At("RETURN"))
    private void quest_clusters$write(CompoundTag nbt, CallbackInfoReturnable<CompoundTag> cir) {
        long id = quest_clusters$id();
        if (id != 0L) cir.getReturnValue().putString(quest_clusters$KEY, String.format("%016X", id));
    }

    @Inject(method = "readData", at = @At("TAIL"))
    private void quest_clusters$read(CompoundTag nbt, CallbackInfo ci) {
        quest_clusters$id = 0L;
        try {
            String hex = nbt.getString(quest_clusters$KEY);
            if (!hex.isEmpty()) quest_clusters$id = Long.parseUnsignedLong(hex.startsWith("#") ? hex.substring(1) : hex, 16);
        } catch (NumberFormatException ignored) {
            // a new id is handed out
        }
    }

    @Inject(method = "writeNetData", at = @At("TAIL"))
    private void quest_clusters$writeNet(FriendlyByteBuf buf, CallbackInfo ci) {
        buf.writeLong(quest_clusters$id());
    }

    @Inject(method = "readNetData", at = @At("TAIL"))
    private void quest_clusters$readNet(FriendlyByteBuf buf, CallbackInfo ci) {
        quest_clusters$id = buf.readLong();
        // a pasted copy arrives with the id of the image it was copied from: the server gives it its own
        if (quest_clusters$id != 0L && chapter != null && chapter.file instanceof ServerQuestFile file) {
            long id = quest_clusters$id;
            boolean[] taken = {false};
            file.forAllChapters(ch -> {
                for (ChapterImage other : ch.getImages()) {
                    if ((Object) other != this && ((ImageId) (Object) other).quest_clusters$id() == id) taken[0] = true;
                }
            });
            if (taken[0]) quest_clusters$id = 0L;
        }
    }
}
