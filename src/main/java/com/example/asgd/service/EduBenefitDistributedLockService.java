package com.example.asgd.service;

import java.util.concurrent.Callable;

public interface EduBenefitDistributedLockService {

    <T> T executeWithLock(String lockKey, Callable<T> callback);
}
