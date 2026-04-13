package com.postit.infrastructure.config;

import com.postit.application.ports.PostitRepositoryPort;
import com.postit.application.ports.PostitServicePort;
import com.postit.application.ports.UserRepositoryPort;
import com.postit.application.usecases.PostitUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfig {

    @Bean
    public PostitServicePort postitServicePort(
            PostitRepositoryPort repository,
            UserRepositoryPort userRepository) {
        return new PostitUseCase(repository, userRepository);
    }
}
