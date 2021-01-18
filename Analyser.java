

import java.util.*;

//import jdk.nashorn.internal.ir.BreakableNode;

public final class Analyser {

    Tokenizer tokenizer;
    Table table;
    int deep = 1;

    /** 当前偷看的 token */
    Token peekedToken = null;

    /** 符号表 */
    // HashMap<String, SymbolEntry> symbolTable = new HashMap<>();

    /** 下一个变量的栈偏移 */
    int nextOffset = 0;

    public Analyser(Tokenizer tokenizer) throws AnalyzeError {
        this.tokenizer = tokenizer;
        this.table = new Table();
    }

    public Table analyse() throws CompileError {
        analyseProgram();
        return table;
    }

    /**
     * 查看下一个 Token
     * 
     * @return
     * @throws TokenizeError
     */
    private Token peek() throws TokenizeError {
        if (peekedToken == null) {
            peekedToken = tokenizer.nextToken();
        }
        return peekedToken;
    }

    /**
     * 获取下一个 Token
     * 
     * @return
     * @throws TokenizeError
     */
    private Token next() throws TokenizeError {
        if (peekedToken != null) {
            Token token = peekedToken;
            peekedToken = null;
            return token;
        } else {
            return tokenizer.nextToken();
        }
    }

    /**
     * 如果下一个 token 的类型是 tt，则返回 true
     * 
     * @param tt
     * @return
     * @throws TokenizeError
     */
    private boolean check(TokenType tt) throws TokenizeError {
        Token token = peek();
        return token.getTokenType() == tt;
    }

    /**
     * 如果下一个 token 的类型是 tt，则前进一个 token 并返回这个 token
     * 
     * @param tt 类型
     * @return 如果匹配则返回这个 token，否则返回 null
     * @throws TokenizeError
     */
    private Token nextIf(TokenType tt) throws TokenizeError {
        Token token = peek();
        if (token.getTokenType() == tt) {
            return next();
        } else {
            return null;
        }
    }

    private List<TokenType> getList(TokenType... tokenTypes) {
        List<TokenType> t=new ArrayList<>();
        for (TokenType tokenType:tokenTypes)
            t.add(tokenType);
        return t;
    }

    /**
     * 如果下一个 token 的类型是 tt，则前进一个 token 并返回，否则抛出异常
     * 
     * @param tt 类型
     * @return 这个 token
     * @throws CompileError 如果类型不匹配
     */
    private Token expect(TokenType tt) throws CompileError {
        Token token = peek();
        if (token.getTokenType() == tt) {
            return next();
        } else {
            throw new ExpectedTokenError(tt, token);
        }
    }

    private Token expectTy() throws CompileError {
        Token token = peek();
        TokenType tokentype=token.getTokenType();
        if ( tokentype== TokenType.INT_KW) {
            return next();
        } else {
            throw new ExpectedTokenError(getList(TokenType.INT_KW), next());
        }
    }

    private Token expectReturnTy() throws CompileError {
        Token token = peek();
        TokenType tokentype=token.getTokenType();
        if ( tokentype== TokenType.INT_KW||tokentype== TokenType.VOID_KW) {
            return next();
        } else {
            throw new ExpectedTokenError(getList(TokenType.INT_KW, TokenType.VOID_KW), next());
        }
    }
    private Token expectLiteral() throws CompileError {
        Token token = peek();
        TokenType tokentype=token.getTokenType();
        if ( tokentype== TokenType.UINT_LITERAL||tokentype== TokenType.STRING_LITERAL) {
            return next();
        } else {
            throw new ExpectedTokenError(getList(TokenType.UINT_LITERAL, TokenType.STRING_LITERAL), next());
        }
    }

    private void expectNotConstant(Token token) throws AnalyzeError {
        String name=token.getValueString();
        Pos curPos=token.getStartPos();
        SymbolEntry entry = this.table.get(name,this.deep,curPos);
        if (entry == null) {
            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
        } else if(entry.isConstant()) {
            throw new AnalyzeError(ErrorCode.AssignToConstant, curPos);
        }
    }

