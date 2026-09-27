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
 * 探索家の日記 3。
 * 山中で白髪の少女と邂逅し、原因不明の頭痛に襲われた探索家の記録。
 */
public class ExplorerDiary3Item extends Item {

    /** 本文。 */
    private static final String TEXT =
            "It seems I may truly graduate from my days of being " +
            "a \"three-day wonder\" at last... " +
            "Ah, my emotions get the better of me again, " +
            "and I nearly forgot the core purpose of this entry. " +
            "Let us speak of what befell me.\n\n" +
            "Having exhausted my coin in this land, " +
            "I was hastily making my departure along a lonely mountain path. " +
            "It was then that I beheld her—" +
            "a maiden with hair as white as snow, walking all alone. " +
            "I opened my mouth to hail her, yet no sound came forth. " +
            "I could not breathe. " +
            "My vision began to dim into absolute darkness, " +
            "and a violent, agonizing ache pierced my skull. " +
            "Before I knew it, consciousness slipped away.\n\n" +
            "When my eyes opened once more, " +
            "the maiden was gone, " +
            "and my malady had vanished as if it were all a dream. " +
            "What, pray tell, was the cause of that sudden, treacherous headache? " +
            "Was it the whimsical changing of the weather? " +
            "Or perhaps the sickness of the high altitudes? " +
            "...Nay, it was likely but a mere lack of sleep.\n\n" +
            "There is little use in dwelling upon it, " +
            "so I steeled myself to cross the mountain with all haste. " +
            "Yet, the memory of that maiden lingers in the recesses of my mind... " +
            "Could this encounter be an omen of grand significance?\n\n" +
            "...Though, thinking upon it logically now, " +
            "I suspect my fascination stems merely from the rarity of white hair. " +
            "For heaven's sake, white hair! " +
            "A child, yet possessing tresses of pure white!? " +
            "Verily, the world harbors the strangest of souls.\n\n" +
            "Ah, and I discovered that precisely three dollars of my coin had been pilfered.";

    /** 見出し。 */
    private static final List<String> HEADERS = List.of(
            "The Third Journal",
            "The Lonely Mountain Path",
            "The Maiden of White",
            "The Darkness",
            "The Lingering Memory",
            "An Omen",
            "White Hair",
            "The Stolen Coin"
    );

    public ExplorerDiary3Item(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    com.tyami.forlaism.client.BookScreen.builder()
                            .title("探索家の日記 3")
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
        tooltip.add(Component.literal("§7白髪の少女と邂逅した日の記録。")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§8右クリックで読む")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}