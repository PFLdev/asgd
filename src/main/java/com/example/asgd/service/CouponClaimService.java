package com.example.asgd.service;

import com.example.asgd.dto.CouponClaimRequest;
import com.example.asgd.dto.CouponClaimResponse;

public interface CouponClaimService {

    CouponClaimResponse claim(CouponClaimRequest request);
}
