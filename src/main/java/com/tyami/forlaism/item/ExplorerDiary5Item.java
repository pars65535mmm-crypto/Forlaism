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
 * 探索家の日記 5（最終）。
 * 「光輪は存在しない」と断じた、探索家の最期の記録。
 * 光輪を鍛造しようとした儀式が、悪魔を召喚してしまったという告白。
 */
public class ExplorerDiary5Item extends Item {

    /** 本文。 */
    private static final String TEXT =
            "This entry, abrupt though it may be, shall be my last. " +
            "To my future self, and to whosoever may read these pages " +
            "in the days to come: hear me. " +
            "The Glowing Ring does not exist. " +
            "I declare this with absolute certainty—" +
            "\"There is no such thing as the Glowing Ring in this world.\"\n\n" +
            "My journey may have been in vain, " +
            "yet it was not entirely without fruit. " +
            "I have found a means to sustain myself: " +
            "I shall live as a potato farmer. " +
            "Potatoes are wholesome for the body, " +
            "and their flavor is sweet. " +
            "Yes, I shall carve out a life as a humble farmer. " +
            "And I shall say it as many times as it takes—" +
            "the Glowing Ring does not exist. " +
            "This is the sole truth I can offer, " +
            "bought with years of my life. " +
            "It is not real....\n\n" +
            "Doubtless, you who read this will question: \"Why?\"\n\n" +
            "It is because I uncovered the recipe. " +
            "I forged it with my own hands... " +
            "But what came forth was no ring of light. " +
            "It was a monstrosity... a demon. " +
            "The rite I believed would craft the ring was, " +
            "in truth, a ritual to birth a fiend. " +
            "And all rituals demand a price. " +
            "I... I could not pay that toll. " +
            "And so... because of me... the villagers.... " +
            "They were devoured by the demon... " +
            "ripped apart by that monstrosity...\n\n" +
            "I can endure it no longer. " +
            "I never wish to gaze upon such horror again... " +
            "nor do I wish for anyone else to witness it. " +
            "So I beg of you... do not seek the Glowing Ring. " +
            "The Glowing Ring... does not exist...";

    /** 見出し。 */
    private static final List<String> HEADERS = List.of(
            "The Final Journal",
            "A Declaration",
            "A Humble Life",
            "The Sole Truth",
            "Why",
            "The Recipe",
            "The Monstrosity",
            "The Price",
            "The Villagers",
            "A Plea"
    );

    public ExplorerDiary5Item(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    com.tyami.forlaism.client.BookScreen.builder()
                            .title("探索家の日記 5")
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
        tooltip.add(Component.literal("§c探索家の最期の記録。")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal("§8右クリックで読む")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}