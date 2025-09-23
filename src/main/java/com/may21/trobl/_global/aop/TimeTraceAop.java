package com.may21.trobl._global.aop;

import com.may21.trobl._global.component.GlobalValues;
import com.may21.trobl._global.component.PerformanceMetrics;
import jakarta.annotation.PostConstruct;
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
            
            // API별 성능 요약 로그 출력
            logApiPerformanceSummary(requestURI, fullMethodName, execTimeMillis, queryCount, requestId);
            
            apiQueryCounter.reset();

            // 성능 메트릭 기록
            performanceMetrics.recordPerformance(fullMethodName, execTimeMillis, queryCount);

            // 성능 임계값 평가
            Threshold queryThreshold = Thresholds.evaluateQuery(queryCount);
            Threshold timeThreshold = Thresholds.evaluateTime(execTimeMillis);

            // 심각한 성능 이슈 별도 경고
            if (isCritical(queryThreshold, timeThreshold)) {
                log.warn("🚨 CRITICAL PERFORMANCE ISSUE: [{}] Request ID: {} | " +
                                "Time: {}ms | Queries: {} | URI: {}",
                        fullMethodName, requestId, execTimeMillis, queryCount, requestURI);
            }

            return result;

        } finally {
            MDC.remove("requestId");
        }
    }

    /**
     * API별 성능 요약 로그 출력
     */
    private void logApiPerformanceSummary(String requestURI, String methodName, long execTime, 
                                        int queryCount, String requestId) {
        // 기본 성능 요약 로그
        String performanceLog = String.format(
            "📊 API Performance: [%s] | Time: %dms | Queries: %d | URI: %s | Request ID: %s",
            methodName, execTime, queryCount, requestURI, requestId
        );
        
        // 임계값에 따른 로그 레벨 결정
        Threshold queryThreshold = Thresholds.evaluateQuery(queryCount);
        Threshold timeThreshold = Thresholds.evaluateTime(execTime);
        
        if (queryThreshold != null || timeThreshold != null) {
            // 임계값 초과 시 경고 로그
            String warningMessage = buildWarningMessage(queryThreshold, timeThreshold);
            log.warn("{} | {}", performanceLog, warningMessage);
        } else if (queryCount > simpleThreshold || execTime > 100) {
            // 설정된 임계값 초과 시 정보 로그
            log.info(performanceLog);
        } else {
            // 정상 범위 내에서도 디버그 로그로 기록
            log.debug(performanceLog);
        }
    }
    
    /**
     * 경고 메시지 구성
     */
    private String buildWarningMessage(Threshold queryThreshold, Threshold timeThreshold) {
        StringBuilder warnings = new StringBuilder();
        
        if (queryThreshold != null) {
            warnings.append(String.format("⚠️  QUERY WARNING: %s (Count: %d)", 
                queryThreshold.message(), queryThreshold.limit()));
        }
        
        if (timeThreshold != null) {
            if (warnings.length() > 0) warnings.append(" | ");
            warnings.append(String.format("⏱️  TIME WARNING: %s (Time: %dms)", 
                timeThreshold.message(), timeThreshold.limit()));
        }
        
        return warnings.toString();
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