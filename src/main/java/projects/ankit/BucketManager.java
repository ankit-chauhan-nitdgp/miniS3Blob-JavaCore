package projects.ankit;

import java.io.File;

public class BucketManager {
    private static final String BASE_PATH = "data";

    public boolean ensureBucket(String bucket) {
        File dir = new File(BASE_PATH, bucket);
        if (!dir.exists())
            return dir.mkdirs();
        else
            return true;
    }
}