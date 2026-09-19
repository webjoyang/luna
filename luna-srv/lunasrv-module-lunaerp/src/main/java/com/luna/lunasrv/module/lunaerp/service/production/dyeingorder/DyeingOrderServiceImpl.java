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
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.dyeingorder.DyeingOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.dyeingorder.color.DyeingColorMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
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
    private SupplierMapper supplierMapper;

    @Resource
    private SalesContractMapper salesContractMapper;

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
        fillSnapshotsAndDefaults(order, refs.contract(), refs.dyeingFactory());
        // 4. 保存主表
        dyeingOrderMapper.insert(order);
        // 5. 颜色明细：按提交顺序重排 line_no（从 1 开始），回填 dyeingOrderId（零明细合法）
        List<DyeingColorDO> colorDOList = buildColorDOList(order.getId(), createReqVO.getColors());
        saveColors(order.getId(), colorDOList);
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
        // 2. 状态校验：仅 DRAFT 态可更新，非 DRAFT 态更新报 DYEING_ORDER_STATUS_NOT_ALLOWED
        if (!"DRAFT".equals(existing.getStatus())) {
            throw exception(DYEING_ORDER_STATUS_NOT_ALLOWED);
        }
        // 3. 引用校验：销售合同必填且存在；承染方供应商必填且存在
        ValidatedRefs refs = validateReferences(updateReqVO);
        // 4. 组装 DO
        DyeingOrderDO order = BeanUtils.toBean(updateReqVO, DyeingOrderDO.class);
        order.setId(existing.getId());
        // 5. 保持 DRAFT 态，保留原有单据日期（若未传），刷新快照与缺省值
        order.setStatus("DRAFT");
        if (order.getOrderDate() == null) {
            order.setOrderDate(existing.getOrderDate());
        }
        fillSnapshotsAndDefaults(order, refs.contract(), refs.dyeingFactory());
        // 6. 整体更新（可空字段清空随 DO 的 updateStrategy=ALWAYS 一并落库）
        dyeingOrderMapper.updateById(order);
        // 7. 颜色明细整体替换：事务内先按 dyeing_order_id 删旧行，再插本次提交的全部颜色行
        //    （零颜色也执行删除，保证"清空全部颜色行"语义；line_no 从 1 开始重排）
        List<DyeingColorDO> colorDOList = buildColorDOList(updateReqVO.getId(), updateReqVO.getColors());
        dyeingColorMapper.deleteByOrderId(updateReqVO.getId());
        saveColors(updateReqVO.getId(), colorDOList);
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
     * 引用校验结果：销售合同与承染方供应商档案，供快照回填
     */
    private record ValidatedRefs(SalesContractDO contract, SupplierDO dyeingFactory) {
    }

    /**
     * 引用校验：销售合同必填且存在；承染方供应商必填且存在，任一不存在抛 DYEING_ORDER_REFERENCE_NOT_FOUND
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
        return new ValidatedRefs(contract, dyeingFactory);
    }

    /**
     * 快照回填 + 缺省值：
     * 合同号快照从销售合同带出；客户ID与客户名快照从销售合同带出；
     * 承染方公司名快照从供应商档案带出；
     * 单据日期缺省今天；状态缺省 DRAFT，传值仅允许 DRAFT/ACTIVE。
     */
    private void fillSnapshotsAndDefaults(DyeingOrderDO order, SalesContractDO contract, SupplierDO dyeingFactory) {
        if (contract != null) {
            order.setContractNo(contract.getContractNo());
            order.setCustomerId(contract.getCustomerId());
            order.setClientName(contract.getClientName());
        }
        if (dyeingFactory != null) {
            order.setDyeingFactoryName(dyeingFactory.getCompanyName());
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
     * 将请求颜色行列表转换为 DO 列表：按数组顺序重排 line_no（从 1 开始）、
     * 数量 null 按 0 处理。
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
            DyeingColorDO colorDO = BeanUtils.toBean(color, DyeingColorDO.class);
            colorDO.setDyeingOrderId(dyeingOrderId);
            colorDO.setLineNo(i + 1);
            if (colorDO.getQuantity() == null) {
                colorDO.setQuantity(BigDecimal.ZERO);
            }
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

}
