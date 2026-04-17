package com.gym.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gym.entity.CourseSchedule;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface CourseScheduleMapper extends BaseMapper<CourseSchedule> {

    @Update("UPDATE gym_course_schedule SET booked_count = booked_count + #{delta} WHERE id = #{id}")
    int adjustBookedCount(@Param("id") Long id, @Param("delta") int delta);
}
