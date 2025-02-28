package com.egemen.TweetBotTelegram.repository;

import com.egemen.TweetBotTelegram.entity.Bot;
import com.egemen.TweetBotTelegram.entity.News;
import com.egemen.TweetBotTelegram.entity.Tweet;
import com.egemen.TweetBotTelegram.enums.TweetStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TweetsRepository extends JpaRepository<Tweet, Long> {
    @Query("SELECT COUNT(t) FROM Tweet t")
    int countAllTweets();

    @Query("SELECT t FROM Tweet t WHERE t.scheduledAt > CURRENT_TIMESTAMP")
    List<Tweet> findScheduledTweets();

    @Query("SELECT t FROM PostLogs t WHERE t.postedAt > CURRENT_TIMESTAMP")
    List<Tweet> findPastTweets();

    @Query("SELECT bc FROM Tweet bc WHERE bc.status = :tweetStatus")
    List<Tweet> findByStatus(@Param("tweetStatus") TweetStatus tweetStatus);

    List<Tweet> findByStatusAndBot(TweetStatus status, Bot bot);

    @Query("SELECT n FROM News n JOIN Tweet t ON n.id = t.news.id WHERE t.id = :tweetId")
    Optional<News> findNewsByTweetId(@Param("tweetId") Long tweetId);

}
