# 用户领取优惠券实现计划

> **给 agentic workers：** 必须使用子技能：推荐使用 `superpowers:subagent-driven-development`，也可以使用 `superpowers:executing-plans`，按任务逐项实现。本计划使用复选框（`- [ ]`）跟踪进度。

**目标：** 新增通用优惠券领取 API，保证同一用户同一张券只能领取一次，并且并发下库存不会超发。

**架构：** 新增独立 coupon 模块，沿用现有 Spring Boot + MyBatis 注解 mapper 风格。服务层负责请求校验、优惠券可用性检查，并在事务中通过“原子条件扣减库存 + `(coupon_id, user_id)` 唯一约束”保证并发安全。

**技术栈：** Java 17、Spring Boot 3.3.5、MyBatis 注解 mapper、H2 MySQL 模式测试库、JUnit 5、AssertJ、MockMvc。

---

## 文件结构

- 新建 `src/main/java/com/example/asgd/dto/CouponClaimStatus.java`：领取响应业务状态。
- 新建 `src/main/java/com/example/asgd/dto/CouponClaimRequest.java`：请求体，包含 `couponId` 和 `userId`。
- 新建 `src/main/java/com/example/asgd/dto/CouponClaimResponse.java`：统一响应 DTO 和工厂方法。
- 新建 `src/main/java/com/example/asgd/entity/Coupon.java`：优惠券实体，包含 `enabledAt(LocalDateTime)`。
- 新建 `src/main/java/com/example/asgd/entity/CouponReceiveRecord.java`：优惠券领取记录实体。
- 新建 `src/main/java/com/example/asgd/dao/CouponClaimMapper.java`：MyBatis mapper，负责优惠券查询、库存扣减和领取记录。
- 新建 `src/main/java/com/example/asgd/service/CouponClaimService.java`：服务接口。
- 新建 `src/main/java/com/example/asgd/service/impl/CouponClaimServiceImpl.java`：事务性领取业务逻辑。
- 新建 `src/main/java/com/example/asgd/controller/CouponClaimController.java`：REST 接口。
- 修改 `src/main/resources/db/schema.sql`：新增 coupon 表、索引和测试种子数据。
- 新建 `src/test/java/com/example/asgd/service/impl/CouponClaimServiceImplTest.java`：使用 fake mapper 的服务单元测试。
- 新建 `src/test/java/com/example/asgd/service/impl/CouponClaimServiceTransactionTest.java`：Spring/H2 事务和并发测试。
- 新建 `src/test/java/com/example/asgd/controller/CouponClaimControllerTest.java`：MockMvc 接口测试。

---

### 任务 1：Schema 和 DTO 基础

**文件：**
- 修改：`src/main/resources/db/schema.sql`
- 新建：`src/main/java/com/example/asgd/dto/CouponClaimStatus.java`
- 新建：`src/main/java/com/example/asgd/dto/CouponClaimRequest.java`
- 新建：`src/main/java/com/example/asgd/dto/CouponClaimResponse.java`
- 新建：`src/main/java/com/example/asgd/entity/Coupon.java`
- 新建：`src/main/java/com/example/asgd/entity/CouponReceiveRecord.java`

- [ ] **步骤 1：扩展 schema，新增优惠券表和种子数据**

在 `src/main/resources/db/schema.sql` 现有 insert 语句后追加：