    /**
     * 获取下一个变量的栈偏移
     * 
     * @return
     */
    private int getNextVariableOffset() {
        return this.nextOffset++;
    }

    private Instruction getVarOrParamAddress(String value, Pos curPos) throws AnalyzeError {
        SymbolEntry symbolEntry=this.table.get(value,this.deep,curPos);
        if(symbolEntry==null)
            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
        Long off=symbolEntry.getOff();
        if(symbolEntry.getNameType()==NameType.Proc)
            throw new AnalyzeError(ErrorCode.CantGetProcAddress, curPos);
        else if(symbolEntry.getNameType()==NameType.Params){
            return new Instruction(Operation.arga,off);
        }else if(symbolEntry.getNameType()==NameType.Var) {
            if(symbolEntry.getDeep()==1)
                return new Instruction(Operation.globa,off);
            else
                return new Instruction(Operation.loca,off);
        }else
            throw new AnalyzeError(ErrorCode.ExpectNameToken, curPos);
    }

    private Instruction getStringAddress(Token token) throws AnalyzeError {
        this.table.addGlobal(token,true,NameType.String,null);
        return new Instruction(Operation.push,(long)this.table.getGlobalId(token));
    }

    /**
     * 添加一个符号
     * 
     * @param name          名字
     * @param isInitialized 是否已赋值
     * @param isConstant    是否是常量
     * @param curPos        当前 token 的位置（报错用）
     * @throws AnalyzeError 如果重复定义了则抛异常
     */
    private void addSymbol(Token token,NameType nameType,TokenType tokenType,int deep,boolean isInitialized, boolean isConstant, Pos curPos) throws AnalyzeError {
        String name=token.getValueString();
        if (this.table.get(name,deep,token.getStartPos()) != null) {
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, curPos);
        }
        else{
            this.table.put(new SymbolEntry(name,nameType,tokenType,deep,isConstant, isInitialized, getNextVariableOffset()),deep,token);
        }
    }

    /**
     * 设置符号为已赋值
     * 
     * @param name   符号名称
     * @param curPos 当前位置（报错用） startPos
     * @throws AnalyzeError 如果未定义则抛异常
     */
    private void initializeSymbol(String name, Pos curPos) throws AnalyzeError {
        SymbolEntry entry = this.table.get(name,this.deep,curPos);
        if (entry == null) {
            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
        } else {
            entry.setInitialized(true);
        }
    }

    private void declareSymbol(String name, Pos curPos) throws AnalyzeError {
        SymbolEntry entry = this.table.get(name,this.deep,curPos);
        if (entry == null) {
            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
        }else if(entry.getNameType()==NameType.Proc){
            throw new AnalyzeError(ErrorCode.AssignedToFunction, curPos);
        }else if(entry.isConstant()){
            throw new AnalyzeError(ErrorCode.AssignToConstant, curPos);
        }
        else {
            entry.setInitialized(true);
        }
    }

    /**
     * 获取变量在栈上的偏移
     * 
     * @param name   符号名
     * @param curPos 当前位置（报错用）
     * @return 栈偏移
     * @throws AnalyzeError
     */
    private int getOffset(String name, Pos curPos) throws AnalyzeError {
        SymbolEntry entry = this.table.get(name,this.deep,curPos);
        if (entry == null) {
            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
        } else {
            return entry.getStackOffset();
        }
    }

    /**
     * 获取变量是否是常量
     * 
     * @param name   符号名
     * @param curPos 当前位置（报错用）
     * @return 是否为常量
     * @throws AnalyzeError
     */
    private boolean isConstant(String name, Pos curPos) throws AnalyzeError {
        SymbolEntry entry = this.table.get(name,this.deep,curPos);
        if (entry == null) {
            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
        } else {
            return entry.isConstant();
        }
    }

    private boolean isInitialized(String name, Pos curPos) throws AnalyzeError {
        var entry = this.table.get(name,this.deep,curPos);
        if(entry==null){
            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
        }
        else if (entry.isInitialized()) {
            return true;
        }else {
            throw new AnalyzeError(ErrorCode.NotInitialized, curPos);
        }
    }

    private void analyseProgram() throws CompileError {
        // 程序 -> 'begin' 主过程 'end'
        // 示例函数，示例如何调用子程序
        // 'begin'
        while(!check(TokenType.EOF)){
            while(check(TokenType.LET_KW)||check(TokenType.CONST_KW)){
                this.table.addAllInstructions(analyseDeclStmt(),this.deep);
            }
            while(check(TokenType.FN_KW)){
                analyseFunction();
            }
        }
        expect(TokenType.EOF);
    }

    private List<Instruction> analyseFunction() throws CompileError {
        List<Instruction> instructions=new ArrayList<>();
        expect(TokenType.FN_KW);
        Token name = expect(TokenType.IDENT);
        addSymbol(name,NameType.Proc, TokenType.VOID_KW,this.deep,true,true,name.getStartPos());
        expect(TokenType.L_PAREN);
        if(check(TokenType.R_PAREN)){
            expect(TokenType.R_PAREN);
        } else {
            analyseFunctionParamList();
            expect(TokenType.R_PAREN);
        }
        expect(TokenType.ARROW);
        Token returnType = expectReturnTy();
        if(returnType.getTokenType() == TokenType.INT_KW){
            this.table.setFuncReturn(name.getValueString(),this.deep,name.getStartPos(),TokenType.INT_KW);
        }
        instructions.addAll(analyseBlockStmt());
        instructions.add(new Instruction(Operation.ret));
        this.table.addAllInstructions(instructions,this.deep+1);
        return instructions;
    }

    private void analyseFunctionParamList() throws CompileError {
        analyseFunctionParam();
        while (check(TokenType.COMMA)) {
            expect(TokenType.COMMA);
            analyseFunctionParam();
        }
    }

    private void analyseFunctionParam() throws CompileError {
        boolean isConst = false;
        if(check(TokenType.CONST_KW)){
            isConst = true;
            expect(TokenType.CONST_KW);
        }
        Token nameToken=expect(TokenType.IDENT);
        expect(TokenType.COLON);
        Token ty=expectTy();
        addSymbol(nameToken,NameType.Params,ty.getTokenType(),this.deep+1,true,isConst,nameToken.getStartPos());
    }

    private List<Instruction> analyseBlockStmt() throws CompileError {
        List<Instruction> instructions=new ArrayList<>();
        this.deep++;
        expect(TokenType.L_BRACE);
        while(!check(TokenType.R_BRACE)){
            if(check(TokenType.LET_KW)){
                instructions.addAll(analyseLet());
            } else if(check(TokenType.CONST_KW)){
                instructions.addAll(analyseConst());
            } else if(check(TokenType.IF_KW)){
                instructions.addAll(analyseIfStmt());
            }  else if(check(TokenType.WHILE_KW)){
                instructions.addAll(analyseWhileStmt());
            } else if(check(TokenType.RETURN_KW)){
                instructions.addAll(analyseReturnStmt());
            } else if(check(TokenType.L_BRACE)){
                instructions.addAll(analyseBlockStmt());
            } else if(check(TokenType.SEMICOLON)){
                instructions.addAll(analyseEmptyStmt());
            } else{
                instructions.addAll(analyseExpr());
                expect(TokenType.SEMICOLON);
                instructions.addAll(OperatorTree.addAllReset());
            }
        }
        expect(TokenType.R_BRACE);
        this.table.outDeep(this.deep);
        this.deep--;
        return instructions;
    }

    private List<Instruction> analyseIfStmt() throws CompileError {
        expect(TokenType.IF_KW);
        ConditionTree conditionTree=new ConditionTree();
        BooleanTree booleanTree=analyseBooleanExpr();
        booleanTree.setTrueInstructions(analyseBlockStmt());
        conditionTree.add(booleanTree);
        while(check(TokenType.ELSE_KW)){
            expect(TokenType.ELSE_KW);
            if(check(TokenType.IF_KW)){
                expect(TokenType.IF_KW);
                booleanTree=analyseBooleanExpr();
                booleanTree.setTrueInstructions(analyseBlockStmt());
                conditionTree.add(booleanTree);
            }
            else{
                booleanTree.setInstructions(new ArrayList<>());
                booleanTree.setOffset(new Instruction(Operation.br,(long)1));
                booleanTree.setTrueInstructions(analyseBlockStmt());
                conditionTree.add(booleanTree);
                break;
            }   
        }
        return conditionTree.generate();
    }

    private List<Instruction> analyseWhileStmt() throws CompileError {
        expect(TokenType.WHILE_KW);
        WhileTree whileTree=new WhileTree();
        BooleanTree booleanTree=analyseBooleanExpr();
        booleanTree.setTrueInstructions(analyseBlockStmt());
        whileTree.setBooleanTree(booleanTree);
        return whileTree.generate();
    }

    
    private List<Instruction> analyseEmptyStmt() throws CompileError {
        expect(TokenType.SEMICOLON);
        return new ArrayList<>();
    }

    private List<Instruction> analyseReturnStmt() throws CompileError {
        Token token=expect(TokenType.RETURN_KW);
        List<Instruction> instructions=new ArrayList<>();
        if(!check(TokenType.SEMICOLON)){
            if (this.table.getNowFuncTable().getTokenType()==TokenType.VOID_KW)
                throw new AnalyzeError(ErrorCode.WrongReturn, token.getStartPos());
            instructions.add(new Instruction(Operation.arga,(long)0));
            instructions.addAll(analyseExpr());
            instructions.add(new Instruction(Operation.store_64));
        }
        expect(TokenType.SEMICOLON);
        instructions.add(new Instruction(Operation.ret));
        return instructions;
    }

    private BooleanTree analyseBooleanExpr() throws CompileError {
        BooleanTree booleanTree=new BooleanTree();
        Instruction b;
        List<Instruction> instructions = new ArrayList<Instruction>(analyseExpr());
        if(nextIf(TokenType.EQ)!=null){
            instructions.addAll(analyseExpr());
            instructions.add(new Instruction(Operation.cmp_i));
            b=new Instruction(Operation.br_false,(long)-1);
        }else if(nextIf(TokenType.NEQ)!=null){
            instructions.addAll(analyseExpr());
            instructions.add(new Instruction(Operation.cmp_i));
            b=new Instruction(Operation.br_true,(long)-1);
        }else if(nextIf(TokenType.LT)!=null){
            instructions.addAll(analyseExpr());
            instructions.add(new Instruction(Operation.cmp_i));
            instructions.add(new Instruction(Operation.set_lt));
            b=new Instruction(Operation.br_true,(long)-1);
        }else if(nextIf(TokenType.GT)!=null){
            instructions.addAll(analyseExpr());
            instructions.add(new Instruction(Operation.cmp_i));
            instructions.add(new Instruction(Operation.set_gt));
            b=new Instruction(Operation.br_true,(long)-1);
        }else if(nextIf(TokenType.LE)!=null){
            instructions.addAll(analyseExpr());
            instructions.add(new Instruction(Operation.cmp_i));
            instructions.add(new Instruction(Operation.set_gt));
            b=new Instruction(Operation.br_false,(long)-1);
        }else if(nextIf(TokenType.GE)!=null){
            instructions.addAll(analyseExpr());
            instructions.add(new Instruction(Operation.cmp_i));
            instructions.add(new Instruction(Operation.set_lt));
            b=new Instruction(Operation.br_false,(long)-1);
        }else{
            b=new Instruction(Operation.br_true,(long)-1);
        }
        booleanTree.setInstructions(instructions);
        booleanTree.setOffset(b);
        return booleanTree;
    }

    private List<Instruction> analyseDeclStmt() throws CompileError {
        List<Instruction> instructions=new ArrayList<>();
        if(check(TokenType.LET_KW)){
            instructions.addAll(analyseLet());
        }else if(check(TokenType.CONST_KW)){
            instructions.addAll(analyseConst());
        }else{
            throw new ExpectedTokenError(getList( TokenType.LET_KW, TokenType.CONST_KW), next());
        }
        return instructions;
    }

    private List<Instruction> analyseLet() throws CompileError {
        List<Instruction> instructions=new ArrayList<>();
        expect(TokenType.LET_KW);
        Token nameToken = expect(TokenType.IDENT);
        expect(TokenType.COLON);
        Token ty=expectTy();
        if(nextIf(TokenType.ASSIGN)!=null){
            addSymbol(nameToken,NameType.Var,ty.getTokenType(),this.deep,true,false,nameToken.getStartPos());
            //获得变量地址
            instructions.add(getVarOrParamAddress(nameToken.getValueString(), nameToken.getStartPos()));
            instructions.addAll(analyseExpr());
            instructions.add(new Instruction(Operation.store_64));
        }
        else
            addSymbol(nameToken,NameType.Var,ty.getTokenType(),this.deep,false,false,nameToken.getStartPos());
        expect(TokenType.SEMICOLON);
        return instructions;
    }

    private List<Instruction> analyseConst() throws CompileError {
        List<Instruction> instructions=new ArrayList<>();
        expect(TokenType.LET_KW);
        Token nameToken = expect(TokenType.IDENT);
        expect(TokenType.COLON);
        Token ty=expectTy();
        expect(TokenType.ASSIGN);
        addSymbol(nameToken,NameType.Var,ty.getTokenType(),this.deep,true,false,nameToken.getStartPos());
        //获得变量地址
        instructions.add(getVarOrParamAddress(nameToken.getValueString(), nameToken.getStartPos()));
        instructions.addAll(analyseExpr());
        instructions.add(new Instruction(Operation.store_64));
        expect(TokenType.SEMICOLON);
        return instructions;
    }

    private boolean isOperatior() throws TokenizeError {
        TokenType ty = peek().getTokenType();
        if(ty == TokenType.PLUS || ty == TokenType.MINUS || ty == TokenType.MUL || ty == TokenType.DIV)
            return true;
        else
            return false;
    }

    private List<Instruction> analyseExpr() throws CompileError {
        List<Instruction> instructions = new ArrayList<>();

        if(check(TokenType.MINUS)){
            instructions.addAll(analyseNegateExpr());
        } else if(check(TokenType.IDENT)){
            // System.out.println("used.");
            Token token = expect(TokenType.IDENT);
            if(check(TokenType.ASSIGN)){
                instructions.addAll(analyseAssignExpr(token));
            } else if(check(TokenType.L_PAREN)){
                instructions.addAll(analyseCallExpr(token));
            } else {
                instructions.addAll(analyseIdentExpr(token));
            }
        } else if(check(TokenType.L_PAREN)){
            instructions.addAll(analyseGroupExpr());
        } else {
            instructions.addAll(analyseLiteralExpr());
        }

        if(isOperatior()){
            instructions.addAll(analyseOperatorExpr());
        } else if(check(TokenType.AS_KW)){
            analyseAsExpr();
        }

        return instructions;
    }

    private List<Instruction> analyseNegateExpr() throws CompileError {
        List<Instruction> instructions = new ArrayList<>();
        expect(TokenType.MINUS);
        instructions.add(new Instruction(Operation.push,(long)0));
        instructions.addAll(analyseExpr());
        instructions.add(new Instruction(Operation.sub_i));
        return instructions;
    }

    private List<Instruction> analyseAssignExpr(Token token) throws CompileError {
        List<Instruction> instructions = new ArrayList<>();
        expectNotConstant(token);
        expect(TokenType.ASSIGN);
        instructions.add(getVarOrParamAddress(token.getValueString(), token.getStartPos()));
        instructions.addAll(analyseExpr());
        instructions.add(new Instruction(Operation.store_64));
        declareSymbol(token.getValueString(), token.getStartPos());
        return instructions;
    }

    private List<Instruction> analyseCallExpr(Token token) throws CompileError {
        List<Instruction> instructions = new ArrayList<>();
        List<TokenType> paraTypes = this.table.getFunctionParamsType(token);
        expect(TokenType.L_PAREN);
        instructions.addAll(this.table.addstackllocInstruction(token.getValueString()));

        if(check(TokenType.R_PAREN) && paraTypes.size() == 0 ){
            ;
        } else if (!check(TokenType.R_PAREN) && paraTypes.size() > 0){
            instructions.addAll(analyseCallParamList(paraTypes));
        } else {
            Token nextToken=peek();
            throw new AnalyzeError(ErrorCode.WrongParamsNum, nextToken.getStartPos());
        }
        expect(TokenType.R_PAREN);
        if(this.table.checkOutFunc(token.getValueString())){
            instructions.add(new Instruction(Operation.callname,(long)this.table.getGlobalId(token)));
        }else{
            instructions.add(new Instruction(Operation.call,this.table.getFunclId(token)));
        }
        
        return instructions;
    }

    private List<Instruction> analyseCallParamList(List<TokenType> tokenTypes) throws CompileError {
        List<Instruction> instructions=new ArrayList<>();
        instructions.addAll(analyseExpr());
        instructions.addAll(OperatorTree.addAllReset());
        int i;
        for(i=1;i<tokenTypes.size();i++){
            if(check(TokenType.COMMA)){
                expect(TokenType.COMMA);
                instructions.addAll(analyseExpr());
                instructions.addAll(OperatorTree.addAllReset());
            } else {
                Token nameToken=next();
                throw new AnalyzeError(ErrorCode.WrongParamsNum, nameToken.getStartPos());
            }
        }
        return instructions;
    }

    
    private List<Instruction> analyseIdentExpr(Token token) throws CompileError {
        List<Instruction> instructions=new ArrayList<>();
        if(!isInitialized(token.getValueString(), token.getStartPos())) {
            throw new AnalyzeError(ErrorCode.NotInitialized, token.getStartPos());
        }
        instructions.add(getVarOrParamAddress(token.getValueString(), token.getStartPos()));
        instructions.add(new Instruction(Operation.load_64));
        return instructions;
    }

    private List<Instruction> analyseGroupExpr() throws CompileError {
        List<Instruction> instructions=new ArrayList<>();
        expect(TokenType.L_PAREN);
        instructions.addAll(OperatorTree.getNewOperator(TokenType.L_PAREN));
        instructions.addAll(analyseExpr());
        expect(TokenType.R_PAREN);
        instructions.addAll(OperatorTree.getNewOperator(TokenType.R_PAREN));
        return instructions;
    }

    private List<Instruction> analyseLiteralExpr() throws CompileError {
        List<Instruction> instructions=new ArrayList<>();
        Token token = expectLiteral();
        TokenType ty = token.getTokenType();
        if(ty == TokenType.UINT_LITERAL){
            instructions.add(new Instruction(Operation.push, (long)token.getValue()));
        } else {
            addSymbol(token,NameType.Var,TokenType.STRING_LITERAL,1,true,true,token.getStartPos());
            instructions.add(getStringAddress(token));
        }
        return instructions;
    }

    private List<Instruction> analyseOperatorExpr() throws CompileError {
        List<Instruction> instructions=new ArrayList<>();
        Token operator=next();
        instructions.addAll(OperatorTree.getNewOperator(operator.getTokenType()));
        instructions.addAll(analyseExpr());
        return instructions;
    }

    private Token analyseAsExpr() throws CompileError {
        expect(TokenType.AS_KW);
        Token ty=expectTy();
        return ty;
    }



