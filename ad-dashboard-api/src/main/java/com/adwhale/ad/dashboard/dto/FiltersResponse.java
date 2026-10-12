package com.adwhale.ad.dashboard.dto;

import java.util.List;

/** GET /v1/stats/filters */
public record FiltersResponse(List<String> adIds, List<String> placementIds) {
}