```sql
CREATE TABLE IF NOT EXISTS coupon (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coupon_name VARCHAR(128) NOT NULL,
    total_stock INT NOT NULL,
    available_stock INT NOT NULL,
    status TINYINT NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_coupon_status_time(status, start_time, end_time)
);

CREATE TABLE IF NOT EXISTS coupon_receive_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coupon_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    claim_status VARCHAR(32) NOT NULL,
    claim_time TIMESTAMP NOT NULL,
    create_time TIMESTAMP NOT NULL,
    update_time TIMESTAMP NOT NULL,
    UNIQUE KEY uk_coupon_receive_coupon_user(coupon_id, user_id),
    KEY idx_coupon_receive_user_id(user_id)
);

INSERT INTO coupon (
    id, coupon_name, total_stock, available_stock, status, start_time, end_time
)
VALUES
    (10001, 'Spring Coupon 2026', 10, 10, 1, '2026-01-01 00:00:00', '2026-12-31 23:59:59'),
    (10002, 'Sold Out Coupon 2026', 1, 0, 1, '2026-01-01 00:00:00', '2026-12-31 23:59:59'),
    (10003, 'Disabled Coupon 2026', 10, 10, 0, '2026-01-01 00:00:00', '2026-12-31 23:59:59')
ON DUPLICATE KEY UPDATE
    coupon_name = VALUES(coupon_name),
    total_stock = VALUES(total_stock),
    available_stock = VALUES(available_stock),
    status = VALUES(status),
    start_time = VALUES(start_time),
    end_time = VALUES(end_time);
```

- [ ] **步骤 2：新增领取状态枚举**

创建 `src/main/java/com/example/asgd/dto/CouponClaimStatus.java`：

```java
package com.example.asgd.dto;

public enum CouponClaimStatus {
    SUCCESS,
    ALREADY_CLAIMED,
    SOLD_OUT,
    UNAVAILABLE,
    INVALID_REQUEST
}
```

- [ ] **步骤 3：新增请求 DTO**

创建 `src/main/java/com/example/asgd/dto/CouponClaimRequest.java`：

```java
package com.example.asgd.dto;

public record CouponClaimRequest(
        Long couponId,
        Long userId
) {
}
```

- [ ] **步骤 4：新增响应 DTO**

创建 `src/main/java/com/example/asgd/dto/CouponClaimResponse.java`：

```java
package com.example.asgd.dto;

public record CouponClaimResponse(
        CouponClaimStatus status,
        Long couponId,
        Long userId,
        String message
) {

    public static CouponClaimResponse success(Long couponId, Long userId) {
        return new CouponClaimResponse(CouponClaimStatus.SUCCESS, couponId, userId, "Coupon claimed");
    }

    public static CouponClaimResponse alreadyClaimed(Long couponId, Long userId) {
        return new CouponClaimResponse(CouponClaimStatus.ALREADY_CLAIMED, couponId, userId, "Coupon already claimed");
    }

    public static CouponClaimResponse soldOut(Long couponId, Long userId) {
        return new CouponClaimResponse(CouponClaimStatus.SOLD_OUT, couponId, userId, "Coupon sold out");
    }

    public static CouponClaimResponse unavailable(Long couponId, Long userId) {
        return new CouponClaimResponse(CouponClaimStatus.UNAVAILABLE, couponId, userId, "Coupon is unavailable");
    }

    public static CouponClaimResponse invalidRequest(Long couponId, Long userId) {
        return new CouponClaimResponse(CouponClaimStatus.INVALID_REQUEST, couponId, userId, "Invalid coupon claim request");
    }
}
```

- [ ] **步骤 5：新增优惠券实体**

创建 `src/main/java/com/example/asgd/entity/Coupon.java`：

```java
package com.example.asgd.entity;

import java.time.LocalDateTime;

public record Coupon(
        Long id,
        String couponName,
        Integer totalStock,
        Integer availableStock,
        Integer status,
        LocalDateTime startTime,
        LocalDateTime endTime,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {

    public boolean enabledAt(LocalDateTime now) {
        return Integer.valueOf(1).equals(status) && !now.isBefore(startTime) && !now.isAfter(endTime);
    }
}
```

- [ ] **步骤 6：新增领取记录实体**

创建 `src/main/java/com/example/asgd/entity/CouponReceiveRecord.java`：

```java
package com.example.asgd.entity;

import com.example.asgd.dto.CouponClaimStatus;

import java.time.LocalDateTime;

public record CouponReceiveRecord(
        Long id,
        Long couponId,
        Long userId,
        CouponClaimStatus claimStatus,
        LocalDateTime claimTime,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
```

- [ ] **步骤 7：运行 schema 和编译检查**

运行：`mvn test -DskipTests`