//     private void analyseMain() throws CompileError {
//         // 主过程 -> 常量声明 变量声明 语句序列

//         analyseConstantDeclaration();
//         analyseVariableDeclaration();
//         analyseStatementSequence();

//         //throw new Error("Not implemented");
//     }

//     private void analyseConstantDeclaration() throws CompileError {
//         // 示例函数，示例如何解析常量声明
//         // 常量声明 -> 常量声明语句*

//         // 如果下一个 token 是 const 就继续
//         while (nextIf(TokenType.Const) != null) {
//             // 常量声明语句 -> 'const' 变量名 '=' 常表达式 ';'

//             // 变量名
//             var nameToken = expect(TokenType.Ident);

//             // 加入符号表
//             String name = (String) nameToken.getValue();
//             addSymbol(name, true, true, nameToken.getStartPos());

//             // 等于号
//             expect(TokenType.Equal);

//             // 常表达式
//             int value = analyseConstantExpression();

//             // 分号
//             expect(TokenType.Semicolon);

//             // 这里把常量值直接放进栈里，位置和符号表记录的一样。
//             // 更高级的程序还可以把常量的值记录下来，遇到相应的变量直接替换成这个常数值，
//             // 我们这里就先不这么干了。
//             instructions.add(new Instruction(Operation.LIT, value));
//         }
//     }

