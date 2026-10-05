package com.atamanahmet.cinelog.domain.entity;

public enum ListType {
    WATCHLIST("watchlist"),
    WATCHED("watchedlist"),
    LOVED("lovedlist"),
    REJECTED("rejectedlist");

    private final String pathValue;

    ListType(String pathValue) {
        this.pathValue = pathValue;
    }

    public String getPathValue() {
        return pathValue;
    }

    /**
     * Accept list type names or the lowercase path values the client sends.
     */
    public static ListType from(String raw) {
        if (raw != null) {
            for (ListType type : values()) {
                if (type.name().equalsIgnoreCase(raw) || type.pathValue.equalsIgnoreCase(raw)) {
                    return type;
                }
            }
        }
        throw new IllegalArgumentException("Unsupported listType: " + raw);
    }
}
