package com.todayjikgwan.domain.companion;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanionMessageRepository extends JpaRepository<CompanionMessage, Long> {

    /**
     * 전체 조회와 증분 조회를 나눈 이유:
     * 하나의 쿼리에서 "(:after is null or created_at > :after)" 로 처리하면
     * PostgreSQL 이 null 파라미터의 타입을 결정하지 못해 실패한다.
     */
    @Query("select m from CompanionMessage m join fetch m.user "
            + "where m.companionPost.id = :postId order by m.createdAt")
    List<CompanionMessage> findAllByPost(@Param("postId") Long postId);

    /** 주기적 조회(폴링)용 증분 조회 */
    @Query("select m from CompanionMessage m join fetch m.user "
            + "where m.companionPost.id = :postId and m.createdAt > :after "
            + "order by m.createdAt")
    List<CompanionMessage> findByPostAfter(@Param("postId") Long postId,
                                           @Param("after") OffsetDateTime after);

    /** 미읽음 개수. 내가 보낸 메시지는 제외한다 */
    @Query("select count(m) from CompanionMessage m "
            + "where m.companionPost.id = :postId and m.createdAt > :since and m.user.id <> :userId")
    long countUnread(@Param("postId") Long postId, @Param("userId") Long userId,
                     @Param("since") OffsetDateTime since);
}
