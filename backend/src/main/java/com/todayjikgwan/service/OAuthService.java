package com.todayjikgwan.service;

import com.todayjikgwan.api.auth.dto.TokenResponse;
import com.todayjikgwan.common.exception.ApiException;
import com.todayjikgwan.common.exception.ErrorCode;
import com.todayjikgwan.config.OAuthProperties;
import com.todayjikgwan.domain.user.*;
import com.todayjikgwan.security.JwtProvider;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * REQ-F-003 소셜 로그인. Authorization Code 방식이고, 코드 교환은 클라이언트 시크릿이
 * 필요하므로 전부 서버에서 한다.
 *
 * <p><b>계정 식별</b><br>
 * 이메일이 아니라 {@code (provider, providerUserId)} 로 찾는다. 카카오는 이메일 제공이
 * 비즈앱 심사 대상이라 내려오지 않을 수 있고, 이메일은 제공자 쪽에서 바뀔 수도 있기 때문이다.
 * 이메일이 함께 오고 그 이메일로 가입한 계정이 이미 있으면 그 계정에 연동한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final OAuthProperties properties;
    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final JwtProvider jwtProvider;
    private final RestClient restClient = RestClient.create();

    /** 화면에 어떤 버튼을 보여줄지 결정하는 데 쓴다. */
    public List<Map<String, Object>> enabledProviders() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (OAuthProvider p : OAuthProvider.values()) {
            if (properties.isEnabled(p)) {
                list.add(Map.of("provider", p.name().toLowerCase(), "displayName", p.displayName()));
            }
        }
        return list;
    }

    public Map<String, String> authorizeUrl(OAuthProvider provider) {
        OAuthProperties.Registration reg = registration(provider);
        String state = randomState();

        StringBuilder url = new StringBuilder(provider.authorizeUrl())
                .append("?response_type=code")
                .append("&client_id=").append(encode(reg.getClientId()))
                .append("&redirect_uri=").append(encode(properties.redirectUriFor(provider)))
                .append("&state=").append(encode(state));
        if (provider.scope() != null) {
            url.append("&scope=").append(encode(provider.scope()));
        }
        return Map.of("authorizeUrl", url.toString(), "state", state);
    }

    @Transactional
    public Map<String, Object> callback(OAuthProvider provider, String code, String state) {
        OAuthProperties.Registration reg = registration(provider);
        String accessToken = exchangeCode(provider, reg, code, state);
        Profile profile = fetchProfile(provider, accessToken);

        Optional<SocialAccount> linked =
                socialAccountRepository.findByProviderAndProviderUserId(provider, profile.id());

        User user;
        boolean created = false;
        if (linked.isPresent()) {
            user = linked.get().getUser();
        } else {
            user = profile.email() == null ? null
                    : userRepository.findByEmail(profile.email()).orElse(null);
            if (user == null) {
                user = userRepository.save(User.builder()
                        .email(profile.email())
                        .nickname(uniqueNickname(profile.nickname(), provider))
                        .build());
                created = true;
            }
            socialAccountRepository.save(
                    SocialAccount.link(user, provider, profile.id(), profile.email()));
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }

        String jwt = jwtProvider.createAccessToken(user.getId(), user.getRole().name());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accessToken", jwt);
        result.put("expiresIn", jwtProvider.getAccessTokenSeconds());
        result.put("newAccount", created);
        result.put("nickname", user.getNickname());
        result.put("needsFavoriteTeam", user.getFavoriteTeam() == null);
        return result;
    }

    // ── 제공자 호출 ─────────────────────────────────────────────

    private String exchangeCode(OAuthProvider provider, OAuthProperties.Registration reg,
                                String code, String state) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", reg.getClientId());
        form.add("code", code);
        form.add("redirect_uri", properties.redirectUriFor(provider));
        if (reg.getClientSecret() != null && !reg.getClientSecret().isBlank()) {
            form.add("client_secret", reg.getClientSecret());
        }
        if (state != null) {
            form.add("state", state);
        }

        Map<?, ?> body;
        try {
            body = restClient.post()
                    .uri(provider.tokenUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);
        } catch (RuntimeException e) {
            log.warn("{} 토큰 교환 실패", provider, e);
            throw new ApiException(ErrorCode.OAUTH_FAILED);
        }
        Object token = body == null ? null : body.get("access_token");
        if (token == null) {
            log.warn("{} 토큰 응답에 access_token 없음: {}", provider, body);
            throw new ApiException(ErrorCode.OAUTH_FAILED);
        }
        return token.toString();
    }

    private Profile fetchProfile(OAuthProvider provider, String accessToken) {
        Map<?, ?> body;
        try {
            body = restClient.get()
                    .uri(provider.profileUrl())
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);
        } catch (RuntimeException e) {
            log.warn("{} 프로필 조회 실패", provider, e);
            throw new ApiException(ErrorCode.OAUTH_FAILED);
        }
        if (body == null) {
            throw new ApiException(ErrorCode.OAUTH_FAILED);
        }

        return switch (provider) {
            case GOOGLE -> new Profile(
                    str(body.get("sub")), str(body.get("email")), str(body.get("name")));
            case NAVER -> {
                Map<?, ?> res = asMap(body.get("response"));
                yield new Profile(
                        str(res.get("id")), str(res.get("email")), str(res.get("nickname")));
            }
            case KAKAO -> {
                Map<?, ?> account = asMap(body.get("kakao_account"));
                Map<?, ?> kakaoProfile = asMap(account.get("profile"));
                yield new Profile(
                        str(body.get("id")), str(account.get("email")), str(kakaoProfile.get("nickname")));
            }
        };
    }

    private record Profile(String id, String email, String nickname) { }

    // ── 보조 ───────────────────────────────────────────────────

    private OAuthProperties.Registration registration(OAuthProvider provider) {
        OAuthProperties.Registration reg = properties.find(provider);
        if (reg == null) {
            throw new ApiException(ErrorCode.OAUTH_PROVIDER_DISABLED);
        }
        return reg;
    }

    /**
     * 제공자가 준 닉네임을 그대로 쓰되, 이미 쓰는 사람이 있으면 뒤에 숫자를 붙인다.
     * 닉네임은 서비스 안에서 유일해야 하는데 제공자 쪽 닉네임은 그 보장이 없다.
     */
    private String uniqueNickname(String raw, OAuthProvider provider) {
        String base = (raw == null || raw.isBlank()) ? provider.displayName() + "직관러" : raw.trim();
        if (base.length() > 20) {
            base = base.substring(0, 20);
        }
        if (!userRepository.existsByNickname(base)) {
            return base;
        }
        for (int i = 2; i < 1000; i++) {
            String candidate = base + i;
            if (!userRepository.existsByNickname(candidate)) {
                return candidate;
            }
        }
        throw new ApiException(ErrorCode.DUPLICATE_NICKNAME);
    }

    private static String randomState() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String encode(String v) {
        return URLEncoder.encode(v, StandardCharsets.UTF_8);
    }

    private static String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private static Map<?, ?> asMap(Object v) {
        return v instanceof Map<?, ?> m ? m : Map.of();
    }
}
