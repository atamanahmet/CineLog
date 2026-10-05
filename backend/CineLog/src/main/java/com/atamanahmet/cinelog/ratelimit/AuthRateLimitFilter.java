package com.atamanahmet.cinelog.ratelimit;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.atamanahmet.cinelog.config.RateLimitProperties;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 19)
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private final RateLimitProperties.BucketLimit authLimit;
    private final ConcurrentHashMap<String, Bucket> bucketsByIp = new ConcurrentHashMap<>();

    public AuthRateLimitFilter(RateLimitProperties rateLimitProperties) {
        this.authLimit = rateLimitProperties.getAuth();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !isRateLimitedPath(request.getRequestURI());
    }

    /**
     * Login, register, and password-confirmed account changes share one per-IP auth bucket.
     */
    static boolean isRateLimitedPath(String path) {
        return "/api/auth/login".equals(path)
                || "/api/auth/register".equals(path)
                || "/api/user/account/password".equals(path)
                || "/api/user/account/email".equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String clientIp = IpRateLimitFilter.resolveClientIp(request);
        Bucket bucket = bucketsByIp.computeIfAbsent(clientIp, ignored -> RateLimitBuckets.create(authLimit));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            RateLimitResponses.writeTooManyRequests(response, probe.getNanosToWaitForRefill());
            return;
        }
        filterChain.doFilter(request, response);
    }
}
