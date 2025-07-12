package com.may21.trobl._global.utility;

import com.may21.trobl._global.aop.ApiQueryCounter;
import com.may21.trobl._global.aop.Threshold;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.may21.trobl._global.component.AnsiColorCode.*;

public class LogUtil {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final String DIVIDER = "━".repeat(80);
    private static final String THIN_DIVIDER = "─".repeat(80);

    private LogUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String color(String text, String color) {
        return color + text + RESET;
    }

    public static String formatWarning(String method, String type, Threshold t) {
        return String.format("⚠️  [%s] %s %s: %s\n", 
            method, 
            getStatusEmoji(t), 
            type, 
            t.message()
        );
    }

    /**
     * 개선된 성능 로그 포맷 - 더 읽기 쉽고 정보가 풍부함
     */
    public static String formatPerformanceLog(
            String uri,
            String method,
            long time,
            int queryCount,
            Threshold queryThreshold,
            Threshold timeThreshold,
            String warning,
            List<ApiQueryCounter.QueryTrace> queryTraces) {
        
        StringBuilder sb = new StringBuilder();
        
        // 헤더 with 타임스탬프
        sb.append("\n").append(color(DIVIDER, TROBL_BLUE)).append("\n");
        sb.append(color("🚀 PERFORMANCE MONITOR", TROBL_PINK));
        sb.append(color(" | ", GRAY));
        sb.append(color(LocalDateTime.now().format(TIMESTAMP_FORMAT), LIGHT_CYAN));
        sb.append("\n").append(color(THIN_DIVIDER, GRAY)).append("\n");
        
        // 요청 정보
        sb.append(formatField("📍 Endpoint", uri, LIGHT_BLUE));
        sb.append(formatField("🎯 Method", method, GREEN));
        sb.append(formatField("🔗 HTTP Method", getHttpMethod(), LIGHT_CYAN));
        sb.append("\n");
        
        // 성능 메트릭
        sb.append(formatMetric("⏱️  Execution Time", time + "ms", timeThreshold, time));
        sb.append(formatMetric("🗃️  Query Count", String.valueOf(queryCount), queryThreshold, queryCount));
        sb.append(formatMetric("📊 Performance Grade", getPerformanceGrade(queryThreshold, timeThreshold), null, 0));
        
        // 쿼리 추적 정보 (디버깅: 모든 경우에 표시)
        if (!queryTraces.isEmpty()) {
            sb.append("\n").append(color("🔍 QUERY CALL TRACES (",GRAY)).append(queryTraces.size()).append(color(" traces):", ORANGE)).append("\n");
            for (ApiQueryCounter.QueryTrace trace : queryTraces) {
                sb.append(color("  " + trace.toString(), GRAY)).append("\n");
            }
        } else {
            sb.append("\n").append(color("🔍 QUERY CALL TRACES: No traces collected", ORANGE)).append("\n");
        }
        
        // 경고 메시지
        if (!warning.isEmpty()) {
            sb.append("\n").append(color("⚠️  WARNINGS:", ORANGE)).append("\n");
            sb.append(warning);
        }
        
        // 추천 사항
        String recommendation = getRecommendation(queryThreshold, timeThreshold);
        if (!recommendation.isEmpty()) {
            sb.append(color("💡 RECOMMENDATION:", YELLOW)).append("\n");
            sb.append(recommendation).append("\n");
        }
        
        sb.append(color(DIVIDER, TROBL_BLUE)).append("\n");
        
        return sb.toString();
    }

    /**
     * 간단한 성능 로그 (한 줄 요약)
     */
    public static String formatSimplePerformanceLog(
            String method, 
            long time, 
            int queryCount, 
            Threshold queryThreshold, 
            Threshold timeThreshold) {
        
        String timeColor = timeThreshold != null ? timeThreshold.color() : GREEN;
        String queryColor = queryThreshold != null ? queryThreshold.color() : GREEN;
        String grade = getPerformanceGrade(queryThreshold, timeThreshold);
        
        return String.format("%s [%s] %s %s⏱️%sms %s🗃️%s %s%s%s",
            getGradeEmoji(grade),
            color(method, LIGHT_BLUE),
            color(grade, getGradeColor(grade)),
            timeColor, time, RESET,
            queryColor, queryCount, RESET,
            timeThreshold != null || queryThreshold != null ? " ⚠️" : " ✅"
        );
    }

