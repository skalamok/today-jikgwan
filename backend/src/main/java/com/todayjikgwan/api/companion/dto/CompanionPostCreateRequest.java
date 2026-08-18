package com.todayjikgwan.api.companion.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CompanionPostCreateRequest(
        @NotNull Long gameId,
        @NotNull @Min(2) @Max(10) Integer capacity,
        @Size(max = 500) String intro) {
}
