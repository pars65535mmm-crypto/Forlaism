package com.tyami.forlaism.lang.ast;

import java.util.List;

/**
 * ベクトル関数ノード。
 *
 * 例:
 *   VecAdd(v1, v2)
 *   VecSub(v1, v2)
 *   VecMul(v, 2)
 *   VecNormalize(v)
 *   VecLength(v)
 *   VecOf(x, y, z)
 *
 * args は VariableNode / NumberNode / VecLiteralNode のいずれか。
 */
public record VecFuncNode(
        String name,
        List<Node> args
) implements Node {
    public VecFuncNode {
        args = List.copyOf(args);
    }
}