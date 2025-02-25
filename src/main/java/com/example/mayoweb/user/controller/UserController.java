package com.example.mayoweb.user.controller;

import com.example.mayoweb.commons.annotation.Authenticated;
import com.example.mayoweb.commons.annotation.CreateUser;
import com.example.mayoweb.fcm.dto.CreateFCMTokenRequest;
import com.example.mayoweb.user.domain.dto.reqeust.CreateUserRequest;
import com.example.mayoweb.user.domain.dto.response.ReadUserResponse;
import com.example.mayoweb.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "유저 API", description = "유저 정보 API")
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "firebase 토큰 값으로 user정보를 가져옵니다.", description = "firebase 토큰 값으로 user정보를 가져옵니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "유저 조회 성공", content = @Content(schema = @Schema(implementation = ReadUserResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    @Authenticated
    @GetMapping("/user")
    public ResponseEntity<ReadUserResponse> getUserByToken(HttpServletRequest req) {
        return ResponseEntity.ok(userService.getUserById(req.getAttribute("uid").toString()));
    }

    @Operation(summary = "userId값으로 fcm 토큰을 가져옵니다.", description = "userId값으로 fcm 토큰을 가져옵니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "토큰 조회 성공", content = @Content(schema = @Schema(implementation = List.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    @Authenticated
    @GetMapping("/user-fcmToken")
    public ResponseEntity<List<String>> getFcmTokenByUserId(HttpServletRequest req) {
        return ResponseEntity.ok(userService.getTokensByUserRef(req.getAttribute("uid").toString()));
    }

    @Operation(summary = "userId값 및 fcmToken으로 해당 user의 fcm토큰을 생성합니다.", description = "userId값 및 fcmToken으로 해당 user의 fcm토큰을 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "토큰 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    @Authenticated
    @PostMapping("/fcm")
    public ResponseEntity<Void> createFCMToken(HttpServletRequest req, @RequestBody CreateFCMTokenRequest request) {
        userService.createWebFCMToken(req.getAttribute("uid").toString() ,request.fcmToken());

        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @Operation(summary = "유저 회원 가입", description = "유저 회원가입")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원 가입 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    @CreateUser
    @PostMapping
    public ResponseEntity<Void> createUser(HttpServletRequest req, @RequestBody CreateUserRequest request) {
        userService.createUser(request, req.getAttribute("uid").toString());
        return ResponseEntity.noContent().build();
    }
}
