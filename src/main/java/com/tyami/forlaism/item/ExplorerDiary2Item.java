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
 * 探索家の日記 2。
 * 南大陸を諦め、東の島「黄金の国」へと向かった探索家の記録。
 */
public class ExplorerDiary2Item extends Item {

    /** 本文。 */
    private static final String TEXT =
            "This marks my second journal. " +
            "Back in the day, my comrades used to mock me, saying, " +
            "\"You aren't just a three-day wonder; you're a one-volume wonder.\" " +
            "It seems I am finally poised to clear my name. " +
            "But enough of this idle chatter; let us return to the matter at hand.\n\n" +
            "Following my previous entry, I scouted the Southern Continent, " +
            "but its vastness yielded nothing but endless, barren wastes. " +
            "Left with naught to do, I resolved to kill time " +
            "by setting sail for an eastern isle—" +
            "a realm whispered by some to be the \"Land of Gold.\"\n\n" +
            "Upon setting foot on this island, my first realization was a dire one: " +
            "I understood not a single word of their tongue. " +
            "Finding myself in a predicament, " +
            "I forced myself to master their language with utmost haste. " +
            "The structure of their speech is queer, " +
            "with the order of words completely reversed from my native tongue... " +
            "Day book..? I know not how to properly utter it, " +
            "but the name of a nation is a trivial thing, hardly worth my concern.\n\n" +
            "I ventured into a shop nearby, " +
            "whose sign read something akin to... " +
            "The Shop of United Children..? " +
            "Perplexed, I nevertheless placed an order. " +
            "What was brought to me was a curious thing: " +
            "three round spheres impaled upon a wooden skewer, " +
            "bearing the name \"DANGO.\" " +
            "I questioned whether such an object was even fit for human consumption. " +
            "Having no choice, I steeled my resolve and took a bite.\n\n" +
            "To my surprise, it was delightful " +
            "and possessed a peculiar, chewy texture. " +
            "To call it \"chewy\" at every turn would grow tedious, " +
            "so henceforth, I shall refer to this texture as Mochi. " +
            "As it turns out, this establishment is known as a Dangoya. " +
            "I queried the shopkeeper regarding the Glowing Ring, " +
            "yet he possessed no tidings of it.\n\n" +
            "I suppose such fruitless days are to be expected. " +
            "Alas, I managed to squander the last of my local coin on those sweet spheres, " +
            "leaving me with no choice but to sleep under the open stars tonight. " +
            "How, pray tell, did my grand journey come to this...?";

    /** 見出し。 */
    private static final List<String> HEADERS = List.of(
            "The Second Journal",
            "The Barren South",
            "The Land of Gold",
            "The Reversed Tongue",
            "The Shop of United Children",
            "Dango",
            "Under the Open Stars"
    );

    public ExplorerDiary2Item(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    com.tyami.forlaism.client.BookScreen.builder()
                            .title("探索家の日記 2")
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
        tooltip.add(Component.literal("§7東の島を訪れた記録。")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§8右クリックで読む")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}