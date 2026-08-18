package com.todayjikgwan.api.auth.dto;

public record TokenResponse(String accessToken, long expiresIn) {
}
