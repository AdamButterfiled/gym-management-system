package com.gym.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gym.common.Result;
import com.gym.dto.FormPageConfig;
import com.gym.dto.RemoteOptionsRequest;
import com.gym.dto.TableQueryRequest;
import com.gym.service.FormConfigPageService;
import com.gym.service.FormConfigQueryService;
import com.gym.service.MenuPermissionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/form-config")
@CrossOrigin
@PreAuthorize("isAuthenticated()")
public class FormConfigController {
    private static final String FORM_CONFIG_PATH = "/sys/form-config";

    private final FormConfigPageService formConfigPageService;
    private final FormConfigQueryService formConfigQueryService;
    private final MenuPermissionService menuPermissionService;

    public FormConfigController(FormConfigPageService formConfigPageService,
                                FormConfigQueryService formConfigQueryService,
                                MenuPermissionService menuPermissionService) {
        this.formConfigPageService = formConfigPageService;
        this.formConfigQueryService = formConfigQueryService;
        this.menuPermissionService = menuPermissionService;
    }

    @GetMapping("/runtime")
    public Result<FormPageConfig> runtime(@RequestParam String routePath) {
        if (!menuPermissionService.hasRouteAction(routePath, MenuPermissionService.ACTION_VIEW)) {
            return denied("查看页面配置");
        }
        return Result.success(formConfigPageService.getConfigByRoutePath(routePath));
    }

    @GetMapping("/pages")
    public Result<List<FormPageConfig>> pages() {
        if (!menuPermissionService.hasMenuAction(FORM_CONFIG_PATH, MenuPermissionService.ACTION_VIEW)) {
            return denied("查看表单配置");
        }
        return Result.success(formConfigPageService.listPageConfigs());
    }

    @GetMapping("/page/{pageKey}")
    public Result<FormPageConfig> page(@PathVariable String pageKey) {
        if (!menuPermissionService.hasMenuAction(FORM_CONFIG_PATH, MenuPermissionService.ACTION_VIEW)) {
            return denied("查看表单配置");
        }
        return Result.success(formConfigPageService.getConfigByPageKey(pageKey));
    }

    @PostMapping("/page")
    public Result<FormPageConfig> create(@RequestBody FormPageConfig config) {
        if (!menuPermissionService.hasMenuAction(FORM_CONFIG_PATH, MenuPermissionService.ACTION_CREATE)) {
            return denied("新增表单配置");
        }
        return Result.success(formConfigPageService.savePageConfig(config));
    }

    @PutMapping("/page/{pageKey}")
    public Result<FormPageConfig> update(@PathVariable String pageKey, @RequestBody FormPageConfig config) {
        if (!menuPermissionService.hasMenuAction(FORM_CONFIG_PATH, MenuPermissionService.ACTION_UPDATE)) {
            return denied("修改表单配置");
        }
        config.setPageKey(pageKey);
        return Result.success(formConfigPageService.savePageConfig(config));
    }

    @GetMapping("/source-pages")
    public Result<List<Map<String, Object>>> sourcePages() {
        if (!menuPermissionService.hasMenuAction(FORM_CONFIG_PATH, MenuPermissionService.ACTION_VIEW)) {
            return denied("查看来源页面");
        }
        return Result.success(formConfigPageService.listSourcePages());
    }

    @GetMapping("/source-fields")
    public Result<List<Map<String, Object>>> sourceFields(@RequestParam String pageKey) {
        if (!menuPermissionService.hasMenuAction(FORM_CONFIG_PATH, MenuPermissionService.ACTION_VIEW)) {
            return denied("查看来源字段");
        }
        return Result.success(formConfigPageService.listSourceFields(pageKey));
    }

    @PostMapping("/query")
    public Result<Page<?>> query(@RequestBody TableQueryRequest request) {
        if (!menuPermissionService.hasPageAction(
                request.getRoutePath(),
                request.getPageKey(),
                MenuPermissionService.ACTION_VIEW)) {
            return denied("查询页面数据");
        }
        return Result.success(formConfigQueryService.queryPage(request));
    }

    @PostMapping("/remote-options/api")
    public Result<List<Map<String, Object>>> remoteApiOptions(@RequestBody RemoteOptionsRequest request) {
        if (!menuPermissionService.hasPageAction(
                request.getRoutePath(),
                request.getPageKey(),
                MenuPermissionService.ACTION_VIEW)) {
            return denied("加载远程选项");
        }
        return Result.success(formConfigQueryService.loadRemoteApiOptions(request));
    }

    @PostMapping("/remote-options/form")
    public Result<List<Map<String, Object>>> remoteFormOptions(@RequestBody RemoteOptionsRequest request) {
        if (!menuPermissionService.hasPageAction(
                request.getRoutePath(),
                request.getPageKey(),
                MenuPermissionService.ACTION_VIEW)) {
            return denied("加载远程选项");
        }
        return Result.success(formConfigQueryService.loadRemoteFormOptions(request));
    }

    private <T> Result<T> denied(String actionLabel) {
        return Result.error("当前角色无权限执行操作: " + actionLabel);
    }
}
