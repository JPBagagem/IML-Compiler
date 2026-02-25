public class PercentageType extends Type {
    
    public PercentageType() {
        super("percentage");
    }

    @Override
    public String convertionString() {
        return "float";
    }

}