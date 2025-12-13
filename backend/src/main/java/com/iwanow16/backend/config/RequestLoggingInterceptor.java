package com.iwanow16.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Enumeration;

/**
 * Перехватчик для логирования всех HTTP запросов и ответов
 */
@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {
    
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingInterceptor.class);
    private static final String REQUEST_START_TIME = "requestStartTime";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Сохраняем время начала запроса
        request.setAttribute(REQUEST_START_TIME, System.currentTimeMillis());
        
        String clientIp = getClientIp(request);
        String method = request.getMethod();
        String requestURI = request.getRequestURI();
        String queryString = request.getQueryString();
        
        log.info("▶ Incoming Request | IP: {} | Method: {} | URI: {}{}", 
                clientIp, 
                method, 
                requestURI,
                queryString != null ? "?" + queryString : "");
        
        // Логируем заголовки на DEBUG уровне
        if (log.isDebugEnabled()) {
            log.debug("  Headers:");
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                String headerValue = request.getHeader(headerName);
                // Не логируем чувствительные заголовки
                if (!isSensitiveHeader(headerName)) {
                    log.debug("    {}: {}", headerName, headerValue);
                }
            }
        }
        
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        long startTime = (long) request.getAttribute(REQUEST_START_TIME);
        long duration = System.currentTimeMillis() - startTime;
        
        String clientIp = getClientIp(request);
        String method = request.getMethod();
        String requestURI = request.getRequestURI();
        int status = response.getStatus();
        
        // Выбираем уровень логирования в зависимости от статуса
        if (status >= 500) {
            log.error("◀ Response | IP: {} | Method: {} | URI: {} | Status: {} | Duration: {}ms", 
                    clientIp, method, requestURI, status, duration);
        } else if (status >= 400) {
            log.warn("◀ Response | IP: {} | Method: {} | URI: {} | Status: {} | Duration: {}ms", 
                    clientIp, method, requestURI, status, duration);
        } else {
            log.info("◀ Response | IP: {} | Method: {} | URI: {} | Status: {} | Duration: {}ms", 
                    clientIp, method, requestURI, status, duration);
        }
        
        // Логируем исключение если оно было
        if (ex != null) {
            log.error("Request failed with exception", ex);
        }
    }

    /**
     * Получить IP адрес клиента (с учётом прокси)
     */
    private String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            return forwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * Проверить, является ли заголовок чувствительным
     */
    private boolean isSensitiveHeader(String headerName) {
        String lower = headerName.toLowerCase();
        return lower.contains("authorization") || 
               lower.contains("cookie") || 
               lower.contains("token") || 
               lower.contains("password") ||
               lower.contains("secret");
    }
}
