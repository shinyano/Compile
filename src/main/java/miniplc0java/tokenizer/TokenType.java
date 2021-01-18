package miniplc0java.tokenizer;

public enum TokenType {
    /** 空 */
    None,
    // function
    FN_KW,
    LET_KW,
    CONST_KW,
    AS_KW,
    WHILE_KW,
    IF_KW,
    ELSE_KW,
    RETURN_KW,
    BREAK_KW,
    CONTINUE_KW,

    // digit+
    UINT_LITERAL,
    // string
    STRING_LITERAL,
    DOUBLE_KW,
    INT_KW,
    VOID_KW,

    IDENT,

    PLUS,
    MINUS,
    MUL,
    DIV,
    // =
    ASSIGN,
    // ==
    EQ,
    // !=
    NEQ,
    // <
    LT,
    // >
    GT,
    // <=
    LE,
    // >=
    GE,
    L_PAREN,
    R_PAREN,
    L_BRACE,
    R_BRACE,
    // ->
    ARROW,
    COMMA,
    COLON,
    SEMICOLON,
    
    EOF,

    ;
    @Override
    public String toString() {
        switch (this) {
            case None:
                return "NullToken";
                case FN_KW:
                return "fn";
            case LET_KW:
                return "let";
            case CONST_KW:
                return "const";
            case AS_KW:
                return "as";
            case WHILE_KW:
                return "while";
            case IF_KW:
                return "if";
            case ELSE_KW:
                return "else";
            case RETURN_KW:
                return "return";
            case INT_KW:
                return "int";
            case DOUBLE_KW:
                return "double";
            case VOID_KW:
                return "void";
            case UINT_LITERAL:
                return "UNIT_LITERAL";
            case STRING_LITERAL:
                return "STRING_LITERAL";
            case IDENT:
                return "IDENT";
            case PLUS:
                return "PLUS";
            case MINUS:
                return "MINUS";
            case MUL:
                return "MUL";
            case DIV:
                return "DIV";
            case ASSIGN:
                return "ASSIGN";
            case EQ:
                return "EQ";
            case NEQ:
                return "NEQ";
            case LT:
                return "LT";
            case GT:
                return "GT";
            case LE:
                return "LE";
            case GE:
                return "GE";
            case L_PAREN:
                return "L_PAREN";
            case R_PAREN:
                return "R_PAREN";
            case L_BRACE:
                return "L_BRACE";
            case R_BRACE:
                return "R_BRACE";
            case ARROW:
                return "ARROW";
            case COMMA:
                return "COMMA";
            case COLON:
                return "COLON";
            case SEMICOLON:
                return "SEMICOLON";
            case EOF:
                return "EOF";
            default:
                return "InvalidToken";
        }
    }
}
