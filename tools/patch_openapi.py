# -*- coding: utf-8 -*-
"""
openapi.yaml 보강 — operationId 와 공통 오류 응답

공통 응답(BadRequest / Unauthorized / Forbidden / NotFound)은 components 에 정의해
두고도 오퍼레이션에서 참조하지 않은 곳이 많았다. 인증이 필요한데 401 이 없거나,
경로 변수를 받는데 404 가 없으면 클라이언트가 무엇을 처리해야 할지 알 수 없다.

원본 서식을 지키려고 줄 단위로 고친다. 파서로 읽어 다시 쓰면 주석과
인라인 표기가 전부 날아간다.

사용: python3 tools/patch_openapi.py
"""
import io
import os
import re
import sys

import yaml

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SPEC = os.path.join(BASE, "docs", "03_API명세", "openapi.yaml")
METHODS = ("get", "post", "put", "patch", "delete")
REF = {"400": "BadRequest", "401": "Unauthorized", "403": "Forbidden", "404": "NotFound"}


# 규칙만으로는 어색해지는 것들. 단건 생성인데 복수형이 되거나,
# 읽음 처리를 replace 로 부르게 되는 경우다.
OVERRIDE = {
    ("/auth/signup", "post"): "signup",
    ("/auth/login", "post"): "login",
    ("/auth/oauth/providers", "get"): "listOauthProviders",
    ("/auth/oauth/{provider}/authorize-url", "get"): "getOauthAuthorizeUrl",
    ("/auth/oauth/{provider}/callback", "post"): "exchangeOauthCode",
    ("/auth/password-reset", "post"): "requestPasswordReset",
    ("/users/me", "get"): "getMe",
    ("/users/me", "patch"): "updateMe",
    ("/users/me/badges", "get"): "listMyBadges",
    ("/users/me/blocks", "get"): "listMyBlocks",
    ("/users/me/blocks", "post"): "blockUser",
    ("/users/me/blocks/{userId}", "delete"): "unblockUser",
    ("/notifications/read", "put"): "markNotificationsRead",
    ("/games/suggest", "post"): "suggestGames",
    ("/games/{gameId}/weather", "get"): "getGameWeather",
    ("/games/weather/sync", "post"): "syncGameWeather",
    ("/stadiums/{stadiumId}/zones/{zoneId}/reviews", "get"): "listZoneReviews",
    ("/attendance-logs", "post"): "createAttendanceLog",
    ("/attendance-logs/{logId}/photos", "post"): "uploadAttendancePhoto",
    ("/attendance-logs/{logId}/photos/{photoId}", "delete"): "deleteAttendancePhoto",
    ("/stats/me/summary", "get"): "getMyStatSummary",
    ("/stats/me", "get"): "getMyStats",
    ("/companion-posts", "post"): "createCompanionPost",
    ("/companion-posts/{postId}/applications", "post"): "applyToCompanionPost",
    ("/companion-posts/{postId}/applications", "delete"): "cancelCompanionApplication",
    ("/companion-posts/{postId}/comments", "post"): "createCompanionComment",
    ("/companion-posts/{postId}/messages", "post"): "sendCompanionMessage",
    ("/companion-posts/{postId}/messages/read", "put"): "markCompanionMessagesRead",
    ("/reports", "post"): "createReport",
    ("/viewing-plans", "post"): "createViewingPlan",
    ("/viewing-plans/{planId}/generate", "post"): "generateViewingPlan",
    ("/admin/games", "post"): "createGame",
    ("/admin/games/unconfirmed", "get"): "listUnconfirmedGames",
    ("/admin/games/{gameId}/revisions", "post"): "reviseGameResult",
    ("/admin/games/{gameId}/revisions", "get"): "listGameRevisions",
    ("/admin/stadiums/{stadiumId}/zones", "get"): "listAdminStadiumZones",
    ("/admin/stadiums/{stadiumId}/zones", "post"): "createStadiumZone",
}


def camel(seg):
    return "".join(w.capitalize() for w in re.split(r"[-_]", seg))


def make_id(method, path):
    """읽어서 뜻이 통하는 이름을 만든다. 경로 변수는 이름에 넣지 않는다."""
    if (path, method) in OVERRIDE:
        return OVERRIDE[(path, method)]
    segs = [s for s in path.strip("/").split("/") if s]
    words = [camel(s) for s in segs if not s.startswith("{")]
    tail_is_param = segs[-1].startswith("{")
    if method == "get":
        verb = "get" if tail_is_param else "list"
        # /users/me 처럼 단수 대상이면 list 가 어색하다
        if not tail_is_param and segs[-1] in ("me", "summary", "providers", "authorize-url"):
            verb = "get"
    else:
        verb = {"post": "create", "put": "replace", "patch": "update", "delete": "delete"}[method]
    if verb in ("get", "update", "delete", "replace") and words and words[-1].endswith("s"):
        words[-1] = words[-1][:-1] if tail_is_param else words[-1]
    return verb + "".join(words)


def needed(method, path, op, global_sec):
    """이 오퍼레이션에 있어야 하는 오류 응답 코드"""
    have = set(op.get("responses", {}))
    want = set()
    sec = op.get("security", global_sec)
    if sec:
        want.add("401")
    if "{" in path:
        want.add("404")
    if method in ("post", "put", "patch"):
        want.add("400")
    return sorted(want - have)


def main():
    text = io.open(SPEC, encoding="utf-8").read()
    spec = yaml.safe_load(text)
    gsec = spec.get("security")

    ids, dup = {}, []
    for path, item in spec["paths"].items():
        for m in item:
            if m in METHODS:
                oid = make_id(m, path)
                if oid in ids:
                    dup.append(oid)
                ids[(path, m)] = oid
    if dup:
        sys.exit("operationId 중복: %s" % dup)

    lines = text.split("\n")
    out, path, method, changed_id, changed_res = [], None, None, 0, 0
    i = 0
    while i < len(lines):
        ln = lines[i]
        m_path = re.match(r"^  (/\S*):\s*$", ln)
        m_meth = re.match(r"^    (%s):\s*$" % "|".join(METHODS), ln)
        if m_path:
            path, method = m_path.group(1), None
        elif m_meth and path:
            method = m_meth.group(1)
            out.append(ln)
            i += 1
            if lines[i].startswith("      operationId:"):
                i += 1                                  # 옛 값은 버리고 다시 적는다
            out.append("      operationId: %s" % ids[(path, method)])
            changed_id += 1
            continue
        elif ln == "      responses:" and path and method:
            op = spec["paths"][path][method]
            add = needed(method, path, op, gsec)
            out.append(ln)
            i += 1
            # 이 오퍼레이션의 responses 블록 끝을 찾는다
            while i < len(lines) and (lines[i].startswith("        ") or not lines[i].strip()):
                out.append(lines[i])
                i += 1
            while out and not out[-1].strip():
                lines.insert(i, out.pop())          # 뒤따르던 빈 줄은 되돌린다
            for code in add:
                out.append("        '%s':" % code)
                out.append("          $ref: '#/components/responses/%s'" % REF[code])
                changed_res += 1
            continue
        out.append(ln)
        i += 1

    io.open(SPEC, "w", encoding="utf-8").write("\n".join(out))
    print("operationId %d개 · 오류 응답 %d개 추가" % (changed_id, changed_res))


if __name__ == "__main__":
    main()
