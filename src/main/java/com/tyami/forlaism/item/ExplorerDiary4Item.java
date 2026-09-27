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
 * 探索家の日記 4。
 * 異界の牢獄から脱獄し、光輪が「鍛造可能」であることを知った探索家の記録。
 */
public class ExplorerDiary4Item extends Item {

    /** 本文。 */
    private static final String TEXT =
            "I pen these words in a state of utmost urgency. " +
            "Whilst scaling a treacherous cliff, " +
            "a bear ambushed me from the rear, " +
            "plunging me into the abyss below. " +
            "Upon regaining consciousness, " +
            "I found myself confined within the dark dungeons of an unfamiliar realm.\n\n" +
            "My crime? " +
            "\"Illegal Entry due to Involuntary Forfeiture of Judgment.\" " +
            "My sentence? " +
            "Twenty-four months of hard labor and a fine of fourteen hundred dollars.\n\n" +
            "...Are they mad?! To hell with this! I shall make my escape!\n\n" +
            "Fortunately, I still possess the purple orb " +
            "I purchased back in the Eastern Land. " +
            "It is said that wheresoever this orb is cast, " +
            "the thrower shall instantly teleport to that very spot. " +
            "And so, I flee!\n\n" +
            "I have managed to escape their clutches... " +
            "It has only just dawned on me " +
            "that I have truly vanquished my old habit of being " +
            "a \"three-day wonder.\" " +
            "Yet, a far more pressing matter weighs upon my mind: " +
            "I have gained absolutely no new tidings of the Glowing Ring. " +
            "Where in the blazes is it?!\n\n" +
            "Perhaps it is time to return to my homestead. " +
            "Returning to familiar ground may yet spark some inspiration.\n\n" +
            "As I walked along the path, " +
            "I casually queried a passing wayfarer about the ring. " +
            "To my astonishment, he possessed a fragment of lore: " +
            "\"It can be... forged,\" he claimed. " +
            "Though the method of its creation remains shrouded in mystery, " +
            "the revelation that it can be crafted at all is a monumental breakthrough.\n\n" +
            "...Verily, it must be so. Yes, it has to be. " +
            "...Aye, let us settle upon that truth.";

    /** 見出し。 */
    private static final List<String> HEADERS = List.of(
            "The Fourth Journal",
            "The Ambush",
            "The Dark Dungeons",
            "The Verdict",
            "The Purple Orb",
            "The Escape",
            "No Tidings",
            "The Homestead",
            "The Wayfarer's Words",
            "A Monumental Breakthrough"
    );

    public ExplorerDiary4Item(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    com.tyami.forlaism.client.BookScreen.builder()
                            .title("探索家の日記 4")
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
        tooltip.add(Component.literal("§7異界の牢獄から脱獄した記録。")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§8右クリックで読む")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}