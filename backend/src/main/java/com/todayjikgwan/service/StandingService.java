package com.todayjikgwan.service;

import com.todayjikgwan.api.game.dto.StandingResponse;
import com.todayjikgwan.domain.game.Game;
import com.todayjikgwan.domain.game.GameRepository;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 팀 순위 산출 (REQ-F-104).
 *
 * <p>외부에서 순위표를 가져오지 않는다. 확정된 경기 결과만 있으면 승·무·패와 승차는
 * 파생 지표이므로 직접 계산할 수 있다. 외부 데이터 의존을 줄이는 설계 결정이다.
 */
@Service
@RequiredArgsConstructor
public class StandingService {

    private final GameRepository gameRepository;

    @Transactional(readOnly = true)
    public List<StandingResponse> standings(int season) {
        Map<Long, int[]> table = new HashMap<>();      // teamId -> [승, 무, 패]
        Map<Long, String> names = new HashMap<>();

        for (Game g : gameRepository.findFinishedInSeason(season)) {
            Long home = g.getHomeTeam().getId(), away = g.getAwayTeam().getId();
            names.putIfAbsent(home, g.getHomeTeam().getName());
            names.putIfAbsent(away, g.getAwayTeam().getName());
            table.putIfAbsent(home, new int[3]);
            table.putIfAbsent(away, new int[3]);

            int hs = g.getHomeScore(), as = g.getAwayScore();
            if (hs == as) {
                table.get(home)[1]++; table.get(away)[1]++;
            } else if (hs > as) {
                table.get(home)[0]++; table.get(away)[2]++;
            } else {
                table.get(away)[0]++; table.get(home)[2]++;
            }
        }

        // 승률 = 승 / (승 + 패). 무승부는 분모에서 제외한다 (야구 표기 관례)
        List<Map.Entry<Long, int[]>> sorted = new ArrayList<>(table.entrySet());
        sorted.sort((a, b) -> Double.compare(rate(b.getValue()), rate(a.getValue())));

        List<StandingResponse> out = new ArrayList<>();
        if (sorted.isEmpty()) {
            return out;
        }
        int[] top = sorted.get(0).getValue();
        int rank = 1;
        for (Map.Entry<Long, int[]> e : sorted) {
            int[] v = e.getValue();
            // 승차 = ((1위 승 - 팀 승) + (팀 패 - 1위 패)) / 2
            double gb = ((top[0] - v[0]) + (v[2] - top[2])) / 2.0;
            out.add(new StandingResponse(rank++, e.getKey(), names.get(e.getKey()),
                    v[0] + v[1] + v[2], v[0], v[1], v[2],
                    Math.round(rate(v) * 1000) / 1000.0, gb));
        }
        return out;
    }

    private double rate(int[] v) {
        int decided = v[0] + v[2];
        return decided == 0 ? 0 : (double) v[0] / decided;
    }
}
