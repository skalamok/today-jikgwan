package com.todayjikgwan.api.team.dto;

import com.todayjikgwan.domain.team.Team;

/** 구단 1건. 공식 채널을 함께 준다 (REQ-F-114) */
public record TeamResponse(Long id, String name, String shortName, String logoUrl,
                           Channels channels) {

    /**
     * REQ-F-114 구단 공식 채널.
     *
     * <p>확인하지 못한 링크는 null 로 둔다. 화면은 값이 있는 것만 그리므로
     * 비어 있어도 빈 버튼이 남지 않는다.
     */
    public record Channels(String homepageUrl, String ticketUrl,
                           String instagramUrl, String youtubeUrl) {

        static Channels of(Team t) {
            return new Channels(t.getHomepageUrl(), t.getTicketUrl(),
                                t.getInstagramUrl(), t.getYoutubeUrl());
        }

        // isEmpty() 같은 메서드를 두면 Jackson 이 프로퍼티로 잡아 명세에 없는 필드가
        // 응답에 새어 나간다. 링크가 있는지는 화면이 값으로 판단한다
    }

    public static TeamResponse from(Team t) {
        return new TeamResponse(t.getId(), t.getName(), t.getShortName(),
                                t.getLogoUrl(), Channels.of(t));
    }
}
