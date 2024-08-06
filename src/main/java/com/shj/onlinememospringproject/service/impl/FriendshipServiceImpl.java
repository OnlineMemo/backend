package com.shj.onlinememospringproject.service.impl;

import com.shj.onlinememospringproject.repository.FriendshipRepository;
import com.shj.onlinememospringproject.service.FriendshipService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FriendshipServiceImpl implements FriendshipService {

    private final FriendshipRepository friendshipRepository;



}
