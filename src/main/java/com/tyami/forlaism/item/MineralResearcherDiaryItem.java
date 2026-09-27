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
 * 鉱物研究員の日記。
 * 右クリックで本のように読める。
 */
public class MineralResearcherDiaryItem extends Item {

    /** 本文。 */
    private static final String TEXT =
            "I have begun recording some observations about the minerals found underground. " +
            "I am not sure how useful these notes will be, so I will keep them simple.\n\n" +
            "Iron is common and easy enough to work with. " +
            "It seems to be one of the most useful materials for ordinary tools and machines. " +
            "Gold is different. It is softer and the tools made from it do not last long, " +
            "but the metal itself has many uses. " +
            "It can be made into blocks, rails, clocks, and several other things. " +
            "I have also heard that piglins are willing to trade when gold is involved. " +
            "I should remember that.\n\n" +
            "Diamond is much harder to find. " +
            "It is usually found deep underground, " +
            "and an iron pickaxe or something better is necessary to obtain it properly. " +
            "The deeper parts of the world seem to contain more of it, " +
            "although mining there is dangerous because of lava and the darkness. " +
            "Diamond is used for strong tools, armor, and enchantment tables. " +
            "It is strange that something so small can be so valuable.\n\n" +
            "Emerald is stranger still. " +
            "I rarely see it underground, and it appears to be connected to mountain regions. " +
            "Unlike diamond, it seems to have a closer relationship with villagers and trading. " +
            "Perhaps its value comes less from its physical properties " +
            "and more from what people are willing to exchange for it.\n\n" +
            "There are other minerals too: coal, copper, lapis lazuli, redstone. " +
            "Each has its own purpose. " +
            "I think I should stop trying to understand everything at once. " +
            "For now, I will continue collecting small samples and writing down what I observe.\n\n" +
            "I wonder if these stones have always been here, waiting beneath our feet.";

    /** 見出し。 */
    private static final List<String> HEADERS = List.of(
            "Notes I",
            "Notes II",
            "Notes III",
            "Notes IV",
            "Notes V",
            "Notes VI"
    );

    public MineralResearcherDiaryItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    com.tyami.forlaism.client.BookScreen.builder()
                            .title("鉱物研究員の日記")
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
        tooltip.add(Component.literal("§7地下の鉱物について記された研究記録。")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§8右クリックで読む")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}