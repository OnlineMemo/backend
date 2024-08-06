package com.shj.onlinememospringproject.service.impl;

import com.shj.onlinememospringproject.repository.UserMemoRepository;
import com.shj.onlinememospringproject.service.UserMemoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserMemoServiceImpl implements UserMemoService {

    private final UserMemoRepository userMemoRepository;



}
