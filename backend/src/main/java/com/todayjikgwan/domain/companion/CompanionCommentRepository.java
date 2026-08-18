package com.todayjikgwan.domain.companion;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanionCommentRepository extends JpaRepository<CompanionComment, Long> {

    @Query("select c from CompanionComment c join fetch c.user "
            + "where c.companionPost.id = :postId and c.deletedAt is null "
            + "order by c.createdAt")
    List<CompanionComment> findByPost(@Param("postId") Long postId);
}
