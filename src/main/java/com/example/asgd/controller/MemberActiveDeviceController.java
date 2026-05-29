package com.example.asgd.controller;

import com.example.asgd.dto.MemberActiveDeviceAccessRequest;
import com.example.asgd.dto.MemberActiveDeviceAccessResponse;
import com.example.asgd.service.MemberActiveDeviceLimitService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/member/active-device")
public class MemberActiveDeviceController {

    private final MemberActiveDeviceLimitService memberActiveDeviceLimitService;

    public MemberActiveDeviceController(MemberActiveDeviceLimitService memberActiveDeviceLimitService) {
        this.memberActiveDeviceLimitService = memberActiveDeviceLimitService;
    }

    @PostMapping("/access")
    public MemberActiveDeviceAccessResponse access(@RequestBody MemberActiveDeviceAccessRequest request) {
        return memberActiveDeviceLimitService.acquire(request);
    }

    @PostMapping("/release")
    public MemberActiveDeviceAccessResponse release(@RequestBody MemberActiveDeviceAccessRequest request) {
        return memberActiveDeviceLimitService.release(request);
    }
}
