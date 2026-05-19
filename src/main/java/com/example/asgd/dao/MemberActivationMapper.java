package com.example.asgd.dao;

import com.example.asgd.entity.MemberActivationRecord;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface MemberActivationMapper {

    @Select("""
            SELECT COUNT(1)
            FROM education_device_model
            WHERE model_code = #{modelCode}
              AND enabled = TRUE
            """)
    int existsEducationModel(@Param("modelCode") String modelCode);

    @Select("""
            SELECT COUNT(1)
            FROM member_activation_record
            WHERE device_id = #{deviceId}
            """)
    int existsActivation(@Param("deviceId") String deviceId);

    @Insert("""
            INSERT INTO member_activation_record (device_id, model_code, user_id, activated_at)
            VALUES (#{deviceId}, #{modelCode}, #{userId}, #{activatedAt})
            """)
    int insertActivation(
            @Param("deviceId") String deviceId,
            @Param("modelCode") String modelCode,
            @Param("userId") String userId,
            @Param("activatedAt") LocalDateTime activatedAt
    );

    @ConstructorArgs({
            @Arg(column = "id", javaType = Long.class),
            @Arg(column = "device_id", javaType = String.class),
            @Arg(column = "model_code", javaType = String.class),
            @Arg(column = "user_id", javaType = String.class),
            @Arg(column = "activated_at", javaType = LocalDateTime.class)
    })
    @Select("""
            <script>
            SELECT id, device_id, model_code, user_id, activated_at
            FROM member_activation_record
            WHERE id &gt; #{lastId}
              AND activated_at &gt;= #{startTime}
              AND activated_at &lt;= #{endTime}
            <if test="modelCode != null">
              AND model_code = #{modelCode}
            </if>
            ORDER BY id
            LIMIT #{limit}
            </script>
            """)
    List<MemberActivationRecord> listActivationRecordsForExport(
            @Param("modelCode") String modelCode,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("lastId") long lastId,
            @Param("limit") int limit
    );
}
