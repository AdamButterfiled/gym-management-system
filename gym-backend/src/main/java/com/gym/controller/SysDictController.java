package com.gym.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gym.common.Result;
import com.gym.entity.SysDict;
import com.gym.service.SysDictService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/dict")
@CrossOrigin
public class SysDictController {

    @Autowired
    private SysDictService sysDictService;

    // 获取所有字典列表 (管理页面使用)
    @GetMapping("/list")
    public Result<List<SysDict>> list(@RequestParam(defaultValue = "") String dictLabel) {
        LambdaQueryWrapper<SysDict> wrapper = new LambdaQueryWrapper<>();
        if (!dictLabel.isEmpty()) {
            wrapper.like(SysDict::getDictLabel, dictLabel);
        }
        wrapper.orderByAsc(SysDict::getDictType).orderByAsc(SysDict::getSort);
        return Result.success(sysDictService.list(wrapper));
    }

    // 获取所有字典类型列表 (左侧列表专用, 去重)
    @GetMapping("/types")
    public Result<List<String>> getTypes() {
        // 使用 QueryWrapper 选出不重复的 dictType
        // 由于 MyBatis Plus 默认不支持 selectDistinct 简单调用，可以使用 QueryWrapper.select("distinct
        // dict_type")
        LambdaQueryWrapper<SysDict> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(SysDict::getDictType);
        wrapper.groupBy(SysDict::getDictType); // group by 也可以实现去重效果
        wrapper.orderByAsc(SysDict::getDictType);

        List<String> types = sysDictService.listObjs(wrapper, Object::toString);
        return Result.success(types);
    }

    // 根据字典类型获取列表 (前端下拉框使用)
    @GetMapping("/type")
    public Result<List<SysDict>> getByType(@RequestParam String type) {
        LambdaQueryWrapper<SysDict> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDict::getDictType, type);
        wrapper.orderByAsc(SysDict::getSort);
        return Result.success(sysDictService.list(wrapper));
    }

    // 新增或更新
    @PostMapping
    public Result<?> save(@RequestBody SysDict sysDict) {
        return Result.success(sysDictService.saveOrUpdate(sysDict)); // MyBatis Plus saveOrUpdate handles logic based on
                                                                     // ID presence
    }

    // 删除
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        return Result.success(sysDictService.removeById(id));
    }

    // 删除某个类型下的所有数据
    @DeleteMapping("/type")
    public Result<?> deleteType(@RequestParam String type) {
        LambdaQueryWrapper<SysDict> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDict::getDictType, type);
        return Result.success(sysDictService.remove(wrapper));
    }

    // 批量删除
    @DeleteMapping("/batch")
    public Result<?> deleteBatch(@RequestBody List<Long> ids) {
        return Result.success(sysDictService.removeByIds(ids));
    }

    // 批量删除类型
    @DeleteMapping("/type/batch")
    public Result<?> deleteBatchTypes(@RequestBody List<String> types) {
        LambdaQueryWrapper<SysDict> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysDict::getDictType, types);
        return Result.success(sysDictService.remove(wrapper));
    }
}
