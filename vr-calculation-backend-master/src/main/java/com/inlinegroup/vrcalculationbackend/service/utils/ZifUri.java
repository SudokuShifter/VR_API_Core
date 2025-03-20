package com.inlinegroup.vrcalculationbackend.service.utils;

import com.inlinegroup.vrcalculationbackend.config.VRCalcConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
@Slf4j
public class ZifUri {
    private static final String REGEX_OF_PATH_IDS = "\\{[^}]++}";
    private final URI uri;

    public ZifUri(ZifUriBuilder zifUriBuilder) {
        this.uri = zifUriBuilder.uri;
    }

    public static class ZifUriBuilder {
        private String path;
        private final List<String> pathIds;
        private final MultiValueMap<String, String> queryParams;
        private final String scheme;
        private final String host;
        private final Integer port;
        private URI uri;

        public ZifUriBuilder(VRCalcConfig config) {
            this.pathIds = new ArrayList<>();
            this.queryParams = new LinkedMultiValueMap<>();
            this.scheme = config.getZifScheme();
            this.host = config.getZifHost();
            this.port = -1;
        }

        public ZifUriBuilder(String scheme, String host) {
            this.pathIds = new ArrayList<>();
            this.queryParams = new LinkedMultiValueMap<>();
            this.scheme = scheme;
            this.host = host;
            this.port = -1;
        }

        public ZifUriBuilder(String scheme, String host, Integer port) {
            this.pathIds = new ArrayList<>();
            this.queryParams = new LinkedMultiValueMap<>();
            this.scheme = scheme;
            this.host = host;
            if (port != null && port > 0) {
                this.port = port;
            } else {
                this.port = -1;
            }
        }

        public ZifUriBuilder(String scheme, String host, String port) {
            this(scheme, host, Integer.parseInt(port));
        }

        public ZifUriBuilder setPath(String path) {
            if (!path.isEmpty()) {
                this.path = path;
            }
            return this;
        }

        public ZifUriBuilder addPathId(String id) {
            if (!id.isEmpty()) {
                this.pathIds.add(id);
            }
            return this;
        }

        public ZifUriBuilder addQueryParams(String paramName, String value) {
            if (!paramName.isEmpty() && !value.isEmpty()) {
                this.queryParams.add(paramName, value);
            }
            return this;
        }

        public ZifUri build() {
            Pattern pattern = Pattern.compile(REGEX_OF_PATH_IDS);
            Matcher matcher = pattern.matcher(this.path);
            Map<String, String> mapPathIds = new HashMap<>();
            int i = 0;
            while (matcher.find()) {
                if (this.pathIds.size() > i) {
                    mapPathIds.put(this.path.substring(matcher.start() + 1, matcher.end() - 1), this.pathIds.get(i));
                }
                ++i;
            }
            DefaultUriBuilderFactory builderFactory = new DefaultUriBuilderFactory();
            this.uri = builderFactory.builder()
                    .scheme(this.scheme)
                    .host(this.host)
                    .port(this.port)
                    .path(this.path)
                    .queryParams(this.queryParams)
                    .build(mapPathIds);
            return new ZifUri(this);
        }
    }
}