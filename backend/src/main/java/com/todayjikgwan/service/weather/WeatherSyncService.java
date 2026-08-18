package com.todayjikgwan.service.weather;

import com.todayjikgwan.config.KmaProperties;
import com.todayjikgwan.domain.game.Game;
import com.todayjikgwan.domain.game.GameRepository;
import com.todayjikgwan.domain.weather.GameWeather;
import com.todayjikgwan.domain.weather.GameWeatherRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 날씨 배치 동기화 (REQ-N-016, REQ-N-024).
 *
 * <p>화면 요청이 기상청 API 로 직접 전달되지 않도록 서버가 주기적으로 당겨와 캐시한다.
 * 단기예보는 3일치만 제공하므로 그 범위의 예정 경기만 대상으로 한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherSyncService {

    /** 단기예보 제공 범위 */
    private static final int FORECAST_DAYS = 3;

    private final GameRepository gameRepository;
    private final GameWeatherRepository weatherRepository;
    private final WeatherProvider weatherProvider;
    private final KmaProperties properties;

    @Scheduled(cron = "${todayjikgwan.external.kma.sync-cron}", zone = "Asia/Seoul")
    public void scheduledSync() {
        sync();
    }

    @Transactional
    public int sync() {
        if (!properties.isConfigured()) {
            log.info("기상청 인증키 미설정 — 날씨 동기화를 건너뜁니다");
            return 0;
        }
        LocalDate from = LocalDate.now();
        LocalDate to = from.plusDays(FORECAST_DAYS);
        List<Game> targets = gameRepository.findUpcomingBetween(from, to);

        int updated = 0;
        for (Game game : targets) {
            var stadium = game.getStadium();
            if (!stadium.hasGrid()) {
                continue;
            }
            var forecast = weatherProvider.forecast(
                    stadium.getGridNx(), stadium.getGridNy(), game.getStartAt());
            if (forecast.isEmpty()) {
                continue;
            }
            GameWeather w = weatherRepository.findById(game.getId())
                    .orElseGet(() -> new GameWeather(game.getId()));
            var f = forecast.get();
            w.update(f.temperature(), f.sky(), f.precipType(), f.precipProbability());
            weatherRepository.save(w);
            updated++;
        }
        log.info("날씨 동기화 완료: 대상 {}건 / 갱신 {}건", targets.size(), updated);
        return updated;
    }
}
