package util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SimpleJsonParser {
    public static Map<String, Object> parseFile(String filePath) throws IOException {
        StringBuilder json = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                json.append(line);
            }
        }
        return parseObject(json.toString());
    }

    public static Map<String, Object> parseObject(String json) {
        json = json.trim();
        if (!json.startsWith("{") || !json.endsWith("}")) {
            throw new IllegalArgumentException("Invalid JSON object");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        String content = json.substring(1, json.length() - 1);

        int i = 0;
        while (i < content.length()) {
            i = skipWhitespaceAndCommas(content, i);
            if (i >= content.length()) break;

            i = parseKeyValuePair(content, i, result);

            i = skipWhitespaceAndCommas(content, i);
        }

        return result;
    }

    public static List<Object> parseArray(String json) {
        json = json.trim();
        if (!json.startsWith("[") || !json.endsWith("]")) {
            throw new IllegalArgumentException("Invalid JSON array");
        }

        List<Object> result = new LinkedList<>();
        String content = json.substring(1, json.length() - 1);

        int i = 0;
        while (i < content.length()) {
            i = skipWhitespaceAndCommas(content, i);
            if (i >= content.length()) break;

            Object[] valueResult = parseValue(content, i);
            result.add(valueResult[0]);
            i = (int) valueResult[1];
        }

        return result;
    }

    private static int skipWhitespaceAndCommas(String json, int pos) {
        while (pos < json.length()) {
            char c = json.charAt(pos);
            if (Character.isWhitespace(c) || c == ',') {
                pos++;
            } else {
                break;
            }
        }
        return pos;
    }

    private static int parseKeyValuePair(String json, int pos, Map<String, Object> result) {
        if (json.charAt(pos) != '"') {
            throw new IllegalArgumentException("Expected '\"' at position " + pos);
        }
        int keyEnd = json.indexOf('"', pos + 1);
        String key = json.substring(pos + 1, keyEnd);
        pos = keyEnd + 1;

        pos = skipWhitespaceAndCommas(json, pos);
        if (pos >= json.length() || json.charAt(pos) != ':') {
            throw new IllegalArgumentException("Expected ':' at position " + pos);
        }
        pos++;

        Object[] valueResult = parseValue(json, pos);
        result.put(key, valueResult[0]);
        return (int) valueResult[1];
    }

    private static int findMatchingBracket(String json, int start, char open, char close) {
        int depth = 1;
        int i = start + 1;
        while (i < json.length()) {
            if (json.charAt(i) == open) {
                depth++;
            } else if (json.charAt(i) == close) {
                depth--;
            }
            if (depth == 0) return i;
            i++;
        }
        throw new IllegalArgumentException("Mismatched brackets");
    }

    private static Object[] parseValue(String json, int startPos) {
        int i = skipWhitespaceAndCommas(json, startPos);

        if (i >= json.length()) {
            throw new IllegalArgumentException("Unexpected end of JSON");
        }

        char c = json.charAt(i);

        return switch (c) {
            case '"' -> parseString(json, i);
            case '[' -> parseArrayValue(json, i);
            case '{' -> parseObjectValue(json, i);
            default -> parseNumberOrBoolean(json, i);
        };
    }

    private static Object[] parseString(String json, int start) {
        int end = json.indexOf('"', start + 1);
        if (end == -1) {
            throw new IllegalArgumentException("Unterminated string at position " + start);
        }
        return new Object[]{json.substring(start + 1, end), end + 1};
    }

    private static Object[] parseArrayValue(String json, int start) {
        int end = findMatchingBracket(json, start, '[', ']');
        return new Object[]{parseArray(json.substring(start, end + 1)), end + 1};
    }

    private static Object[] parseObjectValue(String json, int start) {
        int end = findMatchingBracket(json, start, '{', '}');
        return new Object[]{parseObject(json.substring(start, end + 1)), end + 1};
    }

    private static Object[] parseNumberOrBoolean(String json, int start) {
        int end = start;
        while (end < json.length() && !Character.isWhitespace(json.charAt(end))
                && json.charAt(end) != ',' && json.charAt(end) != '}' && json.charAt(end) != ']') {
            end++;
        }
        String value = json.substring(start, end).trim();

        return switch (value) {
            case "true" -> new Object[]{true, end};
            case "false" -> new Object[]{false, end};
            case "null" -> new Object[]{null, end};
            default -> tryParseNumber(value, end);
        };
    }

    private static Object[] tryParseNumber(String value, int pos) {
        try {
            return new Object[]{Integer.parseInt(value), pos};
        } catch (NumberFormatException e) {
            return new Object[]{value, pos};
        }
    }
}
