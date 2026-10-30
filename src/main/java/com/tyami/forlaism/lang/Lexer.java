package com.tyami.forlaism.lang;

import java.util.ArrayList;
import java.util.List;

/**
 * 字句解析。
 *
 * 追加:
 *   - "..." で囲まれた文字列リテラル → STRING
 *   - 文字列内のエスケープは最小限（\\" と \\\\ のみ）
 */
public final class Lexer {

    private final String src;
    private int pos = 0;

    public Lexer(String src) {
        this.src = src == null ? "" : src;
    }

    public List<Token> tokenize() {
        List<Token> out = new ArrayList<>();

        while (pos < src.length()) {
            char c = src.charAt(pos);

            // 空白
            if (Character.isWhitespace(c)) {
                pos++;
                continue;
            }

            // コメント
            if (c == '#') {
                while (pos < src.length() && src.charAt(pos) != '\n') pos++;
                continue;
            }

            // 文字列リテラル
            if (c == '"') {
                int start = pos;
                pos++; // skip "
                StringBuilder sb = new StringBuilder();
                boolean closed = false;
                while (pos < src.length()) {
                    char d = src.charAt(pos);
                    if (d == '\\' && pos + 1 < src.length()) {
                        char esc = src.charAt(pos + 1);
                        if (esc == '"') { sb.append('"'); pos += 2; continue; }
                        if (esc == '\\') { sb.append('\\'); pos += 2; continue; }
                        sb.append(d);
                        pos++;
                        continue;
                    }
                    if (d == '"') {
                        pos++;
                        closed = true;
                        break;
                    }
                    sb.append(d);
                    pos++;
                }
                if (!closed) {
                    throw new LangException("forlaism.lang.error.unterminated_string", start);
                }
                out.add(new Token(TokenType.STRING, sb.toString(), start));
                continue;
            }

            // 数値
            if (Character.isDigit(c) || (c == '.' && pos + 1 < src.length()
                    && Character.isDigit(src.charAt(pos + 1)))) {
                int start = pos;
                boolean hasDot = false;
                while (pos < src.length()) {
                    char d = src.charAt(pos);
                    if (Character.isDigit(d)) {
                        pos++;
                    } else if (d == '.' && !hasDot) {
                        hasDot = true;
                        pos++;
                    } else {
                        break;
                    }
                }
                out.add(new Token(TokenType.NUMBER, src.substring(start, pos), start));
                continue;
            }

            // 英単語
            if (Character.isLetter(c) || c == '_') {
                int start = pos;
                while (pos < src.length()) {
                    char d = src.charAt(pos);
                    if (Character.isLetterOrDigit(d) || d == '_') {
                        pos++;
                    } else {
                        break;
                    }
                }
                out.add(new Token(TokenType.WORD, src.substring(start, pos), start));
                continue;
            }

            // 記号（2文字記号も先にチェック）
            String two = pos + 1 < src.length() ? src.substring(pos, pos + 2) : "";
            if (two.equals(">=") || two.equals("<=") || two.equals("==") || two.equals("!=")
                    || two.equals("+=") || two.equals("-=") || two.equals("*=") || two.equals("/=")) {
                out.add(new Token(TokenType.SYMBOL, two, pos));
                pos += 2;
                continue;
            }

            switch (c) {
                case '+', '-', '*', '/', '(', ')', '=', ',', '.', '<', '>', '!' -> {
                    out.add(new Token(TokenType.SYMBOL, String.valueOf(c), pos));
                    pos++;
                }
                default -> throw new LangException(
                        "forlaism.lang.error.unexpected_char",
                        String.valueOf(c), pos
                );
            }
        }

        out.add(new Token(TokenType.EOF, "", pos));
        return out;
    }
}