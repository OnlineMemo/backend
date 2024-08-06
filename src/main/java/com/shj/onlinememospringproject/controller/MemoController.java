package com.shj.onlinememospringproject.controller;

import com.shj.onlinememospringproject.service.MemoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Memo")
@RestController
@RequiredArgsConstructor
public class MemoController {

    private final MemoService memoService;



}
