package com.seng.comfy.backend.config;

import com.seng.comfy.backend.entity.UserAuth;
import com.seng.comfy.backend.repository.UserAuthRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.context.annotation.Bean;

import java.util.Optional;

@Configuration
public class AdditionalWebSecurityConfig {

    @Bean
    public UserDetailsService userDetailsService(UserAuthRepository userAuthRepository) {
        return username -> {
            Optional<UserAuth> userAuth = userAuthRepository.findByEmail(username);
            if (userAuth.isPresent()) return (UserDetails) userAuth.get();

            userAuth = userAuthRepository.findByUsername(username);
            if (userAuth.isPresent()) return (UserDetails) userAuth.get();

            throw new UsernameNotFoundException("User not found");
        };
    }
}
