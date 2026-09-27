package com.javadslab.trace;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 手写迷你 JSON 库：序列化 + 解析。
 * 表示法：对象=Map<String,Object>，数组=List<Object>，字符串=String，
 * 数字=Number，布尔=Boolean，空=null。
 * 不依赖任何第三方库，trace 输出与解析都走这里。
 */
public final class Json {

    private Json() {}

    /* ============================ 序列化 ============================ */

    /** 紧凑格式输出。 */
    public static String write(Object v) {
        StringBuilder sb = new StringBuilder();
        writeValue(sb, v);
        return sb.toString();
    }

    /** 缩进格式输出（2 空格），便于人读与教学演示。 */
    public static String writePretty(Object v) {
        StringBuilder sb = new StringBuilder();
        writePretty(sb, v, 0);
        return sb.toString();
    }

    private static void writePretty(StringBuilder sb, Object v, int indent) {
        if (v instanceof Map<?, ?> m && !m.isEmpty()) {
            sb.append("{\n");
            int i = 0;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                pad(sb, indent + 1).append(quote(String.valueOf(e.getKey()))).append(": ");
                writePretty(sb, e.getValue(), indent + 1);
                if (++i < m.size()) sb.append(',');
                sb.append('\n');
            }
            pad(sb, indent).append('}');
        } else if (v instanceof List<?> l && !l.isEmpty()) {
            sb.append("[\n");
            for (int i = 0; i < l.size(); i++) {
                pad(sb, indent + 1);
                writePretty(sb, l.get(i), indent + 1);
                if (i < l.size() - 1) sb.append(',');
                sb.append('\n');
            }
            pad(sb, indent).append(']');
        } else {
            writeValue(sb, v);
        }
    }

    private static StringBuilder pad(StringBuilder sb, int n) {
        for (int i = 0; i < n; i++) sb.append("  ");
        return sb;
    }

    private static void writeValue(StringBuilder sb, Object v) {
        if (v == null) {
            sb.append("null");
        } else if (v instanceof String s) {
            sb.append(quote(s));
        } else if (v instanceof Boolean || v instanceof Number) {
            sb.append(v);
        } else if (v instanceof Map<?, ?> m) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                sb.append(quote(String.valueOf(e.getKey()))).append(':');
                writeValue(sb, e.getValue());
            }
            sb.append('}');
        } else if (v instanceof List<?> l) {
            sb.append('[');
            for (int i = 0; i < l.size(); i++) {
                if (i > 0) sb.append(',');
                writeValue(sb, l.get(i));
            }
            sb.append(']');
        } else if (v instanceof Object[] a) {
            List<Object> list = new ArrayList<>(a.length);
            for (Object o : a) list.add(o);
            writeValue(sb, list);
        } else {
            // 其它类型退化为字符串
            sb.append(quote(String.valueOf(v)));
        }
    }

    private static String quote(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 2);
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    /* ============================ 解析 ============================ */

    /** 解析 JSON 文本为 Map/List/String/Number/Boolean/null。非法输入抛 IllegalArgumentException。 */
    public static Object parse(String text) {
        Parser p = new Parser(text);
        Object v = p.parseValue();
        p.skipWs();
        if (!p.atEnd()) throw p.err("trailing characters after JSON value");
        return v;
    }

    private static final class Parser {
        final String s;
        int i;

        Parser(String s) { this.s = s; }

        boolean atEnd() { return i >= s.length(); }

        IllegalArgumentException err(String msg) {
            return new IllegalArgumentException("JSON parse error at offset " + i + ": " + msg);
        }

        void skipWs() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        }

        char peek() {
            if (atEnd()) throw err("unexpected end of input");
            return s.charAt(i);
        }

        void expect(char c) {
            if (atEnd() || s.charAt(i) != c) throw err("expected '" + c + "'");
            i++;
        }

        Object parseValue() {
            skipWs();
            char c = peek();
            return switch (c) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> parseKeyword("true", Boolean.TRUE);
                case 'f' -> parseKeyword("false", Boolean.FALSE);
                case 'n' -> parseKeyword("null", null);
                default -> parseNumber();
            };
        }

        Object parseKeyword(String word, Object val) {
            if (i + word.length() > s.length() || !s.startsWith(word, i)) throw err("bad keyword");
            i += word.length();
            return val;
        }

        Map<String, Object> parseObject() {
            expect('{');
            Map<String, Object> m = new LinkedHashMap<>();
            skipWs();
            if (peek() == '}') { i++; return m; }
            while (true) {
                skipWs();
                String key = parseString();
                skipWs();
                expect(':');
                Object v = parseValue();
                m.put(key, v);
                skipWs();
                char c = peek();
                if (c == ',') { i++; continue; }
                if (c == '}') { i++; return m; }
                throw err("expected ',' or '}' in object");
            }
        }

        List<Object> parseArray() {
            expect('[');
            List<Object> l = new ArrayList<>();
            skipWs();
            if (peek() == ']') { i++; return l; }
            while (true) {
                l.add(parseValue());
                skipWs();
                char c = peek();
                if (c == ',') { i++; continue; }
                if (c == ']') { i++; return l; }
                throw err("expected ',' or ']' in array");
            }
        }

        String parseString() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (atEnd()) throw err("unterminated string");
                char c = s.charAt(i++);
                if (c == '"') return sb.toString();
                if (c == '\\') {
                    char e = s.charAt(i++);
                    switch (e) {
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case '/' -> sb.append('/');
                        case 'n' -> sb.append('\n');
                        case 't' -> sb.append('\t');
                        case 'r' -> sb.append('\r');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'u' -> {
                            if (i + 4 > s.length()) throw err("bad \\u escape");
                            sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                            i += 4;
                        }
                        default -> throw err("bad escape '\\" + e + "'");
                    }
                } else {
                    sb.append(c);
                }
            }
        }

        Number parseNumber() {
            int start = i;
            if (peek() == '-') i++;
            while (!atEnd()) {
                char c = s.charAt(i);
                if ((c >= '0' && c <= '9') || c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') i++;
                else break;
            }
            String num = s.substring(start, i);
            if (num.isEmpty() || num.equals("-")) throw err("bad number");
            try {
                if (num.indexOf('.') < 0 && num.indexOf('e') < 0 && num.indexOf('E') < 0) {
                    return Long.parseLong(num);
                }
                return Double.parseDouble(num);
            } catch (NumberFormatException e) {
                throw err("bad number '" + num + "'");
            }
        }
    }

    /* ============================ 取值便捷方法 ============================ */

    @SuppressWarnings("unchecked")
    public static Map<String, Object> obj(Object v) {
        return (Map<String, Object>) v;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> arr(Object v) {
        return (List<Object>) v;
    }

    public static String str(Object v) { return String.valueOf(v); }

    public static long num(Object v) { return ((Number) v).longValue(); }
}
