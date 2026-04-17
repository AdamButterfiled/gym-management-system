package com.gym.utils;

import com.gym.dto.FormConfigTarget;
import com.gym.dto.FormFieldConfig;
import com.gym.dto.FormFieldLayout;
import com.gym.dto.FormPageConfig;
import com.gym.dto.FormPageQuickSearch;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class FormConfigCompatUtils {

    public static final String MAIN_TABLE_TARGET_KEY = "main-table";
    public static final String MAIN_FORM_TARGET_KEY = "main-form";

    private FormConfigCompatUtils() {
    }

    public static FormPageConfig normalizeForRead(FormPageConfig input) {
        if (input == null) {
            return null;
        }
        FormPageConfig config = input;
        config.setVersion(2);
        normalizeQuickSearch(config);
        if (config.getTargets() == null) {
            config.setTargets(new ArrayList<>());
        }
        if (config.getFields() == null) {
            config.setFields(new ArrayList<>());
        }
        if (config.getRuntimeIssues() == null) {
            config.setRuntimeIssues(new ArrayList<>());
        }
        if (config.getTargets().isEmpty()) {
            config.setTargets(new ArrayList<>(List.of(createFallbackTableTarget(config.getFields()))));
        } else {
            config.getTargets().forEach(FormConfigCompatUtils::normalizeTarget);
        }
        config.setFields(resolveTableFields(config));
        return config;
    }

    public static FormPageConfig normalizeForWrite(FormPageConfig config) {
        FormPageConfig normalized = normalizeForRead(config);
        normalized.setFields(resolveTableFields(normalized));
        if (normalized.getQuickSearch() != null) {
            normalized.setQuickSearchPlaceholder(nullSafe(normalized.getQuickSearch().getPlaceholder()));
            normalized.setQuickSearchFields(new ArrayList<>(normalized.getQuickSearch().getFields()));
            normalized.setDefaultFilterLogic(nullSafe(normalized.getQuickSearch().getDefaultLogic(), "AND"));
        }
        return normalized;
    }

    public static List<FormFieldConfig> resolveTableFields(FormPageConfig config) {
        if (config == null) {
            return List.of();
        }
        if (config.getTargets() != null && !config.getTargets().isEmpty()) {
            FormConfigTarget tableTarget = config.getTargets().stream()
                .filter(target -> "table".equals(target.getTargetType()))
                .findFirst()
                .orElse(config.getTargets().get(0));
            return tableTarget.getFields().stream()
                .map(FormConfigCompatUtils::normalizeField)
                .sorted(Comparator.comparingInt(field -> valueOrDefault(field.getOrder(), field.getColumnOrder(), 0)))
                .toList();
        }
        return config.getFields() == null ? List.of() : config.getFields().stream().map(FormConfigCompatUtils::normalizeField).toList();
    }

    public static void normalizeQuickSearch(FormPageConfig config) {
        FormPageQuickSearch quickSearch = config.getQuickSearch();
        if (quickSearch == null) {
            quickSearch = new FormPageQuickSearch();
        }
        if (quickSearch.getFields() == null || quickSearch.getFields().isEmpty()) {
            quickSearch.setFields(new ArrayList<>(config.getQuickSearchFields() == null ? List.of() : config.getQuickSearchFields()));
        }
        if (quickSearch.getPlaceholder() == null) {
            quickSearch.setPlaceholder(config.getQuickSearchPlaceholder());
        }
        if (quickSearch.getDefaultLogic() == null) {
            quickSearch.setDefaultLogic(nullSafe(config.getDefaultFilterLogic(), "AND"));
        }
        if (quickSearch.getEnabled() == null) {
            quickSearch.setEnabled(!quickSearch.getFields().isEmpty());
        }
        config.setQuickSearch(quickSearch);
        if (config.getQuickSearchFields() == null) {
            config.setQuickSearchFields(new ArrayList<>());
        }
        if (config.getQuickSearchPlaceholder() == null) {
            config.setQuickSearchPlaceholder(quickSearch.getPlaceholder());
        }
        if (config.getDefaultFilterLogic() == null) {
            config.setDefaultFilterLogic(quickSearch.getDefaultLogic());
        }
    }

    public static FormConfigTarget createFallbackTableTarget(List<FormFieldConfig> fields) {
        FormConfigTarget target = new FormConfigTarget();
        target.setTargetKey(MAIN_TABLE_TARGET_KEY);
        target.setTargetType("table");
        target.setTitle("主表格");
        target.setEnabled(Boolean.TRUE);
        List<FormFieldConfig> normalized = new ArrayList<>();
        List<FormFieldConfig> source = fields == null ? List.of() : fields;
        for (int index = 0; index < source.size(); index++) {
            FormFieldConfig field = normalizeField(source.get(index));
            field.setOrder(valueOrDefault(field.getOrder(), field.getColumnOrder(), index));
            field.setColumnOrder(field.getOrder());
            normalized.add(field);
        }
        target.setFields(normalized);
        return target;
    }

    public static void normalizeTarget(FormConfigTarget target) {
        if (target.getEnabled() == null) {
            target.setEnabled(Boolean.TRUE);
        }
        if (target.getFields() == null) {
            target.setFields(new ArrayList<>());
        }
        for (int index = 0; index < target.getFields().size(); index++) {
            FormFieldConfig normalizedField = normalizeField(target.getFields().get(index));
            if (normalizedField.getOrder() == null) {
                normalizedField.setOrder(valueOrDefault(normalizedField.getColumnOrder(), index));
            }
            if (normalizedField.getColumnOrder() == null) {
                normalizedField.setColumnOrder(normalizedField.getOrder());
            }
            target.getFields().set(index, normalizedField);
        }
    }

    public static FormFieldConfig normalizeField(FormFieldConfig input) {
        FormFieldConfig field = input;
        if (field == null) {
            return null;
        }
        if (field.getVisible() == null) {
            field.setVisible(field.getColumnVisible() == null || field.getColumnVisible());
        }
        if (field.getColumnVisible() == null) {
            field.setColumnVisible(field.getVisible());
        }
        if (field.getOrder() == null) {
            field.setOrder(field.getColumnOrder());
        }
        if (field.getColumnOrder() == null) {
            field.setColumnOrder(field.getOrder());
        }
        if (field.getLayout() == null) {
            FormFieldLayout layout = new FormFieldLayout();
            layout.setX(0);
            layout.setY(valueOrDefault(field.getOrder(), field.getColumnOrder(), 0));
            layout.setW(12);
            layout.setH(1);
            field.setLayout(layout);
        } else {
            if (field.getLayout().getX() == null) {
                field.getLayout().setX(0);
            }
            if (field.getLayout().getY() == null) {
                field.getLayout().setY(valueOrDefault(field.getOrder(), field.getColumnOrder(), 0));
            }
            if (field.getLayout().getW() == null) {
                field.getLayout().setW(12);
            }
            if (field.getLayout().getH() == null) {
                field.getLayout().setH(1);
            }
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
        if (field.getRuntimeIssues() == null) {
            field.setRuntimeIssues(new ArrayList<>());
        }
        return field;
    }

    public static String nullSafe(String value) {
        return value == null ? "" : value;
    }

    public static String nullSafe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public static int valueOrDefault(Integer value, Integer fallback) {
        return value == null ? Objects.requireNonNullElse(fallback, 0) : value;
    }

    public static int valueOrDefault(Integer value, Integer fallback, int defaultValue) {
        return value == null ? (fallback == null ? defaultValue : fallback) : value;
    }
}
