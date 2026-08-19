package com.todayjikgwan.service.gamedata;

import com.todayjikgwan.domain.game.Game;
import com.todayjikgwan.domain.game.GameRepository;
import com.todayjikgwan.domain.game.GameStatus;
import com.todayjikgwan.domain.team.Stadium;
import com.todayjikgwan.domain.team.StadiumRepository;
import com.todayjikgwan.domain.team.Team;
import com.todayjikgwan.domain.team.TeamRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 경기 데이터 동기화 (REQ-F-107).
 *
 * <p>붙어 있는 제공자를 차례로 물어보고 우리 쪽 경기로 옮긴다. 어느 제공자가 붙었는지는
 * 이 서비스가 알 필요가 없다. 켜진 것이 하나도 없으면 아무 일도 일어나지 않고, 그것이
 * 운영자 등록만으로 동작하는 기본 상태다.
 *
 * <p><b>운영자가 확정한 결과는 덮어쓰지 않는다.</b> 경기 결과는 운영자만 정한다는 규칙
 * (REQ-NF-015)이 외부 데이터가 들어와도 그대로 지켜져야 하기 때문이다. 외부 값은 아직
 * 확정되지 않은 경기를 채우는 데만 쓴다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameSyncService {

    private final List<GameDataProvider> providers;
    private final GameRepository gameRepository;
    private final TeamRepository teamRepository;
    private final StadiumRepository stadiumRepository;

    @Transactional
    public Map<String, Object> sync(LocalDate date) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("date", date.toString());

        List<Map<String, Object>> byProvider = providers.stream()
                .map(p -> syncOne(p, date))
                .toList();
        result.put("providers", byProvider);
        result.put("created", byProvider.stream().mapToInt(m -> (int) m.get("created")).sum());
        result.put("updated", byProvider.stream().mapToInt(m -> (int) m.get("updated")).sum());
        return result;
    }

    private Map<String, Object> syncOne(GameDataProvider provider, LocalDate date) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("provider", provider.displayName());
        m.put("enabled", provider.isEnabled());
        int created = 0;
        int updated = 0;
        int skipped = 0;
        // 왜 걸러졌는지 남긴다. 숫자만 보면 제공자가 아무것도 안 준 것과 구별되지 않는다
        List<String> unmatched = new ArrayList<>();

        if (provider.isEnabled()) {
            for (GameDataProvider.ExternalGame ext : provider.fetchByDate(date)) {
                Optional<Team> home = teamRepository.findByName(ext.homeTeamName());
                Optional<Team> away = teamRepository.findByName(ext.awayTeamName());
                Optional<Stadium> stadium = stadiumRepository.findByName(ext.stadiumName());
                // 우리가 모르는 구단·구장이면 넘긴다. 이름 표기가 제공자마다 달라
                // 억지로 맞추면 엉뚱한 경기가 만들어진다
                if (home.isEmpty() || away.isEmpty() || stadium.isEmpty()) {
                    skipped++;
                    if (unmatched.size() < 5) {
                        unmatched.add("%s vs %s @ %s".formatted(
                                ext.homeTeamName(), ext.awayTeamName(), ext.stadiumName()));
                    }
                    continue;
                }

                Optional<Game> existing = gameRepository.findByExternalRef(ext.externalRef());
                if (existing.isPresent()) {
                    Game g = existing.get();
                    if (g.isResultConfirmed()) {
                        skipped++;               // 운영자가 확정한 결과는 건드리지 않는다
                        continue;
                    }
                    if (ext.finished() && ext.homeScore() != null && ext.awayScore() != null) {
                        g.revise(GameStatus.FINISHED, ext.homeScore(), ext.awayScore());
                        g.markSynced(provider.source());
                        updated++;
                    }
                } else {
                    Game g = Game.schedule(date.getYear(), ext.startAt(),
                            stadium.get(), home.get(), away.get());
                    g.markExternal(ext.externalRef(), provider.source());
                    if (ext.finished() && ext.homeScore() != null && ext.awayScore() != null) {
                        g.revise(GameStatus.FINISHED, ext.homeScore(), ext.awayScore());
                        g.markSynced(provider.source());
                    }
                    gameRepository.save(g);
                    created++;
                }
            }
        }

        m.put("created", created);
        m.put("updated", updated);
        m.put("skipped", skipped);
        if (!unmatched.isEmpty()) {
            m.put("unmatchedSample", unmatched);
            m.put("reason", "구단 · 구장 이름이 우리 데이터와 맞지 않는다. 대응표가 있어야 쓸 수 있다");
        }
        return m;
    }
}
