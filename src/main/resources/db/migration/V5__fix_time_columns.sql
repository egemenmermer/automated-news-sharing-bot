-- Convert fetch_time and post_time from TIME to TIMESTAMP
ALTER TABLE bots 
    ALTER COLUMN fetch_time TYPE TIMESTAMP USING fetch_time::timestamp,
    ALTER COLUMN post_time TYPE TIMESTAMP USING post_time::timestamp; 