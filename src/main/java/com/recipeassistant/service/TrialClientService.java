package com.recipeassistant.service;

import com.recipeassistant.model.TrialClientUsage;
import com.recipeassistant.repository.TrialClientUsageRepository;
import com.recipeassistant.util.ClientIpResolver;
import com.recipeassistant.util.ContentHashUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class TrialClientService {

    public static final String CLIENT_COOKIE = "ra_client_id";
    private static final int COOKIE_MAX_AGE_SECONDS = 60 * 60 * 24 * 30;

    private final TrialClientUsageRepository repository;

    @Value("${app.trial.cookie-enabled:true}")
    private boolean cookieTrialEnabled;

    @Value("${server.servlet.session.cookie.secure:false}")
    private boolean secureCookies;

    public TrialClientService(TrialClientUsageRepository repository) {
        this.repository = repository;
    }

    public String ensureClientId(HttpServletRequest request, HttpServletResponse response) {
        if (!cookieTrialEnabled) {
            return "session-" + request.getSession(true).getId();
        }
        String clientId = readCookie(request, CLIENT_COOKIE);
        if (clientId != null && !isValidClientId(clientId)) {
            clientId = null;
        }
        if (clientId == null || clientId.isBlank()) {
            clientId = UUID.randomUUID().toString();
            ResponseCookie cookie = ResponseCookie.from(CLIENT_COOKIE, clientId)
                .path("/")
                .maxAge(COOKIE_MAX_AGE_SECONDS)
                .httpOnly(true)
                .secure(secureCookies)
                .sameSite("Lax")
                .build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        }
        final String resolvedClientId = clientId;
        String ipHash = hashIp(ClientIpResolver.resolve(request));
        repository.findById(resolvedClientId).orElseGet(() -> repository.save(new TrialClientUsage(resolvedClientId, ipHash)));
        return resolvedClientId;
    }

    public int getRecipeCount(String clientId) {
        return repository.findById(clientId).map(TrialClientUsage::getRecipeCount).orElse(0);
    }

    public void incrementRecipeCount(String clientId, HttpServletRequest request) {
        String ipHash = hashIp(ClientIpResolver.resolve(request));
        TrialClientUsage usage = repository.findById(clientId)
            .orElseGet(() -> new TrialClientUsage(clientId, ipHash));
        usage.setRecipeCount(usage.getRecipeCount() + 1);
        usage.setIpHash(ipHash);
        usage.setUpdatedAt(LocalDateTime.now());
        repository.save(usage);
    }

    private boolean isValidClientId(String clientId) {
        try {
            UUID.fromString(clientId);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private String readCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return null;
        }
        Optional<Cookie> match = Arrays.stream(request.getCookies())
            .filter(c -> name.equals(c.getName()))
            .findFirst();
        return match.map(Cookie::getValue).orElse(null);
    }

    private String hashIp(String ip) {
        return ContentHashUtil.sha256("ip:" + ip);
    }
}
