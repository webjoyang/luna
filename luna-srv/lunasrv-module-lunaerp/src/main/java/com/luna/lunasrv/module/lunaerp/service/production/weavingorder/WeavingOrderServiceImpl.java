package com.luna.lunasrv.module.lunaerp.service.production.weavingorder;

import cn.hutool.core.collection.CollUtil;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.vo.WeavingOrderPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.vo.WeavingOrderRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.vo.WeavingOrderSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.vo.WeavingOrderSaveRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.product.ProductDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.greigeorder.GreigeOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.WeavingOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.product.ProductMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.greigeorder.GreigeOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.WeavingOrderMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.*;

/**
 * 生产管理-织造执行-织造单 Service 实现类
 *
 * 核心规则依据 ADR-0022/ADR-0023/ADR-0024：
 * 1. 唯一合法来源：限自织态（productionMode=SELF）且未取消的胚布单；
 * 2. 派生单号：ZZ-{胚布单号去符号}-{2位序号}，租户内全量唯一、软删不释放；
 * 3. 数量软校验：计划匹数超胚布单匹数时提示警告但不拦截；
 * 4. 生命周期：DRAFT 可改可删，删除级联逻辑删除任务与布票；CANCELLED 手动终态。
 *
 * @author luna
 */
@Slf4j
@Service
@Validated
public class WeavingOrderServiceImpl implements WeavingOrderService {

    private static final String DEFAULT_STATUS = "DRAFT";
    private static final int MAX_GENERATE_RETRY = 20;

    @Resource
    private WeavingOrderMapper weavingOrderMapper;

    @Resource
    private GreigeOrderMapper greigeOrderMapper;

    @Resource
    private ProductMapper productMapper;

