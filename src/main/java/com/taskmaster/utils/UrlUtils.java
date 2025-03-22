package com.taskmaster.utils;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

public class UrlUtils {

    /**
     * Generates the base URL based on the current request context.
     */
    public static String getBaseUrl() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new RuntimeException("Cannot create base url because host is missing");
        }

        HttpServletRequest request = attributes.getRequest();
        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        String contextPath = request.getContextPath();

        return String.format("%s://%s%s%s",
                scheme,
                host,
                (port == 80 || port == 443) ? "" : ":" + port,
                contextPath.isEmpty() ? "" : contextPath
        );
    }
}