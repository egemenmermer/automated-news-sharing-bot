package com.egemen.TweetBotTelegram.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Slf4j
@Configuration
public class DatabaseConfig {

    @Bean
    @Primary
    public DataSource dataSource() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        
        String dbUrl = dotenv.get("DB_URL");
        String dbUsername = dotenv.get("DB_USERNAME");
        String dbPassword = dotenv.get("DB_PASSWORD");
        
        log.info("Configuring database connection with URL: {}", dbUrl);
        
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUsername);
        config.setPassword(dbPassword);
        config.setDriverClassName("org.postgresql.Driver");
        
        // Important: Set autoCommit to false
        config.setAutoCommit(false);
        
        // Other HikariCP settings
        config.setConnectionTimeout(20000);
        config.setMinimumIdle(5);
        config.setMaximumPoolSize(15);
        config.setIdleTimeout(300000);
        config.setMaxLifetime(600000);
        config.setLeakDetectionThreshold(60000);
        config.setPoolName("TweetBotHikariPool");
        
        // Add PostgreSQL specific properties
        config.addDataSourceProperty("reWriteBatchedInserts", "true");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        
        HikariDataSource dataSource = new HikariDataSource(config);
        
        log.info("Database connection configured with HikariCP");
        return dataSource;
    }
}