package com.todayjikgwan.service;

import com.todayjikgwan.api.plan.dto.PlanCreateRequest;
import com.todayjikgwan.api.plan.dto.PlanGenerateResponse;
import com.todayjikgwan.common.exception.ApiException;
import com.todayjikgwan.common.exception.ErrorCode;
import com.todayjikgwan.domain.game.Game;
import com.todayjikgwan.domain.game.GameRepository;
import com.todayjikgwan.domain.plan.*;
import com.todayjikgwan.domain.stat.StatDimension;
import com.todayjikgwan.domain.stat.UserStat;
import com.todayjikgwan.domain.stat.UserStatRepository;
import com.todayjikgwan.domain.user.User;
import com.todayjikgwan.domain.user.UserRepository;
import com.todayjikgwan.domain.weather.GameWeather;
import com.todayjikgwan.domain.weather.GameWeatherRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관람 계획 자동 편성 (REQ-F-401 ~ 404).
 *
 * <p><b>접근 방식</b><br>
 * 제약을 만족하는 경기를 걸러낸 뒤 우선순위로 정렬해 목표 수만큼 고른다.
 * 조합 최적화를 돌리지 않는 이유는, 이 문제의 제약이 대부분 <b>경기 단위로 독립적</b>이기 때문이다.
 * 요일·구장·날씨·경기당 예산은 다른 경기의 선택 여부와 무관하게 판정된다.
 * 경기 간 상호작용이 있는 제약은 총 예산과 같은 날 중복뿐이며, 이 둘은 순차 선택으로 처리된다.
 *
 * <p><b>목표를 못 채우면</b><br>
 * 제약을 하나씩 풀어보며 몇 경기가 더 생기는지 계산해 완화 후보로 제시한다.
 * "조건에 맞는 경기가 없습니다"로 끝내면 사용자가 무엇을 바꿔야 할지 알 수 없다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ViewingPlanService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /** 사용자 기록이 없을 때 사용할 경기당 기본 예상 비용 */
    private static final int DEFAULT_COST_PER_GAME = 30_000;

    private final ViewingPlanRepository planRepository;
    private final ViewingPlanItemRepository itemRepository;
    private final GameRepository gameRepository;
    private final GameWeatherRepository weatherRepository;
    private final UserStatRepository userStatRepository;
    private final UserRepository userRepository;

    @Transactional
    public Long create(Long userId, PlanCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));

        // 시즌당 1개. 기존 계획이 있으면 항목까지 지우고 다시 만든다.
        // JPA 는 flush 시점을 스스로 정하므로, 명시적으로 비워주지 않으면
        // DELETE 가 INSERT 뒤로 밀려 uk_plan_user_season 제약에 걸린다.
        planRepository.findByUserIdAndSeasonYear(userId, request.seasonYear())
                .ifPresent(existing -> {
                    itemRepository.deleteByPlanId(existing.getId());
                    itemRepository.flush();
                    planRepository.delete(existing);
                    planRepository.flush();
                });

        Set<DayOfWeek> days = request.availableDays() == null ? Set.of()
                : request.availableDays().stream().map(DayOfWeek::valueOf).collect(Collectors.toSet());

        ViewingPlan plan = new ViewingPlan(user, request.seasonYear(), request.targetCount(),
                request.budgetTotal(), request.maxCostPerGame(), days, request.maxPrecipProb());
        return planRepository.save(plan).getId();
    }

    /** REQ-F-403 */
    @Transactional
    public PlanGenerateResponse generate(Long userId, Long planId, List<Long> stadiumIds) {
        ViewingPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (!plan.getUser().getId().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        List<Game> candidates = gameRepository.findUpcomingInSeason(plan.getSeasonYear(), LocalDate.now());
        Long cheerTeamId = plan.getUser().getFavoriteTeam() == null
                ? null : plan.getUser().getFavoriteTeam().getId();
        Map<Long, Integer> costByStadium = costByStadium(userId);
        Set<Long> allowedStadiums = (stadiumIds == null || stadiumIds.isEmpty())
                ? null : new HashSet<>(stadiumIds);

        // ---------- 1단계: 제약 필터링 (탈락 사유를 함께 센다) ----------
        Map<String, Integer> excluded = new LinkedHashMap<>();
        List<Game> passed = new ArrayList<>();

        for (Game g : candidates) {
            if (!plan.days().contains(g.getGameDate().getDayOfWeek())) {
                excluded.merge("AVAILABLE_DAYS", 1, Integer::sum); continue;
            }
            if (allowedStadiums != null && !allowedStadiums.contains(g.getStadium().getId())) {
                excluded.merge("STADIUMS", 1, Integer::sum); continue;
            }
            if (exceedsPrecip(plan, g)) {
                excluded.merge("PRECIP", 1, Integer::sum); continue;
            }
            if (plan.getMaxCostPerGame() != null
                    && costOf(g, costByStadium) > plan.getMaxCostPerGame()) {
                excluded.merge("MAX_COST_PER_GAME", 1, Integer::sum); continue;
            }
            passed.add(g);
        }

        // ---------- 2단계: 우선순위 정렬 ----------
        // 응원팀 경기 우선 → 과거 승률이 높았던 구장 → 이른 날짜
        Map<Long, Double> winRateByStadium = winRateByStadium(userId);
        passed.sort(Comparator
                .comparing((Game g) -> !isCheerTeamGame(g, cheerTeamId))              // 응원팀 경기 먼저
                .thenComparing(g -> -winRateByStadium.getOrDefault(g.getStadium().getId(), 0.0))
                .thenComparing(Game::getStartAt));

        // ---------- 3단계: 순차 선택 (경기 간 상호작용이 있는 제약 처리) ----------
        List<Game> selected = new ArrayList<>();
        Set<LocalDate> usedDates = new HashSet<>();
        int budgetUsed = 0;
        int budgetBlocked = 0;

        for (Game g : passed) {
            if (selected.size() >= plan.getTargetCount()) {
                break;
            }
            if (!usedDates.add(g.getGameDate())) {       // 같은 날 중복 관람은 제외
                continue;
            }
            int cost = costOf(g, costByStadium);
            if (plan.getBudgetTotal() != null && budgetUsed + cost > plan.getBudgetTotal()) {
                usedDates.remove(g.getGameDate());
                budgetBlocked++;
                continue;
            }
            selected.add(g);
            budgetUsed += cost;
        }
        if (budgetBlocked > 0) {
            excluded.put("BUDGET_TOTAL", budgetBlocked);
        }

        // ---------- 4단계: 저장 ----------
        itemRepository.deleteByPlanId(plan.getId());
        itemRepository.flush();
        selected.forEach(g -> itemRepository.save(new ViewingPlanItem(plan, g)));

        // ---------- 5단계: 목표 미달 시 완화 후보 ----------
        List<PlanGenerateResponse.Relaxation> relaxations = selected.size() < plan.getTargetCount()
                ? suggestRelaxations(plan, candidates, allowedStadiums, costByStadium, selected.size())
                : List.of();

        return new PlanGenerateResponse(
                plan.getTargetCount(), selected.size(), budgetUsed,
                selected.stream()
                        .sorted(Comparator.comparing(Game::getStartAt))   // 화면 표시는 날짜순
                        .map(g -> toProposed(g, cheerTeamId, costByStadium)).toList(),
                excluded.entrySet().stream()
                        .map(e -> new PlanGenerateResponse.FilterStat(e.getKey(), labelOf(e.getKey()), e.getValue()))
                        .toList(),
                relaxations);
    }

    /**
     * 제약을 하나씩 풀어 몇 경기가 더 확보되는지 계산한다.
     * 실제로 편성을 다시 돌리지 않고 필터만 다시 적용하므로 비용이 낮다.
     */
    private List<PlanGenerateResponse.Relaxation> suggestRelaxations(
            ViewingPlan plan, List<Game> candidates, Set<Long> allowedStadiums,
            Map<Long, Integer> costByStadium, int current) {

        List<PlanGenerateResponse.Relaxation> out = new ArrayList<>();

        // 요일 제약을 풀면?  (제약이 걸려 있을 때만 의미가 있다)
        boolean daysConstrained = plan.days().size() < DayOfWeek.values().length;
        long byDays = !daysConstrained ? 0 : candidates.stream()
                .filter(g -> allowedStadiums == null || allowedStadiums.contains(g.getStadium().getId()))
                .filter(g -> !exceedsPrecip(plan, g))
                .map(g -> g.getGameDate()).distinct().count();
        if (daysConstrained && byDays > current) {
            Set<DayOfWeek> missing = EnumSet.allOf(DayOfWeek.class);
            missing.removeAll(plan.days());
            String names = missing.stream()
                    .map(d -> d.getDisplayName(TextStyle.SHORT, Locale.KOREAN))
                    .collect(Collectors.joining("·"));
            out.add(new PlanGenerateResponse.Relaxation("AVAILABLE_DAYS",
                    "%s요일을 추가하면 최대 %d경기까지 채울 수 있어요".formatted(names, byDays),
                    (int) byDays - current));
        }

        // 구장 제약을 풀면?
        if (allowedStadiums != null) {
            long byStadium = candidates.stream()
                    .filter(g -> plan.days().contains(g.getGameDate().getDayOfWeek()))
                    .filter(g -> !exceedsPrecip(plan, g))
                    .map(Game::getGameDate).distinct().count();
            if (byStadium > current) {
                out.add(new PlanGenerateResponse.Relaxation("STADIUMS",
                        "이동 가능 구장을 넓히면 최대 %d경기까지 채울 수 있어요".formatted(byStadium),
                        (int) byStadium - current));
            }
        }

        // 예산 제약을 풀면?
        if (plan.getBudgetTotal() != null) {
            int needed = (plan.getTargetCount() - current) * DEFAULT_COST_PER_GAME;
            out.add(new PlanGenerateResponse.Relaxation("BUDGET_TOTAL",
                    "예산을 약 %,d원 늘리면 목표를 채울 수 있어요".formatted(needed), 0));
        }
        return out;
    }

    private boolean exceedsPrecip(ViewingPlan plan, Game g) {
        if (plan.getMaxPrecipProb() == null) {
            return false;
        }
        return weatherRepository.findById(g.getId())
                .map(GameWeather::getPrecipProb)
                .map(p -> p != null && p > plan.getMaxPrecipProb())
                .orElse(false);      // 예보가 없으면 제외하지 않는다
    }

    private boolean isCheerTeamGame(Game g, Long cheerTeamId) {
        return cheerTeamId != null && g.hasTeam(cheerTeamId);
    }

    private int costOf(Game g, Map<Long, Integer> costByStadium) {
        return costByStadium.getOrDefault(g.getStadium().getId(), DEFAULT_COST_PER_GAME);
    }

    /** 사용자의 과거 기록에서 구장별 평균 지출을 구한다. 기록이 없으면 기본값을 쓴다 */
    private Map<Long, Integer> costByStadium(Long userId) {
        Map<Long, Integer> map = new HashMap<>();
        for (UserStat s : userStatRepository.findByUserIdAndDimensionAndSeasonYear(
                userId, StatDimension.STADIUM, UserStat.SEASON_ALL)) {
            if (s.getGames() > 0 && s.getTotalCost() > 0) {
                map.put(Long.valueOf(s.getDimensionKey()), s.getTotalCost() / s.getGames());
            }
        }
        return map;
    }

    private Map<Long, Double> winRateByStadium(Long userId) {
        Map<Long, Double> map = new HashMap<>();
        for (UserStat s : userStatRepository.findByUserIdAndDimensionAndSeasonYear(
                userId, StatDimension.STADIUM, UserStat.SEASON_ALL)) {
            int decided = s.getWins() + s.getLosses();
            if (decided > 0) {
                map.put(Long.valueOf(s.getDimensionKey()), (double) s.getWins() / decided);
            }
        }
        return map;
    }

    private PlanGenerateResponse.ProposedGame toProposed(Game g, Long cheerTeamId,
                                                         Map<Long, Integer> costByStadium) {
        Integer precip = weatherRepository.findById(g.getId())
                .map(GameWeather::getPrecipProb).orElse(null);
        return new PlanGenerateResponse.ProposedGame(
                g.getId(), g.getGameDate(),
                g.getGameDate().getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.KOREAN),
                g.getStartAt().atZoneSameInstant(KST).toLocalTime().toString().substring(0, 5),
                g.getStadium().getName(),
                "%s vs %s".formatted(g.getHomeTeam().getShortName(), g.getAwayTeam().getShortName()),
                isCheerTeamGame(g, cheerTeamId), precip, costOf(g, costByStadium));
    }

    private String labelOf(String key) {
        return switch (key) {
            case "AVAILABLE_DAYS" -> "관람 가능 요일";
            case "STADIUMS" -> "이동 가능 구장";
            case "PRECIP" -> "허용 강수 확률";
            case "MAX_COST_PER_GAME" -> "경기당 예산 상한";
            case "BUDGET_TOTAL" -> "총 예산";
            default -> key;
        };
    }
}
