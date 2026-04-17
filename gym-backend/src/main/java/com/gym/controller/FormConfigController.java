package com.gym.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gym.common.Result;
import com.gym.dto.FormPageConfig;
import com.gym.dto.RemoteOptionsRequest;
import com.gym.dto.TableQueryRequest;
import com.gym.service.FormConfigPageService;
import com.gym.service.FormConfigQueryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/form-config")
@CrossOrigin
public class FormConfigController {

    private final FormConfigPageService formConfigPageService;
    private final FormConfigQueryService formConfigQueryService;

    public FormConfigController(FormConfigPageService formConfigPageService,
                                FormConfigQueryService formConfigQueryService) {
        this.formConfigPageService = formConfigPageService;
        this.formConfigQueryService = formConfigQueryService;
    }

    @GetMapping("/runtime")
    public Result<FormPageConfig> runtime(@RequestParam String routePath) {
        return Result.success(formConfigPageService.getConfigByRoutePath(routePath));
    }

    @GetMapping("/pages")
    public Result<List<FormPageConfig>> pages() {
        return Result.success(formConfigPageService.listPageConfigs());
    }

    @GetMapping("/page/{pageKey}")
    public Result<FormPageConfig> page(@PathVariable String pageKey) {
        return Result.success(formConfigPageService.getConfigByPageKey(pageKey));
    }

    @PostMapping("/page")
    public Result<FormPageConfig> create(@RequestBody FormPageConfig config) {
        return Result.success(formConfigPageService.savePageConfig(config));
    }

    @PutMapping("/page/{pageKey}")
    public Result<FormPageConfig> update(@PathVariable String pageKey, @RequestBody FormPageConfig config) {
        config.setPageKey(pageKey);
        return Result.success(formConfigPageService.savePageConfig(config));
    }

    @GetMapping("/source-pages")
    public Result<List<Map<String, Object>>> sourcePages() {
        return Result.success(formConfigPageService.listSourcePages());
    }

    @GetMapping("/source-fields")
    public Result<List<Map<String, Object>>> sourceFields(@RequestParam String pageKey) {
        return Result.success(formConfigPageService.listSourceFields(pageKey));
    }

    @PostMapping("/query")
    public Result<Page<?>> query(@RequestBody TableQueryRequest request) {
        return Result.success(formConfigQueryService.queryPage(request));
    }

    @PostMapping("/remote-options/api")
    public Result<List<Map<String, Object>>> remoteApiOptions(@RequestBody RemoteOptionsRequest request) {
        return Result.success(formConfigQueryService.loadRemoteApiOptions(request));
    }

    @PostMapping("/remote-options/form")
    public Result<List<Map<String, Object>>> remoteFormOptions(@RequestBody RemoteOptionsRequest request) {
        return Result.success(formConfigQueryService.loadRemoteFormOptions(request));
    }
}
