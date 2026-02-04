package com.example.mock.parser.config;

import com.example.mock.parser.entity.UserEntity;
import com.example.mock.parser.repository.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Optional;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    public static final String SESSION_USER_ID = "AUTH_USER_ID";

    private final UserRepository userRepository;

    public AuthInterceptor(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String path = request.getRequestURI();
        if (isPublicPath(path)) {
            return true;
        }
        UserEntity user = getSessionUser(request.getSession(false));
        if (user == null) {
            if (path != null && path.endsWith(".html")) {
                response.setStatus(HttpServletResponse.SC_FOUND);
                response.setHeader("Location", "/login.html");
            } else {
                sendUnauthorized(response, "未登录");
            }
            return false;
        }
        if ((path.startsWith("/admin") || path.startsWith("/auth/users")) && !UserEntity.ROLE_ADMIN.equals(user.getRole())) {
            sendForbidden(response, "仅管理员可访问");
            return false;
        }
        return true;
    }

    private boolean isPublicPath(String path) {
        if (path == null) {
            return true;
        }
        if (path.startsWith("/auth/login") || path.startsWith("/auth/register")) {
            return true;
        }
        if (path.startsWith("/assets") || path.startsWith("/logo.png")) {
            return true;
        }
        if (path.endsWith("/login.html") || path.endsWith("login.html")) {
            return true;
        }
        if (path.endsWith("admin.html")) {
            return false;
        }
        if (path.endsWith(".html")) {
            return true;
        }
        return false;
    }

    private UserEntity getSessionUser(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object id = session.getAttribute(SESSION_USER_ID);
        if (!(id instanceof Long)) {
            return null;
        }
        Optional<UserEntity> userOpt = userRepository.findById((Long) id);
        return userOpt.orElse(null);
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\":\"unauthorized\",\"message\":\"" + message + "\"}");
    }

    private void sendForbidden(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\":\"forbidden\",\"message\":\"" + message + "\"}");
    }
}
