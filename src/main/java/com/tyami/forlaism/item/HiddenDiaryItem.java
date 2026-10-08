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
import java.util.ArrayList;
import java.util.List;

/**
 * 裏の日記。
 *
 * Lore: 謎文章図鑑
 *
 * ─ 内容 ────────────────────────────────────────
 *   世界観の説明ではなく「プレイヤー自身への語りかけ」。
 *   メタ的な文章を集めた、謎文章図鑑。
 *
 * ─ 増やし方 ────────────────────────────────────────
 *   下の ENTRIES リストに文字列を追加するだけでOK。
 *   各文章は自動的に「Ⅰ」「Ⅱ」「Ⅲ」… の見出しが振られ、
 *   1ページ1文章として表示される。
 */
public class HiddenDiaryItem extends Item {

    /**
     * 謎文章のリスト。
     *
     * 【追加方法】
     *   文字列を1個追加するだけで、図鑑に載る。
     *   順番は追加順。
     */
    public static final List<String> ENTRIES = new ArrayList<>(List.of(

            "そして彼は永遠に眠る",

            "さあ、行こうか、果てへ...",

            "先に行ってくるね、君はきちゃダメだよ。",

            "懐かしさは時に蘇る",

            "あの物語が諭すの",

            "いつまでもかれないきせきのおはな",

            "「さようなら」と最期に言った彼の声だけが今も響いている",

            "物語は終わり...けど君は始めたばかりなんだね?、" +
                    "じゃあ君の物語を紡ごうか..",

            "君の物語はここで終わり、けど次の君は次に行けるかもしれない、" +
                    "その微かな望み..「希望」を信じて待とう..ここで",

            "また君は繰り返す、何度も繰り返す、" +
                    "君が勝つまで、君が納得できるところまで",

            "君はいつまで続けるのだろう、いつまで繰り返すのだろう、" +
                    "いつまで失い続けるのだろう、いつまで手に入れ続けるのだろう。",

            "物語はついに終わりを迎えた、けど君は終わらない",

            "彼は去ってしまった、君は去らずに居てくれるのだろうか..",

            "君も去っていくんだね、いいよ..気にしないで.."

    ));

    /**
     * 見出しの数字（Ⅰ ～ Ⅹ ～ ...）。
     * 必要になったら増やす。
     */
    private static final String[] NUMERALS = {
            "Ⅰ", "Ⅱ", "Ⅲ", "Ⅳ", "Ⅴ",
            "Ⅵ", "Ⅶ", "Ⅷ", "Ⅸ", "Ⅹ",
            "Ⅺ", "Ⅻ", "XIII", "XIV", "XV",
            "XVI", "XVII", "XVIII", "XIX", "XX"
    };

    public HiddenDiaryItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    // =========================================================
    // 右クリック: 図鑑を開く
    // =========================================================

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    com.tyami.forlaism.client.BookScreen.builder()
                            .title("裏の日記")
                            .text(buildText())
                            .headers(buildHeaders())
                            .palette(
                                    // 羊皮紙: 濃い紫がかった黒
                                    0xFF1A0E1F,
                                    // 縁
                                    0xFF2E1B3A,
                                    // 外枠
                                    0xFF6A2E8A,
                                    // インク: 薄紫
                                    0xFFD8B8E8,
                                    // インク薄
                                    0xFF9A7AA8,
                                    // 表紙暗
                                    0xFF0A0510,
                                    // 表紙明
                                    0xFF1F0A2A,
                                    // しおり
                                    0xFF7A2EAA
                            )
                            .open()
            );
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    // =========================================================
    // 本文と見出しの生成
    // =========================================================

    /**
     * 全文章を1つの文字列に結合。
     * 各文章の間は空行で区切る。
     */
    private static String buildText() {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < ENTRIES.size(); i++) {
            sb.append(ENTRIES.get(i));

            // 最後以外は改行2つ（空行）
            if (i < ENTRIES.size() - 1) {
                sb.append("\n\n");
            }
        }

        return sb.toString();
    }

    /**
     * 見出しリストを生成。
     * 各文章に「Ⅰ」「Ⅱ」…と番号を振る。
     */
    private static List<String> buildHeaders() {
        List<String> headers = new ArrayList<>();

        for (int i = 0; i < ENTRIES.size(); i++) {
            if (i < NUMERALS.length) {
                headers.add(NUMERALS[i]);
            } else {
                headers.add("No." + (i + 1));
            }
        }

        return headers;
    }

    // =========================================================
    // Tooltip
    // =========================================================

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.literal("§5裏の日記")
                .withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.literal("§7謎文章図鑑")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("§8右クリックで読む")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    // =========================================================
    // 外部から文章を追加するためのAPI
    // =========================================================

    /**
     * 謎文章を追加する。
     * 他のMODやイベントから呼び出して、動的に増やせる。
     */
    public static void addEntry(String entry) {
        if (entry != null && !entry.isEmpty()) {
            ENTRIES.add(entry);
        }
    }
}