import syntaxtree.*;
import visitor.*;

import java.io.*;
import java.nio.file.*;

public class Main {
    public static void main(String[] args) throws Exception {

        FileInputStream fis = null;
        FileInputStream fis2 = null;
        FileInputStream fis3 = null;
        for(int i=0; i<args.length; i++) {
            try {
                Path path = Paths.get(args[i]);
                Path filename = path.getFileName();

                String[] info = filename.toString().split("\\.");
                String ll_file = info[0];

                ll_file+=".ll";
                File file = new File(ll_file);

                PrintStream stream = new PrintStream(file);
                System.setOut(stream);

                fis = new FileInputStream(args[i]);
                MiniJavaParser parser = new MiniJavaParser(fis);
                Goal root = parser.Goal();
                ST_visitor eval = new ST_visitor();
                root.accept(eval, null);
                fis2 = new FileInputStream(args[i]);
                MiniJavaParser parser2 = new MiniJavaParser(fis2);
                Goal root2 = parser2.Goal();
                SecondVisitor eval2 = new SecondVisitor(eval.symbol_table);
                root.accept(eval2, null);
                fis3 = new FileInputStream(args[i]);
                MiniJavaParser parser3 = new MiniJavaParser(fis3);
                Goal root3 = parser3.Goal();
                llvmGenerator eval3 = new llvmGenerator(eval2.symbol_table);
                root.accept(eval3, null);
            } catch (ParseException ex) {
                System.out.println(ex.getMessage());
            } catch (FileNotFoundException ex) {
                System.err.println(ex.getMessage());
            } catch(Exception ex) {
                System.err.println(ex);
            }finally {
                try {
                    if (fis != null) fis.close();
                } catch (IOException ex) {
                    System.err.println(ex.getMessage());
                }
            }
        }
    }
}
