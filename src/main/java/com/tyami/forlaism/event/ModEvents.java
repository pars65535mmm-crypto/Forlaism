package com.tyami.forlaism.event;

import com.tyami.forlaism.Forlaism;
import com.tyami.forlaism.registry.Blocks;
import com.tyami.forlaism.registry.Fluids;
import com.tyami.forlaism.registry.Items;
import com.tyami.forlaism.world.TimeAccelerationManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.UUID;


import com.google.common.collect.Multimap;
import com.google.common.collect.HashMultimap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.event.entity.player.PlayerEvent;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = Forlaism.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModEvents {

        private static final UUID HALO_BACK_SLOT_UUID =
            UUID.fromString("7f6d5e4c-3b2a-4910-8f7e-6d5c4b3a2910");

    private static class ClickRecord {
        long windowStart;
        int count;

        ClickRecord(long start) {
            this.windowStart = start;
            this.count = 1;
        }
    }

    private static final Map<BlockPos, ClickRecord> CLICK_RECORDS = new ConcurrentHashMap<>();

    /**
     * 連打クリックによるブロック破砕処理
     * 結晶ブロック: 1秒18クリック -> 粉末5個 + 周囲プレイヤーに13貫通ダメージ
     * 鉄ブロック: 1秒10クリック -> 鉄の粉9個
     */
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide) return;

        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        long now = System.currentTimeMillis();

        if (state.is(Blocks.FORLAISM_CRYSTAL_BLOCK.get())) {
            ClickRecord record = CLICK_RECORDS.compute(pos, (k, v) -> {
                if (v == null || now - v.windowStart > 1000) {
                    return new ClickRecord(now);
                }
                v.count++;
                return v;
            });

            if (record != null && record.count >= 18) {
                CLICK_RECORDS.remove(pos);
                level.destroyBlock(pos, false);

                // フォラリスの粉末 x5 ドロップ
                ItemStack drop = new ItemStack(Items.FORLAISM_POWDER.get(), 5);
                ItemEntity dropEntity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop);
                level.addFreshEntity(dropEntity);

                // 周囲プレイヤーに13の防具貫通ダメージ
                AABB range = new AABB(pos).inflate(6.0);
                List<Player> nearbyPlayers = level.getEntitiesOfClass(Player.class, range);
                for (Player p : nearbyPlayers) {
                    p.hurt(p.damageSources().fellOutOfWorld(), 13.0F);
                }

                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 1.5F, 0.5F);
            }
        } else if (state.is(net.minecraft.world.level.block.Blocks.IRON_BLOCK)) {
            ClickRecord record = CLICK_RECORDS.compute(pos, (k, v) -> {
                if (v == null || now - v.windowStart > 1000) {
                    return new ClickRecord(now);
                }
                v.count++;
                return v;
            });

            if (record != null && record.count >= 10) {
                CLICK_RECORDS.remove(pos);
                level.destroyBlock(pos, false);

                // 鉄の粉 x9 ドロップ
                ItemStack drop = new ItemStack(Items.CRUSHED_IRON.get(), 9);
                ItemEntity dropEntity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop);
                level.addFreshEntity(dropEntity);

                level.playSound(null, pos, SoundEvents.ANVIL_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        }
    }

    /**
     * 鉄インゴット右クリック粉砕 (方法2): 10ダメージ & 鉄の粉 1個
     */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Level level = event.getLevel();
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();

        if (stack.is(net.minecraft.world.item.Items.IRON_INGOT)) {
            if (!level.isClientSide) {
                player.hurt(player.damageSources().generic(), 10.0F);

                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }

                if (player.isAlive()) {
                    ItemStack ironDust = new ItemStack(Items.CRUSHED_IRON.get());
                    if (!player.getInventory().add(ironDust)) {
                        player.drop(ironDust, false);
                    }
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.8F, 1.2F);
                }
            }
            event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
            event.setCanceled(true);
        }
    }

    /**
     * 右クリックによるブロック変換:
     * 1. 鋼鉄ブロック + 培養液入り瓶 -> 高結晶ブロック + 空き瓶
     * 2. 高結晶ブロック + ネザースター or 賢者の石？ -> 多結晶体 x3
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        InteractionHand hand = event.getHand();

        // 1. 鋼鉄ブロック + 培養液入り瓶
        if (state.is(Blocks.STEEL_BLOCK.get()) && stack.is(Items.FORLAISM_CULTURE_BOTTLE.get())) {
            if (!level.isClientSide) {
                level.setBlock(pos, Blocks.FORLAISM_HIGH_CRYSTAL_BLOCK.get().defaultBlockState(), 3);
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.2F, 1.0F);

                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                    ItemStack bottle = new ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE);
                    if (!player.getInventory().add(bottle)) {
                        player.drop(bottle, false);
                    }
                }
            }
            event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
            event.setCanceled(true);
            return;
        }

        // 2. 高結晶ブロック + ネザースター / 賢者の石？
        if (state.is(Blocks.FORLAISM_HIGH_CRYSTAL_BLOCK.get()) &&
                (stack.is(net.minecraft.world.item.Items.NETHER_STAR) || stack.is(Items.SAGE_STONE.get()))) {
            if (!level.isClientSide) {
                level.destroyBlock(pos, false);
                ItemStack polycrystal = new ItemStack(Items.FORLAISM_POLYCRYSTAL.get(), 3);
                ItemEntity drop = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, polycrystal);
                level.addFreshEntity(drop);

                level.playSound(null, pos, SoundEvents.TOTEM_USE, SoundSource.BLOCKS, 1.0F, 1.0F);

                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
            event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
            event.setCanceled(true);
        }
    }

    /**
     * 通常バケツで多結晶養液を汲もうとすると無敵貫通即死
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onFillBucket(FillBucketEvent event) {
        Level level = event.getLevel();
        HitResult target = event.getTarget();

        if (target != null && target.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = ((BlockHitResult) target).getBlockPos();
            FluidState fluidState = level.getFluidState(pos);

            if (fluidState.getType() == Fluids.FORLAISM_POLYCRYSTAL_SOLUTION.get()) {
                ItemStack bucket = event.getEmptyBucket();
                if (bucket.is(net.minecraft.world.item.Items.BUCKET)) {
                    event.setCanceled(true);
                    if (!level.isClientSide && event.getEntity() != null) {
                        // 通常バケツでは回収不可＆無敵貫通即死
                        event.getEntity().kill();
                    }
                }
            }
        }
    }

    /**
     * アイテム投げ入れによる液体変換 & 刻乃杖の時間加速処理
     */
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Level level = event.level;
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) return;

        // 時間加速マネージャーの更新
        TimeAccelerationManager.onLevelTick(level);

        // アイテム投げ入れチェック (20 ticksごと = 1秒に1回チェックでパフォーマンスを維持)
        if (serverLevel.getGameTime() % 10 == 0) {
            List<ItemEntity> items = serverLevel.getEntitiesOfClass(ItemEntity.class,
                    new AABB(-30000000, -64, -30000000, 30000000, 320, 30000000),
                    e -> e.isAlive() && !e.getItem().isEmpty() &&
                            (e.getItem().is(Items.FORLAISM_CRUDE_POWDER.get()) || e.getItem().is(Items.FORLAISM_POLYCRYSTAL.get())));

            for (ItemEntity itemEntity : items) {
                BlockPos pos = itemEntity.blockPosition();
                FluidState fluidState = level.getFluidState(pos);
                ItemStack stack = itemEntity.getItem();

                // 粗粉 -> 水源を培養液に変換
                if (stack.is(Items.FORLAISM_CRUDE_POWDER.get())) {
                    if (fluidState.is(net.minecraft.world.level.material.Fluids.WATER) && fluidState.isSource()) {
                        level.setBlock(pos, Blocks.FORLAISM_CULTURE.get().defaultBlockState(), 3);
                        stack.shrink(1);
                        if (stack.isEmpty()) itemEntity.discard();
                        level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0F, 1.0F);
                    }
                }
                // 多結晶体 -> 培養液を多結晶養液に変換
                else if (stack.is(Items.FORLAISM_POLYCRYSTAL.get())) {
                    if (fluidState.getType() == Fluids.FORLAISM_CULTURE.get() && fluidState.isSource()) {
                        level.setBlock(pos, Blocks.FORLAISM_POLYCRYSTAL_SOLUTION.get().defaultBlockState(), 3);
                        stack.shrink(1);
                        if (stack.isEmpty()) itemEntity.discard();
                        level.playSound(null, pos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 0.8F, 1.5F);
                    }
                }
            }
        }

