package com.tyami.forlaism.lang.ast;

/**
 * 変数代入。
 *
 * 例:
 *   x = 10
 *     → scope="private", name="x", value=NumberNode(10)
 *
 *   player x = 10
 *     → scope="player", name="x", value=NumberNode(10)
 *
 * スコープは "private" / "player" / "item" / "world" のいずれか。
 * 省略時は "private"。
 */
public record AssignNode(
        String scope,
        String name,
        Node value
) implements Node {
}