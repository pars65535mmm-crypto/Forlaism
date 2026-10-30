package com.tyami.forlaism.item;

import com.tyami.forlaism.lang.*;
import com.tyami.forlaism.lang.ast.LoopNode;
import com.tyami.forlaism.lang.ast.Node;
import com.tyami.forlaism.lang.ast.ProgramNode;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 魔道書。
 *
 * 未署名: 右クリックで本文編集（バニラの本と同じ）
 * 署名済み: 右クリックで呪文発動、スニーク+右クリックで読む
 *
 * NBT:
 *   pages   : 本文（バニラと同じ ListTag<String>）
 *   title   : タイトル
 *   author  : 著者
 *   signed  : 署名済みフラグ（boolean）
 */
public class GrimoireItem extends Item {

    public static final String TAG_PAGES = "pages";
    public static final String TAG_TITLE = "title";
    public static final String TAG_AUTHOR = "author";
    public static final String TAG_SIGNED = "signed";

    private static final SpellDictionary DICTIONARY = new SpellDictionary();

    public GrimoireItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    // =========================================================
    // 右クリック
    // =========================================================

        @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            // クライアント側で画面を開く
            boolean signed = isSigned(stack);
            if (!signed) {
                net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                        net.minecraftforge.api.distmarker.Dist.CLIENT,
                        () -> () -> net.minecraft.client.Minecraft.getInstance().setScreen(
                                new com.tyami.forlaism.client.grimoire.GrimoireEditScreen(
                                        stack,
                                        hand == InteractionHand.MAIN_HAND
                                                ? player.getInventory().selected
                                                : 40
                                )
                        )
                );
            }
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        if (!(player instanceof ServerPlayer sp) || !(level instanceof ServerLevel sl)) {
            return InteractionResultHolder.fail(stack);
        }

        boolean signed = isSigned(stack);

        // 未署名: クライアント側で画面が開くので、サーバーは何もしない
        if (!signed) {
            return InteractionResultHolder.sidedSuccess(stack, false);
        }

        // 署名済み + スニーク → タイトル確認
        if (player.isShiftKeyDown()) {
            sp.displayClientMessage(
                    Component.literal("§d§l" + getTitle(stack) + " §7by " + getAuthor(stack)),
                    false
            );
            return InteractionResultHolder.sidedSuccess(stack, false);
        }

        // 署名済み → 発動
        castSpell(sp, sl, stack);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    // =========================================================
    // 呪文発動
    // =========================================================

        private void castSpell(ServerPlayer player, ServerLevel level, ItemStack stack) {

        String source = getPagesAsString(stack);
        if (source.isEmpty()) {
            player.displayClientMessage(
                    Component.literal("§c魔道書に呪文が書かれていません。"),
                    true
            );
            return;
        }

        try {
            List<Token> tokens = new Lexer(source).tokenize();
            ProgramNode program = new Parser(tokens).parseProgram();

            // =========================================================
            // スコープ環境をロード
            // =========================================================
                        // 関数マップをアイテムからロード
            java.util.Map<String, com.tyami.forlaism.lang.ast.FunctionDefNode> functions =
                    loadItemFunctions(stack);

            Environment playerEnv = GrimoireData.loadPlayerScope(player);
            Environment itemEnv = GrimoireData.loadItemScope(stack);
            Environment worldEnv = GrimoireData.loadWorldScope(player.server);

            Interpreter interpreter = new Interpreter(
                    DICTIONARY, playerEnv, itemEnv, worldEnv, functions
            );

            SpellContext ctx = new SpellContext(player, level, stack);

            for (Node stmt : program.statements()) {
                if (stmt instanceof LoopNode loop) {
                    SpellCost cost = DICTIONARY.costOf(extractFirstVerb(loop.body()));
                    if (!cost.canPay(ctx)) {
                        player.displayClientMessage(
                                Component.literal("§cリソースが足りません。"),
                                true
                        );
                        return;
                    }
                    cost.pay(ctx);

                    SpellScheduler.schedule(
                            player.getUUID(),
                            loop.body(),
                            loop.durationTicks(),
                            loop.intervalTicks()
                    );

                    player.displayClientMessage(
                            Component.literal("§d§lループ開始 §7: "
                                    + (loop.isPermanent() ? "永久" : loop.duration() + " ticks")
                                    + " / " + loop.interval() + " tick間隔")
                                    .withStyle(ChatFormatting.LIGHT_PURPLE),
                            true
                    );
                } else {
                    interpreter.executeStatement(stmt, ctx);
                }
            }

            // =========================================================
            // スコープ環境をセーブ
            // =========================================================
            GrimoireData.savePlayerScope(player, playerEnv);
            GrimoireData.saveItemScope(stack, itemEnv);
            GrimoireData.saveWorldScope(player.server, worldEnv);
            saveItemFunctions(stack, interpreter.getFunctions());

            level.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.ENCHANTMENT_TABLE_USE,
                    SoundSource.PLAYERS,
                    1.0F, 1.2F
            );

            player.displayClientMessage(
                    Component.literal("§d§l" + getTitle(stack) + " §7を詠唱した…")
                            .withStyle(ChatFormatting.LIGHT_PURPLE),
                    true
            );

        } catch (LangException e) {
            player.displayClientMessage(
                    Component.translatable(e.getMessageKey(), e.getArgs())
                            .withStyle(ChatFormatting.RED),
                    false
            );
        } catch (Throwable t) {
            player.displayClientMessage(
                    Component.literal("§c呪文の実行に失敗しました: " + t.getMessage()),
                    false
            );
        }
    }

    /**
     * ループ本体の最初の動詞を抜き出す（コスト判定用）。
     * 見つからなければ空文字。
     */
    private static String extractFirstVerb(Node body) {
        if (body instanceof com.tyami.forlaism.lang.ast.CommandNode cmd) {
            return cmd.verb();
        }
        return "";
    }

    // =========================================================
    // NBT
    // =========================================================

    public static boolean isSigned(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(TAG_SIGNED);
    }

    public static String getTitle(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null ? tag.getString(TAG_TITLE) : "Untitled";
    }

    public static String getAuthor(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null ? tag.getString(TAG_AUTHOR) : "Unknown";
    }

    /**
     * pages を改行区切りで1つの文字列に。
     */
    public static String getPagesAsString(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return "";

        ListTag pages = tag.getList(TAG_PAGES, 8); // 8 = TAG_String
        if (pages.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pages.size(); i++) {
            if (i > 0) sb.append("\n");
            sb.append(pages.getString(i));
        }
        return sb.toString();
    }

    /**
     * 署名する。外部から呼ぶ（クラフトGUI等）。
     */
    public static void sign(ItemStack stack, String title, String author) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(TAG_TITLE, title);
        tag.putString(TAG_AUTHOR, author);
        tag.putBoolean(TAG_SIGNED, true);
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
        if (!isSigned(stack)) {
            tooltip.add(Component.literal("§7未署名の魔道書。")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("§8右クリックで編集・署名できます。")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        tooltip.add(Component.literal("§d§l" + getTitle(stack))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("§7by " + getAuthor(stack))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§8右クリックで詠唱 / スニークで題名確認")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isSigned(stack);
    }

        private static java.util.Map<String, com.tyami.forlaism.lang.ast.FunctionDefNode> loadItemFunctions(ItemStack stack) {
        java.util.Map<String, com.tyami.forlaism.lang.ast.FunctionDefNode> map = new java.util.HashMap<>();
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("ForlaismItemFunctions", net.minecraft.nbt.Tag.TAG_LIST)) return map;
        ListTag list = tag.getList("ForlaismItemFunctions", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            String name = t.getString("name");
            ListTag paramsTag = t.getList("params", net.minecraft.nbt.Tag.TAG_STRING);
            java.util.List<String> params = new java.util.ArrayList<>();
            for (int j = 0; j < paramsTag.size(); j++) params.add(paramsTag.getString(j));
            String bodySource = t.getString("bodySource");
            if (bodySource.isEmpty()) continue;
            try {
                java.util.List<com.tyami.forlaism.lang.Token> tokens =
                        new com.tyami.forlaism.lang.Lexer(bodySource).tokenize();
                com.tyami.forlaism.lang.ast.Node body =
                        new com.tyami.forlaism.lang.Parser(tokens).parseSingleStatement();
                map.put(name.toLowerCase(),
                        new com.tyami.forlaism.lang.ast.FunctionDefNode(name, params, body, bodySource));
            } catch (Throwable ignored) {}
        }
        return map;
    }

    private static void saveItemFunctions(ItemStack stack,
                                          java.util.Map<String, com.tyami.forlaism.lang.ast.FunctionDefNode> functions) {
        GrimoireData.saveItemFunctions(stack, functions);
    }
}