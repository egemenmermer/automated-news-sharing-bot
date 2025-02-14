package com.egemen.TweetBotTelegram.repository;

import com.egemen.TweetBotTelegram.entity.InstagramPost;
import com.egemen.TweetBotTelegram.enums.PostStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InstagramPostRepository extends JpaRepository<InstagramPost, Long> {
    List<InstagramPost> findByStatus(PostStatus status);
    List<InstagramPost> findByStatusAndRetryCountLessThan(PostStatus status, int maxRetries);
    List<InstagramPost> findByNewsId(Long newsId);
    @Query("SELECT p FROM InstagramPost p WHERE p.status = :status AND p.retryCount < :maxRetries")
    List<InstagramPost> findRetryablePosts(@Param("status") PostStatus status, @Param("maxRetries") int maxRetries);
    
    // Find failed posts that can be retried
    default List<InstagramPost> findFailedRetryablePosts(int maxRetries) {
        return findByStatusAndRetryCountLessThan(PostStatus.FAILED, maxRetries);
    }
    
    // Find pending posts
    default List<InstagramPost> findPendingPosts() {
        return findByStatus(PostStatus.PENDING);
    }
    
    // Count pending posts
    @Query("SELECT COUNT(p) FROM InstagramPost p WHERE p.status = 'PENDING'")
    int countPendingPosts();
}