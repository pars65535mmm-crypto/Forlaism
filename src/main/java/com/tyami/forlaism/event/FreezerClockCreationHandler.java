package com.tyami.forlaism.event;

import com.tyami.forlaism.registry.Items;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * 回る冷凍時計の登場イベントハンドラー。
 * 氷山地帯の水中に「時計」+「氷結結晶」+「ネザースター」を投げ込むと共鳴し、回る冷凍時計が出現する。
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class FreezerClockCreationHandler {

    private FreezerClockCreationHandler() {
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel sl)) return;

        // 10tick (0.5秒) に1回判定して負荷を抑える
        if (sl.getGameTime() % 10 != 0) return;

        for (ServerPlayer player : sl.players()) {
            AABB checkArea = player.getBoundingBox().inflate(32.0);
            List<ItemEntity> items = sl.getEntitiesOfClass(
                    ItemEntity.class,
                    checkArea,
                    e -> e.isAlive() && e.isInWater()
            );

            for (ItemEntity clockEntity : items) {
                if (!clockEntity.getItem().is(net.minecraft.world.item.Items.CLOCK)) {
                    continue;
                }

                BlockPos pos = clockEntity.blockPosition();
                Holder<Biome> biomeHolder = sl.getBiome(pos);
                if (!isIcebergBiome(biomeHolder)) {
                    continue;
                }

                // 半径3.0ブロック以内にある他のアイテムを検索
                AABB searchArea = clockEntity.getBoundingBox().inflate(3.0);
                List<ItemEntity> nearbyItems = sl.getEntitiesOfClass(
                        ItemEntity.class,
                        searchArea,
                        e -> e.isAlive() && e.isInWater() && e != clockEntity
                );

                ItemEntity crystalEntity = null;
                ItemEntity starEntity = null;

                for (ItemEntity nearby : nearbyItems) {
                    if (crystalEntity == null && nearby.getItem().is(Items.ICE_CRYSTAL.get())) {
                        crystalEntity = nearby;
                    } else if (starEntity == null && nearby.getItem().is(net.minecraft.world.item.Items.NETHER_STAR)) {
                        starEntity = nearby;
                    }
                }

                if (crystalEntity != null && starEntity != null) {
                    // 素材を1つずつ消費
                    consumeOne(clockEntity);
                    consumeOne(crystalEntity);
                    consumeOne(starEntity);

                    double x = clockEntity.getX();
                    double y = clockEntity.getY() + 0.5;
                    double z = clockEntity.getZ();

                    // 召喚エフェクト
                    sl.playSound(null, x, y, z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 2.0F, 1.2F);
                    sl.playSound(null, x, y, z, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 2.0F, 0.8F);
                    sl.playSound(null, x, y, z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 2.0F, 0.5F);
                    sl.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 2.0F, 1.0F);

                    sl.sendParticles(ParticleTypes.FLASH, x, y, z, 5, 0.1, 0.1, 0.1, 0.0);
                    sl.sendParticles(ParticleTypes.SNOWFLAKE, x, y, z, 150, 1.2, 1.5, 1.2, 0.15);
                    sl.sendParticles(ParticleTypes.SPLASH, x, y, z, 100, 1.0, 1.5, 1.0, 0.2);
                    sl.sendParticles(ParticleTypes.END_ROD, x, y, z, 50, 0.8, 1.2, 0.8, 0.08);

                    // 回る冷凍時計をドロップ
                    ItemStack result = new ItemStack(Items.FREEZER_CLOCK.get());
                    ItemEntity resultEntity = new ItemEntity(sl, x, y + 0.3, z, result);
                    resultEntity.setDeltaMovement(0, 0.25, 0);
                    sl.addFreshEntity(resultEntity);

                    // 付近のプレイヤーにアナウンス
                    for (ServerPlayer p : sl.players()) {
                        if (p.distanceToSqr(x, y, z) <= 48 * 48) {
                            p.displayClientMessage(
                                    Component.literal("§b§l氷海の極冷と星の輝きが共鳴し、【回る冷凍時計】が水底より結晶化した！")
                                            .withStyle(ChatFormatting.AQUA),
                                    false
                            );
                        }
                    }

                    break;
                }
            }
        }
    }

    private static void consumeOne(ItemEntity entity) {
        ItemStack stack = entity.getItem();
        stack.shrink(1);
        if (stack.isEmpty()) {
            entity.discard();
        } else {
            entity.setItem(stack);
        }
    }

    /**
     * 氷山地帯（凍結海洋、深凍結海洋、凍結峰、ギザギザ山頂、氷樹等）の判定。
     */
    private static boolean isIcebergBiome(Holder<Biome> biomeHolder) {
        if (biomeHolder.is(Biomes.FROZEN_OCEAN)
                || biomeHolder.is(Biomes.DEEP_FROZEN_OCEAN)
                || biomeHolder.is(Biomes.FROZEN_PEAKS)
                || biomeHolder.is(Biomes.JAGGED_PEAKS)
                || biomeHolder.is(Biomes.ICE_SPIKES)
                || biomeHolder.is(Biomes.SNOWY_PLAINS)
                || biomeHolder.is(Biomes.SNOWY_SLOPES)) {
            return true;
        }

        return biomeHolder.unwrapKey()
                .map(ResourceKey::location)
                .map(loc -> {
                    String path = loc.getPath().toLowerCase();
                    return path.contains("frozen") || path.contains("ice") || path.contains("jagged");
                })
                .orElse(false);
    }
}
