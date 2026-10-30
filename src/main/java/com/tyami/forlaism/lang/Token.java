package com.tyami.forlaism.lang;

/**
 * 1個のトークン。
 *
 * text は「元の文字列そのまま」。
 * 大文字小文字の正規化は Parser 側で行う。
 */
public record Token(TokenType type, String text, int position) {

    /** デバッグ用。 */
    @Override
    public String toString() {
        return type + "('" + text + "')@" + position;
    }
}