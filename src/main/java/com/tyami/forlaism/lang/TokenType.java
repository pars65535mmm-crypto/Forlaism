package com.tyami.forlaism.lang;

/**
 * トークンの種類。
 *
 * STRING を追加。
 * Lexer が "..." を読んだら STRING として返す。
 */
public enum TokenType {
    /** 英単語（Ignite, zombie, loop, for, ...）。 */
    WORD,

    /** 数値リテラル（10, 3.14, ...）。 */
    NUMBER,

    /** 文字列リテラル（"Hello"）。 */
    STRING,

    /** 記号（+ - * / ( ) = , . < > ! など）。 */
    SYMBOL,

    /** 入力終端。 */
    EOF
}