package com.adwhale.ad.dashboard;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 비율 지표 계산 (스펙 3번: 합계끼리 나누고, 분모가 0이면 null) */
public final class Metrics {

    private Metrics() {
    }

    /** numerator / denominator × 100, 소수 2자리 반올림. 분모 0이면 null */
    public static Double percent(long numerator, long denominator) {
        if (denominator == 0) {
            return null;
        }
        return BigDecimal.valueOf(numerator * 100.0 / denominator)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
