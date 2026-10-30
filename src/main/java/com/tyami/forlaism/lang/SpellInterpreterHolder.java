package com.tyami.forlaism.lang;

import com.tyami.forlaism.lang.ast.Node;

/**
 * スケジューラから呼ばれる薄いラッパー。
 *
 * 単発の文を実行するだけの Interpreter を作り直す。
 * 永続スコープ（player/item/world）は空でいい。
 * ループ内で変数を使いたい場合は、あとで拡張する。
 */
public final class SpellInterpreterHolder {

    private static final SpellDictionary DICTIONARY = new SpellDictionary();

    private SpellInterpreterHolder() {
    }

    public static void interpret(Node body, SpellContext ctx) {
        Environment empty = new Environment(null);
        Interpreter interpreter = new Interpreter(
                DICTIONARY, empty, empty, empty
        );
        interpreter.executeStatement(body, ctx);
    }
}