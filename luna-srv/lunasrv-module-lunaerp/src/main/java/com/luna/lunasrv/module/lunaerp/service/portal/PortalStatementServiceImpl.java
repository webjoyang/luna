package com.luna.lunasrv.module.lunaerp.service.portal;

import cn.hutool.core.collection.CollUtil;
import com.luna.lunasrv.framework.common.util.servlet.ServletUtils;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.statement.CustomerStatementDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.statement.CustomerStatementItemDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.statement.CustomerStatementItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.statement.CustomerStatementMapper;
import com.luna.lunasrv.module.lunaerp.portal.controller.portal.statement.vo.*;
import com.luna.lunasrv.module.lunaerp.portal.core.PortalContext;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.starter.annotation.LogRecord;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.luna.lunasrv.framework.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;
import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants.STATEMENT_STATUS_NOT_ALLOW;
import static com.luna.lunasrv.module.lunaerp.enums.portal.PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED;

/**
 * 客户门户 - 对账单服务实现类
 *
 * @author luna
 */
@Slf4j
@Service
@Validated
public class PortalStatementServiceImpl implements PortalStatementService {

    private static final String STATUS_SENT = "SENT";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final Set<String> ALLOWED_PORTAL_STATUSES = Set.of(STATUS_SENT, STATUS_CONFIRMED, STATUS_REJECTED);
    private static final List<String> DEFAULT_PORTAL_STATUSES = List.of(STATUS_SENT, STATUS_CONFIRMED);
    private static final String CONFIRM_TYPE_ONLINE_PORTAL = "ONLINE_PORTAL";

    @Resource
    private CustomerStatementMapper customerStatementMapper;

    @Resource
    private CustomerStatementItemMapper customerStatementItemMapper;

    @Override
    public List<PortalStatementRespVO> getStatementList(Long customerId, String status) {
        Long effectiveCustomerId = resolveEffectiveCustomerId(customerId);
        if (effectiveCustomerId == null) {
            log.warn("[getStatementList][当前请求缺失客户身份（PortalContext 与参数均为空）]");
            throw exception(PORTAL_DATA_ACCESS_DENIED);
        }

        Collection<String> targetStatuses;
        if (StringUtils.hasText(status)) {
            String trimmedStatus = status.trim();
            if (!ALLOWED_PORTAL_STATUSES.contains(trimmedStatus)) {
                log.info("[getStatementList][过滤状态非门户允许状态: status={}]", trimmedStatus);
                return Collections.emptyList();
            }
            targetStatuses = Collections.singletonList(trimmedStatus);
        } else {
            targetStatuses = DEFAULT_PORTAL_STATUSES;
        }

        LambdaQueryWrapperX<CustomerStatementDO> wrapper = new LambdaQueryWrapperX<CustomerStatementDO>()
                .eq(CustomerStatementDO::getCustomerId, effectiveCustomerId)
                .in(CustomerStatementDO::getStatus, targetStatuses)
                .orderByDesc(CustomerStatementDO::getPeriodEnd)
                .orderByDesc(CustomerStatementDO::getId);

        List<CustomerStatementDO> statementList = customerStatementMapper.selectList(wrapper);
        if (CollUtil.isEmpty(statementList)) {
            return Collections.emptyList();
        }

        return statementList.stream()
                .map(this::convertToRespVO)
                .collect(Collectors.toList());
    }

