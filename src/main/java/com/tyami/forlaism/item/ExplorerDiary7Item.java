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
 * 探索家の日記 7。
 * 第6巻の欠落、地下室の封鎖、白髪の少女……不穏な記憶の空白を記した記録。
 */
public class ExplorerDiary7Item extends Item {

    /** 本文。 */
    private static final String TEXT =
            "It has been an eternity since I last looked upon my old journals. " +
            "The sight of them has compelled me to take up my pen once more.\n\n" +
            "I am no longer an explorer, yet I still choose to call myself one. " +
            "Today, I am a potato farmer. " +
            "But... something is amiss. " +
            "I possess absolutely no memory of the transition—" +
            "no recollection of how I abandoned the path of exploration " +
            "to settle into the life of a farmer. " +
            "Why is this...? " +
            "I turned back the pages of my past journals, seeking an answer. " +
            "Yet, no such record exists.\n\n" +
            "And then, a chilling realization struck me. " +
            "Volume VI is missing. The sixth journal does not exist... " +
            "If that be true, " +
            "why did my mind instinctively recognize this very volume as \"Seven\"? " +
            "I searched every corner of my homestead, " +
            "but it was nowhere to be found.\n\n" +
            "Instead, I discovered something else: " +
            "the entrance to my basement has been sealed tight. " +
            "I have no memory of sealing that door. " +
            "Why... by whose hand, and for what sinister purpose, was it barred?\n\n" +
            "A faint ache has begun to throb within my temple. " +
            "These headaches have plagued me frequently of late. " +
            "Speaking of headaches... " +
            "that maiden with hair as white as snow... " +
            "who—or what—was she truly...?\n\n" +
            "The pain in my skull grows unbearable... I must sleep...";

    /** 見出し。 */
    private static final List<String> HEADERS = List.of(
            "The Seventh Journal",
            "A Farmer's Pen",
            "The Missing Memory",
            "Volume VI",
            "The Sealed Basement",
            "The Throbbing Temple",
            "The Maiden of White",
            "I Must Sleep"
    );

    public ExplorerDiary7Item(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    com.tyami.forlaism.client.BookScreen.builder()
                            .title("探索家の日記 7")
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
        tooltip.add(Component.literal("§5欠落した第6巻を巡る、不穏な記録。")
                .withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.literal("§8右クリックで読む")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}