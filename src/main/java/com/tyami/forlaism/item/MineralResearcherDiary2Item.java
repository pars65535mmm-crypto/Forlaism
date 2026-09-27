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
 * 鉱物研究員の日記 2。
 * FO（フォラリス）関連の鉱物と珍しい鉱石について記されている。
 */
public class MineralResearcherDiary2Item extends Item {

    /** 本文。 */
    private static final String TEXT =
            "I have continued my studies, this time on the minerals that appear " +
            "only where FO — the substance the locals call Foralis — is present. " +
            "These are far stranger than anything I found in ordinary stone.\n\n" +
            "The first is the Forlaism Crystal. " +
            "It grows wherever Foralis has seeped into the earth for a long time. " +
            "Its colour shifts between pale blue and white depending on the light, " +
            "and it seems to hum faintly when held. " +
            "I have not yet determined what causes this sound. " +
            "Miners here say it is the crystal 'remembering' something.\n\n" +
            "The Deepslate Forlaism Crystal is the same mineral, " +
            "but formed far deeper, within the ancient stone. " +
            "It is denser and darker, and it takes noticeably longer to break. " +
            "I suspect the pressure of the deep earth changes its structure. " +
            "When I struck one, a thin pale mist rose from the fracture. " +
            "I do not know what that mist was.\n\n" +
            "Then there is the Ore Star. " +
            "This one is not mined so much as found — " +
            "a small, bright shard that appears only rarely among the crystal deposits. " +
            "It resembles a piece of a star that fell into the stone and stayed there. " +
            "The locals treat it with respect. I understand why.\n\n" +
            "There is also the High Crystal. " +
            "This is not a natural ore at all. " +
            "It is what the Forlaism Crystal becomes when it is saturated with more FO " +
            "than the earth would ever give on its own. " +
            "It glows even in complete darkness, and it does not seem to decay. " +
            "I have seen it used as a core in machines I do not fully understand.\n\n" +
            "And finally, the Star Core. " +
            "I have only seen one, and only from a distance. " +
            "It is said to be made from the Ore Star, " +
            "refined until nothing of the original stone remains. " +
            "It does not look like a mineral. It looks like a small, quiet light.\n\n" +
            "I am beginning to think that Foralis is not simply a material. " +
            "It may be something closer to a memory of the world, " +
            "and these stones are what that memory leaves behind.\n\n" +
            "I will keep observing. There is still so much I do not understand.";

    /** 見出し。 */
    private static final List<String> HEADERS = List.of(
            "Notes I — Foralis",
            "Notes II — Crystal",
            "Notes III — Deepslate",
            "Notes IV — Ore Star",
            "Notes V — High Crystal",
            "Notes VI — Star Core",
            "Notes VII"
    );

    public MineralResearcherDiary2Item(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    com.tyami.forlaism.client.BookScreen.builder()
                            .title("鉱物研究員の日記 2")
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
        tooltip.add(Component.literal("§7FOより生まれる鉱物についての研究記録。")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§8右クリックで読む")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}