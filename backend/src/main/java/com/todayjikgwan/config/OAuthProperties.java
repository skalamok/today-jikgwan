package com.todayjikgwan.config;

import com.todayjikgwan.domain.user.OAuthProvider;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 소셜 로그인 키. 키를 넣지 않은 제공자는 목록에서 빠지고 화면에도 버튼이 뜨지 않는다.
 * 셋 다 발급받지 않은 상태에서도 앱이 그대로 동작하게 하기 위해서다.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "todayjikgwan.oauth")
public class OAuthProperties {

    /** 인증 후 브라우저가 돌아올 프런트엔드 주소. {provider} 가 치환된다. */
    private String redirectUri = "http://localhost:5173/oauth/{provider}";

    private Map<String, Registration> providers = new LinkedHashMap<>();

    @Getter
    @Setter
    public static class Registration {
        private String clientId;
        private String clientSecret;
    }

    public Registration find(OAuthProvider provider) {
        Registration r = providers.get(provider.name().toLowerCase());
        if (r == null || r.getClientId() == null || r.getClientId().isBlank()) {
            return null;
        }
        return r;
    }

    public boolean isEnabled(OAuthProvider provider) {
        return find(provider) != null;
    }

    public String redirectUriFor(OAuthProvider provider) {
        return redirectUri.replace("{provider}", provider.name().toLowerCase());
    }
}
