package com.tyami.forlaism.lang;

/**
 * 言語処理中のエラー。
 *
 * messageKey と args を持たせて、
 * 表示時に Component.translatable で解決する。
 *
 * 例:
 *   new LangException("forlaism.lang.error.unknown_verb", "ignite")
 */
public class LangException extends RuntimeException {

    private final String messageKey;
    private final Object[] args;

    public LangException(String messageKey, Object... args) {
        super(messageKey + " " + java.util.Arrays.toString(args));
        this.messageKey = messageKey;
        this.args = args;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Object[] getArgs() {
        return args;
    }
}