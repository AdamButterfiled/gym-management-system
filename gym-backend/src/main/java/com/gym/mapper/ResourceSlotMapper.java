package com.gym.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gym.entity.ResourceSlot;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface ResourceSlotMapper extends BaseMapper<ResourceSlot> {

    @Update("""
            UPDATE gym_resource_slot
            SET occupied_count = occupied_count + #{count},
                version = version + 1,
                status = CASE WHEN occupied_count + #{count} >= capacity THEN 'FULL' ELSE 'OPEN' END
            WHERE id = #{slotId}
              AND status IN ('OPEN', 'FULL')
              AND occupied_count + #{count} <= capacity
            """)
    int reserve(@Param("slotId") Long slotId, @Param("count") int count);

    @Update("""
            UPDATE gym_resource_slot
            SET occupied_count = CASE WHEN occupied_count - #{count} < 0 THEN 0 ELSE occupied_count - #{count} END,
                version = version + 1,
                status = 'OPEN'
            WHERE id = #{slotId}
            """)
    int release(@Param("slotId") Long slotId, @Param("count") int count);
}