//     private void analyseVariableDeclaration() throws CompileError {
//         // 变量声明 -> 变量声明语句*

//         // 如果下一个 token 是 var 就继续
//         while (nextIf(TokenType.Var) != null) {
//             // 变量声明语句 -> 'var' 变量名 ('=' 表达式)? ';'

//             // 变量名
//             var nameToken = expect(TokenType.Ident);

//             // 变量初始化了吗
//             boolean initialized = false;

//             // 下个 token 是等于号吗？如果是的话分析初始化
//             if(nextIf(TokenType.Equal) != null){
//                 analyseExpression();
//                 initialized = true;
//             }

//             // 分析初始化的表达式

//             // 分号
//             expect(TokenType.Semicolon);

//             // 加入符号表，请填写名字和当前位置（报错用）
//             String name = (String) nameToken.getValue();
//             addSymbol(name, initialized, false, nameToken.getStartPos());

//             // 如果没有初始化的话在栈里推入一个初始值
//             if (!initialized) {
//                 instructions.add(new Instruction(Operation.LIT, 0));
//             }
//         }
//     }

//     private void analyseStatementSequence() throws CompileError {
//         // 语句序列 -> 语句*
//         // 语句 -> 赋值语句 | 输出语句 | 空语句

//         while (true) {
//             // 如果下一个 token 是……
//             var peeked = peek();
//             if (peeked.getTokenType() == TokenType.Ident) {
//                 analyseAssignmentStatement();
//             } else if(peeked.getTokenType() == TokenType.Semicolon) {
//                 expect(TokenType.Semicolon);
//             } else if(peeked.getTokenType() == TokenType.Print) {
//                 analyseOutputStatement();
//             } else {
//                 break;
//             }
//         }
//         //throw new Error("Not implemented");
//     }

