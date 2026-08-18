package com.todayjikgwan.api.companion.dto;

/** 확정 순번을 함께 돌려주어 선착순 결과를 클라이언트가 확인할 수 있게 한다. */
public record ApplyResponse(int seq, int confirmedCount, int capacity) {
}
