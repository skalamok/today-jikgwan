-- 설계에는 넣었으나 구현에서 쓰지 않는 테이블을 정리한다.
-- 두 테이블 모두 엔티티와 리포지터리가 없고 행도 0이다.

-- 이메일 인증(REQ-F-001)과 비밀번호 재설정(REQ-F-004)이 미구현이라 발급되는 토큰이 없다.
-- 두 기능을 구현할 때 다시 만든다.
DROP TABLE IF EXISTS auth_tokens;

-- 관람 계획 편성이 구장 목록을 요청 파라미터로 받아 처리하도록 구현되어
-- 계획-구장 조인 테이블을 쓰지 않는다. 가능 요일(available_days)도 같은 방식으로
-- viewing_plans 컬럼에 두고 있어 표현 방식을 맞춘다.
DROP TABLE IF EXISTS viewing_plan_stadiums;
