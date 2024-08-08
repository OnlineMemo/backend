package com.shj.onlinememospringproject.controller;

import com.shj.onlinememospringproject.dto.MemoDto;
import com.shj.onlinememospringproject.response.ResponseCode;
import com.shj.onlinememospringproject.response.ResponseData;
import com.shj.onlinememospringproject.service.MemoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Memo")
@RestController
@RequiredArgsConstructor
@RequestMapping("/memos")
public class MemoController {

    private final MemoService memoService;


    @GetMapping("/{memoId}")
    @Operation(summary = "메모정보 조회 [JWT O]")
    public ResponseEntity<ResponseData<MemoDto.Response>> findMemoInfo(@PathVariable(value = "memoId") Long memoId) {
        MemoDto.Response memoResponseDto = memoService.findMemoInfo(memoId);
        return ResponseData.toResponseEntity(ResponseCode.READ_MEMO, memoResponseDto);
    }
}
