package com.example.demo;

import java.util.ArrayList;
import java.util.List;

public class Solution {
    // 数字→字母 预映射，下标对应数字
    private final String[] letterMap = {
            "",     // 0
            "",     // 1
            "abc",  // 2
            "def",  // 3
            "ghi",  // 4
            "jkl",  // 5
            "mno",  // 6
            "pqrs", // 7
            "tuv",  // 8
            "wxyz"  // 9
    };

    public List<String> letterCombinations(String digits) {
        List<String> result = new ArrayList<>();
        // 边界判断：空输入直接返回空集合，面试必写
        if (digits == null || digits.length() == 0) {
            return result;
        }

        // 回溯递归遍历
        backtrack(digits, 0, new StringBuilder(), result);
        return result;
    }

    /**
     * 回溯核心函数
     * @param index 当前正在处理第几位数字
     * @param path  当前已经拼接好的字母路径
     */
    private void backtrack(String digits, int index, StringBuilder path, List<String> result) {
        // 递归终止：所有数字都匹配完，收集结果
        if (index == digits.length()) {
            result.add(path.toString());
            return;
        }

        // 拿到当前位置的数字
        int num = digits.charAt(index) - '0';
        String letters = letterMap[num];

        // 遍历当前数字的所有可选字母
        for (char c : letters.toCharArray()) {
            path.append(c);                 // 1. 做选择
            backtrack(digits, index + 1, path, result); // 2. 递归处理下一位
            path.deleteCharAt(path.length() - 1); // 3. 撤销选择（回溯）
        }
    }
}
