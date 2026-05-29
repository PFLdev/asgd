package com.example.demo;

import java.util.*;

public class Solution2 {

    public static void main(String[] args) {
        int[] nums = new int[]{1,1,2,2,2,3,3,3,4,4,4};
        int k = 4;
        System.out.println(findMajorityElements(nums, k));
    }

    public static List<Integer> findMajorityElements(int[] nums, int k) {
        List<Integer> result = new ArrayList<>();

        if (nums == null || nums.length == 0 || k <= 1) {
            return result;
        }

        int n = nums.length;

        // 第一阶段：找候选人，最多 k - 1 个
        Map<Integer, Integer> candidateMap = new HashMap<>();

        for (int num : nums) {
            if (candidateMap.containsKey(num)) {
                candidateMap.put(num, candidateMap.get(num) + 1);
            } else if (candidateMap.size() < k - 1) {
                candidateMap.put(num, 1);
            } else {
                // 所有候选人的票数都减 1
                List<Integer> removeList = new ArrayList<>();

                for (Map.Entry<Integer, Integer> entry : candidateMap.entrySet()) {
                    int newCount = entry.getValue() - 1;
                    if (newCount == 0) {
                        removeList.add(entry.getKey());
                    } else {
                        entry.setValue(newCount);
                    }
                }

                for (Integer key : removeList) {
                    candidateMap.remove(key);
                }

            }
            System.out.println(candidateMap + " - " + num);
        }

        // 第二阶段：重新统计候选人的真实出现次数
        Map<Integer, Integer> realCountMap = new HashMap<>();

        for (int num : nums) {
            if (candidateMap.containsKey(num)) {
                realCountMap.put(num, realCountMap.getOrDefault(num, 0) + 1);
            }
        }

        // 注意不要直接用 count > n / k，因为整数除法可能有误差
        // 应该用 count * k > n
        for (Map.Entry<Integer, Integer> entry : realCountMap.entrySet()) {
            if (entry.getValue() * k > n) {
                result.add(entry.getKey());
            }
        }

        return result;
    }
}
