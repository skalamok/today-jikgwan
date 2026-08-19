package com.todayjikgwan.domain.stat;

/** REQ-F-301~304. 차원별로 테이블을 나누지 않고 (dimension, key)로 일반화한다. */
/**
 * 전적을 나누어 보는 축.
 *
 * <p>컬럼이 varchar 라 값을 늘려도 마이그레이션이 필요 없다. 축이 늘어날 여지가
 * 있다고 보아 처음부터 그렇게 잡았다 (REQ-F-301 ~ 310).
 */
public enum StatDimension { TOTAL, SEASON, STADIUM, OPPONENT, DAY_OF_WEEK, COMPANION, WEATHER }
