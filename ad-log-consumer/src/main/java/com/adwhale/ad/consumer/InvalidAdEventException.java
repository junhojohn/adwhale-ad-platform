package com.adwhale.ad.consumer;

/**
 * 메시지 자체가 잘못된 경우 (JSON 아님, 필수값 없음 등).
 * 몇 번을 다시 시도해도 결과가 같으므로 재시도 없이 바로 DLQ로 보낸다.
 */
public class InvalidAdEventException extends RuntimeException {

    public InvalidAdEventException(String message) {
        super(message);
    }

    public InvalidAdEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
