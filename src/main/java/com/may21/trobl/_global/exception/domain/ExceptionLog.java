package com.may21.trobl._global.exception.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@RequiredArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ExceptionLog {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(columnDefinition = "text")
    @Getter
    private String message;

    @Getter
    private String methodName;
    @Getter
    private String serviceName;

    @Getter
    private Integer codeLineNumber;

    private String apiPath;

    @Column(columnDefinition = "text")
    private String stackTrace;

    @Getter
    @CreatedDate
    private LocalDateTime occurredAt;

    public ExceptionLog(
            String message,
            String methodName,
            String serviceName,
            String apiPath,
            Integer codeLineNumber,
            String stackTrace) {
        this.message = message;
        this.methodName = methodName;
        this.serviceName = serviceName;
        this.apiPath = apiPath;
        this.codeLineNumber = codeLineNumber;
        this.stackTrace = stackTrace;
    }
}
