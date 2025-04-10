package com.mrtasks.config;

import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Component;

@Component
public class SessionExpirationListener implements HttpSessionListener {

    @Autowired
    private SessionRegistry sessionRegistry;

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        // Optional: Log session creation if needed
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        String sessionId = se.getSession().getId();
        sessionRegistry.removeSessionInformation(sessionId);
    }
}