package com.tyami.forlaism.lang.ast;

/**
 * 文字列リテラル。
 *
 * 例:
 *   "Hello, world."
 */
public record StringNode(String value) implements Node {
}