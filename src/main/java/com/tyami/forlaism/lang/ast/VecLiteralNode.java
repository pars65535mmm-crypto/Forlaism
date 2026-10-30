package com.tyami.forlaism.lang.ast;

/**
 * ベクトルリテラル。
 * 例: pos = (1, 64, 3).
 */
public record VecLiteralNode(
        Node x,
        Node y,
        Node z
) implements Node {
}