package com.tyami.forlaism.event;

import com.tyami.forlaism.network.BloodMoonSyncPacket;
import com.tyami.forlaism.network.FactotumPacketHandler;
import com.tyami.forlaism.world.BloodMoonManager;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;

/**
 * ブラッドムーン本体の挙動。
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class BloodMoonHandler {

    // モブ強化用
    private static final UUID BM_ARMOR_UUID  = UUID.fromString("b1000000-0000-0000-0000-000000000001");
    private static final UUID BM_HP_UUID     = UUID.fromString("b1000000-0000-0000-0000-000000000002");
    private static final UUID BM_SPEED_UUID  = UUID.fromString("b1000000-0000-0000-0000-000000000003");
    private static final UUID BM_FOLLOW_UUID = UUID.fromString("b1000000-0000-0000-0000-000000000004");
    private static final UUID BM_ATTACK_UUID = UUID.fromString("b1000000-0000-0000-0000-000000000005");

    private BloodMoonHandler() {
    }

    // =========================================================
    // 発動 / 解除 API（外部から呼ぶ）
    // =========================================================

    public static void start(ServerLevel level) {
        BloodMoonManager.start(level);

        // 全クライアントへ同期
        FactotumPacketHandler.CHANNEL.send(
                PacketDistributor.ALL.noArg(),
                new BloodMoonSyncPacket(true)
        );

        // メッセージ
        for (ServerPlayer p : level.players()) {
            p.displayClientMessage(
                    Component.literal("§4§l空が、血に染まる…")
                            .withStyle(net.minecraft.ChatFormatting.DARK_RED),
                    false
            );
        }
    }

    public static void stop(ServerLevel level) {
        BloodMoonManager.stop(level);

        FactotumPacketHandler.CHANNEL.send(
                PacketDistributor.ALL.noArg(),
                new BloodMoonSyncPacket(false)
        );

        for (ServerPlayer p : level.players()) {
            p.displayClientMessage(
                    Component.literal("§7血の月が沈んだ…")
                            .withStyle(net.minecraft.ChatFormatting.DARK_GRAY),
                    false
            );
        }
    }

    // =========================================================
    // サーバーtick: 夜明け解除
    // =========================================================

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        var server = event.getServer();
        if (server == null) return;
        if (server.getTickCount() % 20 != 0) return; // 1秒ごと

        for (ServerLevel level : server.getAllLevels()) {
            if (!BloodMoonManager.isActive(level)) continue;

            long time = level.getDayTime() % 24000L;
            // 明け方（13000未満）になったら解除
            if (time < 13000L) {
                stop(level);
            }
        }
    }

    // =========================================================
    // 寝れない
    // =========================================================

    @SubscribeEvent
    public static void onSleep(PlayerSleepInBedEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        if (!(player.level() instanceof ServerLevel sl)) return;
        if (!BloodMoonManager.isActive(sl)) return;

        event.setResult(Player.BedSleepingProblem.OTHER_PROBLEM);

        player.displayClientMessage(
                Component.literal("§4§l今日は寝れそうにない..")
                        .withStyle(net.minecraft.ChatFormatting.DARK_RED),
                true
        );
    }

    // =========================================================
    // モブスポーン時強化
    // =========================================================

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onMobJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) return;
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        if (!BloodMoonManager.isActive(sl)) return;
        if (!(event.getEntity() instanceof Monster monster)) return;

        var armor = monster.getAttribute(Attributes.ARMOR);
        if (armor != null && armor.getModifier(BM_ARMOR_UUID) != null) return;

        applyBloodMoonBuffs(monster);
    }

    public static void applyBloodMoonBuffs(LivingEntity living) {

        // 防御力 ×3
        AttributeInstance armor = living.getAttribute(Attributes.ARMOR);
        if (armor != null) {
            addModifier(armor, BM_ARMOR_UUID, "bm_armor",
                    armor.getBaseValue() * 2.0D, AttributeModifier.Operation.ADDITION);
        }

        // HP ×3
        AttributeInstance hp = living.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) {
            addModifier(hp, BM_HP_UUID, "bm_hp",
                    hp.getBaseValue() * 2.0D, AttributeModifier.Operation.ADDITION);
            if (living instanceof Mob mob) {
                mob.setHealth((float) hp.getValue());
            }
        }

        // 移動速度 ×1.3
        AttributeInstance speed = living.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            addModifier(speed, BM_SPEED_UUID, "bm_speed",
                    0.30D, AttributeModifier.Operation.MULTIPLY_TOTAL);
        }

        // 検知範囲 ×5
        AttributeInstance follow = living.getAttribute(Attributes.FOLLOW_RANGE);
        if (follow != null) {
            addModifier(follow, BM_FOLLOW_UUID, "bm_follow",
                    follow.getBaseValue() * 4.0D, AttributeModifier.Operation.ADDITION);
        }

        // 攻撃力 ×2
        AttributeInstance atk = living.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atk != null) {
            addModifier(atk, BM_ATTACK_UUID, "bm_attack",
                    atk.getBaseValue() * 1.0D, AttributeModifier.Operation.ADDITION);
        }

        // 火炎耐性（永続）
        living.addEffect(new MobEffectInstance(
                MobEffects.FIRE_RESISTANCE, Integer.MAX_VALUE, 0,
                false, false, false
        ));

        // タグ（AI Mixin が参照）
        living.getPersistentData().putBoolean("ForlaismBloodMoonBuffed", true);
    }

    private static void addModifier(
            AttributeInstance attr, UUID uuid, String name,
            double amount, AttributeModifier.Operation op
    ) {
        if (attr == null) return;
        if (attr.getModifier(uuid) != null) return;
        attr.addPermanentModifier(new AttributeModifier(uuid, name, amount, op));
    }
}