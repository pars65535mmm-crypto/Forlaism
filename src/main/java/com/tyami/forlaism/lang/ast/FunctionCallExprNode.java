package com.tyami.forlaism.lang.ast;

import java.util.List;

/**
 * 関数呼び出し形式の式ノード。
 *
 * 例:
 *   GetLook(self)
 *   VecMul(look, 3)
 *   SetMotion(self, power)
 *
 * 文としても式としても使える。
 */
public record FunctionCallExprNode(
        String name,
        List<Node> args
) implements Node {
    public FunctionCallExprNode {
        args = List.copyOf(args);
    }
}