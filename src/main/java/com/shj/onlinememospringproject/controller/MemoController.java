package com.shj.onlinememospringproject.controller;

import com.shj.onlinememospringproject.dto.MemoDto;
import com.shj.onlinememospringproject.response.ResponseCode;
import com.shj.onlinememospringproject.response.ResponseData;
import com.shj.onlinememospringproject.service.MemoService;
import com.shj.onlinememospringproject.service.UserMemoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Memo")
@RestController
@RequiredArgsConstructor
@RequestMapping("/memos")
public class MemoController {

    private final MemoService memoService;
    private final UserMemoService userMemoService;


    @GetMapping("/{memoId}")
    @Operation(summary = "메모정보 조회 [JWT O]")
    public ResponseEntity<ResponseData<MemoDto.Response>> findMemoInfo(@PathVariable(value = "memoId") Long memoId) {
        MemoDto.Response memoResponseDto = memoService.findMemoInfo(memoId);
        return ResponseData.toResponseEntity(ResponseCode.READ_MEMO, memoResponseDto);
    }

    @PostMapping
    @Operation(summary = "개인/공동 메모 생성 [JWT O]")
    public ResponseEntity<ResponseData> createMemo(MemoDto.CreateRequest createRequestDto) {
        memoService.createMemo(createRequestDto);
        return ResponseData.toResponseEntity(ResponseCode.CREATED_MEMO);
    }

    @PostMapping("/{memoId}")
    @Operation(summary = "메모 사용자 초대 [JWT O]")
    public ResponseEntity<ResponseData> invteUsersToMemo(@PathVariable(value = "memoId") Long memoId, @RequestBody MemoDto.InviteRequest inviteRequest) {
        userMemoService.inviteUsersToMemo(memoId, inviteRequest.getUserIdList());
        return ResponseData.toResponseEntity(ResponseCode.CREATED_USERMEMO);
    }

    @DeleteMapping("/{memoId}")
    @Operation(summary = "메모 삭제/탈퇴 [JWT O]")
    public ResponseEntity<ResponseData> deleteMemo(@PathVariable(value = "memoId") Long memoId) {
        memoService.deleteMemo(memoId);
        return ResponseData.toResponseEntity(ResponseCode.DELETE_MEMO);
    }
}