预期：构建成功，并能编译新增 DTO 和实体类。如果 H2 不接受新增 insert 块里的 MySQL `ON DUPLICATE KEY UPDATE`，保留项目现有 schema 风格，只把新增 insert 调整为测试库可接受的写法。

- [ ] **步骤 8：提交基础结构**

```bash
git add src/main/resources/db/schema.sql src/main/java/com/example/asgd/dto/CouponClaimStatus.java src/main/java/com/example/asgd/dto/CouponClaimRequest.java src/main/java/com/example/asgd/dto/CouponClaimResponse.java src/main/java/com/example/asgd/entity/Coupon.java src/main/java/com/example/asgd/entity/CouponReceiveRecord.java
git commit -m "Add coupon claim schema and DTOs"
```

---

### 任务 2：Mapper 和服务单元行为

**文件：**
- 新建：`src/main/java/com/example/asgd/dao/CouponClaimMapper.java`
- 新建：`src/main/java/com/example/asgd/service/CouponClaimService.java`
- 新建：`src/main/java/com/example/asgd/service/impl/CouponClaimServiceImpl.java`
- 新建：`src/test/java/com/example/asgd/service/impl/CouponClaimServiceImplTest.java`

- [ ] **步骤 1：先写服务单元测试**

创建 `src/test/java/com/example/asgd/service/impl/CouponClaimServiceImplTest.java`：

