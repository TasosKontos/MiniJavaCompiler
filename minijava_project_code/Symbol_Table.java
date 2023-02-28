import java.util.*;
import java.util.LinkedList;

public class Symbol_Table{
    LinkedHashMap<SymbolTable_Key, String> variable_table;
    LinkedHashMap<SymbolTable_Key, FunctTable_Value> function_table;
    LinkedHashMap<String, ArrayList<String>> classes;

    LinkedHashMap<String, Offset_Element> offsets;

    LinkedHashMap<String, LinkedHashMap<String, String>> v_tables;
    LinkedHashMap<String, LinkedHashMap<String, Integer>> var_map;


    String top_class;

    public Symbol_Table(){
        variable_table = new LinkedHashMap<SymbolTable_Key, String>();
        function_table = new LinkedHashMap<SymbolTable_Key, FunctTable_Value>();
        classes = new LinkedHashMap<String, ArrayList<String>>();
        offsets = new LinkedHashMap<String, Offset_Element>();
        v_tables = new LinkedHashMap<String, LinkedHashMap<String, String>>();
        var_map = new LinkedHashMap<String, LinkedHashMap<String, Integer>>();
    }

    public void Fill_Var_Map(){
        Set<String> class_keys=classes.keySet();
        for(String ckey : class_keys){
            if(ckey.equals(top_class))continue;
            var_map.put(ckey, new LinkedHashMap<String, Integer>());
            ArrayList<String> ext_class = classes.get(ckey);
            for(int i=0; i<ext_class.size(); i++){
                String parent = ext_class.get(i);
                if(offsets.get(parent)==null)continue;
                LinkedList<Offset_Element_Value> variables = offsets.get(parent).Variables;
                if(variables!=null){
                    for(int j=0; j<variables.size(); j++){
                        var_map.get(ckey).put(variables.get(j).name, variables.get(j).position);
                    }
                }
            }
            if(offsets.get(ckey)==null)continue;
            if(offsets.get(ckey).Variables!=null){
                for(int j=0; j<offsets.get(ckey).Variables.size(); j++){
                    var_map.get(ckey).put(offsets.get(ckey).Variables.get(j).name, offsets.get(ckey).Variables.get(j).position);
                }
            }
        }
    }

    public int Get_var_offset(String class_name, String var_name){
        return var_map.get(class_name).get(var_name);
    }

    public void Print_varmap_table(){
        Set<String> keys1 = var_map.keySet();
        for(String key1 : keys1){
            System.out.println("Var_map for class " + key1+":");
            Set<String> keys2 = var_map.get(key1).keySet();
            for(String key2 : keys2){
                System.out.println("name: "+key2+" position:"+var_map.get(key1).get(key2));
            }
            System.out.println("");
        }
    }

    public void Insert_V_table_Class(String class_name){
        if(!classes.get(class_name).isEmpty()){
            LinkedHashMap<String ,String> temp = new LinkedHashMap<String, String>(v_tables.get(classes.get(class_name).get(classes.get(class_name).size()-1)));
            v_tables.put(class_name, temp);
        }
        else {
            LinkedHashMap<String, String> key = new LinkedHashMap<>();
            v_tables.put(class_name, key);
        }
    }

    public void Insert_V_table_Method(String class_name, String method_name){
        if(!v_tables.containsKey(class_name)){
            if(!classes.get(class_name).isEmpty()){
                LinkedHashMap<String ,String> temp = new LinkedHashMap<String, String>(v_tables.get(classes.get(class_name).get(classes.get(class_name).size()-1)));
                v_tables.put(class_name, temp);
            }
            else {
                LinkedHashMap<String, String> key = new LinkedHashMap<>();
                v_tables.put(class_name, key);
            }
        }
        v_tables.get(class_name).put(method_name, class_name);
    }

    public int Get_Var_Size(String name){
        if(offsets.get(name)==null){
            if(classes.get(name).isEmpty())return 0;
            String parent = classes.get(name).get(classes.get(name).size()-1);
            return Get_Var_Size(parent);
        }
        else
            return offsets.get(name).var_size;
    }

    public void Print_V_table(){
        Set<String> keys1 = v_tables.keySet();
        for(String key1 : keys1){
            System.out.println("V_table for class " + key1+":");
             Set<String> keys2 = v_tables.get(key1).keySet();
             for(String key2 : keys2){
                 System.out.println(v_tables.get(key1).get(key2)+"."+key2);
             }
             System.out.println("");
        }
    }
    
