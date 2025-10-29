package projects.ankit;
import java.io.*;

public class ObjectStore {
    private static final String BASE_PATH = "data";

    public void save(String bucket, String key, InputStream in, int contentLength) throws IOException {
        System.out.println("ObjectStore save method, bucket : "+bucket+" key: "+key +" contentLength: "+contentLength);

        File file = new File("data/" + bucket + "/" + key);
        file.getParentFile().mkdirs();

        try (FileOutputStream fos = new FileOutputStream(file)) {
            byte[] buffer = new byte[8192];
            int total = 0;
            while (total < contentLength) {
                System.out.println("ObjectStore write total: " +total);
                int read = in.read(buffer, 0, Math.min(buffer.length, contentLength - total));
                if (read == -1) break;
                fos.write(buffer, 0, read);
                total += read;
            }
        }
    }


    public File load(String bucket, String key) {
        System.out.println("ObjectStore load method, bucket: "+bucket+" key: "+key);
        return new File(BASE_PATH + "/" + bucket + "/" + key);
    }

    public void delete(String bucket, String key) {
        System.out.println("ObjectStore delete method, bucket: "+bucket+" key: "+key);
        File file = load(bucket, key);
        if (file.exists()) file.delete();
    }
}
