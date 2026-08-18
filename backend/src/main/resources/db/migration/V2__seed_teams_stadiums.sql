-- =====================================================================
-- 구단 · 구장 시드 데이터
-- 출처: TheSportsDB search_all_teams.php?l=Korean KBO League (2026-08-18 조회)
-- 영문명은 API 반환값, 한글명은 자체 매핑
-- =====================================================================

INSERT INTO stadiums (name, name_en) VALUES
    ('잠실야구장',          'Jamsil Baseball Stadium'),
    ('고척스카이돔',        'Gocheok Sky Dome'),
    ('인천SSG랜더스필드',   'Incheon SSG Landers Field'),
    ('수원KT위즈파크',      'Suwon Baseball Stadium'),
    ('대전한밭야구장',      'Daejeon Hanbat Baseball Stadium'),
    ('광주기아챔피언스필드','Gwangju-Kia Champions Field'),
    ('대구삼성라이온즈파크','Daegu Samsung Lions Park'),
    ('사직야구장',          'Sajik Baseball Stadium'),
    ('창원NC파크',          'Changwon NC Park');

INSERT INTO teams (name, name_en, short_name, external_ref, home_stadium_id) VALUES
    ('NC 다이노스',   'NC Dinos',      'NC',   '139819', (SELECT id FROM stadiums WHERE name='창원NC파크')),
    ('LG 트윈스',     'LG Twins',      'LG',   '139820', (SELECT id FROM stadiums WHERE name='잠실야구장')),
    ('롯데 자이언츠', 'Lotte Giants',  '롯데', '139821', (SELECT id FROM stadiums WHERE name='사직야구장')),
    ('두산 베어스',   'Doosan Bears',  '두산', '139822', (SELECT id FROM stadiums WHERE name='잠실야구장')),
    ('키움 히어로즈', 'Kiwoom Heroes', '키움', '139823', (SELECT id FROM stadiums WHERE name='고척스카이돔')),
    ('KIA 타이거즈',  'Kia Tigers',    'KIA',  '139824', (SELECT id FROM stadiums WHERE name='광주기아챔피언스필드')),
    ('삼성 라이온즈', 'Samsung Lions', '삼성', '139825', (SELECT id FROM stadiums WHERE name='대구삼성라이온즈파크')),
    ('한화 이글스',   'Hanwha Eagles', '한화', '139826', (SELECT id FROM stadiums WHERE name='대전한밭야구장')),
    ('KT 위즈',       'KT Wiz',        'KT',   '139827', (SELECT id FROM stadiums WHERE name='수원KT위즈파크')),
    ('SSG 랜더스',    'SSG Landers',   'SSG',  '139828', (SELECT id FROM stadiums WHERE name='인천SSG랜더스필드'));

-- ---------------------------------------------------------------------
-- 좌석 구역
-- 주의: 구장별 실제 구역명은 상이하다. 아래는 공통 골격이며 운영자가 구장별로 조정한다 (REQ-F-605)
-- ---------------------------------------------------------------------
INSERT INTO stadium_zones (stadium_id, name, sort_order)
SELECT s.id, z.name, z.sort_order
FROM stadiums s
CROSS JOIN (VALUES
    ('중앙 지정석',      1),
    ('1루 내야 지정석',  2),
    ('3루 내야 지정석',  3),
    ('1루 응원 지정석',  4),
    ('3루 응원 지정석',  5),
    ('외야 자유석',      6),
    ('프리미엄석',       7)
) AS z(name, sort_order);

INSERT INTO zone_stats (stadium_zone_id) SELECT id FROM stadium_zones;

-- ---------------------------------------------------------------------
-- 배지 (REQ-F-703)
-- ---------------------------------------------------------------------
INSERT INTO badges (code, name, description) VALUES
    ('FIRST_LOG',    '첫 직관',        '첫 관람 기록을 남겼어요'),
    ('FIRST_WIN',    '첫 승리',        '직관한 경기에서 처음 이겼어요'),
    ('FIRST_AWAY',   '원정 개시',      '홈이 아닌 구장에서 처음 관람했어요'),
    ('ALL_STADIUMS', '전 구장 정복',   '9개 구장을 모두 방문했어요'),
    ('STREAK_3',     '3연승 직관',     '직관한 경기에서 3연승을 기록했어요'),
    ('TEN_GAMES',    '시즌 10경기',    '한 시즌에 10경기를 직관했어요');
