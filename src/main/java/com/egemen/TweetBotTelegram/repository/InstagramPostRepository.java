package com.egemen.TweetBotTelegram.repository;

import com.egemen.TweetBotTelegram.entity.Bot;
import com.egemen.TweetBotTelegram.entity.InstagramPost;
import com.egemen.TweetBotTelegram.enums.PostStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InstagramPostRepository extends JpaRepository<InstagramPost, Long> {
    
    List<InstagramPost> findByPostStatus(PostStatus status);
    
    List<InstagramPost> findByBotAndPostStatus(Bot bot, PostStatus status);
    
    @Query("SELECT p FROM InstagramPost p WHERE p.bot = ?1 AND p.postStatus = ?2 ORDER BY p.createdAt DESC")
    List<InstagramPost> findRecentPosts(Bot bot, PostStatus status, int limit);
    
    @Query(value = "SELECT * FROM instagram_posts WHERE bot_id = ?1 AND post_status = ?2 ORDER BY created_at DESC LIMIT ?3", 
           nativeQuery = true)
    List<InstagramPost> findRecentPostsNative(Long botId, String status, int limit);
    
    // Count posts by status
    long countByPostStatus(PostStatus status);
    
    // Count posts by bot and status
    long countByBotAndPostStatus(Bot bot, PostStatus status);
}
