package com.may21.trobl._global.aop;

import lombok.Getter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.ArrayList;
import java.util.List;

@Component
@RequestScope
@Getter
public class ApiQueryCounter {

    private int count;
    private final List<QueryTrace> queryTraces = new ArrayList<>();


    public void increaseCount() {
        count++;
        
        // 쿼리 추적 정보 수집 (로그 없이)
        recordQueryTrace();
    }
    
    private void recordQueryTrace() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        
        // 실제 비즈니스 로직을 찾기 위해 특정 패키지만 필터링
        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            
            // trobl 패키지의 비즈니스 로직만 추적
            if (className.contains("com.may21.trobl") && 
                !className.contains("aop") && 
                !className.contains("$HibernateProxy") &&
                !className.contains("$$EnhancerBy") &&
                !className.contains("CGLIB") &&
                !className.contains("ApiQueryInspector")) {
                
                queryTraces.add(new QueryTrace(
                    count,
                    getSimpleClassName(className),
                    element.getMethodName(),
                    element.getLineNumber()
                ));
                break; // 첫 번째 비즈니스 로직 클래스만 기록
            }
        }
    }
    
    private String getSimpleClassName(String fullClassName) {
        return fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
    }

    public int getCount() {
        return count;
    }

    public void reset() {
        count = 0;
        queryTraces.clear();
    }
    
    /**
     * 쿼리 추적 정보
     */
    @Getter
    public static class QueryTrace {
        private final int queryNumber;
        private final String className;
        private final String methodName;
        private final int lineNumber;
        
        public QueryTrace(int queryNumber, String className, String methodName, int lineNumber) {
            this.queryNumber = queryNumber;
            this.className = className;
            this.methodName = methodName;
            this.lineNumber = lineNumber;
        }
        
        @Override
        public String toString() {
            return String.format("#%d: %s.%s():%d", queryNumber, className, methodName, lineNumber);
        }
    }
}