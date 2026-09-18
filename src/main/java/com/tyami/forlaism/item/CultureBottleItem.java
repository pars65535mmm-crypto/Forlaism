package com.tyami.forlaism.item;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class CultureBottleItem extends Item {

    public CultureBottleItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entityLiving) {
        super.finishUsingItem(stack, level, entityLiving);

        if (!level.isClientSide && entityLiving instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
            serverPlayer.awardStat(Stats.ITEM_USED.get(this));
            // 飲むと無敵貫通で死亡する
            serverPlayer.kill();
        }

        if (stack.isEmpty()) {
            return new ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE);
        } else {
            if (entityLiving instanceof Player player && !player.getAbilities().instabuild) {
                ItemStack bottle = new ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE);
                if (!player.getInventory().add(bottle)) {
                    player.drop(bottle, false);
                }
            }
            return stack;
        }
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public SoundEvent getDrinkingSound() {
        return SoundEvents.GENERIC_DRINK;
    }

    @Override
    public SoundEvent getEatingSound() {
        return SoundEvents.GENERIC_DRINK;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }
}
