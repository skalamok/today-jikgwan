-- REQ-NF-015. 경기 결과는 운영자만 등록·정정한다.
-- 사용자 제보로 결과를 확정하던 경로를 제거한다. 스코어는 어디서든 확인할 수 있는
-- 객관적 사실이라 사용자 입력으로 정할 이유가 없고, 잘못 확정되면 그 경기를 기록한
-- 모든 사용자의 전적이 함께 틀어진다.
DROP TABLE IF EXISTS game_result_reports;

-- 제보로 확정된 결과가 남아 있으면 출처를 운영자 등록으로 정정한다.
UPDATE games SET source = 'MANUAL' WHERE source = 'USER_REPORT';
