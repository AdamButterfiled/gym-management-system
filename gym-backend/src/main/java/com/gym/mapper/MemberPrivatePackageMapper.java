package com.gym.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gym.entity.MemberPrivatePackage;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface MemberPrivatePackageMapper extends BaseMapper<MemberPrivatePackage> {

    @Update("UPDATE gym_member_private_package SET remaining_sessions = remaining_sessions - 1 WHERE id = #{id} AND remaining_sessions > 0")
    int consumeSession(@Param("id") Long id);

    @Update("UPDATE gym_member_private_package SET remaining_sessions = remaining_sessions + 1 WHERE id = #{id}")
    int restoreSession(@Param("id") Long id);
}
