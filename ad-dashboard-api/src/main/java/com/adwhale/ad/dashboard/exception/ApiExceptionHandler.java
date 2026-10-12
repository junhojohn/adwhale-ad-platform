package com.adwhale.ad.dashboard.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

/** 스펙 6번 공통 에러 형식: 400 {"error":"INVALID_PARAM","message":"..."} */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(InvalidParamException.class)
    public ResponseEntity<Map<String, String>> invalid(InvalidParamException e) {
        return badRequest(e.getMessage());
    }

    /** 날짜 형식 오류 (예: from=2026-13-01) */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> typeMismatch(MethodArgumentTypeMismatchException e) {
        return badRequest(e.getName() + " 값 형식이 잘못되었습니다 (yyyy-MM-dd): " + e.getValue());
    }

    /** 필수 파라미터 누락 (예: from 없음) */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, String>> missing(MissingServletRequestParameterException e) {
        return badRequest(e.getParameterName() + " 는 필수입니다");
    }

    private static ResponseEntity<Map<String, String>> badRequest(String message) {
        return ResponseEntity.badRequest().body(Map.of("error", "INVALID_PARAM", "message", message));
    }
}
