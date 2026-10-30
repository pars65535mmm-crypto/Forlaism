package com.tyami.forlaism.lang.ast;

/**
 * 数値リテラル。
 */
public record NumberNode(double value) implements Node {
}