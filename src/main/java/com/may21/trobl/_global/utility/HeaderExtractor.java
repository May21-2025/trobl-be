package com.may21.trobl._global.utility;

import com.may21.trobl._global.exception.BusinessException;
import com.may21.trobl._global.exception.ExceptionCode;
import jakarta.servlet.http.HttpServletRequest;

public class HeaderExtractor {

    private static final String STR_LC_UNKNOWN = "unknown";

    public static String extractIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");

        if (ipAddress == null || ipAddress.isEmpty() || STR_LC_UNKNOWN.equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || STR_LC_UNKNOWN.equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || STR_LC_UNKNOWN.equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || STR_LC_UNKNOWN.equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ipAddress == null || ipAddress.isEmpty() || STR_LC_UNKNOWN.equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();

            // 로컬 테스트 환경에서 IPv6 주소 "0:0:0:0:0:0:0:1"을 "127.0.0.1"로 변환
            if (ipAddress.equals("0:0:0:0:0:0:0:1")) {
                ipAddress = "127.0.0.1";
            }
        }

        // 여러 IP가 있는 경우 첫 번째 것만 사용
        if (ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }

        return ipAddress;
    }

    public static String extractDeviceInfo(HttpServletRequest request) {
        StringBuilder deviceInfo = new StringBuilder();

        // 기본 User-Agent 정보
        String userAgent = request.getHeader("User-Agent");
        deviceInfo.append("UserAgent: ").append(userAgent != null ? userAgent : STR_LC_UNKNOWN);

        // 추가 정보들 (사용 가능한 경우)
        String language = request.getHeader("Accept-Language");
        if (language != null) {
            deviceInfo.append("; Language: ").append(language);
        }

        String browserInfo = request.getHeader("Sec-CH-UA");
        if (browserInfo != null) {
            deviceInfo.append("; Browser: ").append(browserInfo);
        }

        String platform = request.getHeader("Sec-CH-UA-Platform");
        if (platform != null) {
            deviceInfo.append("; Platform: ").append(platform);
        }

        // 모바일 접속 여부 확인
        boolean isMobile =
                userAgent != null
                        && (userAgent.contains("Mobile")
                        || userAgent.contains("Android")
                        || userAgent.contains("iPhone")
                        || userAgent.contains("iPad"));
        deviceInfo.append("; Mobile: ").append(isMobile);

        return deviceInfo.toString();
    }


    public static String extractRefreshToken(HttpServletRequest request) {
        String tokenStr = request.getHeader("Authorization");
        if (tokenStr == null || tokenStr.isEmpty()) {
            throw new BusinessException(ExceptionCode.TOKEN_MISSING);
        }
        String refreshToken = tokenStr.startsWith("Bearer") ? tokenStr.substring("Bearer ".length()).trim() : tokenStr;
        if ( refreshToken.isEmpty() || STR_LC_UNKNOWN.equalsIgnoreCase(refreshToken)) {
            throw new BusinessException(ExceptionCode.INVALID_REFRESH_TOKEN);
        }
        return refreshToken;
    }

    public static String extractDeviceId(HttpServletRequest request) {
        String deviceId = request.getHeader("X-Device-ID");
        if (deviceId == null || deviceId.isEmpty() || STR_LC_UNKNOWN.equalsIgnoreCase(deviceId)) {
            deviceId = "unknown";
        }
        return deviceId;
    }

}
