package com.shj.onlinememospringproject.service.impl;

import com.shj.onlinememospringproject.repository.UserRepository;
import com.shj.onlinememospringproject.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;



}
