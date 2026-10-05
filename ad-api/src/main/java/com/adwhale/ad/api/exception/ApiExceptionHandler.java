package com.adwhale.ad.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * 컨트롤러에서 던진 예외를 HTTP 응답으로 바꿔주는 전역 처리기.
 * (Android로 치면 앱 전체에 걸어둔 UncaughtExceptionHandler 같은 역할)
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /** 서버 쪽 일시 장애 → 503 + Retry-After: SDK에게 "5초 뒤 다시 보내라"고 알려줌 */
    @ExceptionHandler(EventPublishException.class)
    public ResponseEntity<Map<String, String>> handleEventPublish(EventPublishException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header("Retry-After", "5")
                .body(Map.of(
                        "error", "EVENT_PUBLISH_FAILED",
                        "message", "일시적으로 이벤트를 저장할 수 없습니다. 잠시 후 다시 시도하세요."));
    }
}
