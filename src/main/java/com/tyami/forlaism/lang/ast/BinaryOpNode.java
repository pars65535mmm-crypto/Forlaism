package com.tyami.forlaism.lang.ast;

/**
 * 二項演算。
 *
 * op: + - * / > < = == != >= <=
 */
public record BinaryOpNode(String op, Node left, Node right) implements Node {
}