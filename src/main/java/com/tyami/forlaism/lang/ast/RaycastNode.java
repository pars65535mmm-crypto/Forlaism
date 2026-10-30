package com.tyami.forlaism.lang.ast;

/**
 * レイキャストノード。
 *
 * 例:
 *   hitPos = Raycast self 30.
 *   hitPos = Raycast the nearest zombie 16.
 *
 * 実行結果は Vec3 として変数に保存される。
 */
public record RaycastNode(
        TargetNode target,
        double maxDistance
) implements Node {
}