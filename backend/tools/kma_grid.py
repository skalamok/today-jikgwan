# -*- coding: utf-8 -*-
"""
위경도 → 기상청 단기예보 격자(nx, ny) 변환

기상청이 공개한 Lambert Conformal Conic 변환식을 그대로 옮긴 것이다.
격자 간격이 5km 이므로 구장 좌표가 수백 미터 어긋나도 같은 격자로 떨어진다.
"""
import math

RE, GRID = 6371.00877, 5.0          # 지구 반경(km), 격자 간격(km)
SLAT1, SLAT2 = 30.0, 60.0           # 표준 위도
OLON, OLAT = 126.0, 38.0            # 기준점 경위도
XO, YO = 43, 136                    # 기준점 격자 좌표

DEGRAD = math.pi / 180.0


def to_grid(lat, lon):
    re = RE / GRID
    slat1, slat2 = SLAT1 * DEGRAD, SLAT2 * DEGRAD
    olon, olat = OLON * DEGRAD, OLAT * DEGRAD

    sn = math.tan(math.pi * 0.25 + slat2 * 0.5) / math.tan(math.pi * 0.25 + slat1 * 0.5)
    sn = math.log(math.cos(slat1) / math.cos(slat2)) / math.log(sn)
    sf = math.tan(math.pi * 0.25 + slat1 * 0.5)
    sf = (sf ** sn) * math.cos(slat1) / sn
    ro = math.tan(math.pi * 0.25 + olat * 0.5)
    ro = re * sf / (ro ** sn)

    ra = math.tan(math.pi * 0.25 + lat * DEGRAD * 0.5)
    ra = re * sf / (ra ** sn)
    theta = lon * DEGRAD - olon
    if theta > math.pi:
        theta -= 2.0 * math.pi
    if theta < -math.pi:
        theta += 2.0 * math.pi
    theta *= sn

    nx = int(ra * math.sin(theta) + XO + 0.5)
    ny = int(ro - ra * math.cos(theta) + YO + 0.5)
    return nx, ny


# 구장 좌표 (공개된 위치 정보 기준. 격자 5km 단위이므로 소폭 오차는 결과에 영향이 없다)
STADIUMS = [
    ("잠실야구장",           37.5122, 127.0719),
    ("고척스카이돔",         37.4982, 126.8671),
    ("인천SSG랜더스필드",    37.4370, 126.6932),
    ("수원KT위즈파크",       37.2997, 127.0097),
    ("대전한밭야구장",       36.3170, 127.4290),
    ("광주기아챔피언스필드", 35.1682, 126.8891),
    ("대구삼성라이온즈파크", 35.8410, 128.6816),
    ("사직야구장",           35.1940, 129.0615),
    ("창원NC파크",           35.2225, 128.5822),
]

if __name__ == "__main__":
    print("구장                    위도       경도        nx   ny")
    print("-" * 58)
    rows = []
    for name, lat, lon in STADIUMS:
        nx, ny = to_grid(lat, lon)
        rows.append((name, lat, lon, nx, ny))
        print("%-22s %8.4f %10.4f %5d %4d" % (name, lat, lon, nx, ny))

    print("\n-- V4 마이그레이션용 UPDATE 문")
    for name, lat, lon, nx, ny in rows:
        print("UPDATE stadiums SET latitude=%.4f, longitude=%.4f, grid_nx=%d, grid_ny=%d WHERE name='%s';"
              % (lat, lon, nx, ny, name))
