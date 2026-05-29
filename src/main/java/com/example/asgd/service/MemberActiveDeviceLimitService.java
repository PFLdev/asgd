package com.example.asgd.service;

import com.example.asgd.dto.MemberActiveDeviceAccessRequest;
import com.example.asgd.dto.MemberActiveDeviceAccessResponse;

public interface MemberActiveDeviceLimitService {

    MemberActiveDeviceAccessResponse acquire(MemberActiveDeviceAccessRequest request);

    MemberActiveDeviceAccessResponse release(MemberActiveDeviceAccessRequest request);
}
