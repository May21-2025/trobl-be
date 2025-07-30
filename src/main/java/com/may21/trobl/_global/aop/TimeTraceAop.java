package com.may21.trobl._global.aop;

import com.may21.trobl._global.component.GlobalValues;
import com.may21.trobl._global.component.PerformanceMetrics;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.UUID;

import static com.may21.trobl._global.component.AnsiColorCode.*;
import static com.may21.trobl._global.utility.LogUtil.*;

@Aspect
@Component
public class TimeTraceAop {

    private static final Logger log = LoggerFactory.getLogger(TimeTraceAop.class);
    private final ApiQueryCounter apiQueryCounter;
    private final PerformanceMetrics performanceMetrics;

    @Value("${STAGE}")
    private String stage;

    @Value("${performance.monitor.detailed:true}")
    private boolean detailedLogging;

    @Value("${performance.monitor.simple-threshold:50}")
    private int simpleThreshold;

    public TimeTraceAop(ApiQueryCounter apiQueryCounter, PerformanceMetrics performanceMetrics) {
        this.apiQueryCounter = apiQueryCounter;
        this.performanceMetrics = performanceMetrics;
    }

    @PostConstruct
    public void init() {
        String asciiLogo = generateAsciiLogo();
        log.warn("\n\n{}\n\n{}\n\n",
                color("STAGE : " + stage +"  Version : " + GlobalValues.getBEVersion(), CYAN),
                color(asciiLogo, TROBL_PINK)
        );
    }

    @Around("execution(* com.may21.trobl..controller..*(..))")
    public Object traceExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        if ("prod".equalsIgnoreCase(stage)) {
            return joinPoint.proceed();
        }

        // 고유한 요청 ID 생성 (MDC에 저장)
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("requestId", requestId);

        long startTime = System.nanoTime();
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String fullMethodName = className + "." + methodName;
        String requestURI = getCurrentUri();

