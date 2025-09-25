package com.shj.onlinememospringproject.response.responseitem;

public class MessageItem {

    // < User >
    public static final String CREATED_USER = "SUCCESS - 회원 가입 성공";
    public static final String READ_USER = "SUCCESS - 회원 정보 조회 성공";
    public static final String UPDATE_USER = "SUCCESS - 회원 정보 수정 성공";
    public static final String DELETE_USER = "SUCCESS - 회원 탈퇴 성공";
    public static final String NOT_FOUND_USER = "ERROR - 회원을 찾을 수 없습니다.";
    public static final String BAD_REQUEST_USER = "ERROR - 잘못된 회원요청 에러";
    public static final String DUPLICATE_EMAIL = "ERROR - 회원가입 로그인아이디 중복 에러";

    // < Memo >
    public static final String CREATED_MEMO = "SUCCESS - 메모 생성 성공";
    public static final String READ_MEMO = "SUCCESS - 메모 정보 조회 성공";
    public static final String READ_MEMOLIST = "SUCCESS - 회원의 메모 목록 조회 성공";
    public static final String UPDATE_MEMO = "SUCCESS - 메모 수정 성공";
    public static final String DELETE_MEMO = "SUCCESS - 메모 삭제 성공";
    public static final String NOT_FOUND_MEMO = "ERROR - 메모를 찾을 수 없습니다.";
    public static final String BAD_REQUEST_MEMO = "ERROR - 잘못된 메모요청 에러";

    // < UserMemo >
    public static final String CREATED_USERMEMO = "SUCCESS - 사용자와 메모 관계 생성(초대) 성공";
    public static final String NOT_FOUND_USERMEMO = "ERROR - 사용자와 메모 관계를 찾을 수 없습니다.";
    public static final String BAD_REQUEST_USERMEMO = "ERROR - 잘못된 사용자메모관계 요청 에러";
    public static final String DUPLICATE_USERANDMEMO = "ERROR - 사용자와 메모 관계 중복 에러";

    // < Friendship >
    public static final String CREATED_SENDFRIENDSHIP = "SUCCESS - 친구요청 보내기 성공";
    public static final String READ_SENDERLIST = "SUCCESS - 회원에게 친구요청 보낸 사용자들 목록 조회 성공";
    public static final String READ_FRIENDLIST = "SUCCESS - 회원의 친구 목록 조회 성공";
    public static final String UPDATE_FRIENDSHIP = "SUCCESS - 친구관계 수정 성공";
    public static final String DELETE_FRIENDSHIP = "SUCCESS - 친구관계 삭제 성공";
    public static final String NOT_FOUND_FRIENDSHIP = "ERROR - 친구관계를 찾을 수 없습니다.";
    public static final String BAD_REQUEST_FRIENDSHIP = "ERROR - 잘못된 친구요청 에러";

    // < Auth >
    public static final String LOGIN_SUCCESS = "SUCCESS - 로그인 성공";
    public static final String UPDATE_PASSWORD = "SUCCESS - 비밀번호 수정 성공";
    public static final String READ_IS_LOGIN = "SUCCESS - 현재 로그인 여부 조회 성공";
    public static final String UNAUTHORIZED = "ERROR - Unauthorized 에러";
    public static final String FORBIDDEN = "ERROR - Forbidden 에러";

    // < Token >
    public static final String REISSUE_SUCCESS = "SUCCESS - JWT Access 토큰 재발급 성공";
    public static final String TOKEN_EXPIRED = "ERROR - JWT 토큰 만료 에러";
    public static final String TOKEN_ERROR = "ERROR - 잘못된 JWT 토큰 에러";
    public static final String BAD_REQUEST_TOKEN = "ERROR - 잘못된 토큰 요청 에러";

    // < Ga4Filtered >
    public static final String READ_GA4FILTERED = "SUCCESS - GA4지표 조회 성공";
    public static final String BAD_REQUEST_GA4FILTERED = "ERROR - 잘못된 GA4지표 요청 에러";

    // < Etc >
    public static final String HEALTHY_SUCCESS = "SUCCESS - Health check 성공";
    public static final String TEST_SUCCESS = "SUCCESS - Test 성공";
    public static final String PREVENT_GET_ERROR = "Status 204 - 리소스 및 리다이렉트 GET호출 에러 방지";
    // - Lock
    public static final String LOCK_ACQUIRED = "SUCCESS - Lock 획득 성공";
    public static final String DELETE_LOCK = "SUCCESS - Lock 삭제 성공";
    public static final String CONFLICT_DATA_ERROR = "ERROR - 데이터 충돌 에러";  // Status 409
    public static final String LOCKED_DATA_ERROR = "ERROR - 데이터 잠금 에러";  // Status 423
    // - OpenAI
    public static final String SUCCESS_RESPONSE_OPENAI = "SUCCESS - AI 응답 성공";
    public static final String EXCESS_REQUEST_OPENAI = "ERROR - AI 요청 제한 초과";  // Status 429
    // - Client (HTTP)
    public static final String NOT_ALLOWED_METHOD = "ERROR - 지원되지 않는 HTTP 메서드";  // Status 405
    public static final String NOT_ACCEPTABLE_TYPE = "ERROR - 지원되지 않는 응답 타입 (Accept)";  // Status 406 : 클라이언트가 원하는 응답형식(Accept)을 서버가 제공할 수 없음.
    public static final String UNSUPPORTED_TYPE = "ERROR - 지원되지 않는 요청 타입 (Content-Type)";  // Status 415 : 클라이언트가 보낸 요청본문(Content-Type)을 서버가 처리할 수 없음.
    // - Server (Internal)
    public static final String ANONYMOUS_USER_ERROR = "ERROR - anonymousUser 에러";  // Status 500
    public static final String INTERNAL_SERVER_ERROR = "ERROR - 서버 내부 에러";  // Status 500
}
