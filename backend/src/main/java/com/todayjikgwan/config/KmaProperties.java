package com.todayjikgwan.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 기상청 공개 API 설정 (REQ-N-024). 인증키는 환경변수로 주입한다. */
@ConfigurationProperties(prefix = "todayjikgwan.external.kma")
public record KmaProperties(
        String serviceKey,
        String forecastUrl,
        String alertUrl,
        int rainRiskThreshold,
        String syncCron) {

    public boolean isConfigured() {
        return serviceKey != null && !serviceKey.isBlank();
    }
}
