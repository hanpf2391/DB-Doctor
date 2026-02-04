package com.dbdoctor.controller;

import com.dbdoctor.common.Result;
import com.dbdoctor.entity.AiServiceInstance;
import com.dbdoctor.service.AiServiceInstanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI服务实例管理 Controller
 *
 * REST API：
 * - GET    /api/ai-service-instances              - 获取所有实例
 * - GET    /api/ai-service-instances/{id}          - 根据ID查询
 * - POST   /api/ai-service-instances              - 创建实例
 * - PUT    /api/ai-service-instances/{id}          - 更新实例
 * - DELETE /api/ai-service-instances/{id}          - 删除实例
 * - POST   /api/ai-service-instances/{id}/set-default - 设置默认
 * - PUT    /api/ai-service-instances/{id}/enabled?enabled={true|false} - 启用/禁用
 *
 * @author DB-Doctor
 * @version 3.1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/ai-service-instances")
@RequiredArgsConstructor
public class AiServiceInstanceController {

    private final AiServiceInstanceService service;

    /**
     * 获取所有AI服务实例
     *
     * @return 实例列表
     */
    @GetMapping
    public Result<List<AiServiceInstance>> findAll() {
        log.info("API调用: 获取所有AI服务实例");
        List<AiServiceInstance> instances = service.findAllEnabled();
        return Result.success(instances);
    }

    /**
     * 根据ID查询实例
     *
     * @param id 实例ID
     * @return 实例对象
     */
    @GetMapping("/{id}")
    public Result<AiServiceInstance> findById(@PathVariable Long id) {
        log.info("API调用: 查询AI服务实例: id={}", id);
        AiServiceInstance instance = service.findById(id);
        return Result.success(instance);
    }

    /**
     * 创建AI服务实例
     *
     * @param instance 实例信息
     * @return 创建后的实例
     */
    @PostMapping
    public Result<AiServiceInstance> create(@RequestBody AiServiceInstance instance) {
        log.info("API调用: 创建AI服务实例: instanceName={}", instance.getInstanceName());

        try {
            AiServiceInstance created = service.createInstance(instance);
            return Result.success(created);
        } catch (IllegalArgumentException e) {
            log.error("创建AI服务实例失败: instanceName={}", instance.getInstanceName(), e);
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("创建AI服务实例失败: instanceName={}", instance.getInstanceName(), e);
            return Result.error("创建失败: " + e.getMessage());
        }
    }

    /**
     * 更新AI服务实例
     *
     * @param id 实例ID
     * @param instance 更新后的实例信息
     * @return 更新后的实例
     */
    @PutMapping("/{id}")
    public Result<AiServiceInstance> update(
            @PathVariable Long id,
            @RequestBody AiServiceInstance instance
    ) {
        log.info("API调用: 更新AI服务实例: id={}", id);

        try {
            AiServiceInstance updated = service.updateInstance(id, instance);
            return Result.success(updated);
        } catch (IllegalArgumentException e) {
            log.error("更新AI服务实例失败: id={}", id, e);
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("更新AI服务实例失败: id={}", id, e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    /**
     * 删除AI服务实例
     *
     * @param id 实例ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        log.info("API调用: 删除AI服务实例: id={}", id);

        try {
            service.deleteInstance(id);
            return Result.success();
        } catch (IllegalArgumentException e) {
            log.error("删除AI服务实例失败: id={}", id, e);
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("删除AI服务实例失败: id={}", id, e);
            return Result.error("删除失败: " + e.getMessage());
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
        log.info("API调用: 设置默认AI服务实例: id={}", id);

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
        log.info("API调用: {}AI服务实例: id={}", enabled ? "启用" : "禁用", id);

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
