package com.egemen.TweetBotTelegram;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(properties = {
    "spring.main.allow-bean-definition-overriding=true",
    "spring.flyway.enabled=false"
})
class TweetBotTelegramApplicationTests {
    
    @Test
    void contextLoads() {
        // Empty test that will pass
        assert true;
    }
}
