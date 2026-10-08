package com.tyami.forlaism.event;

import com.tyami.forlaism.registry.Items;
import com.tyami.forlaism.world.DreamCollapseData;
import com.tyami.forlaism.world.DreamDimension;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Dream崩壊イベント。
 *
 * 目覚め薬をDreamで飲むと startCollapse() が呼ばれ、
 * 30秒かけて世界が崩壊、最後にプレイヤーを上空へ弾き飛ばし、
 * オーバーワールドのリスポーン地点へ送る。
 *
 * 崩壊後は DreamCollapseData に「破壊済み」フラグが立ち、
 * 二度とDreamへは入れなくなる。
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class DreamCollapseHandler {

    public static final int COLLAPSE_DURATION = 600; // 30秒
    private static final Random RANDOM = new Random();

    /** 崩壊進行中のプレイヤー。UUID -> 残りtick。 */
    private static final Map<UUID, Integer> COLLAPSING = new HashMap<>();

    /** 崩壊前に保存した dream_floor の座標（復元用 / 二重起動防止）。 */
    private static final Map<UUID, BlockPos> COLLAPSE_CENTER = new HashMap<>();

    private DreamCollapseHandler() {
    }

    // =========================================================
    // 開始
    // =========================================================

    public static void startCollapse(ServerPlayer player) {
        COLLAPSING.put(player.getUUID(), COLLAPSE_DURATION);
        COLLAPSE_CENTER.put(player.getUUID(), player.blockPosition());

        // 開始演出
        if (player.level() instanceof ServerLevel sl) {
            sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.END_PORTAL_SPAWN, SoundSource.AMBIENT, 3.0F, 0.3F);
        }

        player.displayClientMessage(
                Component.literal("§4§l……… 何かが、終わる。")
                        .withStyle(net.minecraft.ChatFormatting.DARK_RED),
                false
        );
    }

    public static boolean isCollapsing(ServerPlayer player) {
        return COLLAPSING.containsKey(player.getUUID());
    }

    // =========================================================
    // 毎tick処理
    // =========================================================

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (COLLAPSING.isEmpty()) return;

        var server = event.getServer();
        if (server == null) return;

        var iterator = COLLAPSING.entrySet().iterator();

        while (iterator.hasNext()) {
            var entry = iterator.next();
            UUID uuid = entry.getKey();
            int remaining = entry.getValue() - 1;

            ServerPlayer player = server.getPlayerList().getPlayer(uuid);

            if (player == null || !player.isAlive()) {
                iterator.remove();
                COLLAPSE_CENTER.remove(uuid);
                continue;
            }

            float progress = 1.0F - (remaining / (float) COLLAPSE_DURATION);

            tickCollapse(player, remaining, progress);

            if (remaining <= 0) {
                ejectPlayer(player, server);
                iterator.remove();
                COLLAPSE_CENTER.remove(uuid);
            } else {
                entry.setValue(remaining);
            }
        }
    }

    // =========================================================
    // 崩壊演出本体（狂気）
    // =========================================================

    private static void tickCollapse(ServerPlayer player, int remaining, float progress) {

        if (!(player.level() instanceof ServerLevel sl)) return;

        double cx = player.getX();
        double cy = player.getY();
        double cz = player.getZ();
        BlockPos center = COLLAPSE_CENTER.get(player.getUUID());

        // =========================================================
        // 1. パーティクル: 段階的にヤバくなる
        // =========================================================

        // 基本: PORTALの渦
        sl.sendParticles(
                ParticleTypes.PORTAL,
                cx, cy + 1.0, cz,
                (int) (20 + progress * 100),
                3.0 + progress * 10.0,
                2.0 + progress * 5.0,
                3.0 + progress * 10.0,
                0.5
        );

        // 空中に END_ROD が舞う
        sl.sendParticles(
                ParticleTypes.END_ROD,
                cx, cy + 3.0, cz,
                (int) (10 + progress * 60),
                5.0 + progress * 15.0,
                5.0 + progress * 10.0,
                5.0 + progress * 15.0,
                0.1
        );

        // 40%以降: SOUL_FIRE
        if (progress > 0.4F) {
            sl.sendParticles(
                    ParticleTypes.SOUL_FIRE_FLAME,
                    cx, cy + 1.0, cz,
                    (int) (20 + progress * 40),
                    5.0, 3.0, 5.0,
                    0.2
            );
        }

        // 60%以降: DRAGON_BREATH
        if (progress > 0.6F) {
            sl.sendParticles(
                    ParticleTypes.DRAGON_BREATH,
                    cx, cy + 1.0, cz,
                    (int) (30 + progress * 50),
                    8.0, 5.0, 8.0,
                    0.5
            );
        }

        // 80%以降: FLASH 連発
        if (progress > 0.8F && remaining % 3 == 0) {
            sl.sendParticles(
                    ParticleTypes.FLASH,
                    cx, cy + 1.0, cz,
                    5,
                    6.0, 4.0, 6.0,
                    0
            );
        }

        // 95%以降: EXPLOSION_EMITTER を乱発
        if (progress > 0.95F && remaining % 2 == 0) {
            sl.sendParticles(
                    ParticleTypes.EXPLOSION_EMITTER,
                    cx + (RANDOM.nextDouble() - 0.5) * 10,
                    cy + RANDOM.nextDouble() * 5,
                    cz + (RANDOM.nextDouble() - 0.5) * 10,
                    1,
                    0, 0, 0,
                    0
            );
        }

        // =========================================================
        // 2. 稲妻を落とす（見た目のみ）
        // =========================================================

        // 40%以降: 5tickごとにランダムな位置に稲妻
        if (progress > 0.4F && remaining % 5 == 0) {
            for (int i = 0; i < 3; i++) {
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl);
                if (bolt != null) {
                    bolt.moveTo(
                            cx + (RANDOM.nextDouble() - 0.5) * 30,
                            cy,
                            cz + (RANDOM.nextDouble() - 0.5) * 30
                    );
                    bolt.setVisualOnly(true);
                    sl.addFreshEntity(bolt);
                }
            }
        }

        // 70%以降: プレイヤー直上に稲妻
        if (progress > 0.7F && remaining % 20 == 0) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl);
            if (bolt != null) {
                bolt.moveTo(cx, cy + 20, cz);
                bolt.setVisualOnly(true);
                sl.addFreshEntity(bolt);
            }
        }

        // =========================================================
        // 3. 音: 段階的に激化
        // =========================================================

        // 基本BGM的轟音
        if (remaining % 40 == 0) {
            float pitch = 0.3F + progress * 0.5F;
            sl.playSound(
                    null, cx, cy, cz,
                    SoundEvents.ENDER_DRAGON_GROWL,
                    SoundSource.AMBIENT,
                    3.0F,
                    pitch
            );
        }

        // 心音的な低音
        if (remaining % 20 == 0) {
            sl.playSound(
                    null, cx, cy, cz,
                    SoundEvents.WARDEN_HEARTBEAT,
                    SoundSource.AMBIENT,
                    2.0F,
                    0.5F
            );
        }

        // 30%以降: 雷鳴
        if (progress > 0.3F && remaining % 60 == 0) {
            sl.playSound(
                    null, cx, cy, cz,
                    SoundEvents.LIGHTNING_BOLT_THUNDER,
                    SoundSource.AMBIENT,
                    5.0F,
                    0.5F
            );
        }

        // 80%以降: ウィザースポーン
        if (progress > 0.8F && remaining % 100 == 0) {
            sl.playSound(
                    null, cx, cy, cz,
                    SoundEvents.WITHER_SPAWN,
                    SoundSource.AMBIENT,
                    4.0F,
                    0.5F
            );
        }

        // 95%以降: カウントダウン音
        if (progress > 0.95F && remaining % 10 == 0) {
            sl.playSound(
                    null, cx, cy, cz,
                    SoundEvents.BEACON_ACTIVATE,
                    SoundSource.AMBIENT,
                    2.0F,
                    2.0F
            );
        }

        // =========================================================
        // 4. プレイヤーにデバフ
        // =========================================================

        player.addEffect(new MobEffectInstance(
                MobEffects.CONFUSION,
                100,
                1,
                false, false, false
        ));

        if (progress > 0.5F) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.BLINDNESS,
                    60,
                    0,
                    false, false, false
            ));
        }

        // =========================================================
        // 5. ブロック崩壊（見た目のみ）
        // =========================================================

        if (progress > 0.2F && remaining % 15 == 0) {
            for (int i = 0; i < 3; i++) {
                BlockPos pos = center.offset(
                        RANDOM.nextInt(20) - 10,
                        RANDOM.nextInt(5) - 2,
                        RANDOM.nextInt(20) - 10
                );

                if (!sl.getBlockState(pos).isAir()) {
                    // ★ BLOCK → CRIT に変更
                    sl.sendParticles(
                            ParticleTypes.CRIT,
                            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            20,
                            0.5, 0.5, 0.5,
                            0.1
                    );
                }
            }
        }

        // =========================================================
        // 6. 段階メッセージ
        // =========================================================

        if (remaining == 550) {
            player.displayClientMessage(
                    Component.literal("§5§l空が、 §d軋み §5始めた…"),
                    false
            );
        }
        if (remaining == 480) {
            player.displayClientMessage(
                    Component.literal("§5§l地面が、 §d悲鳴 §5を上げている…"),
                    false
            );
        }
        if (remaining == 400) {
            player.displayClientMessage(
                    Component.literal("§4§l空間が、 §c裂け §4始めた…！"),
                    false
            );
        }
        if (remaining == 300) {
            player.displayClientMessage(
                    Component.literal("§4§l世界の端が、 §c消えていく…！"),
                    false
            );
        }
        if (remaining == 200) {
            player.displayClientMessage(
                    Component.literal("§4§l§nもう、戻れない。"),
                    false
            );
        }
        if (remaining == 100) {
            player.displayClientMessage(
                    Component.literal("§c§l10…"),
                    false
            );
        }
        if (remaining == 80) {
            player.displayClientMessage(
                    Component.literal("§c§l9… 8… 7…"),
                    false
            );
        }
        if (remaining == 60) {
            player.displayClientMessage(
                    Component.literal("§c§l6… 5… 4…"),
                    false
            );
        }
        if (remaining == 40) {
            player.displayClientMessage(
                    Component.literal("§4§l3… 2… 1…"),
                    false
            );
        }
        if (remaining == 20) {
            player.displayClientMessage(
                    Component.literal("§4§l§n……… 目を、覚ませ。"),
                    false
            );
        }
    }

    // =========================================================
    // プレイヤンを弾き飛ばす
    // =========================================================

    private static void ejectPlayer(ServerPlayer player, net.minecraft.server.MinecraftServer server) {

        // =========================================================
        // DreamCollapseData に「破壊済み」フラグを立てる
        // =========================================================
        DreamCollapseData.get(server).markCollapsed();

        ServerLevel overworld = server.overworld();

        // =========================================================
        // 帰還先を決定
        // =========================================================
        BlockPos targetPos;
        if (player.getRespawnPosition() != null
                && player.getRespawnDimension().equals(Level.OVERWORLD)) {
            targetPos = player.getRespawnPosition();
        } else {
            targetPos = overworld.getSharedSpawnPos();
        }

        // =========================================================
        // 上空 200m から落下
        // =========================================================
        double spawnX = targetPos.getX() + 0.5;
        double spawnY = targetPos.getY() + 200.0;
        double spawnZ = targetPos.getZ() + 0.5;

        // 一旦Dream内で盛大に爆散させてから転送
        if (player.level() instanceof ServerLevel dream) {
            dream.sendParticles(
                    ParticleTypes.EXPLOSION_EMITTER,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    20,
                    3.0, 3.0, 3.0,
                    0
            );
            dream.sendParticles(
                    ParticleTypes.FLASH,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    30,
                    2.0, 2.0, 2.0,
                    0
            );
            dream.playSound(
                    null,
                    player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENDER_DRAGON_DEATH,
                    SoundSource.AMBIENT,
                    5.0F, 0.5F
            );
        }

        // 転送
        player.teleportTo(
                overworld,
                spawnX, spawnY, spawnZ,
                player.getYRot(),
                player.getXRot()
        );

        player.setDeltaMovement(0, -3.5D, 0);
        player.hurtMarked = true;
        player.fallDistance = 0.0F;

        // 落下ダメージ対策（着地まで無効化）
        player.addEffect(new MobEffectInstance(
                MobEffects.SLOW_FALLING,
                300, 0, false, false, false
        ));

        // 着地後に「挑戦」を配置
        placeChallenge(overworld, targetPos);

        // =========================================================
        // 帰還演出
        // =========================================================
        overworld.sendParticles(
                ParticleTypes.EXPLOSION_EMITTER,
                spawnX, spawnY, spawnZ,
                10,
                5.0, 5.0, 5.0,
                0
        );

        overworld.sendParticles(
                ParticleTypes.FLASH,
                spawnX, spawnY, spawnZ,
                20,
                3.0, 3.0, 3.0,
                0
        );

        overworld.playSound(
                null,
                spawnX, spawnY, spawnZ,
                SoundEvents.END_PORTAL_SPAWN,
                SoundSource.PLAYERS,
                3.0F, 0.5F
        );

        // =========================================================
        // メッセージ
        // =========================================================
        player.displayClientMessage(
                Component.literal("§d§l目が覚めた…")
                        .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE),
                false
        );

        player.displayClientMessage(
                Component.literal("§5何かが、 §dスポーン地点 §5に落ちている…")
                        .withStyle(net.minecraft.ChatFormatting.DARK_PURPLE),
                false
        );

        player.displayClientMessage(
                Component.literal("§8§o夢は、もう戻らない。")
                        .withStyle(net.minecraft.ChatFormatting.DARK_GRAY, net.minecraft.ChatFormatting.ITALIC),
                false
        );
    }

    /**
     * 挑戦をスポーン地点に配置する。
     */
    private static void placeChallenge(ServerLevel overworld, BlockPos pos) {
        // 3秒後に配置（プレイヤーが着地するのを待つ）
        overworld.getServer().execute(() -> {
            BlockPos safePos = findSafePos(overworld, pos);

            ItemStack challenge = new ItemStack(Items.CHALLENGE.get());
            ItemEntity itemEntity = new ItemEntity(
                    overworld,
                    safePos.getX() + 0.5,
                    safePos.getY() + 1.0,
                    safePos.getZ() + 0.5,
                    challenge
            );
            itemEntity.setNoPickUpDelay();
            itemEntity.setGlowingTag(true);
            itemEntity.setNeverPickUp();  // 一旦PickUp不可にする（安全のため）
            overworld.addFreshEntity(itemEntity);

            // 5秒後にPickUp可能にする
            overworld.getServer().execute(() -> {
                itemEntity.setNoPickUpDelay();
            });
        });
    }

    private static BlockPos findSafePos(ServerLevel level, BlockPos pos) {
        // 上から下へ走査して地面を探す
        for (int y = level.getMaxBuildHeight() - 1; y > level.getMinBuildHeight(); y--) {
            BlockPos check = new BlockPos(pos.getX(), y, pos.getZ());
            if (!level.getBlockState(check).isAir()) {
                return check.above();
            }
        }
        return pos;
    }
}