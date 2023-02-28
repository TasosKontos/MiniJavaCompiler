import java.util.*;
import java.util.Arrays;
import syntaxtree.*;
import visitor.*;

public class SecondVisitor extends GJDepthFirst<String, String>{
    Symbol_Table symbol_table;
    String current_class;
    String current_method;

    ArrayList<String> primitive_types;

    SecondVisitor(Symbol_Table table){
        symbol_table=table;
        primitive_types =new ArrayList<String>(Arrays.asList("int", "int[]", "boolean", "boolean[]"));
    }

    void SymbolTable_Check() throws Exception{

        Set<SymbolTable_Key> keys = symbol_table.variable_table.keySet();
        for(SymbolTable_Key key : keys){
            if(!symbol_table.Search_Class(key.class_name) && !this.primitive_types.contains(key.class_name)) throw new Exception("Class type of declared variable doesnt exist!");
            if(key.class_name.equals(symbol_table.top_class)) throw new Exception("Class type of variable cannot be that of program top class!");
        }

        keys = symbol_table.function_table.keySet();
        for(SymbolTable_Key key : keys){
            if(key.class_name.equals(symbol_table.top_class) && key.var_name.equals("main")) continue;
            FunctTable_Value info = symbol_table.function_table.get(key);

            Set<String> inner_keys = info.args.keySet();
            for(String inner_key:inner_keys){
                if(!symbol_table.Search_Class(info.args.get(inner_key)) && !this.primitive_types.contains(info.args.get(inner_key))) throw new Exception("Class type of declared method argument doesnt exist!");
                if(info.args.get(inner_key).equals(symbol_table.top_class)) throw new Exception("Class type of method argument cannot be that of program top class!");
            }

            inner_keys = info.local_vars.keySet();
            for(String inner_key:inner_keys){
                if(!symbol_table.Search_Class(info.local_vars.get(inner_key)) && !this.primitive_types.contains(info.local_vars.get(inner_key))) throw new Exception("Class type of declared method local variable doesnt exist!");
                if(info.local_vars.get(inner_key).equals(symbol_table.top_class)) throw new Exception("Class type of method local variable cannot be that of program top class!");
            }

            if(!symbol_table.Search_Class(info.return_type) && !this.primitive_types.contains(info.return_type)) throw new Exception("Class type of method return type doesnt exist");
            if(info.return_type.equals(symbol_table.top_class)) throw new Exception("Method cannot return class type of program top class!");
        }
    }

