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
 * 探索家の日記 1。
 * 光輪（Glowing Ring）の伝承を追って南大陸へ向かった探索家の記録。
 */
public class ExplorerDiary1Item extends Item {

    /** 本文。 */
    private static final String TEXT =
            "Today, word reached me of a myth from the Southern Continent—" +
            "whispers of an entity known as the \"Glowing Ring.\" " +
            "To unveil the truth of this legend, I set sail for the southern lands.\n\n" +
            "The Southern Continent is a harsh realm, parched and biting with heat. " +
            "Walking upon this barren earth, I murmured to myself: " +
            "Can potatoes truly thrive in such a desolate climate? " +
            "A passing villager, catching my words, offered a strange counsel: " +
            "\"True, these lands are dry and scorched. " +
            "Yet, with but a handful of bone meal, life sprouts within mere minutes. " +
            "Fear not, for food is never scarce.\" " +
            "A dreamlike soil that defies the laws of nature... " +
            "The mystery only deepens.\n\n" +
            "Seeking answers, I was led to the village Elder. " +
            "I queried him directly: \"Does the Glowing Ring reside here?\"\n\n" +
            "The Elder looked upon me with heavy eyes and replied, " +
            "\"The Glowing Ring... the Ring of Light... " +
            "It is said to exist somewhere, but it rests not within this village. " +
            "I possess the knowledge you seek, traveler. " +
            "But tell me—are you prepared to wager your very life upon this ring?\"\n\n" +
            "Startled by his gravity, I hesitated, yet answered: " +
            "\"I am. For I am not merely an explorer, " +
            "but a collector of the world's lost relics.\"\n\n" +
            "The Elder nodded slowly, and spoke these words: " +
            "\"So be it. Hearken to the ancient lore: " +
            "'He who seeks the Ring of Light shall never arrive " +
            "without the Monarch's Guidance. " +
            "He who claims the Ring of Light shall forever be bound by destiny.'\"\n\n" +
            "I could not grasp the true meaning of his words. " +
            "Whosoever lacks the guidance of the \"Monarch\" " +
            "is barred from reaching the ring... " +
            "But what of the latter half? " +
            "What does it mean to be bound by destiny? " +
            "The shadows lengthen, and the riddle remains unsolved...";

    /** 見出し。 */
    private static final List<String> HEADERS = List.of(
            "The Myth",
            "The Southern Land",
            "The Elder",
            "The Wager",
            "The Prophecy",
            "The Riddle"
    );

    public ExplorerDiary1Item(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    com.tyami.forlaism.client.BookScreen.builder()
                            .title("探索家の日記 1")
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
        tooltip.add(Component.literal("§7南大陸の伝承を追った探索家の記録。")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§8右クリックで読む")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}