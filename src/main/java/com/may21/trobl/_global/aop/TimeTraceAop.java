package com.may21.trobl._global.aop;

import jakarta.annotation.PostConstruct;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static com.may21.trobl._global.component.AnsiColorCode.BROWN;
import static com.may21.trobl._global.component.AnsiColorCode.TROBL_PINK;
import static com.may21.trobl._global.utility.LogUtil.*;

@Aspect
@Component
public class TimeTraceAop {

    private static final Logger log = LoggerFactory.getLogger(TimeTraceAop.class);
    private final ApiQueryCounter apiQueryCounter;

    @Value("${STAGE}")
    private String stage;

    private String asciiLogo;

    public TimeTraceAop(ApiQueryCounter apiQueryCounter) {
        this.apiQueryCounter = apiQueryCounter;
    }

    @PostConstruct
    public void init() {
        asciiLogo =
                "\n"
                        + "⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀        ⠀⠀⢄⠀⠀⠀\n"
                        + "⠀⠀⠀⠀⡄⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀        ⠀⠀⠀⢜⠀⠀⠀\n"
                        + "⠀⠀⠀⠀⢦⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀       ⠀⠀⢠⢣⠃⠀⠀\n"
                        + "⠀⠀⠀⠀⢇⢆⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀       ⠀⠀⠀⡎⡎⠆⠀⠀\n"
                        + "⠀⠀⠀⠀⢱⢱⢱⠀⠀⠀⠀⠀⠀⡀⡄⡄⡆⡆⡆⡆⡆⡤⡠⣀⣀⣀⡀⡄⢤⢰⢰⢰⢰⢠⢄⢄⠀⠀⠀⠀⠀⠀  ⠀⢀⢎⢎⢎⠅⠀⠀\n"
                        + "⠀⠀⠀⠀⠐⢕⢕⢍⢆⢄⢀⢔⢜⢜⢜⢜⢜⢜⢜⢜⢜⢜⢜⢔⡒⡆⡇⡇⡇⡇⡇⡇⡇⡇⡇⡇⡝⣔⢢⢀⠀⡀⡔⡕⡕⡕⡕⠁⠀⠀\n"
                        + "⠀⠀⠀⠀⠀⠑⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⡕⠕⠀⠀⠀\n"
                        + "⠀⠀⠀⠀⠀⠀⢘⢜⢜⢜⢜⢜⢜⢜⢜⢜⣜⢜⣜⢜⢜⢜⢜⢜⢜⢜⢜⢜⢜⣜⣜⣜⣜⢜⢜⢜⢜⢜⢜⢜⢜⢜⢜⢜⢜⠜⠀⠀⠀⠀\n"
                        + "⠀⠀⠀⠀⠀⡰⡸⡸⡸⡸⡸⡸⡸⡸⠘⠈⠀⠁⠀⠁⠁⠑⠑⠑⠉⠊⠁⠁⠁⠀⠀⠀⠈⠈⠁⠃⠇⢇⢇⢕⢕⢕⢕⢕⢕⢕⡢⡀⠀⠀\n"
                        + "⠀⠀⠀⡀⡆⡇⡇⡇⡇⡇⡇⣇⣷⣧⠀⠀⠀⢀⡆⠀⠀⠀⠀⠀⠀⠠⠀⠀⠀⠀⠀⠀⠀⣀⢀⠀⠀⢀⡕⠕⡕⡕⡕⢕⢕⢕⢜⢜⢔⠄\n"
                        + "⡀⡄⡆⡇⡇⡇⡇⡇⡇⢇⢇⡛⡟⡿⡿⡿⣷⡿⣇⡀⡀⠀⠀⠀⠀⠜⠀⠀⠀⠀⠀⠀⣀⣿⣿⣿⣿⣿⣿⣷⣾⣮⢪⢪⢪⢪⢪⢪⢪⠊\n"
                        + "⠈⠊⠊⠎⢎⢎⢎⢎⢎⢎⢎⢎⢎⢎⢎⢎⢆⢇⢇⢎⢎⢕⢕⡪⡢⡳⡰⡰⡰⡰⡲⣾⣿⣿⣿⣿⣿⣿⢿⡿⢟⢜⢜⢜⢜⢜⢜⢜⠜⠀\n"
                        + "⠀⠀⠀⠀⠀⠀⠈⠈⠘⠸⠸⡸⡸⡸⡸⡸⡸⡸⡸⡸⡸⡸⡸⡸⡸⡸⡸⡸⡸⡸⡸⡸⢭⢡⠥⡱⡡⡢⡪⡪⡪⡪⡪⡪⡪⡪⡪⡪⠊⠀\n"
                        + "⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠁⠇⡇⡇⡇⡇⡇⡇⡇⡇⡇⡇⡇⡇⡇⡇⢇⢇⢇⢇⢇⢇⢇⢇⢇⢇⢇⢇⢇⢇⢇⢇⢇⢇⢇⠇⠀⠀\n"
                        + "⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠈⠪⢎⡎⡎⡎⡎⡎⡎⡎⡎⡎⡪⡪⡪⡪⡪⡪⡪⡪⠪⠪⡪⢪⠪⠪⠪⡪⡪⡪⡪⣪⠏⠀⠀⠀\n"
                        + "⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠉⠳⡳⡵⣕⣕⢕⢕⢕⢕⢕⢕⢕⢕⢕⢕⢜⠤⡄⡄⣀⢀⢄⡪⡪⡪⣪⢞⡕⠁⠀⠀⠀\n"
                        + "⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠈⠘⠱⡳⡽⣝⢞⣞⢼⡪⡮⣪⢎⣎⢮⣪⣪⡪⣲⡱⣕⢼⡪⡯⣺⠍⠀⠀⠀⠀⠀\n"
                        + "⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠁⠙⠕⢗⢯⢞⡽⡵⣫⣞⢵⡳⡵⣝⣞⢮⣳⡫⣞⠽⠁⠀⠀⠀⠀⠀⠀\n"
                        + "⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠁⠉⠋⠚⠮⢳⠽⡝⡮⠺⠕⠗⠝⠁⠁⠀⠀⠀⠀⠀⠀⠀\n";
        log.info("\n\n{}\n\n{}\n\n", color("STAGE : " + stage, BROWN), color(asciiLogo, TROBL_PINK));
    }

