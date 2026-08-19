package com.todayjikgwan.service.gamedata;

import com.todayjikgwan.domain.game.GameSource;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 기본 구현체 (REQ-F-107, REQ-F-601).
 *
 * <p>경기 데이터의 원본은 운영자가 직접 넣은 것이다. 밖에서 가져올 것이 없으므로
 * 빈 목록을 준다. 아무것도 하지 않는 구현체를 굳이 두는 것은, 외부 제공자를 하나도
 * 붙이지 않았을 때 동기화가 "제공자 없음" 으로 실패하지 않고 조용히 지나가게 하려는 것이다.
 */
@Component
public class ManualGameProvider implements GameDataProvider {

    @Override
    public GameSource source() {
        return GameSource.MANUAL;
    }

    /** 운영자 등록은 언제나 쓸 수 있다. 다만 가져올 것이 없다 */
    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String displayName() {
        return "운영자 등록";
    }

    @Override
    public List<ExternalGame> fetchByDate(LocalDate date) {
        return List.of();
    }
}
