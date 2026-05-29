package com.example.demo;

class Node {
    int val;
    Node next;

    public Node(int val) {
        this.val = val;
    }
}

public class ReverseList {
    public static void main(String[] args) {
        Node head = new Node(1);
        Node cur = head;
        for (int i = 2; i < 9; i++) {
            Node node = new Node(i);
            cur.next = node;
            cur = cur.next;
        }
//        reverse(head, 3);
        Node newHead = reverse(head, 3);

        while (newHead != null) {
            System.out.print(newHead.val + "-");
            newHead = newHead.next;
        }

    }

    public static Node reverse(Node head, int k) {
        Node dummy = new Node(-1);
        dummy.next = head;
        Node pre = dummy;

        while (true) {
            Node tail = pre;
            for (int i = 0; i < k; i++) {
                tail = tail.next;
                if (tail == null) {
                    return dummy.next;
                }
            }

            Node start = pre.next;
            Node end = tail.next;
            reverseSub(start, end);
            pre.next = tail;
            pre = start;
        }
    }

    public static void reverseSub(Node start, Node end) {
        Node prev = end;
        Node cur = start;
        while (cur != end) {
            Node tem = cur.next;
            cur.next = prev;
            prev = cur;
            cur = tem;
        }
    }

    public static Node reverseKGroup(Node head, int k) {
        if (head == null) return null;
        // 区间 [a, b) 包含 k 个待反转元素
        Node a, b;
        a = b = head;
        for (int i = 0; i < k; i++) {
            // 不足 k 个，不需要反转，base case
            if (b == null) return head;
            b = b.next;
        }
        // 反转前 k 个元素
        Node newHead = reverse2(a, b);
        // 递归反转后续链表并连接起来
        a.next = reverseKGroup(b, k);
        return newHead;
    }

    // 反转区间 [a, b) 的元素，注意是左闭右开
    static Node reverse2(Node a, Node b) {
        Node pre, cur, nxt;
        pre = null;
        cur = a;
        nxt = a;
        // while 终止的条件改一下就行了
        while (cur != b) {
            nxt = cur.next;
            cur.next = pre;
            pre = cur;
            cur = nxt;
        }
        // 返回反转后的头结点
        return pre;
    }
}
