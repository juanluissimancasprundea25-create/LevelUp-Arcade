package com.leveluparcade;

import com.leveluparcade.llm.OpenRouterProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(OpenRouterProperties.class)
public class LevelupArcadeApplication {

    public static void main(String[] args) {
        SpringApplication.run(LevelupArcadeApplication.class, args);
    }
}