import java.util.*;

import syntaxtree.*;
import visitor.*;

public class ST_visitor extends GJDepthFirst<String, String> {
    public Symbol_Table symbol_table = new Symbol_Table();



    /**
     * f0 -> MainClass()
     * f1 -> ( TypeDeclaration() )*
     * f2 -> <EOF>
     */
    public String visit(Goal n, String argu) throws Exception {

        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> "public"
     * f4 -> "static"
     * f5 -> "void"
     * f6 -> "main"
     * f7 -> "("
     * f8 -> "String"
     * f9 -> "["
     * f10 -> "]"
     * f11 -> Identifier()
     * f12 -> ")"
     * f13 -> "{"
     * f14 -> ( VarDeclaration() )*
     * f15 -> ( Statement() )*
     * f16 -> "}"
     * f17 -> "}"
     */
    @Override
    public String visit(MainClass n, String argu) throws Exception {
        String classname = n.f1.accept(this, null);
        symbol_table.Insert_Class(classname, null);
        symbol_table.top_class = classname;
        LinkedHashMap<String, String> args = new LinkedHashMap<String, String>();
        args.put(n.f11.accept(this, null), "String[]");

        LinkedHashMap<String, String> vars = new LinkedHashMap<String, String>();
        String var;
        for(Node node: n.f14.nodes){
            var=node.accept(this, null);
            String[] info = var.split(" ");
            if(vars.containsKey(info[1])) throw new Exception("Double variable declaration in method!");
            vars.put(info[1], info[0]);
        }

        symbol_table.Insert_Funct(new SymbolTable_Key("main", classname), new FunctTable_Value(args, vars, "void"));

        return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> ( VarDeclaration() )*
     * f4 -> ( MethodDeclaration() )*
     * f5 -> "}"
     */
    @Override
    public String visit(ClassDeclaration n, String argu) throws Exception {
        String classname = n.f1.accept(this, null);
        if(symbol_table.Search_Class(classname)) throw new Exception("Double class declaration!");
        symbol_table.Insert_Class(classname, null);

        symbol_table.Insert_V_table_Class(classname);

        for(Node node: n.f3.nodes){
            String var=node.accept(this, null);
            String[] info = var.split(" ");
            if(symbol_table.Search(new SymbolTable_Key(info[1], classname), 'v')) throw new Exception("Double declaration of class member!");
            symbol_table.Insert_Var(new SymbolTable_Key(info[1], classname), info[0]);
        }

        n.f4.accept(this, classname);

        return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "extends"
     * f3 -> Identifier()
     * f4 -> "{"
     * f5 -> ( VarDeclaration() )*
     * f6 -> ( MethodDeclaration() )*
     * f7 -> "}"
     */
    @Override
    public String visit(ClassExtendsDeclaration n, String argu) throws Exception {
        String classname = n.f1.accept(this, null);
        if(symbol_table.Search_Class(classname)) throw new Exception("Double class declaration!");
        String parent_name = n.f3.accept(this, null);
        if(!symbol_table.Search_Class(parent_name)) throw new Exception("Parent class not declared before child class declaration!");
        if(classname.equals(parent_name)) throw new Exception("Class and parent class with the same name!");

        symbol_table.Insert_Class(classname, parent_name);

        symbol_table.Insert_V_table_Class(classname);

        for(Node node: n.f5.nodes){
            String var=node.accept(this, null);
            String[] info = var.split(" ");
            if(symbol_table.Search(new SymbolTable_Key(info[1], classname), 'r')) throw new Exception("Double declaration of class member!");
            symbol_table.Insert_Var(new SymbolTable_Key(info[1], classname), info[0]);
        }

        n.f6.accept(this, classname);

        return null;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     */
    @Override
    public String visit(VarDeclaration n, String argu) throws Exception{
        return n.f0.accept(this, null) +" "+ n.f1.accept(this, null);
    }

    /**
     * f0 -> "public"
     * f1 -> Type()
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( FormalParameterList() )?
     * f5 -> ")"
     * f6 -> "{"
     * f7 -> ( VarDeclaration() )*
     * f8 -> ( Statement() )*
     * f9 -> "return"
     * f10 -> Expression()
     * f11 -> ";"
     * f12 -> "}"
     */
    @Override
    public String visit(MethodDeclaration n, String argu) throws Exception {

        String classname = argu;
        String type = n.f1.accept(this, null);
        String name = n.f2.accept(this, null);

        if(symbol_table.Search(new SymbolTable_Key(name, classname), 'f')) throw new Exception("Double method declaration in class!");

        String argumentList = n.f4.present() ? n.f4.accept(this, null) : null;
        LinkedHashMap<String, String> args = new LinkedHashMap<String, String>();
        if(argumentList!=null){
            String[] split = argumentList.split("[\\s,]");
            for(int i=0; i<split.length; i+=2){
                if(args.containsKey(split[i+1])) throw new Exception("Same argument name in method argument list!");
                args.put(split[i+1], split[i]);
            }
        }


        LinkedHashMap<String, String> vars = new LinkedHashMap<String, String>();
        String var;
        for(Node node: n.f7.nodes){
            var=node.accept(this, null);
            String[] info = var.split(" ");
            if(args.containsKey(info[1])) throw new Exception("Double variable declaration in method!(in var declaration and arg_list)");
            if(vars.containsKey(info[1])) throw new Exception("Double variable declaration in method!(in var declaration)");
            vars.put(info[1], info[0]);
        }

        SymbolTable_Key key = new SymbolTable_Key(name, classname);
        FunctTable_Value value = new FunctTable_Value(args, vars, type);
        ArrayList <String> arg_list_types = value.GetArgTypeList();
        symbol_table.Insert_Funct(key, value);

        ArrayList<String> ext_class = symbol_table.classes.get(classname);
        if(!ext_class.isEmpty()){
            ListIterator<String> iter = ext_class.listIterator(ext_class.size());
            while(iter.hasPrevious()){
                SymbolTable_Key key2 = new SymbolTable_Key(name, iter.previous());
                if(symbol_table.function_table.containsKey(key2)){
                    String parent_return_type = symbol_table.function_table.get(key2).return_type;
                    ArrayList<String> arg_list2 = symbol_table.function_table.get(key2).GetArgTypeList();
                    if(!type.equals(parent_return_type) || !arg_list_types.equals(arg_list2)) throw new Exception("Return types and arguments types must be the same in overriden methods between classes!");
                }
            }
        }

        symbol_table.Insert_V_table_Method(classname, name);

        return null;
    }

    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    @Override
    public String visit(FormalParameterList n, String argu) throws Exception {
        String ret = n.f0.accept(this, null);

        if (n.f1 != null) {
            ret += n.f1.accept(this, null);
        }

        return ret;
    }

    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    public String visit(FormalParameterTerm n, String argu) throws Exception {
        return n.f1.accept(this, argu);
    }

    /**
     * f0 -> ","
     * f1 -> FormalParameter()
     */
    @Override
    public String visit(FormalParameterTail n, String argu) throws Exception {
        String ret = "";
        for ( Node node: n.f0.nodes) {
            ret += "," + node.accept(this, null);
        }

        return ret;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */
    @Override
    public String visit(FormalParameter n, String argu) throws Exception{
        String type = n.f0.accept(this, null);
        String name = n.f1.accept(this, null);
        return type + " " + name;
    }

    /**
     * f0 -> "boolean"
     * f1 -> "["
     * f2 -> "]"
     */
    public String visit(BooleanArrayType n, String argu) throws Exception {
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        return "boolean[]";
    }

    /**
     * f0 -> "int"
     * f1 -> "["
     * f2 -> "]"
     */
    public String visit(IntegerArrayType n, String argu) throws Exception {
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        return "int[]";
    }

    public String visit(BooleanType n, String argu) {
        return "boolean";
    }

    public String visit(IntegerType n, String argu) {
        return "int";
    }

    @Override
    public String visit(Identifier n, String argu) {
        return n.f0.toString();
    }
}

