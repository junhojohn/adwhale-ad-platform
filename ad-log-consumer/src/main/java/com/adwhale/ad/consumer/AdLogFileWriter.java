package com.adwhale.ad.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 토픽별 / 날짜별 로그 파일에 한 줄씩 덧붙여 쓴다.
 *
 * 저장 위치 예: logs/ad-impression/ad-impression-2026-10-03.log
 * 한 줄 = 메시지 1건 (JSON Lines 형식) → 나중에 저장소에 적재하거나 Presto로 읽기 좋은 형태
 *
 * 매 메시지마다 파일을 열고 닫는 단순한 방식이라 대량 트래픽엔 느리지만, 지금은 흐름 이해가 목적.
 */
@Component
public class AdLogFileWriter {

    private static final Logger log = LoggerFactory.getLogger(AdLogFileWriter.class);
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final Path baseDir;

    public AdLogFileWriter(@Value("${adwhale.log.dir}") String dir) {
        this.baseDir = Path.of(dir).toAbsolutePath().normalize();
        log.info("광고 로그 저장 경로: {}", baseDir);
    }

    public synchronized void append(String topic, String line) {
        String date = LocalDate.now(KST).toString(); // 2026-10-03
        Path file = baseDir.resolve(topic).resolve(topic + "-" + date + ".log");

        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, line + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            // 예외를 던지면 spring-kafka가 같은 메시지를 몇 번 재시도한 뒤, 그래도 실패하면 에러 로그를 남기고 넘어간다
            throw new UncheckedIOException("로그 파일 쓰기 실패: " + file, e);
        }
    }
}
