package com.example.asgd.dao;

import com.example.asgd.dto.EduBenefitGrantStatus;
import com.example.asgd.dto.EduBenefitReceiveStatus;
import com.example.asgd.entity.EduBenefitActivity;
import com.example.asgd.entity.EduBenefitGrantTask;
import com.example.asgd.entity.EduBenefitReceiveRecord;
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
public interface EduBenefitClaimMapper {

    @ConstructorArgs({
            @Arg(column = "id", javaType = Long.class),
            @Arg(column = "activity_code", javaType = String.class),
            @Arg(column = "activity_name", javaType = String.class),
            @Arg(column = "benefit_type", javaType = String.class),
            @Arg(column = "benefit_value", javaType = String.class),
            @Arg(column = "start_time", javaType = LocalDateTime.class),
            @Arg(column = "end_time", javaType = LocalDateTime.class),
            @Arg(column = "status", javaType = Integer.class)
    })
    @Select("""
            SELECT id, activity_code, activity_name, benefit_type, benefit_value, start_time, end_time, status
            FROM edu_benefit_activity
            WHERE id = #{activityId}
            """)
    Optional<EduBenefitActivity> findActivityById(@Param("activityId") long activityId);

    @Select("""
            SELECT COUNT(1)
            FROM edu_new_device_whitelist
            WHERE device_id_hash = #{deviceIdHash}
              AND status = 1
            """)
    int existsValidNewDevice(@Param("deviceIdHash") String deviceIdHash);

    @ConstructorArgs({
            @Arg(column = "id", javaType = Long.class),
            @Arg(column = "activity_id", javaType = Long.class),
            @Arg(column = "user_id", javaType = Long.class),
            @Arg(column = "device_id_hash", javaType = String.class),
            @Arg(column = "sn", javaType = String.class),
            @Arg(column = "receive_status", javaType = EduBenefitReceiveStatus.class),
            @Arg(column = "grant_status", javaType = EduBenefitGrantStatus.class),
            @Arg(column = "grant_order_no", javaType = String.class),
            @Arg(column = "receive_time", javaType = LocalDateTime.class),
            @Arg(column = "success_time", javaType = LocalDateTime.class),
            @Arg(column = "fail_code", javaType = String.class),
            @Arg(column = "fail_reason", javaType = String.class),
            @Arg(column = "retry_count", javaType = Integer.class),
            @Arg(column = "next_retry_time", javaType = LocalDateTime.class),
            @Arg(column = "expire_time", javaType = LocalDateTime.class),
            @Arg(column = "create_time", javaType = LocalDateTime.class),
            @Arg(column = "update_time", javaType = LocalDateTime.class)
    })
    @Select("""
            SELECT id, activity_id, user_id, device_id_hash, sn, receive_status, grant_status, grant_order_no,
                   receive_time, success_time, fail_code, fail_reason, retry_count, next_retry_time,
                   expire_time, create_time, update_time
            FROM edu_benefit_receive_record
            WHERE activity_id = #{activityId}
              AND device_id_hash = #{deviceIdHash}
            """)
    Optional<EduBenefitReceiveRecord> findReceiveRecord(
            @Param("activityId") long activityId,
            @Param("deviceIdHash") String deviceIdHash
    );

    @Insert("""
            INSERT INTO edu_benefit_receive_record (
                activity_id, user_id, device_id_hash, sn, receive_status, grant_status, grant_order_no,
                receive_time, success_time, fail_code, fail_reason, retry_count, next_retry_time,
                expire_time, create_time, update_time
            )
            VALUES (
                #{activityId}, #{userId}, #{deviceIdHash}, #{sn}, #{receiveStatus}, #{grantStatus}, #{grantOrderNo},
                #{receiveTime}, #{successTime}, #{failCode}, #{failReason}, #{retryCount}, #{nextRetryTime},
                #{expireTime}, #{createTime}, #{updateTime}
            )
            """)
    int insertReceiveRecord(EduBenefitReceiveRecord record);

    @Insert("""
            INSERT INTO edu_benefit_grant_task (
                grant_order_no, receive_record_id, activity_id, user_id, device_id_hash, benefit_type,
                member_days, status, retry_count, next_retry_time, request_body, response_body,
                fail_reason, create_time, update_time
            )
            VALUES (
                #{grantOrderNo}, #{receiveRecordId}, #{activityId}, #{userId}, #{deviceIdHash}, #{benefitType},
                #{memberDays}, #{status}, #{retryCount}, #{nextRetryTime}, #{requestBody}, #{responseBody},
                #{failReason}, #{createTime}, #{updateTime}
            )
            """)
    int insertGrantTask(EduBenefitGrantTask task);

    @Update("""
            UPDATE edu_benefit_receive_record
            SET grant_status = 'PROCESSING',
                update_time = #{updateTime}
            WHERE grant_order_no = #{grantOrderNo}
              AND grant_status IN ('NEW', 'PROCESSING', 'RETRYING', 'UNKNOWN')
            """)
    int markGrantProcessing(
            @Param("grantOrderNo") String grantOrderNo,
            @Param("updateTime") LocalDateTime updateTime
    );

    @Update("""
            UPDATE edu_benefit_receive_record
            SET grant_status = 'SUCCESS',
                success_time = #{successTime},
                update_time = #{successTime}
            WHERE grant_order_no = #{grantOrderNo}
              AND grant_status IN ('NEW', 'PROCESSING', 'RETRYING', 'UNKNOWN', 'SUCCESS')
            """)
    int markGrantSuccess(
            @Param("grantOrderNo") String grantOrderNo,
            @Param("successTime") LocalDateTime successTime
    );

    @Update("""
            UPDATE edu_benefit_receive_record
            SET grant_status = 'RETRYING',
                fail_reason = #{failReason},
                retry_count = retry_count + 1,
                next_retry_time = #{nextRetryTime},
                update_time = #{updateTime}
            WHERE grant_order_no = #{grantOrderNo}
              AND grant_status IN ('NEW', 'PROCESSING', 'RETRYING', 'UNKNOWN')
            """)
    int markGrantRetrying(
            @Param("grantOrderNo") String grantOrderNo,
            @Param("failReason") String failReason,
            @Param("nextRetryTime") LocalDateTime nextRetryTime,
            @Param("updateTime") LocalDateTime updateTime
    );
}
