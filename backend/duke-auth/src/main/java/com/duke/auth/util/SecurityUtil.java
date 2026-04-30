package com.duke.auth.util;

import com.duke.auth.security.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Security 工具类
 */
public final class SecurityUtil {

    private SecurityUtil() {}

    public static LoginUser getLoginUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser;
        }
        return null;
    }

    public static Long getCurrentUserId() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getUserId() : null;
    }

    public static String getCurrentUsername() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getUsername() : null;
    }
}
