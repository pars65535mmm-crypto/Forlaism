package com.tyami.forlaism.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * 核の汚染エリア管理。
 *
 * - 爆心地から半径16チャンクのバイオームを汚染バイオームに置き換え
 * - 汚染エリア内のEntityに毎tickダメージ・デバフ
 * - バイオーム置き換えはtick分割で少しずつ実行
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class NucleusContaminationManager {

    /** 爆心地から半径（チャンク）。 */
    public static final int CONTAMINATION_RADIUS_CHUNKS = 16;

    /** 毎tickダメージの量（21億）。 */
    public static final float TICK_DAMAGE = 2_100_000_000.0F;

    /** 20チャンク以内の烈火判定距離。 */
    public static final double FIRE_RADIUS = 20 * 16.0;

    /** 30チャンク以内のプレイヤーダメージ距離。 */
    public static final double PLAYER_BURST_RADIUS = 30 * 16.0;

    /** 45チャンク以内のMobダメージ距離。 */
    public static final double MOB_BURST_RADIUS = 45 * 16.0;

    /** プレイヤーへの一括ダメージ。 */
    public static final float PLAYER_BURST_DAMAGE = 240.0F;

    /** Mobへの一括ダメージ。 */
    public static final float MOB_BURST_DAMAGE = 3_000_000.0F;

    /** 汚染バイオームのキー。 */
    public static final ResourceKey<Biome> CONTAMINATED_BIOME =
            ResourceKey.create(
                    Registries.BIOME,
                    new ResourceLocation("forlaism", "contaminated_wasteland")
            );

    /** 爆心地リスト（ワールド座標 + ディメンション）。 */
    private static final List<ContaminationZone> ZONES = new ArrayList<>();

    /** バイオーム置き換え待ちチャンク。 */
    private static final List<PendingChunkReplace> PENDING_REPLACE = new ArrayList<>();

    /** 1tickあたりの最大チャンク置換数。 */
    private static final int REPLACE_PER_TICK = 8;

    private NucleusContaminationManager() {
    }

    // =========================================================
    // 起爆
    // =========================================================

    public static void detonate(ServerLevel level, BlockPos center, int stackCount) {

        // スタック数に応じて半径をスケール（今回は固定16チャンク）
        int radiusChunks = CONTAMINATION_RADIUS_CHUNKS;

        // ゾーン登録
        ZONES.add(new ContaminationZone(
                level.dimension().location().toString(),
                center.immutable()
        ));

        // バイオーム置き換えをスケジュール
        scheduleBiomeReplace(level, center, radiusChunks);

        // 即時ダメージ処理（プレイヤー / Mob）
        immediateBurst(level, center);

        // 爆発演出
        level.explode(
                null,
                center.getX() + 0.5,
                center.getY() + 0.5,
                center.getZ() + 0.5,
                8.0F,
                Level.ExplosionInteraction.NONE
        );
    }

    // =========================================================
    // 即時ダメージ
    // =========================================================

    private static void immediateBurst(ServerLevel level, BlockPos center) {

        // プレイヤー：30チャンク以内 → 240ダメージ
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            if (!p.level().dimension().equals(level.dimension())) continue;

            double dist = p.blockPosition().distSqr(center);
            if (dist <= PLAYER_BURST_RADIUS * PLAYER_BURST_RADIUS) {
                p.invulnerableTime = 0;
                p.hurt(p.damageSources().explosion(p, p), PLAYER_BURST_DAMAGE);
            }
        }

        // Mob：45チャンク以内 → 3,000,000ダメージ
        var mobs = level.getEntitiesOfClass(
                Mob.class,
                new net.minecraft.world.phys.AABB(center).inflate(MOB_BURST_RADIUS)
        );
        for (Mob mob : mobs) {
            mob.invulnerableTime = 0;
            mob.hurt(mob.damageSources().explosion(null, null), MOB_BURST_DAMAGE);
        }
    }

    // =========================================================
    // バイオーム置き換えスケジュール
    // =========================================================

    private static void scheduleBiomeReplace(ServerLevel level, BlockPos center, int radiusChunks) {

        ChunkPos centerChunk = new ChunkPos(center.getX() >> 4, center.getZ() >> 4);

        for (int dx = -radiusChunks; dx <= radiusChunks; dx++) {
            for (int dz = -radiusChunks; dz <= radiusChunks; dz++) {

                int cx = centerChunk.x + dx;
                int cz = centerChunk.z + dz;

                PENDING_REPLACE.add(new PendingChunkReplace(
                        level.dimension().location().toString(),
                        cx, cz
                ));
            }
        }
    }

    // =========================================================
    // 毎tick処理
    // =========================================================

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {

        if (event.phase != TickEvent.Phase.END) return;

        var server = event.getServer();
        if (server == null) return;

        // =========================================================
        // バイオーム置き換え
        // =========================================================
        int processed = 0;
        var it = PENDING_REPLACE.iterator();

        while (it.hasNext() && processed < REPLACE_PER_TICK) {

            PendingChunkReplace pending = it.next();

            ServerLevel level = getLevel(server, pending.dim);
            if (level == null) {
                it.remove();
                continue;
            }

            // チャンクロード
            ChunkAccess chunk = level.getChunk(pending.chunkX, pending.chunkZ);
            if (chunk instanceof LevelChunk levelChunk) {
                replaceChunkBiome(level, levelChunk);
            }

            it.remove();
            processed++;
        }

        // =========================================================
        // 汚染ゾーン内のEntityに継続ダメージ
        // =========================================================
        if (server.getTickCount() % 2 != 0) return; // 2tick毎に処理（軽量化）

        for (ContaminationZone zone : ZONES) {
            ServerLevel level = getLevel(server, zone.dim);
            if (level == null) continue;

            BlockPos center = zone.center;
            double radiusBlocks = CONTAMINATION_RADIUS_CHUNKS * 16.0;
            double radiusSqr = radiusBlocks * radiusBlocks;

            // プレイヤー
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (!p.level().dimension().equals(level.dimension())) continue;

                double dist = p.blockPosition().distSqr(center);

                if (dist <= radiusSqr) {
                    applyTickEffects(p);
                } else if (dist <= FIRE_RADIUS * FIRE_RADIUS) {
                    p.setSecondsOnFire(10);
                }
            }

            // Mob
            var entities = level.getEntitiesOfClass(
                    LivingEntity.class,
                    new net.minecraft.world.phys.AABB(center).inflate(radiusBlocks)
            );

            for (LivingEntity entity : entities) {
                double dist = entity.blockPosition().distSqr(center);
                if (dist <= radiusSqr) {
                    applyTickEffects(entity);
                }
            }
        }
    }

    /**
     * 汚染エリア内のEntityに毎tick効果を適用。
     */
    private static void applyTickEffects(LivingEntity entity) {

        // デバフ
        entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 4, false, false));
        entity.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 9, false, false));
        entity.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 9, false, false));
        entity.addEffect(new MobEffectInstance(MobEffects.HUNGER, 200, 9, false, false));

        // 無敵貫通21億ダメージ
        entity.invulnerableTime = 0;
        entity.hurtTime = 0;

        // hurt() ではなく直接HPを削る（クリエイティブ・不死身貫通）
        float newHealth = Math.max(0.0F, entity.getHealth() - TICK_DAMAGE);
        entity.setHealth(newHealth);

        if (newHealth <= 0.0F && !entity.isDeadOrDying()) {
            entity.die(entity.damageSources().fellOutOfWorld());
        }
    }

    // =========================================================
    // バイオーム置き換え
    // =========================================================

