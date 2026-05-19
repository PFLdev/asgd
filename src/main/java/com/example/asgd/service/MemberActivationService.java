package com.example.asgd.service;

import com.example.asgd.dto.MemberActivationRequest;
import com.example.asgd.dto.MemberActivationResponse;

public interface MemberActivationService {

    MemberActivationResponse activate(MemberActivationRequest request);
}
