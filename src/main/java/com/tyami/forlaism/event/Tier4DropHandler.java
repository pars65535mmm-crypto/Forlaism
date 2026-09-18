package com.tyami.forlaism.event;

import com.tyami.forlaism.registry.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber
public class Tier4DropHandler {

    private static final Random RANDOM = new Random();
    private static final int DREAM_CHANCE = 1;
    private static final int DREAM_DENOMINATOR = 5;
    private static final double VOID_Y = -63.0D;
    private static final double FLOAT_DURATION_SECONDS = 10.0D;

    // ★★★ 修正：Levelも保持する ★★★
    private static final ConcurrentHashMap<Integer, FloatData> TRACKED_ITEMS = new ConcurrentHashMap<>();

    private static class FloatData {
        final Level level;        // ← 追加！ どのディメンションで投げられたか
        final double startY;
        long startTick;
        boolean isAscending = false;

        FloatData(Level level, double startY, long startTick) {
            this.level = level;
            this.startY = startY;
            this.startTick = startTick;
        }
    }

    // ============================================================
    // 1. 夢幻核を投げたとき（追跡開始）
    // ============================================================
    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        ItemEntity itemEntity = event.getEntity();
        ItemStack stack = itemEntity.getItem();

        if (!stack.is(Items.MUGENKAKU.get())) {
            return;
        }

        if (!itemEntity.level().isClientSide) {
            Player player = event.getPlayer();
            // ★★★ 修正：Levelも保存する ★★★
            TRACKED_ITEMS.put(
                    itemEntity.getId(),
                    new FloatData(itemEntity.level(), player.getY(), -1L)
            );
        }
    }

    // ============================================================
    // 2. 毎tick処理（奈落チェック & 浮上処理）
    // ============================================================
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Level level = event.level;
        if (level.isClientSide || TRACKED_ITEMS.isEmpty()) {
            return;
        }

        Iterator<Integer> iterator = TRACKED_ITEMS.keySet().iterator();

        while (iterator.hasNext()) {
            int entityId = iterator.next();
            FloatData data = TRACKED_ITEMS.get(entityId);

            // ★★★ 修正：ディメンションが違ったらスキップ ★★★
            if (data.level != level) {
                continue;
            }

            ItemEntity itemEntity = (ItemEntity) level.getEntity(entityId);
            if (itemEntity == null || itemEntity.isRemoved()) {
                iterator.remove();
                continue;
            }

            ItemStack stack = itemEntity.getItem();

            // ---- 奈落に到達したら（抽選） ----
            if (stack.is(Items.MUGENKAKU.get()) && itemEntity.getY() < VOID_Y) {

                if (RANDOM.nextInt(DREAM_DENOMINATOR) < DREAM_CHANCE) {
                    // 虚幻核に変身！
                    itemEntity.setItem(new ItemStack(Items.KYOGENKAKU.get()));

                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(
                                ParticleTypes.PORTAL,
                                itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(),
                                30, 0.3, 0.3, 0.3, 0.1
                        );
                    }

                    itemEntity.setPos(itemEntity.getX(), -62.0D, itemEntity.getZ());
                    itemEntity.setNoGravity(true);
                    itemEntity.setDeltaMovement(0.0D, 0.0D, 0.0D);

                    data.startTick = level.getGameTime();
                    data.isAscending = true;

                } else {
                    itemEntity.discard();
                    iterator.remove();
                }
                continue;
            }

            // ---- 虚幻核が浮上中 ----
            if (stack.is(Items.KYOGENKAKU.get()) && data.isAscending) {

                double currentY = itemEntity.getY();
                double targetY = data.startY;

                if (currentY >= targetY) {
                    itemEntity.setNoGravity(false);
                    itemEntity.setDeltaMovement(0.0D, 0.0D, 0.0D);

                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(
                                ParticleTypes.END_ROD,
                                itemEntity.getX(), currentY, itemEntity.getZ(),
                                20, 0.2, 0.2, 0.2, 0.05
                        );
                    }

                    iterator.remove();
                    continue;
                }

                long elapsedTicks = level.getGameTime() - data.startTick;
                double totalTicks = FLOAT_DURATION_SECONDS * 20.0D;
                double progress = Math.min(1.0D, elapsedTicks / totalTicks);
                double easedProgress = easeInOutCubic(progress);

                double newY = -62.0D + (targetY - (-62.0D)) * easedProgress;
                itemEntity.setPos(itemEntity.getX(), newY, itemEntity.getZ());

                double deltaProgress = 0.01D;
                double nextProgress = Math.min(1.0D, progress + deltaProgress);
                double nextEased = easeInOutCubic(nextProgress);
                double speed = (nextEased - easedProgress) / deltaProgress * 0.5D;
                itemEntity.setDeltaMovement(0.0D, speed, 0.0D);

                if (level instanceof ServerLevel serverLevel && level.getGameTime() % 5 == 0) {
                    serverLevel.sendParticles(
                            ParticleTypes.END_ROD,
                            itemEntity.getX(), currentY, itemEntity.getZ(),
                            1, 0.05, 0.05, 0.05, 0.01
                    );
                }

                continue;
            }

            if (!stack.is(Items.MUGENKAKU.get()) && !stack.is(Items.KYOGENKAKU.get())) {
                iterator.remove();
            }
        }
    }

    // ============================================================
    // イージング関数
    // ============================================================
    private static double easeInOutCubic(double t) {
        if (t < 0.5D) {
            return 4.0D * t * t * t;
        } else {
            return 1.0D - Math.pow(-2.0D * t + 2.0D, 3.0D) / 2.0D;
        }
    }

    // ============================================================
    // 3. 岩盤落下死 → Tier4生成
    // ============================================================
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        Level level = player.level();
        if (level.isClientSide) {
            return;
        }

        BlockPos pos = player.blockPosition();
        BlockPos below = pos.below();

        if (!level.getBlockState(below).is(Blocks.BEDROCK)) {
            return;
        }

        int slotIndex = -1;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(Items.KYOGENKAKU.get())) {
                slotIndex = i;
                break;
            }
        }

        if (slotIndex == -1) {
            return;
        }

        ItemStack stack = player.getInventory().getItem(slotIndex);
        stack.shrink(1);
        if (stack.isEmpty()) {
            player.getInventory().setItem(slotIndex, ItemStack.EMPTY);
        }

        ServerLevel serverLevel = (ServerLevel) level;
        ItemStack madoromu = new ItemStack(Items.MADOROMU.get());
        ItemEntity itemEntity = new ItemEntity(
                serverLevel,
                below.getX() + 0.5,
                below.getY() + 1.5,
                below.getZ() + 0.5,
                madoromu
        );
        serverLevel.addFreshEntity(itemEntity);

        serverLevel.sendParticles(
                ParticleTypes.DRAGON_BREATH,
                below.getX() + 0.5,
                below.getY() + 1.0,
                below.getZ() + 0.5,
                100,
                2.0, 2.0, 2.0,
                0.2
        );

        serverLevel.sendParticles(
                ParticleTypes.END_ROD,
                below.getX() + 0.5,
                below.getY() + 1.0,
                below.getZ() + 0.5,
                50,
                1.0, 1.0, 1.0,
                0.1
        );

        player.displayClientMessage(
                Component.literal("§d§l微睡む九十九の夢 §fが §b岩盤 §fより顕現した…！"),
                true
        );
    }
}