private static void replaceChunkBiome(ServerLevel level, LevelChunk chunk) {

    var biomeRegistry = level.registryAccess().registryOrThrow(Registries.BIOME);
    Holder<Biome> contaminated = biomeRegistry.getHolderOrThrow(CONTAMINATED_BIOME);

    LevelChunkSection[] sections = chunk.getSections();

    for (int i = 0; i < sections.length; i++) {
        LevelChunkSection section = sections[i];
        if (section == null) continue;

        // 新しいバイオーム用の PalettedContainer を生成
        PalettedContainer<Holder<Biome>> newBiomeContainer =
                new PalettedContainer<>(
                        biomeRegistry.asHolderIdMap(),
                        contaminated,  // デフォルト値（全部これで埋まる）
                        PalettedContainer.Strategy.SECTION_BIOMES
                );

        // 既存のブロック状態コンテナはそのまま流用
        PalettedContainer<BlockState> blockStates = section.getStates();

        // 新しいセクションで置き換え
        sections[i] = new LevelChunkSection(blockStates, newBiomeContainer);
    }

    chunk.setUnsaved(true);
}

    // =========================================================
    // ユーティリティ
    // =========================================================

    private static ServerLevel getLevel(net.minecraft.server.MinecraftServer server, String dimId) {
        for (ServerLevel level : server.getAllLevels()) {
            if (level.dimension().location().toString().equals(dimId)) {
                return level;
            }
        }
        return null;
    }

    // =========================================================
    // 内部クラス
    // =========================================================

    private static class ContaminationZone {
        final String dim;
        final BlockPos center;

        ContaminationZone(String dim, BlockPos center) {
            this.dim = dim;
            this.center = center;
        }
    }

    private static class PendingChunkReplace {
        final String dim;
        final int chunkX;
        final int chunkZ;

        PendingChunkReplace(String dim, int chunkX, int chunkZ) {
            this.dim = dim;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }
    }
}