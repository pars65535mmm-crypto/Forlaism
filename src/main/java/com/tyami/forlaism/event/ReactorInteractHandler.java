package com.tyami.forlaism.event;

import com.tyami.forlaism.Forlaism;
import com.tyami.forlaism.block.entity.InserterBlockEntity;
import com.tyami.forlaism.registry.Blocks;
import com.tyami.forlaism.registry.Items;
import com.tyami.forlaism.world.ForlaismReactorMultiblockManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 輪廻再転式量子融合炉の起動ハンドラ。
 *
 * InserterBlock.use() ではなく、Forge の RightClickBlock イベントで拾う。
 * 理由: Shift + アイテム持ちの右クリックは、バニラの設置判定に横取りされて
 *       Block#use() が呼ばれないケースがあるため。
 */
@Mod.EventBusSubscriber(modid = Forlaism.MOD_ID)
public final class ReactorInteractHandler {

    private ReactorInteractHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        var player = event.getEntity();
        ItemStack held = event.getItemStack();

        // サーバー側でのみ処理
        if (level.isClientSide) {
            return;
        }

        // 搬入機チェック
        BlockState state = level.getBlockState(pos);
        if (!state.is(Blocks.FORLAISM_INSERTER.get())) {
            return;
        }

        // リンネディウム + Shift チェック
        if (!player.isShiftKeyDown()) {
            return;
        }

        if (!held.is(Items.RINNEDIUM_INGOT.get())) {
            return;
        }

        // ここまで来たら起動処理
        BlockPos origin = ForlaismReactorMultiblockManager.originFromInserter(pos);

        // 既にアクティブ？
        if (ForlaismReactorMultiblockManager.isActive(origin)) {
            player.displayClientMessage(
                    Component.literal("§c既に起動しています。"),
                    true
            );
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }

        // 構造チェック
        if (!ForlaismReactorMultiblockManager.validateStructure(level, origin)) {
            player.displayClientMessage(
                    Component.literal("§c輪廻再転式量子融合炉の構造が正しくありません。"),
                    true
            );
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }

        // 登録 + reactorMode ON
        ForlaismReactorMultiblockManager.register(origin);

        if (level.getBlockEntity(pos) instanceof InserterBlockEntity inserter) {
            inserter.setReactorMode(true);
        }

        // 演出
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.playSound(
                    null, pos,
                    SoundEvents.BEACON_ACTIVATE,
                    SoundSource.BLOCKS,
                    1.5F, 1.0F
            );

            BlockPos center = ForlaismReactorMultiblockManager.getCenterHole(origin);

            serverLevel.sendParticles(
                    ParticleTypes.END_ROD,
                    center.getX() + 0.5,
                    center.getY() + 3.0,
                    center.getZ() + 0.5,
                    120, 1.5, 3.0, 1.5, 0.2
            );
        }

        player.displayClientMessage(
                Component.literal("§d§l輪廻再転式量子融合炉 §fが §b起動 §fした…"),
                true
        );

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}