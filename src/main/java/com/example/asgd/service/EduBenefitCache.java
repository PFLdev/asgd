package com.example.asgd.service;

public interface EduBenefitCache {

    boolean mightBeValidDevice(String deviceIdHash);

    boolean hasValidDeviceCache(String deviceIdHash);

    void cacheValidDevice(String deviceIdHash);

    void cacheInvalidDevice(String deviceIdHash);

    boolean mightBeClaimed(long activityId, String deviceIdHash);

    boolean hasClaimedCache(long activityId, String deviceIdHash);

    void cacheClaimed(long activityId, String deviceIdHash);
}
