package miniplc0java.tokenizer;

import miniplc0java.error.TokenizeError;
import miniplc0java.error.ErrorCode;
import miniplc0java.util.Pos;

public class Tokenizer {

    private StringIter it;

    public Tokenizer(StringIter it) {
        this.it = it;
    }

    // 这里本来是想实现 Iterator<Token> 的，但是 Iterator 不允许抛异常，于是就这样了
    /**
     * 获取下一个 Token
     * 
     * @return
     * @throws TokenizeError 如果解析有异常则抛出
     */
    public Token nextToken() throws TokenizeError {
        it.readAll();

        // 跳过之前的所有空白字符
        skipSpaceCharacters();

        if (it.isEOF()) {
            return new Token(TokenType.EOF, "", it.currentPos(), it.currentPos());
        }

        char peek = it.peekChar();
        if (Character.isDigit(peek)) {
            return lexUInt();
        } else if (Character.isAlphabetic(peek) || peek == '_') {
            return lexIdentOrKeyword();
        }else if(peek == '\"'){
            return lexSTRING_LITERAL();
        } else {
            return lexOperatorOrUnknown();
        }
    }

    private Token lexUInt() throws TokenizeError {
        if(!Character.isDigit(it.peekChar())){
            throw new Error("Not implemented");
        }

        Long value=(long)0;
        Pos startPos = it.currentPos();
        while(Character.isDigit(it.peekChar())){
            value=value*(long)10+Long.parseLong(String.valueOf(it.nextChar()));
        }
        Pos endPos = it.currentPos();
        TokenType tokentype = TokenType.UINT_LITERAL;

        Token intToken = new Token(tokentype, value, startPos, endPos);
        return intToken;

        // 请填空：
        // 直到查看下一个字符不是数字为止:
        // -- 前进一个字符，并存储这个字符
        //
        // 解析存储的字符串为无符号整数
        // 解析成功则返回无符号整数类型的token，否则返回编译错误
        //
        // Token 的 Value 应填写数字的值
        
    }

    private TokenType stringFindType(String s){
        if(s.contentEquals("fn")) return TokenType.FN_KW;
        if(s.contentEquals("let")) return TokenType.LET_KW;
        if(s.contentEquals("const")) return TokenType.CONST_KW;
        if(s.contentEquals("as")) return TokenType.AS_KW;
        if(s.contentEquals("while")) return TokenType.WHILE_KW;
        if(s.contentEquals("if")) return TokenType.IF_KW;
        if(s.contentEquals("else")) return TokenType.ELSE_KW;
        if(s.contentEquals("return")) return TokenType.RETURN_KW;
        // if(s.contentEquals("break")) return TokenType.BREAK_KW;
        // if(s.contentEquals("continue")) return TokenType.CONTINUE_KW;
        if(s.contentEquals("int")) return TokenType.INT_KW;
        if(s.contentEquals("void")) return TokenType.VOID_KW;
        return TokenType.IDENT;
    }

    private Token lexIdentOrKeyword() throws TokenizeError {
        if(!(Character.isDigit(it.peekChar()) || Character.isAlphabetic(it.peekChar()) || it.peekChar() == '_')){
            throw new Error("Not implemented");
        }

        String value = "";
        Pos startPos = it.currentPos();
        while(Character.isDigit(it.peekChar()) || Character.isAlphabetic(it.peekChar()) || it.peekChar() == '_'){
            value = value + it.nextChar();
        }
        Pos endPos = it.currentPos();
        TokenType tokentype = stringFindType(value);

        Token intToken = new Token(tokentype, value, startPos, endPos);
        return intToken;
        // 请填空：
        // 直到查看下一个字符不是数字或字母为止:
        // -- 前进一个字符，并存储这个字符
        //
        // 尝试将存储的字符串解释为关键字
        // -- 如果是关键字，则返回关键字类型的 token
        // -- 否则，返回标识符
        //
        // Token 的 Value 应填写标识符或关键字的字符串
    }

    private Token lexSTRING_LITERAL() throws TokenizeError {
        Pos startPos = it.currentPos();
        String value = "";
        StringBuffer temp = new StringBuffer("\"");
        char ch = it.nextChar();
        while(it.peekChar()!='\"'){
            ch = it.nextChar();
            temp.append(ch);
            if(ch == '\\'){
                ch = it.nextChar();
                switch(ch) {
                    case '\\':
                    case '\"':
                    case '\'':
                    case 'n':
                    case 'r':
                    case 't':
                        temp.append(ch);
                        break;
                    default:
                        throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
                }
            }
        }
        temp.append(it.nextChar());
        Pos endPos = it.currentPos();
        value = new String(temp);
        return new Token(TokenType.STRING_LITERAL, value, startPos, endPos);

    }

    private Token lexOperatorOrUnknown() throws TokenizeError {
        Pos startPos = it.currentPos();
        switch (it.nextChar()) {
            case '+':
                return new Token(TokenType.PLUS, '+', startPos, it.currentPos());

            case '-':
                if(it.peekChar() == '>'){
                    it.nextChar();
                    return new Token(TokenType.ARROW, "->", startPos, it.currentPos());
                } else {
                    return new Token(TokenType.MINUS, '-', startPos, it.currentPos());
                }
            case '*':
                return new Token(TokenType.MUL, '*', startPos, it.currentPos());
            case '/':
                return new Token(TokenType.DIV, '/', startPos, it.currentPos());
            case '=':
                if(it.peekChar() == '='){
                    it.nextChar();
                    return new Token(TokenType.EQ, "==", startPos, it.currentPos());
                } else {
                    return new Token(TokenType.EQ, '=', startPos, it.currentPos());
                }
            case '!':
                if(it.peekChar() != '='){
                    throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
                }
                it.nextChar();
                return new Token(TokenType.NEQ, "!=", startPos, it.currentPos());
            case '<':
                if(it.peekChar() == '='){
                    it.nextChar();
                    return new Token(TokenType.LE, "<=", startPos, it.currentPos());
                } else {
                    return new Token(TokenType.LT, '<', startPos, it.currentPos());
                }
            case '>':
                if(it.peekChar() == '='){
                    it.nextChar();
                    return new Token(TokenType.GE, ">=", startPos, it.currentPos());
                } else {
                    return new Token(TokenType.GT, '>', startPos, it.currentPos());
                }
            case ',':
                return new Token(TokenType.COMMA, ':', startPos, it.currentPos());
            case ':':
                return new Token(TokenType.COLON, ':', startPos, it.currentPos());
            case ';':
                return new Token(TokenType.SEMICOLON, ';', startPos, it.currentPos());
            
            case '(':
                return new Token(TokenType.L_PAREN, '(', startPos, it.currentPos());
            case ')':
                return new Token(TokenType.R_PAREN, ')', startPos, it.currentPos());

            case '{':
                return new Token(TokenType.L_BRACE, '{', startPos, it.currentPos());
            case '}':
                return new Token(TokenType.R_BRACE, '}', startPos, it.currentPos());

            default:
                // 不认识这个输入，摸了
                throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
        }
    }

    private void skipSpaceCharacters() {
        while (!it.isEOF() && Character.isWhitespace(it.peekChar())) {
            it.nextChar();
        }
    }
    
}
