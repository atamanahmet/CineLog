package com.atamanahmet.cinelog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Site-wide content guardrails. adultEnabled=false forces adult results off and hides every adult option.
 */
@ConfigurationProperties(prefix = "content")
public record ContentPolicyProperties(@DefaultValue("false") boolean adultEnabled) {
}
