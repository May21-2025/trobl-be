package com.may21.trobl._global.component;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 성능 지표를 수집하고 통계를 제공하는 클래스
 */
@Slf4j
@Component
@Getter
public class PerformanceMetrics {

    // 메서드별 성능 통계
    private final ConcurrentHashMap<String, MethodStats> methodStats = new ConcurrentHashMap<>();

    // 전체 요청 통계
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong totalExecutionTime = new AtomicLong(0);
    private final AtomicLong totalQueries = new AtomicLong(0);
    private final AtomicInteger slowRequests = new AtomicInteger(0);

    /**
     * 성능 데이터 기록
     */
    public void recordPerformance(String methodName, long executionTime, int queryCount) {
        // 전체 통계 업데이트
        totalRequests.incrementAndGet();
        totalExecutionTime.addAndGet(executionTime);
        totalQueries.addAndGet(queryCount);

        if (executionTime > 1000) { // 1초 이상이면 느린 요청
            slowRequests.incrementAndGet();
        }

        // 메서드별 통계 업데이트
        methodStats.computeIfAbsent(methodName, k -> new MethodStats())
                .update(executionTime, queryCount);
    }

    /**
     * 성능 요약 리포트 생성
     */
    public String generatePerformanceReport() {
        StringBuilder report = new StringBuilder();

        report.append("\n📊 PERFORMANCE SUMMARY REPORT\n");
        report.append("━".repeat(60)).append("\n");

        // 전체 통계
        long totalReq = totalRequests.get();
        if (totalReq > 0) {
            report.append(String.format("📈 Total Requests: %d\n", totalReq));
            report.append(String.format("⏱️  Avg Execution Time: %.2fms\n",
                    (double) totalExecutionTime.get() / totalReq));
            report.append(String.format("🗃️  Avg Queries per Request: %.1f\n",
                    (double) totalQueries.get() / totalReq));
            report.append(String.format("🐌 Slow Requests (>1s): %d (%.1f%%)\n",
                    slowRequests.get(), (slowRequests.get() * 100.0 / totalReq)));
        }

        report.append("\n🎯 TOP PERFORMANCE CONCERNS:\n");
        report.append("─".repeat(60)).append("\n");

        // 가장 느린 메서드들
        methodStats.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e2.getValue().getAvgExecutionTime(),
                        e1.getValue().getAvgExecutionTime()))
                .limit(5)
                .forEach(entry -> {
                    String method = entry.getKey();
                    MethodStats stats = entry.getValue();
                    report.append(String.format("🔍 %s: %.1fms avg (calls: %d, max: %dms)\n",
                            method, stats.getAvgExecutionTime(), stats.getCallCount().get(), stats.getMaxExecutionTime()));
                });

        return report.toString();
    }

    /**
     * 특정 메서드의 성능 히스토리 조회
     */
    public MethodStats getMethodStats(String methodName) {
        return methodStats.get(methodName);
    }

    /**
     * 성능 지표 초기화
     */
    public void reset() {
        methodStats.clear();
        totalRequests.set(0);
        totalExecutionTime.set(0);
        totalQueries.set(0);
        slowRequests.set(0);
    }

    /**
     * 메서드별 성능 통계 내부 클래스
     */
    @Getter
    public static class MethodStats {
        private final AtomicInteger callCount = new AtomicInteger(0);
        private final AtomicLong totalExecutionTime = new AtomicLong(0);
        private final AtomicLong totalQueries = new AtomicLong(0);
        private volatile long maxExecutionTime = 0;
        private volatile long minExecutionTime = Long.MAX_VALUE;
        private volatile int maxQueries = 0;

        public void update(long executionTime, int queryCount) {
            callCount.incrementAndGet();
            totalExecutionTime.addAndGet(executionTime);
            totalQueries.addAndGet(queryCount);

            // max/min 업데이트 (동시성 고려하여 volatile 사용)
            if (executionTime > maxExecutionTime) {
                maxExecutionTime = executionTime;
            }
            if (executionTime < minExecutionTime) {
                minExecutionTime = executionTime;
            }
            if (queryCount > maxQueries) {
                maxQueries = queryCount;
            }
        }

        public double getAvgExecutionTime() {
            int count = callCount.get();
            return count > 0 ? (double) totalExecutionTime.get() / count : 0;
        }

        public double getAvgQueries() {
            int count = callCount.get();
            return count > 0 ? (double) totalQueries.get() / count : 0;
        }

        public long getMinExecutionTime() {
            return minExecutionTime == Long.MAX_VALUE ? 0 : minExecutionTime;
        }
    }
}
