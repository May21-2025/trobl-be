package com.may21.trobl._global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ExceptionCode {
    //ANNOUNCEMENT A000
    ANNOUNCEMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "A000", "공지사항을 찾을 수 없습니다."),

    // BRAND B000
    BRAND_NOT_FOUND(HttpStatus.NOT_FOUND, "B000", "브랜드를 찾을 수 없습니다."),
    ADVERTISEMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "B001", "광고를 찾을 수 없습니다."),

    // COMMENT C000
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "C000", "댓글을 찾을 수 없습니다."),

    // GenerateAI G000
    AI_REPORT_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "G000", "AI 기록 생성 한도를 초과했습니다."),

    // LAYOUT L000
    LAYOUT_NOT_FOUND(HttpStatus.NOT_FOUND, "L000", "레이아웃을 찾을 수 없습니다."),
    LAYOUT_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "L001", "이미 존재하는 레이아웃입니다."),
    INVALID_LAYOUT_INDEX(HttpStatus.BAD_REQUEST, "L002", "유효하지 않은 인덱스입니다."),

    // NOTIFICATION N000
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "N000", "알림을 찾을 수 없습니다."),
    NOTIFICATION_TYPE_NOT_BLOCKABLE(HttpStatus.BAD_REQUEST, "N001", "차단할 수 없는 알림 유형입니다."),

    // USER U000
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U000", "사용자를 찾을 수 없습니다."),
    USERNAME_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "U001", "이미 존재하는 아이디입니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "U002", "비밀번호가 일치하지 않습니다."),
    NICKNAME_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "U003", "이미 존재하는 닉네임입니다."),
    NICKNAME_REQUIREMENTS_NOT_MET(HttpStatus.BAD_REQUEST, "U004", "닉네임은 2자 이상 10자 이하로 입력해주세요."),
    NICKNAME_UPDATE_RESTRICTED(HttpStatus.BAD_REQUEST, "U005", "닉네임 변경은 30일에 한 번만 가능합니다."),
    NICKNAME_EQUAL_TO_EXISTING(HttpStatus.BAD_REQUEST, "U006", "닉네임이 기존 닉네임과 동일합니다."),
    NICKNAME_CANNOT_BE_BLANK(HttpStatus.BAD_REQUEST, "U007", "닉네임은 비워둘 수 없습니다."),
    USER_ALREADY_HAS_PARTNER(HttpStatus.BAD_REQUEST, "U008", "이미 파트너가 있습니다."),
    REQUESTED_PARTNER_ALREADY_HAS_PARTNER(HttpStatus.BAD_REQUEST, "U009", "요청한 파트너는 이미 파트너가 있습니다."),
    CANNOT_REQUEST_SELF(HttpStatus.BAD_REQUEST, "U010", "자기 자신에게 파트너 요청을 할 수 없습니다."),
    MARRIAGE_DATE_IS_NOT_SAME(HttpStatus.BAD_REQUEST, "U011", "결혼 날짜가 서로 다릅니다."),
    NICKNAME_CANNOT_CONTAIN_PROFANITY(HttpStatus.BAD_REQUEST, "U012", "닉네임에 욕설/비방어를 포함 할 수 없습니다"),

    // OAUTH O000
    OAUTH2_AUTHORIZATION_NOT_FOUND(HttpStatus.NOT_FOUND, "O000", "OAuth2 인증 정보를 찾을 수 없습니다."),
    OAUTH2_AUTHORIZATION_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "O001", "이미 존재하는 OAuth2 인증 정보입니다."),
    OAUTH2_AUTHORIZATION_INVALID(HttpStatus.BAD_REQUEST, "O002", "유효하지 않은 OAuth2 인증 정보입니다."),
    GOOGLE_USERINFO_INVALID(HttpStatus.BAD_REQUEST, "O003", "구글 사용자 정보가 유효하지 않습니다."),
    GOOGLE_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "O004", "구글 토큰이 유효하지 않습니다."),
    GOOGLE_LOGIN_FAILED(HttpStatus.BAD_REQUEST, "O005", "구글 로그인에 실패했습니다."),
    GOOGLE_CODE_INVALID(HttpStatus.BAD_REQUEST, "O006", "구글 코드가 유효하지 않습니다."),
    GOOGLE_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "O007", "구글 사용자 정보를 찾을 수 없습니다."),
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "O008", "인증에 실패했습니다."),
    INVALID_USER_OAUTH_INFO(HttpStatus.BAD_REQUEST, "O009", "유효하지 않은 사용자 OAuth 정보입니다."),
    OAUTH_MISMATCH(HttpStatus.BAD_REQUEST, "O010", "OAuth 제공자와 일치하지 않습니다."),

    // POST P000
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "P000", "게시글을 찾을 수 없습니다."),
    POST_NOT_AUTHORIZED(HttpStatus.FORBIDDEN, "P001", "게시글에 대한 권한이 없습니다."),
    POLL_OPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "P002", "투표 항목을 찾을 수 없습니다."),
    VOTE_NOT_FOUND(HttpStatus.NOT_FOUND, "P003", "투표를 찾을 수 없습니다."),
    FAIR_VIEW_CAN_NOT_BE_ADDED(HttpStatus.BAD_REQUEST, "P004", "페어뷰 항목을 추가할 수 없습니다."),
    POST_NOT_FIT_FOR_CONFIRMATION(HttpStatus.BAD_REQUEST, "P005", "게시글이 확인용 게시글이 아닙니다."),
    FAIR_VIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "P006", "페어뷰 항목을 찾을 수 없습니다."),
    PARTNER_NOT_FOUND(HttpStatus.NOT_FOUND, "P007", "페어뷰 파트너를 찾을 수 없습니다."),
    PARTNER_REQUEST_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "P008", "이미 파트너 요청을 하였습니다."),
    FAIR_VIEW_NOT_CONFIRMED(HttpStatus.BAD_REQUEST, "P009", "페어뷰 작성이 완료되지 않았습니다."),

    //POLL P100
    POLL_NOT_FOUND(HttpStatus.NOT_FOUND, "P100", "설문조사를 찾을 수 없습니다."),

    //REPORT R000
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "R000", "신고를 찾을 수 없습니다."),

    // TOKEN T000
    TOKEN_MISSING(HttpStatus.UNAUTHORIZED, "T000", "토큰이 없습니다."),
    ACCESS_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "T001", "토큰이 만료되었습니다."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "T002", "리프레시 토큰이 만료되었습니다."),
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "T003", "유효하지 않은 토큰입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "T004", "리프레시 토큰이 유효하지 않습니다."),
    TOKEN_PARSE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "T005", "토큰 파싱에 실패했습니다."),
    MALFORMED_TOKEN(HttpStatus.UNAUTHORIZED, "T006", "잘못된 토큰입니다."),

    //TAG T100
    TAG_NOT_FOUND(HttpStatus.NOT_FOUND, "T100", "태그를 찾을 수 없습니다."),
    TAG_EXISTS(HttpStatus.BAD_REQUEST, "T101", "이미 존재하는 태그입니다."),
    TAG_PROFANITY(HttpStatus.BAD_REQUEST, "T102", "태그에 욕설/비방어를 포함 할 수 없습니다."),
    TAG_POOL_NOT_FOUND(HttpStatus.NOT_FOUND, "T103", "태그풀을 찾을 수 없습니다."),
    TAG_POOL_EXISTS(HttpStatus.BAD_REQUEST, "T104", "이미 존재하는 태그풀입니다."),
    KEYWORD_NOT_FOUND(HttpStatus.NOT_FOUND, "T105", "키워드를 찾을 수 없습니다."),
    KEYWORD_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "T106", "이미 존재하는 키워드입니다."),

    // TEST T200
    TEST_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "T200", "테스트가 실패했습니다."),

    // Other Z000
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Z000", "UNAUTHORIZED"),
    RESTRICTED(HttpStatus.UNAUTHORIZED, "Z001", "RESTRICTED"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Z003", "FORBIDDEN"),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "Z004", "INPUT_VALUE_INVALID"),
    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "Z005", "INVALID_PARAMETER."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "Z006", "INVALID_REQUEST"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Z007", "INTERNAL_SERVER_ERROR"),
    NOT_IMPLEMENTED(HttpStatus.NOT_IMPLEMENTED, "Z008", "NOT_IMPLEMENTED"),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;

    ExceptionCode(final HttpStatus status, final String code, final String message) {
        this.status = status;
        this.message = message;
        this.code = code;
    }
}