```java
package com.example.asgd.service.impl;

import com.example.asgd.dao.CouponClaimMapper;
import com.example.asgd.dto.CouponClaimRequest;
import com.example.asgd.dto.CouponClaimResponse;
import com.example.asgd.dto.CouponClaimStatus;
import com.example.asgd.entity.Coupon;
import com.example.asgd.entity.CouponReceiveRecord;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CouponClaimServiceImplTest {

    @Test
    void claimCreatesReceiveRecordAndDecrementsStock() {
        FakeMapper mapper = new FakeMapper();
        CouponClaimServiceImpl service = new CouponClaimServiceImpl(mapper, transactionTemplate(), fixedClock());

        CouponClaimResponse response = service.claim(new CouponClaimRequest(10001L, 123456L));

        assertThat(response.status()).isEqualTo(CouponClaimStatus.SUCCESS);
        assertThat(mapper.availableStock).isEqualTo(9);
        assertThat(mapper.receiveRecords).containsKey("10001:123456");
    }

    @Test
    void claimReturnsAlreadyClaimedWithoutDecrementingStockAgain() {
        FakeMapper mapper = new FakeMapper();
        mapper.receiveRecords.put("10001:123456", record(10001L, 123456L));
        CouponClaimServiceImpl service = new CouponClaimServiceImpl(mapper, transactionTemplate(), fixedClock());

        CouponClaimResponse response = service.claim(new CouponClaimRequest(10001L, 123456L));

        assertThat(response.status()).isEqualTo(CouponClaimStatus.ALREADY_CLAIMED);
        assertThat(mapper.availableStock).isEqualTo(10);
    }

    @Test
    void claimReturnsSoldOutWhenStockCannotBeDecremented() {
        FakeMapper mapper = new FakeMapper();
        mapper.availableStock = 0;
        CouponClaimServiceImpl service = new CouponClaimServiceImpl(mapper, transactionTemplate(), fixedClock());

        CouponClaimResponse response = service.claim(new CouponClaimRequest(10001L, 123456L));

        assertThat(response.status()).isEqualTo(CouponClaimStatus.SOLD_OUT);
        assertThat(mapper.receiveRecords).isEmpty();
    }

    @Test
    void claimReturnsUnavailableForDisabledCoupon() {
        FakeMapper mapper = new FakeMapper();
        mapper.couponStatus = 0;
        CouponClaimServiceImpl service = new CouponClaimServiceImpl(mapper, transactionTemplate(), fixedClock());

        CouponClaimResponse response = service.claim(new CouponClaimRequest(10001L, 123456L));

        assertThat(response.status()).isEqualTo(CouponClaimStatus.UNAVAILABLE);
        assertThat(mapper.receiveRecords).isEmpty();
    }

    @Test
    void claimReturnsInvalidRequestForMissingFields() {
        FakeMapper mapper = new FakeMapper();
        CouponClaimServiceImpl service = new CouponClaimServiceImpl(mapper, transactionTemplate(), fixedClock());

        CouponClaimResponse response = service.claim(new CouponClaimRequest(null, 123456L));

        assertThat(response.status()).isEqualTo(CouponClaimStatus.INVALID_REQUEST);
        assertThat(mapper.findCouponCalls).isZero();
    }

    @Test
    void duplicateKeyDuringInsertReturnsAlreadyClaimedAndTransactionRollsBack() {
        FakeMapper mapper = new FakeMapper();
        mapper.throwDuplicateOnInsert = true;
        CouponClaimServiceImpl service = new CouponClaimServiceImpl(mapper, transactionTemplate(), fixedClock());

        CouponClaimResponse response = service.claim(new CouponClaimRequest(10001L, 123456L));

        assertThat(response.status()).isEqualTo(CouponClaimStatus.ALREADY_CLAIMED);
        assertThat(mapper.availableStock).isEqualTo(10);
    }

    private static Clock fixedClock() {
        return Clock.fixed(Instant.parse("2026-06-10T10:00:00Z"), ZoneId.of("Asia/Shanghai"));
    }

    private static TransactionTemplate transactionTemplate() {
        return new TransactionTemplate(new RollbackAwareTransactionManager());
    }

    private static CouponReceiveRecord record(long couponId, long userId) {
        LocalDateTime now = LocalDateTime.of(2026, 6, 10, 18, 0);
        return new CouponReceiveRecord(1L, couponId, userId, CouponClaimStatus.SUCCESS, now, now, now);
    }

    private static class FakeMapper implements CouponClaimMapper {
        private final Map<String, CouponReceiveRecord> receiveRecords = new HashMap<>();
        private int availableStock = 10;
        private int couponStatus = 1;
        private int findCouponCalls;
        private boolean throwDuplicateOnInsert;

        @Override
        public Optional<Coupon> findCouponById(long couponId) {
            findCouponCalls++;
            return Optional.of(new Coupon(
                    couponId,
                    "Spring Coupon 2026",
                    10,
                    availableStock,
                    couponStatus,
                    LocalDateTime.of(2026, 1, 1, 0, 0),
                    LocalDateTime.of(2026, 12, 31, 23, 59, 59),
                    LocalDateTime.of(2026, 1, 1, 0, 0),
                    LocalDateTime.of(2026, 1, 1, 0, 0)
            ));
        }

        @Override
        public Optional<CouponReceiveRecord> findReceiveRecord(long couponId, long userId) {
            return Optional.ofNullable(receiveRecords.get(couponId + ":" + userId));
        }

        @Override
        public int decrementStock(long couponId, LocalDateTime updateTime) {
            if (availableStock <= 0) {
                return 0;
            }
            availableStock--;
            return 1;
        }

        @Override
        public int insertReceiveRecord(CouponReceiveRecord record) {
            if (throwDuplicateOnInsert) {
                receiveRecords.put(record.couponId() + ":" + record.userId(), record);
                availableStock++;
                throw new DuplicateKeyException("duplicate coupon user");
            }
            receiveRecords.put(record.couponId() + ":" + record.userId(), record);
            return 1;
        }
    }

    private static class RollbackAwareTransactionManager implements PlatformTransactionManager {
        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
        }

        @Override
        public void rollback(TransactionStatus status) {
        }
    }
}
```

- [ ] **步骤 2：运行新单元测试，确认先失败**

运行：`mvn test -Dtest=CouponClaimServiceImplTest`

预期：编译失败，因为 `CouponClaimMapper`、`CouponClaimService` 和 `CouponClaimServiceImpl` 还不存在。

- [ ] **步骤 3：新增 mapper 接口**

创建 `src/main/java/com/example/asgd/dao/CouponClaimMapper.java`：

