package com.tyami.forlaism.enchantment;

import com.tyami.forlaism.registry.ModEnchantments;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * 無幻エンチャントの最大HP+500%を属性Modifierで付与する。
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class MugenHealthHandler {

    /** 無幻用のModifier UUID。 */
    private static final UUID MUGEN_HP_UUID =
            UUID.fromString("d3a7b1c4-1111-2222-3333-444444444401");

    private static final String MUGEN_HP_NAME = "mugen_max_health";

    private MugenHealthHandler() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {

        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        if (player.level().isClientSide) return;

        // 防具4部位の無幻合計
        int total = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET
        }) {
            ItemStack armor = player.getItemBySlot(slot);
            if (armor.isEmpty()) continue;
            int lv = EnchantmentHelper.getItemEnchantmentLevel(
                    ModEnchantments.MUGEN.get(), armor
            );
            if (lv > 0) total += lv;
        }

        AttributeInstance maxHp = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHp == null) return;

        AttributeModifier existing = maxHp.getModifier(MUGEN_HP_UUID);

        // 付与なし → 除去
        if (total <= 0) {
            if (existing != null) {
                maxHp.removeModifier(MUGEN_HP_UUID);
                // 現在HPが新しい最大値を超えていたら合わせる
                if (player.getHealth() > player.getMaxHealth()) {
                    player.setHealth(player.getMaxHealth());
                }
            }
            return;
        }

        // 無幻あり → +500%（= MULTIPLY_TOTAL で 5.0D）
        double target = 5.0D;

        if (existing == null) {
            maxHp.addPermanentModifier(new AttributeModifier(
                    MUGEN_HP_UUID, MUGEN_HP_NAME,
                    target, AttributeModifier.Operation.MULTIPLY_TOTAL
            ));
            // 現在HPも最大値に追従
            player.setHealth(player.getMaxHealth());
        } else if (existing.getAmount() != target) {
            maxHp.removeModifier(MUGEN_HP_UUID);
            maxHp.addPermanentModifier(new AttributeModifier(
                    MUGEN_HP_UUID, MUGEN_HP_NAME,
                    target, AttributeModifier.Operation.MULTIPLY_TOTAL
            ));
        }
    }
}