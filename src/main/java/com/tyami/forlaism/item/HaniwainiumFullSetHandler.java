package com.tyami.forlaism.item;

import com.tyami.forlaism.registry.Items;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * ハニワイニウムフルセット時の防御力ボーナス。
 *
 * 各部位の防御力を 128 に合計するため、
 * フルセットのときだけ追加で防御力を加算する。
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class HaniwainiumFullSetHandler {

    /**
     * フルセット時の追加防御力。
     *
     * 部位合計 = 128 を目標にする。
     * ArmorMaterial の合計が既に 128 なので、追加は 0。
     * ただし「フルセット時に特別な追加」が必要ならここで加算する。
     *
     * ここでは「将来の拡張枠」としてUUIDだけ用意。
     */
    private static final UUID FULLSET_ARMOR_UUID =
            UUID.fromString("a1b2c3d4-aaaa-bbbb-cccc-000000000001");

    private HaniwainiumFullSetHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        if (player.level().isClientSide) return;

        boolean fullSet = isFullSet(player);

        AttributeInstance armor = player.getAttribute(Attributes.ARMOR);
        if (armor == null) return;

        AttributeModifier existing = armor.getModifier(FULLSET_ARMOR_UUID);

        if (fullSet) {
            // 現状 0 だけど、将来「フルセット時に+α」したくなったらここで数値を変更
            double bonus = 0.0D;
            if (bonus > 0 && existing == null) {
                armor.addTransientModifier(new AttributeModifier(
                        FULLSET_ARMOR_UUID,
                        "haniwainium_fullset",
                        bonus,
                        AttributeModifier.Operation.ADDITION
                ));
            }
        } else if (existing != null) {
            armor.removeModifier(FULLSET_ARMOR_UUID);
        }
    }

    public static boolean isFullSet(Player player) {
        return isSlot(player, EquipmentSlot.HEAD, Items.HANIWAINIUM_HELMET.get())
                && isSlot(player, EquipmentSlot.CHEST, Items.HANIWAINIUM_CHESTPLATE.get())
                && isSlot(player, EquipmentSlot.LEGS, Items.HANIWAINIUM_LEGGINGS.get())
                && isSlot(player, EquipmentSlot.FEET, Items.HANIWAINIUM_BOOTS.get());
    }

    private static boolean isSlot(Player player, EquipmentSlot slot, net.minecraft.world.item.Item item) {
        ItemStack s = player.getItemBySlot(slot);
        return !s.isEmpty() && s.is(item);
    }

        @net.minecraftforge.eventbus.api.SubscribeEvent(
            priority = net.minecraftforge.eventbus.api.EventPriority.HIGH)
    public static void onLivingHurt(
            net.minecraftforge.event.entity.living.LivingHurtEvent event
    ) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;
        if (!isFullSet(player)) return;

        // 128 防御力 = ダメージ 80% カット 相当（計算式による）
        // バニラの防御力キャップを超える分を独自にカット
        event.setAmount(event.getAmount() * 0.20F);
    }
}