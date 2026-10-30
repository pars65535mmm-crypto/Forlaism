package com.tyami.forlaism.mixin;

import com.tyami.forlaism.world.WorldTimeFreezeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 全世界時間停止発動中、ServerLevelの各種tick処理をプレイヤー以外完全にスキップするMixin。
 * 機械（BlockEntity）、ブロック更新、液体流動、天候、太陽/月、Entityの動きを全て停止する。
 */
@Mixin(value = ServerLevel.class, priority = 31000)
public abstract class ServerLevelTimeFreezeMixin {

    /**
     * プレイヤー以外の全エンティティのtickを停止。
     */
    @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
    private void forlaism$timeFreezeNonPassenger(Entity entity, CallbackInfo ci) {
        if (WorldTimeFreezeManager.isFrozen() && !(entity instanceof Player)) {
            ci.cancel();
        }
    }

    /**
     * スケジュールされたブロック更新（赤石、砂落下など）を停止。
     */
    @Inject(method = "tickBlock", at = @At("HEAD"), cancellable = true)
    private void forlaism$timeFreezeTickBlock(BlockPos pos, Block block, CallbackInfo ci) {
        if (WorldTimeFreezeManager.isFrozen()) {
            ci.cancel();
        }
    }

    /**
     * スケジュールされた液体流動（水、溶岩）を停止。
     */
    @Inject(method = "tickFluid", at = @At("HEAD"), cancellable = true)
    private void forlaism$timeFreezeTickFluid(BlockPos pos, Fluid fluid, CallbackInfo ci) {
        if (WorldTimeFreezeManager.isFrozen()) {
            ci.cancel();
        }
    }

    /**
     * ランダムチャンク更新（作物の成長、氷の溶解、火の延焼）を停止。
     */
    @Inject(method = "tickChunk", at = @At("HEAD"), cancellable = true)
    private void forlaism$timeFreezeTickChunk(LevelChunk chunk, int randomTickSpeed, CallbackInfo ci) {
        if (WorldTimeFreezeManager.isFrozen()) {
            ci.cancel();
        }
    }

    /**
     * 天候サイクル（雨・雷・晴れ）の進行を停止。
     */
    @Inject(method = "advanceWeatherCycle", at = @At("HEAD"), cancellable = true)
    private void forlaism$timeFreezeWeather(CallbackInfo ci) {
        if (WorldTimeFreezeManager.isFrozen()) {
            ci.cancel();
        }
    }

    /**
     * ワールド時間（太陽と月の運行）を停止。
     */
    @Inject(method = "tickTime", at = @At("HEAD"), cancellable = true)
    private void forlaism$timeFreezeTickTime(CallbackInfo ci) {
        if (WorldTimeFreezeManager.isFrozen()) {
            ci.cancel();
        }
    }

    /**
     * Mobの自然スポーンなどを停止。
     */
    @Inject(method = "tickCustomSpawners", at = @At("HEAD"), cancellable = true)
    private void forlaism$timeFreezeSpawners(boolean spawnEnemies, boolean spawnFriendlies, CallbackInfo ci) {
        if (WorldTimeFreezeManager.isFrozen()) {
            ci.cancel();
        }
    }
}