//     private int analyseConstantExpression() throws CompileError {
//         // 常表达式 -> 符号? 无符号整数
//         boolean negative = false;
//         if (nextIf(TokenType.Plus) != null) {
//             negative = false;
//         } else if (nextIf(TokenType.Minus) != null) {
//             negative = true;
//         }

//         var token = expect(TokenType.Uint);

//         int value = (int) token.getValue();
//         if (negative) {
//             value = -value;
//         }

//         return value;
//     }

//     private void analyseExpression() throws CompileError {
//         // 表达式 -> 项 (加法运算符 项)*
//         // 项
//         analyseItem();

//         while (true) {
//             // 预读可能是运算符的 token
//             var op = peek();
//             if (op.getTokenType() != TokenType.Plus && op.getTokenType() != TokenType.Minus) {
//                 break;
//             }

//             // 运算符
//             next();

//             // 项
//             analyseItem();

//             // 生成代码
//             if (op.getTokenType() == TokenType.Plus) {
//                 instructions.add(new Instruction(Operation.ADD));
//             } else if (op.getTokenType() == TokenType.Minus) {
//                 instructions.add(new Instruction(Operation.SUB));
//             }
//         }
//     }

//     private void analyseAssignmentStatement() throws CompileError {
//         // 赋值语句 -> 标识符 '=' 表达式 ';'