```java
package com.example.asgd.dao;

import com.example.asgd.dto.CouponClaimStatus;
import com.example.asgd.entity.Coupon;
import com.example.asgd.entity.CouponReceiveRecord;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.Optional;

@Mapper
public interface CouponClaimMapper {

    @ConstructorArgs({
            @Arg(column = "id", javaType = Long.class),
            @Arg(column = "coupon_name", javaType = String.class),
            @Arg(column = "total_stock", javaType = Integer.class),
            @Arg(column = "available_stock", javaType = Integer.class),
            @Arg(column = "status", javaType = Integer.class),
            @Arg(column = "start_time", javaType = LocalDateTime.class),
            @Arg(column = "end_time", javaType = LocalDateTime.class),
            @Arg(column = "create_time", javaType = LocalDateTime.class),
            @Arg(column = "update_time", javaType = LocalDateTime.class)
    })
    @Select("""
            SELECT id, coupon_name, total_stock, available_stock, status, start_time, end_time,
                   create_time, update_time
            FROM coupon
            WHERE id = #{couponId}
            """)
    Optional<Coupon> findCouponById(@Param("couponId") long couponId);

    @ConstructorArgs({
            @Arg(column = "id", javaType = Long.class),
            @Arg(column = "coupon_id", javaType = Long.class),
            @Arg(column = "user_id", javaType = Long.class),
            @Arg(column = "claim_status", javaType = CouponClaimStatus.class),
            @Arg(column = "claim_time", javaType = LocalDateTime.class),
            @Arg(column = "create_time", javaType = LocalDateTime.class),
            @Arg(column = "update_time", javaType = LocalDateTime.class)
    })
    @Select("""
            SELECT id, coupon_id, user_id, claim_status, claim_time, create_time, update_time
            FROM coupon_receive_record
            WHERE coupon_id = #{couponId}
              AND user_id = #{userId}
            """)
    Optional<CouponReceiveRecord> findReceiveRecord(
            @Param("couponId") long couponId,
            @Param("userId") long userId
    );

    @Update("""
            UPDATE coupon
            SET available_stock = available_stock - 1,
                update_time = #{updateTime}
            WHERE id = #{couponId}
              AND available_stock > 0
            """)
    int decrementStock(
            @Param("couponId") long couponId,
            @Param("updateTime") LocalDateTime updateTime
    );

    @Insert("""
            INSERT INTO coupon_receive_record (
                coupon_id, user_id, claim_status, claim_time, create_time, update_time
            )
            VALUES (
                #{couponId}, #{userId}, #{claimStatus}, #{claimTime}, #{createTime}, #{updateTime}
            )
            """)
    int insertReceiveRecord(CouponReceiveRecord record);
}
```

- [ ] **步骤 4：新增服务接口**

创建 `src/main/java/com/example/asgd/service/CouponClaimService.java`：

```java
package com.example.asgd.service;

import com.example.asgd.dto.CouponClaimRequest;
import com.example.asgd.dto.CouponClaimResponse;

public interface CouponClaimService {

    CouponClaimResponse claim(CouponClaimRequest request);
}
```

- [ ] **步骤 5：新增服务实现**

创建 `src/main/java/com/example/asgd/service/impl/CouponClaimServiceImpl.java`：