    @Around("execution(* com.may21.trobl..controller..*(..))")
    public Object traceExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        if ("prod".equalsIgnoreCase(stage)) return joinPoint.proceed();

        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().getName();
        String requestURI = getCurrentUri();

        Object result = joinPoint.proceed();

        long execTime = System.currentTimeMillis() - startTime;
        int queryCount = apiQueryCounter.getCount();
        apiQueryCounter.reset();

        if (queryCount <= 15 && execTime <= 300) return result;

        StringBuilder warn = new StringBuilder();
        boolean isUrgent = false;

        Threshold queryThreshold = Thresholds.evaluateQuery(queryCount);
        Threshold timeThreshold = Thresholds.evaluateTime(execTime);

        if (queryThreshold != null) {
            isUrgent |= queryThreshold.isUrgent();
            warn.append(formatWarning(methodName, "QUERY", queryThreshold));
        }
        if (timeThreshold != null) {
            isUrgent |= timeThreshold.isUrgent();
            warn.append(formatWarning(methodName, "EXECUTION TIME", timeThreshold));
        }

        log.info(
                formatTable(
                        requestURI,
                        methodName,
                        execTime,
                        queryCount,
                        queryThreshold,
                        timeThreshold,
                        warn.toString()));

        if (isUrgent) {
            log.error("URGENT WARNING: [{}] in CRITICAL state!", methodName);
        }

        return result;
    }

    private String getCurrentUri() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? "NONE" : attrs.getRequest().getRequestURI();
    }
}
