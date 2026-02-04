package com.example.mock.parser.controller;

import com.example.mock.parser.config.AuthInterceptor;
import com.example.mock.parser.entity.UserEntity;
import com.example.mock.parser.repository.UserRepository;
import com.example.mock.parser.service.PasswordUtil;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> payload, HttpServletRequest request) {
        String username = text(payload.get("username"));
        String password = text(payload.get("password"));
        if (username.isEmpty() || password.isEmpty()) {
            return error("用户名或密码不能为空");
        }
        Optional<UserEntity> userOpt = userRepository.findByUsername(username);
        if (!userOpt.isPresent()) {
            return error("账号不存在");
        }
        UserEntity user = userOpt.get();
        if (!UserEntity.STATUS_APPROVED.equals(user.getStatus())) {
            return error("账号未审批或已拒绝");
        }
        if (!PasswordUtil.verifyPassword(password, user.getPasswordHash())) {
            return error("密码错误");
        }
        HttpSession session = request.getSession(true);
        session.setAttribute(AuthInterceptor.SESSION_USER_ID, user.getId());
        return userInfo(user);
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> payload) {
        String username = text(payload.get("username"));
        String password = text(payload.get("password"));
        if (username.isEmpty() || password.isEmpty()) {
            return error("用户名或密码不能为空");
        }
        if (userRepository.findByUsername(username).isPresent()) {
            return error("用户名已存在");
        }
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setPasswordHash(PasswordUtil.hashPassword(password));
        user.setRole(UserEntity.ROLE_USER);
        user.setStatus(UserEntity.STATUS_PENDING);
        userRepository.save(user);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", true);
        resp.put("message", "注册成功，等待管理员审批");
        return resp;
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", true);
        return resp;
    }

    @GetMapping("/me")
    public Map<String, Object> me(HttpServletRequest request) {
        UserEntity user = getSessionUser(request);
        if (user == null) {
            return error("未登录");
        }
        return userInfo(user);
    }

    @GetMapping("/users")
    public List<Map<String, Object>> listUsers(@RequestParam(value = "status", required = false) String status,
                                               HttpServletRequest request) {
        if (!isAdmin(request)) {
            return Collections.emptyList();
        }
        List<UserEntity> users = userRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (UserEntity user : users) {
            if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase(user.getStatus())) {
                continue;
            }
            result.add(userInfo(user));
        }
        return result;
    }

    @PostMapping("/users/{id}/approve")
    public Map<String, Object> approve(@PathVariable("id") Long id, HttpServletRequest request) {
        if (!isAdmin(request)) {
            return error("仅管理员可访问");
        }
        UserEntity user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return error("用户不存在");
        }
        user.setStatus(UserEntity.STATUS_APPROVED);
        userRepository.save(user);
        return userInfo(user);
    }

    @PostMapping("/users/{id}/reject")
    public Map<String, Object> reject(@PathVariable("id") Long id, HttpServletRequest request) {
        if (!isAdmin(request)) {
            return error("仅管理员可访问");
        }
        UserEntity user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return error("用户不存在");
        }
        user.setStatus(UserEntity.STATUS_REJECTED);
        userRepository.save(user);
        return userInfo(user);
    }

    @PostMapping("/users/{id}/role")
    public Map<String, Object> updateRole(@PathVariable("id") Long id,
                                          @RequestBody Map<String, String> payload,
                                          HttpServletRequest request) {
        if (!isAdmin(request)) {
            return error("仅管理员可访问");
        }
        String role = text(payload.get("role")).toUpperCase(Locale.ROOT);
        if (!UserEntity.ROLE_ADMIN.equals(role) && !UserEntity.ROLE_USER.equals(role)) {
            return error("角色不合法");
        }
        UserEntity user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return error("用户不存在");
        }
        user.setRole(role);
        userRepository.save(user);
        return userInfo(user);
    }

    private boolean isAdmin(HttpServletRequest request) {
        UserEntity user = getSessionUser(request);
        return user != null && UserEntity.ROLE_ADMIN.equals(user.getRole());
    }

    private UserEntity getSessionUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object id = session.getAttribute(AuthInterceptor.SESSION_USER_ID);
        if (!(id instanceof Long)) {
            return null;
        }
        return userRepository.findById((Long) id).orElse(null);
    }

    private Map<String, Object> userInfo(UserEntity user) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", true);
        resp.put("id", user.getId());
        resp.put("username", user.getUsername());
        resp.put("role", user.getRole());
        resp.put("status", user.getStatus());
        return resp;
    }

    private Map<String, Object> error(String message) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", false);
        resp.put("message", message);
        return resp;
    }

    private String text(String value) {
        return value == null ? "" : value.trim();
    }
}
