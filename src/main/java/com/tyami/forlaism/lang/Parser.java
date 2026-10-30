package com.tyami.forlaism.lang;

import com.tyami.forlaism.lang.ast.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class Parser {

    private static final Set<String> SCOPES = Set.of("player", "item", "world", "private");
    private static final Set<String> QUALIFIERS = Set.of("the", "all", "a", "an");
    private static final String TERMINATOR = ".";

    /** 名詞として連続して読み続けることを許可しない予約語。 */
    private static final Set<String> TARGET_STOP_WORDS = Set.of(
            "with", "within", "blocks", "then", "else"
    );

    private final List<Token> tokens;
    private int pos = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public ProgramNode parseProgram() {
        List<Node> statements = new ArrayList<>();
        while (!check(TokenType.EOF)) statements.add(parseStatement());
        return new ProgramNode(statements);
    }

    public Node parseSingleStatement() {
        return parseStatement();
    }

    // =========================================================
    // 文
    // =========================================================

    private Node parseStatement() {
        if (matchWord("if")) return parseIf();
        if (matchWord("loop")) return parseLoop();
        if (matchWord("define")) return parseDefine();
        if (matchWord("print") || matchWord("say")) return parsePrint();

        // 代入
        Token t = peek();
        if (t.type() == TokenType.WORD) {
            String scope = null;
            if (SCOPES.contains(t.text().toLowerCase())
                    && peek(1).type() == TokenType.WORD
                    && peek(2).type() == TokenType.SYMBOL
                    && isAssignOp(peek(2).text())) {
                scope = t.text().toLowerCase();
                advance();
            }

            Token nameTok = peek();
            if (nameTok.type() == TokenType.WORD
                    && peek(1).type() == TokenType.SYMBOL
                    && isAssignOp(peek(1).text())) {
                if (scope == null) scope = "private";
                String op = peek(1).text();
                advance();
                advance();
                Node value;
                // 名詞句判定: WORD が2つ以上続く場合
                if (isNounPhraseStart()) {
                    value = parseNounPhraseAsValue();
                } else {
                    value = parseExpr();
                }

                matchSymbol(TERMINATOR);
                if (!op.equals("=")) {
                    String bareOp = op.substring(0, 1);
                    Node lhs = new VariableNode(nameTok.text());
                    Node rhs = new BinaryOpNode(bareOp, lhs, value);
                    return new AssignNode(scope, nameTok.text(), rhs);
                }
                return new AssignNode(scope, nameTok.text(), value);
            }
        }

        // 関数呼び出し形式
        if (t.type() == TokenType.WORD
                && peek(1).type() == TokenType.SYMBOL
                && peek(1).text().equals("(")) {
            FunctionCallExprNode call = parseFunctionCallExpr();
            matchSymbol(TERMINATOR);
            return call;
        }

        return parseCommand();
    }

    private boolean isAssignOp(String s) {
        return s.equals("=") || s.equals("+=") || s.equals("-=")
                || s.equals("*=") || s.equals("/=");
    }

    private Node parseIf() {
        Node condition = parseCondition();
        expectWord("then");
        Node body = parseStatement();
        Node elseBody = null;
        if (matchWord("else")) elseBody = parseStatement();
        return new IfNode(condition, body, elseBody);
    }

    private Node parseCondition() {
        Node left = parseExpr();
        Token t = peek();
        if (t.type() == TokenType.SYMBOL
                && Set.of(">", "<", ">=", "<=", "=", "==", "!=").contains(t.text())) {
            String op = t.text();
            advance();
            Node right = parseExpr();
            return new BinaryOpNode(op, left, right);
        }
        return left;
    }

    private Node parseLoop() {
        Integer duration = null;
        Integer interval = null;
        if (matchWord("for")) {
            int n = (int) expectNumber();
            expectWord("ticks");
            duration = n;
        }
        if (matchWord("every")) {
            int n = (int) expectNumber();
            expectWord("ticks");
            interval = n;
        }
        expectSymbol(",");
        Node body = parseStatement();
        if (containsLoop(body)) throw new LangException("forlaism.lang.error.nested_loop");
        return new LoopNode(duration, interval, body);
    }

    private boolean containsLoop(Node node) {
        if (node instanceof LoopNode) return true;
        if (node instanceof IfNode iff) {
            if (containsLoop(iff.body())) return true;
            return iff.hasElse() && containsLoop(iff.elseBody());
        }
        return false;
    }

    private Node parseDefine() {
        String name = expectWordAny();
        List<String> params = new ArrayList<>();
        if (matchSymbol("(")) {
            if (!checkSymbol(")")) {
                params.add(expectWordAny());
                while (matchSymbol(",")) params.add(expectWordAny());
            }
            expectSymbol(")");
        }
        if (matchWord("with")) expectWordAny();
        expectSymbol(":");
        int bodyStart = pos;
        Node body = parseStatement();
        String bodySource = reconstructSource(bodyStart, pos - 1);
        return new FunctionDefNode(name, params, body, bodySource);
    }

    private String reconstructSource(int from, int to) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i <= to && i < tokens.size(); i++) {
            Token t = tokens.get(i);
            if (t.type() == TokenType.STRING) sb.append('"').append(t.text()).append('"');
            else sb.append(t.text());
            sb.append(' ');
        }
        return sb.toString().trim();
    }

    private Node parsePrint() {
        Node body;
        if (peek().type() == TokenType.STRING) {
            String s = peek().text();
            advance();
            body = new StringNode(s);
        } else {
            body = parseExpr();
        }
        matchSymbol(TERMINATOR);
        return new PrintNode(body);
    }

    // =========================================================
    // 関数呼び出し
    // =========================================================

    private FunctionCallExprNode parseFunctionCallExpr() {
        String name = expectWordAny();
        expectSymbol("(");
        List<Node> args = new ArrayList<>();
        if (!checkSymbol(")")) {
            args.add(parseFunctionArg());
            while (matchSymbol(",")) args.add(parseFunctionArg());
        }
        expectSymbol(")");
        return new FunctionCallExprNode(name, args);
    }

    /**
     * 関数の引数1個を読む。
     * 名詞句（複数単語）も受け付けるため、特殊な判定を行う。
     *
     *   数値 → NumberNode
     *   文字列 → StringNode
     *   単語 → VariableNode または TargetNode(名詞句)
     *   ベクトルリテラル → VecLiteralNode
     *   関数呼び出し → FunctionCallExprNode
     */
    private Node parseFunctionArg() {
        Token t = peek();

        if (t.type() == TokenType.NUMBER) {
            advance();
            return new NumberNode(Double.parseDouble(t.text()));
        }
        if (t.type() == TokenType.STRING) {
            advance();
            return new StringNode(t.text());
        }

        // 関数呼び出し
        if (t.type() == TokenType.WORD
                && peek(1).type() == TokenType.SYMBOL
                && peek(1).text().equals("(")) {
            return parseFunctionCallExpr();
        }

                // 名詞句（複数単語）
        if (t.type() == TokenType.WORD) {
            // 単語数カウント
            int count = 0;
            int i = 0;
            while (peek(i).type() == TokenType.WORD
                    && !TARGET_STOP_WORDS.contains(peek(i).text().toLowerCase())) {
                count++;
                i++;
                if (peek(i).type() == TokenType.SYMBOL
                        && (peek(i).text().equals(",") || peek(i).text().equals(")"))) {
                    break;
                }
            }

            // "within" が続くなら名詞句
            boolean hasWithin = peek(i).type() == TokenType.WORD
                    && peek(i).text().equalsIgnoreCase("within");

            if (count == 1 && !hasWithin) {
                // 1単語 → 変数名
                String word = peek().text();
                advance();
                return new VariableNode(word);
            }

            // 名詞句 → TargetNode
            String qualifier = null;
            String first = peek().text().toLowerCase();
            if (QUALIFIERS.contains(first) || first.equals("nearest")) {
                qualifier = first;
                advance();
            }
            List<String> nouns = new ArrayList<>();
            while (peek().type() == TokenType.WORD
                    && !TARGET_STOP_WORDS.contains(peek().text().toLowerCase())) {
                nouns.add(peek().text().toLowerCase());
                advance();
            }
            Double within = null;
            if (matchWord("within")) {
                within = expectNumber();
                expectWord("blocks");
            }
            return new TargetNode(qualifier, nouns, within);
        }

        if (t.type() == TokenType.SYMBOL && t.text().equals("(")) {
            advance();
            int checkpoint = pos;
            try {
                Node x = parseExpr();
                if (matchSymbol(",")) {
                    Node y = parseExpr();
                    expectSymbol(",");
                    Node z = parseExpr();
                    expectSymbol(")");
                    return new VecLiteralNode(x, y, z);
                }
                expectSymbol(")");
                return x;
            } catch (LangException e) {
                pos = checkpoint;
                Node inner = parseExpr();
                expectSymbol(")");
                return inner;
            }
        }

        throw new LangException("forlaism.lang.error.unexpected_token", t.text());
    }

    // =========================================================
    // 従来の動詞形式
    // =========================================================

    private Node parseCommand() {
        String verb = expectWordAny();

        TargetNode target = null;
        if (!checkSymbol(TERMINATOR) && !checkWord("with") && !isArgStart()) {
            target = parseTarget();
        }

        String element = null;
        if (matchWord("with")) element = expectWordAny();

        List<Node> args = new ArrayList<>();
        while (!checkSymbol(TERMINATOR) && !check(TokenType.EOF)) {
            args.add(parseExpr());
        }

        matchSymbol(TERMINATOR);
        return new CommandNode(verb, target, element, args);
    }

    private boolean isArgStart() {
        return peek().type() == TokenType.NUMBER;
    }

    private TargetNode parseTarget() {
        String qualifier = null;
        if (peek().type() == TokenType.WORD) {
            String lower = peek().text().toLowerCase();
            if (QUALIFIERS.contains(lower)) {
                qualifier = lower;
                advance();
            }
        }
        List<String> nouns = new ArrayList<>();
        while (peek().type() == TokenType.WORD
                && !TARGET_STOP_WORDS.contains(peek().text().toLowerCase())) {
            nouns.add(peek().text().toLowerCase());
            advance();
        }
        if (nouns.isEmpty()) throw new LangException("forlaism.lang.error.expected_noun");
        Double within = null;
        if (matchWord("within")) {
            within = expectNumber();
            expectWord("blocks");
        }
        return new TargetNode(qualifier, nouns, within);
    }

    // =========================================================
    // 式
    // =========================================================

    private Node parseExpr() {
        Node left = parseTerm();
        while (checkSymbol("+") || checkSymbol("-")) {
            String op = peek().text();
            advance();
            Node right = parseTerm();
            left = new BinaryOpNode(op, left, right);
        }
        return left;
    }

    private Node parseTerm() {
        Node left = parseFactor();
        while (checkSymbol("*") || checkSymbol("/")) {
            String op = peek().text();
            advance();
            Node right = parseFactor();
            left = new BinaryOpNode(op, left, right);
        }
        return left;
    }

    private Node parseFactor() {
        Token t = peek();

        if (t.type() == TokenType.NUMBER) {
            advance();
            return new NumberNode(Double.parseDouble(t.text()));
        }

        if (t.type() == TokenType.STRING) {
            advance();
            return new StringNode(t.text());
        }

        if (t.type() == TokenType.WORD) {
            if (peek(1).type() == TokenType.SYMBOL && peek(1).text().equals("(")) {
                return parseFunctionCallExpr();
            }
            advance();
            return new VariableNode(t.text());
        }

        if (matchSymbol("(")) {
            int checkpoint = pos;
            try {
                Node x = parseExpr();
                if (matchSymbol(",")) {
                    Node y = parseExpr();
                    expectSymbol(",");
                    Node z = parseExpr();
                    expectSymbol(")");
                    return new VecLiteralNode(x, y, z);
                }
                expectSymbol(")");
                return x;
            } catch (LangException e) {
                pos = checkpoint;
                Node inner = parseExpr();
                expectSymbol(")");
                return inner;
            }
        }

        throw new LangException("forlaism.lang.error.unexpected_token", t.text());
    }

    // =========================================================
    // トークンヘルパ
    // =========================================================

    private Token peek() { return tokens.get(pos); }
    private Token peek(int offset) {
        int i = pos + offset;
        return i < tokens.size() ? tokens.get(i) : tokens.get(tokens.size() - 1);
    }
    private Token advance() { return tokens.get(pos++); }
    private boolean check(TokenType type) { return peek().type() == type; }
    private boolean checkSymbol(String s) { return peek().type() == TokenType.SYMBOL && peek().text().equals(s); }
    private boolean checkWord(String w) { return peek().type() == TokenType.WORD && peek().text().equalsIgnoreCase(w); }
    private boolean matchWord(String w) { if (checkWord(w)) { advance(); return true; } return false; }
    private boolean matchSymbol(String s) { if (checkSymbol(s)) { advance(); return true; } return false; }
    private void expectWord(String w) { if (!matchWord(w)) throw new LangException("forlaism.lang.error.expected_word", w); }
    private String expectWordAny() {
        Token t = peek();
        if (t.type() != TokenType.WORD) throw new LangException("forlaism.lang.error.expected_word_any", t.text());
        advance();
        return t.text();
    }
    private void expectSymbol(String s) { if (!matchSymbol(s)) throw new LangException("forlaism.lang.error.expected_symbol", s); }
    private double expectNumber() {
        Token t = peek();
        if (t.type() != TokenType.NUMBER) throw new LangException("forlaism.lang.error.expected_number", t.text());
        advance();
        return Double.parseDouble(t.text());
    }

        /**
     * 名詞句の開始か？
     * 「WORD WORD」または「WORD .」などで、かつ最初の WORD が関数名でないケース。
     */
    private boolean isNounPhraseStart() {
        Token t = peek();
        if (t.type() != TokenType.WORD) return false;
        // 関数呼び出しは除外
        if (peek(1).type() == TokenType.SYMBOL && peek(1).text().equals("(")) {
            return false;
        }
        // "self" 単独は変数として扱う
        if (t.text().equalsIgnoreCase("self")
                && peek(1).type() == TokenType.SYMBOL
                && peek(1).text().equals(".")) {
            return false;
        }
        // 次のトークンが WORD なら名詞句
        if (peek(1).type() == TokenType.WORD) return true;
        return false;
    }

    /**
     * 名詞句を値としてパースし、TargetNode として返す。
     * 例: nearest zombie / all zombies within 8 blocks
     */
    private Node parseNounPhraseAsValue() {
        String qualifier = null;
        if (peek().type() == TokenType.WORD) {
            String lower = peek().text().toLowerCase();
            if (QUALIFIERS.contains(lower) || lower.equals("nearest")) {
                qualifier = lower;
                advance();
            }
        }
        List<String> nouns = new ArrayList<>();
        while (peek().type() == TokenType.WORD
                && !TARGET_STOP_WORDS.contains(peek().text().toLowerCase())) {
            nouns.add(peek().text().toLowerCase());
            advance();
        }
        Double within = null;
        if (matchWord("within")) {
            within = expectNumber();
            expectWord("blocks");
        }
        return new TargetNode(qualifier, nouns, within);
    }
}