package com.gym.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gym.entity.PrivateSchedule;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface PrivateScheduleMapper extends BaseMapper<PrivateSchedule> {

    @Update("UPDATE gym_private_schedule SET booked_count = booked_count + #{delta} WHERE id = #{id}")
    int adjustBookedCount(@Param("id") Long id, @Param("delta") int delta);
}
