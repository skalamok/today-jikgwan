package com.todayjikgwan.api.companion.dto;

import com.todayjikgwan.domain.companion.CompanionPost;
import com.todayjikgwan.domain.game.Game;
import java.time.OffsetDateTime;
import java.util.List;

public record CompanionPostResponse(
        Long id,
        Long gameId,
        String gameLabel,
        OffsetDateTime startAt,
        String stadium,
        String authorNickname,
        String intro,
        int capacity,
        int confirmedCount,
        String status,
        String myStatus,
        List<Member> confirmedMembers) {

    public record Member(String nickname, int seq, boolean isAuthor) { }

    public static CompanionPostResponse of(CompanionPost p, String myStatus, List<Member> members) {
        Game g = p.getGame();
        return new CompanionPostResponse(
                p.getId(), g.getId(),
                "%s vs %s".formatted(g.getHomeTeam().getShortName(), g.getAwayTeam().getShortName()),
                g.getStartAt(), g.getStadium().getName(),
                p.getAuthor().getNickname(), p.getIntro(),
                p.getCapacity(), p.getConfirmedCount(), p.getStatus().name(),
                myStatus, members);
    }
}
