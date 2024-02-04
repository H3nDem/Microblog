import java.io.Serializable;

public class Tag implements Serializable {
    private String tagName;

    public Tag(String name) {
        this.tagName = "#" + name;
    }

    public String getTagName() {
        return tagName;
    }

    @Override
    public String toString() {
        return tagName;
    }
}
