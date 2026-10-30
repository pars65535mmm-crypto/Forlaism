package com.tyami.forlaism.lang.ast;

import java.util.List;

/**
 * 命令文。
 *
 * args は Node のリスト（数値 or 変数参照の両方を許容）。
 * 後方互換のため List<Double> コンストラクタも残す。
 */
public record CommandNode(
        String verb,
        TargetNode target,
        String element,
        List<Node> args
) implements Node {

    public CommandNode {
        args = args == null ? List.of() : List.copyOf(args);
    }

    /** 後方互換用（引数なし）。 */
    public CommandNode(String verb, TargetNode target, String element) {
        this(verb, target, element, List.of());
    }

    /** 数値のみの引数で構築する簡易版。 */
    public static CommandNode numeric(String verb, TargetNode target,
                                      String element, List<Double> numericArgs) {
        List<Node> list = new java.util.ArrayList<>();
        for (double d : numericArgs) list.add(new NumberNode(d));
        return new CommandNode(verb, target, element, list);
    }
}