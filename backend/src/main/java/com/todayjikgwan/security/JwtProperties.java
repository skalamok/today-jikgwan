package com.todayjikgwan.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "todayjikgwan.jwt")
public record JwtProperties(String secret, long accessTokenSeconds) {
}
