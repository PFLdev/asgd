package com.example.asgd.controller;

import com.example.asgd.dto.CouponClaimRequest;
import com.example.asgd.dto.CouponClaimResponse;
import com.example.asgd.service.CouponClaimService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coupon/claim")
public class CouponClaimController {

    private final CouponClaimService couponClaimService;

    public CouponClaimController(CouponClaimService couponClaimService) {
        this.couponClaimService = couponClaimService;
    }

    @PostMapping
    public CouponClaimResponse claim(@RequestBody CouponClaimRequest request) {
        return couponClaimService.claim(request);
    }
}
