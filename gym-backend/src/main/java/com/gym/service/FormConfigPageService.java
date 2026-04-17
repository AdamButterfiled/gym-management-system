package com.gym.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gym.dto.FormPageConfig;
import com.gym.entity.FormConfigPage;

import java.util.List;
import java.util.Map;

public interface FormConfigPageService extends IService<FormConfigPage> {
    FormPageConfig getConfigByPageKey(String pageKey);

    FormPageConfig getConfigByRoutePath(String routePath);

    List<FormPageConfig> listPageConfigs();

    FormPageConfig savePageConfig(FormPageConfig config);

    List<Map<String, Object>> listSourcePages();

    List<Map<String, Object>> listSourceFields(String pageKey);
}
