package com.tyami.forlaism.lang.ast;

/**
 * else 節を表すだけのラッパ。
 * IfNode 側で then と else を別々に持つため、単独では使わない。
 */
public record ElseNode(Node body) implements Node {
}