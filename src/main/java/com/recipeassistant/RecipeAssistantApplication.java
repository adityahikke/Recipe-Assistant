package com.recipeassistant;

import com.recipeassistant.util.DotEnvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

import java.nio.file.Path;

@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
public class RecipeAssistantApplication {

    public static void main(String[] args) {
        DotEnvLoader.load(Path.of(".env"));
        if (System.getenv("SPRING_PROFILES_ACTIVE") == null
                && System.getProperty("SPRING_PROFILES_ACTIVE") == null
                && (System.getenv("RAILWAY_ENVIRONMENT") != null || System.getenv("RAILWAY_SERVICE_ID") != null)) {
            System.setProperty("spring.profiles.active", "prod");
        }
        SpringApplication.run(RecipeAssistantApplication.class, args);
    }
}
