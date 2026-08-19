-- REQ-F-114 구단 공식 채널 링크
--
-- 홈페이지 주소만 넣는다. 열 곳 모두 직접 호출해 응답을 확인했다.
-- 예매처와 SNS 는 계정 주소를 확인할 방법이 마땅치 않아 비워 둔다. 화면은 값이 있는
-- 링크만 그리므로 비어 있어도 깨지지 않는다. 확인이 되면 새 마이그레이션으로 채운다.
--
-- external_ref 는 외부 제공자가 준 숫자 코드라 사람이 읽을 수 없다. short_name 으로 맞춘다.

UPDATE teams SET homepage_url = 'https://www.ncdinos.com'          WHERE short_name = 'NC';
UPDATE teams SET homepage_url = 'https://www.lgtwins.com'          WHERE short_name = 'LG';
UPDATE teams SET homepage_url = 'https://www.giantsclub.com'       WHERE short_name = '롯데';
UPDATE teams SET homepage_url = 'https://www.doosanbears.com'      WHERE short_name = '두산';
UPDATE teams SET homepage_url = 'https://www.heroesbaseball.co.kr' WHERE short_name = '키움';
UPDATE teams SET homepage_url = 'https://www.tigers.co.kr'         WHERE short_name = 'KIA';
UPDATE teams SET homepage_url = 'https://www.samsunglions.com'     WHERE short_name = '삼성';
UPDATE teams SET homepage_url = 'https://www.hanwhaeagles.co.kr'   WHERE short_name = '한화';
UPDATE teams SET homepage_url = 'https://www.ktwiz.co.kr'          WHERE short_name = 'KT';
UPDATE teams SET homepage_url = 'https://www.ssglanders.com'       WHERE short_name = 'SSG';
