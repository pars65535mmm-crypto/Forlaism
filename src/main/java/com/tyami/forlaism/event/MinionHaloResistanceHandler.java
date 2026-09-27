package com.tyami.forlaism.event;

import com.tyami.forlaism.registry.Items;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 眷属の光輪を装備したMobの耐性処理。
 *
 * - setHealth / remove / discard は Mixin（MinionHaloMixin）で処理
 * - 通常ダメージ・即死系ダメージソースは ここで丸める
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class MinionHaloResistanceHandler {

    private MinionHaloResistanceHandler() {
    }

    /** 眷属の光輪を頭に装備しているか。 */
    public static boolean hasMinionHalo(LivingEntity entity) {
        ItemStack helmet = entity.getItemBySlot(EquipmentSlot.HEAD);
        return !helmet.isEmpty() && helmet.is(Items.MINION_HALO.get());
    }

    /**
     * 即死系ダメージソース（fellOutOfWorld, genericKill等）を無効化。
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingAttackEvent event) {
        LivingEntity entity = event.getEntity();
        if (!hasMinionHalo(entity)) return;

        DamageSource source = event.getSource();

        // 奈落・kill 系は完全無効
        if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)
                || source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)
                        && source.getEntity() == null) {
            event.setCanceled(true);
        }
    }

    /**
     * ダメージを丸める（10以上 → 10、それ以外は素通し）。
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (!hasMinionHalo(entity)) return;

        float amount = event.getAmount();
        if (amount > 10.0F) {
            event.setAmount(10.0F);
        }
    }
}