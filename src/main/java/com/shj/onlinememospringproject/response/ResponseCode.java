package com.shj.onlinememospringproject.response;

import com.shj.onlinememospringproject.response.responseitem.MessageItem;
import com.shj.onlinememospringproject.response.responseitem.StatusItem;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseCode {

    // Swagger API 응답값 미리보기 용도
    string(StatusItem.OK, "Swagger API"),

    // ===================== //

    // User 관련 성공 응답
    CREATED_USER(StatusItem.CREATED, MessageItem.CREATED_USER),
    READ_USER(StatusItem.OK, MessageItem.READ_USER),
    UPDATE_USER(StatusItem.NO_CONTENT, MessageItem.UPDATE_USER),
    DELETE_USER(StatusItem.NO_CONTENT, MessageItem.DELETE_USER),

    // User 관련 실패 응답
    NOT_FOUND_USER(StatusItem.NOT_FOUND, MessageItem.NOT_FOUND_USER),
    BAD_REQUEST_USER(StatusItem.BAD_REQUEST, MessageItem.BAD_REQUEST_USER),
    DUPLICATE_EMAIL(StatusItem.BAD_REQUEST, MessageItem.DUPLICATE_EMAIL),

    // ===================== //

    // Memo 관련 성공 응답
    CREATED_MEMO(StatusItem.CREATED, MessageItem.CREATED_MEMO),
    READ_MEMO(StatusItem.OK, MessageItem.READ_MEMO),
    READ_MEMOLIST(StatusItem.OK, MessageItem.READ_MEMOLIST),
    UPDATE_MEMO(StatusItem.NO_CONTENT, MessageItem.UPDATE_MEMO),
    DELETE_MEMO(StatusItem.NO_CONTENT, MessageItem.DELETE_MEMO),

    // Memo 관련 실패 응답
    NOT_FOUND_MEMO(StatusItem.NOT_FOUND, MessageItem.NOT_FOUND_MEMO),
    BAD_REQUEST_MEMO(StatusItem.BAD_REQUEST, MessageItem.BAD_REQUEST_MEMO),

    // ===================== //

    // UserMemo 관련 성공 응답
    CREATED_USERMEMO(StatusItem.CREATED, MessageItem.CREATED_USERMEMO),

    // UserMemo 관련 실패 응답
    NOT_FOUND_USERMEMO(StatusItem.NOT_FOUND, MessageItem.NOT_FOUND_USERMEMO),
    BAD_REQUEST_USERMEMO(StatusItem.BAD_REQUEST, MessageItem.BAD_REQUEST_USERMEMO),
    DUPLICATE_USERANDMEMO(StatusItem.BAD_REQUEST, MessageItem.DUPLICATE_USERANDMEMO),

    // ===================== //

    // Friendship 관련 성공 응답
    CREATED_SENDFRIENDSHIP(StatusItem.CREATED, MessageItem.CREATED_SENDFRIENDSHIP),
    READ_SENDERLIST(StatusItem.OK, MessageItem.READ_SENDERLIST),
    READ_FRIENDLIST(StatusItem.OK, MessageItem.READ_FRIENDLIST),
    UPDATE_FRIENDSHIP(StatusItem.NO_CONTENT, MessageItem.UPDATE_FRIENDSHIP),
    DELETE_FRIENDSHIP(StatusItem.NO_CONTENT, MessageItem.DELETE_FRIENDSHIP),

    // Friendship 관련 실패 응답
    NOT_FOUND_FRIENDSHIP(StatusItem.NOT_FOUND, MessageItem.NOT_FOUND_FRIENDSHIP),
    BAD_REQUEST_FRIENDSHIP(StatusItem.BAD_REQUEST, MessageItem.BAD_REQUEST_FRIENDSHIP),

    // ===================== //

    // Token 성공 응답
    REISSUE_SUCCESS(StatusItem.OK, MessageItem.REISSUE_SUCCESS),

    // Token 실패 응답
    TOKEN_EXPIRED(StatusItem.UNAUTHORIZED, MessageItem.TOKEN_EXPIRED),
    TOKEN_ERROR(StatusItem.UNAUTHORIZED, MessageItem.TOKEN_ERROR),
    BAD_REQUEST_TOKEN(StatusItem.BAD_REQUEST, MessageItem.BAD_REQUEST_TOKEN),

    // ===================== //

    // Ga4Filtered 성공 응답
    READ_GA4FILTERED(StatusItem.OK, MessageItem.READ_GA4FILTERED),

    // Ga4Filtered 실패 응답
    BAD_REQUEST_GA4FILTERED(StatusItem.BAD_REQUEST, MessageItem.BAD_REQUEST_GA4FILTERED),

    // ===================== //

    // 기타 성공 응답
    READ_IS_LOGIN(StatusItem.OK, MessageItem.READ_IS_LOGIN),
    LOGIN_SUCCESS(StatusItem.OK, MessageItem.LOGIN_SUCCESS),
    UPDATE_PASSWORD(StatusItem.NO_CONTENT, MessageItem.UPDATE_PASSWORD),
    LOCK_ACQUIRED(StatusItem.CREATED, MessageItem.LOCK_ACQUIRED),
    DELETE_LOCK(StatusItem.NO_CONTENT, MessageItem.DELETE_LOCK),
    SUCCESS_RESPONSE_OPENAI(StatusItem.OK, MessageItem.SUCCESS_RESPONSE_OPENAI),
    HEALTHY_SUCCESS(StatusItem.OK, MessageItem.HEALTHY_SUCCESS),
    TEST_SUCCESS(StatusItem.OK, MessageItem.TEST_SUCCESS),
    PREVENT_GET_ERROR(StatusItem.NO_CONTENT, MessageItem.PREVENT_GET_ERROR),

    // 기타 실패 응답
    INTERNAL_SERVER_ERROR(StatusItem.INTERNAL_SERVER_ERROR, MessageItem.INTERNAL_SERVER_ERROR),
    EXTERNAL_SERVER_ERROR(StatusItem.INTERNAL_SERVER_ERROR, MessageItem.EXTERNAL_SERVER_ERROR),
    ANONYMOUS_USER_ERROR(StatusItem.INTERNAL_SERVER_ERROR, MessageItem.ANONYMOUS_USER_ERROR),
    NOT_ALLOWED_METHOD(StatusItem.METHOD_NOT_ALLOWED, MessageItem.NOT_ALLOWED_METHOD),
    NOT_ACCEPTABLE_TYPE(StatusItem.NOT_ACCEPTABLE, MessageItem.NOT_ACCEPTABLE_TYPE),
    UNSUPPORTED_TYPE(StatusItem.UNSUPPORTED_MEDIA_TYPE, MessageItem.UNSUPPORTED_TYPE),
    CONFLICT_DATA_ERROR(StatusItem.CONFLICT, MessageItem.CONFLICT_DATA_ERROR),
    LOCKED_DATA_ERROR(StatusItem.LOCKED, MessageItem.LOCKED_DATA_ERROR),
    EXCESS_REQUEST_OPENAI(StatusItem.TO_MANY_REQUESTS, MessageItem.EXCESS_REQUEST_OPENAI),
    UNAUTHORIZED_ERROR(StatusItem.UNAUTHORIZED, MessageItem.UNAUTHORIZED),
    FORBIDDEN_ERROR(StatusItem.FORBIDDEN, MessageItem.FORBIDDEN),

    // ===================== //
    ;

    private int httpStatus;
    private String message;
}
