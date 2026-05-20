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
