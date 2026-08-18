package com.recipeassistant.interceptor;

import com.recipeassistant.service.RateLimitService;
import com.recipeassistant.util.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    
    @Autowired
    private RateLimitService rateLimitService;
    
    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        String clientIp = ClientIpResolver.resolve(request);
        
        if (!rateLimitService.tryConsume(clientIp)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"success\":false,\"error\":\"Too many requests. Please wait a minute and try again.\"}");
            return false;
        }
        
        // Add rate limit headers
        response.setHeader("X-RateLimit-Limit", "10");
        response.setHeader("X-RateLimit-Remaining", String.valueOf(rateLimitService.getAvailableTokens(clientIp)));
        response.setHeader("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() + 60000)); // 1 minute from now
        
        return true;
    }
}