    public void Insert_Offset_Var(String varname, String varclass, String vartype){
        int o;
        switch(vartype){
            case "int":
                o=4;
                break;
            case "boolean":
                o=1;
                break;
            default:
                o=8;
        }

        if(!offsets.containsKey(varclass)) {
            Offset_Element temp = new Offset_Element();
            String parent;
            int start=0;
            if(!classes.get(varclass).isEmpty()){
                parent = classes.get(varclass).get(classes.get(varclass).size()-1);
                start = offsets.get(parent).var_size;
            }
            temp.Variables.add(new Offset_Element_Value(varname, start));
            temp.var_size = temp.var_size+o+start;
            offsets.put(varclass, temp);
        }
        else
        {
            offsets.get(varclass).Variables.add(new Offset_Element_Value(varname, offsets.get(varclass).var_size));
            offsets.get(varclass).var_size+=o;
        }
    }

    public void Insert_Offset_Method(String mname, String mclass){
        if(!offsets.containsKey(mclass)) {
            Offset_Element temp = new Offset_Element();
            String parent;
            int start=0;
            if(!classes.get(mclass).isEmpty()){
                parent = classes.get(mclass).get(classes.get(mclass).size()-1);
                if(Search_and_Return(null, parent, mname, 'f')!=null) return;
                start =  offsets.get(parent).method_size;
            }
            temp.Methods.add(new Offset_Element_Value(mname, start));
            temp.method_size = temp.method_size + start + 8;
            offsets.put(mclass, temp);
        }
        else{
            offsets.get(mclass).Methods.add(new Offset_Element_Value(mname, offsets.get(mclass).method_size));
            offsets.get(mclass).method_size+=8;
        }
    }

    public void Print_Offsets(){
        Set<String> keys = offsets.keySet();
        for(String key : keys){
            System.out.println("-----------Class "+key+"-----------");
            System.out.println("--Variables--");
            for(int i=0; i<offsets.get(key).Variables.size(); i++){
                System.out.println(key+"."+offsets.get(key).Variables.get(i).name+" : "+offsets.get(key).Variables.get(i).position);
            }
            System.out.println("--Methods--");
            for(int i=0; i<offsets.get(key).Methods.size(); i++){
                System.out.println(key+"."+offsets.get(key).Methods.get(i).name+" : "+offsets.get(key).Methods.get(i).position);
            }

        }
    }

    public void Insert_Var(SymbolTable_Key key, String type){
        this.variable_table.put(key, type);
    }

    public void Insert_Funct(SymbolTable_Key key, FunctTable_Value value){
        this.function_table.put(key, value);
    }

    public void Insert_Class(String class_name, String class_parent){
        if(class_parent==null) {
            this.classes.put(class_name, new ArrayList<String>());
            return;
        }

        ArrayList<String> parent_extention_list = this.classes.get(class_parent);
        if(parent_extention_list.isEmpty()) {
            this.classes.put(class_name, new ArrayList<String>());
            this.classes.get(class_name).add(class_parent);
        }
        else{
            this.classes.put(class_name, new ArrayList<String>());
            this.classes.get(class_name).addAll(parent_extention_list);
            this.classes.get(class_name).add(class_parent);
        }
    }

    public boolean Search_Class(String class_name){
        return this.classes.containsKey(class_name);
    }

    public boolean Search(SymbolTable_Key key, char type) {
        if(type=='v')
            return this.variable_table.containsKey(key);
        else if(type=='f')
            return this.function_table.containsKey(key);
        else
            return (this.variable_table.containsKey(key) || this.function_table.containsKey(key));
    }

    public boolean Search_Class_Member(String varname ,String classname, String method){
        SymbolTable_Key functt_key = new SymbolTable_Key(method, classname);
        SymbolTable_Key vart_key = new SymbolTable_Key(varname, classname);

        //check if var exists in method vardecl
        FunctTable_Value value = this.function_table.get(functt_key);
        if(value.local_vars.containsKey(varname)) return false;

        //check if var exists in method arglist
        if(value.args.containsKey(varname)) return false;

        //check if var exists in class member list
        if(this.variable_table.containsKey(vart_key)) return true;

        //check if var exists in any extended class member list

        ArrayList<String> ext_class = this.classes.get(classname);
        if(!ext_class.isEmpty()) {
            ListIterator<String> iter = ext_class.listIterator(ext_class.size());
            while(iter.hasPrevious()){
                SymbolTable_Key key = new SymbolTable_Key(varname, iter.previous());
                if(this.variable_table.containsKey(key)) return true;
            }
        }

        return false;
    }

