
public class ImageType extends Type {
    
    public ImageType() {
        super("image");
    }

    @Override
    public String convertionString() {
        return "np.array";
    }

}