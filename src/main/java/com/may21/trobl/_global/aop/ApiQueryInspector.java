package com.may21.trobl._global.aop;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;

@RequiredArgsConstructor
@Component
public class ApiQueryInspector implements StatementInspector {
  private final ApiQueryCounter apiQueryCounter;

  @Override
  public String inspect(final String sql) {
    if (isInRequestScope()) {
      apiQueryCounter.increaseCount();
    }
    return sql;
  }

  private boolean isInRequestScope() {
    return Objects.nonNull(RequestContextHolder.getRequestAttributes());
  }
}
