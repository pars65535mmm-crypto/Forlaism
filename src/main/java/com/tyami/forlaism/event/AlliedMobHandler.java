package com.tyami.forlaism.event;

import com.tyami.forlaism.item.AnbicrleberItem;
import com.tyami.forlaism.registry.Items;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 時を忘れた剣（アンビシレーバー）で敵を倒したとき、
 * その敵を「味方」として復活させる処理。
 *
 * 味方化されたMobは {@link #ALLIED_TAG} を持ち、
 * MobPacifiedMixin 側で「プレイヤーを狙わない／敵対Mobを狙う」挙動になる。
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class AlliedMobHandler {

    /** 味方化タグ。MobPacifiedMixin と揃える。 */
    public static final String ALLIED_TAG = "ForlaismAllied";

    private AlliedMobHandler() {
    }

    // =========================================================
    // 死亡時フック
    // =========================================================

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingDeath(LivingDeathEvent event) {

        // 対象は Mob のみ（プレイヤー・動物は除外してもいいが、ここではMob全般）
        if (!(event.getEntity() instanceof Mob mob)) return;

        // クライアント側除外
        if (mob.level().isClientSide) return;

        // 既に味方化済みなら何もしない（無限ループ防止）
        if (mob.getPersistentData().getBoolean(ALLIED_TAG)) return;

        // 攻撃者を取得
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        // メインハンドがアンビシレーバーか
        ItemStack weapon = player.getMainHandItem();
        if (!(weapon.getItem() instanceof AnbicrleberItem)) return;

        // サーバーレベル取得
        if (!(mob.level() instanceof ServerLevel level)) return;

        // =========================================================
        // 死亡をキャンセル → 味方として復活
        // =========================================================
        event.setCanceled(true);

        // HP全快
        mob.setHealth(mob.getMaxHealth());

        // 死にかけフラグを全部消す
        mob.deathTime = 0;
        mob.hurtTime = 0;
        mob.invulnerableTime = 0;

        // 燃えてたら消す
        mob.clearFire();
        mob.setRemainingFireTicks(0);

        // ノックバックとか速度リセット
        mob.setDeltaMovement(0, 0, 0);
        mob.hurtMarked = true;

        // タグ付与
        mob.getPersistentData().putBoolean(ALLIED_TAG, true);

        // プレイヤー関連の敵対情報をクリア
        mob.setTarget(null);
        mob.setLastHurtByMob(null);
        mob.setLastHurtByPlayer(null);
        player.setLastHurtMob(null);
        player.setLastHurtByMob(null);

        // エフェクト剥がし（毒・炎上等）
        mob.removeAllEffects();

        // =========================================================
        // 演出
        // =========================================================
        level.playSound(
                null,
                mob.getX(), mob.getY(), mob.getZ(),
                SoundEvents.BEACON_POWER_SELECT,
                SoundSource.PLAYERS,
                1.0F, 1.5F
        );

        level.playSound(
                null,
                mob.getX(), mob.getY(), mob.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS,
                1.2F, 0.8F
        );

        // 金色の粒子が舞い上がる
        level.sendParticles(
                ParticleTypes.END_ROD,
                mob.getX(),
                mob.getY() + mob.getBbHeight() * 0.5,
                mob.getZ(),
                40,
                0.5, 0.8, 0.5,
                0.08
        );

        level.sendParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                mob.getX(),
                mob.getY() + mob.getBbHeight() * 0.5,
                mob.getZ(),
                20,
                0.4, 0.6, 0.4,
                0.05
        );

        level.sendParticles(
                ParticleTypes.FLASH,
                mob.getX(),
                mob.getY() + mob.getBbHeight() * 0.5,
                mob.getZ(),
                1,
                0, 0, 0, 0
        );


    }

    // =========================================================
    // ユーティリティ
    // =========================================================

    /** このMobが味方化されているか。 */
    public static boolean isAllied(Mob mob) {
        return mob != null && mob.getPersistentData().getBoolean(ALLIED_TAG);
    }
}