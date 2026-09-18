package com.tyami.forlaism.item;

import com.tyami.forlaism.ForalisItem;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class PolycrystalItem extends ForalisItem {

    public PolycrystalItem(Properties properties) {
        super(properties.food(new FoodProperties.Builder().alwaysEat().nutrition(0).saturationMod(0.0F).build()));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entityLiving) {
        super.finishUsingItem(stack, level, entityLiving);

        if (!level.isClientSide && entityLiving instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
            serverPlayer.awardStat(Stats.ITEM_USED.get(this));
            // 食べると無敵貫通で即死
            serverPlayer.kill();
        }

        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.EAT;
    }

    @Override
    public SoundEvent getDrinkingSound() {
        return SoundEvents.GENERIC_EAT;
    }

    @Override
    public SoundEvent getEatingSound() {
        return SoundEvents.GENERIC_EAT;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }
}
