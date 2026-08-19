package com.todayjikgwan.domain.user;

/**
 * 지원하는 소셜 로그인 제공자.
 *
 * <p>제공자마다 엔드포인트와 응답 형태가 달라, 그 차이를 여기에 모아 둔다.
 * 프로필 파싱만 {@code OAuthService} 가 담당한다.
 */
public enum OAuthProvider {

    GOOGLE("구글",
            "https://accounts.google.com/o/oauth2/v2/auth",
            "https://oauth2.googleapis.com/token",
            "https://www.googleapis.com/oauth2/v3/userinfo",
            "openid email profile"),

    NAVER("네이버",
            "https://nid.naver.com/oauth2.0/authorize",
            "https://nid.naver.com/oauth2.0/token",
            "https://openapi.naver.com/v1/nid/me",
            null),

    /** 이메일은 비즈앱 심사를 통과해야 내려온다. 없을 수 있다고 보고 다룬다. */
    KAKAO("카카오",
            "https://kauth.kakao.com/oauth/authorize",
            "https://kauth.kakao.com/oauth/token",
            "https://kapi.kakao.com/v2/user/me",
            "profile_nickname account_email");

    private final String displayName;
    private final String authorizeUrl;
    private final String tokenUrl;
    private final String profileUrl;
    private final String scope;

    OAuthProvider(String displayName, String authorizeUrl, String tokenUrl, String profileUrl, String scope) {
        this.displayName = displayName;
        this.authorizeUrl = authorizeUrl;
        this.tokenUrl = tokenUrl;
        this.profileUrl = profileUrl;
        this.scope = scope;
    }

    public String displayName() { return displayName; }
    public String authorizeUrl() { return authorizeUrl; }
    public String tokenUrl() { return tokenUrl; }
    public String profileUrl() { return profileUrl; }
    public String scope() { return scope; }

    public static OAuthProvider from(String value) {
        for (OAuthProvider p : values()) {
            if (p.name().equalsIgnoreCase(value)) {
                return p;
            }
        }
        return null;
    }
}
