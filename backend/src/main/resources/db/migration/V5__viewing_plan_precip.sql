-- =====================================================================
-- 관람 계획 강수 확률 제약 (REQ-F-402)
-- =====================================================================
ALTER TABLE viewing_plans ADD COLUMN max_precip_prob INT;
COMMENT ON COLUMN viewing_plans.max_precip_prob IS '이 값을 초과하는 강수 확률의 경기는 편성에서 제외한다';
