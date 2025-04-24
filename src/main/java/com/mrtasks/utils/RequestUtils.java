package com.mrtasks.utils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.util.StringUtils;

@Log4j2
public class RequestUtils {

    /**
     * Retrieves the client's IP address from the request.
     * It first checks the "X-Forwarded-For" header, which is commonly used in proxy setups.
     * If that header is not present, it falls back to the remote address of the request.
     *
     * @param request The HttpServletRequest object containing the request information.
     * @return The client's IP address as a String.
     */
    public static String getClientIp(HttpServletRequest request) {
        String remoteAddr = request.getHeader("X-Forwarded-For");

        if (StringUtils.hasText(remoteAddr)) {
            remoteAddr = remoteAddr.split(",")[0].trim();
        } else {
            remoteAddr = request.getRemoteAddr();
            if ("127.0.0.1".equals(remoteAddr)) {
                log.warn("X-Forwarded-For header missing; falling back to local IP: {}", remoteAddr);
            }
        }

        return remoteAddr;
    }

}
