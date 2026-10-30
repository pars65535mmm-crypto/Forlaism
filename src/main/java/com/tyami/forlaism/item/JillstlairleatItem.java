package com.tyami.forlaism.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.tyami.forlaism.damage.RevengeDamageSource;
import com.tyami.forlaism.network.FactotumPacketHandler;
import com.tyami.forlaism.network.JillstlairleatSelfDestructPacket;
import com.tyami.forlaism.registry.Items;
import com.tyami.forlaism.registry.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.List;

/**
 * ジルスレイト。
 *
 * 基礎ダメージ: 1
 * 攻撃速度: 1.6
 * 耐久値: 0になった瞬間1に戻る（絶対に壊れない）
 *
 * 【特性】
 *  - 所持中: orcliam (空回り) エフェクトが発動し、10ブロック先が見えない濃霧が立ち込める
 *  - 攻撃時: プレイ時間1時間につき+1ダメージ加算のリベンジダメージ
 *  - 所持死亡時: 20秒暗転 + 5秒待機ののち青い塵となり砕け散って通常リスポーン
 *  - Shift+右クリック: 自らを砕き、カメラが離脱して消滅・リスポーン。その場に耐久値-1のジルスレイトを残す
 */
public class JillstlairleatItem extends Item {

    public static final int MAX_DURABILITY = 1000;

    public JillstlairleatItem(Properties properties) {
        super(properties.durability(MAX_DURABILITY).fireResistant());
    }

    /**
     * 耐久値が0になった瞬間1に戻る。
     */
    @Override
    public void setDamage(ItemStack stack, int damage) {
        if (damage >= getMaxDamage(stack)) {
            // 壊れる瞬間、耐久値1（残り耐久1）へ復帰
            super.setDamage(stack, getMaxDamage(stack) - 1);
        } else {
            super.setDamage(stack, damage);
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getDamage(stack) > 0;
    }

    /**
     * 所持中: orcliam (空回り) エフェクトを継続付与。
     */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (entity instanceof LivingEntity living) {
            boolean isHolding = living.getMainHandItem() == stack || living.getOffhandItem() == stack;
            if (isHolding) {
                // 持っている間は orcliam (空回り) を付与
                living.addEffect(new MobEffectInstance(
                        ModEffects.ORCLIAM.get(),
                        30, 0, false, false, true
                ));
            }
        }
        super.inventoryTick(stack, level, entity, slot, selected);
    }

    /**
     * 攻撃時: プレイ時間加算のリベンジダメージ。
     */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide) {
            float baseDamage = 1.0F;
            float extraDamage = 0.0F;

            if (attacker instanceof ServerPlayer serverPlayer) {
                // プレイヤーの総プレイ時間（ticks）を取得 (72000 ticks = 1時間)
                int playTimeTicks = serverPlayer.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
                extraDamage = (float) playTimeTicks / 72000.0F;
            }

            float totalDamage = baseDamage + extraDamage;
            target.hurt(RevengeDamageSource.of(attacker.level(), attacker), totalDamage);
        }

        // 耐久値を消費（0になった瞬間1に戻る）
        stack.hurtAndBreak(1, attacker, (e) -> {
            stack.setDamageValue(MAX_DURABILITY - 1);
        });

        return true;
    }

    /**
     * Shift + 右クリック: 武器を砕き自壊、カメラ離脱演出、耐久値-1のジルスレイトを残してリスポーン。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            // 1. 手の武器を消費（破壊）
            stack.shrink(1);

            double px = serverPlayer.getX();
            double py = serverPlayer.getY();
            double pz = serverPlayer.getZ();

            // 2. その場に「耐久値が-1のジルスレイト」を生成
            ItemStack brokenStack = new ItemStack(Items.BROKEN_JILLSTLAIRLEAT.get());
            ItemEntity itemEntity = new ItemEntity(level, px, py + 0.5, pz, brokenStack);
            itemEntity.setDefaultPickUpDelay();
            level.addFreshEntity(itemEntity);

            // 3. クライアントへカメラ離脱パケットを送信
            FactotumPacketHandler.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new JillstlairleatSelfDestructPacket()
            );

            // 4. 自壊・リスポーンマネージャーに登録
            JillstlairleatDeathEventHandler.triggerSelfDestruct(serverPlayer);

            return InteractionResultHolder.consume(stack);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        if (slot == EquipmentSlot.MAINHAND) {
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
            builder.put(
                    Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", 1.0, AttributeModifier.Operation.ADDITION)
            );
            builder.put(
                    Attributes.ATTACK_SPEED,
                    new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", -2.4, AttributeModifier.Operation.ADDITION)
            );
            return builder.build();
        }
        return super.getDefaultAttributeModifiers(slot);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§d特性: §fリベンジダメージ").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("§7プレイ時間 1時間につき +1 ダメージ加算").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§8所持中: orcliam (空回り)").withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
