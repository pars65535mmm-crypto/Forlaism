package com.tyami.forlaism.lang.ast;

/**
 * 変数参照。
 *
 * スコープ情報は持たない。
 * Interpreter が Environment 階層を内→外へ探す。
 */
public record VariableNode(String name) implements Node {
}