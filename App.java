import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class App {
    public static void main(String[] args) throws CompileError {

        var inputFileName = args[1];
        var outputFileName = args[2];

        InputStream input;
        if (inputFileName.equals("-")) {
            input = System.in;
        } else {
            try {
                input = new FileInputStream(inputFileName);
            } catch (FileNotFoundException e) {
                System.err.println("Cannot find input file.");
                e.printStackTrace();
                System.exit(2);
                return;
            }
        }

        PrintStream output = System.out;

        DataOutputStream biOutput;
        if (outputFileName.equals("-")) {
            biOutput = new DataOutputStream(System.out);
        } else {
            try {
                biOutput = new DataOutputStream(new FileOutputStream(new File(outputFileName)));
            } catch (FileNotFoundException e) {
                System.err.println("Cannot open output file.");
                e.printStackTrace();
                System.exit(2);
                return;
            }
        }

        Scanner scanner;
        scanner = new Scanner(input);
        var iter = new StringIter(scanner);
        var tokenizer = tokenize(iter);

        if (args[0].equals("t")) {
            // tokenize
            var tokens = new ArrayList<Token>();
            try {
                while (true) {
                    var token = tokenizer.nextToken();
                    if (token.getTokenType().equals(TokenType.EOF)) {
                        break;
                    }
                    tokens.add(token);
                }
            } catch (Exception e) {
                System.err.println(e);
                System.exit(0);
                return;
            }
            if(output != null){
                for (Token token : tokens) {
                    output.println(token.toString());
                }
            }
        } else if (args[0].equals("l")) {
            // analyze
            var analyzer = new Analyser(tokenizer);
            List<Instruction> instructions;
            Table table = new Table();
            List<FunctionTable> functionTables;
            List<Token> global;
            table = analyzer.analyse();
            global=table.getGlobal();
            functionTables=table.getFunctionTables();
            // for(Token token:global){
            //     output.println(token);
            // }
            // for (FunctionTable function:functionTables){
            //     output.println(function.getName()+" pos:"+function.getPos()+"params:"+function.getParamSoltNum()+" var:"+function.getVarSoltNum()+" ->"+function.getReturnSoltNum());
            //     output.println(function.getInstructions());
            // }

            OutPutBinary outPutBinary = new OutPutBinary(table);
            List<Byte> bs = outPutBinary.generate();
            byte[] temp = new byte[bs.size()];
            for (int i = 0; i < bs.size(); i++)
                temp[i] = bs.get(i);
            try {
                biOutput.write(temp);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }


        } else {
            System.err.println("Please specify either '--analyse' or '--tokenize'.");
            System.exit(3);
        }
    }

    // private static ArgumentParser buildArgparse() {
    //     var builder = ArgumentParsers.newFor("miniplc0-java");
    //     var parser = builder.build();
    //     parser.addArgument("-t", "--tokenize").help("Tokenize the input").action(Arguments.storeTrue());
    //     parser.addArgument("-l", "--analyse").help("Analyze the input").action(Arguments.storeTrue());
    //     parser.addArgument("-o", "--output").help("Set the output file").required(true).dest("output")
    //             .action(Arguments.store());
    //     parser.addArgument("file").required(true).dest("input").action(Arguments.store()).help("Input file");
    //     return parser;
    // }

    private static Tokenizer tokenize(StringIter iter) {
        var tokenizer = new Tokenizer(iter);
        return tokenizer;
    }
}

// javac -encoding utf-8 App.java && java App l input.txt output.txt