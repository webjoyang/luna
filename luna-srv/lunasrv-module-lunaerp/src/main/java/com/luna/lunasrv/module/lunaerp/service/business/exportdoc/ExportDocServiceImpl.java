package com.luna.lunasrv.module.lunaerp.service.business.exportdoc;

import cn.hutool.core.collection.CollUtil;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.framework.datapermission.core.annotation.DataPermission;
import com.luna.lunasrv.framework.datapermission.core.util.DataPermissionUtils;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.business.exportdoc.vo.ExportDocItemSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.business.exportdoc.vo.ExportDocItemRespVO;
import com.luna.lunasrv.module.lunaerp.controller.business.exportdoc.vo.ExportDocPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.business.exportdoc.vo.ExportDocRespVO;
import com.luna.lunasrv.module.lunaerp.controller.business.exportdoc.vo.ExportDocSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.exportdoc.ExportDocDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.exportdoc.ExportDocItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.exportdoc.ExportSellerConfigDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.exportdoc.ExportDocItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.exportdoc.ExportDocMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.exportdoc.ExportSellerConfigMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.ExportDocErrorCodeConstants.*;

/**
 * 业务管理-出口单证 Service 实现类
 *
 * @author luna
 */
@Service
public class ExportDocServiceImpl implements ExportDocService {

    private static final String DOC_NO_PREFIX = "ED";
    private static final int NO_MIN_DIGITS = 4;
    private static final int MAX_GENERATE_RETRY = 3;
    private static final Pattern NO_SUFFIX_NUMBER_PATTERN = Pattern.compile("^\\d+");

    private static final String DEFAULT_STATUS = "DRAFT";
    private static final String DEFAULT_SOURCE_TYPE = "MANUAL";
    private static final String DEFAULT_CURRENCY = "USD";
    private static final String DEFAULT_ORIGIN = "CHINA";

    @Resource
    private ExportDocMapper exportDocMapper;

    @Resource
    private ExportDocItemMapper exportDocItemMapper;

    @Resource
    private ExportSellerConfigMapper exportSellerConfigMapper;

