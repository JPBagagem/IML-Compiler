public class NumberType extends Type {
    
    public NumberType() {
        super("number");
    }

    @Override
    public String convertionString() {
        return "float";
    }

}