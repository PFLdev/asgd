package com.example.asgd.service.impl;

import com.example.asgd.service.EduBenefitDistributedLockService;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class InMemoryEduBenefitDistributedLockService implements EduBenefitDistributedLockService {

    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    @Override
    public <T> T executeWithLock(String lockKey, Callable<T> callback) {
        ReentrantLock lock = locks.computeIfAbsent(lockKey, key -> new ReentrantLock());
        lock.lock();
        try {
            return callback.call();
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to execute locked benefit claim", ex);
        } finally {
            lock.unlock();
        }
    }
}
