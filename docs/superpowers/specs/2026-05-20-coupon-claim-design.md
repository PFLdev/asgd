# 用户领取优惠券设计

## 背景

项目里已经有教育会员权益领取流程，但本功能会新增一个独立的通用优惠券领取模块。第一版聚焦两个核心业务规则：同一用户对同一张优惠券只能领取一次，并且在并发请求下优惠券库存不能超发。

## 目标

- 新增一个独立于教育权益模块的通用优惠券领取 API。
- 保证同一个 `(couponId, userId)` 最多只有一条成功领取记录。
- 原子扣减优惠券库存，保证库存不会变成负数。
- 为领取成功、重复领取、库存不足、优惠券不可用、请求无效提供稳定的业务状态。
- 保持模块足够小，方便后续扩展券类型、用户资格校验、过期策略和领取记录查询。

## 非目标

- 第一版不接入登录认证，`userId` 由请求体传入。
- 不接入外部发券平台。
- 不实现优惠券核销、退款、过期任务或使用记录。
- 不引入新的数据库迁移框架，只更新当前项目已有的 schema 文件。

## API

### 领取优惠券

`POST /api/coupon/claim`

请求：

```json
{
  "couponId": 10001,
  "userId": 123456
}
```

响应：

```json
{
  "status": "SUCCESS",
  "couponId": 10001,
  "userId": 123456,
  "message": "Coupon claimed"
}
```

状态值：

- `SUCCESS`：本次请求领取成功。
- `ALREADY_CLAIMED`：该用户已经领取过这张优惠券。
- `SOLD_OUT`：优惠券存在且可领取，但库存已耗尽。
- `UNAVAILABLE`：优惠券不存在、已禁用，或不在领取时间窗口内。
- `INVALID_REQUEST`：必填请求字段缺失或非法。

## 数据模型

新增 `coupon` 表：

- `id`：主键。
- `coupon_name`：优惠券名称，用于测试和未来 API 展示。
- `total_stock`：初始库存。
- `available_stock`：当前可领取库存。
- `status`：启用或禁用状态。
- `start_time`、`end_time`：领取时间窗口。
- `create_time`、`update_time`：审计时间。

新增 `coupon_receive_record` 表：

- `id`：主键。
- `coupon_id`：被领取的优惠券 ID。
- `user_id`：领取用户 ID。
- `claim_status`：第一版只记录成功领取。
- `claim_time`：领取时间。
- `create_time`、`update_time`：审计时间。

约束和索引：

- `coupon.available_stock` 只能通过原子条件更新扣减。
- 唯一索引 `uk_coupon_receive_coupon_user(coupon_id, user_id)` 防止同一用户重复领取同一张优惠券。
- 索引 `idx_coupon_receive_user_id(user_id)` 支撑未来按用户查询领取记录。

## 组件

- `CouponClaimController`：提供 `POST /api/coupon/claim` REST 接口，控制器保持轻量。
- `CouponClaimService`：优惠券领取业务接口。
- `CouponClaimServiceImpl`：负责请求校验、优惠券可用性检查、重复领取处理、事务内扣减库存和创建领取记录。
- `CouponClaimMapper`：持久化抽象，负责优惠券查询、原子扣减库存、插入领取记录和查询领取记录。
- DTO：
  - `CouponClaimRequest`
  - `CouponClaimResponse`
  - `CouponClaimStatus`
- 实体：
  - `Coupon`
  - `CouponReceiveRecord`

## 领取流程

1. 校验 `couponId` 和 `userId` 是否存在且为正数。
2. 查询优惠券，并校验优惠券已启用且当前时间在领取窗口内。
3. 查询 `(couponId, userId)` 是否已经存在领取记录。
4. 在事务内执行原子库存扣减：

   ```sql
   UPDATE coupon
   SET available_stock = available_stock - 1
   WHERE id = ? AND available_stock > 0
   ```

5. 如果更新影响行数为 `0`，返回 `SOLD_OUT`。
6. 插入 `(couponId, userId)` 领取记录。
7. 如果插入时因为并发重复请求触发唯一键冲突，则回滚事务，并重新查询已有领取记录后返回 `ALREADY_CLAIMED`。
8. 返回 `SUCCESS`。

## 并发保证

库存安全依赖数据库的原子条件更新，而不是“先查库存再扣库存”。当多个请求同时争抢最后几份库存时，只有成功更新一行的请求才能继续创建领取记录。

重复领取安全依赖 `(coupon_id, user_id)` 唯一约束。如果同一用户同时提交多个领取请求，最多只有一个事务能成功插入领取记录。其他事务即使已经扣减库存，也会因为唯一键冲突导致事务回滚，从而撤销本次库存扣减。

## 错误处理

- `couponId` 或 `userId` 缺失、非法时返回 `INVALID_REQUEST`。
- 优惠券不存在、已禁用、未开始或已过期时返回 `UNAVAILABLE`。
- 已存在领取记录时返回 `ALREADY_CLAIMED`。
- 库存扣减影响行数为 `0` 时返回 `SOLD_OUT`。
- 插入领取记录时发生唯一键冲突，按并发重复领取处理，返回 `ALREADY_CLAIMED`，不作为服务端错误。

控制器对业务结果保持 `200 OK`，和当前项目风格一致。请求校验失败也返回统一响应 DTO，不依赖框架异常响应，以保持第一版简单稳定。

## 测试

新增服务测试：

- 首次领取成功时扣减库存并创建领取记录。
- 同一用户重复领取时返回 `ALREADY_CLAIMED`，且不再次扣减库存。
- 库存为 `0` 时返回 `SOLD_OUT`。
- 优惠券禁用、不存在、未开始或已过期时返回 `UNAVAILABLE`。
- 并发领取不会超过库存数量。
- 同一用户并发重复领取只创建一条领取记录，并且只消耗一份库存。

新增控制器测试：

- `POST /api/coupon/claim` 成功响应。
- 重复领取响应。
- 无效请求响应。
- 库存不足响应。

## 已确认问题

- 第一版使用独立的通用优惠券模块，不复用现有教育权益领取模块。
- 同一用户对同一张优惠券只能领取一次。
- 优惠券需要控制库存，并且库存扣减必须支持并发安全。
- 接口形态为 `POST /api/coupon/claim`，请求体包含 `couponId` 和 `userId`。
