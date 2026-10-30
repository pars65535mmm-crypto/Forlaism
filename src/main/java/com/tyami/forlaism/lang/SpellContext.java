package com.tyami.forlaism.lang;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 呪文実行時のコンテキスト。
 *
 * 誰が、どの魔道書で、どのレベルで、どこにいるか。
 * 実行エンジンはこのオブジェクトを経由して
 * Minecraft の世界に作用する。
 *
 * Minecraft 側の GrimoireItem がこのオブジェクトを組み立てる。
 */
public final class SpellContext {

    private final ServerPlayer player;
    private final ServerLevel level;
    private final ItemStack grimoire;

    public SpellContext(ServerPlayer player, ServerLevel level, ItemStack grimoire) {
        this.player = player;
        this.level = level;
        this.grimoire = grimoire;
    }

    public ServerPlayer player() {
        return player;
    }

    public ServerLevel level() {
        return level;
    }

    public ItemStack grimoire() {
        return grimoire;
    }

    /**
     * 対象選択の解決。
     *
     * TargetSelector と名詞リストから実際の LivingEntity リストを返す。
     * 実装は Minecraft 側に依存するので、ここではインターフェースだけ。
     * 実際の解決は SpellResolver に委譲する。
     */
    public List<LivingEntity> resolveTargets(
            com.tyami.forlaism.lang.effect.TargetSelector selector,
            List<String> nouns,
            Double withinBlocks
    ) {
        return SpellResolver.resolve(level, player, selector, nouns, withinBlocks);
    }
}