```java
package com.example.asgd.service.impl;

import com.example.asgd.dao.CouponClaimMapper;
import com.example.asgd.dto.CouponClaimRequest;
import com.example.asgd.dto.CouponClaimResponse;
import com.example.asgd.dto.CouponClaimStatus;
import com.example.asgd.entity.Coupon;
import com.example.asgd.entity.CouponReceiveRecord;
import com.example.asgd.service.CouponClaimService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class CouponClaimServiceImpl implements CouponClaimService {

    private final CouponClaimMapper couponClaimMapper;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public CouponClaimServiceImpl(
            CouponClaimMapper couponClaimMapper,
            TransactionTemplate transactionTemplate,
            Clock clock
    ) {
        this.couponClaimMapper = couponClaimMapper;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    @Override
    public CouponClaimResponse claim(CouponClaimRequest request) {
        if (request == null || !positive(request.couponId()) || !positive(request.userId())) {
            return CouponClaimResponse.invalidRequest(
                    request == null ? null : request.couponId(),
                    request == null ? null : request.userId()
            );
        }

        LocalDateTime currentTime = now();
        Optional<Coupon> coupon = couponClaimMapper.findCouponById(request.couponId());
        if (coupon.isEmpty() || !coupon.get().enabledAt(currentTime)) {
            return CouponClaimResponse.unavailable(request.couponId(), request.userId());
        }
        if (couponClaimMapper.findReceiveRecord(request.couponId(), request.userId()).isPresent()) {
            return CouponClaimResponse.alreadyClaimed(request.couponId(), request.userId());
        }

        try {
            return transactionTemplate.execute(status -> claimInTransaction(request, currentTime));
        } catch (DuplicateKeyException ex) {
            return couponClaimMapper.findReceiveRecord(request.couponId(), request.userId())
                    .map(record -> CouponClaimResponse.alreadyClaimed(record.couponId(), record.userId()))
                    .orElseGet(() -> CouponClaimResponse.alreadyClaimed(request.couponId(), request.userId()));
        }
    }

    private CouponClaimResponse claimInTransaction(CouponClaimRequest request, LocalDateTime currentTime) {
        if (couponClaimMapper.findReceiveRecord(request.couponId(), request.userId()).isPresent()) {
            return CouponClaimResponse.alreadyClaimed(request.couponId(), request.userId());
        }

        int updatedRows = couponClaimMapper.decrementStock(request.couponId(), currentTime);
        if (updatedRows == 0) {
            return CouponClaimResponse.soldOut(request.couponId(), request.userId());
        }

        couponClaimMapper.insertReceiveRecord(new CouponReceiveRecord(
                null,
                request.couponId(),
                request.userId(),
                CouponClaimStatus.SUCCESS,
                currentTime,
                currentTime,
                currentTime
        ));
        return CouponClaimResponse.success(request.couponId(), request.userId());
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private static boolean positive(Long value) {
        return value != null && value > 0;
    }
}
```

- [ ] **步骤 6：运行单元测试**

运行：`mvn test -Dtest=CouponClaimServiceImplTest`

预期：全部测试通过。

- [ ] **步骤 7：提交 mapper 和服务**

```bash
git add src/main/java/com/example/asgd/dao/CouponClaimMapper.java src/main/java/com/example/asgd/service/CouponClaimService.java src/main/java/com/example/asgd/service/impl/CouponClaimServiceImpl.java src/test/java/com/example/asgd/service/impl/CouponClaimServiceImplTest.java
git commit -m "Add coupon claim service"
```

---

### 任务 3：Controller API

**文件：**
- 新建：`src/main/java/com/example/asgd/controller/CouponClaimController.java`
- 新建：`src/test/java/com/example/asgd/controller/CouponClaimControllerTest.java`

- [ ] **步骤 1：先写 controller 测试**

创建 `src/test/java/com/example/asgd/controller/CouponClaimControllerTest.java`：

```java
package com.example.asgd.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CouponClaimControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void claimCouponReturnsSuccess() throws Exception {
        mockMvc.perform(post("/api/coupon/claim")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "couponId": 10001,
                                  "userId": 123456
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.couponId").value(10001))
                .andExpect(jsonPath("$.userId").value(123456));
    }

    @Test
    void claimCouponReturnsAlreadyClaimedForDuplicateUser() throws Exception {
        String requestBody = """
                {
                  "couponId": 10001,
                  "userId": 223456
                }
                """;

        mockMvc.perform(post("/api/coupon/claim")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        mockMvc.perform(post("/api/coupon/claim")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ALREADY_CLAIMED"));
    }

    @Test
    void claimCouponReturnsInvalidRequestForMissingCouponId() throws Exception {
        mockMvc.perform(post("/api/coupon/claim")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 323456
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INVALID_REQUEST"));
    }

    @Test
    void claimCouponReturnsSoldOut() throws Exception {
        mockMvc.perform(post("/api/coupon/claim")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "couponId": 10002,
                                  "userId": 423456
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SOLD_OUT"));
    }
}
```

