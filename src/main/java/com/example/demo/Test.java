package com.example.demo;

import java.util.ArrayList;
import java.util.List;

public class Test {
    /**
     * 小赛酷爱游戏，在他卧室的抽屉里有n个游戏币，总面值m，游戏币的设置有1分的，2分的，5分的，
     * 而在小赛所拥有的游戏币中有些面值的游戏币可能没有，求一共有多少种可能的游戏币组合方式？
     * 入参：
     * 输入两个数n(游戏币的个数)，m(总面值)。
     * 输出：
     * 请输出可能的组合列表；
     */

    public static void main(String[] args) {
        List<List<Integer>> list = findList(10, 20);
        for (List<Integer> integers : list) {
            System.out.println(integers.toString());
        }
    }

    public static List<List<Integer>> findList(int n, int m) {
        List<List<Integer>> res = new ArrayList<>();
        for(int z = 0; z <= n; z++) {
            List<Integer> list = new ArrayList<>();
            int y = m - n - 4 * z;
            int x = 2 * n - m + 3 * z;
            System.out.println("x=" + x);
            System.out.println("y=" + y);
            System.out.println("z=" + z);

            if ((x + y + z) > n) {
                continue;
            }

            if (x >= 0 && y >= 0) {

                while (x > 0) {
                    list.add(1);
                    x--;
                }

                while (y > 0) {
                    list.add(2);
                    y--;
                }

                int w = z;
                while (w > 0) {
                    list.add(5);
                    w--;
                }
                res.add(list);
            }
        }
        return res;
    }
}
