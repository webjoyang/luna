package com.luna.lunasrv.module.lunaerp.service.production.inspectionorder;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.vo.InspectionOrderPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.vo.InspectionOrderRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.vo.InspectionOrderSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.product.ProductDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.supplier.SupplierDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salescontract.SalesContractDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.inspectionorder.InspectionOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.product.ProductMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.supplier.SupplierMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salescontract.SalesContractMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.dyeingorder.DyeingOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.greigeorder.GreigeOrderMapper;

import com.luna.lunasrv.module.lunaerp.dal.mysql.production.inspectionorder.InspectionOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.inspectionorder.item.InspectionOrderItemMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import cn.hutool.core.collection.CollUtil;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.*;

/**
 * 生产管理-检验单 Service 实现类
 *
 * @author luna
 */
@Service
@Validated
public class InspectionOrderServiceImpl implements InspectionOrderService {

    /**
     * 检验单编号前缀（INS-YYYYMMDD-NNN，ADR-0015）
     */
    private static final String ORDER_NO_PREFIX = "INS-";
    /**
     * 编号数字部分最小补零位数（3 位起，超 999 自然扩位）
     */
    private static final int NO_MIN_DIGITS = 3;
    /**
     * 自动取号撞唯一索引时的最大顺延重试次数
     */
    private static final int MAX_GENERATE_RETRY = 3;
    /**
     * 日期格式化：年月日 8 位
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    /**
     * 编号前缀后的数字部分
     */
    private static final Pattern NO_SUFFIX_NUMBER_PATTERN = Pattern.compile("^\\d+");
    /**
     * 检验单状态常量
     */
    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    /**
     * 检验单默认状态：草稿
     */
    private static final String DEFAULT_STATUS = STATUS_DRAFT;


    @Resource
    private InspectionOrderMapper inspectionOrderMapper;
    @Resource
    private ProductMapper productMapper;
    @Resource
    private SupplierMapper supplierMapper;
    @Resource
    private GreigeOrderMapper greigeOrderMapper;
    @Resource
    private DyeingOrderMapper dyeingOrderMapper;
    @Resource
    private SalesContractMapper salesContractMapper;
    @Resource
    private InspectionOrderItemMapper inspectionOrderItemMapper;

    @Override
    public PageResult<InspectionOrderRespVO> getInspectionOrderPage(InspectionOrderPageReqVO reqVO) {
        LambdaQueryWrapperX<InspectionOrderDO> wrapper = buildPageWrapper(reqVO);
        wrapper.orderByDesc(InspectionOrderDO::getOrderDate);
        PageResult<InspectionOrderDO> pageResult = inspectionOrderMapper.selectPage(reqVO, wrapper);
        PageResult<InspectionOrderRespVO> result = BeanUtils.toBean(pageResult, InspectionOrderRespVO.class);
        if (result != null && CollUtil.isNotEmpty(result.getList())) {
            List<Long> orderIds = result.getList().stream()
                    .map(InspectionOrderRespVO::getId)
                    .filter(Objects::nonNull)
                    .toList();
            Map<Long, Integer> countMap = inspectionOrderItemMapper.selectCountMapByInspectionOrderIds(orderIds);
            for (InspectionOrderRespVO item : result.getList()) {
                item.setRollCount(countMap.getOrDefault(item.getId(), 0));
            }
        }
        return result;
    }


    @Override
    public InspectionOrderRespVO getInspectionOrder(Long id) {
        InspectionOrderDO order = validateInspectionOrderExists(id);
        InspectionOrderRespVO respVO = BeanUtils.toBean(order, InspectionOrderRespVO.class);
        Long count = inspectionOrderItemMapper.selectCountByInspectionOrderId(id);
        respVO.setRollCount(count != null ? count.intValue() : 0);
        return respVO;
    }


