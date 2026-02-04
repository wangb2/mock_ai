package com.example.mock.parser.config;

import com.example.mock.parser.entity.UserEntity;
import com.example.mock.parser.repository.UserRepository;
import com.example.mock.parser.service.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Optional;

@Component
public class AdminBootstrap {
    private static final Logger logger = LoggerFactory.getLogger(AdminBootstrap.class);
    private static final String DEFAULT_ADMIN_USER = "admin";
    private static final String DEFAULT_ADMIN_PASS = "Admin@123";

    private final UserRepository userRepository;

    public AdminBootstrap(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostConstruct
    public void init() {
        Optional<UserEntity> existing = userRepository.findByUsername(DEFAULT_ADMIN_USER);
        if (existing.isPresent()) {
            return;
        }
        UserEntity admin = new UserEntity();
        admin.setUsername(DEFAULT_ADMIN_USER);
        admin.setPasswordHash(PasswordUtil.hashPassword(DEFAULT_ADMIN_PASS));
        admin.setRole(UserEntity.ROLE_ADMIN);
        admin.setStatus(UserEntity.STATUS_APPROVED);
        userRepository.save(admin);
        logger.info("Default admin created: {} / {}", DEFAULT_ADMIN_USER, DEFAULT_ADMIN_PASS);
    }
}
