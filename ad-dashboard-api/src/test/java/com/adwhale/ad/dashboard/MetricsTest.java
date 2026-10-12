package com.adwhale.ad.dashboard;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** DB 없이 도는 단위 테스트: 비율 계산 규칙 (스펙 3번) */
class MetricsTest {

    @Test
    void 합계끼리_나누고_소수_2자리_반올림() {
        assertEquals(3.08, Metrics.percent(312, 10120));
        assertEquals(33.33, Metrics.percent(1, 3));
    }

    @Test
    void 분모가_0이면_null() {
        assertNull(Metrics.percent(0, 0));
    }

    @Test
    void 시간별_CTR_평균이_아니라_합계_비율() {
        // 10시 1/10 (10%), 11시 9/90 (10%) → 합계 10/100 = 10%
        assertEquals(10.0, Metrics.percent(1 + 9, 10 + 90));
    }
}
