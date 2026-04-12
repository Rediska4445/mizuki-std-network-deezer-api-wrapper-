package deezer;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class QueryBuilder {
    private final String baseUrl;
    private final List<String> pathSegments = new ArrayList<>();
    private final Map<String, String> params = new LinkedHashMap<>();

    public QueryBuilder(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public QueryBuilder path(String... segments) {
        for (String segment : segments) {
            pathSegments.add(encode(segment));
        }

        return this;
    }

    public QueryBuilder path(String segment) {
        pathSegments.add(encode(segment));
        return this;
    }

    public QueryBuilder path(long id) {
        pathSegments.add(String.valueOf(id));
        return this;
    }

    public QueryBuilder param(String key, String value) {
        params.put(key, encode(value));
        return this;
    }

    public QueryBuilder param(String key, int value) {
        params.put(key, String.valueOf(value));
        return this;
    }

    public QueryBuilder param(String key, boolean value) {
        params.put(key, String.valueOf(value));
        return this;
    }

    public String build() {
        StringBuilder url = new StringBuilder(baseUrl);

        for (String segment : pathSegments) {
            url.append("/").append(segment);
        }

        if (!params.isEmpty()) {
            url.append("?");
            String paramsStr = params.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining("&"));
            url.append(paramsStr);
        }

        return url.toString();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}