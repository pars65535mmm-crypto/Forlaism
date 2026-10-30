package com.tyami.forlaism.lang.ast;

/**
 * チャット出力。
 *
 * 例:
 *   Print "Hello".
 *   Print x.
 *
 * body は文字列リテラル（StringNode）または任意の式。
 */
public record PrintNode(Node body) implements Node {
}