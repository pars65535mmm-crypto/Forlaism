package com.tyami.forlaism.lang.ast;

/**
 * 取得系ノード。
 *
 * 例:
 *   hp = GetHealth self.
 *   pos = GetPos the nearest zombie.
 *
 * 実行結果は数値または Vec3。
 */
public record GetterNode(
        String name,
        TargetNode target
) implements Node {
}