    public String Search_and_Return(String varname, String classname, String method, char mode){

        SymbolTable_Key functt_key = new SymbolTable_Key(method, classname);
        if(mode=='f'){
            if(this.function_table.containsKey(functt_key))
                return (classname+" "+this.function_table.get(functt_key).return_type);


            ArrayList<String> ext_class = this.classes.get(classname);
            if(!ext_class.isEmpty()) {
                ListIterator<String> iter = ext_class.listIterator(ext_class.size());
                while(iter.hasPrevious()){
                    String parent = iter.previous();
                    SymbolTable_Key key = new SymbolTable_Key(method, parent);
                    if(this.function_table.containsKey(key)) return (parent+" "+this.function_table.get(key).return_type);
                }
            }
        }
        else if(mode == 'v'){
            SymbolTable_Key vart_key = new SymbolTable_Key(varname, classname);

            //check if var exists in method vardecl
            FunctTable_Value value = this.function_table.get(functt_key);
            if(value.local_vars.containsKey(varname)) return value.local_vars.get(varname);

            //check if var exists in method arglist
            if(value.args.containsKey(varname)) return value.args.get(varname);

            //check if var exists in class member list
            if(this.variable_table.containsKey(vart_key)) return this.variable_table.get(vart_key);

            //check if var exists in any extended class member list

            ArrayList<String> ext_class = this.classes.get(classname);
            if(!ext_class.isEmpty()) {
                ListIterator<String> iter = ext_class.listIterator(ext_class.size());
                while(iter.hasPrevious()){
                    SymbolTable_Key key = new SymbolTable_Key(varname, iter.previous());
                    if(this.variable_table.containsKey(key)) return this.variable_table.get(key);
                }
            }


        }
        return null;
    }

    public ArrayList<String> GetFunctionArgs(String method_name, String classname){
        return this.function_table.get(new SymbolTable_Key(method_name, classname)).GetArgTypeList();
    }

    public void Print(char type){
        if(type=='v'){
            System.out.println("\n---- VARIABLE TABLE ----  \n");
            variable_table.entrySet().forEach(entry -> {
                entry.getKey().Print();
                System.out.print(" --> "+entry.getValue()+"\n");

            });
        }
        else if(type=='f'){
            System.out.println("\n---- FUNCTION TABLE ---- \n");
            function_table.entrySet().forEach(entry -> {
                entry.getKey().Print();
                System.out.print(" --> ");
                entry.getValue().Print();
            });
        }
        else{
            System.out.println("\n---- VARIABLE TABLE ----  \n");
            variable_table.entrySet().forEach(entry -> {
                entry.getKey().Print();
                System.out.print(" --> "+entry.getValue()+"\n");

            });

            System.out.println("\n---- FUNCTION TABLE ---- \n");
            function_table.entrySet().forEach(entry -> {
                entry.getKey().Print();
                System.out.println();
                entry.getValue().Print();
            });

        }
    }
}

class SymbolTable_Key{
    public String var_name;
    public String class_name;

    public SymbolTable_Key(String var_name,String class_name){
        this.var_name=var_name;
        this.class_name=class_name;
    }

    public int hashCode(){
        return (var_name!=null ? var_name.hashCode() : 0) + 31 * (class_name!=null ? class_name.hashCode(): 0 );
    }

    public boolean equals(Object o){
        if (o == null || o.getClass() != this.getClass()) { return false; }
        SymbolTable_Key elem = (SymbolTable_Key) o;
        return (var_name == null ? elem.var_name == null : var_name.equals(elem.var_name))
                && (class_name == null ? elem.class_name == null : class_name.equals(elem.class_name));
    }

    public void Print(){
        System.out.print("["+this.var_name+" , "+this.class_name+"]");
    }
}

class FunctTable_Value{
    public LinkedHashMap<String, String> args;
    public LinkedHashMap<String, String> local_vars;
    public String return_type;

    public FunctTable_Value(LinkedHashMap<String, String> args, LinkedHashMap<String, String> local_vars, String return_type){
        this.args=args;
        this.local_vars=local_vars;
        this.return_type=return_type;
    }

    public void Print(){
        System.out.print("args: { ");
        args.entrySet().forEach(entry -> {
            System.out.print("["+ entry.getKey() + ", " + entry.getValue()+"] ");
        });
        System.out.println(" } ");

        System.out.print("variables: { ");
        local_vars.entrySet().forEach(entry -> {
            System.out.print("["+ entry.getKey() + ", " + entry.getValue()+"] ");
        });
        System.out.println(" }");

        System.out.print("return type:"+ return_type);
        System.out.print("\n\n");

    }

    public ArrayList<String> GetArgTypeList(){
        Set<String> keys = args.keySet();
        ArrayList<String> list = new ArrayList<String>();
        for(String key:keys){
            list.add(args.get(key));
        }
        return list;
    }
}

class Offset_Element{
     LinkedList<Offset_Element_Value> Variables;
     LinkedList<Offset_Element_Value> Methods;

     int var_size;
     int method_size;

     Offset_Element(){
         this.Variables = new LinkedList<Offset_Element_Value>();
         this.Methods = new LinkedList<Offset_Element_Value>();
         this.var_size = 0;
         this.method_size = 0;
     }
}

class Offset_Element_Value{
    public String name;
    public int position;

    Offset_Element_Value(String name, int position){
        this.name = name;
        this.position = position;
    }

}