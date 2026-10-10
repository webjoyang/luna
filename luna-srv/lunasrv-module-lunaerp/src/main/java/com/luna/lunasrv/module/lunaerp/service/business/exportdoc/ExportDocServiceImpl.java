package com.luna.lunasrv.module.lunaerp.service.business.exportdoc;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.framework.datapermission.core.annotation.DataPermission;
import com.luna.lunasrv.framework.datapermission.core.util.DataPermissionUtils;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.infra.api.config.ConfigApi;
import com.luna.lunasrv.module.lunaerp.controller.business.exportdoc.vo.ExportDocItemSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.business.exportdoc.vo.ExportDocItemRespVO;
import com.luna.lunasrv.module.lunaerp.controller.business.exportdoc.vo.ExportDocPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.business.exportdoc.vo.ExportDocPrintRespVO;
import com.luna.lunasrv.module.lunaerp.controller.business.exportdoc.vo.ExportDocRespVO;
import com.luna.lunasrv.module.lunaerp.controller.business.exportdoc.vo.ExportDocSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.business.exportdoc.vo.ExportSellerConfigRespVO;
import com.luna.lunasrv.module.lunaerp.controller.business.exportdoc.vo.ExportSellerConfigSaveReqVO;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.starter.annotation.LogRecord;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.exportdoc.ExportDocDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.exportdoc.ExportDocItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.exportdoc.ExportSellerConfigDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salescontract.SalesContractDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salescontract.SalesContractItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentRollDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.exportdoc.ExportDocItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.exportdoc.ExportDocMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.exportdoc.ExportSellerConfigMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salescontract.SalesContractItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salescontract.SalesContractMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentRollMapper;
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

    @Resource
    private SalesContractMapper salesContractMapper;

    @Resource
    private SalesContractItemMapper salesContractItemMapper;

    @Resource
    private ShipmentMapper shipmentMapper;

    @Resource
    private ShipmentItemMapper shipmentItemMapper;

    @Resource
    private ShipmentRollMapper shipmentRollMapper;

    @Resource
    private ConfigApi configApi;

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

    @Override
    public ExportSellerConfigRespVO getSellerConfig() {
        ExportSellerConfigDO config = exportSellerConfigMapper.selectByTenant();
        if (config == null) {
            return new ExportSellerConfigRespVO();
        }
        return BeanUtils.toBean(config, ExportSellerConfigRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSellerConfig(ExportSellerConfigSaveReqVO saveReqVO) {
        ExportSellerConfigDO existing = exportSellerConfigMapper.selectByTenant();
        try {
            if (existing == null) {
                ExportSellerConfigDO newConfig = BeanUtils.toBean(saveReqVO, ExportSellerConfigDO.class);
                exportSellerConfigMapper.insert(newConfig);
            } else {
                ExportSellerConfigDO updateConfig = BeanUtils.toBean(saveReqVO, ExportSellerConfigDO.class);
                updateConfig.setId(existing.getId());
                exportSellerConfigMapper.updateById(updateConfig);
            }
        } catch (Exception ex) {
            throw exception(EXPORT_DOC_SELLER_CONFIG_SAVE_FAILED);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finalizeExportDoc(Long id) {
        ExportDocDO doc = validateExportDocExists(id);
        if (!"DRAFT".equalsIgnoreCase(doc.getStatus())) {
            throw exception(EXPORT_DOC_STATUS_NOT_ALLOW);
        }
        List<ExportDocItemDO> items = exportDocItemMapper.selectListByDocId(id);
        if (CollUtil.isEmpty(items)) {
            throw exception(EXPORT_DOC_ITEM_EMPTY);
        }
        int rows = exportDocMapper.update(null, new LambdaUpdateWrapper<ExportDocDO>()
                .eq(ExportDocDO::getId, id)
                .eq(ExportDocDO::getStatus, "DRAFT")
                .set(ExportDocDO::getStatus, "FINALIZED"));
        if (rows == 0) {
            throw exception(EXPORT_DOC_STATUS_NOT_ALLOW);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeExportDoc(Long id) {
        ExportDocDO doc = validateExportDocExists(id);
        if (!"FINALIZED".equalsIgnoreCase(doc.getStatus())) {
            throw exception(EXPORT_DOC_STATUS_NOT_ALLOW);
        }
        int rows = exportDocMapper.update(null, new LambdaUpdateWrapper<ExportDocDO>()
                .eq(ExportDocDO::getId, id)
                .eq(ExportDocDO::getStatus, "FINALIZED")
                .set(ExportDocDO::getStatus, "DRAFT"));
        if (rows == 0) {
            throw exception(EXPORT_DOC_STATUS_NOT_ALLOW);
        }
    }

    @Override
    public ExportDocRespVO getDraftFromContract(Long contractId) {
        if (contractId == null) {
            throw exception(EXPORT_DOC_REFERENCE_NOT_FOUND, "销售合同");
        }
        SalesContractDO contract = salesContractMapper.selectById(contractId);
        if (contract == null) {
            throw exception(EXPORT_DOC_REFERENCE_NOT_FOUND, "销售合同");
        }
        if (!"EXPORT".equalsIgnoreCase(contract.getTradeType())) {
            throw exception(EXPORT_DOC_CONTRACT_NOT_EXPORT);
        }

        CustomerDO customer = contract.getCustomerId() != null ? customerMapper.selectById(contract.getCustomerId()) : null;

        ExportDocRespVO draft = new ExportDocRespVO();
        draft.setStatus(DEFAULT_STATUS);
        draft.setSourceType("CONTRACT");
        draft.setContractId(contract.getId());
        draft.setContractNo(contract.getContractNo());
        draft.setCustomerId(contract.getCustomerId());
        draft.setCustomerName(customer != null ? customer.getCompanyName() : contract.getClientName());

        if (customer != null && StringUtils.hasText(customer.getNameEn())) {
            draft.setBuyerName(customer.getNameEn());
        } else {
            draft.setBuyerName(contract.getClientName());
        }

        if (customer != null && StringUtils.hasText(customer.getAddressEn())) {
            draft.setBuyerAddress(customer.getAddressEn());
        } else if (customer != null && StringUtils.hasText(customer.getAddress())) {
            draft.setBuyerAddress(customer.getAddress());
        } else {
            draft.setBuyerAddress(contract.getClientAddress());
        }

        draft.setBuyerBankInfo(null);
        draft.setIncoterm(contract.getIncoterm());
        draft.setPortOfLoading(contract.getPortOfLoading());
        draft.setPortOfDischarge(contract.getPortOfDischarge());
        draft.setPaymentTerm(contract.getPaymentTerm());
        draft.setCurrency(StringUtils.hasText(contract.getCurrency()) ? contract.getCurrency() : DEFAULT_CURRENCY);

        // 卖方配置缺省带出
        prefillSellerConfigRespIfEmpty(draft);

        // 合同明细带出
        List<SalesContractItemDO> contractItems = salesContractItemMapper.selectListByContractId(contractId);
        List<ExportDocItemRespVO> items = new ArrayList<>();
        if (CollUtil.isNotEmpty(contractItems)) {
            for (int i = 0; i < contractItems.size(); i++) {
                SalesContractItemDO ci = contractItems.get(i);
                ExportDocItemRespVO item = new ExportDocItemRespVO();
                item.setLineNo(ci.getLineNo() != null ? ci.getLineNo() : i + 1);
                item.setNameCn(ci.getProductName());
                item.setNameEn(null);
                item.setSpec(buildContractItemSpec(ci));
                item.setQuantity(ci.getQuantity());
                item.setUnit(contract.getUnit());
                item.setUnitPrice(ci.getUnitPrice());
                item.setAmount(ci.getAmount());
                item.setOrigin(DEFAULT_ORIGIN);
                item.setNetWeight(null);
                item.setGrossWeight(null);
                item.setPackages(null);
                item.setVolume(null);
                item.setRemark(ci.getRemark());
                items.add(item);
            }
        }
        draft.setItems(items);
        return draft;
    }

    @Override
    public ExportDocRespVO getDraftFromShipment(Long shipmentId) {
        if (shipmentId == null) {
            throw exception(EXPORT_DOC_REFERENCE_NOT_FOUND, "发货单");
        }
        ShipmentDO shipment = shipmentMapper.selectById(shipmentId);
        if (shipment == null) {
            throw exception(EXPORT_DOC_REFERENCE_NOT_FOUND, "发货单");
        }

        CustomerDO customer = shipment.getCustomerId() != null ? customerMapper.selectById(shipment.getCustomerId()) : null;

        ExportDocRespVO draft = new ExportDocRespVO();
        draft.setStatus(DEFAULT_STATUS);
        draft.setSourceType("SHIPMENT");
        draft.setShipmentId(shipment.getId());
        draft.setShipmentNo(shipment.getShipmentNo());
        draft.setContractId(shipment.getSalesContractId());
        draft.setContractNo(shipment.getContractNo());
        draft.setCustomerId(shipment.getCustomerId());

        // 实发客户名优先于合同抬头
        String customerName = StringUtils.hasText(shipment.getCustomerName()) ? shipment.getCustomerName()
                : (customer != null ? customer.getCompanyName() : null);
        draft.setCustomerName(customerName);

        if (customer != null && StringUtils.hasText(customer.getNameEn())) {
            draft.setBuyerName(customer.getNameEn());
        } else {
            draft.setBuyerName(customerName);
        }

        if (customer != null && StringUtils.hasText(customer.getAddressEn())) {
            draft.setBuyerAddress(customer.getAddressEn());
        } else if (customer != null && StringUtils.hasText(customer.getAddress())) {
            draft.setBuyerAddress(customer.getAddress());
        } else {
            draft.setBuyerAddress(shipment.getReceiveAddress());
        }

        draft.setBuyerBankInfo(null);

        // 合同条款经发货单的合同联动带出
        if (shipment.getSalesContractId() != null) {
            SalesContractDO contract = salesContractMapper.selectById(shipment.getSalesContractId());
            if (contract != null) {
                draft.setIncoterm(contract.getIncoterm());
                draft.setPortOfLoading(contract.getPortOfLoading());
                draft.setPortOfDischarge(contract.getPortOfDischarge());
                draft.setPaymentTerm(contract.getPaymentTerm());
                if (!StringUtils.hasText(draft.getContractNo())) {
                    draft.setContractNo(contract.getContractNo());
                }
                if (!StringUtils.hasText(shipment.getCurrency()) && StringUtils.hasText(contract.getCurrency())) {
                    draft.setCurrency(contract.getCurrency());
                }
            }
        }
        if (!StringUtils.hasText(draft.getCurrency())) {
            draft.setCurrency(StringUtils.hasText(shipment.getCurrency()) ? shipment.getCurrency() : DEFAULT_CURRENCY);
        }

        // 卖方配置缺省带出
        prefillSellerConfigRespIfEmpty(draft);

        // 发货明细与卷清单净重 SUM 聚合
        List<ShipmentItemDO> shipmentItems = shipmentItemMapper.selectListByShipmentId(shipmentId);
        List<ShipmentRollDO> rolls = shipmentRollMapper.selectListByShipmentId(shipmentId);
        java.util.Map<Long, BigDecimal> netWeightMap = new java.util.HashMap<>();
        if (CollUtil.isNotEmpty(rolls)) {
            for (ShipmentRollDO roll : rolls) {
                if (roll.getShipmentItemId() != null && roll.getWeight() != null) {
                    netWeightMap.merge(roll.getShipmentItemId(), roll.getWeight(), BigDecimal::add);
                }
            }
        }

        List<ExportDocItemRespVO> items = new ArrayList<>();
        if (CollUtil.isNotEmpty(shipmentItems)) {
            for (int i = 0; i < shipmentItems.size(); i++) {
                ShipmentItemDO si = shipmentItems.get(i);
                ExportDocItemRespVO item = new ExportDocItemRespVO();
                item.setLineNo(i + 1);
                item.setNameCn(si.getProductName());
                item.setNameEn(null);
                item.setSpec(StringUtils.hasText(si.getColorName()) ? si.getColorName() : si.getCustomerColorNo());
                item.setQuantity(si.getQuantity());
                item.setUnit(si.getUnit());
                item.setUnitPrice(si.getUnitPrice());
                item.setAmount(si.getAmount());
                item.setOrigin(DEFAULT_ORIGIN);

                BigDecimal netWeight = netWeightMap.get(si.getId());
                if (netWeight != null && netWeight.compareTo(BigDecimal.ZERO) > 0) {
                    item.setNetWeight(netWeight.setScale(2, RoundingMode.HALF_UP));
                } else {
                    item.setNetWeight(null);
                }
                item.setGrossWeight(null);
                item.setPackages(null);
                item.setVolume(null);
                item.setRemark(si.getRemark());
                items.add(item);
            }
        }
        draft.setItems(items);
        return draft;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resyncFromSource(Long id) {
        ExportDocDO doc = validateExportDocExists(id);
        if (!"DRAFT".equalsIgnoreCase(doc.getStatus())) {
            throw exception(EXPORT_DOC_STATUS_NOT_ALLOW);
        }
        if ("MANUAL".equalsIgnoreCase(doc.getSourceType()) || !StringUtils.hasText(doc.getSourceType())) {
            throw exception(EXPORT_DOC_SOURCE_REQUIRED);
        }

        ExportDocRespVO candidate;
        if ("CONTRACT".equalsIgnoreCase(doc.getSourceType())) {
            if (doc.getContractId() == null) {
                throw exception(EXPORT_DOC_REFERENCE_NOT_FOUND, "销售合同");
            }
            candidate = getDraftFromContract(doc.getContractId());
        } else if ("SHIPMENT".equalsIgnoreCase(doc.getSourceType())) {
            if (doc.getShipmentId() == null) {
                throw exception(EXPORT_DOC_REFERENCE_NOT_FOUND, "发货单");
            }
            candidate = getDraftFromShipment(doc.getShipmentId());
        } else {
            throw exception(EXPORT_DOC_SOURCE_REQUIRED);
        }

        // 1. 抬头层补空：只填当前为空字段、永不覆盖已编辑值
        if (!StringUtils.hasText(doc.getCustomerName())) doc.setCustomerName(candidate.getCustomerName());
        if (!StringUtils.hasText(doc.getBuyerName())) doc.setBuyerName(candidate.getBuyerName());
        if (!StringUtils.hasText(doc.getBuyerAddress())) doc.setBuyerAddress(candidate.getBuyerAddress());
        if (!StringUtils.hasText(doc.getSellerName())) doc.setSellerName(candidate.getSellerName());
        if (!StringUtils.hasText(doc.getSellerAddress())) doc.setSellerAddress(candidate.getSellerAddress());
        if (!StringUtils.hasText(doc.getSellerBankInfo())) doc.setSellerBankInfo(candidate.getSellerBankInfo());
        if (!StringUtils.hasText(doc.getIncoterm())) doc.setIncoterm(candidate.getIncoterm());
        if (!StringUtils.hasText(doc.getPortOfLoading())) doc.setPortOfLoading(candidate.getPortOfLoading());
        if (!StringUtils.hasText(doc.getPortOfDischarge())) doc.setPortOfDischarge(candidate.getPortOfDischarge());
        if (!StringUtils.hasText(doc.getCountryOfDest())) doc.setCountryOfDest(candidate.getCountryOfDest());
        if (!StringUtils.hasText(doc.getTransportMode())) doc.setTransportMode(candidate.getTransportMode());
        if (!StringUtils.hasText(doc.getPaymentTerm())) doc.setPaymentTerm(candidate.getPaymentTerm());
        if (!StringUtils.hasText(doc.getCurrency())) doc.setCurrency(candidate.getCurrency());
        if (!StringUtils.hasText(doc.getShippingMark())) doc.setShippingMark(candidate.getShippingMark());
        if (!StringUtils.hasText(doc.getContractNo()) && StringUtils.hasText(candidate.getContractNo())) doc.setContractNo(candidate.getContractNo());
        if (doc.getContractId() == null && candidate.getContractId() != null) doc.setContractId(candidate.getContractId());

        // 2. 单据 meta 层补空（如果单证元数据为空字符串或 null，而来源 candidate 中有内容）
        if (!StringUtils.hasText(doc.getPiMeta()) && StringUtils.hasText(candidate.getPiMeta())) doc.setPiMeta(candidate.getPiMeta());
        if (!StringUtils.hasText(doc.getCiMeta()) && StringUtils.hasText(candidate.getCiMeta())) doc.setCiMeta(candidate.getCiMeta());
        if (!StringUtils.hasText(doc.getPlMeta()) && StringUtils.hasText(candidate.getPlMeta())) doc.setPlMeta(candidate.getPlMeta());

        exportDocMapper.updateById(doc);

        // 3. 明细行层补空：按来源行匹配补空
        List<ExportDocItemDO> currentItems = exportDocItemMapper.selectListByDocId(id);
        List<ExportDocItemRespVO> candItems = candidate.getItems();
        List<ExportDocItemSaveReqVO> mergedItems = new ArrayList<>();

        int maxLen = Math.max(currentItems.size(), candItems != null ? candItems.size() : 0);
        for (int i = 0; i < maxLen; i++) {
            ExportDocItemSaveReqVO itemReq = new ExportDocItemSaveReqVO();
            ExportDocItemDO cur = i < currentItems.size() ? currentItems.get(i) : null;
            ExportDocItemRespVO cand = (candItems != null && i < candItems.size()) ? candItems.get(i) : null;

            if (cur != null) {
                itemReq.setId(cur.getId());
                itemReq.setNameCn(StringUtils.hasText(cur.getNameCn()) ? cur.getNameCn() : (cand != null ? cand.getNameCn() : null));
                itemReq.setNameEn(StringUtils.hasText(cur.getNameEn()) ? cur.getNameEn() : (cand != null ? cand.getNameEn() : null));
                itemReq.setHsCode(StringUtils.hasText(cur.getHsCode()) ? cur.getHsCode() : (cand != null ? cand.getHsCode() : null));
                itemReq.setSpec(StringUtils.hasText(cur.getSpec()) ? cur.getSpec() : (cand != null ? cand.getSpec() : null));
                itemReq.setQuantity(cur.getQuantity() != null && cur.getQuantity().compareTo(BigDecimal.ZERO) > 0 ? cur.getQuantity() : (cand != null ? cand.getQuantity() : cur.getQuantity()));
                itemReq.setUnit(StringUtils.hasText(cur.getUnit()) ? cur.getUnit() : (cand != null ? cand.getUnit() : null));
                itemReq.setUnitPrice(cur.getUnitPrice() != null && cur.getUnitPrice().compareTo(BigDecimal.ZERO) > 0 ? cur.getUnitPrice() : (cand != null ? cand.getUnitPrice() : cur.getUnitPrice()));
                itemReq.setAmount(cur.getAmount() != null && cur.getAmount().compareTo(BigDecimal.ZERO) > 0 ? cur.getAmount() : (cand != null ? cand.getAmount() : cur.getAmount()));
                itemReq.setNetWeight(cur.getNetWeight() != null ? cur.getNetWeight() : (cand != null ? cand.getNetWeight() : null));
                itemReq.setGrossWeight(cur.getGrossWeight() != null ? cur.getGrossWeight() : (cand != null ? cand.getGrossWeight() : null));
                itemReq.setPackages(cur.getPackages() != null ? cur.getPackages() : (cand != null ? cand.getPackages() : null));
                itemReq.setVolume(cur.getVolume() != null ? cur.getVolume() : (cand != null ? cand.getVolume() : null));
                itemReq.setOrigin(StringUtils.hasText(cur.getOrigin()) ? cur.getOrigin() : (cand != null ? cand.getOrigin() : DEFAULT_ORIGIN));
                itemReq.setRemark(StringUtils.hasText(cur.getRemark()) ? cur.getRemark() : (cand != null ? cand.getRemark() : null));
            } else if (cand != null) {
                // 来源中新增的行
                itemReq.setNameCn(cand.getNameCn());
                itemReq.setNameEn(cand.getNameEn());
                itemReq.setHsCode(cand.getHsCode());
                itemReq.setSpec(cand.getSpec());
                itemReq.setQuantity(cand.getQuantity());
                itemReq.setUnit(cand.getUnit());
                itemReq.setUnitPrice(cand.getUnitPrice());
                itemReq.setAmount(cand.getAmount());
                itemReq.setNetWeight(cand.getNetWeight());
                itemReq.setGrossWeight(cand.getGrossWeight());
                itemReq.setPackages(cand.getPackages());
                itemReq.setVolume(cand.getVolume());
                itemReq.setOrigin(StringUtils.hasText(cand.getOrigin()) ? cand.getOrigin() : DEFAULT_ORIGIN);
                itemReq.setRemark(cand.getRemark());
            }
            mergedItems.add(itemReq);
        }

        exportDocItemMapper.deleteByDocId(id);
        saveItems(id, mergedItems);
    }

    @Override
    @LogRecord(type = "打印", subType = "EXPORT_DOC_PRINT", bizNo = "{{#id}}", success = "打印出口单证【{{#id}}】-【{{#template}}】")
    public ExportDocPrintRespVO getExportDocPrintData(Long id, String template) {
        // 1. 校验单据存在（DRAFT 与 FINALIZED 均可打印）
        ExportDocDO doc = validateExportDocExists(id);

        // 2. 留痕上下文
        LogRecordContext.putVariable("id", doc.getId());
        LogRecordContext.putVariable("template", StringUtils.hasText(template) ? template : "ALL");

        // 3. 基础字段拷贝（豁免脱敏）
        ExportDocPrintRespVO vo = BeanUtils.toBean(doc, ExportDocPrintRespVO.class);

        // 4. 卖方银行与抬头字段（租户配置 -> PI meta 可覆盖）
        ExportSellerConfigDO sellerConfig = exportSellerConfigMapper.selectByTenant();
        if (sellerConfig != null) {
            vo.setSellerBankName(sellerConfig.getBankName());
            vo.setSellerBankAccount(sellerConfig.getBankAccount());
            vo.setSellerBankSwift(sellerConfig.getBankSwift());
            vo.setSellerBankAddress(sellerConfig.getBankAddress());
            if (!StringUtils.hasText(vo.getSellerName())) {
                vo.setSellerName(sellerConfig.getSellerName());
            }
            if (!StringUtils.hasText(vo.getSellerAddress())) {
                vo.setSellerAddress(sellerConfig.getSellerAddress());
            }
        }
        if ("PI".equalsIgnoreCase(template) && StringUtils.hasText(doc.getPiMeta())) {
            try {
                JSONObject piObj = JSONUtil.parseObj(doc.getPiMeta());
                if (StringUtils.hasText(piObj.getStr("bankName"))) {
                    vo.setSellerBankName(piObj.getStr("bankName"));
                }
                if (StringUtils.hasText(piObj.getStr("bankAccount"))) {
                    vo.setSellerBankAccount(piObj.getStr("bankAccount"));
                }
                if (StringUtils.hasText(piObj.getStr("bankSwift"))) {
                    vo.setSellerBankSwift(piObj.getStr("bankSwift"));
                }
                if (StringUtils.hasText(piObj.getStr("bankAddress"))) {
                    vo.setSellerBankAddress(piObj.getStr("bankAddress"));
                }
            } catch (Exception ignore) {
                // 忽略异常
            }
        }

        // 5. 客户门户验真二维码 URL（未配置时不阻断打印，二维码位由前端渲染提示）
        String qrUrl = null;
        String baseUrl = configApi != null ? configApi.getConfigValueByKey("luna.portal.base-url") : null;
        if (StringUtils.hasText(baseUrl)) {
            String cleanBaseUrl = baseUrl.trim().replaceAll("/+$", "");
            qrUrl = cleanBaseUrl + "/portal/export-doc?no=" + doc.getDocNo();
        }
        vo.setQrUrl(qrUrl);

        // 6. 货物明细
        List<ExportDocItemDO> items = exportDocItemMapper.selectListByDocId(id);
        List<ExportDocItemRespVO> itemVOList = BeanUtils.toBean(items, ExportDocItemRespVO.class);
        vo.setItems(itemVOList != null ? itemVOList : Collections.emptyList());

        // 7. 汇总合计：手填优先、空则求和
        BigDecimal sumQuantity = BigDecimal.ZERO;
        BigDecimal sumAmount = BigDecimal.ZERO;
        BigDecimal sumNetWeight = BigDecimal.ZERO;
        BigDecimal sumGrossWeight = BigDecimal.ZERO;
        int sumPackages = 0;
        BigDecimal sumVolume = BigDecimal.ZERO;

        boolean hasNetWeight = false;
        boolean hasGrossWeight = false;
        boolean hasPackages = false;
        boolean hasVolume = false;

        if (CollUtil.isNotEmpty(items)) {
            for (ExportDocItemDO item : items) {
                if (item.getQuantity() != null) {
                    sumQuantity = sumQuantity.add(item.getQuantity());
                }
                if (item.getAmount() != null) {
                    sumAmount = sumAmount.add(item.getAmount());
                }
                if (item.getNetWeight() != null) {
                    sumNetWeight = sumNetWeight.add(item.getNetWeight());
                    hasNetWeight = true;
                }
                if (item.getGrossWeight() != null) {
                    sumGrossWeight = sumGrossWeight.add(item.getGrossWeight());
                    hasGrossWeight = true;
                }
                if (item.getPackages() != null) {
                    sumPackages += item.getPackages();
                    hasPackages = true;
                }
                if (item.getVolume() != null) {
                    sumVolume = sumVolume.add(item.getVolume());
                    hasVolume = true;
                }
            }
        }

        boolean totalCustom = false;
        BigDecimal finalTotalQuantity = sumQuantity.setScale(2, RoundingMode.HALF_UP);
        BigDecimal finalTotalAmount = sumAmount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal finalTotalNetWeight = hasNetWeight ? sumNetWeight.setScale(2, RoundingMode.HALF_UP) : null;
        BigDecimal finalTotalGrossWeight = hasGrossWeight ? sumGrossWeight.setScale(2, RoundingMode.HALF_UP) : null;
        Integer finalTotalPackages = hasPackages ? sumPackages : null;
        BigDecimal finalTotalVolume = hasVolume ? sumVolume.setScale(3, RoundingMode.HALF_UP) : null;

        if (StringUtils.hasText(doc.getPlMeta())) {
            try {
                JSONObject plObj = JSONUtil.parseObj(doc.getPlMeta());
                if (plObj.get("totalPackages") != null) {
                    finalTotalPackages = plObj.getInt("totalPackages");
                    totalCustom = true;
                }
                if (plObj.get("totalNetWeight") != null) {
                    finalTotalNetWeight = plObj.getBigDecimal("totalNetWeight");
                    totalCustom = true;
                }
                if (plObj.get("totalGrossWeight") != null) {
                    finalTotalGrossWeight = plObj.getBigDecimal("totalGrossWeight");
                    totalCustom = true;
                }
                if (plObj.get("totalQuantity") != null) {
                    finalTotalQuantity = plObj.getBigDecimal("totalQuantity");
                    totalCustom = true;
                }
                if (plObj.get("totalVolume") != null) {
                    finalTotalVolume = plObj.getBigDecimal("totalVolume");
                    totalCustom = true;
                }
                if (plObj.get("totalAmount") != null) {
                    finalTotalAmount = plObj.getBigDecimal("totalAmount");
                    totalCustom = true;
                }
            } catch (Exception ignore) {
                // plMeta 非合法 JSON 时回退服务端求和
            }
        }

        vo.setTotalQuantity(finalTotalQuantity);
        vo.setTotalAmount(finalTotalAmount);
        vo.setTotalNetWeight(finalTotalNetWeight);
        vo.setTotalGrossWeight(finalTotalGrossWeight);
        vo.setTotalPackages(finalTotalPackages);
        vo.setTotalVolume(finalTotalVolume);
        vo.setTotalCustom(totalCustom);

        return vo;
    }

    private void prefillSellerConfigRespIfEmpty(ExportDocRespVO draft) {
        ExportSellerConfigDO sellerConfig = exportSellerConfigMapper.selectByTenant();
        if (sellerConfig == null) {
            return;
        }
        if (!StringUtils.hasText(draft.getSellerName())) {
            draft.setSellerName(sellerConfig.getSellerName());
        }
        if (!StringUtils.hasText(draft.getSellerAddress())) {
            draft.setSellerAddress(sellerConfig.getSellerAddress());
        }
        if (!StringUtils.hasText(draft.getSellerBankInfo())) {
            StringBuilder sb = new StringBuilder();
            if (StringUtils.hasText(sellerConfig.getBankName())) sb.append(sellerConfig.getBankName()).append(" ");
            if (StringUtils.hasText(sellerConfig.getBankAccount())) sb.append("A/C: ").append(sellerConfig.getBankAccount()).append(" ");
            if (StringUtils.hasText(sellerConfig.getBankSwift())) sb.append("SWIFT: ").append(sellerConfig.getBankSwift()).append(" ");
            if (StringUtils.hasText(sellerConfig.getBankAddress())) sb.append(sellerConfig.getBankAddress());
            String bankInfo = sb.toString().trim();
            if (!bankInfo.isEmpty()) {
                draft.setSellerBankInfo(bankInfo);
            }
        }
    }

    private String buildContractItemSpec(SalesContractItemDO ci) {
        if (StringUtils.hasText(ci.getComposition()) && StringUtils.hasText(ci.getWidth())) {
            return ci.getComposition().trim() + " " + ci.getWidth().trim();
        } else if (StringUtils.hasText(ci.getComposition())) {
            return ci.getComposition().trim();
        } else if (StringUtils.hasText(ci.getWidth())) {
            return ci.getWidth().trim();
        }
        return null;
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
