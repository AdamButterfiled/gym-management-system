package com.gym.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gym.dto.RemoteOptionsRequest;
import com.gym.dto.TableQueryRequest;

import java.util.List;
import java.util.Map;

public interface FormConfigQueryService {
    Page<?> queryPage(TableQueryRequest request);

    List<Map<String, Object>> loadRemoteApiOptions(RemoteOptionsRequest request);

    List<Map<String, Object>> loadRemoteFormOptions(RemoteOptionsRequest request);
}
