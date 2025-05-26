package com.may21.trobl._global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ExceptionCode {

  // COMMENT C000
  COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "C000", "댓글을 찾을 수 없습니다."),

  // NOTIFICATION N000
  NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "N000", "알림을 찾을 수 없습니다."),

  // TOKEN T000
  TOKEN_MISSING(HttpStatus.UNAUTHORIZED, "T000", "토큰이 없습니다."),
  INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "T001", "유효하지 않은 토큰입니다."),
  INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "T002", "리프레시 토큰이 유효하지 않습니다."),
  TOKEN_PARSE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "T003", "토큰 파싱에 실패했습니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "T004", "토큰이 만료되었습니다."),
  MALFORMED_TOKEN(HttpStatus.UNAUTHORIZED, "T005", "잘못된 토큰입니다."),

  // USER U000
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U000", "사용자를 찾을 수 없습니다."),
  USERNAME_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "U001", "이미 존재하는 아이디입니다."),
  INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "U002", "비밀번호가 일치하지 않습니다."),
  NICKNAME_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "U003", "이미 존재하는 닉네임입니다."),
  NICKNAME_REQUIREMENTS_NOT_MET(HttpStatus.BAD_REQUEST, "U004", "닉네임은 2자 이상 10자 이하로 입력해주세요."),


  // POST P000
  POST_NOT_FOUND(HttpStatus.NOT_FOUND, "P000", "게시글을 찾을 수 없습니다."),
  POST_NOT_AUTHORIZED(HttpStatus.FORBIDDEN, "P001", "게시글에 대한 권한이 없습니다."),
  POLL_OPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "P002", "투표 항목을 찾을 수 없습니다."),
  VOTE_NOT_FOUND(HttpStatus.NOT_FOUND, "P003", "투표를 찾을 수 없습니다."),
  PAIR_VIEW_CAN_NOT_BE_ADDED(HttpStatus.BAD_REQUEST, "P004", "페어뷰 항목을 추가할 수 없습니다."),


  //POLL P100
  POLL_NOT_FOUND(HttpStatus.NOT_FOUND, "P100", "설문조사를 찾을 수 없습니다."),

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
