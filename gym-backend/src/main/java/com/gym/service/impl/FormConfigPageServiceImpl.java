package com.gym.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.dto.FormFieldConfig;
import com.gym.dto.FormConfigTarget;
import com.gym.dto.FormPageConfig;
import com.gym.entity.FormConfigPage;
import com.gym.mapper.FormConfigPageMapper;
import com.gym.service.FormConfigPageService;
import com.gym.utils.FormConfigCompatUtils;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
@Service
public class FormConfigPageServiceImpl extends ServiceImpl<FormConfigPageMapper, FormConfigPage> implements FormConfigPageService {

    private final ObjectMapper objectMapper;

    public FormConfigPageServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public FormPageConfig getConfigByPageKey(String pageKey) {
        FormConfigPage entity = getOne(new LambdaQueryWrapper<FormConfigPage>()
            .eq(FormConfigPage::getPageKey, pageKey)
            .last("limit 1"));
        return entity == null ? null : toConfig(entity);
    }

    @Override
    public FormPageConfig getConfigByRoutePath(String routePath) {
        FormConfigPage entity = getOne(new LambdaQueryWrapper<FormConfigPage>()
            .eq(FormConfigPage::getRoutePath, routePath)
            .last("limit 1"));
        return entity == null ? null : toConfig(entity);
    }

    @Override
    public List<FormPageConfig> listPageConfigs() {
        return list(new LambdaQueryWrapper<FormConfigPage>()
            .orderByAsc(FormConfigPage::getPageTitle))
            .stream()
            .map(this::toConfig)
            .toList();
    }

    @Override
    public FormPageConfig savePageConfig(FormPageConfig config) {
        validate(config);
        FormPageConfig normalized = FormConfigCompatUtils.normalizeForWrite(config);
        FormConfigPage entity = getOne(new LambdaQueryWrapper<FormConfigPage>()
            .eq(FormConfigPage::getPageKey, normalized.getPageKey())
            .last("limit 1"));
        if (entity == null) {
            entity = new FormConfigPage();
        }
        entity.setPageKey(normalized.getPageKey());
        entity.setRoutePath(normalized.getRoutePath());
        entity.setPageTitle(normalized.getPageTitle());
        entity.setEnabled(Boolean.TRUE.equals(normalized.getEnabled()));
        entity.setConfigJson(writeConfig(normalized));
        saveOrUpdate(entity);
        return getConfigByPageKey(normalized.getPageKey());
    }

    @Override
    public List<Map<String, Object>> listSourcePages() {
        return listPageConfigs().stream()
            .filter(cfg -> Boolean.TRUE.equals(cfg.getEnabled()))
            .map(cfg -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("pageKey", cfg.getPageKey());
                item.put("routePath", cfg.getRoutePath());
                item.put("pageTitle", cfg.getPageTitle());
                return item;
            })
            .toList();
    }

    @Override
    public List<Map<String, Object>> listSourceFields(String pageKey) {
        FormPageConfig config = getConfigByPageKey(pageKey);
        if (config == null) {
            return List.of();
        }
        return FormConfigCompatUtils.resolveTableFields(config).stream().map(field -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("fieldKey", field.getFieldKey());
            item.put("label", field.getLabel());
            item.put("queryKey", field.getQueryKey());
            item.put("controlType", field.getControlType());
            item.put("filterEnabled", Boolean.TRUE.equals(field.getFilterEnabled()));
            item.put("columnVisible", field.getColumnVisible() == null || field.getColumnVisible());
            return item;
        }).toList();
    }

    private FormPageConfig toConfig(FormConfigPage entity) {
        try {
            FormPageConfig config = objectMapper.readValue(entity.getConfigJson(), FormPageConfig.class);
            if (config.getPageKey() == null) {
                config.setPageKey(entity.getPageKey());
            }
            if (config.getRoutePath() == null) {
                config.setRoutePath(entity.getRoutePath());
            }
            if (config.getPageTitle() == null) {
                config.setPageTitle(entity.getPageTitle());
            }
            if (config.getEnabled() == null) {
                config.setEnabled(entity.getEnabled());
            }
            return FormConfigCompatUtils.normalizeForRead(config);
        } catch (Exception e) {
            throw new IllegalStateException("无法解析表单配置: " + entity.getPageKey(), e);
        }
    }

    private String writeConfig(FormPageConfig config) {
        try {
            return objectMapper.writeValueAsString(config);
        } catch (Exception e) {
            throw new IllegalStateException("无法保存表单配置: " + config.getPageKey(), e);
        }
    }

    private void validate(FormPageConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("配置不能为空");
        }
        if (isBlank(config.getPageKey()) || isBlank(config.getRoutePath()) || isBlank(config.getPageTitle())) {
            throw new IllegalArgumentException("页面键、路由路径和页面标题不能为空");
        }
        if (config.getDefaultFilterLogic() == null) {
            config.setDefaultFilterLogic("AND");
        }
        if (config.getEnabled() == null) {
            config.setEnabled(Boolean.TRUE);
        }
        if (config.getFormInputFollowSystemRadius() == null) {
            config.setFormInputFollowSystemRadius(Boolean.FALSE);
        }
        FormConfigCompatUtils.normalizeQuickSearch(config);
        if (config.getTargets() == null) {
            config.setTargets(List.of());
        }
        if (config.getFields() == null) {
            config.setFields(List.of());
        }
        if (config.getTargets().isEmpty() && config.getFields().isEmpty()) {
            throw new IllegalArgumentException("字段配置不能为空");
        }
        if (!config.getTargets().isEmpty()) {
            config.getTargets().forEach(this::validateTarget);
        }
        config.getFields().forEach(this::validateField);
    }

    private void validateTarget(FormConfigTarget target) {
        if (target == null || isBlank(target.getTargetKey()) || isBlank(target.getTargetType())) {
            throw new IllegalArgumentException("配置目标缺少 targetKey 或 targetType");
        }
        if (target.getEnabled() == null) {
            target.setEnabled(Boolean.TRUE);
        }
        if (target.getFields() == null) {
            throw new IllegalArgumentException("配置目标字段不能为空");
        }
        target.getFields().forEach(this::validateField);
    }

    private void validateField(FormFieldConfig field) {
        if (field == null || isBlank(field.getFieldKey()) || isBlank(field.getLabel()) || isBlank(field.getQueryKey())) {
            throw new IllegalArgumentException("字段 key、label、queryKey 不能为空");
        }
        if (field.getVisible() == null) {
            field.setVisible(field.getColumnVisible() == null || field.getColumnVisible());
        }
        if (field.getOrder() == null) {
            field.setOrder(field.getColumnOrder());
        }
        if (field.getColumnVisible() == null) {
            field.setColumnVisible(field.getVisible() == null || field.getVisible());
        }
        if (field.getFilterEnabled() == null) {
            field.setFilterEnabled(Boolean.FALSE);
        }
        if (field.getQuickSearchEnabled() == null) {
            field.setQuickSearchEnabled(Boolean.FALSE);
        }
        if (field.getAllowMatchModeToggle() == null) {
            field.setAllowMatchModeToggle(Boolean.FALSE);
        }
        if (field.getAllowMultiple() == null) {
            field.setAllowMultiple(Boolean.FALSE);
        }
        if (field.getHiddenButFilterable() == null) {
            field.setHiddenButFilterable(Boolean.FALSE);
        }
        FormConfigCompatUtils.normalizeField(field);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