- [ ] **步骤 2：运行 controller 测试，确认先失败**

运行：`mvn test -Dtest=CouponClaimControllerTest`

预期：由于 controller 尚不存在，`/api/coupon/claim` 返回 404，测试失败。

- [ ] **步骤 3：新增 controller**

创建 `src/main/java/com/example/asgd/controller/CouponClaimController.java`：

```java
package com.example.asgd.controller;

import com.example.asgd.dto.CouponClaimRequest;
import com.example.asgd.dto.CouponClaimResponse;
import com.example.asgd.service.CouponClaimService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coupon/claim")
public class CouponClaimController {

    private final CouponClaimService couponClaimService;

    public CouponClaimController(CouponClaimService couponClaimService) {
        this.couponClaimService = couponClaimService;
    }

    @PostMapping
    public CouponClaimResponse claim(@RequestBody CouponClaimRequest request) {
        return couponClaimService.claim(request);
    }
}
```

- [ ] **步骤 4：运行 controller 测试**

运行：`mvn test -Dtest=CouponClaimControllerTest`

预期：全部 controller 测试通过。

- [ ] **步骤 5：提交 controller**

```bash
git add src/main/java/com/example/asgd/controller/CouponClaimController.java src/test/java/com/example/asgd/controller/CouponClaimControllerTest.java
git commit -m "Add coupon claim endpoint"
```

---

### 任务 4：事务和并发验证

**文件：**
- 新建：`src/test/java/com/example/asgd/service/impl/CouponClaimServiceTransactionTest.java`
- 必要时修改：`src/main/java/com/example/asgd/service/impl/CouponClaimServiceImpl.java`

- [ ] **步骤 1：编写事务和并发测试**

创建 `src/test/java/com/example/asgd/service/impl/CouponClaimServiceTransactionTest.java`：

```java
package com.example.asgd.service.impl;

import com.example.asgd.dao.CouponClaimMapper;
import com.example.asgd.dto.CouponClaimRequest;
import com.example.asgd.dto.CouponClaimResponse;
import com.example.asgd.dto.CouponClaimStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CouponClaimServiceTransactionTest {

    @Autowired
    private CouponClaimServiceImpl couponClaimService;

    @Autowired
    private CouponClaimMapper couponClaimMapper;

    @Test
    void concurrentClaimsDoNotExceedStock() throws Exception {
        long couponId = 20001L;
        insertCoupon(couponId, 3);

        List<CouponClaimResponse> responses = runConcurrentClaims(couponId, List.of(
                900001L, 900002L, 900003L, 900004L, 900005L, 900006L
        ));

        assertThat(responses).filteredOn(response -> response.status() == CouponClaimStatus.SUCCESS).hasSize(3);
        assertThat(responses).filteredOn(response -> response.status() == CouponClaimStatus.SOLD_OUT).hasSize(3);
        assertThat(couponClaimMapper.findCouponById(couponId)).get().extracting("availableStock").isEqualTo(0);
    }

    @Test
    void concurrentDuplicateClaimsCreateOneRecordAndConsumeOneStock() throws Exception {
        long couponId = 20002L;
        insertCoupon(couponId, 5);

        List<CouponClaimResponse> responses = runConcurrentClaims(couponId, List.of(
                910001L, 910001L, 910001L, 910001L, 910001L
        ));

        assertThat(responses).filteredOn(response -> response.status() == CouponClaimStatus.SUCCESS).hasSize(1);
        assertThat(responses).filteredOn(response -> response.status() == CouponClaimStatus.ALREADY_CLAIMED).hasSize(4);
        assertThat(couponClaimMapper.findCouponById(couponId)).get().extracting("availableStock").isEqualTo(4);
    }

    private List<CouponClaimResponse> runConcurrentClaims(long couponId, List<Long> userIds) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(userIds.size());
        CountDownLatch ready = new CountDownLatch(userIds.size());
        CountDownLatch start = new CountDownLatch(1);
        List<Future<CouponClaimResponse>> futures = new ArrayList<>();

        for (Long userId : userIds) {
            futures.add(executor.submit(() -> {
                ready.countDown();
                start.await();
                return couponClaimService.claim(new CouponClaimRequest(couponId, userId));
            }));
        }

        ready.await();
        start.countDown();

        List<CouponClaimResponse> responses = new ArrayList<>();
        for (Future<CouponClaimResponse> future : futures) {
            responses.add(future.get());
        }
        executor.shutdown();
        return responses;
    }

    private void insertCoupon(long couponId, int stock) {
        couponClaimMapper.insertCouponForTest(
                couponId,
                "Concurrent Coupon " + couponId,
                stock,
                stock,
                1,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 12, 31, 23, 59, 59),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
```

