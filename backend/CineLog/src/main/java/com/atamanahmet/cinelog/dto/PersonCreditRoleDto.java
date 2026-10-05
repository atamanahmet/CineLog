package com.atamanahmet.cinelog.dto;

/**
 * One cast or crew role on a credit item.
 */
public record PersonCreditRoleDto(
        String kind,
        String label,
        String department,
        Integer episodeCount) {
}
