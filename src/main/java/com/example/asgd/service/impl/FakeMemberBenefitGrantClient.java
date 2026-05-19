package com.example.asgd.service.impl;

import com.example.asgd.service.MemberBenefitGrantClient;
import org.springframework.stereotype.Service;

@Service
public class FakeMemberBenefitGrantClient implements MemberBenefitGrantClient {

    @Override
    public GrantResult grant(GrantRequest request) {
        return GrantResult.granted();
    }
}
