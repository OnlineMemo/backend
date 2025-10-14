package com.shj.onlinememospringproject.controller;

import com.shj.onlinememospringproject.response.ResponseCode;
import com.shj.onlinememospringproject.response.ResponseData;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// @Hidden
@Tag(name = "AWS Test")
@RestController
@RequiredArgsConstructor
public class TestController {

    @GetMapping("/health")
    @Operation(summary = "AWS - 서버 헬스체크 [JWT X]")
    public ResponseEntity<ResponseData> healthCheck() {
        return ResponseData.toResponseEntity(ResponseCode.HEALTHY_SUCCESS);
    }

    @GetMapping({"/", "/login", "/favicon.ico"})
    @Operation(summary = "AWS - GET 리소스 및 리다이렉트 에러 방지 [JWT X]")  // No static resource 및 프론트엔드의 window.location.href='/login' 호출시 발생 에러 방지.
    public ResponseEntity<ResponseData> preventGetError() {
        return ResponseData.toResponseEntity(ResponseCode.PREVENT_GET_ERROR);
    }


    // ========== Test 메소드 ========== //

//    @GetMapping("/test")
//    @Operation(summary = "Test API [JWT X]")
//    public ResponseEntity<ResponseData<String>> getTestResult() {
//        String testResult = "Test ResponseStr";
//        return ResponseData.toResponseEntity(ResponseCode.TEST_SUCCESS, testResult);
//    }
}
