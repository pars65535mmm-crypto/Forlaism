package com.tyami.forlaism.lang;

/**
 * 変数スコープの種類。
 *
 * private : 呪文実行内だけ（デフォルト）
 * player  : プレイヤーごとに永続
 * item    : 魔道書アイテムごとに永続
 * world   : ワールド全体で共有
 */
public enum ScopeType {
    PRIVATE,
    PLAYER,
    ITEM,
    WORLD;

    public static ScopeType fromKeyword(String keyword) {
        if (keyword == null) return PRIVATE;
        return switch (keyword.toLowerCase()) {
            case "player" -> PLAYER;
            case "item"   -> ITEM;
            case "world"  -> WORLD;
            default       -> PRIVATE;
        };
    }
}