    /**
     * f0 -> MainClass()
     * f1 -> ( TypeDeclaration() )*
     * f2 -> <EOF>
     */
    @Override
    public String visit(Goal n, String argu) throws Exception {
        this.SymbolTable_Check();
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        //symbol_table.Print_Offsets();
        symbol_table.Fill_Var_Map();
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
        n.f0.accept(this, argu);
        this.current_class = n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        n.f5.accept(this, argu);
        this.current_method = "main";
        n.f7.accept(this, argu);
        n.f8.accept(this, argu);
        n.f9.accept(this, argu);
        n.f10.accept(this, argu);
        n.f11.accept(this, argu);
        n.f12.accept(this, argu);
        n.f13.accept(this, argu);
        n.f14.accept(this, argu);
        n.f15.accept(this, argu);
        n.f16.accept(this, argu);
        n.f17.accept(this, argu);
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
        n.f0.accept(this, argu);
        this.current_class = n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        n.f3.accept(this, "offset");
        n.f4.accept(this, argu);
        n.f5.accept(this, argu);
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
        n.f0.accept(this, argu);
        this.current_class = n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        n.f5.accept(this, "offset");
        n.f6.accept(this, argu);
        n.f7.accept(this, argu);
        return null;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     */
    public String visit(VarDeclaration n, String argu) throws Exception {
        String type = n.f0.accept(this, argu);
        String name = n.f1.accept(this, argu);
        if(argu!=null) symbol_table.Insert_Offset_Var(name, this.current_class, type);
        n.f2.accept(this, argu);
        return null;
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
        n.f0.accept(this, argu);
        String declared_type = n.f1.accept(this, argu);
        this.current_method = n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        n.f5.accept(this, argu);
        n.f6.accept(this, argu);
        n.f7.accept(this, argu);
        n.f8.accept(this, argu);
        n.f9.accept(this, argu);
        String return_type = n.f10.accept(this, argu);
        if(!return_type.equals(declared_type)) throw new Exception("Method must return value of the declared type!");
        n.f11.accept(this, argu);
        n.f12.accept(this, argu);
        symbol_table.Insert_Offset_Method(this.current_method, this.current_class);
        return null;
    }


    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Expression()
     * f3 -> ";"
     */
    @Override
    public String visit(AssignmentStatement n, String argu) throws Exception {
        String id = n.f0.accept(this, argu);
        String type = symbol_table.Search_and_Return(id, this.current_class, this.current_method, 'v');
        if(type==null) throw new Exception("Variable not found in symbol table in assignment statement!");
        n.f1.accept(this, argu);
        String exp = n.f2.accept(this, argu);
        if(!type.equals(exp)){
            if(primitive_types.contains(type) || primitive_types.contains(exp)) throw new Exception("Type error in assignment statement!");
            if(symbol_table.classes.get(exp).isEmpty()|| !symbol_table.classes.get(exp).contains(type)) throw new Exception("Type error in assignment statement!");
        }
        n.f3.accept(this, argu);
        return null;
    }

    /**
     * f0 -> Identifier()
     * f1 -> "["
     * f2 -> Expression()
     * f3 -> "]"
     * f4 -> "="
     * f5 -> Expression()
     * f6 -> ";"
     */
    @Override
    public String visit(ArrayAssignmentStatement n, String argu) throws Exception {
        String id = n.f0.accept(this, argu);
        String type = symbol_table.Search_and_Return(id, this.current_class, this.current_method, 'v');
        if(type==null || !(type.equals("int[]") || type.equals("boolean[]"))) throw new Exception("Identifier not of array type in array assignment!");
        n.f1.accept(this, argu);
        String length = n.f2.accept(this, argu);
        if(!length.equals("int")) throw new Exception("Index not of type int in array assignment!");
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        String exp = n.f5.accept(this, argu);
        if((type.equals("int[]") && !exp.equals("int"))||(type.equals("boolean[]") && !exp.equals("boolean")))
            throw new Exception("Incompatible array type and expression type in array assignment!");
        n.f6.accept(this, argu);
        return null;
    }

    /**
     * f0 -> "if"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     * f5 -> "else"
     * f6 -> Statement()
     */
    @Override
    public String visit(IfStatement n, String argu) throws Exception {
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        String exp = n.f2.accept(this, argu);
        if(!exp.equals("boolean")) throw new Exception("If condition is not of type boolean!");
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        n.f5.accept(this, argu);
        n.f6.accept(this, argu);
        return null;
    }


    /**
     * f0 -> "while"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     */
    @Override
    public String visit(WhileStatement n, String argu) throws Exception {
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        String exp = n.f2.accept(this, argu);
        if(!exp.equals("boolean")) throw new Exception("While condition is not of type boolean!");
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        return null;
    }

    /**
     * f0 -> "System.out.println"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> ";"
     */
    public String visit(PrintStatement n, String argu) throws Exception {
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        String print_arg = n.f2.accept(this, argu);
        if(!print_arg.equals("boolean") && !print_arg.equals("int")) throw new Exception("Invalid expression in print statement!");
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        return null;
    }

    /**
     * f0 -> Clause()
     * f1 -> "&&"
     * f2 -> Clause()
     */
    @Override
    public String visit(AndExpression n, String argu) throws Exception {
        String term1 = n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        String term2 = n.f2.accept(this, argu);
        if(!term1.equals("boolean") || !term2.equals("boolean")) throw new Exception("And expression type error!");
        return "boolean";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "<"
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(CompareExpression n, String argu) throws Exception {
        String term1 = n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        String term2 = n.f2.accept(this, argu);
        if(!term1.equals("int") || !term2.equals("int")) throw new Exception("Compare expression type error!");
        return "boolean";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "+"
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(PlusExpression n, String argu) throws Exception {
        String term1 = n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        String term2 = n.f2.accept(this, argu);
        if(!term1.equals("int") || !term2.equals("int")) throw new Exception("Plus expression type error!");
        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "-"
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(MinusExpression n, String argu) throws Exception {
        String term1 = n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        String term2 = n.f2.accept(this, argu);
        if(!term1.equals("int") || !term2.equals("int")) throw new Exception("Minus expression type error!");
        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "*"
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(TimesExpression n, String argu) throws Exception {
        String term1 = n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        String term2 = n.f2.accept(this, argu);
        if(!term1.equals("int") || !term2.equals("int")) throw new Exception("Times expression type error!");
        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "["
     * f2 -> PrimaryExpression()
     * f3 -> "]"
     */
    @Override
    public String visit(ArrayLookup n, String argu) throws Exception {
        String table_name = n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        String index = n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        if(!table_name.equals("int[]") && !table_name.equals("boolean[]")) throw new Exception("Array lookup type error!");
        if(!index.equals("int")) throw new Exception("Array lookup index type error!");

        if(table_name.equals("int[]")) return "int";
        else return "boolean";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> "length"
     */
    @Override
    public String visit(ArrayLength n, String argu) throws Exception {
        String table_name = n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        if(!table_name.equals("int[]") && !table_name.equals("boolean[]")) throw new Exception("Array length type error!");
        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( ExpressionList() )?
     * f5 -> ")"
     */
    @Override
    public String visit(MessageSend n, String argu) throws Exception {
        String variable_type = n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        String method_name = n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        String result = symbol_table.Search_and_Return(null,variable_type,method_name,'f');
        String [] info;
        String return_type=null, final_class=null;
        if(result!=null){
            info = result.split(" ");
            final_class = info[0];
            return_type = info[1];
        }

        if(return_type == null) throw new Exception("Message send function not found in symbol table!");
        String args = n.f4.accept(this, argu);
        n.f5.accept(this, argu);
        ArrayList<String> arg_list;
         if(args!=null)
             if(args.contains(","))
                 arg_list= new ArrayList<String>(Arrays.asList(args.split(",")));
             else
                 arg_list = new ArrayList<String>(Collections.singletonList(args));

        else
            arg_list = new ArrayList<String>();

        ArrayList<String> correct_args = symbol_table.function_table.get(new SymbolTable_Key(method_name, final_class)).GetArgTypeList();

        if(arg_list.size()!=correct_args.size()) throw new Exception("Wrong argument count in message send!");
        for(int i=0; i<arg_list.size(); i++){
            if(!arg_list.get(i).equals(correct_args.get(i))){
                if(!symbol_table.classes.containsKey(arg_list.get(i))) throw new Exception("Type error 1 in message send argument check!");
                if(!symbol_table.classes.get(arg_list.get(i)).contains(correct_args.get(i))) throw new Exception("Type error 2 in message send argument check!");
            }
        }

        return return_type;
    }

    /**
     * f0 -> Expression()
     * f1 -> ExpressionTail()
     */
    @Override
    public String visit(ExpressionList n, String argu) throws Exception {
        String t1=n.f0.accept(this, argu);
        String t2=n.f1.accept(this, argu);
        return t1+t2;
    }

    /**
     * f0 -> ( ExpressionTerm() )*
     */
    @Override
    public String visit(ExpressionTail n, String argu) throws Exception {
        String expt=null;
        for(Node node : n.f0.nodes){
            String temp = node.accept(this, null);
            if(temp!=null){
                if(expt==null)
                    expt=temp;
                else
                    expt+=temp;
            }
        }

        if(expt == null) return "";
        return expt;
    }

    /**
     * f0 -> ","
     * f1 -> Expression()
     */
    @Override
    public String visit(ExpressionTerm n, String argu) throws Exception {
        n.f0.accept(this, argu);
        String t2=n.f1.accept(this, argu);
        if(t2==null) return "";
        return ","+t2;
    }

    /**
     * f0 -> NotExpression()
     *       | PrimaryExpression()
     */
    @Override
    public String visit(Clause n, String argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> IntegerLiteral()
     *       | TrueLiteral()
     *       | FalseLiteral()
     *       | Identifier()
     *       | ThisExpression()
     *       | ArrayAllocationExpression()
     *       | AllocationExpression()
     *       | BracketExpression()
     */
    @Override
    public String visit(PrimaryExpression n, String argu) throws Exception {
        String prim = n.f0.accept(this, argu);
        if(!(this.primitive_types.contains(prim) || symbol_table.classes.containsKey(prim))){
            String type = symbol_table.Search_and_Return(prim, this.current_class, this.current_method, 'v');
            if(type==null) throw new Exception("Variable name not found in symbol table in primary expression!");
            return type;
        }
        return prim;
    }

    /**
     * f0 -> <INTEGER_LITERAL>
     */
    @Override
    public String visit(IntegerLiteral n, String argu) throws Exception {
        n.f0.accept(this, argu);
        return "int";
    }

    /**
     * f0 -> "true"
     */
    @Override
    public String visit(TrueLiteral n, String argu) throws Exception {
        n.f0.accept(this, argu);
        return "boolean";
    }

    /**
     * f0 -> "false"
     */
    @Override
    public String visit(FalseLiteral n, String argu) throws Exception {
        n.f0.accept(this, argu);
        return "boolean";
    }

    /**
     * f0 -> "this"
     */
    @Override
    public String visit(ThisExpression n, String argu) throws Exception {
        return this.current_class;
    }

    /**
     * f0 -> BooleanArrayAllocationExpression()
     *       | IntegerArrayAllocationExpression()
     */
    @Override
    public String visit(ArrayAllocationExpression n, String argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> "new"
     * f1 -> "boolean"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */
    @Override
    public String visit(BooleanArrayAllocationExpression n, String argu) throws Exception {
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        String exp = n.f3.accept(this, argu);
        if(!exp.equals("int")) throw new Exception("Not int length in boolean array allocation!");
        n.f4.accept(this, argu);
        return "boolean[]";
    }

    /**
     * f0 -> "new"
     * f1 -> "int"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */
    @Override
    public String visit(IntegerArrayAllocationExpression n, String argu) throws Exception {
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        String exp = n.f3.accept(this, argu);
        if(!exp.equals("int")) throw new Exception("Not int length in int array allocation!");
        n.f4.accept(this, argu);
        return "int[]";
    }

    /**
     * f0 -> "new"
     * f1 -> Identifier()
     * f2 -> "("
     * f3 -> ")"
     */
    @Override
    public String visit(AllocationExpression n, String argu) throws Exception {
        n.f0.accept(this, argu);
        String classname = n.f1.accept(this, argu);
        if(!symbol_table.classes.containsKey(classname)) throw new Exception("Class doesnt exist in allocation expression!");
        n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        return classname;
    }

    /**
     * f0 -> "!"
     * f1 -> PrimaryExpression()
     */
    @Override
    public String visit(NotExpression n, String argu) throws Exception {
        n.f0.accept(this, argu);
        String exp = n.f1.accept(this, argu);
        if(!exp.equals("boolean")) throw new Exception("Not expression is not boolean type!");
        return "boolean";
    }

    /**
     * f0 -> "("
     * f1 -> Expression()
     * f2 -> ")"
     */
    @Override
    public String visit(BracketExpression n, String argu) throws Exception {
        n.f0.accept(this, argu);
        String type = n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        return type;
    }

    @Override
    public String visit(Identifier n, String argu) {
        return n.f0.toString();
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

    /**
     * f0 -> "boolean"
     */
    public String visit(BooleanType n, String argu) throws Exception {
        n.f0.accept(this, argu);
        return "boolean";
    }

    /**
     * f0 -> "int"
     */
    public String visit(IntegerType n, String argu) throws Exception {
        n.f0.accept(this, argu);
        return "int";
    }
}
