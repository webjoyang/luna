package com.luna.lunasrv.module.lunaerp.service.production.dyeingorder;

import cn.hutool.core.collection.CollUtil;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.production.dyeingorder.vo.DyeingColorRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.dyeingorder.vo.DyeingColorSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.dyeingorder.vo.DyeingOrderPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.dyeingorder.vo.DyeingOrderRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.dyeingorder.vo.DyeingOrderSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.supplier.SupplierDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salescontract.SalesContractDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.dyeingorder.DyeingOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.dyeingorder.color.DyeingColorDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.supplier.SupplierMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salescontract.SalesContractMapper;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.colorinstruction.ColorInstructionDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.colorinstruction.ColorInstructionMapper;
import com.luna.lunasrv.module.lunaerp.service.production.colorinstruction.ColorInstructionService;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.dyeingorder.DyeingOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.dyeingorder.color.DyeingColorMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.DYEING_ORDER_NOT_EXISTS;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.DYEING_ORDER_REFERENCE_NOT_FOUND;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.DYEING_ORDER_STATUS_NOT_ALLOWED;

/**
 * 生产管理-染色单 Service 实现类
 *
 * 染色单无业务编号（ADR-0009），对外标识靠 id + 合同号快照。
 * 状态为四态 DRAFT/ACTIVE/COMPLETED/CANCELLED，缺省 DRAFT（ADR-0008）。
 * 颜色明细（04）随单聚合保存（ADR-0003）。
 *
 * @author luna
 */
@Service
public class DyeingOrderServiceImpl implements DyeingOrderService {

    @Resource
    private DyeingOrderMapper dyeingOrderMapper;

    @Resource
    private DyeingColorMapper dyeingColorMapper;

    @Resource
    private DyeingColorDeleteGuard dyeingColorDeleteGuard;

    @Resource
    private SupplierMapper supplierMapper;

    @Resource
    private SalesContractMapper salesContractMapper;

    @Resource
    private ColorInstructionMapper colorInstructionMapper;

    @Resource
    private ColorInstructionService colorInstructionService;