    @Override
    public String getNextInspectionOrderNo() {
        return generateOrderNo(LocalDate.now());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createInspectionOrder(InspectionOrderSaveReqVO createReqVO) {
        InspectionOrderDO order = BeanUtils.toBean(createReqVO, InspectionOrderDO.class);
        // 单据日期缺省当天
        if (order.getOrderDate() == null) {
            order.setOrderDate(LocalDate.now());
        }
        // 状态缺省 DRAFT
        if (!StringUtils.hasText(order.getStatus())) {
            order.setStatus(DEFAULT_STATUS);
        }
        // 编号：手填查重；留空自动生成
        if (StringUtils.hasText(order.getCode())) {
            validateOrderCodeUnique(null, order.getCode());
        } else {
            order.setCode(generateOrderNoWithRetry(order.getOrderDate()));
        }
        // 校验关联并回填快照
        fillSnapshotsAndValidateRefs(order, createReqVO);

        inspectionOrderMapper.insert(order);
        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateInspectionOrder(InspectionOrderSaveReqVO updateReqVO) {
        // 校验存在
        InspectionOrderDO exist = validateInspectionOrderExists(updateReqVO.getId());
        // 仅 DRAFT 态可编辑主表字段
        if (!DEFAULT_STATUS.equals(exist.getStatus())) {
            throw exception(INSPECTION_ORDER_STATUS_NOT_ALLOWED);
        }
        InspectionOrderDO updateObj = BeanUtils.toBean(updateReqVO, InspectionOrderDO.class);
        // 主表更新不直接修改状态（状态变更由专用端点处理）
        updateObj.setStatus(null);
        // 编号变更时查重
        if (StringUtils.hasText(updateObj.getCode())) {
            validateOrderCodeUnique(updateObj.getId(), updateObj.getCode());
        } else {
            updateObj.setCode(null); // 不修改编号
        }
        // 校验关联并回填快照
        fillSnapshotsAndValidateRefs(updateObj, updateReqVO);

        inspectionOrderMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateInspectionOrderStatus(Long id, String status) {
        transitionStatus(id, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeInspectionOrder(Long id) {
        InspectionOrderDO order = validateInspectionOrderExists(id);
        if (!STATUS_DRAFT.equals(order.getStatus()) && !STATUS_ACTIVE.equals(order.getStatus())) {
            throw exception(INSPECTION_ORDER_STATUS_NOT_ALLOWED);
        }
        transitionStatus(id, STATUS_COMPLETED);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelInspectionOrder(Long id) {
        transitionStatus(id, STATUS_CANCELLED);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteInspectionOrder(Long id) {
        InspectionOrderDO order = validateInspectionOrderExists(id);
        if (!STATUS_DRAFT.equals(order.getStatus())) {
            throw exception(INSPECTION_ORDER_STATUS_NOT_ALLOWED);
        }
        inspectionOrderMapper.deleteById(id);
        inspectionOrderItemMapper.deleteByInspectionOrderId(id);
    }

    // ==================== 私有方法 ====================

    private void transitionStatus(Long id, String toStatus) {
        validateInspectionOrderExists(id);
        inspectionOrderMapper.update(null, new LambdaUpdateWrapper<InspectionOrderDO>()
                .eq(InspectionOrderDO::getId, id)
                .set(InspectionOrderDO::getStatus, toStatus));
    }


    private InspectionOrderDO validateInspectionOrderExists(Long id) {
        if (id == null) {
            throw exception(INSPECTION_ORDER_NOT_EXISTS);
        }
        InspectionOrderDO order = inspectionOrderMapper.selectById(id);
        if (order == null) {
            throw exception(INSPECTION_ORDER_NOT_EXISTS);
        }
        return order;
    }

    private void validateOrderCodeUnique(Long excludeId, String code) {
        if (!StringUtils.hasText(code)) {
            return;
        }
        InspectionOrderDO exist = inspectionOrderMapper.selectByCodeIncludingDeleted(code);
        if (exist != null && !exist.getId().equals(excludeId)) {
            throw exception(INSPECTION_ORDER_NO_DUPLICATE, code);
        }
    }

    private String generateOrderNoWithRetry(LocalDate orderDate) {
        String lastCandidate = null;
        for (int i = 0; i < MAX_GENERATE_RETRY; i++) {
            String candidate = generateOrderNo(orderDate);
            lastCandidate = candidate;
            if (inspectionOrderMapper.selectByCodeIncludingDeleted(candidate) == null) {
                return candidate;
            }
        }
        throw exception(INSPECTION_ORDER_NO_DUPLICATE, lastCandidate);
    }

    private String generateOrderNo(LocalDate orderDate) {
        LocalDate date = orderDate != null ? orderDate : LocalDate.now();
        String prefix = ORDER_NO_PREFIX + date.format(DATE_FORMATTER) + "-";
        List<InspectionOrderDO> candidates = inspectionOrderMapper.selectListByCodePrefixIncludingDeleted(prefix);
        long maxNum = 0L;
        int maxDigits = NO_MIN_DIGITS;
        for (InspectionOrderDO candidate : candidates) {
            String no = candidate.getCode();
            if (no == null || no.length() <= prefix.length()) {
                continue;
            }
            String suffix = no.substring(prefix.length());
            Matcher matcher = NO_SUFFIX_NUMBER_PATTERN.matcher(suffix);
            if (!matcher.find()) {
                continue;
            }
            String digits = matcher.group();
            long num;
            try {
                num = Long.parseLong(digits);
            } catch (NumberFormatException ignore) {
                continue;
            }
            if (num > maxNum) {
                maxNum = num;
                maxDigits = Math.max(NO_MIN_DIGITS, digits.length());
            }
        }
        if (maxNum == 0L) {
            return prefix + "001";
        }
        return prefix + String.format("%0" + maxDigits + "d", maxNum + 1);
    }

    private void fillSnapshotsAndValidateRefs(InspectionOrderDO order, InspectionOrderSaveReqVO reqVO) {
        // 1. 关联产品（必填）
        ProductDO product = productMapper.selectById(reqVO.getProductId());
        if (product == null) {
            throw exception(INSPECTION_ORDER_REFERENCE_NOT_FOUND, "产品");
        }
        order.setProductCode(product.getCode());
        order.setProductName(product.getName());

        // 2. 承检方（可选）
        if (reqVO.getInspectionFactoryId() != null) {
            SupplierDO supplier = supplierMapper.selectById(reqVO.getInspectionFactoryId());
            if (supplier == null) {
                throw exception(INSPECTION_ORDER_REFERENCE_NOT_FOUND, "承检方");
            }
            order.setInspectionFactoryName(supplier.getCompanyName());
        } else {
            order.setInspectionFactoryName(null);
        }

        // 3. 上游单据（按类型区分）
        if ("GREIGE".equals(order.getType())) {
            order.setDyeingOrderId(null);
            if (reqVO.getGreigeOrderId() != null) {
                if (greigeOrderMapper.selectById(reqVO.getGreigeOrderId()) == null) {
                    throw exception(INSPECTION_ORDER_REFERENCE_NOT_FOUND, "胚布单");
                }
            }
        } else if ("FINISHED".equals(order.getType())) {
            order.setGreigeOrderId(null);
            if (reqVO.getDyeingOrderId() != null) {
                if (dyeingOrderMapper.selectById(reqVO.getDyeingOrderId()) == null) {
                    throw exception(INSPECTION_ORDER_REFERENCE_NOT_FOUND, "染色单");
                }
            }
        }

        // 4. 销售合同（可选）
        if (reqVO.getSalesContractId() != null) {
            SalesContractDO contract = salesContractMapper.selectById(reqVO.getSalesContractId());
            if (contract == null) {
                throw exception(INSPECTION_ORDER_REFERENCE_NOT_FOUND, "销售合同");
            }
            order.setContractNo(contract.getContractNo());
        } else {
            order.setContractNo(null);
        }
    }

    /**
     * 构造分页查询条件：
     * keyword 对 code / product_code / product_name / inspection_factory_name 做 OR 模糊；
     * type / status / inspection_factory_id 等值过滤；
     * order_date 区间过滤。
     */
    private LambdaQueryWrapperX<InspectionOrderDO> buildPageWrapper(InspectionOrderPageReqVO reqVO) {
        LambdaQueryWrapperX<InspectionOrderDO> wrapper = new LambdaQueryWrapperX<>();
        // 等值过滤
        wrapper.eqIfPresent(InspectionOrderDO::getType, reqVO.getType());
        wrapper.eqIfPresent(InspectionOrderDO::getStatus, reqVO.getStatus());
        wrapper.eqIfPresent(InspectionOrderDO::getInspectionFactoryId, reqVO.getInspectionFactoryId());

        // 日期区间过滤（兼容 orderDateStart/End 与 orderDate 数组）
        LocalDate orderStart = reqVO.getOrderDateStart() != null ? reqVO.getOrderDateStart()
                : (reqVO.getOrderDate() != null && reqVO.getOrderDate().length > 0 ? reqVO.getOrderDate()[0] : null);
        LocalDate orderEnd = reqVO.getOrderDateEnd() != null ? reqVO.getOrderDateEnd()
                : (reqVO.getOrderDate() != null && reqVO.getOrderDate().length > 1 ? reqVO.getOrderDate()[1] : null);
        wrapper.betweenIfPresent(InspectionOrderDO::getOrderDate, orderStart, orderEnd);

        // keyword：命中 code / product_code / product_name / inspection_factory_name 之一
        String keyword = reqVO.getKeyword() == null ? "" : reqVO.getKeyword().trim();
        if (!keyword.isEmpty()) {
            String like = "%" + keyword + "%";
            wrapper.and(w -> w
                    .apply("LOWER(code) LIKE LOWER({0})", like)
                    .or().apply("LOWER(product_code) LIKE LOWER({0})", like)
                    .or().apply("LOWER(product_name) LIKE LOWER({0})", like)
                    .or().apply("LOWER(inspection_factory_name) LIKE LOWER({0})", like));
        }
        return wrapper;
    }

}