    // === 기존 함수 (호환성 유지) ===
    public static String formatTable(
            String uri,
            String method,
            long time,
            int queryCount,
            Threshold qt,
            Threshold tt,
            String warning) {
        return "\n+----------------------+-----------------------------------+\n"
                + String.format("| %-20s | %-33s |%n", "Request URI", uri)
                + String.format("| %-20s | %-33s |%n", "Method", method)
                + String.format(
                "| %-20s | %-33s |%n",
                "Execution Time (ms)", color(String.valueOf(time), tt != null ? tt.color() : GREEN))
                + String.format(
                "| %-20s | %-33s |%n",
                "Query Count", color(String.valueOf(queryCount), qt != null ? qt.color() : GREEN))
                + "+----------------------+-----------------------------------+\n"
                + warning;
    }

    // 오버로드: 기존 메서드 (쿼리 추적 없음)
    public static String formatPerformanceLog(
            String uri,
            String method,
            long time,
            int queryCount,
            Threshold queryThreshold,
            Threshold timeThreshold,
            String warning) {
        return formatPerformanceLog(uri, method, time, queryCount, queryThreshold, timeThreshold, warning, List.of());
    }

    // === 헬퍼 메서드들 ===
    private static String formatField(String label, String value, String color) {
        return String.format("%-20s %s\n", 
            color(label, GRAY), 
            color(value != null ? value : "N/A", color)
        );
    }

    private static String formatMetric(String label, String value, Threshold threshold, long numValue) {
        String color = threshold != null ? threshold.color() : GREEN;
        String status = threshold != null ? getStatusEmoji(threshold) : "✅";
        
        return String.format("%-20s %s %s %s\n", 
            color(label, GRAY), 
            status,
            color(value, color),
            threshold != null ? color("(" + threshold.label() + ")", GRAY) : ""
        );
    }

    private static String getStatusEmoji(Threshold threshold) {
        if (threshold == null) return "✅";
        return switch (threshold.label()) {
            case "1" -> "🚨";
            case "2" -> "⚠️";
            case "3" -> "⚡";
            case "4" -> "💭";
            default -> "❓";
        };
    }

    private static String getPerformanceGrade(Threshold queryThreshold, Threshold timeThreshold) {
        if (queryThreshold != null && queryThreshold.isUrgent() || 
            timeThreshold != null && timeThreshold.isUrgent()) {
            return "F";
        }
        if (queryThreshold != null && "2".equals(queryThreshold.label()) || 
            timeThreshold != null && "2".equals(timeThreshold.label())) {
            return "D";
        }
        if (queryThreshold != null && "3".equals(queryThreshold.label()) || 
            timeThreshold != null && "3".equals(timeThreshold.label())) {
            return "C";
        }
        if (queryThreshold != null && "4".equals(queryThreshold.label()) || 
            timeThreshold != null && "4".equals(timeThreshold.label())) {
            return "B";
        }
        return "A";
    }

    private static String getGradeEmoji(String grade) {
        return switch (grade) {
            case "A" -> "🟢";
            case "B" -> "🟡";
            case "C" -> "🟠";
            case "D" -> "🔴";
            case "F" -> "🚨";
            default -> "❓";
        };
    }

    private static String getGradeColor(String grade) {
        return switch (grade) {
            case "A" -> GREEN;
            case "B" -> YELLOW;
            case "C" -> ORANGE;
            case "D" -> RED;
            case "F" -> TROBL_RED;
            default -> GRAY;
        };
    }

    private static String getRecommendation(Threshold queryThreshold, Threshold timeThreshold) {
        StringBuilder recommendations = new StringBuilder();
        
        if (queryThreshold != null) {
            String queryAdvice = switch (queryThreshold.label()) {
                case "1" -> "🔥 즉시 쿼리 최적화 필요: N+1 문제, 인덱스 누락 확인";
                case "2" -> "⚡ 배치 처리나 페이징 도입 고려";
                case "3" -> "💡 불필요한 JOIN이나 중복 쿼리 제거";
                case "4" -> "📝 쿼리 개수 모니터링 지속";
                default -> "";
            };
            if (!queryAdvice.isEmpty()) {
                recommendations.append(queryAdvice).append("\n");
            }
        }
        
        if (timeThreshold != null) {
            String timeAdvice = switch (timeThreshold.label()) {
                case "1" -> "🔥 즉시 성능 개선 필요: 캐싱, 인덱스, 비동기 처리 검토";
                case "2" -> "⚡ 병목 지점 프로파일링 필요";
                case "3" -> "💡 알고리즘 개선이나 데이터베이스 최적화 고려";
                case "4" -> "📝 성능 추이 지속 모니터링";
                default -> "";
            };
            if (!timeAdvice.isEmpty()) {
                recommendations.append(timeAdvice).append("\n");
            }
        }
        
        return recommendations.toString();
    }

    private static String getHttpMethod() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                return attrs.getRequest().getMethod();
            }
        } catch (Exception e) {
            // Silent fail
        }
        return "N/A";
    }
}