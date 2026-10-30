package com.tyami.forlaism.lang.ast;

import java.util.List;

public record FunctionDefNode(
        String name,
        List<String> parameters,
        Node body,
        String bodySource
) implements Node {

    public FunctionDefNode {
        parameters = List.copyOf(parameters);
        bodySource = bodySource == null ? "" : bodySource;
    }

    /** 後方互換用（bodySource なし）。 */
    public FunctionDefNode(String name, List<String> parameters, Node body) {
        this(name, parameters, body, "");
    }
}