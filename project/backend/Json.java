import java.util.*;

// ============================================================================
// Json.java
// A tiny helper for JSON. No external library is used (beginner friendly).
//   * str(...)         -> turns a Java String into a safe JSON string literal.
//   * parseObject(...) -> reads a FLAT JSON object like {"a":"text","b":12}
//                         and returns every value as a String.
// Our request bodies are always flat objects, so this simple parser is enough.
// ============================================================================
public class Json {

    // Escapes a Java String into a JSON string literal (with the quotes added).
    public static String str(String value) {
        if (value == null) return "null";
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:   sb.append(c);
            }
        }
        sb.append("\"");
        return sb.toString();
    }

    // Reads a flat JSON object and returns a Map of key -> value (as text).
    public static Map<String, String> parseObject(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null) return map;
        int i = 0, n = json.length();
        while (i < n && json.charAt(i) != '{') i++;   // move to the first '{'
        i++;                                           // step past '{'
        while (i < n) {
            while (i < n && (Character.isWhitespace(json.charAt(i)) || json.charAt(i) == ',')) i++;
            if (i >= n || json.charAt(i) == '}') break;
            if (json.charAt(i) != '"') break;          // keys are quoted strings

            // --- read the key ---
            StringBuilder key = new StringBuilder();
            i++;
            while (i < n && json.charAt(i) != '"') {
                if (json.charAt(i) == '\\' && i + 1 < n) { i++; key.append(unescape(json.charAt(i))); }
                else key.append(json.charAt(i));
                i++;
            }
            i++;
            while (i < n && (Character.isWhitespace(json.charAt(i)) || json.charAt(i) == ':')) i++;

            // --- read the value ---
            StringBuilder val = new StringBuilder();
            if (i < n && json.charAt(i) == '"') {               // string value
                i++;
                while (i < n && json.charAt(i) != '"') {
                    if (json.charAt(i) == '\\' && i + 1 < n) { i++; val.append(unescape(json.charAt(i))); }
                    else val.append(json.charAt(i));
                    i++;
                }
                i++;
            } else {                                            // number / boolean / null
                while (i < n && json.charAt(i) != ',' && json.charAt(i) != '}') {
                    val.append(json.charAt(i));
                    i++;
                }
            }
            map.put(key.toString(), val.toString().trim());
        }
        return map;
    }

    private static char unescape(char c) {
        switch (c) {
            case 'n': return '\n';
            case 'r': return '\r';
            case 't': return '\t';
            default:  return c;
        }
    }
}
