package com.tyami.forlaism.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import javax.annotation.Nullable;
import java.util.List;

/**
 * SAKURAの日記（復元版）。
 * 後世の手によって古語・文語体に書き改められたもの。
 */
public class SakuraDiaryRestoredItem extends Item {

    private static final String TEXT =
            "三月十九日\n\n" +
            "今日は洞窟へと降りたり。\n" +
            "地の底は暗く、道は狭く、" +
            "己が後ろに何か潜み居るやうに思へてならぬ。\n\n" +
            "殊に恐ろしきは、物音も立てず忍び寄る緑の怪物なり。\n" +
            "振り返りたる時には、既に其処に立ち居ることあり。\n" +
            "あの者の近くにて油斷することは、決して良き事に非ず。\n\n" +
            "又、洞窟の地面は何処も平らとは限らぬ。\n" +
            "一歩踏み違へば深き穴へ落つるやもしれず、" +
            "溶岩の近くなど歩かば、足を滑らせて其の儘すべてを失ふこともあらう。\n\n" +
            "されど、地の底には我が欲する鉱石が眠れり。\n" +
            "鐵も、金も、其他の石も、暗き土の下にて人の手を待ち居る。\n\n" +
            "故に吾は、恐ろしければ恐ろしきほど、洞窟へ赴かねばならぬ。\n\n" +
            "明日もまた、灯りを携へて降りるとしよう。\n" +
            "どうか後ろより怪物の來たらざらんことを。";

    private static final List<String> HEADERS = List.of(
            "三月十九日",
            "洞窟",
            "緑の怪物",
            "足元",
            "鉱石",
            "決意",
            "祈り"
    );

    public SakuraDiaryRestoredItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    com.tyami.forlaism.client.BookScreen.builder()
                            .title("SAKURAの日記（復元版）")
                            .text(TEXT)
                            .headers(HEADERS)
                            .open()
            );
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7後世の手により復元された日記。")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§8右クリックで読む")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}