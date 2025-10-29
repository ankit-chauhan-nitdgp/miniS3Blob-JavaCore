package projects.ankit;

import projects.ankit.model.ObjectMetadata;
import projects.ankit.util.JsonUtil;

import java.util.*;

public class MetadataStore {
    private static final String META_FILE = "data/metadata.json";
    private Map<String, Map<String, ObjectMetadata>> data;

    public MetadataStore() {
        data = JsonUtil.load(META_FILE);
    }

    public synchronized void add(String bucket, String key) {
        data.computeIfAbsent(bucket, k -> new HashMap<>())
                .put(key, new ObjectMetadata(key, new Date()));
        JsonUtil.save(META_FILE, data);
    }

    public synchronized void remove(String bucket, String key) {
        if (data.containsKey(bucket)) {
            data.get(bucket).remove(key);
            JsonUtil.save(META_FILE, data);
        }
    }

    public synchronized String list(String bucket) {
        if (!data.containsKey(bucket)) return "[]";
        return JsonUtil.toJson(data.get(bucket).keySet());
    }
}
