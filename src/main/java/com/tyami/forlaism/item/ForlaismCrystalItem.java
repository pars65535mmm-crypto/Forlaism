package com.tyami.forlaism.item;

import com.tyami.forlaism.registry.Items;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ForlaismCrystalItem extends Item {

    public ForlaismCrystalItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            RandomSource random = level.getRandom();

            // 1/5 の確率でフォラリスの結晶自体が消滅
            boolean vanishes = (random.nextInt(5) == 0);

            // 1/3 の確率で防具貫通
            boolean armorPiercing = (random.nextInt(3) == 0);

            // その1/3のケースで無敵状態無視即死 + 爆発
            boolean instantDeath = armorPiercing && (random.nextInt(3) == 0);

            if (instantDeath) {
                // 即死と爆発
                level.explode(null, player.getX(), player.getY(), player.getZ(), 3.0F, Level.ExplosionInteraction.MOB);
                player.kill();
            } else if (armorPiercing) {
                // 防具貫通19ダメージ (bypass armor damage)
                player.hurt(player.damageSources().fellOutOfWorld(), 19.0F);
            } else {
                // 通常19ダメージ
                player.hurt(player.damageSources().generic(), 19.0F);
            }

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }

            if (!vanishes && player.isAlive()) {
                // 成功: フォラリスの粗粉を生成
                ItemStack powder = new ItemStack(Items.FORLAISM_CRUDE_POWDER.get());
                if (!player.getInventory().add(powder)) {
                    player.drop(powder, false);
                }
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
            } else if (vanishes) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 0.8F);
            }

            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
