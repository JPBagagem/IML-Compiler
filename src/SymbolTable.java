import java.util.HashMap;
import java.util.Map;

public class SymbolTable {
    private final Map<String, Type> symbols = new HashMap<>();

    private static Type[] types = {new BooleanType(), new StringType(), new NumberType(), new PercentageType(), new ImageType()};

    public boolean declare(String varName, Type type) {
        if (symbols.containsKey(varName)) {
            return false;
        }
        symbols.put(varName, type);
        return true;
    }

    public boolean declare(String varName, String typeName) {
        if (symbols.containsKey(varName)) {
            return false;
        }
        symbols.put(varName, getTypeByName(typeName));
        return true;
    }

    public Type getTypeByName(String typeName){

        
        if (typeName.length() > 8 && typeName.substring(0,8).equals("list of ")){
            return new ListType(getTypeByName(typeName.substring(0,8)));
        }

        for (Type t : types){
            if (t.name().equals(typeName)){
                return t;
            }
        }
        return null;
    }

    public Type resolve(String name){
        return symbols.get(name);
    }

    public boolean isDeclared(String name) {
        return symbols.containsKey(name);
    }
    
    @Override
    public String toString() {
        return symbols.toString();
    }
}
