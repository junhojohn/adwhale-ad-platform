package com.adwhale.ad.dashboard.exception;

/** 잘못된 조회 조건 → 400 */
public class InvalidParamException extends RuntimeException {

    public InvalidParamException(String message) {
        super(message);
    }
}
