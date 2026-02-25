public abstract class Type {
    protected final String name;

    protected Type(String name) {
        assert name != null;
        this.name = name;
    }

    public String name() {
        return name;
    }

    public String convertionString() {
        return "";
    }

    public boolean conformsTo(Type other) {
        return name().equals(other.name());
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {

        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Type other = (Type) obj;
        return (name.equals(other.name));
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