// 量子転送プールを定期保存
if (serverLevel.getGameTime() % 100 == 0) {
    com.tyami.forlaism.quantum.QuantumTransferData.get(serverLevel.getServer()).setDirty();
}

        // Warpゴーストの寿命管理（100tick後に消す）
        if (serverLevel.getGameTime() % 5 == 0) {
            var ghosts = serverLevel.getEntitiesOfClass(
                    net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(
                            -30000000, -64, -30000000,
                            30000000, 320, 30000000
                    ),
                    e -> e.getPersistentData().getBoolean("ForlaismWarpGhost")
            );

            for (var ghost : ghosts) {
                if (ghost.tickCount > 100) {
                    ghost.discard();
                }
            }
        }
    }





/* 
@SubscribeEvent
public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
    CuriosApi.getCuriosInventory(event.getEntity()).ifPresent(curios -> {
        curios.growSlotType("back", 1);
    });
}
    */




private static final UUID GARBAGE_METAL_SPEED_UUID =
        UUID.fromString("8b5c8c45-5b0a-4f3c-9f36-2d1b5a8e7c11");

private static final UUID CONS_STEEL_ATTACK_UUID =
        UUID.fromString("9c6d9e56-6c1b-4a4d-a047-3e2c6b9f8d22");

@SubscribeEvent
public static void onPlayerTick(TickEvent.PlayerTickEvent event) {

    if (event.phase != TickEvent.Phase.END) {
        return;
    }

    Player player = event.player;

    // =========================================================
    // コミメタルフルセットで移動速度 +10%
    // =========================================================

    boolean garbageFullSet =
            player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(
                    com.tyami.forlaism.registry.Items.GARBAGE_METAL_HELMET.get()
            )
            && player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).is(
                    com.tyami.forlaism.registry.Items.GARBAGE_METAL_CHESTPLATE.get()
            )
            && player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS).is(
                    com.tyami.forlaism.registry.Items.GARBAGE_METAL_LEGGINGS.get()
            )
            && player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).is(
                    com.tyami.forlaism.registry.Items.GARBAGE_METAL_BOOTS.get()
            );

    AttributeInstance speed =
            player.getAttribute(Attributes.MOVEMENT_SPEED);

    if (speed != null) {
        AttributeModifier existing =
                speed.getModifier(GARBAGE_METAL_SPEED_UUID);

        if (garbageFullSet) {
            if (existing == null) {
                speed.addTransientModifier(
                        new AttributeModifier(
                                GARBAGE_METAL_SPEED_UUID,
                                "Garbage Metal full set speed",
                                0.1D,
                                AttributeModifier.Operation.MULTIPLY_TOTAL
                        )
                );
            }
        } else {
            if (existing != null) {
                speed.removeModifier(GARBAGE_METAL_SPEED_UUID);
            }
        }
    }

    // =========================================================
    // コンスチールフルセットで攻撃力 +5
    // =========================================================

    boolean consSteelFullSet =
            player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(
                    com.tyami.forlaism.registry.Items.CONS_STEEL_HELMET.get()
            )
            && player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).is(
                    com.tyami.forlaism.registry.Items.CONS_STEEL_CHESTPLATE.get()
            )
            && player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS).is(
                    com.tyami.forlaism.registry.Items.CONS_STEEL_LEGGINGS.get()
            )
            && player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).is(
                    com.tyami.forlaism.registry.Items.CONS_STEEL_BOOTS.get()
            );

    AttributeInstance attackDamage =
            player.getAttribute(Attributes.ATTACK_DAMAGE);

    if (attackDamage != null) {
        AttributeModifier existingAttack =
                attackDamage.getModifier(CONS_STEEL_ATTACK_UUID);

        if (consSteelFullSet) {
            if (existingAttack == null) {
                attackDamage.addTransientModifier(
                        new AttributeModifier(
                                CONS_STEEL_ATTACK_UUID,
                                "Cons Steel full set attack",
                                15.0D,
                                AttributeModifier.Operation.ADDITION
                        )
                );
            }
        } else {
            if (existingAttack != null) {
                attackDamage.removeModifier(CONS_STEEL_ATTACK_UUID);
            }
        }
    }
}
    @SubscribeEvent
    public static void onServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        if (event.getServer() == null) return;

        com.tyami.forlaism.quantum.WarpDeliveryData.tick(event.getServer());
    }

}
