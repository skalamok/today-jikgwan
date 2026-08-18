package com.todayjikgwan.domain.team;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "teams")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "name_en", length = 100)
    private String nameEn;

    @Column(name = "short_name", nullable = false, length = 10)
    private String shortName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_stadium_id")
    private Stadium homeStadium;

    /** TheSportsDB idTeam. 외부 동기화 시 매핑 키 */
    @Column(name = "external_ref", length = 50, unique = true)
    private String externalRef;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    /** REQ-F-114 구단 공식 채널. 운영자가 등록하며 미등록 시 화면에 노출하지 않는다 */
    @Column(name = "homepage_url", length = 500)
    private String homepageUrl;

    @Column(name = "ticket_url", length = 500)
    private String ticketUrl;

    @Column(name = "instagram_url", length = 500)
    private String instagramUrl;

    @Column(name = "youtube_url", length = 500)
    private String youtubeUrl;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
