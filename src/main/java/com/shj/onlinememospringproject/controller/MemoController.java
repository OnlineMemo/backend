package com.shj.onlinememospringproject.controller;

import com.shj.onlinememospringproject.dto.MemoDto;
import com.shj.onlinememospringproject.response.ResponseCode;
import com.shj.onlinememospringproject.response.ResponseData;
import com.shj.onlinememospringproject.service.MemoFacade;
import com.shj.onlinememospringproject.service.MemoService;
import com.shj.onlinememospringproject.service.UserMemoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Memo")
@RestController
@RequiredArgsConstructor
@RequestMapping("/memos")
public class MemoController {

    private final MemoFacade memoFacade;
    private final MemoService memoService;
    private final UserMemoService userMemoService;


    @GetMapping("/{memoId}")
    @Operation(summary = "메모정보 조회 [JWT O]")
    public ResponseEntity<ResponseData<MemoDto.Response>> findMemoInfo(@PathVariable(value = "memoId") Long memoId) {
        MemoDto.Response memoResponseDto = memoService.findMemoInfo(memoId);
        return ResponseData.toResponseEntity(ResponseCode.READ_MEMO, memoResponseDto);
    }

    @GetMapping
    @Operation(summary = "메모 목록 조회/정렬/검색 [JWT O]",
            description = """
                    <strong>< RequestParam ></strong>
                    - <strong>filter</strong> : required = false
                    - <strong>search</strong> : required = false  \n
                    <strong>< URI ></strong>
                    - <strong>정렬 URI</strong> : /memos?filter={정렬종류 string}
                    - <strong>검색 URI</strong> : /memos?search={검색종류 string}  \n
                    <strong>< value ></strong>
                    - <strong>정렬 종류(의미)</strong> : 빈값(전체 메모), private-memo(개인 메모), group-memo(공동 메모), star-memo(즐겨찾기 개인메모)
                    - <strong>검색 종류(의미)</strong> : 빈값(전체 메모), 제목 또는 내용에 포함된 키워드
                    """)
    public ResponseEntity<ResponseData<List<MemoDto.MemoPageResponse>>> findMemos(
            @RequestParam(value = "filter", required = false) String filter,
            @RequestParam(value = "search", required = false) String search) {
        List<MemoDto.MemoPageResponse> memoPageResponseDtoList = memoService.findMemos(filter, search);
        return ResponseData.toResponseEntity(ResponseCode.READ_MEMOLIST, memoPageResponseDtoList);
    }

    @PostMapping
    @Operation(summary = "개인/공동 메모 생성 [JWT O]", description = "- userIdList 필드 : null 허용 (개인메모인 경우에만)")
    public ResponseEntity<ResponseData<MemoDto.CreateResponse>> createMemo(@RequestBody MemoDto.CreateRequest createRequestDto) {
        MemoDto.CreateResponse createResponseDto = memoService.createMemo(createRequestDto);
        return ResponseData.toResponseEntity(ResponseCode.CREATED_MEMO, createResponseDto);
    }

    @PostMapping("/{memoId}/lock")
    @Operation(summary = "메모 편집모드 Lock 검사/생성/연장 [JWT O]", description = "!!! 프론트엔드 : <strong>공동 메모</strong>인 경우에만 호출할 것 !!!<br>(비록 백엔드에서 한 번 더 검증하지만, 불필요한 API 호출은 리소스 낭비로 이어지므로 지양해야함.)")
    public ResponseEntity<ResponseData> checkEditLock(@PathVariable(value = "memoId") Long memoId) {
        memoService.checkEditLock(memoId);
        return ResponseData.toResponseEntity(ResponseCode.LOCK_ACQUIRED);
    }

    @DeleteMapping("/{memoId}/lock")
    @Operation(summary = "메모 편집모드 Lock 삭제 [JWT O]", description = "!!! 프론트엔드 : <strong>공동 메모</strong>인 경우에만 호출할 것 !!!<br>(비록 백엔드에서 한 번 더 검증하지만, 불필요한 API 호출은 리소스 낭비로 이어지므로 지양해야함.)")
    public ResponseEntity<ResponseData> releaseEditLock(@PathVariable(value = "memoId") Long memoId) {
        memoService.releaseEditLock(memoId);
        return ResponseData.toResponseEntity(ResponseCode.DELETE_LOCK);
    }

    @PutMapping("/{memoId}")
    @Operation(summary = "메모 제목/내용/즐겨찾기 수정 [JWT O]",
            description = """
                    - title & content 필드 : null 허용 (즐겨찾기 수정인 경우에만)
                    - isStar 필드 : null 허용 (제목/내용 수정인 경우에만)
                    """)
    public ResponseEntity<ResponseData> updateMemo(@PathVariable(value = "memoId") Long memoId, @RequestBody MemoDto.UpdateRequest updateRequestDto) {
        if(updateRequestDto.getIsStar() != null) memoService.updateMemo(memoId, updateRequestDto);  // Lock 제어는 메모의 즐겨찾기 수정과는 무관하므로, 퍼사드 메소드 호출 X.
        else memoFacade.updateMemoFacade(memoId, updateRequestDto);  // 퍼사드 메소드 호출 O.
        return ResponseData.toResponseEntity(ResponseCode.UPDATE_MEMO);
    }

    @PostMapping("/{memoId}")
    @Operation(summary = "메모 사용자 초대 [JWT O]")
    public ResponseEntity<ResponseData> invteUsersToMemo(@PathVariable(value = "memoId") Long memoId, @RequestBody MemoDto.InviteRequest inviteRequestDto) {
        userMemoService.inviteUsersToMemo(memoId, inviteRequestDto.getUserIdList());
        return ResponseData.toResponseEntity(ResponseCode.CREATED_USERMEMO);
    }

    @DeleteMapping("/{memoId}")
    @Operation(summary = "메모 삭제/탈퇴 [JWT O]")
    public ResponseEntity<ResponseData> deleteMemo(@PathVariable(value = "memoId") Long memoId) {
        memoService.deleteMemo(memoId);
        return ResponseData.toResponseEntity(ResponseCode.DELETE_MEMO);
    }

    @PostMapping("/{memoId}/ai/title")
    @Operation(summary = "메모 내용 기반의 AI 제목 생성 [JWT O]")
    public ResponseEntity<ResponseData<MemoDto.GenerateResponse>> generateTitleByOpenAI(@PathVariable(value = "memoId") Long memoId, @RequestBody MemoDto.GenerateRequest generateRequestDto) {
        MemoDto.GenerateResponse generateResponseDto = memoService.generateTitleByOpenAI(memoId, generateRequestDto);
        return ResponseData.toResponseEntity(ResponseCode.SUCCESS_RESPONSE_OPENAI, generateResponseDto);
    }
}