- [ ] **步骤 2：新增测试用 mapper helper**

在 `src/main/java/com/example/asgd/dao/CouponClaimMapper.java` 接口末尾加入：

```java
    @Insert("""
            INSERT INTO coupon (
                id, coupon_name, total_stock, available_stock, status, start_time, end_time, create_time, update_time
            )
            VALUES (
                #{couponId}, #{couponName}, #{totalStock}, #{availableStock}, #{status},
                #{startTime}, #{endTime}, #{createTime}, #{updateTime}
            )
            """)
    int insertCouponForTest(
            @Param("couponId") long couponId,
            @Param("couponName") String couponName,
            @Param("totalStock") int totalStock,
            @Param("availableStock") int availableStock,
            @Param("status") int status,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("createTime") LocalDateTime createTime,
            @Param("updateTime") LocalDateTime updateTime
    );
```

- [ ] **步骤 3：运行事务测试**

运行：`mvn test -Dtest=CouponClaimServiceTransactionTest`

预期：两个测试都通过。如果重复领取并发测试偶发返回过多 `SOLD_OUT`，检查唯一键异常是否在事务回滚前逃逸；服务行为必须保持“回滚后重新查询已有领取记录”。

- [ ] **步骤 4：提交并发测试**

```bash
git add src/main/java/com/example/asgd/dao/CouponClaimMapper.java src/test/java/com/example/asgd/service/impl/CouponClaimServiceTransactionTest.java
git commit -m "Verify coupon claim concurrency"
```

---

### 任务 5：完整验证和清理

**文件：**
- Review 任务 1-4 创建或修改的所有文件。

- [ ] **步骤 1：运行完整测试套件**

运行：`mvn test`

预期：所有测试通过。

- [ ] **步骤 2：检查 git diff**

运行：`git diff --stat HEAD`

预期：没有意外生成文件，没有 `target/` 下的变更，只包含优惠券功能文件、schema 和测试更新。

- [ ] **步骤 3：可选手动 API smoke test**

启动应用：`mvn spring-boot:run`

然后发送：

```http
POST http://localhost:8080/api/coupon/claim
Content-Type: application/json

{
  "couponId": 10001,
  "userId": 555001
}
```

预期 JSON：

```json
{
  "status": "SUCCESS",
  "couponId": 10001,
  "userId": 555001,
  "message": "Coupon claimed"
}
```

- [ ] **步骤 4：如有清理变更则提交**

如果步骤 1 或步骤 2 需要小修：

```bash
git add src/main/java src/test/java src/main/resources/db/schema.sql
git commit -m "Polish coupon claim implementation"
```

如果没有需要修复的内容，不要创建空提交。

---

## 自检

- 规格覆盖：API 接口、独立 coupon 模块、同一用户同一券只能领取一次、库存控制、业务状态、schema、服务行为、controller 行为和并发验证都映射到了任务 1-4。
- 占位符扫描：没有未解决的占位符，也没有空泛实现说明。
- 类型一致性：`CouponClaimRequest`、`CouponClaimResponse`、`CouponClaimStatus`、`Coupon`、`CouponReceiveRecord`、`CouponClaimMapper` 和 `CouponClaimServiceImpl` 在所有任务中的命名保持一致。
