package com.tyami.forlaism.event;

import com.tyami.forlaism.registry.Items;
import com.tyami.forlaism.world.WorldTimeFreezeManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 回る冷凍時計の「即時復活」ハンドラー (fakeDream風)。
 * 時計を所持しているか、あるいは全世界時間停止中であれば、
 * プレイヤーが死んだ瞬間に死亡を否定し、HP全快でその場に即時復活する。
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class FreezerClockRevivalHandler {

    private FreezerClockRevivalHandler() {
    }

    /**
     * プレイヤーが回る冷凍時計を持っているか、または時間停止中かを判定。
     */
    public static boolean shouldRevive(Player player) {
        if (player == null) return false;
        if (WorldTimeFreezeManager.isFrozen()) return true;

        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.is(Items.FREEZER_CLOCK.get())) {
                return true;
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (!stack.isEmpty() && stack.is(Items.FREEZER_CLOCK.get())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 致死ダメージを受けた瞬間に検知し、死亡処理に入る前に即座にHP全回復させる。
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (shouldRevive(player) && event.getAmount() >= player.getHealth()) {
                event.setCanceled(true);
                event.setAmount(0.0F);
                revivePlayer(player);
            }
        }
    }

    /**
     * 死亡イベント時にも検知し、死亡をキャンセルして即時復活させる。
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (shouldRevive(player)) {
                if (event.isCancelable()) {
                    event.setCanceled(true);
                }
                revivePlayer(player);
            }
        }
    }

    /**
     * 即時復活処理本体 (fakeDream風の演出)。
     */
    public static void revivePlayer(ServerPlayer player) {
        player.setHealth(player.getMaxHealth());
        player.removeAllEffects();
        player.clearFire();
        player.setRemainingFireTicks(0);
        player.setAirSupply(player.getMaxAirSupply());
        player.resetFallDistance();
        player.setDeltaMovement(0, 0.1, 0);
        player.invulnerableTime = 60; // 3秒間無敵

        ServerLevel sl = player.serverLevel();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        // 特殊SE
        sl.playSound(null, x, y, z, SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.8F, 0.8F);
        sl.playSound(null, x, y, z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 2.0F, 0.5F);
        sl.playSound(null, x, y, z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.0F, 1.2F);
        sl.playSound(null, x, y, z, SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 1.2F, 1.8F);

        // パーティクル演出
        sl.sendParticles(ParticleTypes.FLASH, x, y + 1.0, z, 5, 0.2, 0.2, 0.2, 0.0);
        sl.sendParticles(ParticleTypes.SNOWFLAKE, x, y + 1.0, z, 100, 1.0, 1.2, 1.0, 0.15);
        sl.sendParticles(ParticleTypes.REVERSE_PORTAL, x, y + 1.0, z, 60, 0.8, 1.0, 0.8, 0.2);
        sl.sendParticles(ParticleTypes.END_ROD, x, y + 1.0, z, 40, 0.6, 1.0, 0.6, 0.08);

        // クライアントメッセージ
        player.displayClientMessage(
                Component.literal("§b§l【回る冷凍時計】§f 死の瞬間、事象が巻き戻り即座に蘇生した…！")
                        .withStyle(ChatFormatting.AQUA),
                true
        );
    }
}
