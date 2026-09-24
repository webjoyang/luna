package com.luna.lunasrv.module.lunaerp.service.stock.warehouse;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.stock.warehouse.vo.WarehousePageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.stock.warehouse.vo.WarehouseRespVO;
import com.luna.lunasrv.module.lunaerp.controller.stock.warehouse.vo.WarehouseSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.warehouse.WarehouseDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.stockin.StockInMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.warehouse.WarehouseMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.warehouselocation.WarehouseLocationMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.WAREHOUSE_HAS_LOCATION;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.WAREHOUSE_NOT_EXISTS;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.WAREHOUSE_NO_DUPLICATE;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.WAREHOUSE_REFERENCED_BY_STOCK_IN;

/**
 * 库存管理 - 仓库 Service 实现类
 *
 * 编号生成沿用 WH+年份+4位序号格式，扫描与查重范围包含已删除行（ADR-0005, ADR-0018）。
 * 删除守卫：
 * 1. 存在库位时禁止删除，引导停用（1_030_013_002）
 * 2. 已被入库单引用时禁止删除，引导停用（1_030_013_003）
 *
 * @author luna
 */
@Service
@Validated
public class WarehouseServiceImpl implements WarehouseService {

    /**
     * 仓库编号前缀
     */
    private static final String WAREHOUSE_NO_PREFIX = "WH";
    /**
     * 编号数字部分最小补零位数（超 9999 自然扩位）
     */
    private static final int NO_MIN_DIGITS = 4;
    /**
     * 自动取号撞唯一索引时的最大顺延重试次数
     */
    private static final int MAX_GENERATE_RETRY = 3;
    /**
     * 编号前缀（WH+年份）后的数字部分
     */
    private static final Pattern NO_SUFFIX_NUMBER_PATTERN = Pattern.compile("^\\d+");

    /**
     * 仓库默认状态
     */
    private static final String DEFAULT_STATUS = "ACTIVE";
    /**
     * 停用状态
     */
    private static final String STATUS_INACTIVE = "INACTIVE";

    @Resource
    private WarehouseMapper warehouseMapper;

    @Resource
    private WarehouseLocationMapper warehouseLocationMapper;

    @Resource
    private StockInMapper stockInMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createWarehouse(WarehouseSaveReqVO createReqVO) {
        WarehouseDO warehouse = BeanUtils.toBean(createReqVO, WarehouseDO.class);
        // 1. 编号：手填查重（含已删除行，撞号报错不顺延）；留空自动生成（撞号有限次顺延重试）
        if (StringUtils.hasText(warehouse.getWarehouseNo())) {
            validateWarehouseNoUnique(null, warehouse.getWarehouseNo());
        } else {
            warehouse.setWarehouseNo(generateWarehouseNoWithRetry());
        }
        // 2. 状态缺省 ACTIVE
        if (!StringUtils.hasText(warehouse.getStatus())) {
            warehouse.setStatus(DEFAULT_STATUS);
        }
        warehouseMapper.insert(warehouse);
        return warehouse.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWarehouse(WarehouseSaveReqVO updateReqVO) {
        validateWarehouseExists(updateReqVO.getId());
        WarehouseDO updateObj = BeanUtils.toBean(updateReqVO, WarehouseDO.class);
        // 编号变更时查重（排除自身，含已删除行）
        if (StringUtils.hasText(updateObj.getWarehouseNo())) {
            validateWarehouseNoUnique(updateObj.getId(), updateObj.getWarehouseNo());
        } else {
            updateObj.setWarehouseNo(null);
        }
        warehouseMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWarehouse(Long id) {
        validateWarehouseExists(id);

        // 删除守卫 1：仓库下有库位禁止删除，引导改用停用
        Long locationCount = warehouseLocationMapper.selectCountByWarehouseId(id);
        if (locationCount != null && locationCount > 0) {
            throw exception(WAREHOUSE_HAS_LOCATION);
        }

        // 删除守卫 2：仓库被入库单引用禁止删除，引导改用停用
        Long stockInCount = stockInMapper.selectCountByWarehouseId(id);
        if (stockInCount != null && stockInCount > 0) {
            throw exception(WAREHOUSE_REFERENCED_BY_STOCK_IN);
        }

        // 先置 status='INACTIVE'，再逻辑删除
        WarehouseDO updateObj = new WarehouseDO();
        updateObj.setId(id);
        updateObj.setStatus(STATUS_INACTIVE);
        warehouseMapper.updateById(updateObj);

        warehouseMapper.deleteById(id);
    }

    @Override
    public WarehouseRespVO getWarehouse(Long id) {
        WarehouseDO warehouse = warehouseMapper.selectById(id);
        if (warehouse == null) {
            throw exception(WAREHOUSE_NOT_EXISTS);
        }
        return BeanUtils.toBean(warehouse, WarehouseRespVO.class);
    }

    @Override
    public PageResult<WarehouseRespVO> getWarehousePage(WarehousePageReqVO pageReqVO) {
        PageResult<WarehouseDO> pageResult = warehouseMapper.selectPage(pageReqVO);
        return BeanUtils.toBean(pageResult, WarehouseRespVO.class);
    }

    @Override
    public List<WarehouseRespVO> getWarehouseListAll(String status) {
        String queryStatus = StringUtils.hasText(status) ? status : DEFAULT_STATUS;
        List<WarehouseDO> list = warehouseMapper.selectList(new LambdaQueryWrapperX<WarehouseDO>()
                .eq(WarehouseDO::getStatus, queryStatus)
                .orderByAsc(WarehouseDO::getWarehouseNo));
        return BeanUtils.toBean(list, WarehouseRespVO.class);
    }

    @Override
    public String getNextWarehouseNo() {
        return generateWarehouseNo();
    }

    // ==================== 私有方法 ====================

    private void validateWarehouseExists(Long id) {
        if (id == null || warehouseMapper.selectById(id) == null) {
            throw exception(WAREHOUSE_NOT_EXISTS);
        }
    }

    private void validateWarehouseNoUnique(Long excludeId, String warehouseNo) {
        if (!StringUtils.hasText(warehouseNo)) {
            return;
        }
        WarehouseDO exist = warehouseMapper.selectByWarehouseNoIncludingDeleted(warehouseNo.trim());
        if (exist != null && !exist.getId().equals(excludeId)) {
            throw exception(WAREHOUSE_NO_DUPLICATE, warehouseNo);
        }
    }

    private String generateWarehouseNoWithRetry() {
        for (int i = 0; i < MAX_GENERATE_RETRY; i++) {
            String candidate = generateWarehouseNo();
            if (warehouseMapper.selectByWarehouseNoIncludingDeleted(candidate) == null) {
                return candidate;
            }
        }
        throw exception(WAREHOUSE_NO_DUPLICATE, generateWarehouseNo());
    }

    private String generateWarehouseNo() {
        String prefix = WAREHOUSE_NO_PREFIX + LocalDate.now().getYear();
        List<WarehouseDO> candidates = warehouseMapper.selectListByWarehouseNoPrefixIncludingDeleted(prefix);
        long maxNum = 0L;
        int maxDigits = NO_MIN_DIGITS;
        for (WarehouseDO candidate : candidates) {
            String no = candidate.getWarehouseNo();
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
            return prefix + "0001";
        }
        return prefix + String.format("%0" + maxDigits + "d", maxNum + 1);
    }

}
