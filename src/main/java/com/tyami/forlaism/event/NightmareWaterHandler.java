package com.tyami.forlaism.event;

import com.tyami.forlaism.world.NightmareHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ナイトメアモード中の水仕様。
 *
 * ・水バケツで水を置くと、その水源は「有限水源」となり
 *   一定時間（tick）経過後に消滅する。
 *
 * ・水流は普通に広がるが、水源だけが枯れる。
 *
 * → 無限水源が作れない。
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class NightmareWaterHandler {

    /** 水バケツで置いた水源の寿命（tick）。5秒 = 100tick。 */
    public static final int WATER_LIFETIME_TICKS = 100;

    /** 有限水源リスト: (ディメンション, 座標) → 設置tick。 */
    private static final Map<String, Long> FINITE_WATER = new ConcurrentHashMap<>();

    private NightmareWaterHandler() {
    }

    // =========================================================
    // 水バケツで設置
    // =========================================================

    @SubscribeEvent
    public static void onUseBucket(PlayerInteractEvent.RightClickBlock event) {

        if (event.getLevel().isClientSide) return;
        if (!NightmareHelper.isActive(event.getLevel())) return;

        ItemStack stack = event.getItemStack();
        if (!stack.is(Items.WATER_BUCKET)) return;

        // バニラの設置処理は通しつつ、設置位置を記録する必要があるので
        // 次のtickでチェックする（設置はバニラ側で行われる）
        Level level = event.getLevel();
        BlockPos clickedPos = event.getPos();
        Direction face = event.getFace();

        if (face == null) return;

        BlockPos waterPos = clickedPos.relative(face);

        // 次のtickで「そこに水があるか」確認して記録
        if (level instanceof ServerLevel sl) {
            sl.getServer().execute(() -> {
                BlockState state = level.getBlockState(waterPos);
                if (state.is(Blocks.WATER)
                        && state.getFluidState().isSource()) {
                    markFiniteWater(level, waterPos);
                }
            });
        }
    }

    // =========================================================
    // 有限水源の登録
    // =========================================================

    private static void markFiniteWater(Level level, BlockPos pos) {
        String key = dimKey(level) + ":" + pos.asLong();
        FINITE_WATER.put(key, level.getGameTime());
    }

    // =========================================================
    // 毎tick処理: 寿命切れの水源を消す
    // =========================================================

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (FINITE_WATER.isEmpty()) return;

        var server = event.getServer();
        if (server == null) return;

        var it = FINITE_WATER.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            String key = entry.getKey();
            long placedAt = entry.getValue();

            // key から座標を復元
            int sep = key.lastIndexOf(':');
            String dim = key.substring(0, sep);
            long packed = Long.parseLong(key.substring(sep + 1));
            BlockPos pos = BlockPos.of(packed);

            ServerLevel level = null;
            for (ServerLevel l : server.getAllLevels()) {
                if (dimKey(l).equals(dim)) {
                    level = l;
                    break;
                }
            }

            if (level == null) {
                it.remove();
                continue;
            }

            long elapsed = level.getGameTime() - placedAt;

            if (elapsed >= WATER_LIFETIME_TICKS) {
                // 水源を消す
                BlockState state = level.getBlockState(pos);
                if (state.is(Blocks.WATER)
                        && state.getFluidState().isSource()) {
                    level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());

                    level.playSound(
                            null,
                            pos.getX() + 0.5,
                            pos.getY() + 0.5,
                            pos.getZ() + 0.5,
                            SoundEvents.FIRE_EXTINGUISH,
                            SoundSource.BLOCKS,
                            0.6F, 1.5F
                    );
                }
                it.remove();
            }
        }
    }

    // =========================================================
    // ヘルパー
    // =========================================================

    private static String dimKey(Level level) {
        return level.dimension().location().toString();
    }
}