//         // 分析这个语句
//         var nameToken = expect(TokenType.Ident);
//         expect(TokenType.Equal);
//         analyseExpression();
//         expect(TokenType.Semicolon);

//         // 标识符是什么？
//         String name = (String) nameToken.getValue();
//         Pos curPos = nameToken.getStartPos();
//         var symbol = symbolTable.get(name);
//         if (symbol == null) {
//             // 没有这个标识符
//             throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
//         } else if (symbol.isConstant) {
//             // 标识符是常量
//             throw new AnalyzeError(ErrorCode.AssignToConstant, curPos);
//         }
//         // 设置符号已初始化
//         initializeSymbol(name, curPos);

//         // 把结果保存
//         var offset = getOffset(name, curPos);
//         instructions.add(new Instruction(Operation.STO, offset));
//     }

//     private void analyseOutputStatement() throws CompileError {
//         // 输出语句 -> 'print' '(' 表达式 ')' ';'

//         expect(TokenType.Print);
//         expect(TokenType.LParen);

//         analyseExpression();

//         expect(TokenType.RParen);
//         expect(TokenType.Semicolon);

//         instructions.add(new Instruction(Operation.WRT));
//     }

//     private void analyseItem() throws CompileError {
//         // 项 -> 因子 (乘法运算符 因子)*

