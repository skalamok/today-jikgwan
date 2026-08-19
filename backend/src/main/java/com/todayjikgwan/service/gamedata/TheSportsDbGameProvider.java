package com.todayjikgwan.service.gamedata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.todayjikgwan.config.TheSportsDbProperties;
import com.todayjikgwan.domain.game.GameSource;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * TheSportsDB 연동 (REQ-F-107).
 *
 * <p>약관상 데이터 복사·가공이 허용된 것을 확인해 후보로 두었다(부록. 경기 데이터 확보 정책).
 * 다만 무료 공용 키는 한도가 낮고 KBO 데이터의 정확도를 검증하지 못해 기본은 꺼 둔다.
 * 설정에서 켜야 동작하며, 켜더라도 운영자가 확정한 결과를 덮어쓰지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TheSportsDbGameProvider implements GameDataProvider {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final TheSportsDbProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    @Override
    public GameSource source() {
        return GameSource.EXTERNAL;
    }

    @Override
    public boolean isEnabled() {
        return properties.enabled()
                && properties.apiKey() != null && !properties.apiKey().isBlank()
                && properties.leagueId() != null && !properties.leagueId().isBlank();
    }

    @Override
    public String displayName() {
        return "TheSportsDB";
    }

    @Override
    public List<ExternalGame> fetchByDate(LocalDate date) {
        if (!isEnabled()) {
            return List.of();
        }
        String url = "%s/%s/eventsday.php?d=%s&l=%s".formatted(
                properties.baseUrl(), properties.apiKey(), date, properties.leagueId());
        try {
            HttpResponse<String> res = http.send(
                    HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(8)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                log.warn("TheSportsDB 응답 {} ({}). 이번 동기화는 건너뛴다", res.statusCode(), date);
                return List.of();
            }
            JsonNode events = objectMapper.readTree(res.body()).path("events");
            List<ExternalGame> out = new ArrayList<>();
            for (JsonNode e : events) {
                OffsetDateTime startAt = parseStart(e, date);
                out.add(new ExternalGame(
                        "tsdb:" + e.path("idEvent").asText(),
                        date, startAt,
                        text(e, "strVenue"), text(e, "strHomeTeam"), text(e, "strAwayTeam"),
                        score(e, "intHomeScore"), score(e, "intAwayScore"),
                        "Match Finished".equalsIgnoreCase(text(e, "strStatus"))));
            }
            return out;
        } catch (Exception ex) {
            // REQ-NF-010 외부 실패가 서비스 전체를 멈추게 두지 않는다
            log.warn("TheSportsDB 조회 실패 ({}): {}", date, ex.toString());
            return List.of();
        }
    }

    private OffsetDateTime parseStart(JsonNode e, LocalDate date) {
        String t = text(e, "strTime");
        LocalTime time = LocalTime.of(18, 30);          // 표기가 없으면 KBO 통상 개시 시각
        try {
            if (t != null && !t.isBlank()) {
                time = LocalTime.parse(t.length() > 5 ? t.substring(0, 5) : t);
            }
        } catch (Exception ignored) {
            // 표기가 어긋나면 기본값을 쓴다. 시각 하나 때문에 경기를 버릴 이유는 없다
        }
        return date.atTime(time).atZone(KST).toOffsetDateTime();
    }

    private String text(JsonNode e, String field) {
        JsonNode n = e.path(field);
        return n.isMissingNode() || n.isNull() ? null : n.asText();
    }

    private Integer score(JsonNode e, String field) {
        String v = text(e, field);
        try {
            return v == null || v.isBlank() ? null : Integer.valueOf(v);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
