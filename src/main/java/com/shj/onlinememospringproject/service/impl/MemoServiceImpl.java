package com.shj.onlinememospringproject.service.impl;

import com.shj.onlinememospringproject.repository.MemoRepository;
import com.shj.onlinememospringproject.service.MemoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemoServiceImpl implements MemoService {

    private final MemoRepository memoRepository;



}
