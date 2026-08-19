package com.todayjikgwan.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 외부 경기 데이터 후보 (REQ-F-107, REQ-NF-013).
 *
 * <p>약관상 수집·가공이 허용된 것을 확인해 두었으나 도입 여부는 운영 시점에 정한다.
 * {@code enabled} 가 꺼져 있으면 제공자가 통째로 비활성이라 운영자 등록만으로 동작한다.
 */
@ConfigurationProperties(prefix = "todayjikgwan.external.thesportsdb")
public record TheSportsDbProperties(boolean enabled, String baseUrl, String apiKey,
                                    String leagueId, int rateLimitPerMinute) { }
