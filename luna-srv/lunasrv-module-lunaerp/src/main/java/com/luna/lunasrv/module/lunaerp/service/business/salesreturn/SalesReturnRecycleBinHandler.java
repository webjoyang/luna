package com.luna.lunasrv.module.lunaerp.service.business.salesreturn;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.datapermission.core.annotation.DataPermission;
import com.luna.lunasrv.framework.mybatis.core.util.MyBatisUtils;
import com.luna.lunasrv.framework.security.core.util.SecurityFrameworkUtils;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.common.recycle.RecycleBinHandler;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecyclePageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.product.ProductDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salesreturn.SalesReturnDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salesreturn.SalesReturnItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.warehouse.WarehouseDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.product.ProductMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salesreturn.SalesReturnItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salesreturn.SalesReturnMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salesreturn.SalesReturnRollMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.warehouse.WarehouseMapper;
import com.mzt.logapi.starter.annotation.LogRecord;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.RECYCLE_BIN_HAS_ACTIVE_REFERENCE;
import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.RECYCLE_BIN_RECORD_NOT_EXISTS;
import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.RECYCLE_BIN_REFERENCE_IN_RECYCLE;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.CUSTOMER_NOT_EXISTS;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.PRODUCT_NOT_EXISTS;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.WAREHOUSE_NOT_EXISTS;
import static com.luna.lunasrv.module.lunaerp.enums.business.salesreturn.SalesReturnErrorCodeConstants.SALES_RETURN_SHIPMENT_NOT_COMPLETED;