    @Override
    public PortalStatementDetailRespVO getStatementDetail(Long id, String no, Long customerId) {
        boolean hasId = id != null && id > 0;
        boolean hasNo = StringUtils.hasText(no);
        if (!hasId && !hasNo) {
            log.warn("[getStatementDetail][未提供有效的 id 且未提供 no 参数]");
            throw exception(PORTAL_DATA_ACCESS_DENIED);
        }

        Long effectiveCustomerId = resolveEffectiveCustomerId(customerId);
        if (effectiveCustomerId == null) {
            log.warn("[getStatementDetail][当前请求缺失客户身份]");
            throw exception(PORTAL_DATA_ACCESS_DENIED);
        }

        CustomerStatementDO statement;
        if (hasId) {
            statement = customerStatementMapper.selectById(id);
            if (statement != null && hasNo && !Objects.equals(statement.getStatementNo(), no.trim())) {
                log.warn("[getStatementDetail][id 与 no 不一致: id={}, no={}, actualNo={}]", id, no, statement.getStatementNo());
                throw exception(PORTAL_DATA_ACCESS_DENIED);
            }
        } else {
            statement = customerStatementMapper.selectByStatementNo(no.trim());
        }

        // 安全守卫 1: 存在性校验
        if (statement == null) {
            log.warn("[getStatementDetail][对账单不存在: id={}, no={}]", id, no);
            throw exception(PORTAL_DATA_ACCESS_DENIED);
        }

        // 安全守卫 2: 客户归属（横向隔离）
        if (!effectiveCustomerId.equals(statement.getCustomerId())) {
            log.warn("[getStatementDetail][客户归属不匹配: effectiveCustomerId={}, statementCustomerId={}]",
                    effectiveCustomerId, statement.getCustomerId());
            throw exception(PORTAL_DATA_ACCESS_DENIED);
        }

        // 安全守卫 3: 租户归属（跨租户隔离）
        Long currentTenantId = TenantContextHolder.getTenantId();
        if (currentTenantId != null && !currentTenantId.equals(statement.getTenantId())) {
            log.warn("[getStatementDetail][租户归属不匹配: currentTenantId={}, statementTenantId={}]",
                    currentTenantId, statement.getTenantId());
            throw exception(PORTAL_DATA_ACCESS_DENIED);
        }

        // 安全守卫 4: 门户暴露状态守卫（仅允许 SENT, CONFIRMED, REJECTED；DRAFT与VOID不可见）
        if (!ALLOWED_PORTAL_STATUSES.contains(statement.getStatus())) {
            log.warn("[getStatementDetail][状态非门户可见: status={}, id={}]", statement.getStatus(), statement.getId());
            throw exception(PORTAL_DATA_ACCESS_DENIED);
        }

        // 查询对账行明细
        List<CustomerStatementItemDO> itemDOList = customerStatementItemMapper.selectListByStatementId(statement.getId());
        List<PortalStatementItemRespVO> itemVOList = CollUtil.isEmpty(itemDOList)
                ? Collections.emptyList()
                : itemDOList.stream().map(this::convertToItemVO).collect(Collectors.toList());

        return PortalStatementDetailRespVO.builder()
                .id(statement.getId())
                .statementNo(statement.getStatementNo())
                .customerId(statement.getCustomerId())
                .customerName(statement.getCustomerName())
                .currency(statement.getCurrency())
                .periodStart(statement.getPeriodStart())
                .periodEnd(statement.getPeriodEnd())
                .openingBalance(statement.getOpeningBalance())
                .periodDebit(statement.getPeriodDebit())
                .periodCredit(statement.getPeriodCredit())
                .closingBalance(statement.getClosingBalance())
                .status(statement.getStatus())
                .confirmType(statement.getConfirmType())
                .confirmByName(statement.getConfirmByName())
                .confirmTime(statement.getConfirmTime())
                .rejectReason(statement.getRejectReason())
                .remark(statement.getRemark())
                .items(itemVOList)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "客户门户", subType = "对账确认", bizNo = "{{#reqVO.id}}",
            success = "门户客户确认对账单【{{#statementNo}}】：确认人【{{#reqVO.confirmByName}}】")
    public void confirmStatement(PortalStatementConfirmReqVO reqVO) {
        if (reqVO == null || reqVO.getId() == null) {
            log.warn("[confirmStatement][请求参数或单据ID为空]");
            throw exception(PORTAL_DATA_ACCESS_DENIED);
        }
        if (!StringUtils.hasText(reqVO.getConfirmByName())) {
            log.warn("[confirmStatement][确认人姓名为空: id={}]", reqVO.getId());
            throw exception(BAD_REQUEST, "确认人姓名不能为空");
        }

        Long effectiveCustomerId = resolveEffectiveCustomerId(reqVO.getCustomerId());
        if (effectiveCustomerId == null) {
            log.warn("[confirmStatement][当前请求缺失客户身份]");
            throw exception(PORTAL_DATA_ACCESS_DENIED);
        }

        CustomerStatementDO statement = customerStatementMapper.selectById(reqVO.getId());
        if (statement == null) {
            log.warn("[confirmStatement][对账单不存在或已删除: id={}]", reqVO.getId());
            throw exception(PORTAL_DATA_ACCESS_DENIED);
        }

        LogRecordContext.putVariable("statementNo", statement.getStatementNo());

        // 客户归属守卫
        if (!effectiveCustomerId.equals(statement.getCustomerId())) {
            log.warn("[confirmStatement][客户归属不匹配: effectiveCustomerId={}, statementCustomerId={}]",
                    effectiveCustomerId, statement.getCustomerId());
            throw exception(PORTAL_DATA_ACCESS_DENIED);
        }

        // 租户归属守卫
        Long currentTenantId = TenantContextHolder.getTenantId();
        if (currentTenantId != null && !currentTenantId.equals(statement.getTenantId())) {
            log.warn("[confirmStatement][租户归属不匹配: currentTenantId={}, statementTenantId={}]",
                    currentTenantId, statement.getTenantId());
            throw exception(PORTAL_DATA_ACCESS_DENIED);
        }

        // 状态守卫：仅 SENT 允许确认
        if (!STATUS_SENT.equals(statement.getStatus())) {
            log.warn("[confirmStatement][对账单状态不允许确认: status={}, id={}]", statement.getStatus(), reqVO.getId());
            throw exception(STATEMENT_STATUS_NOT_ALLOW);
        }

        CustomerStatementDO updateDO = new CustomerStatementDO();
        updateDO.setId(statement.getId());
        updateDO.setStatus(STATUS_CONFIRMED);
        updateDO.setConfirmType(CONFIRM_TYPE_ONLINE_PORTAL);
        updateDO.setConfirmByName(reqVO.getConfirmByName().trim());
        updateDO.setConfirmTime(LocalDateTime.now());
        updateDO.setConfirmIp(ServletUtils.getClientIP());

        customerStatementMapper.updateById(updateDO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "客户门户", subType = "对账驳回", bizNo = "{{#reqVO.id}}",
            success = "门户客户驳回对账单【{{#statementNo}}】：驳回原因【{{#reqVO.rejectReason}}】")
    public void rejectStatement(PortalStatementRejectReqVO reqVO) {
        if (reqVO == null || reqVO.getId() == null) {
            log.warn("[rejectStatement][请求参数或单据ID为空]");
            throw exception(PORTAL_DATA_ACCESS_DENIED);
        }
        if (!StringUtils.hasText(reqVO.getRejectReason())) {
            log.warn("[rejectStatement][驳回原因为空: id={}]", reqVO.getId());
            throw exception(BAD_REQUEST, "驳回原因不能为空");
        }

        Long effectiveCustomerId = resolveEffectiveCustomerId(reqVO.getCustomerId());
        if (effectiveCustomerId == null) {
            log.warn("[rejectStatement][当前请求缺失客户身份]");
            throw exception(PORTAL_DATA_ACCESS_DENIED);
        }

        CustomerStatementDO statement = customerStatementMapper.selectById(reqVO.getId());
        if (statement == null) {
            log.warn("[rejectStatement][对账单不存在或已删除: id={}]", reqVO.getId());
            throw exception(PORTAL_DATA_ACCESS_DENIED);
        }

        LogRecordContext.putVariable("statementNo", statement.getStatementNo());

        // 客户归属守卫
        if (!effectiveCustomerId.equals(statement.getCustomerId())) {
            log.warn("[rejectStatement][客户归属不匹配: effectiveCustomerId={}, statementCustomerId={}]",
                    effectiveCustomerId, statement.getCustomerId());
            throw exception(PORTAL_DATA_ACCESS_DENIED);
        }

        // 租户归属守卫
        Long currentTenantId = TenantContextHolder.getTenantId();
        if (currentTenantId != null && !currentTenantId.equals(statement.getTenantId())) {
            log.warn("[rejectStatement][租户归属不匹配: currentTenantId={}, statementTenantId={}]",
                    currentTenantId, statement.getTenantId());
            throw exception(PORTAL_DATA_ACCESS_DENIED);
        }

        // 状态守卫：仅 SENT 允许驳回
        if (!STATUS_SENT.equals(statement.getStatus())) {
            log.warn("[rejectStatement][对账单状态不允许驳回: status={}, id={}]", statement.getStatus(), reqVO.getId());
            throw exception(STATEMENT_STATUS_NOT_ALLOW);
        }

        CustomerStatementDO updateDO = new CustomerStatementDO();
        updateDO.setId(statement.getId());
        updateDO.setStatus(STATUS_REJECTED);
        updateDO.setRejectReason(reqVO.getRejectReason().trim());

        customerStatementMapper.updateById(updateDO);

        // 尝试发送站内信通知制单财务（旁路容错，失败绝不阻塞或回滚业务事务）
        try {
            Long creatorUserId = null;
            if (cn.hutool.core.util.StrUtil.isNotBlank(statement.getCreator())) {
                try {
                    creatorUserId = Long.valueOf(statement.getCreator().trim());
                } catch (NumberFormatException ignored) {
                }
            }
            if (creatorUserId != null) {
                com.luna.lunasrv.module.lunaerp.common.notify.LunaNotifyHelper helper =
                        com.luna.lunasrv.module.lunaerp.common.notify.LunaNotifyHelper.getInstance();
                if (helper != null) {
                    helper.sendStatementRejected(creatorUserId, statement.getId(), statement.getStatementNo(),
                            statement.getCustomerName(), reqVO.getRejectReason().trim());
                }
            }
        } catch (Throwable t) {
            log.error("[rejectStatement][站内消息通知发送异常，已容忍错误并不阻塞主业务: id={}]", reqVO.getId(), t);
        }
    }

    private Long resolveEffectiveCustomerId(Long explicitCustomerId) {
        Long portalCustomerId = PortalContext.getCustomerId();
        return portalCustomerId != null ? portalCustomerId : explicitCustomerId;
    }

    private PortalStatementRespVO convertToRespVO(CustomerStatementDO statement) {
        return PortalStatementRespVO.builder()
                .id(statement.getId())
                .statementNo(statement.getStatementNo())
                .customerId(statement.getCustomerId())
                .customerName(statement.getCustomerName())
                .currency(statement.getCurrency())
                .periodStart(statement.getPeriodStart())
                .periodEnd(statement.getPeriodEnd())
                .openingBalance(statement.getOpeningBalance())
                .periodDebit(statement.getPeriodDebit())
                .periodCredit(statement.getPeriodCredit())
                .closingBalance(statement.getClosingBalance())
                .status(statement.getStatus())
                .confirmType(statement.getConfirmType())
                .confirmByName(statement.getConfirmByName())
                .confirmTime(statement.getConfirmTime())
                .rejectReason(statement.getRejectReason())
                .remark(statement.getRemark())
                .build();
    }

    private PortalStatementItemRespVO convertToItemVO(CustomerStatementItemDO item) {
        return PortalStatementItemRespVO.builder()
                .id(item.getId())
                .lineNo(item.getLineNo())
                .direction(item.getDirection())
                .bizDate(item.getBizDate())
                .refType(item.getRefType())
                .refNo(item.getRefNo())
                .summary(item.getSummary())
                .debitAmount(item.getDebitAmount())
                .creditAmount(item.getCreditAmount())
                .signStatus(item.getSignStatus())
                .remark(item.getRemark())
                .build();
    }

}
