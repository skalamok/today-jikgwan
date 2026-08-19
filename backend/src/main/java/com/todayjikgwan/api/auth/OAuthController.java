package com.todayjikgwan.api.auth;

import com.todayjikgwan.common.exception.ApiException;
import com.todayjikgwan.common.exception.ErrorCode;
import com.todayjikgwan.domain.user.OAuthProvider;
import com.todayjikgwan.service.OAuthService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** REQ-F-003 소셜 로그인 */
@RestController
@RequestMapping("/api/v1/auth/oauth")
@RequiredArgsConstructor
public class OAuthController {

    private final OAuthService oauthService;

    /** 키가 등록된 제공자만 내려준다. 화면은 이 목록대로 버튼을 그린다. */
    @GetMapping("/providers")
    public List<Map<String, Object>> providers() {
        return oauthService.enabledProviders();
    }

    @GetMapping("/{provider}/authorize-url")
    public Map<String, String> authorizeUrl(@PathVariable String provider) {
        return oauthService.authorizeUrl(parse(provider));
    }

    @PostMapping("/{provider}/callback")
    public Map<String, Object> callback(@PathVariable String provider,
                                        @RequestBody CallbackRequest request) {
        return oauthService.callback(parse(provider), request.code(), request.state());
    }

    private OAuthProvider parse(String value) {
        OAuthProvider provider = OAuthProvider.from(value);
        if (provider == null) {
            throw new ApiException(ErrorCode.NOT_FOUND);
        }
        return provider;
    }

    public record CallbackRequest(String code, String state) { }
}
