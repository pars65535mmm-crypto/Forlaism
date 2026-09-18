package com.tyami.forlaism.event;

import com.tyami.forlaism.registry.Items;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "forlaism")
public class KnifeEvolutionHandler {

    /**
     * fo_knife で村人を殺すと a_knife を入手
     */
    @SubscribeEvent
    public static void onVillagerKilled(LivingDeathEvent event) {

        if (!(event.getEntity() instanceof Villager)) return;

        if (!(event.getSource().getEntity() instanceof Player player)) return;

        ItemStack weapon = player.getMainHandItem();

        if (!weapon.is(Items.FO_KNIFE.get())) return;

        ItemStack reward = new ItemStack(Items.ASSASSIN_KNIFE.get());
        if (!player.getInventory().add(reward)) {
            player.drop(reward, false);
        }
    }

    /**
     * fo_knife を使い切ると r_knife を入手
     */
    @SubscribeEvent
    public static void onItemDestroyed(PlayerDestroyItemEvent event) {

        ItemStack original = event.getOriginal();

        if (!original.is(Items.FO_KNIFE.get())) return;

        Player player = event.getEntity();
        ItemStack reward = new ItemStack(Items.R_KNIFE.get());

        if (!player.getInventory().add(reward)) {
            player.drop(reward, false);
        }
    }
}