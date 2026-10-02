package com.tyami.forlaism.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;

/**
 * デスメタルのツール。
 *
 * ブロックを破壊するたびに 1XP を獲得する。
 *
 * ※ DiggerItem を直接 new で使うのではなく、
 *    このクラスを PickaxeItem / AxeItem / ShovelItem / HoeItem の
 *    代わりに「そのまま使う」形にはできないので、
 *    各ツールクラスからこのロジックを呼ぶ静的メソッドを提供する方式にする。
 *    → 実際には下の DeathMetalXXXItem を参照。
 */
public final class DeathMetalToolHelper {

    private DeathMetalToolHelper() {}

    /**
     * ブロック破壊成功時に呼ぶ。1XP を付与する。
     */
    public static void awardXp(Player player) {
        if (player == null) return;
        if (player.level().isClientSide) return;
        player.giveExperiencePoints(1);
    }
}