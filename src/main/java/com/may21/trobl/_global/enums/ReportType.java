package com.may21.trobl._global.enums;

public enum ReportType {
    ABUSE,              // 욕설/비하/폭언
    HATE_SPEECH,        // 혐오 표현
    SEXUAL_CONTENT,     // 음란/선정적 콘텐츠
    PRIVACY_VIOLATION,  // 사생활 침해/폭로
    PERSONAL_INFO,      // 개인정보 노출
    FALSE_INFORMATION,  // 허위 사실 유포
    SELF_HARM,          // 자살/자해/위해 언급
    SPAM,               // 도배/중복 게시물
    ADVERTISEMENT,      // 광고/스팸
    ETC;                // 기타 부적절한 글/댓글

    public static ReportType fromStr(String reportType) {
        try {
            return ReportType.valueOf(reportType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ETC;
        }
    }
}
