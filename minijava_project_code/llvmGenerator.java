import syntaxtree.*;
import visitor.*;
import java.util.*;


public class llvmGenerator extends GJDepthFirst<String, String>{
    String current_class;
    String current_method;

    int reg_counter=0;
    int label_counter=0;
    LinkedHashMap<String, String>regs_to_type;

    public Symbol_Table symbol_table;

    public SecondVisitor sv;

    public void emit(String print_str){
        System.out.println(print_str);
    }

    public llvmGenerator(Symbol_Table st){
        symbol_table=st;
        regs_to_type = new LinkedHashMap<String, String>();
        sv=new SecondVisitor(symbol_table);
    }

    public boolean isInt(String str){
        return str.matches("-?(0|[1-9]\\d*)");
    }

    public String jtype_to_llvm(String javatype){
        switch(javatype){
            case "int":
                return "i32";
            case "boolean":
                return "i1";
            case "boolean[]":
                return "i1*";
            case "int[]":
                return "i32*";
            default:
                return "i8*";
        }
    }

    public void Print_VT(){
        LinkedHashMap<String,LinkedHashMap<String, String>> vt = symbol_table.v_tables;
        List<String> reverse_order_keys = new ArrayList<>(symbol_table.v_tables.keySet());
        Collections.reverse(reverse_order_keys);
        for(String key : reverse_order_keys){
            LinkedHashMap<String, String> c_vt = vt.get(key);
            emit("@."+key+"_vtable = global ["+c_vt.size()+" x i8*] [");
            Set<String> cvt_keys = c_vt.keySet();
            int counter=1;
            for(String cvt_key : cvt_keys){
                FunctTable_Value ftv = symbol_table.function_table.get(new SymbolTable_Key(cvt_key, c_vt.get(cvt_key)));
                ArrayList<String> args = ftv.GetArgTypeList();
                StringBuilder args_str= new StringBuilder();
                for (String arg : args) args_str.append(",").append(jtype_to_llvm(arg));
                if(counter==c_vt.size())
                    emit("\ti8* bitcast ("+jtype_to_llvm(ftv.return_type)+" (i8*"+args_str+")* @"+c_vt.get(cvt_key)+"."+cvt_key+" to i8*)");
                else
                    emit("\ti8* bitcast ("+jtype_to_llvm(ftv.return_type)+" (i8*"+args_str+")* @"+c_vt.get(cvt_key)+"."+cvt_key+" to i8*),");
                counter++;
            }
            emit("]\n");
        }
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
        current_class = n.f1.accept(this ,argu);
        current_method = "main";
        Print_VT();
        emit("@."+ current_class+"_vtable = global [0 x i8*] []\n");
        emit("declare i8* @calloc(i32, i32)");
        emit("declare i32 @printf(i8*, ...)");
        emit("declare void @exit(i32)");
        emit("");
        emit("@_cint = constant [4 x i8] c\"%d\\0a\\00\"");
        emit("@_cOOB = constant [15 x i8] c\"Out of bounds\\0a\\00\"");
        emit("@_cNSZ = constant [15 x i8] c\"Negative size\\0a\\00\"");
        emit("");
        emit("define void @print_int(i32 %i) {");
        emit("\t%_str = bitcast [4 x i8]* @_cint to i8*");
        emit("\tcall i32 (i8*, ...) @printf(i8* %_str, i32 %i)");
        emit("\tret void");
        emit("}\n");
        emit("define void @throw_oob() {");
        emit("\t%_str = bitcast [15 x i8]* @_cOOB to i8*");
        emit("\tcall i32 (i8*, ...) @printf(i8* %_str)");
        emit("\tcall void @exit(i32 1)");
        emit("\tret void");
        emit("}\n");
        emit("define void @throw_nsz() {");
        emit("\t%_str = bitcast [15 x i8]* @_cNSZ to i8*");
        emit("\tcall i32 (i8*, ...) @printf(i8* %_str)");
        emit("\tcall void @exit(i32 1)");
        emit("\tret void");
        emit("}\n");
        emit("define i32 @main() {");
        n.f14.accept(this, "m");
        emit("");
        n.f15.accept(this, argu);
        emit("\tret i32 0");
        emit("}\n");
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
    public String visit(ClassDeclaration n, String argu) throws Exception {
        n.f0.accept(this, argu);
        current_class = n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        n.f3.accept(this, argu);
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
    public String visit(ClassExtendsDeclaration n, String argu) throws Exception {
        n.f0.accept(this, argu);
        current_class = n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        n.f5.accept(this, argu);
        n.f6.accept(this, argu);
        n.f7.accept(this, argu);
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
    public String visit(MethodDeclaration n, String argu) throws Exception {
        regs_to_type.clear();
        reg_counter=0;
        label_counter=0;
        n.f0.accept(this, argu);
        String type = n.f1.accept(this, argu);
        String name = n.f2.accept(this, argu);
        current_method = name;
        n.f3.accept(this, argu);
        String llvm_param = "(i8* %this";
        String fpl = n.f4.accept(this, argu);
        if(fpl!=null) llvm_param += ","+fpl;
        llvm_param += ")";
        n.f5.accept(this, argu);
        n.f6.accept(this, argu);
        emit("define "+jtype_to_llvm(type)+" @"+current_class+"."+name+llvm_param+" {");
        FunctTable_Value fn_v = symbol_table.function_table.get(new SymbolTable_Key(name, current_class));
        Set<String> keys = fn_v.args.keySet();
        for(String key : keys){
            emit("\t%"+key+" = alloca "+jtype_to_llvm(fn_v.args.get(key)));
            emit("\tstore "+jtype_to_llvm(fn_v.args.get(key))+" %."+key+", "+jtype_to_llvm(fn_v.args.get(key))+"* %"+key+"\n");
        }
        n.f7.accept(this, "m");
        n.f8.accept(this, argu);
        n.f9.accept(this, argu);
        String ret_reg = n.f10.accept(this, argu);
        emit("\tret "+jtype_to_llvm(type)+" "+ret_reg+"\n");
        n.f11.accept(this, argu);
        n.f12.accept(this, argu);
        emit("}\n");
        return null;
    }

    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    public String visit(FormalParameterList n, String argu) throws Exception {
        String result="";
        result+=n.f0.accept(this, argu);
        result+=n.f1.accept(this, argu);
        return result;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */
    public String visit(FormalParameter n, String argu) throws Exception {
        String type = n.f0.accept(this, argu);
        String name = n.f1.accept(this, argu);
        return (" "+jtype_to_llvm(type)+" "+"%."+name);
    }

    /**
     * f0 -> ( FormalParameterTerm() )*
     */
    public String visit(FormalParameterTail n, String argu) throws Exception {
        String ret = "";
        for ( Node node: n.f0.nodes) {
            ret += "," + node.accept(this, null);
        }

        return ret;
    }

    /**
     * f0 -> ","
     * f1 -> FormalParameter()
     */
    public String visit(FormalParameterTerm n, String argu) throws Exception {

        n.f0.accept(this, argu);
        String fp = n.f1.accept(this, argu);
        return fp;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     */
    public String visit(VarDeclaration n, String argu) throws Exception {
        String type = n.f0.accept(this, argu);
        String name = n.f1.accept(this, argu);
        if(argu!=null) emit("\t%"+name+" = alloca "+ jtype_to_llvm(type));
        return null;
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Expression()
     * f3 -> ";"
     */
    public String visit(AssignmentStatement n, String argu) throws Exception {
        String var = n.f0.accept(this, argu);
        String type = symbol_table.Search_and_Return(var, current_class, current_method,'v');
        String lltype = jtype_to_llvm(type);
        if(!current_class.equals(symbol_table.top_class)) {
//            if(symbol_table.offsets.get(current_class)!=null) {
//                LinkedList<Offset_Element_Value> list = symbol_table.offsets.get(current_class).Variables;
//                for (int i = 0; i < list.size(); i++) {
//                    if (list.get(i).name.equals(var)) {
//                        emit("\t%_" + reg_counter + " = getelementptr i8, i8* %this, " + lltype + " " + (list.get(i).position + 8));
//                        reg_counter++;
//                        emit("\t%_" + reg_counter + " = bitcast i8* %_" + (reg_counter - 1) + "to " + lltype + "*");
//                        reg_counter++;
//                        break;
//                    }
//                }
//            }
            if(symbol_table.Search_Class_Member(var, current_class, current_method)){
                emit("\t%_" + reg_counter + " = getelementptr i8, i8* %this, i32 " + (symbol_table.Get_var_offset(current_class, var) + 8));
                reg_counter++;
                String bitcast_reg = "%_"+reg_counter;
                emit("\t" + bitcast_reg + " = bitcast i8* %_" + (reg_counter - 1) + " to " + lltype + "*");
                reg_counter++;
                String exp = n.f2.accept(this, argu);
                emit("\tstore "+lltype+" "+exp+", "+lltype+"* "+bitcast_reg+"\n");
                return null;
            }
        }
        String exp = n.f2.accept(this, argu);
        emit("\tstore "+lltype+" "+exp+", "+lltype+"* %"+var+"\n");

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
    public String visit(ArrayAssignmentStatement n, String argu) throws Exception {
        String name = n.f0.accept(this, argu);
        String type = symbol_table.Search_and_Return(name, current_class, current_method,'v');
        String lltype = jtype_to_llvm(type);
        int addr;
        if(!current_class.equals(symbol_table.top_class) && symbol_table.Search_Class_Member(name, current_class, current_method)) {
            emit("\t%_" + reg_counter + " = getelementptr i8, i8* %this, i32 " + (symbol_table.Get_var_offset(current_class, name) + 8));
            reg_counter++;
            emit("\t%_" + reg_counter + " = bitcast i8* %_" + (reg_counter - 1) + " to " + lltype + "*");
            reg_counter++;
            emit("\t%_" + reg_counter + " = load " + lltype + ", " + lltype + "* %_" + (reg_counter - 1) + "\n");
            reg_counter++;
            regs_to_type.put(("%_" + (reg_counter - 1)), type);
            addr = (reg_counter-1);
        }
        else{
            addr = reg_counter++;
            emit("\t%_"+addr+" = load i32*, i32** %"+name);
        }
        String array_size = "%_"+reg_counter++;
        emit("\t"+array_size +" = load i32, i32* %_"+addr);
        String index = n.f2.accept(this,argu);
        emit("\t%_"+reg_counter++ +" = icmp sge i32 "+index+", 0");
        emit("\t%_"+reg_counter+" = icmp slt i32 "+index+", "+array_size);
        reg_counter++;
        emit("\t%_"+reg_counter+" = and i1 %_"+(reg_counter-2)+", %_"+(reg_counter-1));
        reg_counter++;
        int error_lbl = label_counter++;
        int ok_lbl = label_counter++;
        emit("\tbr i1 %_"+(reg_counter-1)+", label %label"+ok_lbl+", label %label"+error_lbl+"\n");
        emit("\tlabel"+error_lbl+":");
        emit("\tcall void @throw_oob()");
        emit("\tbr label %label"+ok_lbl+"\n");
        emit("\tlabel"+ok_lbl+":");
        emit("\t%_"+reg_counter++ +" = add i32 1, "+index);
        String getelem = "%_"+reg_counter;
        emit("\t"+getelem+" = getelementptr i32, i32* %_"+addr+", i32 %_"+(reg_counter-1));
        reg_counter++;
        emit("\tstore i32 "+n.f5.accept(this,argu)+", i32*"+getelem+"\n");
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
        String print_reg = n.f2.accept(this, argu);
        emit("\tcall void (i32) @print_int(i32 "+print_reg+")\n");
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
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
    public String visit(IfStatement n, String argu) throws Exception {
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        String expr_reg = n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        String then_lbl = "label"+label_counter++;
        String else_lbl = "label"+label_counter++;
        String endl_lbl = "label"+label_counter++;
        emit("\tbr i1 "+expr_reg+", label %"+then_lbl+", label %"+else_lbl);
        emit("\t"+else_lbl+":");
        n.f6.accept(this, argu);
        emit("\tbr label %"+endl_lbl+"\n");
        emit("\t"+then_lbl+":");
        n.f4.accept(this,argu);
        emit("\tbr label %"+endl_lbl);
        emit("\t"+endl_lbl+":");
        return null;
    }

    /**
     * f0 -> "while"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     */
    public String visit(WhileStatement n, String argu) throws Exception {
        String first = "label"+label_counter++;
        String second = "label"+label_counter++;
        String third = "label"+label_counter++;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        emit("\tbr label %"+first);
        emit("\t"+first+":");
        String expr_res = n.f2.accept(this, argu);
        emit("\tbr i1 "+expr_res+", label %"+second+", label %"+third);
        n.f3.accept(this, argu);
        emit("\t"+second+":");
        n.f4.accept(this, argu);
        emit("\tbr label %"+first);
        emit("\t"+third+":");
        return null;
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

    public String visit(BooleanType n, String argu) {
        return "boolean";
    }

    public String visit(IntegerType n, String argu) {
        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "+"
     * f2 -> PrimaryExpression()
     */
    public String visit(PlusExpression n, String argu) throws Exception {
        String term1 = n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        String term2 = n.f2.accept(this, argu);
        String result = "%_"+reg_counter;
        reg_counter++;
        emit("\t"+result+" = add i32 "+term1+", "+term2);
        return result;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "-"
     * f2 -> PrimaryExpression()
     */
    public String visit(MinusExpression n, String argu) throws Exception {
        String term1 = n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        String term2 = n.f2.accept(this, argu);
        String result = "%_"+reg_counter;
        reg_counter++;
        emit("\t"+result+" = sub i32 "+term1+", "+term2);
        return result;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "*"
     * f2 -> PrimaryExpression()
     */
    public String visit(TimesExpression n, String argu) throws Exception {
        String term1 = n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        String term2 = n.f2.accept(this, argu);
        String result = "%_"+reg_counter;
        reg_counter++;
        emit("\t"+result+" = mul i32 "+term1+", "+term2);
        return result;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "<"
     * f2 -> PrimaryExpression()
     */
    public String visit(CompareExpression n, String argu) throws Exception {
        String term1 = n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        String term2 = n.f2.accept(this, argu);
        String result = "%_"+reg_counter;
        reg_counter++;
        emit("\t"+result+" = icmp slt i32 "+term1+", "+term2);
        return result;
    }

    /**
     * f0 -> Clause()
     * f1 -> "&&"
     * f2 -> Clause()
     */
    public String visit(AndExpression n, String argu) throws Exception {

        //emit("\t%_"+reg_counter+" = load i1, i1* "+cl_reg1);
        //reg_counter++;
        int Alabels = label_counter;
        label_counter+=4;
        String cl_reg1 = n.f0.accept(this, argu);
        emit("\tbr i1 "+cl_reg1+", label %label"+(Alabels+1)+", label %label"+Alabels);
        emit("\tlabel"+Alabels+":");
        Alabels++;
        emit("\tbr label %label"+(Alabels+2)+"\n");
        emit("\tlabel"+Alabels+":");
        Alabels++;
        //emit("\t%_"+reg_counter+" = load i1, i1* "+ cl_reg2);
        //reg_counter++;
        String cl_reg2 = n.f2.accept(this, argu);
        emit("\tbr label %label"+Alabels+"\n");
        emit("\tlabel"+Alabels+":");
        Alabels++;
        emit("\tbr label %label"+Alabels+"\n");
        emit("\tlabel"+Alabels+":");
        Alabels++;
        emit("\t%_"+reg_counter+" = phi i1 [ 0, %label"+(Alabels-4)+" ], [ "+cl_reg2+", %label"+(Alabels-2)+" ]\n");
        reg_counter++;

        return "%_"+(reg_counter-1);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "["
     * f2 -> PrimaryExpression()
     * f3 -> "]"
     */
    public String visit(ArrayLookup n, String argu) throws Exception {
        String name = n.f0.accept(this, argu);
        //int addr = reg_counter++;
        //emit("\t%_"+addr+" = load i32*, i32** "+name);
        String array_size = "%_"+reg_counter++;
        emit("\t"+array_size +" = load i32, i32* "+name);
        String index = n.f2.accept(this,argu);
        emit("\t%_"+reg_counter++ +" = icmp sge i32 "+index+", 0");
        emit("\t%_"+reg_counter+" = icmp slt i32 "+index+", "+array_size);
        reg_counter++;
        emit("\t%_"+reg_counter+" = and i1 %_"+(reg_counter-2)+", %_"+(reg_counter-1));
        reg_counter++;
        int error_lbl = label_counter++;
        int ok_lbl = label_counter++;
        emit("\tbr i1 %_"+(reg_counter-1)+", label %label"+ok_lbl+", label %label"+error_lbl+"\n");
        emit("\tlabel"+error_lbl+":");
        emit("\tcall void @throw_oob()");
        emit("\tbr label %label"+ok_lbl+"\n");
        emit("\tlabel"+ok_lbl+":");
        emit("\t%_"+reg_counter++ +" = add i32 1, "+index);
        emit("\t%_"+reg_counter+" = getelementptr i32, i32* "+name+", i32 %_"+(reg_counter-1));
        reg_counter++;
        emit("\t%_"+reg_counter++ +" = load i32, i32* %_"+(reg_counter-2)+"\n");
        return "%_"+(reg_counter-1);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> "length"
     */
    public String visit(ArrayLength n, String argu) throws Exception {
        String addr = n.f0.accept(this, argu);
        emit("\t%_"+reg_counter++ +" = load i32, i32* "+addr);
        return "%_"+(reg_counter-1);
    }

    /**
     * f0 -> "new"
     * f1 -> "int"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */
    public String visit(IntegerArrayAllocationExpression n, String argu) throws Exception {
        String size = n.f3.accept(this, null);
        int size_reg = reg_counter++;
        emit("\t%_"+size_reg+" = add i32 1, "+size+"\n");
        emit("\t%_"+reg_counter++ +" = icmp sge i32 %_"+size_reg+", 1\n");
        int error_lbl = label_counter++;
        int ok_lbl = label_counter++;
        emit("\tbr i1 %_"+(reg_counter-1)+", label %label"+ok_lbl +", label %label"+error_lbl+"\n");
        emit("\tlabel"+error_lbl+":");
        emit("\tcall void @throw_nsz()");
        emit("\tbr label %label"+ok_lbl+"\n");
        emit("\tlabel"+ok_lbl+":");
        int int_all = reg_counter++;
        int cast_res = reg_counter++;
        emit("\t%_"+int_all+" = call i8* @calloc(i32 %_"+size_reg+", i32 4)");
        emit("\t%_"+cast_res+" = bitcast i8* %_"+int_all+" to i32*\n");
        emit("\tstore i32 "+size+", i32* %_"+cast_res);
        return "%_"+cast_res;
    }

    /**
     * f0 -> "new"
     * f1 -> Identifier()
     * f2 -> "("
     * f3 -> ")"
     */
    public String visit(AllocationExpression n, String argu) throws Exception {
        String type = n.f1.accept(this, argu);
        int size = symbol_table.Get_Var_Size(type);
        String calloc_reg = "%_"+reg_counter++;
        emit("\t"+calloc_reg +" = call i8* @calloc(i32 1, i32 "+(size+8)+")");
        emit("\t%_"+reg_counter+" = bitcast i8* %_"+(reg_counter-1)+" to i8***");
        reg_counter++;

        int number_of_methods = symbol_table.v_tables.get(type).size();

        emit("\t%_"+reg_counter++ +" = getelementptr ["+number_of_methods+" x i8*], ["+number_of_methods+" x i8*]* @."+type+"_vtable, i32 0, i32 0");
        emit("\tstore i8** %_"+(reg_counter-1)+", i8*** %_"+(reg_counter-2)+"\n");

        return calloc_reg;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( ExpressionList() )?
     * f5 -> ")"
     */
    public String visit(MessageSend n, String argu) throws Exception {
        String term1 = n.f0.accept(this, argu);
        //String load_reg = ""+reg_counter++;
        //emit("\t%_"+load_reg +" = load i8*, i8** "+term1);
        emit("\t%_"+reg_counter+" = bitcast i8*"+term1+" to i8***");
        reg_counter++;
        emit("\t%_"+reg_counter+" = load i8**, i8*** %_"+(reg_counter-1));
        reg_counter++;


        String method_name = n.f2.accept(this, argu);
        sv.symbol_table=symbol_table;
        sv.current_method=current_method;
        sv.current_class=current_class;
        String id_class = n.f0.accept(sv, null);
//        if(!term1.equals("this")) id_class = symbol_table.Search_and_Return(term1, current_class, current_method, 'v');
//        else id_class = current_class;
        String info[] = symbol_table.Search_and_Return(null, id_class, method_name, 'f').split(" ");
        String method_class = info[0];
        String method_ret_type = info[1];

        LinkedHashMap<String, String> map = symbol_table.v_tables.get(method_class);
        Set<String> keys =map.keySet();
        int funct_offset=0;
        for(String key : keys){
            if(key.equals(method_name) && map.get(key).equals(method_class)) break;
            funct_offset++;
        }
        emit("\t%_"+reg_counter+" = getelementptr i8*, i8** %_"+(reg_counter-1)+", i32 "+" "+funct_offset);
        reg_counter++;
        emit("\t%_"+reg_counter+" = load i8*, i8** %_"+(reg_counter-1));
        reg_counter++;

        ArrayList<String> args = symbol_table.function_table.get(new SymbolTable_Key(method_name, method_class)).GetArgTypeList();
        String args_str="";
        for(int i=0; i<args.size(); i++) args_str+=","+jtype_to_llvm(args.get(i));

        String bitcast_reg = "%_"+reg_counter;
        emit("\t%_"+reg_counter+" = bitcast i8* %_"+(reg_counter-1)+" to "+jtype_to_llvm(method_ret_type)+" (i8*"+args_str+")*");
        reg_counter++;

        String expr_list_str = "";
        if(n.f4.accept(this, argu)!=null) {
            String expr_list[] = n.f4.accept(this, argu).split(",");
            for (int i = 0; i < args.size(); i++) {
                expr_list_str += ", " + jtype_to_llvm(args.get(i)) + " " + expr_list[i];
            }
        }
        emit("\t%_"+reg_counter+" = call "+jtype_to_llvm(method_ret_type)+" "+bitcast_reg+"(i8* "+term1+expr_list_str+")");
        reg_counter++;
        return "%_"+(reg_counter-1);
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
     * f0 -> IntegerLiteral()
     *       | TrueLiteral()
     *       | FalseLiteral()
     *       | Identifier()
     *       | ThisExpression()
     *       | ArrayAllocationExpression()
     *       | AllocationExpression()
     *       | BracketExpression()
     */
    public String visit(PrimaryExpression n, String argu) throws Exception {
        String f=n.f0.accept(this, argu);
        if(!isInt(f) && !f.equals("true") && !f.equals("false") && !f.startsWith("%")){
            String type = symbol_table.Search_and_Return(f, current_class, current_method,'v');
            String lltype = jtype_to_llvm(type);
            if(!current_class.equals(symbol_table.top_class)) {
//                if(symbol_table.offsets.get(current_class)!=null) {
//                    LinkedList<Offset_Element_Value> list = symbol_table.offsets.get(current_class).Variables;
//                    for (int i = 0; i < list.size(); i++) {
//                        if (list.get(i).name.equals(f)) {
//                            emit("\t%_" + reg_counter + " = getelementptr i8, i8* %this, " + lltype + " " + (list.get(i).position + 8));
//                            reg_counter++;
//                            emit("\t%_" + reg_counter + " = bitcast i8* %_" + (reg_counter - 1) + "to " + lltype + "*");
//                            reg_counter++;
//                            break;
//                        }
//                    }
//                }
                if(symbol_table.Search_Class_Member(f, current_class, current_method)){
                    emit("\t%_" + reg_counter + " = getelementptr i8, i8* %this, i32 " + (symbol_table.Get_var_offset(current_class, f) + 8));
                    reg_counter++;
                    emit("\t%_" + reg_counter + " = bitcast i8* %_" + (reg_counter - 1) + " to " + lltype + "*");
                    reg_counter++;
                    emit("\t%_"+reg_counter+" = load "+lltype+", "+lltype+"* %_"+(reg_counter-1)+"\n");
                    reg_counter++;
                    regs_to_type.put(("%_"+(reg_counter-1)), type);
                    return ("%_"+(reg_counter-1));
                }

            }
            emit("\t%_"+reg_counter+" = load "+lltype+", "+lltype+"* %"+f+"\n");
            reg_counter++;
            regs_to_type.put(("%_"+(reg_counter-1)), type);
            return ("%_"+(reg_counter-1));
        }

        return f;
    }

    /**
     * f0 -> "("
     * f1 -> Expression()
     * f2 -> ")"
     */
    public String visit(BracketExpression n, String argu) throws Exception {
        return n.f1.accept(this, argu);
    }

    /**
     * f0 -> "!"
     * f1 -> Clause()
     */
    public String visit(NotExpression n, String argu) throws Exception {
        n.f0.accept(this, argu);
        String cl_reg = n.f1.accept(this, argu);
        emit("\t%_"+reg_counter+" = xor i1 "+cl_reg+" , 1");
        reg_counter++;
        return "%_"+(reg_counter-1);
    }

    /**
     * f0 -> <INTEGER_LITERAL>
     */
    public String visit(IntegerLiteral n, String argu) throws Exception {
        return n.f0.toString();
    }

    /**
     * f0 -> "true"
     */
    public String visit(TrueLiteral n, String argu) throws Exception {
        n.f0.accept(this, argu);
        return "true";
    }

    /**
     * f0 -> "false"
     */
    public String visit(FalseLiteral n, String argu) throws Exception {
        n.f0.accept(this, argu);
        return "false";
    }

    /**
     * f0 -> "this"
     */
    public String visit(ThisExpression n, String argu) throws Exception {
        return "%this";
    }





}
