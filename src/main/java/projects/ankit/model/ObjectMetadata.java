package projects.ankit.model;
import java.util.Date;

public class ObjectMetadata {
    public String key;
    public Date createdAt;

    public ObjectMetadata(String key, Date createdAt) {
        this.key = key;
        this.createdAt = createdAt;
    }
}
