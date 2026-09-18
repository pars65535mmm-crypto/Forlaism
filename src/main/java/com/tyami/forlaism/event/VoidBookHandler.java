package com.tyami.forlaism.event;

import com.tyami.forlaism.registry.Items;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;
import java.util.UUID;
import java.util.HashSet;

/**
 * 生贄の書の死亡トリガー処理。
 *
 * 条件:
 *   - 「儀式の書」というタイトルの書き込み済みの本
 *   - 中身に「生贄を捧げる」が含まれている
 *   - その本を持ったまま死ぬ
 *
 * → リスポーン地点に生贄の書が出現
 * → 拾った瞬間に10秒の盲目 + 吐き気
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class VoidBookHandler {

    /** デバフの持続時間 (10秒 = 200 tick)。 */
    private static final int DEBUFF_DURATION = 200;

    /** 儀式の書のタイトル。 */
    private static final String RITUAL_BOOK_TITLE = "儀式の書";

    /** 本文に含めるべきキーワード。 */
    private static final String RITUAL_KEYWORD = "生贄を捧げる";

    private VoidBookHandler() {
    }

    // =========================================================
    // 死亡時に本をリスポーン地点へ
    // =========================================================

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // 儀式の書を持っているか
        if (!hasRitualBook(player)) {
            return;
        }

        PendingBookData.schedule(player);
    }

    /**
     * 「儀式の書」条件を満たす本を持っているか。
     */
    private static boolean hasRitualBook(ServerPlayer player) {

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {

            ItemStack stack = player.getInventory().getItem(i);

            if (!stack.is(net.minecraft.world.item.Items.WRITTEN_BOOK)) {
                continue;
            }

            if (isValidRitualBook(stack)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 書き込み済みの本が儀式の書条件を満たすか。
     */
    private static boolean isValidRitualBook(ItemStack stack) {

        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return false;
        }

        // ---- タイトルチェック ----
        String title = tag.getString("title");
        if (!RITUAL_BOOK_TITLE.equals(title)) {
            return false;
        }

        // ---- 本文チェック ----
        if (!tag.contains("pages", Tag.TAG_LIST)) {
            return false;
        }

        ListTag pages = tag.getList("pages", Tag.TAG_STRING);

        for (int i = 0; i < pages.size(); i++) {
            String pageText = pages.getString(i);

            // 文字列にキーワードが含まれているか
            if (pageText.contains(RITUAL_KEYWORD)) {
                return true;
            }
        }

        return false;
    }

    // =========================================================
    // リスポーン時に本を出現
    // =========================================================

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!PendingBookData.consume(player)) {
            return;
        }

        spawnBookAtRespawn(player);
    }

    private static void spawnBookAtRespawn(ServerPlayer player) {

        BlockPos spawnPos = player.getRespawnPosition();
        ServerLevel respawnLevel = player.server.getLevel(player.getRespawnDimension());

        if (spawnPos == null || respawnLevel == null) {
            spawnPos = player.blockPosition();
            respawnLevel = player.serverLevel();
        }

        ItemStack book = new ItemStack(Items.BOOK_OF_SACRIFICE.get());

        ItemEntity itemEntity = new ItemEntity(
                respawnLevel,
                spawnPos.getX() + 0.5,
                spawnPos.getY() + 0.5,
                spawnPos.getZ() + 0.5,
                book
        );

        itemEntity.setNoPickUpDelay();

        respawnLevel.addFreshEntity(itemEntity);

        respawnLevel.playSound(
                null,
                spawnPos,
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.BLOCKS,
                1.0F, 0.5F
        );
    }

    // =========================================================
    // 拾った瞬間のデバフ
    // =========================================================

    @SubscribeEvent
    public static void onPickup(EntityItemPickupEvent event) {

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack stack = event.getItem().getItem();

        if (!stack.is(Items.BOOK_OF_SACRIFICE.get())) {
            return;
        }

        player.addEffect(new MobEffectInstance(
                MobEffects.BLINDNESS,
                DEBUFF_DURATION,
                0,
                false,
                true,
                true
        ));

        player.addEffect(new MobEffectInstance(
                MobEffects.CONFUSION,
                DEBUFF_DURATION,
                0,
                false,
                true,
                true
        ));

        player.displayClientMessage(
                Component.literal("§5生贄の書が… 視界を奪う…"),
                true
        );
    }

    // =========================================================
    // 死亡→リスポーンの橋渡し
    // =========================================================

    private static final class PendingBookData {

        private static final Set<UUID> PENDING = new HashSet<>();

        static void schedule(ServerPlayer player) {
            PENDING.add(player.getUUID());
        }

        static boolean consume(ServerPlayer player) {
            return PENDING.remove(player.getUUID());
        }
    }
}