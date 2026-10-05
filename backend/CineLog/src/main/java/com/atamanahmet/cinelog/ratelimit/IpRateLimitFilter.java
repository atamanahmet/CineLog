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
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class IpRateLimitFilter extends OncePerRequestFilter {

    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";

    private final RateLimitProperties.BucketLimit ipLimit;
    private final ConcurrentHashMap<String, Bucket> bucketsByIp = new ConcurrentHashMap<>();

    public IpRateLimitFilter(RateLimitProperties rateLimitProperties) {
        this.ipLimit = rateLimitProperties.getIp();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !isRateLimitedPath(path);
    }

    /**
     * Public media paths subject to per-IP limiting.
     */
    static boolean isRateLimitedPath(String path) {
        return path.startsWith("/api/movie/")
                || path.startsWith("/api/tv/")
                || path.startsWith("/api/discover/")
                || path.equals("/api/movies")
                || path.equals("/api/tvshows")
                || path.matches("/api/[^/]+/search(/.*)?");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String clientIp = resolveClientIp(request);
        Bucket bucket = bucketsByIp.computeIfAbsent(clientIp, ignored -> RateLimitBuckets.create(ipLimit));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            RateLimitResponses.writeTooManyRequests(response, probe.getNanosToWaitForRefill());
            return;
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Reads first ip in X-Forwarded-For, Render sets this to the real client ip.
     * Falls back to remote addr if header missing.
     * Safe only because Render is the sole network path to this app, review if that changes.
     */
    static String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader(FORWARDED_FOR_HEADER);
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}