package projects.ankit.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import projects.ankit.model.ObjectMetadata;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class JsonUtil {
    private static final Gson gson = new Gson();

    public static Map<String, Map<String, ObjectMetadata>> load(String file) {
        File f = new File(file);
        if (!f.exists()) return new HashMap<>();
        try (Reader reader = new FileReader(f)) {
            Type type = new TypeToken<Map<String, Map<String, Object>>>() {}.getType();
            return gson.fromJson(reader, type);
        } catch (IOException e) {
            return new HashMap<>();
        }
    }

    public static void save(String file, Object data) {
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(data, writer);
        } catch (IOException ignored) {}
    }

    public static String toJson(Object data) {
        return gson.toJson(data);
    }
}
