package com.leveluparcade;

import com.leveluparcade.llm.OpenRouterProperties;
import com.leveluparcade.security.PasswordResetProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({OpenRouterProperties.class, PasswordResetProperties.class})
public class LevelupArcadeApplication {

    public static void main(String[] args) {
        SpringApplication.run(LevelupArcadeApplication.class, args);
    }
}