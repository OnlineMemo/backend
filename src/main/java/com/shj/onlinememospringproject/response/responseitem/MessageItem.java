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
    public static final String DUPLICATE_USERMEMO = "ERROR - 사용자와 메모 관계 중복 에러";
    public static final String NOT_FOUND_USERMEMO = "ERROR - 사용자와 메모 관계를 찾을 수 없습니다.";
    public static final String BAD_REQUEST_USERMEMO = "ERROR - 잘못된 사용자메모관계 요청 에러";

    // < Friendship >
    public static final String CREATED_SENDFRIENDSHIP = "SUCCESS - 친구요청 보내기 성공";
    public static final String READ_SENDERLIST = "SUCCESS - 회원에게 친구요청 보낸 사용자들 목록 조회 성공";
    public static final String READ_FRIENDLIST = "SUCCESS - 회원의 친구 목록 조회 성공";
    public static final String UPDATE_FRIENDSHIP = "SUCCESS - 친구관계 수정 성공";
    public static final String DELETE_FRIENDSHIP = "SUCCESS - 친구관계 삭제 성공";
    public static final String NOT_FOUND_FRIENDSHIP = "ERROR - 친구관계를 찾을 수 없습니다.";
    public static final String DUPLICATE_FRIENDSHIP = "ERROR - 친구요청 중복 에러";
    public static final String BAD_REQUEST_FRIENDSHIP = "ERROR - 잘못된 친구요청 에러";

    // < Auth >
    public static final String LOGIN_SUCCESS = "SUCCESS - 로그인 성공";
    public static final String UPDATE_PASSWORD = "SUCCESS - 비밀번호 수정 성공";
    public static final String READ_IS_LOGIN = "SUCCESS - 현재 로그인 여부 조회 성공";
    public static final String GET_LOGIN = "204 - login api를 get method로 호출하였음";
    public static final String UNAUTHORIZED = "ERROR - Unauthorized 에러";
    public static final String FORBIDDEN = "ERROR - Forbidden 에러";

    // < Token >
    public static final String REISSUE_SUCCESS = "SUCCESS - JWT Access 토큰 재발급 성공";
    public static final String TOKEN_EXPIRED = "ERROR - JWT 토큰 만료 에러";
    public static final String TOKEN_ERROR = "ERROR - 잘못된 JWT 토큰 에러";
    public static final String BAD_REQUEST_TOKEN = "ERROR - 잘못된 토큰 요청 에러";

    // < Etc >
    public static final String HEALTHY_SUCCESS = "SUCCESS - Health check 성공";
    public static final String anonymousUser_ERROR = "ERROR - anonymousUser 에러";
    public static final String INTERNAL_SERVER_ERROR = "ERROR - 서버 내부 에러";
}