        try {
            // 메서드 실행 전 로그 (개발 환경에서만)
            if ("local".equalsIgnoreCase(stage)) {
                log.debug("🚀 Starting execution: {} | Request ID: {}", fullMethodName, requestId);
            }

            Object result = joinPoint.proceed();

            // 실행 시간 계산
            long execTimeNanos = System.nanoTime() - startTime;
            long execTimeMillis = execTimeNanos / 1_000_000;
            int queryCount = apiQueryCounter.getCount();
            List<ApiQueryCounter.QueryTrace> queryTraces = apiQueryCounter.getQueryTraces();
            
            // 디버깅: 쿼리 추적 정보 확인
            log.info("[DEBUG] " + fullMethodName + " - Queries: " + queryCount + ", Traces: " + queryTraces.size());
            if (!queryTraces.isEmpty()) {
                log.info("[DEBUG] Sample traces:");
                queryTraces.stream().limit(3).forEach(trace ->
                        log.info("[DEBUG]   " + trace.toString()));
            }
            
            apiQueryCounter.reset();

            // 성능 메트릭 기록
            performanceMetrics.recordPerformance(fullMethodName, execTimeMillis, queryCount);

            // 성능 임계값 평가
            Threshold queryThreshold = Thresholds.evaluateQuery(queryCount);
            Threshold timeThreshold = Thresholds.evaluateTime(execTimeMillis);

            // 로깅 전략 결정
            boolean shouldLog = shouldLogPerformance(execTimeMillis, queryCount, queryThreshold, timeThreshold);

            if (shouldLog) {
                logPerformanceMetrics(requestURI, fullMethodName, execTimeMillis, queryCount,
                        queryThreshold, timeThreshold, requestId);
            }

            // 심각한 성능 이슈 별도 경고
            if (isCritical(queryThreshold, timeThreshold)) {
                log.info("🚨 CRITICAL PERFORMANCE ISSUE: [{}] Request ID: {} | " +
                                "Time: {}ms | Queries: {} | URI: {}",
                        fullMethodName, requestId, execTimeMillis, queryCount, requestURI);
            }

            return result;

        } finally {
            MDC.remove("requestId");
        }
    }

    private boolean shouldLogPerformance(long execTime, int queryCount,
                                         Threshold queryThreshold, Threshold timeThreshold) {
        // 임계값을 초과하거나 설정된 값보다 높은 경우 로깅
        return queryThreshold != null || timeThreshold != null ||
                queryCount > simpleThreshold || execTime > 100;
    }

    private void logPerformanceMetrics(String requestURI, String methodName, long execTime,
                                       int queryCount, Threshold queryThreshold,
                                       Threshold timeThreshold, String requestId) {

        StringBuilder warnings = new StringBuilder();

        if (queryThreshold != null) {
            warnings.append(formatWarning(methodName, "QUERY", queryThreshold));
        }
        if (timeThreshold != null) {
            warnings.append(formatWarning(methodName, "EXECUTION TIME", timeThreshold));
        }

        // 상세 로깅 vs 간단 로깅
        if (detailedLogging && (queryThreshold != null || timeThreshold != null)) {
            // 상세한 성능 로그
            log.debug(formatPerformanceLog(requestURI, methodName, execTime, queryCount,
                    queryThreshold, timeThreshold, warnings.toString()));
        } else {
            // 간단한 한 줄 로그
            log.debug("{} | Request ID: {}",
                    formatSimplePerformanceLog(methodName, execTime, queryCount,
                            queryThreshold, timeThreshold),
                    requestId);
        }
    }

    private boolean isCritical(Threshold queryThreshold, Threshold timeThreshold) {
        return (queryThreshold != null && queryThreshold.isUrgent()) ||
                (timeThreshold != null && timeThreshold.isUrgent());
    }

    private String getCurrentUri() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs == null ? "NONE" : attrs.getRequest().getRequestURI();
        } catch (Exception e) {
            return "ERROR";
        }
    }

    private String generateAsciiLogo() {
        return """
                
                ⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀        ⠀⠀⢄⠀⠀⠀
                ⠀⠀⠀⠀⡄⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀        ⠀⠀⠀⢜⠀⠀⠀
                ⠀⠀⠀⠀⢦⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀       ⠀⠀⢠⢣⠃⠀⠀
                ⠀⠀⠀⠀⢇⢆⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀       ⠀⠀⠀⡎⡎⠆⠀⠀
                ⠀⠀⠀⠀⢱⢱⢱⠀⠀⠀⠀⠀⠀⡀⡄⡄⡆⡆⡆⡆⡆⡤⡠⣀⣀⣀⡀⡄⢤⢰⢰⢰⢰⢠⢄⢄⠀⠀⠀⠀⠀⠀  ⠀⢀⢎⢎⢎⠅⠀⠀
                ⠀⠀⠀⠀⠐⢕⢕⢍⢆⢄⢀⢔⢜⢜⢜⢜⢜⢜⢜⢜⢜⢜⢜⢔⡒⡆⡇⡇⡇⡇⡇⡇⡇⡇⡇⡇⡝⣔⢢⢀⠀⡀⡔⡕⡕⡕⡕⠁⠀⠀
                ⠀⠀⠀⠀⠀⠑⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⠕⠀⠀⠀
                ⠀⠀⠀⠀⠀⠀⢘⢜⢜⢜⢜⢜⢜⢜⢜⢜⣜⢜⣜⢜⢜⢜⢜⢜⢜⢜⢜⢜⢜⣜⣜⣜⣜⢜⢜⢜⢜⢜⢜⢜⢜⢜⢜⢜⢜⠜⠀⠀⠀⠀
                ⠀⠀⠀⠀⠀⡰⡸⡸⡸⡸⡸⡸⡸⡸⠘⠈⠀⠁⠀⠁⠁⠑⠑⠑⠉⠊⠁⠁⠁⠀⠀⠀⠈⠈⠁⠃⠇⢇⢇⢕⢕⢕⢕⢕⢕⢕⡢⡀⠀⠀
                ⠀⠀⠀⡀⡆⡇⡇⡇⡇⡇⡇⣇⣷⣧⠀⠀⠀⢀⡆⠀⠀⠀⠀⠀⠀⠠⠀⠀⠀⠀⠀⠀⠀⣀⢀⠀⠀⢀⡕⠕⡕⡕⡕⢕⢕⢕⢜⢜⢔⠄
                ⡀⡄⡆⡇⡇⡇⡇⡇⡇⢇⢇⡛⡟⡿⡿⡿⣷⡿⣇⡀⡀⠀⠀⠀⠀⠜⠀⠀⠀⠀⠀⠀⣀⣿⣿⣿⣿⣿⣿⣷⣾⣮⢪⢪⢪⢪⢪⢪⢪⠊
                ⠈⠊⠊⠎⢎⢎⢎⢎⢎⢎⢎⢎⢎⢎⢎⢎⢆⢇⢇⢎⢎⢕⢕⡪⡢⡳⡰⡰⡰⡰⡲⣾⣿⣿⣿⣿⣿⣿⢿⡿⢟⢜⢜⢜⢜⢜⢜⢜⠜⠀
                ⠀⠀⠀⠀⠀⠀⠈⠈⠘⠸⠸⡸⡸⡸⡸⡸⡸⡸⡸⡸⡸⡸⡸⡸⡸⡸⡸⡸⡸⡸⡸⡸⢭⢡⠥⡱⡡⡢⡪⡪⡪⡪⡪⡪⡪⡪⡪⡪⠊⠀
                ⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠁⠇⡇⡇⡇⡇⡇⡇⡇⡇⡇⡇⡇⡇⡇⡇⢇⢇⢇⢇⢇⢇⢇⢇⢇⢇⢇⢇⢇⢇⢇⢇⢇⢇⢇⠇⠀⠀
                ⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠈⠪⢎⡎⡎⡎⡎⡎⡎⡎⡎⡎⡪⡪⡪⡪⡪⡪⡪⡪⠪⠪⡪⢪⠪⠪⠪⡪⡪⡪⡪⣪⠏⠀⠀⠀
                ⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠉⠳⡳⡵⣕⣕⢕⢕⢕⢕⢕⢕⢕⢕⢕⢕⢜⠤⡄⡄⣀⢀⢄⡪⡪⡪⣪⢞⡕⠁⠀⠀⠀
                ⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠈⠘⠱⡳⡽⣝⢞⣞⢼⡪⡮⣪⢎⣎⢮⣪⣪⡪⣲⡱⣕⢼⡪⡯⣺⠍⠀⠀⠀⠀⠀
                ⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠁⠙⠕⢗⢯⢞⡽⡵⣫⣞⢵⡳⡵⣝⣞⢮⣳⡫⣞⠽⠁⠀⠀⠀⠀⠀⠀
                ⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠁⠉⠋⠚⠮⢳⠽⡝⡮⠺⠕⠗⠝⠁⠁⠀⠀⠀⠀⠀⠀⠀
                """;
    }
}