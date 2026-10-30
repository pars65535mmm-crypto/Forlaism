package com.tyami.forlaism.lang.ast;

/**
 * If 文（else 対応版）。
 *
 * 例:
 *   If mana > 10 then heal the self with light.
 *   If mana > 10 then heal the self with light. Else Ignite the nearest zombie with fire.
 *
 * elseBody は null 許容。
 */
public record IfNode(
        Node condition,
        Node body,
        Node elseBody
) implements Node {

    /** 後方互換用：else なし版。 */
    public IfNode(Node condition, Node body) {
        this(condition, body, null);
    }

    public boolean hasElse() {
        return elseBody != null;
    }
}