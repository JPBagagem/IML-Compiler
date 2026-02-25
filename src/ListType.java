public class ListType extends Type {

    private Type elementType;
    private int length;

    public ListType(Type elementType) {
        super((elementType!=null) ? "list of " + elementType.name() : "empty list");
        this.elementType = elementType;
    }

    public Type getElementType() {
        return elementType;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getLength() {
        return length;
    }

    public Type getBasicType() {
        if (elementType instanceof ListType){
            ListType elemType = (ListType) elementType;
            return elemType.getBasicType();
        }
        return elementType;
    }

    public int countDimensions() {
        int dimensions = 1;  // Já sabemos que é pelo menos 1 (porque é uma lista)
        Type inner = elementType;
        while (inner instanceof ListType) {
            dimensions++;
            inner = ((ListType) inner).getElementType(); // Vai para o próximo nível
        }
        return dimensions;
    }

    @Override
    public String convertionString() {
        return "np.array";
    }
}
