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
 * SAKURAの日記（原本）。
 * 現代口語のまま記された、ありのままの日記。
 */
public class SakuraDiaryItem extends Item {

    private static final String TEXT =
            "3月19日 SAKURAの日記\n\n" +
            "洞窟探索って怖いよね後ろに忍び寄って来る\n" +
            "クリーパーとか足を滑らせそうな地形とか\n" +
            "でも鉱石が欲しいから行くしかないんだよね";

    private static final List<String> HEADERS = List.of(
            "3月19日"
    );

    public SakuraDiaryItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    com.tyami.forlaism.client.BookScreen.builder()
                            .title("SAKURAの日記")
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
        tooltip.add(Component.literal("§7誰かの日記。ありのままの文字で綴られている。")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§8右クリックで読む")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}