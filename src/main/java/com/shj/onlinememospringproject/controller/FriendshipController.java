package com.shj.onlinememospringproject.controller;

import com.shj.onlinememospringproject.service.FriendshipService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Friendship")
@RestController
@RequiredArgsConstructor
public class FriendshipController {

    private final FriendshipService friendshipService;



}
