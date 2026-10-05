package com.atamanahmet.cinelog.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.atamanahmet.cinelog.cache.SortOption;
import com.atamanahmet.cinelog.domain.entity.ListType;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, ListType.class, ListType::from);
        registry.addConverter(String.class, SortOption.class, SortOption::fromRequest);
    }
}
