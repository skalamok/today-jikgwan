package com.todayjikgwan.service.weather;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.todayjikgwan.config.KmaProperties;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 공공데이터포털 기상청 단기예보 · 기상특보 연동 (REQ-N-024).
 *
 * <p>단기예보 발표 시각은 02·05·08·11·14·17·20·23시이며, 각 발표는 이후 3일치를 담는다.
 * 조회 시점 기준 가장 최근 발표본을 사용한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KmaWeatherProvider implements WeatherProvider {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int[] BASE_HOURS = {23, 20, 17, 14, 11, 8, 5, 2};

    /** 단기예보 코드 → 한글 표기 */
    private static final Map<String, String> SKY = Map.of("1", "맑음", "3", "구름많음", "4", "흐림");
    private static final Map<String, String> PTY = Map.of(
            "0", "없음", "1", "비", "2", "비/눈", "3", "눈", "4", "소나기");

    private final KmaProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    @Override
    public Optional<Forecast> forecast(int nx, int ny, OffsetDateTime target) {
        if (!properties.isConfigured()) {
            log.debug("기상청 인증키가 설정되지 않아 조회를 건너뜁니다");
            return Optional.empty();
        }
        var now = OffsetDateTime.now(KST);
        var base = latestBase(now);
        String url = "%s?serviceKey=%s&pageNo=1&numOfRows=1000&dataType=JSON&base_date=%s&base_time=%s&nx=%d&ny=%d"
                .formatted(properties.forecastUrl(), properties.serviceKey(),
                           base.date().format(DATE), base.time(), nx, ny);
        try {
            JsonNode items = call(url);
            if (items == null) {
                return Optional.empty();
            }
            String fcstDate = target.atZoneSameInstant(KST).toLocalDate().format(DATE);
            String fcstTime = "%02d00".formatted(target.atZoneSameInstant(KST).getHour());

            BigDecimal tmp = null; String sky = null; String pty = null; Integer pop = null;
            for (JsonNode it : items) {
                if (!fcstDate.equals(it.path("fcstDate").asText())
                        || !fcstTime.equals(it.path("fcstTime").asText())) {
                    continue;
                }
                String v = it.path("fcstValue").asText();
                switch (it.path("category").asText()) {
                    case "TMP" -> tmp = new BigDecimal(v);
                    case "SKY" -> sky = SKY.getOrDefault(v, v);
                    case "PTY" -> pty = PTY.getOrDefault(v, v);
                    case "POP" -> pop = Integer.valueOf(v);
                    default -> { }
                }
            }
            if (tmp == null && sky == null && pop == null) {
                return Optional.empty();      // 예보 범위 밖
            }
            return Optional.of(new Forecast(tmp, sky, pty, pop));
        } catch (Exception e) {
            log.warn("단기예보 조회 실패 nx={} ny={}: {}", nx, ny, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> alert(String regionCode) {
        if (!properties.isConfigured()) {
            return Optional.empty();
        }
        String url = "%s?serviceKey=%s&pageNo=1&numOfRows=10&dataType=JSON&stnId=%s"
                .formatted(properties.alertUrl(), properties.serviceKey(), regionCode);
        try {
            JsonNode items = call(url);
            if (items == null || items.isEmpty()) {
                return Optional.empty();
            }
            String title = items.get(0).path("title").asText(null);
            return Optional.ofNullable(title).filter(t -> !t.isBlank());
        } catch (Exception e) {
            log.warn("기상특보 조회 실패 stnId={}: {}", regionCode, e.getMessage());
            return Optional.empty();
        }
    }

    /** 응답의 body.items.item 배열을 꺼낸다. 실패 응답은 null. */
    private JsonNode call(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10)).GET().build();
        HttpResponse<byte[]> res = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
        String body = new String(res.body(), StandardCharsets.UTF_8);
        if (res.statusCode() != 200 || !body.trim().startsWith("{")) {
            log.warn("기상청 응답이 JSON 이 아닙니다 (status={}): {}", res.statusCode(),
                     body.substring(0, Math.min(200, body.length())));
            return null;
        }
        JsonNode root = objectMapper.readTree(body);
        String code = root.path("response").path("header").path("resultCode").asText();
        if (!"00".equals(code)) {
            log.warn("기상청 오류 응답 resultCode={} msg={}", code,
                     root.path("response").path("header").path("resultMsg").asText());
            return null;
        }
        JsonNode items = root.path("response").path("body").path("items").path("item");
        return items.isArray() ? items : null;
    }

    /** 현재 시각 기준 가장 최근 발표본 */
    private Base latestBase(OffsetDateTime now) {
        var kst = now.atZoneSameInstant(KST);
        LocalDate date = kst.toLocalDate();
        int hour = kst.getHour();
        // 발표 후 약 10분 뒤부터 조회 가능하므로 여유를 둔다
        int effective = kst.getMinute() < 15 ? hour - 1 : hour;
        for (int h : BASE_HOURS) {
            if (effective >= h) {
                return new Base(date, "%02d00".formatted(h));
            }
        }
        return new Base(date.minusDays(1), "2300");
    }

    private record Base(LocalDate date, String time) { }
}
