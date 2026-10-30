package com.tyami.forlaism.lang.ast;

import java.util.List;

/**
 * 関数呼び出し。
 *
 * 例:
 *   Call igniteAll(zombies).
 *
 * arguments は現状「名詞」または「数値リテラル文字列」を保持。
 * 実引数の意味解釈は Interpreter 側で行う。
 */
public record CallNode(
        String name,
        List<String> arguments
) implements Node {
    public CallNode {
        arguments = List.copyOf(arguments);
    }
}