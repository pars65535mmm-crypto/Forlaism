package com.tyami.forlaism.lang.ast;

import java.util.List;

/**
 * プログラム全体。
 *
 * 複数の文を順に持つ。
 * 上から下へ順番に実行される。
 */
public record ProgramNode(List<Node> statements) implements Node {
}