    @Override
    public PageResult<DyeingOrderRespVO> getDyeingOrderPage(DyeingOrderPageReqVO reqVO) {
        // 构造查询条件
        LambdaQueryWrapperX<DyeingOrderDO> wrapper = buildPageWrapper(reqVO);
        wrapper.orderByDesc(DyeingOrderDO::getOrderDate);
        // 执行查询
        PageResult<DyeingOrderDO> pageResult = dyeingOrderMapper.selectPage(reqVO, wrapper);
        return BeanUtils.toBean(pageResult, DyeingOrderRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDyeingOrder(DyeingOrderSaveReqVO createReqVO) {
        // 1. 引用校验：销售合同必填且存在；承染方供应商必填且存在
        ValidatedRefs refs = validateReferences(createReqVO);
        // 2. 组装 DO
        DyeingOrderDO order = BeanUtils.toBean(createReqVO, DyeingOrderDO.class);
        // 3. 快照回填 + 缺省值（状态缺省 DRAFT、日期缺省当天、快照回填）
        fillSnapshotsAndDefaults(order, refs.contract(), refs.dyeingFactory(), refs.colorInstruction());
        // 4. 保存主表
        dyeingOrderMapper.insert(order);
        // 5. 颜色明细：按提交顺序重排 line_no（从 1 开始），回填 dyeingOrderId（零明细合法）
        List<DyeingColorDO> colorDOList = buildColorDOList(order.getId(), createReqVO.getColors());
        saveColors(order.getId(), colorDOList);
        // 6. 若创建时直接为 ACTIVE 状态且引用打色单，触发投产（DRAFT 态严禁触发）
        if ("ACTIVE".equals(order.getStatus()) && order.getColorInstructionId() != null) {
            colorInstructionService.markInProduction(order.getColorInstructionId());
        }
        return order.getId();
    }

    @Override
    public DyeingOrderRespVO getDyeingOrder(Long id) {
        DyeingOrderDO order = validateDyeingOrderExists(id);
        DyeingOrderRespVO respVO = BeanUtils.toBean(order, DyeingOrderRespVO.class);
        // 嵌套返回颜色行（按 line_no 升序）
        List<DyeingColorDO> colorDOList = dyeingColorMapper.selectListByOrderId(id);
        respVO.setColors(BeanUtils.toBean(colorDOList, DyeingColorRespVO.class));
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDyeingOrder(DyeingOrderSaveReqVO updateReqVO) {
        // 1. 存在性校验：目标单据不存在报 DYEING_ORDER_NOT_EXISTS
        DyeingOrderDO existing = validateDyeingOrderExists(updateReqVO.getId());
        // 2. 状态校验：仅 DRAFT 态可更新；已为 ACTIVE 状态仅允许补填或变更打色单引用（新引用非空且与原引用不同），其他非 DRAFT 态更新报 DYEING_ORDER_STATUS_NOT_ALLOWED
        boolean isDraft = "DRAFT".equals(existing.getStatus());
        boolean isActive = "ACTIVE".equals(existing.getStatus());
        boolean isColorInstructionPatch = isActive
                && updateReqVO.getColorInstructionId() != null
                && !Objects.equals(updateReqVO.getColorInstructionId(), existing.getColorInstructionId());
        if (!isDraft && !isColorInstructionPatch) {
            throw exception(DYEING_ORDER_STATUS_NOT_ALLOWED);
        }
        // 3. 引用校验：销售合同必填且存在；承染方供应商必填且存在；打色单可空且校验合法性
        ValidatedRefs refs = validateReferences(updateReqVO);
        // 4. 组装 DO
        DyeingOrderDO order = BeanUtils.toBean(updateReqVO, DyeingOrderDO.class);
        order.setId(existing.getId());
        // 5. 保持原有状态，保留原有单据日期（若未传），刷新快照与缺省值
        order.setStatus(existing.getStatus());
        if (order.getOrderDate() == null) {
            order.setOrderDate(existing.getOrderDate());
        }
        fillSnapshotsAndDefaults(order, refs.contract(), refs.dyeingFactory(), refs.colorInstruction());
        // 6. 整体更新（可空字段清空随 DO 的 updateStrategy=ALWAYS 一并落库）
        dyeingOrderMapper.updateById(order);
        // 7. 颜色明细差异保存（ADR-0032）：
        //    带 id 且属于本单据的行走 update、无 id 行走 insert、本单据库内有但本次未提交的行走逻辑删除；
        //    删除前触发删除守卫校验（Stage 2 扩展点）；提交行 id 不属于本单据时报错；行号按提交顺序重排。
        if (isDraft || updateReqVO.getColors() != null) {
            diffSaveColors(updateReqVO.getId(), updateReqVO.getColors());
        }
        // 8. 已为 ACTIVE 状态补填或变更打色单引用时（新引用非空且与原引用不同），触发投产
        if (isColorInstructionPatch) {
            colorInstructionService.markInProduction(updateReqVO.getColorInstructionId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void effectDyeingOrder(Long id) {
        DyeingOrderDO order = transitionStatus(id, "DRAFT", "ACTIVE");
        if (order.getColorInstructionId() != null) {
            colorInstructionService.markInProduction(order.getColorInstructionId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeDyeingOrder(Long id) {
        transitionStatus(id, "ACTIVE", "COMPLETED");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelDyeingOrder(Long id) {
        transitionStatus(id, "ACTIVE", "CANCELLED");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDyeingOrder(Long id) {
        DyeingOrderDO order = validateDyeingOrderExists(id);
        if (!"DRAFT".equals(order.getStatus())) {
            throw exception(DYEING_ORDER_STATUS_NOT_ALLOWED);
        }
        dyeingOrderMapper.deleteById(id);
        dyeingColorMapper.deleteByOrderId(id);
    }

    /**
     * 执行单据状态流转校验与单字段更新
     *
     * @param id         染色单ID
     * @param fromStatus 前置合法状态
     * @param toStatus   流转目标状态
     * @return 更新前的单据 DO
     */
    private DyeingOrderDO transitionStatus(Long id, String fromStatus, String toStatus) {
        DyeingOrderDO order = validateDyeingOrderExists(id);
        if (!fromStatus.equals(order.getStatus())) {
            throw exception(DYEING_ORDER_STATUS_NOT_ALLOWED);
        }
        dyeingOrderMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<DyeingOrderDO>()
                .eq(DyeingOrderDO::getId, id)
                .set(DyeingOrderDO::getStatus, toStatus));
        return order;
    }

    /**
     * 构造分页查询条件：
     * keyword 对 contract_no / client_name / product_code / product_name 做 OR 模糊
     * （PostgreSQL 用 LOWER(...) LIKE LOWER({0}) 实现 ILIKE 语义，占位符走预编译）；
     * status / customer_id / dyeing_factory_id 等值过滤；
     * order_date / delivery_date 区间过滤。
     */
    private LambdaQueryWrapperX<DyeingOrderDO> buildPageWrapper(DyeingOrderPageReqVO reqVO) {
        LambdaQueryWrapperX<DyeingOrderDO> wrapper = new LambdaQueryWrapperX<>();
        // 等值过滤
        wrapper.eqIfPresent(DyeingOrderDO::getStatus, reqVO.getStatus());
        wrapper.eqIfPresent(DyeingOrderDO::getCustomerId, reqVO.getCustomerId());
        wrapper.eqIfPresent(DyeingOrderDO::getDyeingFactoryId, reqVO.getDyeingFactoryId());

        // 日期区间过滤（兼容 orderDateStart/End 与 orderDate 数组）
        LocalDate orderStart = reqVO.getOrderDateStart() != null ? reqVO.getOrderDateStart()
                : (reqVO.getOrderDate() != null && reqVO.getOrderDate().length > 0 ? reqVO.getOrderDate()[0] : null);
        LocalDate orderEnd = reqVO.getOrderDateEnd() != null ? reqVO.getOrderDateEnd()
                : (reqVO.getOrderDate() != null && reqVO.getOrderDate().length > 1 ? reqVO.getOrderDate()[1] : null);
        wrapper.betweenIfPresent(DyeingOrderDO::getOrderDate, orderStart, orderEnd);

        LocalDate deliveryStart = reqVO.getDeliveryDateStart() != null ? reqVO.getDeliveryDateStart()
                : (reqVO.getDeliveryDate() != null && reqVO.getDeliveryDate().length > 0 ? reqVO.getDeliveryDate()[0] : null);
        LocalDate deliveryEnd = reqVO.getDeliveryDateEnd() != null ? reqVO.getDeliveryDateEnd()
                : (reqVO.getDeliveryDate() != null && reqVO.getDeliveryDate().length > 1 ? reqVO.getDeliveryDate()[1] : null);
        wrapper.betweenIfPresent(DyeingOrderDO::getDeliveryDate, deliveryStart, deliveryEnd);

        // keyword：命中 contract_no / client_name / product_code / product_name 之一
        String keyword = reqVO.getKeyword() == null ? "" : reqVO.getKeyword().trim();
        if (!keyword.isEmpty()) {
            String like = "%" + keyword + "%";
            wrapper.and(w -> w
                    .apply("LOWER(contract_no) LIKE LOWER({0})", like)
                    .or().apply("LOWER(client_name) LIKE LOWER({0})", like)
                    .or().apply("LOWER(product_code) LIKE LOWER({0})", like)
                    .or().apply("LOWER(product_name) LIKE LOWER({0})", like));
        }
        return wrapper;
    }

    /**
     * 校验染色单存在，不存在抛 DYEING_ORDER_NOT_EXISTS
     */
    private DyeingOrderDO validateDyeingOrderExists(Long id) {
        if (id == null) {
            throw exception(DYEING_ORDER_NOT_EXISTS);
        }
        DyeingOrderDO order = dyeingOrderMapper.selectById(id);
        if (order == null) {
            throw exception(DYEING_ORDER_NOT_EXISTS);
        }
        return order;
    }

    /**
     * 引用校验结果：销售合同、承染方供应商档案与打色指示单档案，供快照回填
     */
    private record ValidatedRefs(SalesContractDO contract, SupplierDO dyeingFactory, ColorInstructionDO colorInstruction) {
    }

    /**
     * 引用校验：销售合同必填且存在；承染方供应商必填且存在；打色指示单可空，任一不存在或不合法抛 DYEING_ORDER_REFERENCE_NOT_FOUND
     */
    private ValidatedRefs validateReferences(DyeingOrderSaveReqVO reqVO) {
        SalesContractDO contract = salesContractMapper.selectById(reqVO.getSalesContractId());
        if (contract == null) {
            throw exception(DYEING_ORDER_REFERENCE_NOT_FOUND, "销售合同");
        }
        SupplierDO dyeingFactory = supplierMapper.selectById(reqVO.getDyeingFactoryId());
        if (dyeingFactory == null) {
            throw exception(DYEING_ORDER_REFERENCE_NOT_FOUND, "承染方");
        }
        ColorInstructionDO colorInstruction = null;
        if (reqVO.getColorInstructionId() != null) {
            colorInstruction = colorInstructionMapper.selectById(reqVO.getColorInstructionId());
            if (colorInstruction == null || Boolean.TRUE.equals(colorInstruction.getDeleted())) {
                throw exception(DYEING_ORDER_REFERENCE_NOT_FOUND, "打色指示单");
            }
            Long currentTenantId = TenantContextHolder.getTenantId();
            if (currentTenantId != null && colorInstruction.getTenantId() != null && !currentTenantId.equals(colorInstruction.getTenantId())) {
                throw exception(DYEING_ORDER_REFERENCE_NOT_FOUND, "打色指示单");
            }
            if (!"CONFIRMED".equals(colorInstruction.getStatus()) && !"IN_PRODUCTION".equals(colorInstruction.getStatus())) {
                throw exception(DYEING_ORDER_REFERENCE_NOT_FOUND, "打色指示单状态不是已确认或已投产");
            }
            if (contract.getCustomerId() != null && colorInstruction.getCustomerId() != null
                    && !contract.getCustomerId().equals(colorInstruction.getCustomerId())) {
                throw exception(DYEING_ORDER_REFERENCE_NOT_FOUND, "打色指示单客户与销售合同客户不一致");
            }
            if (StringUtils.hasText(reqVO.getProductCode()) && StringUtils.hasText(colorInstruction.getProductCode())
                    && !reqVO.getProductCode().trim().equalsIgnoreCase(colorInstruction.getProductCode().trim())) {
                throw exception(DYEING_ORDER_REFERENCE_NOT_FOUND, "打色指示单产品与染色单产品不一致");
            }
        }
        return new ValidatedRefs(contract, dyeingFactory, colorInstruction);
    }

    /**
     * 快照回填 + 缺省值：
     * 合同号快照从销售合同带出；客户ID与客户名快照从销售合同带出；
     * 承染方公司名快照从供应商档案带出；
     * 关联打色指示单时回填单号快照，清空引用时快照置空；
     * 单据日期缺省今天；状态缺省 DRAFT，传值仅允许 DRAFT/ACTIVE。
     */
    private void fillSnapshotsAndDefaults(DyeingOrderDO order, SalesContractDO contract, SupplierDO dyeingFactory, ColorInstructionDO colorInstruction) {
        if (contract != null) {
            order.setContractNo(contract.getContractNo());
            order.setCustomerId(contract.getCustomerId());
            order.setClientName(contract.getClientName());
        }
        if (dyeingFactory != null) {
            order.setDyeingFactoryName(dyeingFactory.getCompanyName());
        }
        if (order.getColorInstructionId() != null && colorInstruction != null) {
            order.setColorInstructionNo(colorInstruction.getInstructionNo());
        } else if (order.getColorInstructionId() == null) {
            order.setColorInstructionNo(null);
        }
        if (order.getOrderDate() == null) {
            order.setOrderDate(LocalDate.now());
        }
        if (!StringUtils.hasText(order.getStatus())) {
            order.setStatus("DRAFT");
        } else if (!"DRAFT".equals(order.getStatus()) && !"ACTIVE".equals(order.getStatus())) {
            throw exception(DYEING_ORDER_STATUS_NOT_ALLOWED);
        }
    }

    // ==================== 颜色明细聚合私有方法（ADR-0003） ====================

    /**
     * 将请求颜色行转换为 DO：按提交顺序重排 lineNo、回填 dyeingOrderId、数量 null 按 0 处理。
     */
    private DyeingColorDO toColorDO(Long dyeingOrderId, int lineNo, DyeingColorSaveReqVO color) {
        DyeingColorDO colorDO = BeanUtils.toBean(color, DyeingColorDO.class);
        colorDO.setDyeingOrderId(dyeingOrderId);
        colorDO.setLineNo(lineNo);
        if (colorDO.getQuantity() == null) {
            colorDO.setQuantity(BigDecimal.ZERO);
        }
        return colorDO;
    }

    /**
     * 将请求颜色行列表转换为 DO 列表：按数组顺序重排 line_no（从 1 开始）、
     * 数量 null 按 0 处理。新建时 id 统一置空。
     *
     * @param dyeingOrderId 染色单ID
     * @param colors        请求颜色行列表（为 null/空表示零颜色单据，合法）
     * @return 颜色行 DO 列表
     */
    private List<DyeingColorDO> buildColorDOList(Long dyeingOrderId, List<DyeingColorSaveReqVO> colors) {
        List<DyeingColorDO> colorDOList = new ArrayList<>();
        if (CollUtil.isEmpty(colors)) {
            return colorDOList;
        }
        for (int i = 0; i < colors.size(); i++) {
            DyeingColorSaveReqVO color = colors.get(i);
            DyeingColorDO colorDO = toColorDO(dyeingOrderId, i + 1, color);
            colorDO.setId(null);
            colorDOList.add(colorDO);
        }
        return colorDOList;
    }

    /**
     * 批量保存颜色行（回填 dyeingOrderId）；零颜色时跳过插入。
     */
    private void saveColors(Long dyeingOrderId, List<DyeingColorDO> colors) {
        if (CollUtil.isEmpty(colors)) {
            return;
        }
        for (DyeingColorDO color : colors) {
            color.setDyeingOrderId(dyeingOrderId);
        }
        dyeingColorMapper.insertBatch(colors);
    }

    /**
     * 对染色单颜色明细做差异保存（ADR-0032）：
     * - 提交行带 id 且属于本单据存量 → updateById
     * - 提交行无 id → insert
     * - 本单据库内存量但本次未提交的 id → deleteByIds 批量逻辑删除
     * - 提交行 id 不属于本单据时报错（防跨父 id 串数据）
     * - 提交列表中不得有重复行 id
     * - 行号按提交顺序从 1 开始重排
     * - 删除前调用 DyeingColorDeleteGuard 扩展点留桩校验
     *
     * @param dyeingOrderId 染色单ID
     * @param colors        提交的颜色列表（可为 null 或空）
     */
    private void diffSaveColors(Long dyeingOrderId, List<DyeingColorSaveReqVO> colors) {
        // 1. 查询库内存量颜色明细（按 line_no 升序，MP 自动过滤 deleted=1）
        List<DyeingColorDO> dbColors = dyeingColorMapper.selectListByOrderId(dyeingOrderId);
        Map<Long, DyeingColorDO> dbColorMap = dbColors.stream()
                .collect(Collectors.toMap(DyeingColorDO::getId, Function.identity()));
        Set<Long> remainingDbColorIds = new HashSet<>(dbColorMap.keySet());

        List<DyeingColorDO> toInsert = new ArrayList<>();
        List<DyeingColorDO> toUpdate = new ArrayList<>();
        Set<Long> seenIds = new HashSet<>();

        // 2. 遍历提交列表，三分流：更新 / 新增 / 校验跨父非法 ID 及重复 ID
        if (CollUtil.isNotEmpty(colors)) {
            for (int i = 0; i < colors.size(); i++) {
                DyeingColorSaveReqVO color = colors.get(i);
                DyeingColorDO colorDO = toColorDO(dyeingOrderId, i + 1, color);

                if (color.getId() != null) {
                    Long colorId = color.getId();
                    if (!seenIds.add(colorId)) {
                        throw invalidParamException("染色单颜色明细行 ID【{}】重复提交", colorId);
                    }
                    if (!remainingDbColorIds.remove(colorId)) {
                        // ID 不在当前染色单存量颜色明细中（可能不存在、已删除或属于其他单据）
                        throw invalidParamException("染色单颜色明细行 ID【{}】不属于当前染色单", colorId);
                    }
                    colorDO.setId(colorId);
                    toUpdate.add(colorDO);
                } else {
                    colorDO.setId(null);
                    toInsert.add(colorDO);
                }
            }
        }

        // 3. 执行删除（存量中未被本次提交命中的 ID，前置守卫校验）
        if (!remainingDbColorIds.isEmpty()) {
            dyeingColorDeleteGuard.validateCanDeleteColors(dyeingOrderId, remainingDbColorIds);
            dyeingColorMapper.deleteByIds(remainingDbColorIds);
        }

        // 4. 执行更新
        for (DyeingColorDO updateColorDO : toUpdate) {
            dyeingColorMapper.updateById(updateColorDO);
        }

        // 5. 执行新增
        for (DyeingColorDO insertColorDO : toInsert) {
            dyeingColorMapper.insert(insertColorDO);
        }
    }

}
