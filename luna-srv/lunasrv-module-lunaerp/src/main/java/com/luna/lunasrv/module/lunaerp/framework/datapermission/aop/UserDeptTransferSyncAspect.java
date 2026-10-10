package com.luna.lunasrv.module.lunaerp.framework.datapermission.aop;

import cn.hutool.core.bean.BeanUtil;
import com.luna.lunasrv.framework.datapermission.core.util.DataPermissionUtils;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salescontract.SalesContractMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentMapper;
import com.luna.lunasrv.module.system.api.user.AdminUserApi;
import com.luna.lunasrv.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;

/**
 * 调岗归属同步切面（ADR-0046）
 *
 * <p>核心契约：</p>
 * <ul>
 *   <li>bean 名切点环绕 system 用户更新（{@code bean(adminUserService) && execution(* updateUser(..))}），
 *       不反向依赖 system 模块内部实现类与 Controller VO；</li>
 *   <li>比较 update 前后 {@code dept_id}：未变化直接放行；发生变化则在同一事务内执行 4 表刷列（本票覆盖已存的 3 表，报价待 05 票补挂）；</li>
 *   <li>切面通过 {@link DataPermissionUtils#executeIgnore(Runnable)} 上下文与 Mapper {@code @DataPermission(enable=false)} 执行刷列，
 *       绕过数据权限规则，租户条件由拦截器照加；</li>
 *   <li>owner 列（{@code salesperson_user_id}）不变，SELF 档不受调岗影响；</li>
 *   <li>同事务原子性：任一步骤失败整事务回滚，禁止半刷。</li>
 * </ul>
 *
 * @author luna
 */
@Aspect
@Component
@Slf4j
public class UserDeptTransferSyncAspect {

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private CustomerMapper customerMapper;

    @Resource
    private SalesContractMapper salesContractMapper;

    @Resource
    private ShipmentMapper shipmentMapper;

    private TransactionTemplate transactionTemplate;

    @Autowired(required = false)
    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        if (transactionManager != null) {
            this.transactionTemplate = new TransactionTemplate(transactionManager);
        }
    }

    public void setTransactionTemplate(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    @Around("bean(adminUserService) && execution(* updateUser(..))")
    public Object aroundUpdateUser(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0 || args[0] == null) {
            return joinPoint.proceed();
        }

        Object req = args[0];
        Long userId = extractUserId(req);
        if (userId == null) {
            return joinPoint.proceed();
        }

        Long newDeptId = extractDeptId(req);

        // 1. 获取更新前该用户的原始部门
        AdminUserRespDTO oldUser = adminUserApi.getUser(userId);
        if (oldUser == null) {
            return joinPoint.proceed();
        }

        Long oldDeptId = oldUser.getDeptId();

        // 2. 比较 update 前后 dept_id：未变化直接放行
        if (Objects.equals(oldDeptId, newDeptId)) {
            return joinPoint.proceed();
        }

        log.info("[aroundUpdateUser] 检测到用户 {} 调岗 (dept_id: {} -> {})，将在同一事务内刷新销售归属单据部门",
                userId, oldDeptId, newDeptId);

        // 3. 发生变化：在同一事务内执行用户更新与 4 表刷列（当前覆盖已存的 3 表：客户/合同/发货）
        if (transactionTemplate != null) {
            return transactionTemplate.execute(status -> {
                try {
                    Object result = joinPoint.proceed();
                    syncDeptId(userId, newDeptId);
                    return result;
                } catch (Throwable e) {
                    status.setRollbackOnly();
                    if (e instanceof RuntimeException) {
                        throw (RuntimeException) e;
                    }
                    if (e instanceof Error) {
                        throw (Error) e;
                    }
                    throw new RuntimeException(e);
                }
            });
        } else {
            Object result = joinPoint.proceed();
            syncDeptId(userId, newDeptId);
            return result;
        }
    }

    private void syncDeptId(Long userId, Long newDeptId) {
        DataPermissionUtils.executeIgnore(() -> {
            int custCount = customerMapper.updateDeptIdBySalespersonUserId(userId, newDeptId);
            int contractCount = salesContractMapper.updateDeptIdBySalespersonUserId(userId, newDeptId);
            int shipmentCount = shipmentMapper.updateDeptIdBySalespersonUserId(userId, newDeptId);
            log.info("[aroundUpdateUser] 用户 {} 调岗部门刷新完成: 客户更新 {} 条, 销售合同更新 {} 条, 发货单更新 {} 条",
                    userId, custCount, contractCount, shipmentCount);
        });
    }

    private Long extractUserId(Object req) {
        if (req == null) {
            return null;
        }
        Object idVal = BeanUtil.getProperty(req, "id");
        if (idVal instanceof Number) {
            return ((Number) idVal).longValue();
        }
        if (idVal instanceof String && !((String) idVal).isBlank()) {
            try {
                return Long.parseLong((String) idVal);
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private Long extractDeptId(Object req) {
        if (req == null) {
            return null;
        }
        Object deptVal = BeanUtil.getProperty(req, "deptId");
        if (deptVal instanceof Number) {
            return ((Number) deptVal).longValue();
        }
        if (deptVal instanceof String && !((String) deptVal).isBlank()) {
            try {
                return Long.parseLong((String) deptVal);
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

}
