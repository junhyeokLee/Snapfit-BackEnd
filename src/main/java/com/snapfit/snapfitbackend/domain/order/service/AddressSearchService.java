package com.snapfit.snapfitbackend.domain.order.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressSearchService {

    private final ObjectMapper objectMapper;

    @Value("${snapfit.address.juso.enabled:true}")
    private boolean enabled;

    @Value("${snapfit.address.juso.key:}")
    private String jusoKey;

    @Value("${snapfit.address.juso.base-url:https://business.juso.go.kr/addrlink/addrLinkApi.do}")
    private String jusoBaseUrl;

    @Value("${snapfit.address.juso.count-per-page:10}")
    private int countPerPage;

    @Value("${snapfit.address.juso.timeout-ms:4000}")
    private int timeoutMs;

    @Value("${snapfit.address.juso.history-yn:N}")
    private String historyYn;

    @Value("${snapfit.address.juso.first-sort:road}")
    private String firstSort;

    @Value("${snapfit.address.juso.add-info-yn:Y}")
    private String addInfoYn;

    public AddressSearchResponse search(String keyword, int page) {
        String query = keyword == null ? "" : keyword.trim();
        if (query.length() < 2) {
            throw new IllegalArgumentException("keyword must be at least 2 chars");
        }
        if (!enabled) {
            return AddressSearchResponse.builder()
                    .keyword(query)
                    .page(Math.max(1, page))
                    .countPerPage(countPerPage)
                    .totalCount(0)
                    .items(List.of())
                    .build();
        }
        if (jusoKey == null || jusoKey.isBlank()) {
            throw new IllegalStateException("SNAPFIT_ADDRESS_JUSO_KEY is required");
        }

        int safePage = Math.max(1, page);
        String url = jusoBaseUrl
                + "?confmKey=" + enc(jusoKey)
                + "&currentPage=" + safePage
                + "&countPerPage=" + Math.max(1, countPerPage)
                + "&keyword=" + enc(query)
                + "&resultType=json"
                + "&hstryYn=" + enc(blankToDefault(historyYn, "N"))
                + "&firstSort=" + enc(blankToDefault(firstSort, "road"))
                + "&addInfoYn=" + enc(blankToDefault(addInfoYn, "Y"));

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(timeoutMs))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(Duration.ofMillis(timeoutMs))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Address API HTTP error: " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode results = root.path("results");
            JsonNode common = results.path("common");
            String errorCode = common.path("errorCode").asText("0");
            if (!"0".equals(errorCode)) {
                String errorMessage = common.path("errorMessage").asText("주소 검색 실패");
                throw new IllegalStateException("Address API error: " + errorMessage);
            }

            int total = parseInt(common.path("totalCount").asText("0"), 0);
            List<AddressItem> items = new ArrayList<>();
            JsonNode juso = results.path("juso");
            if (juso.isArray()) {
                for (JsonNode node : juso) {
                    items.add(AddressItem.builder()
                            .zipCode(node.path("zipNo").asText(""))
                            .roadAddress(node.path("roadAddr").asText(""))
                            .roadAddressPart1(node.path("roadAddrPart1").asText(""))
                            .roadAddressPart2(node.path("roadAddrPart2").asText(""))
                            .jibunAddress(node.path("jibunAddr").asText(""))
                            .englishAddress(node.path("engAddr").asText(""))
                            .buildingName(node.path("bdNm").asText(""))
                            .hemdName(node.path("hemdNm").asText(""))
                            .build());
                }
            }

            return AddressSearchResponse.builder()
                    .keyword(query)
                    .page(safePage)
                    .countPerPage(countPerPage)
                    .totalCount(total)
                    .items(items)
                    .build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Address API call failed", e);
        } catch (IOException e) {
            throw new IllegalStateException("Address API call failed", e);
        }
    }

    private static int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private static String enc(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static String blankToDefault(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    @Getter
    @Builder
    public static class AddressSearchResponse {
        private String keyword;
        private int page;
        private int countPerPage;
        private int totalCount;
        private List<AddressItem> items;
    }

    @Getter
    @Builder
    public static class AddressItem {
        private String zipCode;
        private String roadAddress;
        private String roadAddressPart1;
        private String roadAddressPart2;
        private String jibunAddress;
        private String englishAddress;
        private String buildingName;
        private String hemdName;
    }
}
