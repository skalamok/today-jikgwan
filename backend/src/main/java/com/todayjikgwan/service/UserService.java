package com.todayjikgwan.service;

import com.todayjikgwan.common.exception.ApiException;
import com.todayjikgwan.common.exception.ErrorCode;
import com.todayjikgwan.domain.team.Team;
import com.todayjikgwan.domain.team.TeamRepository;
import com.todayjikgwan.domain.user.User;
import com.todayjikgwan.domain.user.SocialAccount;
import com.todayjikgwan.domain.user.SocialAccountRepository;
import com.todayjikgwan.domain.user.UserRepository;
import com.todayjikgwan.domain.user.UserTeamHistory;
import com.todayjikgwan.domain.user.UserTeamHistoryRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final UserTeamHistoryRepository teamHistoryRepository;
    private final SocialAccountRepository socialAccountRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> me(Long userId) {
        return toMap(userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND)));
    }

    /**
     * REQ-F-005. 응원팀을 바꿔도 <b>이미 작성된 기록의 승패 판정은 소급하지 않는다.</b>
     * 기록마다 작성 시점의 응원팀을 보관하기 때문이다. 변경 이력만 남긴다.
     */
    @Transactional
    public Map<String, Object> update(Long userId, String nickname, Long favoriteTeamId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));

        if (nickname != null && !nickname.isBlank() && !nickname.equals(user.getNickname())) {
            if (userRepository.existsByNickname(nickname)) {
                throw new ApiException(ErrorCode.DUPLICATE_NICKNAME);
            }
            user.changeNickname(nickname);
        }
        if (favoriteTeamId != null) {
            Team team = teamRepository.findById(favoriteTeamId)
                    .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
            // 같은 팀을 다시 고른 경우까지 이력으로 남기면 무엇이 바뀐 기록인지 흐려진다
            boolean changed = user.getFavoriteTeam() == null
                    || !user.getFavoriteTeam().getId().equals(team.getId());
            user.changeFavoriteTeam(team);
            if (changed) {
                teamHistoryRepository.save(new UserTeamHistory(user, team));
            }
        }
        return toMap(user);
    }

    private Map<String, Object> toMap(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("email", u.getEmail());
        m.put("nickname", u.getNickname());
        m.put("role", u.getRole().name());
        m.put("favoriteTeamId", u.getFavoriteTeam() == null ? null : u.getFavoriteTeam().getId());
        m.put("favoriteTeam", u.getFavoriteTeam() == null ? null : u.getFavoriteTeam().getName());
        m.put("favoriteTeamShort", u.getFavoriteTeam() == null ? null : u.getFavoriteTeam().getShortName());
        m.put("hasPassword", u.getPasswordHash() != null);
        m.put("socialAccounts", socialAccountRepository.findByUserId(u.getId()).stream()
                .map(a -> a.getProvider().name().toLowerCase()).toList());
        return m;
    }
}