/**
 * 销售退货草稿回收站业务处理器（ADR-0038 / W0 Spec / Issue 07）
 *
 * 仅 DRAFT 状态（approval_status=PENDING/REJECTED）销售退货单进入回收站。
 * 恢复为 DRAFT 状态，并按父 -> 子顺序级联复活退货明细与退货卷清单；恢复不补账。
 * CANCELLED 永不进站。
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class SalesReturnRecycleBinHandler implements RecycleBinHandler {

    public static final String BIZ_TYPE = "SALES_RETURN";

    @Resource
    private SalesReturnMapper salesReturnMapper;

    @Resource
    private SalesReturnItemMapper salesReturnItemMapper;

    @Resource
    private SalesReturnRollMapper salesReturnRollMapper;

    @Resource
    private CustomerMapper customerMapper;

    @Resource
    private ProductMapper productMapper;

    @Resource
    private ShipmentMapper shipmentMapper;

    @Resource
    private WarehouseMapper warehouseMapper;

    @Override
    public String getBizType() {
        return BIZ_TYPE;
    }

    @Override
    @DataPermission(enable = false)
    public PageResult<RecycleItemRespVO> pageDeleted(RecyclePageReqVO req) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        IPage<RecycleItemRespVO> mpPage = MyBatisUtils.buildPage(req);
        salesReturnMapper.selectRecyclePage(mpPage, tenantId);
        return new PageResult<>(mpPage.getRecords(), mpPage.getTotal());
    }

    @Override
    @DataPermission(enable = false)
    public void validateRestore(Long id) {
        if (id == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        SalesReturnDO salesReturn = salesReturnMapper.selectDeletedById(id, tenantId);
        if (salesReturn == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }

        // 1. 校验客户档案
        Long customerId = salesReturn.getCustomerId();
        if (customerId != null) {
            CustomerDO deletedCustomer = customerMapper.selectDeletedById(customerId, tenantId);
            if (deletedCustomer != null) {
                throw exception(RECYCLE_BIN_REFERENCE_IN_RECYCLE, deletedCustomer.getCompanyName());
            }
            CustomerDO activeCustomer = customerMapper.selectById(customerId);
            if (activeCustomer == null) {
                throw exception(CUSTOMER_NOT_EXISTS);
            }
        }

        // 2. 校验退货仓库
        Long warehouseId = salesReturn.getWarehouseId();
        if (warehouseId != null) {
            WarehouseDO warehouse = warehouseMapper.selectById(warehouseId);
            if (warehouse == null) {
                throw exception(WAREHOUSE_NOT_EXISTS);
            }
        }

        // 3. 校验关联发货单：发货单不能在回收站，且必须存在且仍为 COMPLETED 状态
        Long shipmentId = salesReturn.getShipmentId();
        if (shipmentId != null) {
            ShipmentDO deletedShipment = shipmentMapper.selectDeletedById(shipmentId, tenantId);
            if (deletedShipment != null) {
                throw exception(RECYCLE_BIN_REFERENCE_IN_RECYCLE, deletedShipment.getShipmentNo());
            }
            ShipmentDO activeShipment = shipmentMapper.selectById(shipmentId);
            if (activeShipment == null || !"COMPLETED".equals(activeShipment.getStatus())) {
                throw exception(SALES_RETURN_SHIPMENT_NOT_COMPLETED);
            }
        }

        // 4. 校验明细中的产品档案
        List<SalesReturnItemDO> items = salesReturnItemMapper.selectDeletedListByReturnId(id, tenantId);
        if (CollUtil.isNotEmpty(items)) {
            for (SalesReturnItemDO item : items) {
                Long productId = item.getProductId();
                if (productId != null) {
                    ProductDO deletedProduct = productMapper.selectDeletedById(productId, tenantId);
                    if (deletedProduct != null) {
                        throw exception(RECYCLE_BIN_REFERENCE_IN_RECYCLE, deletedProduct.getName());
                    }
                    ProductDO activeProduct = productMapper.selectById(productId);
                    if (activeProduct == null) {
                        throw exception(PRODUCT_NOT_EXISTS);
                    }
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DataPermission(enable = false)
    @LogRecord(type = "RECYCLE_BIN", subType = "RESTORE", bizNo = "{{#id}}", success = "恢复草稿销售退货单【{{#id}}】")
    public void restore(Long id) {
        if (id == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();

        // 1. 恢复前校验存在且在回收站中
        validateRestore(id);

        // 2. 并发条件更新主数据（deleted=0, status=DRAFT, approval_status=PENDING）
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        String updater = loginUserId != null ? String.valueOf(loginUserId) : "1";
        LocalDateTime now = LocalDateTime.now();

        int affected = salesReturnMapper.restoreSalesReturn(id, tenantId, updater, now);
        if (affected == 0) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }

        // 3. 父 -> 子顺序级联复活退货明细与退货卷清单
        salesReturnItemMapper.restoreByReturnId(id, tenantId, updater, now);
        salesReturnRollMapper.restoreByReturnId(id, tenantId, updater, now);
        log.info("[SalesReturnRecycleBinHandler] 草稿销售退货单已恢复: id={}, tenantId={}", id, tenantId);
    }

    @Override
    @DataPermission(enable = false)
    public boolean hasActiveReference(Long id) {
        // 草稿退货单不产生任何下游活动引用
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DataPermission(enable = false)
    @LogRecord(type = "RECYCLE_BIN", subType = "PURGE", bizNo = "{{#id}}", success = "彻底删除草稿销售退货单【{{#id}}】")
    public void purge(Long id) {
        if (id == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();

        // 1. 检查是否存在于回收站
        SalesReturnDO salesReturn = salesReturnMapper.selectDeletedById(id, tenantId);
        if (salesReturn == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }

        // 2. 引用守卫
        if (hasActiveReference(id)) {
            throw exception(RECYCLE_BIN_HAS_ACTIVE_REFERENCE);
        }

        // 3. 子 -> 父顺序彻底删除：先物理删除卷清单和明细
        salesReturnRollMapper.purgeByReturnId(id, tenantId);
        salesReturnItemMapper.purgeByReturnId(id, tenantId);

        // 4. 物理删除主表
        int affected = salesReturnMapper.purgeSalesReturn(id, tenantId);
        if (affected == 0) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        log.info("[SalesReturnRecycleBinHandler] 草稿销售退货单已彻底删除: id={}, tenantId={}", id, tenantId);
    }

}
