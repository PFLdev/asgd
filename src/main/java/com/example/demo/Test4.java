package com.example.demo;

import java.util.UUID;

public class Test4 {
    // nums = [4,5,6,7,0,1,2], target = 0
    public static void main(String[] args) {
        UUID uuid = UUID.randomUUID();
        System.out.println(System.nanoTime());
        System.out.println(System.currentTimeMillis());
    }
    public int search(int[] nums, int target) {
        int left = 0;
        int right = nums.length - 1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (nums[mid] == target) {
                return mid;
            }

            if (nums[left] < nums[mid]) {
                if (target > nums[left] && target < nums[mid]) {
                    right = mid - 1;
                } else {
                    left = mid + 1;
                }
            } else {
                if (target > nums[mid] && target <= nums[right]) {
                    left = mid + 1;
                } else {
                    right = mid - 1;
                }
            }
        }
        return -1;
    }
}
