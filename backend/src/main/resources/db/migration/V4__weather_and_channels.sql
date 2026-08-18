-- =====================================================================
-- 날씨 연동 및 구단 공식 채널
-- REQ-F-112 ~ 114, REQ-F-216, REQ-N-024
-- =====================================================================

-- 구장 격자 좌표 (기상청 단기예보 조회용)
ALTER TABLE stadiums ADD COLUMN grid_nx INT;
ALTER TABLE stadiums ADD COLUMN grid_ny INT;
COMMENT ON COLUMN stadiums.grid_nx IS '기상청 단기예보 격자 X. backend/tools/kma_grid.py 로 산출';

UPDATE stadiums SET latitude=37.5122, longitude=127.0719, grid_nx=61, grid_ny=126 WHERE name='잠실야구장';
UPDATE stadiums SET latitude=37.4982, longitude=126.8671, grid_nx=58, grid_ny=125 WHERE name='고척스카이돔';
UPDATE stadiums SET latitude=37.4370, longitude=126.6932, grid_nx=55, grid_ny=124 WHERE name='인천SSG랜더스필드';
UPDATE stadiums SET latitude=37.2997, longitude=127.0097, grid_nx=60, grid_ny=121 WHERE name='수원KT위즈파크';
UPDATE stadiums SET latitude=36.3170, longitude=127.4290, grid_nx=68, grid_ny=100 WHERE name='대전한밭야구장';
UPDATE stadiums SET latitude=35.1682, longitude=126.8891, grid_nx=59, grid_ny=75  WHERE name='광주기아챔피언스필드';
UPDATE stadiums SET latitude=35.8410, longitude=128.6816, grid_nx=90, grid_ny=90  WHERE name='대구삼성라이온즈파크';
UPDATE stadiums SET latitude=35.1940, longitude=129.0615, grid_nx=98, grid_ny=76  WHERE name='사직야구장';
UPDATE stadiums SET latitude=35.2225, longitude=128.5822, grid_nx=89, grid_ny=76  WHERE name='창원NC파크';

-- 구단 공식 채널 (REQ-F-114)
-- 실제 URL 은 운영자가 등록한다 (REQ-F-605). 잘못된 링크를 넣지 않기 위해 비워둔다.
ALTER TABLE teams ADD COLUMN homepage_url  VARCHAR(500);
ALTER TABLE teams ADD COLUMN ticket_url    VARCHAR(500);
ALTER TABLE teams ADD COLUMN instagram_url VARCHAR(500);
ALTER TABLE teams ADD COLUMN youtube_url   VARCHAR(500);

-- 경기별 날씨 캐시 (REQ-F-112, REQ-F-113)
CREATE TABLE game_weather (
    game_id      BIGINT PRIMARY KEY REFERENCES games(id),
    temperature  NUMERIC(4,1),
    sky_code     VARCHAR(10),
    precip_type  VARCHAR(10),
    precip_prob  INT,
    alert_title  VARCHAR(100),
    fetched_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
COMMENT ON TABLE game_weather IS '기상청 단기예보 결과를 경기 단위로 캐시 (REQ-N-024 배치 동기화)';

-- 기록 시점의 날씨 (REQ-F-216)
-- 단기예보는 미래 예보이므로 경기가 지나면 재조회할 수 없다. 기록마다 복사해 보관한다.
ALTER TABLE attendance_logs ADD COLUMN weather_sky  VARCHAR(10);
ALTER TABLE attendance_logs ADD COLUMN weather_temp NUMERIC(4,1);
