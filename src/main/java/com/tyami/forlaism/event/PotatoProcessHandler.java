package com.tyami.forlaism.event;

import com.tyami.forlaism.item.PotatoProcessItem;
import com.tyami.forlaism.registry.Fluids;
import com.tyami.forlaism.registry.Items;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * ジャガイモ加工のワールド処理。
 *
 * 1. 水(バニラ水源)に投げ入れ → 洗った
 * 2. 足場(固体ブロック)の上に放置 2分 → 乾燥した切った
 * 3. 温泉に投げ入れ → こんにゃく
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class PotatoProcessHandler {

    private PotatoProcessHandler() {
    }

    // =========================================================
    // 水に入れたら洗う
    // =========================================================

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        ItemEntity entity = event.getEntity();
        ItemStack stack = entity.getItem();

        if (!(stack.getItem() instanceof PotatoProcessItem)) return;
        if (PotatoProcessItem.getStage(stack) != PotatoProcessItem.STAGE_RAW) return;

        // 投げた瞬間はまだ水じゃないので、ワールド側のtickで判定する
        // → ここでは何もしない
    }

    // =========================================================
    // 毎tick処理（洗う・乾燥・こんにゃく化）
    // =========================================================

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.level.isClientSide) return;
        if (!(event.level instanceof ServerLevel serverLevel)) return;

        // パフォーマンスのため2tickに1回
        if (serverLevel.getGameTime() % 2 != 0) return;

        // プレイヤー周辺だけ舐める（広すぎると重い）
        // → 全ロードチャンクのItemEntityを舐めるのは重いので、
        //    簡易的にプレイヤー周辺のアイテムだけチェック
        serverLevel.players().forEach(player -> {

            AABB area = player.getBoundingBox().inflate(32.0);

            List<ItemEntity> items = serverLevel.getEntitiesOfClass(
                    ItemEntity.class,
                    area,
                    e -> e.isAlive()
                            && !e.getItem().isEmpty()
                            && e.getItem().getItem() instanceof PotatoProcessItem
            );

            for (ItemEntity entity : items) {
                processItem(serverLevel, entity);
            }
        });
    }

    private static void processItem(ServerLevel level, ItemEntity entity) {

        ItemStack stack = entity.getItem();
        int stage = PotatoProcessItem.getStage(stack);

        BlockPos pos = entity.blockPosition();

        // =========================================================
        // STAGE_RAW: 水に触れたら洗う
        // =========================================================
        if (stage == PotatoProcessItem.STAGE_RAW) {
            FluidState fluid = level.getFluidState(pos);
            if (fluid.is(net.minecraft.world.level.material.Fluids.WATER)
                    && fluid.isSource()) {

                PotatoProcessItem.setStage(stack, PotatoProcessItem.STAGE_WASHED);
                entity.setItem(stack);

                level.playSound(null, pos,
                        SoundEvents.GENERIC_SPLASH,
                        SoundSource.NEUTRAL, 0.8F, 1.2F);
            }
            return;
        }

        // =========================================================
        // STAGE_CUT: 固体ブロックの上で2分放置 → 乾燥
        // =========================================================
        if (stage == PotatoProcessItem.STAGE_CUT) {

            BlockPos below = pos.below();
            BlockState belowState = level.getBlockState(below);

            // 「足場の上」= 下に固体ブロックがある & 自分は空気中
            boolean onSolid =
                    !belowState.isAir()
                            && belowState.isSolidRender(level, below)
                            && level.getBlockState(pos).isAir()
                            && entity.onGround();

            if (onSolid) {
                int progress = PotatoProcessItem.getDryProgress(stack) + 2; // 2tick刻み

                if (progress >= PotatoProcessItem.DRY_REQUIRED_TICKS) {
                    PotatoProcessItem.setStage(stack, PotatoProcessItem.STAGE_DRIED);
                    entity.setItem(stack);

                    level.playSound(null, pos,
                            SoundEvents.CROP_BREAK,
                            SoundSource.NEUTRAL, 1.0F, 1.0F);
                } else {
                    PotatoProcessItem.setDryProgress(stack, progress);
                    // NBT更新を確実に反映
                    entity.setItem(stack);
                }
            } else {
                // 足場から離れたら進捗リセット（任意）
                // → 厳しくしたいならここで0に
                // PotatoProcessItem.setDryProgress(stack, 0);
            }
            return;
        }

        // =========================================================
        // STAGE_HARDENED: 温泉に触れたらこんにゃくに！
        // =========================================================
        if (stage == PotatoProcessItem.STAGE_HARDENED) {

            FluidState fluid = level.getFluidState(pos);

            if (fluid.getType() == Fluids.ONSEN.get()
                    || fluid.getType() == Fluids.ONSEN_FLOWING.get()) {

                ItemStack konnyaku = new ItemStack(Items.KONNYAKU.get(), stack.getCount());

                entity.setItem(konnyaku);

                level.playSound(null, pos,
                        SoundEvents.SLIME_BLOCK_BREAK,
                        SoundSource.NEUTRAL, 1.0F, 0.7F);

                level.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        entity.getX(), entity.getY() + 0.3, entity.getZ(),
                        15, 0.3, 0.3, 0.3, 0.02
                );
            }
        }
    }
}