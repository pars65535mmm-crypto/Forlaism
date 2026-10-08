package com.tyami.forlaism.item;

import com.tyami.watelib.item.IAnimatedTextItem;
import com.tyami.watelib.text.AnimatedText;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 時を忘れた剣（アンビシレーバー）。
 *
 * - 耐久値が減らない
 * - 攻撃力 13
 * - 攻撃速度 100
 * - この剣で殺した敵は「時を忘れて」味方になる
 * - Tooltip は「時を忘れた剣。」をタイプライター表示
 *
 * 味方化の実処理は {@link com.tyami.forlaism.event.AlliedMobHandler} 側で行う。
 */
public class AnbicrleberItem extends SwordItem implements IAnimatedTextItem {

    /** 攻撃力 13。 */
    public static final float ATTACK_DAMAGE = 13.0F;

    /** 攻撃速度 100。 */
    public static final double ATTACK_SPEED = 100.0D;

    public AnbicrleberItem(Properties properties) {
        super(
                Tiers.NETHERITE,
                0,
                -2.4F,
                properties
                        .stacksTo(1)
                        .fireResistant()
                        .rarity(Rarity.EPIC)
        );
    }

    // =========================================================
    // 属性（攻撃力13 / 攻撃速度100）
    // =========================================================

    private static final java.util.UUID ATK_UUID =
            java.util.UUID.fromString("a11b1c1d-1111-2222-3333-444444444401");
    private static final java.util.UUID SPD_UUID =
            java.util.UUID.fromString("a11b1c1d-1111-2222-3333-444444444402");

    @Override
    public com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute,
            net.minecraft.world.entity.ai.attributes.AttributeModifier>
    getDefaultAttributeModifiers(net.minecraft.world.entity.EquipmentSlot slot) {

        var original = super.getDefaultAttributeModifiers(slot);
        if (slot != net.minecraft.world.entity.EquipmentSlot.MAINHAND) {
            return original;
        }

        var builder = com.google.common.collect.ImmutableMultimap
                .<net.minecraft.world.entity.ai.attributes.Attribute,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier>builder();

        original.forEach(builder::put);

        // 攻撃力: ベース1 + 12 = 13
        builder.put(
                net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        ATK_UUID,
                        "anbicrleber_attack",
                        ATTACK_DAMAGE - 1.0D,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION
                )
        );

        // 攻撃速度: ベース4.0 に +96 で 100
        builder.put(
                net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        SPD_UUID,
                        "anbicrleber_speed",
                        ATTACK_SPEED - 4.0D,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION
                )
        );

        return builder.build();
    }

    // =========================================================
    // 耐久値を減らさない
    // =========================================================

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    // =========================================================
    // エンチャント発光 OFF
    // =========================================================

    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
    }

    // =========================================================
    // アニメーション名（null → バニラ表示にフォールバック）
    // =========================================================
    @Override
    public AnimatedText createAnimatedName(ItemStack stack) {
        return AnimatedText.of("アンビシレーバー")
                .dropIn(12.0f, 8.0f, 2.0f)
                .withColor(0xABCDEF);
    }
    // =========================================================
    // アニメーション Tooltip（タイプライター）
    // =========================================================

    @Override
    public AnimatedText createAnimatedTooltip(ItemStack stack, int lineIndex) {
        if (lineIndex == 1) {
            return AnimatedText.of("時を忘れた剣。")
                    .scrambleResolve(40, "口寺ｦ妄。れﾅ二")
                    .withColor(0xABCDEF);
        }
        return null;
    }

    // =========================================================
    // フォールバック用 Tooltip
    // =========================================================
    // IAnimatedTextItem 側で lineIndex==1 を処理しているので、
    // ここでは何も追加しない。
    // アニメーション失敗時に「時を忘れた剣。」が出ないのは
    // 仕様書の「Returning null from an animated name/tooltip provider
    // intentionally falls back to vanilla rendering.」に従う。
    //
    // 保険でバニラ側にも同じ文言を足しておきたい場合は、
    // 下の appendHoverText を有効化してね。

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        // 何も追加しない（animatedTooltip 側で表示する）
        // 万が一アニメーションが無効化されている環境用に保険を置くなら：
        // tooltip.add(Component.literal("時を忘れた剣。").withStyle(ChatFormatting.GRAY));
    }
}