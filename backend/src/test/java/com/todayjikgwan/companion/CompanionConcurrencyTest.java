package com.todayjikgwan.companion;

import static org.assertj.core.api.Assertions.assertThat;

import com.todayjikgwan.common.exception.ApiException;
import com.todayjikgwan.domain.companion.*;
import com.todayjikgwan.domain.game.Game;
import com.todayjikgwan.domain.game.GameRepository;
import com.todayjikgwan.domain.user.User;
import com.todayjikgwan.domain.user.UserRepository;
import com.todayjikgwan.service.CompanionService;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * REQ-F-504 / REQ-N-011 동시성 검증.
 *
 * 여러 사용자가 같은 순간에 참여를 눌러도 확정 인원이 정원을 넘지 않아야 한다.
 */
@SpringBootTest
class CompanionConcurrencyTest {

    @Autowired CompanionService companionService;
    @Autowired CompanionPostRepository postRepository;
    @Autowired CompanionApplicationRepository applicationRepository;
    @Autowired UserRepository userRepository;
    @Autowired GameRepository gameRepository;

    @Test
    @DisplayName("정원 4명 모집에 30명이 동시에 참여해도 확정은 정확히 4명이다")
    void concurrentApplyNeverExceedsCapacity() throws Exception {
        int capacity = 4;          // 작성자 1명 포함
        int challengers = 30;

        Game game = futureGame();
        User author = createUser("author");
        Long postId = postRepository.save(
                new CompanionPost(game, author, capacity, "동시성 테스트")).getId();

        List<User> users = new ArrayList<>();
        for (int i = 0; i < challengers; i++) {
            users.add(createUser("racer" + i));
        }

        ExecutorService pool = Executors.newFixedThreadPool(16);
        CountDownLatch ready = new CountDownLatch(challengers);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(challengers);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        List<Integer> seqs = new CopyOnWriteArrayList<>();

        for (User u : users) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();                       // 동시에 출발
                    var res = companionService.apply(postId, u.getId());
                    seqs.add(res.seq());
                    success.incrementAndGet();
                } catch (ApiException e) {
                    rejected.incrementAndGet();
                } catch (Exception e) {
                    rejected.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }
        ready.await(10, TimeUnit.SECONDS);
        start.countDown();
        done.await(60, TimeUnit.SECONDS);
        pool.shutdown();

        CompanionPost post = postRepository.findById(postId).orElseThrow();
        long confirmedRows = applicationRepository
                .countByCompanionPostIdAndStatus(postId, ApplicationStatus.CONFIRMED);

        System.out.printf("%n[동시성 결과] 도전 %d명 → 성공 %d / 거절 %d%n",
                challengers, success.get(), rejected.get());
        System.out.printf("[확정 순번] %s%n", seqs.stream().sorted().toList());
        System.out.printf("[게시글] confirmedCount=%d capacity=%d status=%s%n",
                post.getConfirmedCount(), post.getCapacity(), post.getStatus());

        // 작성자 1 + 확정 3 = 정원 4
        assertThat(post.getConfirmedCount()).isEqualTo(capacity);
        assertThat(post.getConfirmedCount()).isLessThanOrEqualTo(post.getCapacity());
        assertThat(success.get()).isEqualTo(capacity - 1);
        assertThat(confirmedRows).isEqualTo(capacity - 1);
        assertThat(post.getStatus()).isEqualTo(PostStatus.FULL);
        // 순번은 중복 없이 2,3,4 로 부여되어야 한다
        assertThat(seqs.stream().sorted().toList()).containsExactly(2, 3, 4);
    }

    private Game futureGame() {
        return gameRepository.findAll().stream()
                .filter(g -> g.getStartAt().isAfter(OffsetDateTime.now().plusDays(2)))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("미래 경기 시드 데이터가 필요합니다"));
    }

    private User createUser(String tag) {
        String unique = tag + "-" + System.nanoTime();
        return userRepository.save(User.builder()
                .email(unique + "@test.local")
                .passwordHash("x")
                .nickname(unique)
                .build());
    }
}
