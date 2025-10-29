package projects.ankit.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Map;

public class SignatureUtil {
    private static final String SECRET_KEY = "miniS3Secret";

    public static String generate(String method, String path, long expires) throws Exception {
        String data = method + ":" + path + ":" + expires;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET_KEY.getBytes(), "HmacSHA256"));
        byte[] hash = mac.doFinal(data.getBytes());
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }

    public static boolean isValid(String method, String path, Map<String, String> query) {
        try {
            long expires = Long.parseLong(query.get("expires"));
            if (System.currentTimeMillis() / 1000 > expires) return false;
            String expected = generate(method, path, expires);
            return expected.equals(query.get("signature"));
        } catch (Exception e) {
            return false;
        }
    }
}
