package com.tyami.forlaism.item;

import com.tyami.forlaism.network.FactotumPacketHandler;
import com.tyami.forlaism.network.JillstlairleatDeathPacket;
import com.tyami.forlaism.registry.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ジルスレイト所持時の死亡シーケンスおよび自壊（Shift+右クリック）シーケンスの管理イベント。
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class JillstlairleatDeathEventHandler {

    /** 死亡シーケンス中のプレイヤー残りtick (500 -> 0) */
    private static final Map<UUID, Integer> DEATH_SEQUENCE_PLAYERS = new ConcurrentHashMap<>();

    /** 自壊シーケンス中のプレイヤー残りtick (35 -> 0) */
    private static final Map<UUID, Integer> SELF_DESTRUCT_PLAYERS = new ConcurrentHashMap<>();

    private JillstlairleatDeathEventHandler() {
    }

    public static boolean hasJillstlairleat(Player player) {
        if (player.getMainHandItem().is(Items.JILLSTLAIRLEAT.get()) ||
                player.getOffhandItem().is(Items.JILLSTLAIRLEAT.get())) {
            return true;
        }
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(Items.JILLSTLAIRLEAT.get())) {
                return true;
            }
        }
        return false;
    }

    /**
     * ジルスレイトを持った状態で死亡した時:
     * リスポーン画面を出さず、20秒暗転 + 5秒待機ののち青い塵となって砕け散り、リスポーンする。
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        if (hasJillstlairleat(player)) {
            // 即座の死亡をキャンセルし、25秒の仮死シーケンスへ移行
            event.setCanceled(true);

            player.setHealth(1.0F);
            player.setInvulnerable(true);
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 520, 255, false, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 520, 255, false, false, false));

            DEATH_SEQUENCE_PLAYERS.put(player.getUUID(), 500);

            // クライアントへ暗転開始パケットを送信
            FactotumPacketHandler.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new JillstlairleatDeathPacket()
            );
        }
    }

    /**
     * Shift+右クリック自壊シーケンスの開始。
     */
    public static void triggerSelfDestruct(ServerPlayer player) {
        player.setInvulnerable(true);
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 255, false, false, false));
        SELF_DESTRUCT_PLAYERS.put(player.getUUID(), 35);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var server = event.getServer();
        if (server == null) return;

        // 1. 死亡シーケンス更新
        if (!DEATH_SEQUENCE_PLAYERS.isEmpty()) {
            for (Map.Entry<UUID, Integer> entry : DEATH_SEQUENCE_PLAYERS.entrySet()) {
                UUID uuid = entry.getKey();
                int ticks = entry.getValue() - 1;
                ServerPlayer player = server.getPlayerList().getPlayer(uuid);

                if (player == null) {
                    DEATH_SEQUENCE_PLAYERS.remove(uuid);
                    continue;
                }

                if (ticks <= 0) {
                    DEATH_SEQUENCE_PLAYERS.remove(uuid);

                    // ジルスレイトが砕け散って青い塵となる
                    ServerLevel sLevel = player.serverLevel();
                    sLevel.sendParticles(
                            ParticleTypes.SOUL_FIRE_FLAME,
                            player.getX(), player.getY() + 1.0, player.getZ(),
                            50, 0.5, 0.5, 0.5, 0.05
                    );

                    // インベントリからジルスレイトをすべて削除
                    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                        ItemStack stack = player.getInventory().getItem(i);
                        if (stack.is(Items.JILLSTLAIRLEAT.get())) {
                            player.getInventory().setItem(i, ItemStack.EMPTY);
                        }
                    }

                    // 普通にリスポーン
                    respawnPlayer(player);
                } else {
                    DEATH_SEQUENCE_PLAYERS.put(uuid, ticks);
                }
            }
        }

        // 2. 自壊シーケンス更新
        if (!SELF_DESTRUCT_PLAYERS.isEmpty()) {
            for (Map.Entry<UUID, Integer> entry : SELF_DESTRUCT_PLAYERS.entrySet()) {
                UUID uuid = entry.getKey();
                int ticks = entry.getValue() - 1;
                ServerPlayer player = server.getPlayerList().getPlayer(uuid);

                if (player == null) {
                    SELF_DESTRUCT_PLAYERS.remove(uuid);
                    continue;
                }

                if (ticks == 20) {
                    // 一瞬見えたあと、消滅する演出（不可視＋青い塵）
                    player.setInvisible(true);
                    ServerLevel sLevel = player.serverLevel();
                    sLevel.sendParticles(
                            ParticleTypes.SOUL_FIRE_FLAME,
                            player.getX(), player.getY() + 1.0, player.getZ(),
                            30, 0.3, 0.5, 0.3, 0.05
                    );
                }

                if (ticks <= 0) {
                    SELF_DESTRUCT_PLAYERS.remove(uuid);
                    player.setInvisible(false);
                    // その直後リスポーン
                    respawnPlayer(player);
                } else {
                    SELF_DESTRUCT_PLAYERS.put(uuid, ticks);
                }
            }
        }
    }

    /**
     * プレイヤーをリスポーン地点へ移動させ、全快復帰させる。
     */
    private static void respawnPlayer(ServerPlayer player) {
        player.setInvulnerable(false);
        player.removeAllEffects();
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);

        ServerLevel respawnLevel = player.server.getLevel(player.getRespawnDimension());
        if (respawnLevel == null) {
            respawnLevel = player.server.overworld();
        }

        BlockPos respawnPos = player.getRespawnPosition();
        if (respawnPos != null) {
            player.teleportTo(respawnLevel, respawnPos.getX() + 0.5, respawnPos.getY() + 0.5, respawnPos.getZ() + 0.5, player.getRespawnAngle(), 0.0F);
            return;
        }

        BlockPos sharedSpawn = respawnLevel.getSharedSpawnPos();
        player.teleportTo(respawnLevel, sharedSpawn.getX() + 0.5, sharedSpawn.getY(), sharedSpawn.getZ() + 0.5, 0.0F, 0.0F);
    }
}