    @Resource
    private CustomerMapper customerMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createExportDoc(ExportDocSaveReqVO createReqVO) {
        // 1. 校验客户存在
        CustomerDO customer = validateCustomerExists(createReqVO.getCustomerId());

        // 2. 组装主表 DO
        ExportDocDO doc = BeanUtils.toBean(createReqVO, ExportDocDO.class);

        // 3. 客户快照回填
        if (!StringUtils.hasText(doc.getCustomerName())) {
            doc.setCustomerName(customer.getCompanyName());
        }
        if (!StringUtils.hasText(doc.getBuyerName())) {
            doc.setBuyerName(StringUtils.hasText(customer.getNameEn()) ? customer.getNameEn() : customer.getCompanyName());
        }
        if (!StringUtils.hasText(doc.getBuyerAddress())) {
            doc.setBuyerAddress(StringUtils.hasText(customer.getAddressEn()) ? customer.getAddressEn() : customer.getAddress());
        }

        // 4. 缺省值
        if (!StringUtils.hasText(doc.getStatus())) {
            doc.setStatus(DEFAULT_STATUS);
        }
        if (!StringUtils.hasText(doc.getSourceType())) {
            doc.setSourceType(DEFAULT_SOURCE_TYPE);
        }
        if (!StringUtils.hasText(doc.getCurrency())) {
            doc.setCurrency(DEFAULT_CURRENCY);
        }

        // 5. 卖方配置缺省带出
        prefillSellerConfigIfEmpty(doc);

        // 6. 取号与查重（含软删行，ADR-0004）
        if (StringUtils.hasText(doc.getDocNo())) {
            validateDocNoUnique(null, doc.getDocNo());
        } else {
            doc.setDocNo(generateDocNoWithRetry());
        }

        // 7. 保存主表
        exportDocMapper.insert(doc);

        // 8. 聚合保存明细
        saveItems(doc.getId(), createReqVO.getItems());

        return doc.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateExportDoc(ExportDocSaveReqVO updateReqVO) {
        // 1. 校验存在
        ExportDocDO existing = validateExportDocExists(updateReqVO.getId());

        // 2. 状态机守卫：仅 DRAFT 允许修改
        if (!"DRAFT".equalsIgnoreCase(existing.getStatus())) {
            throw exception(EXPORT_DOC_STATUS_NOT_ALLOW);
        }

        // 3. 客户校验
        if (updateReqVO.getCustomerId() != null) {
            CustomerDO customer = validateCustomerExists(updateReqVO.getCustomerId());
            if (!StringUtils.hasText(updateReqVO.getCustomerName())) {
                updateReqVO.setCustomerName(customer.getCompanyName());
            }
        }

        // 4. 单号查重（若手填改单号）
        ExportDocDO updateObj = BeanUtils.toBean(updateReqVO, ExportDocDO.class);
        if (StringUtils.hasText(updateObj.getDocNo())) {
            validateDocNoUnique(updateObj.getId(), updateObj.getDocNo());
        } else {
            updateObj.setDocNo(null);
        }

        // 5. 校验提交的明细行 ID 归属（防跨单串数据）
        List<ExportDocItemDO> dbItems = exportDocItemMapper.selectListByDocId(updateReqVO.getId());
        Set<Long> dbItemIds = dbItems.stream().map(ExportDocItemDO::getId).collect(Collectors.toSet());
        if (CollUtil.isNotEmpty(updateReqVO.getItems())) {
            for (ExportDocItemSaveReqVO item : updateReqVO.getItems()) {
                if (item.getId() != null && !dbItemIds.contains(item.getId())) {
                    throw exception(EXPORT_DOC_ITEM_INVALID_ID, item.getId());
                }
            }
        }

        // 6. 聚合整体替换明细：先删旧明细，再重排插入
        exportDocItemMapper.deleteByDocId(updateReqVO.getId());
        saveItems(updateReqVO.getId(), updateReqVO.getItems());

        // 7. 更新主表
        exportDocMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteExportDoc(Long id) {
        // 1. 校验存在
        ExportDocDO existing = validateExportDocExists(id);

        // 2. 状态机守卫：仅 DRAFT 允许删除
        if (!"DRAFT".equalsIgnoreCase(existing.getStatus())) {
            throw exception(EXPORT_DOC_STATUS_NOT_ALLOW);
        }

        // 3. 级联逻辑删除主表与明细（进回收站）
        exportDocMapper.deleteById(id);
        exportDocItemMapper.deleteByDocId(id);
    }

    @Override
    public ExportDocRespVO getExportDoc(Long id) {
        ExportDocDO doc = exportDocMapper.selectById(id);
        if (doc == null) {
            throw exception(EXPORT_DOC_NOT_EXISTS);
        }
        ExportDocRespVO respVO = BeanUtils.toBean(doc, ExportDocRespVO.class);
        List<ExportDocItemDO> items = exportDocItemMapper.selectListByDocId(id);
        respVO.setItems(BeanUtils.toBean(items, ExportDocItemRespVO.class));
        return respVO;
    }

    @Override
    public PageResult<ExportDocRespVO> getExportDocPage(ExportDocPageReqVO reqVO) {
        LambdaQueryWrapperX<ExportDocDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eqIfPresent(ExportDocDO::getStatus, reqVO.getStatus());
        wrapper.eqIfPresent(ExportDocDO::getCustomerId, reqVO.getCustomerId());
        wrapper.eqIfPresent(ExportDocDO::getContractNo, reqVO.getContractNo());
        if (StringUtils.hasText(reqVO.getKeyword())) {
            String kw = reqVO.getKeyword().trim();
            String like = "%" + kw + "%";
            wrapper.and(w -> w
                    .apply("LOWER(doc_no) LIKE LOWER({0})", like)
                    .or().apply("LOWER(customer_name) LIKE LOWER({0})", like)
                    .or().apply("LOWER(contract_no) LIKE LOWER({0})", like)
                    .or().apply("LOWER(shipment_no) LIKE LOWER({0})", like));
        }
        wrapper.orderByDesc(ExportDocDO::getId);
        PageResult<ExportDocDO> pageResult = exportDocMapper.selectPage(reqVO, wrapper);
        return BeanUtils.toBean(pageResult, ExportDocRespVO.class);
    }

    @Override
    @DataPermission(enable = false)
    public String getNextDocNo() {
        return DataPermissionUtils.executeIgnore(this::generateDocNo);
    }

    // ==================== 私有方法 ====================

    private ExportDocDO validateExportDocExists(Long id) {
        if (id == null) {
            throw exception(EXPORT_DOC_NOT_EXISTS);
        }
        ExportDocDO doc = exportDocMapper.selectById(id);
        if (doc == null) {
            throw exception(EXPORT_DOC_NOT_EXISTS);
        }
        return doc;
    }

    private CustomerDO validateCustomerExists(Long customerId) {
        if (customerId == null) {
            throw exception(EXPORT_DOC_REFERENCE_NOT_FOUND, "客户");
        }
        CustomerDO customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw exception(EXPORT_DOC_REFERENCE_NOT_FOUND, "客户");
        }
        return customer;
    }

    private void prefillSellerConfigIfEmpty(ExportDocDO doc) {
        ExportSellerConfigDO sellerConfig = exportSellerConfigMapper.selectByTenant();
        if (sellerConfig == null) {
            return;
        }
        if (!StringUtils.hasText(doc.getSellerName())) {
            doc.setSellerName(sellerConfig.getSellerName());
        }
        if (!StringUtils.hasText(doc.getSellerAddress())) {
            doc.setSellerAddress(sellerConfig.getSellerAddress());
        }
        if (!StringUtils.hasText(doc.getSellerBankInfo())) {
            StringBuilder sb = new StringBuilder();
            if (StringUtils.hasText(sellerConfig.getBankName())) sb.append(sellerConfig.getBankName()).append(" ");
            if (StringUtils.hasText(sellerConfig.getBankAccount())) sb.append("A/C: ").append(sellerConfig.getBankAccount()).append(" ");
            if (StringUtils.hasText(sellerConfig.getBankSwift())) sb.append("SWIFT: ").append(sellerConfig.getBankSwift()).append(" ");
            if (StringUtils.hasText(sellerConfig.getBankAddress())) sb.append(sellerConfig.getBankAddress());
            String bankInfo = sb.toString().trim();
            if (!bankInfo.isEmpty()) {
                doc.setSellerBankInfo(bankInfo);
            }
        }
    }

    private void saveItems(Long docId, List<ExportDocItemSaveReqVO> items) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        List<ExportDocItemDO> itemDOList = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            ExportDocItemSaveReqVO item = items.get(i);
            ExportDocItemDO itemDO = BeanUtils.toBean(item, ExportDocItemDO.class);
            itemDO.setId(null);
            itemDO.setDocId(docId);
            itemDO.setLineNo(i + 1);
            if (!StringUtils.hasText(itemDO.getOrigin())) {
                itemDO.setOrigin(DEFAULT_ORIGIN);
            }
            BigDecimal quantity = zeroIfNull(item.getQuantity());
            BigDecimal unitPrice = zeroIfNull(item.getUnitPrice());
            itemDO.setQuantity(quantity.setScale(2, RoundingMode.HALF_UP));
            itemDO.setUnitPrice(unitPrice.setScale(2, RoundingMode.HALF_UP));
            // 金额权威重算：数量 * 单价
            itemDO.setAmount(quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP));
            if (item.getNetWeight() != null) {
                itemDO.setNetWeight(item.getNetWeight().setScale(2, RoundingMode.HALF_UP));
            }
            if (item.getGrossWeight() != null) {
                itemDO.setGrossWeight(item.getGrossWeight().setScale(2, RoundingMode.HALF_UP));
            }
            if (item.getVolume() != null) {
                itemDO.setVolume(item.getVolume().setScale(3, RoundingMode.HALF_UP));
            }
            itemDOList.add(itemDO);
        }
        exportDocItemMapper.insertBatch(itemDOList);
    }

    private BigDecimal zeroIfNull(BigDecimal val) {
        return val == null ? BigDecimal.ZERO : val;
    }

    @DataPermission(enable = false)
    private String generateDocNoWithRetry() {
        return DataPermissionUtils.executeIgnore(() -> {
            for (int i = 0; i < MAX_GENERATE_RETRY; i++) {
                String candidate = generateDocNo();
                if (exportDocMapper.selectByDocNoIncludingDeleted(candidate) == null) {
                    return candidate;
                }
            }
            throw exception(EXPORT_DOC_NO_DUPLICATE, "生成单证套号冲突");
        });
    }

    @DataPermission(enable = false)
    private String generateDocNo() {
        return DataPermissionUtils.executeIgnore(() -> {
            String prefix = DOC_NO_PREFIX + LocalDate.now().getYear();
            List<ExportDocDO> candidates = exportDocMapper.selectListByDocNoPrefixIncludingDeleted(prefix);
            long maxNum = 0L;
            int maxDigits = NO_MIN_DIGITS;
            for (ExportDocDO candidate : candidates) {
                String no = candidate.getDocNo();
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
        });
    }

    @DataPermission(enable = false)
    private void validateDocNoUnique(Long excludeId, String docNo) {
        if (!StringUtils.hasText(docNo)) {
            return;
        }
        DataPermissionUtils.executeIgnore(() -> {
            ExportDocDO exist = exportDocMapper.selectByDocNoIncludingDeleted(docNo);
            if (exist != null && !exist.getId().equals(excludeId)) {
                throw exception(EXPORT_DOC_NO_DUPLICATE, docNo);
            }
        });
    }

}