//         // 因子
//         analyseFactor();

//         while (true) {
//             // 预读可能是运算符的 token
//             var op = peek();
//             if (op.getTokenType() != TokenType.Mult && op.getTokenType() != TokenType.Div) {
//                 break;
//             }

//             // 运算符
//             next();

//             // 因子
//             analyseFactor();

//             // 生成代码
//             if (op.getTokenType() == TokenType.Mult) {
//                 instructions.add(new Instruction(Operation.MUL));
//             } else if (op.getTokenType() == TokenType.Div) {
//                 instructions.add(new Instruction(Operation.DIV));
//             }
//         }
//     }

//     private void analyseFactor() throws CompileError {
//         // 因子 -> 符号? (标识符 | 无符号整数 | '(' 表达式 ')')

//         boolean negate;
//         if (nextIf(TokenType.Minus) != null) {
//             negate = true;
//             // 计算结果需要被 0 减
//             instructions.add(new Instruction(Operation.LIT, 0));
//         } else {
//             nextIf(TokenType.Plus);
//             negate = false;
//         }

//         if (check(TokenType.Ident)) {
//             // 是标识符
//             var nameToken = expect(TokenType.Ident);

//             // 加载标识符的值
//             String name = (String) nameToken.getValue();
//             Pos curPos = nameToken.getStartPos();
//             var symbol = symbolTable.get(name);
//             if (symbol == null) {
//                 // 没有这个标识符
//                 throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
//             } else if (!symbol.isInitialized) {
//                 // 标识符没初始化
//                 throw new AnalyzeError(ErrorCode.NotInitialized, curPos);
//             }
//             var offset = getOffset(name, curPos);
//             instructions.add(new Instruction(Operation.LOD, offset));
//         } else if (check(TokenType.Uint)) {
//             // 是整数
//             var nameToken = expect(TokenType.Uint);

//             // 加载整数值
//             int value = (int) nameToken.getValue();
//             instructions.add(new Instruction(Operation.LIT, value));
//         } else if (check(TokenType.LParen)) {
//             // 是表达式
//             // 调用相应的处理函数

//             expect(TokenType.LParen);
//             analyseExpression();
//             expect(TokenType.RParen);
//         } else {
//             // 都不是，摸了
//             throw new ExpectedTokenError(List.of(TokenType.Ident, TokenType.Uint, TokenType.LParen), next());
//         }

//         if (negate) {
//             instructions.add(new Instruction(Operation.SUB));
//         }
//         //throw new Error("Not implemented");
//     }
}
