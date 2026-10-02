package com.tyami.forlaism.item;

import com.tyami.forlaism.damage.RinneDamageSource;
import com.tyami.forlaism.registry.Items;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * デスメタルフルセット時の輪廻ダメージ無効化処理。
 *
 * 輪廻ダメージを受けたとき、特殊効果（最大HP削り・回復阻害）を無効化し、
 * 純粋な通常ダメージとして通す。
 *
 * priority = EventPriority.HIGHEST で、輪廻の自前 Mixin より前に処理する。
 *  → LivingEntityRinneMixin (priority 2600) は hurt() の HEAD で横取りするので、
 *    ここでは hurt() に渡る前に「輪廻ダメージを普通のダメージに置換」する。
 *
 * 実装方針:
 *   LivingHurtEvent は hurt() の後なので間に合わない。
 *   代わりに player.hurt() を呼ぶ前に差し替える必要があるが、
 *   輪廻ダメージは SwordItem#hurtEnemy などから直接投げられるため、
 *   ここでは「フルセットなら輪廻の特殊効果Mixinをスキップさせる」方法を取る。
 *
 * 具体的には:
 *   - フルセット時、LivingEntityRinneMixin の処理をスキップする
 *     ための静的なフラグを立てる
 *   - ダメージはそのままバニラ処理で通す
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class DeathMetalFullSetHandler {

    /** フルセット判定中かどうかをチェックするためのThreadLocal。 */
    private static final ThreadLocal<Boolean> BYPASS_RINNE =
            ThreadLocal.withInitial(() -> false);

    private DeathMetalFullSetHandler() {}

    /** 輪廻の特殊効果をスキップすべきかどうか。 */
    public static boolean shouldBypassRinne() {
        return BYPASS_RINNE.get();
    }

    /**
     * デスメタル防具フルセットを装備しているか。
     */
    public static boolean isFullSet(LivingEntity entity) {
        if (entity == null) return false;

        ItemStack helmet = entity.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack chest  = entity.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack legs   = entity.getItemBySlot(EquipmentSlot.LEGS);
        ItemStack feet   = entity.getItemBySlot(EquipmentSlot.FEET);

        return helmet.is(Items.DEATH_METAL_HELMET.get())
                && chest.is(Items.DEATH_METAL_CHESTPLATE.get())
                && legs.is(Items.DEATH_METAL_LEGGINGS.get())
                && feet.is(Items.DEATH_METAL_BOOTS.get());
    }

    /**
     * 輪廻ダメージの特殊効果をスキップして「素のダメージ」に変換する。
     * フルセット時のみ発動。
     *
     * 呼び出し元: LivingEntityRinneMixin の hurt() 内で
     * このメソッドをチェックして、trueなら独自処理をスキップして
     * バニラの hurt() を通す。
     */
    public static boolean shouldConvertRinneToNormal(LivingEntity target, DamageSource source) {
        if (!(target instanceof Player)) return false;
        if (!source.is(RinneDamageSource.RINNE)) return false;
        return isFullSet(target);
    }
}