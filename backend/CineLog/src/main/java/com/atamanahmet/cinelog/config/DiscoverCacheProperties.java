package com.atamanahmet.cinelog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "discover.cache")
public class DiscoverCacheProperties {

    private int itemsPerSort = 500;
    private long ttlHours = 6;
    private long refreshHours = 5;

    public int getItemsPerSort() {
        return itemsPerSort;
    }

    public void setItemsPerSort(int itemsPerSort) {
        this.itemsPerSort = itemsPerSort;
    }

    public long getTtlHours() {
        return ttlHours;
    }

    public void setTtlHours(long ttlHours) {
        this.ttlHours = ttlHours;
    }

    public long getRefreshHours() {
        return refreshHours;
    }

    public void setRefreshHours(long refreshHours) {
        this.refreshHours = refreshHours;
    }
}
