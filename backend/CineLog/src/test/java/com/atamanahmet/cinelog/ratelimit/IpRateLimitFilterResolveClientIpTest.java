package com.atamanahmet.cinelog.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class IpRateLimitFilterResolveClientIpTest {

    @Test
    void usesSingleForwardedForValue() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.10");

        assertEquals("203.0.113.10", IpRateLimitFilter.resolveClientIp(request));
    }

    @Test
    void usesFirstValueWhenForwardedForHasMultipleIps() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.10, 198.51.100.20, 192.0.2.30");

        assertEquals("203.0.113.10", IpRateLimitFilter.resolveClientIp(request));
    }

    @Test
    void fallsBackToRemoteAddrWhenHeaderAbsent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");

        assertEquals("10.0.0.1", IpRateLimitFilter.resolveClientIp(request));
    }

    @Test
    void fallsBackToRemoteAddrWhenHeaderBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "   ");

        assertEquals("10.0.0.1", IpRateLimitFilter.resolveClientIp(request));
    }
}