    @Autowired(required = false)
    private List<WeavingOrderCascadeDeleteListener> cascadeDeleteListeners;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WeavingOrderSaveRespVO createWeavingOrder(WeavingOrderSaveReqVO createReqVO) {
        // 1. 校验胚布单存在且为自织态未取消（1-030-015-003）
        GreigeOrderDO greigeOrder = validateGreigeOrderSelf(createReqVO.getGreigeOrderId());

        // 2. 校验计划匹数合法性（正整数，1-030-015-004）
        if (createReqVO.getPlannedQty() == null || createReqVO.getPlannedQty() <= 0) {
            throw exception(WEAVING_ORDER_PLANNED_QTY_INVALID);
        }

        // 3. 组装 DO 并回填快照
        WeavingOrderDO order = BeanUtils.toBean(createReqVO, WeavingOrderDO.class);
        order.setStatus(DEFAULT_STATUS);
        fillSnapshotsAndDefaults(order, greigeOrder);

        // 4. 编号处理：手填查重（全量唯一含软删行）；留空自动生成（并发撞号顺延重试）
        if (StringUtils.hasText(order.getCode())) {
            validateCodeUnique(order.getCode().trim(), null);
            order.setCode(order.getCode().trim());
        } else {
            order.setCode(generateCodeWithRetry(greigeOrder.getOrderNo()));
        }

        // 5. 软校验超量警告（返回 warning 字段但不拦截保存）
        String warning = calculateWarning(greigeOrder, null, order.getPlannedQty());

        // 6. 落库
        weavingOrderMapper.insert(order);
        return new WeavingOrderSaveRespVO(order.getId(), order.getCode(), warning);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WeavingOrderSaveRespVO updateWeavingOrder(WeavingOrderSaveReqVO updateReqVO) {
        // 1. 存在性校验
        WeavingOrderDO exist = validateWeavingOrderExists(updateReqVO.getId());

        // 2. 状态守卫：仅 DRAFT 态允许修改（1-030-015-002）
        if (!"DRAFT".equals(exist.getStatus())) {
            throw exception(WEAVING_ORDER_STATUS_NOT_ALLOW, "仅待排产（DRAFT）状态的织造单允许修改");
        }

        // 3. 计划匹数校验
        if (updateReqVO.getPlannedQty() == null || updateReqVO.getPlannedQty() <= 0) {
            throw exception(WEAVING_ORDER_PLANNED_QTY_INVALID);
        }

        // 4. 校验关联胚布单（不可随意篡改关联）
        GreigeOrderDO greigeOrder = validateGreigeOrderSelf(exist.getGreigeOrderId());

        // 5. 编号修改查重
        if (StringUtils.hasText(updateReqVO.getCode())) {
            validateCodeUnique(updateReqVO.getCode().trim(), exist.getId());
            exist.setCode(updateReqVO.getCode().trim());
        }

        // 6. 更新可修改字段
        exist.setPlannedQty(updateReqVO.getPlannedQty());
        exist.setPlannedWeight(updateReqVO.getPlannedWeight());
        exist.setComposition(updateReqVO.getComposition());
        exist.setGsm(updateReqVO.getGsm());
        exist.setWidth(updateReqVO.getWidth());
        exist.setRemark(updateReqVO.getRemark());

        // 7. 计算软警告
        String warning = calculateWarning(greigeOrder, exist.getId(), exist.getPlannedQty());

        weavingOrderMapper.updateById(exist);
        return new WeavingOrderSaveRespVO(exist.getId(), exist.getCode(), warning);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWeavingOrder(Long id) {
        // 1. 存在性校验
        WeavingOrderDO exist = validateWeavingOrderExists(id);

        // 2. 状态守卫：仅 DRAFT 态允许删除（1-030-015-002）
        if (!"DRAFT".equals(exist.getStatus())) {
            throw exception(WEAVING_ORDER_STATUS_NOT_ALLOW, "仅待排产（DRAFT）状态的织造单允许删除");
        }

        // 3. 逻辑删除织造单主表
        weavingOrderMapper.deleteById(id);

        // 4. 级联逻辑删除下属机台任务/布票
        if (CollUtil.isNotEmpty(cascadeDeleteListeners)) {
            for (WeavingOrderCascadeDeleteListener listener : cascadeDeleteListeners) {
                listener.onWeavingOrderDeleted(id);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelWeavingOrder(Long id) {
        // 1. 存在性校验
        WeavingOrderDO exist = validateWeavingOrderExists(id);

        // 2. 状态守卫：已取消不能重复取消，已完工不能取消
        if ("CANCELLED".equals(exist.getStatus())) {
            throw exception(WEAVING_ORDER_STATUS_NOT_ALLOW, "织造单已是取消状态，无需重复取消");
        }
        if ("COMPLETED".equals(exist.getStatus())) {
            throw exception(WEAVING_ORDER_STATUS_NOT_ALLOW, "已完工的织造单不允许取消");
        }

        // 3. 更新为 CANCELLED 终态
        WeavingOrderDO updateDO = new WeavingOrderDO();
        updateDO.setId(id);
        updateDO.setStatus("CANCELLED");
        weavingOrderMapper.updateById(updateDO);
    }

    @Override
    public WeavingOrderRespVO getWeavingOrder(Long id) {
        WeavingOrderDO order = validateWeavingOrderExists(id);
        WeavingOrderRespVO respVO = BeanUtils.toBean(order, WeavingOrderRespVO.class);

        // 完工匹数（Issue 03 缺省 0，若单据为 COMPLETED 则为计划匹数）
        respVO.setCompletedQty("COMPLETED".equals(order.getStatus()) ? order.getPlannedQty() : 0);

        // 实时超量警告（非取消态单据返回当前胚布单下是否超量）
        GreigeOrderDO greigeOrder = greigeOrderMapper.selectById(order.getGreigeOrderId());
        if (greigeOrder != null && !"CANCELLED".equals(order.getStatus())) {
            respVO.setWarning(calculateWarning(greigeOrder, null, 0));
        }
        return respVO;
    }

    @Override
    public PageResult<WeavingOrderRespVO> getWeavingOrderPage(WeavingOrderPageReqVO pageReqVO) {
        LambdaQueryWrapperX<WeavingOrderDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eqIfPresent(WeavingOrderDO::getGreigeOrderId, pageReqVO.getGreigeOrderId());
        wrapper.eqIfPresent(WeavingOrderDO::getStatus, pageReqVO.getStatus());
        wrapper.betweenIfPresent(WeavingOrderDO::getCreateTime, pageReqVO.getCreateTime());

        String keyword = pageReqVO.getKeyword() == null ? "" : pageReqVO.getKeyword().trim();
        if (!keyword.isEmpty()) {
            String like = "%" + keyword + "%";
            wrapper.and(w -> w
                    .apply("LOWER(code) LIKE LOWER({0})", like)
                    .or().apply("LOWER(greige_order_no) LIKE LOWER({0})", like)
                    .or().apply("LOWER(contract_no) LIKE LOWER({0})", like)
                    .or().apply("LOWER(product_name) LIKE LOWER({0})", like)
                    .or().apply("LOWER(product_code) LIKE LOWER({0})", like));
        }
        wrapper.orderByDesc(WeavingOrderDO::getId);

        PageResult<WeavingOrderDO> pageResult = weavingOrderMapper.selectPage(pageReqVO, wrapper);
        PageResult<WeavingOrderRespVO> resultVO = BeanUtils.toBean(pageResult, WeavingOrderRespVO.class);
        if (CollUtil.isNotEmpty(resultVO.getList())) {
            for (WeavingOrderRespVO item : resultVO.getList()) {
                item.setCompletedQty("COMPLETED".equals(item.getStatus()) ? item.getPlannedQty() : 0);
            }
        }
        return resultVO;
    }

    @Override
    public String getNextWeavingOrderCode(Long greigeOrderId) {
        GreigeOrderDO greigeOrder = validateGreigeOrderSelf(greigeOrderId);
        return generateCode(greigeOrder.getOrderNo());
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 校验织造单存在，不存在报 1-030-015-000
     */
    private WeavingOrderDO validateWeavingOrderExists(Long id) {
        if (id == null) {
            throw exception(WEAVING_ORDER_NOT_EXISTS);
        }
        WeavingOrderDO order = weavingOrderMapper.selectById(id);
        if (order == null) {
            throw exception(WEAVING_ORDER_NOT_EXISTS);
        }
        return order;
    }

    /**
     * 校验胚布单存在且为自织态未取消（1-030-015-003）
     */
    private GreigeOrderDO validateGreigeOrderSelf(Long greigeOrderId) {
        if (greigeOrderId == null) {
            throw exception(WEAVING_ORDER_GREIGE_ORDER_INVALID);
        }
        GreigeOrderDO greigeOrder = greigeOrderMapper.selectById(greigeOrderId);
        if (greigeOrder == null || Boolean.TRUE.equals(greigeOrder.getDeleted())) {
            throw exception(WEAVING_ORDER_GREIGE_ORDER_INVALID);
        }
        if (!"SELF".equals(greigeOrder.getProductionMode())) {
            throw exception(WEAVING_ORDER_GREIGE_ORDER_INVALID);
        }
        if ("CANCELLED".equals(greigeOrder.getStatus())) {
            throw exception(WEAVING_ORDER_GREIGE_ORDER_INVALID);
        }
        return greigeOrder;
    }

    /**
     * 快照从胚布单与产品档案带入
     */
    private void fillSnapshotsAndDefaults(WeavingOrderDO order, GreigeOrderDO greigeOrder) {
        order.setGreigeOrderNo(greigeOrder.getOrderNo());
        order.setContractNo(greigeOrder.getContractNo());
        order.setProductId(greigeOrder.getProductId());

        ProductDO product = null;
        if (greigeOrder.getProductId() != null) {
            product = productMapper.selectById(greigeOrder.getProductId());
        }

        if (!StringUtils.hasText(order.getProductCode())) {
            order.setProductCode(StringUtils.hasText(greigeOrder.getProductCode())
                    ? greigeOrder.getProductCode() : (product != null ? product.getCode() : null));
        }
        if (!StringUtils.hasText(order.getProductName())) {
            order.setProductName(StringUtils.hasText(greigeOrder.getProductName())
                    ? greigeOrder.getProductName() : (product != null ? product.getName() : null));
        }
        if (!StringUtils.hasText(order.getComposition())) {
            order.setComposition(StringUtils.hasText(greigeOrder.getComposition())
                    ? greigeOrder.getComposition() : (product != null ? product.getComposition() : null));
        }
        if (!StringUtils.hasText(order.getGsm())) {
            order.setGsm(StringUtils.hasText(greigeOrder.getGsm())
                    ? greigeOrder.getGsm() : (product != null && product.getWeightGsm() != null ? product.getWeightGsm().toString() : null));
        }
        if (!StringUtils.hasText(order.getWidth())) {
            order.setWidth(StringUtils.hasText(greigeOrder.getWidth())
                    ? greigeOrder.getWidth() : (product != null && product.getWidthCm() != null ? product.getWidthCm().toString() : null));
        }
    }

    /**
     * 查重单号（全量唯一含软删行，1-030-015-001）
     */
    private void validateCodeUnique(String code, Long excludeId) {
        WeavingOrderDO duplicate = weavingOrderMapper.selectByCodeIncludingDeleted(code);
        if (duplicate != null && (excludeId == null || !duplicate.getId().equals(excludeId))) {
            throw exception(WEAVING_ORDER_CODE_DUPLICATE, code);
        }
    }

    private String buildGreigeCodePrefix(String greigeOrderNo) {
        String cleanNo = greigeOrderNo == null ? "" : greigeOrderNo.replaceAll("[^a-zA-Z0-9]", "");
        return "ZZ-" + cleanNo + "-";
    }

    private int getMaxSequence(String prefix) {
        List<WeavingOrderDO> candidates = weavingOrderMapper.selectListByCodePrefixIncludingDeleted(prefix);
        int maxSeq = 0;
        if (CollUtil.isNotEmpty(candidates)) {
            for (WeavingOrderDO candidate : candidates) {
                String code = candidate.getCode();
                if (code != null && code.startsWith(prefix)) {
                    String seqStr = code.substring(prefix.length());
                    try {
                        int seq = Integer.parseInt(seqStr);
                        if (seq > maxSeq) {
                            maxSeq = seq;
                        }
                    } catch (NumberFormatException ignored) {
                        // 忽略非数字后缀
                    }
                }
            }
        }
        return maxSeq;
    }

    /**
     * 为胚布单派生下一个单号：ZZ-{胚布单号去符号}-{2位序号}
     */
    private String generateCode(String greigeOrderNo) {
        String prefix = buildGreigeCodePrefix(greigeOrderNo);
        int nextSeq = getMaxSequence(prefix) + 1;
        return String.format("%s%02d", prefix, nextSeq);
    }

    /**
     * 生成单号并带有限次顺延重试
     */
    private String generateCodeWithRetry(String greigeOrderNo) {
        String prefix = buildGreigeCodePrefix(greigeOrderNo);
        int maxSeq = getMaxSequence(prefix);
        for (int i = 1; i <= MAX_GENERATE_RETRY; i++) {
            int seq = maxSeq + i;
            String candidate = String.format("%s%02d", prefix, seq);
            if (weavingOrderMapper.selectByCodeIncludingDeleted(candidate) == null) {
                return candidate;
            }
        }
        throw exception(WEAVING_ORDER_CODE_DUPLICATE, "生成单号并发冲突，请稍后重试");
    }

    /**
     * 软校验：聚合该胚布单已有有效织造单 plannedQty，超量警告但允许
     */
    private String calculateWarning(GreigeOrderDO greigeOrder, Long excludeId, int newPlannedQty) {
        if (greigeOrder == null || greigeOrder.getPieceCount() == null) {
            return null;
        }
        List<WeavingOrderDO> activeOrders = weavingOrderMapper.selectActiveListByGreigeOrderId(greigeOrder.getId());
        int existingSum = 0;
        if (CollUtil.isNotEmpty(activeOrders)) {
            for (WeavingOrderDO order : activeOrders) {
                if (excludeId != null && order.getId().equals(excludeId)) {
                    continue;
                }
                if (order.getPlannedQty() != null) {
                    existingSum += order.getPlannedQty();
                }
            }
        }
        int totalPlanned = existingSum + newPlannedQty;
        if (totalPlanned > greigeOrder.getPieceCount()) {
            return String.format("织造单计划匹数合计(%d匹)超出胚布单总匹数(%d匹)", totalPlanned, greigeOrder.getPieceCount());
        }
        return null;
    }

}
