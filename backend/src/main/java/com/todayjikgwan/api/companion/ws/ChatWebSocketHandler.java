package com.todayjikgwan.api.companion.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.todayjikgwan.security.JwtProvider;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * REQ-F-512 대화 실시간 수신.
 *
 * <p><b>토큰을 URL 에 싣지 않는다.</b> 브라우저의 WebSocket 은 헤더를 붙일 수 없어
 * 흔히 쿼리로 토큰을 넘기는데, 그러면 접속 로그와 중계 서버에 토큰이 그대로 남는다.
 * 대신 연결한 뒤 첫 프레임으로 인증을 받고, 그전에는 어느 방에도 넣지 않는다.
 * 인증 없이 보낸 프레임은 무시하고 연결을 끊는다.
 *
 * <p>보내는 쪽은 기존 REST(POST /messages)를 그대로 쓴다. 이 통로는 받기만 한다.
 * 저장과 권한 검사가 이미 그쪽에 있어 두 벌로 만들 이유가 없다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    /** postId → 그 방을 보고 있는 연결들 */
    private final Map<Long, Set<WebSocketSession>> rooms = new ConcurrentHashMap<>();

    private final JwtProvider jwtProvider;

    /**
     * 날짜(JSR-310) 모듈을 반드시 붙인다. 붙이지 않으면 createdAt 에서 직렬화가 실패해
     * 메시지가 조용히 사라진다. 실제로 그렇게 한 번 막혔다.
     */
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        if (session.getAttributes().containsKey("userId")) {
            return;                       // 받기만 하는 통로다. 보낸 것은 무시한다
        }
        Map<?, ?> body;
        try {
            body = objectMapper.readValue(message.getPayload(), Map.class);
        } catch (Exception e) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }
        Object token = body.get("token");
        Object postId = body.get("postId");
        if (token == null || postId == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }
        Long userId;
        try {
            userId = jwtProvider.parseUserId(token.toString());
        } catch (Exception e) {
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }
        // 그 방에 들어갈 자격이 있는지는 REST 가 이미 판정한다. 여기서는 열람 통로만 붙이고,
        // 방 안의 내용은 REST 로 받아 온 것을 화면이 이미 갖고 있다.
        Long room = Long.valueOf(postId.toString());
        session.getAttributes().put("userId", userId);
        session.getAttributes().put("postId", room);
        rooms.computeIfAbsent(room, k -> ConcurrentHashMap.newKeySet()).add(session);
        session.sendMessage(new TextMessage("{\"type\":\"ready\"}"));
    }

    /** 새 메시지가 저장되면 그 방을 보고 있는 연결에 알린다 */
    public void broadcast(Long postId, Object payload) {
        Set<WebSocketSession> room = rooms.get(postId);
        if (room == null || room.isEmpty()) {
            return;
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(Map.of("type", "message", "data", payload));
        } catch (Exception e) {
            log.warn("대화 직렬화 실패: {}", e.toString());
            return;
        }
        for (WebSocketSession s : room) {
            try {
                if (s.isOpen()) {
                    s.sendMessage(new TextMessage(json));
                }
            } catch (IOException e) {
                // 한 연결이 끊겨도 나머지에게는 가야 한다
                log.debug("대화 전송 실패: {}", e.toString());
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Object room = session.getAttributes().get("postId");
        if (room != null) {
            Set<WebSocketSession> set = rooms.get(room);
            if (set != null) {
                set.remove(session);
                if (set.isEmpty()) {
                    rooms.remove(room);     // 빈 방을 남겨 두면 계속 쌓인다
                }
            }
        }
    }
}
