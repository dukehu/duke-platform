package com.duke.framework.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

public final class IpUtil {

    private IpUtil() {}

    public static String getIpAddr(HttpServletRequest request) {
        String[] headers = {"X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"};
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        String ip = request.getRemoteAddr();
        return "0:0:0:0:0:0:0:1".equals(ip) ? "127.0.0.1" : ip;
    }
}
