package com.dbdoctor.controller;

import com.dbdoctor.common.Result;
import com.dbdoctor.entity.DatabaseInstance;
import com.dbdoctor.model.EnvCheckReport;
import com.dbdoctor.service.DatabaseInstanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据库实例管理 Controller
 *
 * REST API：
 * - GET    /api/database-instances           - 获取所有实例
 * - GET    /api/database-instances/{id}       - 根据ID查询
 * - POST   /api/database-instances           - 创建实例
 * - PUT    /api/database-instances/{id}       - 更新实例
 * - DELETE /api/database-instances/{id}       - 删除实例
 * - POST   /api/database-instances/{id}/validate - 验证连接
 * - POST   /api/database-instances/{id}/set-default - 设置默认
 *
 * @author DB-Doctor
 * @version 3.1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/database-instances")
@RequiredArgsConstructor
public class DatabaseInstanceController {

    private final DatabaseInstanceService service;

    /**
     * 获取所有数据库实例（包括禁用的）
     *
     * @return 实例列表
     */
    @GetMapping
    public Result<List<DatabaseInstance>> findAll() {
        log.info("API调用: 获取所有数据库实例");
        List<DatabaseInstance> instances = service.findAll();
        return Result.success(instances);
    }

    /**
     * 根据ID查询实例
     *
     * @param id 实例ID
     * @return 实例对象
     */
    @GetMapping("/{id}")
    public Result<DatabaseInstance> findById(@PathVariable Long id) {
        log.info("API调用: 查询数据库实例: id={}", id);
        DatabaseInstance instance = service.findById(id);
        return Result.success(instance);
    }

    /**
     * 创建数据库实例
     *
     * @param instance 实例信息
     * @return 创建后的实例
     */
    @PostMapping
    public Result<DatabaseInstance> create(@RequestBody DatabaseInstance instance) {
        log.info("API调用: 创建数据库实例: instanceName={}", instance.getInstanceName());

        try {
            DatabaseInstance created = service.createInstance(instance);
            return Result.success(created);
        } catch (IllegalArgumentException e) {
            log.error("创建数据库实例失败: instanceName={}", instance.getInstanceName(), e);
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("创建数据库实例失败: instanceName={}", instance.getInstanceName(), e);
            return Result.error("创建失败: " + e.getMessage());
        }
    }

    /**
     * 更新数据库实例
     *
     * @param id 实例ID
     * @param instance 更新后的实例信息
     * @return 更新后的实例
     */
    @PutMapping("/{id}")
    public Result<DatabaseInstance> update(
            @PathVariable Long id,
            @RequestBody DatabaseInstance instance
    ) {
        log.info("API调用: 更新数据库实例: id={}", id);

        try {
            DatabaseInstance updated = service.updateInstance(id, instance);
            return Result.success(updated);
        } catch (IllegalArgumentException e) {
            log.error("更新数据库实例失败: id={}", id, e);
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("更新数据库实例失败: id={}", id, e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    /**
     * 删除数据库实例
     *
     * @param id 实例ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        log.info("API调用: 删除数据库实例: id={}", id);

        try {
            service.deleteInstance(id);
            return Result.success();
        } catch (IllegalArgumentException e) {
            log.error("删除数据库实例失败: id={}", id, e);
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("删除数据库实例失败: id={}", id, e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    /**
     * 验证数据库连接
     *
     * @param id 实例ID
     * @return 验证报告
     */
    @PostMapping("/{id}/validate")
    public Result<EnvCheckReport> validate(@PathVariable Long id) {
        log.info("API调用: 验证数据库连接: id={}", id);

        try {
            EnvCheckReport report = service.validateConnection(id);
            return Result.success(report);
        } catch (RuntimeException e) {
            log.error("验证数据库连接失败: id={}", id, e);
            return Result.error("验证失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("验证数据库连接失败: id={}", id, e);
            return Result.error("验证失败: " + e.getMessage());
        }
    }

    /**
     * 设置默认实例
     *
     * @param id 实例ID
     * @return 操作结果
     */
    @PostMapping("/{id}/set-default")
    public Result<Void> setDefault(@PathVariable Long id) {
        log.info("API调用: 设置默认数据库实例: id={}", id);

        try {
            service.setDefaultInstance(id);
            return Result.success();
        } catch (IllegalArgumentException e) {
            log.error("设置默认实例失败: id={}", id, e);
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("设置默认实例失败: id={}", id, e);
            return Result.error("设置失败: " + e.getMessage());
        }
    }

    /**
     * 启用/禁用实例
     *
     * @param id 实例ID
     * @param enabled 是否启用
     * @return 操作结果
     */
    @PutMapping("/{id}/enabled")
    public Result<Void> setEnabled(
            @PathVariable Long id,
            @RequestParam boolean enabled
    ) {
        log.info("API调用: {}数据库实例: id={}", enabled ? "启用" : "禁用", id);

        try {
            service.setEnabled(id, enabled);
            return Result.success();
        } catch (IllegalArgumentException e) {
            log.error("更新实例状态失败: id={}", id, e);
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("更新实例状态失败: id={}", id, e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }
}
