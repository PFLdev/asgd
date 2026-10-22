package com.example.mq;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;

import java.nio.charset.StandardCharsets;

public class SyncProducerDemo {

    public static void main(String[] args) throws Exception {
        // 1. 创建 Producer，并指定生产者组
        DefaultMQProducer producer = new DefaultMQProducer("order_producer_group");

        // 2. 设置 NameServer 地址
        producer.setNamesrvAddr("127.0.0.1:9876");

        // 3. 设置发送失败重试次数
        // 表示失败后再重试 2 次，总共最多发送 3 次
        producer.setRetryTimesWhenSendFailed(2);

        // 4. 设置发送超时时间
        producer.setSendMsgTimeout(3000);

        // 5. 启动 Producer
        producer.start();

        try {
            String topic = "ORDER_TOPIC";
            String tag = "ORDER_CREATED";
            String orderNo = "ORDER_202606150001";

            String body = "{"
                    + "\"orderNo\":\"" + orderNo + "\","
                    + "\"userId\":10001,"
                    + "\"amount\":19900"
                    + "}";

            // 6. 创建消息
            // topic：主题
            // tag：消息标签
            // key：业务唯一键，方便查询和幂等处理
            // body：消息体
            Message message = new Message(
                    topic,
                    tag,
                    orderNo,
                    body.getBytes(StandardCharsets.UTF_8)
            );

            // 7. 同步发送消息
            SendResult sendResult = producer.send(message);

            // 8. 判断发送结果
            if (sendResult.getSendStatus() == SendStatus.SEND_OK) {
                System.out.println("消息发送成功");
                System.out.println("msgId = " + sendResult.getMsgId());
                System.out.println("queue = " + sendResult.getMessageQueue());
                System.out.println("queueOffset = " + sendResult.getQueueOffset());
            } else {
                System.out.println("消息发送状态异常：" + sendResult.getSendStatus());
                // 生产环境这里可以记录本地消息表，后续定时补偿重发
            }

        } finally {
            // Demo 可以 shutdown
            // 生产环境中 Producer 一般作为单例 Bean 长期存在，不要每发一次就关闭
            producer.shutdown();
        }
    }
}
