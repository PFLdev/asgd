package com.example.asgd.service.impl;

import com.example.asgd.service.EduBenefitCache;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryEduBenefitCache implements EduBenefitCache {

    private final Set<String> validDeviceBloom = ConcurrentHashMap.newKeySet();
    private final Set<String> validDeviceCache = ConcurrentHashMap.newKeySet();
    private final Set<String> invalidDeviceCache = ConcurrentHashMap.newKeySet();
    private final Set<String> claimedDeviceBloom = ConcurrentHashMap.newKeySet();
    private final Set<String> claimedCache = ConcurrentHashMap.newKeySet();

    @Override
    public boolean mightBeValidDevice(String deviceIdHash) {
        return validDeviceBloom.isEmpty() || validDeviceBloom.contains(deviceIdHash);
    }

    @Override
    public boolean hasValidDeviceCache(String deviceIdHash) {
        return validDeviceCache.contains(deviceIdHash) && !invalidDeviceCache.contains(deviceIdHash);
    }

    @Override
    public void cacheValidDevice(String deviceIdHash) {
        validDeviceBloom.add(deviceIdHash);
        validDeviceCache.add(deviceIdHash);
        invalidDeviceCache.remove(deviceIdHash);
    }

    @Override
    public void cacheInvalidDevice(String deviceIdHash) {
        invalidDeviceCache.add(deviceIdHash);
    }

    @Override
    public boolean mightBeClaimed(long activityId, String deviceIdHash) {
        return claimedDeviceBloom.contains(claimKey(activityId, deviceIdHash));
    }

    @Override
    public boolean hasClaimedCache(long activityId, String deviceIdHash) {
        return claimedCache.contains(claimKey(activityId, deviceIdHash));
    }

    @Override
    public void cacheClaimed(long activityId, String deviceIdHash) {
        String claimKey = claimKey(activityId, deviceIdHash);
        claimedDeviceBloom.add(claimKey);
        claimedCache.add(claimKey);
    }

    private static String claimKey(long activityId, String deviceIdHash) {
        return activityId + ":" + deviceIdHash;
    }
}
