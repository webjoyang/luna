package com.luna.lunasrv.module.lunaerp.service.stock.stockin;

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
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.product.ProductDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.supplier.SupplierDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnspec.YarnSpecDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.stockin.StockInDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.stockin.StockInItemDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.product.ProductMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.supplier.SupplierMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnspec.YarnSpecMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.stockin.StockInItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.stockin.StockInItemPlacementMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.stockin.StockInMapper;
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

/**
 * 入库单草稿回收站业务处理器（ADR-0038 / W0 Spec）
 *
 * 仅 DRAFT 状态入库单进入回收站。
 * 恢复为 DRAFT 状态，并级联复活入库明细与库位放置记录（父 -> 子顺序）。
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class StockInRecycleBinHandler implements RecycleBinHandler {

    public static final String BIZ_TYPE = "STOCK_IN";

    @Resource
    private StockInMapper stockInMapper;

    @Resource
    private StockInItemMapper stockInItemMapper;

    @Resource
    private StockInItemPlacementMapper stockInItemPlacementMapper;

    @Resource
    private SupplierMapper supplierMapper;

    @Resource
    private ProductMapper productMapper;

    @Resource
    private YarnSpecMapper yarnSpecMapper;

    @Override
    public String getBizType() {
        return BIZ_TYPE;
    }

    @Override
    @DataPermission(enable = false)
    public PageResult<RecycleItemRespVO> pageDeleted(RecyclePageReqVO req) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        IPage<RecycleItemRespVO> mpPage = MyBatisUtils.buildPage(req);
        stockInMapper.selectRecyclePage(mpPage, tenantId);
        return new PageResult<>(mpPage.getRecords(), mpPage.getTotal());
    }

    @Override
    @DataPermission(enable = false)
    public void validateRestore(Long id) {
        if (id == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        StockInDO stockIn = stockInMapper.selectDeletedById(id, tenantId);
        if (stockIn == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }

        // 1. 供应商校验
        Long supplierId = stockIn.getSupplierId();
        if (supplierId != null) {
            SupplierDO deletedSupplier = supplierMapper.selectDeletedById(supplierId, tenantId);
            if (deletedSupplier != null) {
                throw exception(RECYCLE_BIN_REFERENCE_IN_RECYCLE, deletedSupplier.getCompanyName());
            }
            SupplierDO activeSupplier = supplierMapper.selectById(supplierId);
            if (activeSupplier == null) {
                throw exception(RECYCLE_BIN_REFERENCE_IN_RECYCLE, "供应商");
            }
        }

        // 2. 明细中的产品/纱线规格校验
        List<StockInItemDO> items = stockInItemMapper.selectDeletedListByStockInId(id, tenantId);
        if (CollUtil.isNotEmpty(items)) {
            for (StockInItemDO item : items) {
                Long productId = item.getProductId();
                if (productId != null) {
                    ProductDO deletedProduct = productMapper.selectDeletedById(productId, tenantId);
                    if (deletedProduct != null) {
                        throw exception(RECYCLE_BIN_REFERENCE_IN_RECYCLE, deletedProduct.getName());
                    }
                    ProductDO activeProduct = productMapper.selectById(productId);
                    if (activeProduct == null) {
                        throw exception(RECYCLE_BIN_REFERENCE_IN_RECYCLE, "产品档案");
                    }
                }

                Long yarnSpecId = item.getYarnSpecId();
                if (yarnSpecId != null) {
                    YarnSpecDO deletedSpec = yarnSpecMapper.selectDeletedById(yarnSpecId, tenantId);
                    if (deletedSpec != null) {
                        throw exception(RECYCLE_BIN_REFERENCE_IN_RECYCLE, deletedSpec.getSpecName());
                    }
                    YarnSpecDO activeSpec = yarnSpecMapper.selectById(yarnSpecId);
                    if (activeSpec == null) {
                        throw exception(RECYCLE_BIN_REFERENCE_IN_RECYCLE, "纱线规格");
                    }
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DataPermission(enable = false)
    @LogRecord(type = "RECYCLE_BIN", subType = "RESTORE", bizNo = "{{#id}}", success = "恢复草稿入库单【{{#id}}】")
    public void restore(Long id) {
        if (id == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();

        // 1. 恢复前校验存在且在回收站中
        validateRestore(id);

        // 2. 并发条件更新主数据（deleted=0, status=DRAFT）
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        String updater = loginUserId != null ? String.valueOf(loginUserId) : "1";
        LocalDateTime now = LocalDateTime.now();

        int affected = stockInMapper.restoreStockIn(id, tenantId, updater, now);
        if (affected == 0) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }

        // 3. 父 -> 子顺序级联复活明细与库位放置记录
        stockInItemMapper.restoreByStockInId(id, tenantId, updater, now);
        stockInItemPlacementMapper.restoreByStockInId(id, tenantId, updater, now);
        log.info("[StockInRecycleBinHandler] 草稿入库单已恢复: id={}, tenantId={}", id, tenantId);
    }

    @Override
    @DataPermission(enable = false)
    public boolean hasActiveReference(Long id) {
        if (id == null) {
            return false;
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Long count = stockInMapper.selectActiveReferenceCount(id, tenantId);
        return count != null && count > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DataPermission(enable = false)
    @LogRecord(type = "RECYCLE_BIN", subType = "PURGE", bizNo = "{{#id}}", success = "彻底删除草稿入库单【{{#id}}】")
    public void purge(Long id) {
        if (id == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();

        // 1. 检查是否存在于回收站
        StockInDO stockIn = stockInMapper.selectDeletedById(id, tenantId);
        if (stockIn == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }

        // 2. 引用守卫：存在活跃引用时禁止彻底删除
        if (hasActiveReference(id)) {
            throw exception(RECYCLE_BIN_HAS_ACTIVE_REFERENCE);
        }

        // 3. 子 -> 父顺序彻底删除：先删除库位放置和明细
        stockInItemPlacementMapper.purgeByStockInId(id, tenantId);
        stockInItemMapper.purgeByStockInId(id, tenantId);

        // 4. 物理删除主表
        int affected = stockInMapper.purgeStockIn(id, tenantId);
        if (affected == 0) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        log.info("[StockInRecycleBinHandler] 草稿入库单已彻底删除: id={}, tenantId={}", id, tenantId);
    }

}
