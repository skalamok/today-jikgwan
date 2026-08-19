package com.todayjikgwan.service.gamedata;

import com.todayjikgwan.domain.game.GameSource;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 경기 데이터 제공자 추상화 (REQ-F-107).
 *
 * <p>KBO 는 공식 공개 API 가 없고, 홈페이지 수집은 이용약관과 robots.txt 로 막혀 있다
 * (부록. 경기 데이터 확보 정책). 그래서 운영자 등록을 기본 수단으로 두되, 나중에 이용
 * 조건을 확인한 외부 경로가 생기면 구현체만 더해 갈아 끼울 수 있게 인터페이스를 둔다.
 *
 * <p>날씨(WeatherProvider)와 같은 구조다. 서비스는 어느 제공자가 붙었는지 모른 채
 * {@link #fetchByDate} 만 부른다.
 */
public interface GameDataProvider {

    /** 이 제공자가 채우는 출처. 동기화한 경기의 source 로 남는다 */
    GameSource source();

    /**
     * 쓸 수 있는 상태인가.
     *
     * <p>이용 조건을 확인하지 못했거나 키가 없으면 꺼진 채로 둔다. 꺼진 제공자는
     * 동기화에서 통째로 건너뛰므로, 설정을 넣지 않으면 운영자 등록만으로 동작한다.
     */
    boolean isEnabled();

    /** 사람이 읽을 이름. 동기화 결과를 알릴 때 쓴다 */
    String displayName();

    /** 그 날짜의 경기. 가져올 수 없으면 빈 목록 */
    List<ExternalGame> fetchByDate(LocalDate date);

    /**
     * 제공자가 준 경기 1건.
     *
     * <p>구단·구장은 이름으로 온다. 우리 쪽 식별자로 맞추는 일은 동기화 서비스가 한다.
     * 제공자마다 표기가 달라 그 변환을 인터페이스 바깥에 두면 구현체가 우리 도메인을
     * 알아야 하기 때문이다.
     */
    record ExternalGame(String externalRef, LocalDate gameDate, OffsetDateTime startAt,
                        String stadiumName, String homeTeamName, String awayTeamName,
                        Integer homeScore, Integer awayScore, boolean finished) { }
}
