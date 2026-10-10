package com.luna.lunasrv.module.lunaerp.service.business.exportdoc;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.controller.business.exportdoc.vo.ExportDocItemRespVO;
import com.luna.lunasrv.module.lunaerp.controller.business.exportdoc.vo.ExportDocItemSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.business.exportdoc.vo.ExportDocRespVO;
import com.luna.lunasrv.module.lunaerp.controller.business.exportdoc.vo.ExportDocSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.business.exportdoc.vo.ExportSellerConfigRespVO;
import com.luna.lunasrv.module.lunaerp.controller.business.exportdoc.vo.ExportSellerConfigSaveReqVO;
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
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.luna.lunasrv.module.lunaerp.enums.ExportDocErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link ExportDocServiceImpl} 纯 Mockito 单元测试
 */
class ExportDocServiceImplTest {

    @Mock
    private ExportDocMapper exportDocMapper;

    @Mock
    private ExportDocItemMapper exportDocItemMapper;

    @Mock
    private ExportSellerConfigMapper exportSellerConfigMapper;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private SalesContractMapper salesContractMapper;

    @Mock
    private SalesContractItemMapper salesContractItemMapper;

    @Mock
    private ShipmentMapper shipmentMapper;

    @Mock
    private ShipmentItemMapper shipmentItemMapper;

    @Mock
    private ShipmentRollMapper shipmentRollMapper;

    @InjectMocks
    private ExportDocServiceImpl exportDocService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ExportDocDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ExportDocItemDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ExportSellerConfigDO.class);
        TenantContextHolder.setTenantId(1L);

        when(exportDocMapper.insert(any(ExportDocDO.class))).thenAnswer(invocation -> {
            ExportDocDO doc = invocation.getArgument(0);
            doc.setId(100L);
            return 1;
        });
        when(exportDocMapper.updateById(any(ExportDocDO.class))).thenReturn(1);
        when(exportDocItemMapper.insert(any(ExportDocItemDO.class))).thenReturn(1);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    private CustomerDO createMockCustomer(Long id) {
        CustomerDO customer = new CustomerDO();
        customer.setId(id);
        customer.setCompanyName("Acme Corp Ltd");
        customer.setNameEn("ACME GLOBAL CO., LTD.");
        customer.setAddress("浙江省杭州市西湖区100号");
        customer.setAddressEn("100 West Lake Rd, Hangzhou, China");
        return customer;
    }

    @Test
    @DisplayName("创建出口单证套：自动取号按当前年 ED{yyyy}0001，包含软删行扫描与明细金额重算")
    void testCreateExportDoc_Success_AutoDocNo() {
        // Arrange
        CustomerDO customer = createMockCustomer(1L);
        when(customerMapper.selectById(1L)).thenReturn(customer);

        int currentYear = LocalDate.now().getYear();
        String prefix = "ED" + currentYear;
        // 软删行含 ED{currentYear}0003，max+1 应取 ED{currentYear}0004
        ExportDocDO deletedRow = new ExportDocDO();
        deletedRow.setDocNo(prefix + "0003");
        when(exportDocMapper.selectListByDocNoPrefixIncludingDeleted(prefix))
                .thenReturn(Collections.singletonList(deletedRow));
        when(exportDocMapper.selectByDocNoIncludingDeleted(prefix + "0004")).thenReturn(null);

        ExportSellerConfigDO sellerConfig = new ExportSellerConfigDO();
        sellerConfig.setSellerName("LUNA TEXTILE CO., LTD.");
        sellerConfig.setSellerAddress("No. 88 Luna Industrial Park, Hangzhou");
        sellerConfig.setBankName("CITIC BANK");
        sellerConfig.setBankAccount("99887766");
        when(exportSellerConfigMapper.selectByTenant()).thenReturn(sellerConfig);

        ExportDocSaveReqVO req = new ExportDocSaveReqVO();
        req.setCustomerId(1L);
        ExportDocItemSaveReqVO item1 = new ExportDocItemSaveReqVO();
        item1.setNameEn("Silk Fabric");
        item1.setQuantity(new BigDecimal("100.50"));
        item1.setUnitPrice(new BigDecimal("12.00"));
        // 前端传入计算错误的金额 10.00，服务端应重算为 1206.00
        item1.setAmount(new BigDecimal("10.00"));
        req.setItems(Collections.singletonList(item1));

        // Act
        Long docId = exportDocService.createExportDoc(req);

        // Assert
        assertEquals(100L, docId);

        ArgumentCaptor<ExportDocDO> docCaptor = ArgumentCaptor.forClass(ExportDocDO.class);
        verify(exportDocMapper).insert(docCaptor.capture());
        ExportDocDO savedDoc = docCaptor.getValue();
        assertEquals(prefix + "0004", savedDoc.getDocNo());
        assertEquals("DRAFT", savedDoc.getStatus());
        assertEquals("MANUAL", savedDoc.getSourceType());
        assertEquals("USD", savedDoc.getCurrency());
        assertEquals("ACME GLOBAL CO., LTD.", savedDoc.getBuyerName());
        assertEquals("100 West Lake Rd, Hangzhou, China", savedDoc.getBuyerAddress());
        assertEquals("LUNA TEXTILE CO., LTD.", savedDoc.getSellerName());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ExportDocItemDO>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(exportDocItemMapper).insertBatch(itemsCaptor.capture());
        List<ExportDocItemDO> savedItems = itemsCaptor.getValue();
        assertEquals(1, savedItems.size());
        ExportDocItemDO savedItem = savedItems.get(0);
        assertEquals(1, savedItem.getLineNo());
        assertEquals("Silk Fabric", savedItem.getNameEn());
        assertEquals(new BigDecimal("1206.00"), savedItem.getAmount());
    }

    @Test
    @DisplayName("创建出口单证套：手填单号已存在（含已删除行）报 EXPORT_DOC_NO_DUPLICATE")
    void testCreateExportDoc_ManualDocNoDuplicate() {
        CustomerDO customer = createMockCustomer(1L);
        when(customerMapper.selectById(1L)).thenReturn(customer);

        ExportDocDO existing = new ExportDocDO();
        existing.setId(99L);
        existing.setDocNo("ED20268888");
        when(exportDocMapper.selectByDocNoIncludingDeleted("ED20268888")).thenReturn(existing);

        ExportDocSaveReqVO req = new ExportDocSaveReqVO();
        req.setCustomerId(1L);
        req.setDocNo("ED20268888");

        ServiceException ex = assertThrows(ServiceException.class, () -> exportDocService.createExportDoc(req));
        assertEquals(EXPORT_DOC_NO_DUPLICATE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("创建出口单证套：客户不存在报 EXPORT_DOC_REFERENCE_NOT_FOUND")
    void testCreateExportDoc_CustomerNotFound() {
        when(customerMapper.selectById(999L)).thenReturn(null);

        ExportDocSaveReqVO req = new ExportDocSaveReqVO();
        req.setCustomerId(999L);

        ServiceException ex = assertThrows(ServiceException.class, () -> exportDocService.createExportDoc(req));
        assertEquals(EXPORT_DOC_REFERENCE_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("更新出口单证套：非 DRAFT 状态修改报 EXPORT_DOC_STATUS_NOT_ALLOW")
    void testUpdateExportDoc_Finalized_Reject() {
        ExportDocDO finalizedDoc = new ExportDocDO();
        finalizedDoc.setId(10L);
        finalizedDoc.setStatus("FINALIZED");
        when(exportDocMapper.selectById(10L)).thenReturn(finalizedDoc);

        ExportDocSaveReqVO req = new ExportDocSaveReqVO();
        req.setId(10L);

        ServiceException ex = assertThrows(ServiceException.class, () -> exportDocService.updateExportDoc(req));
        assertEquals(EXPORT_DOC_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("更新出口单证套：携带其他单据的明细 ID 报 EXPORT_DOC_ITEM_INVALID_ID")
    void testUpdateExportDoc_CrossDocItemId_Reject() {
        ExportDocDO draftDoc = new ExportDocDO();
        draftDoc.setId(10L);
        draftDoc.setStatus("DRAFT");
        when(exportDocMapper.selectById(10L)).thenReturn(draftDoc);

        ExportDocItemDO dbItem1 = new ExportDocItemDO();
        dbItem1.setId(101L);
        dbItem1.setDocId(10L);
        when(exportDocItemMapper.selectListByDocId(10L)).thenReturn(Collections.singletonList(dbItem1));

        ExportDocSaveReqVO req = new ExportDocSaveReqVO();
        req.setId(10L);
        ExportDocItemSaveReqVO rogueItem = new ExportDocItemSaveReqVO();
        rogueItem.setId(999L); // 不属于此 doc
        rogueItem.setNameEn("Cotton Yarn");
        req.setItems(Collections.singletonList(rogueItem));

        ServiceException ex = assertThrows(ServiceException.class, () -> exportDocService.updateExportDoc(req));
        assertEquals(EXPORT_DOC_ITEM_INVALID_ID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("更新出口单证套：聚合替换明细、重新编号 line_no、服务端重新计算金额")
    void testUpdateExportDoc_Success_AggregateReplaceItems() {
        ExportDocDO draftDoc = new ExportDocDO();
        draftDoc.setId(10L);
        draftDoc.setStatus("DRAFT");
        draftDoc.setDocNo("ED20260001");
        when(exportDocMapper.selectById(10L)).thenReturn(draftDoc);

        ExportDocItemDO dbItem1 = new ExportDocItemDO();
        dbItem1.setId(101L);
        dbItem1.setDocId(10L);
        when(exportDocItemMapper.selectListByDocId(10L)).thenReturn(Collections.singletonList(dbItem1));

        ExportDocSaveReqVO req = new ExportDocSaveReqVO();
        req.setId(10L);
        req.setDocNo("ED20260001");

        List<ExportDocItemSaveReqVO> newItems = new ArrayList<>();
        ExportDocItemSaveReqVO item1 = new ExportDocItemSaveReqVO();
        item1.setId(101L);
        item1.setNameEn("Goods A");
        item1.setQuantity(new BigDecimal("2.5"));
        item1.setUnitPrice(new BigDecimal("4.00"));

        ExportDocItemSaveReqVO item2 = new ExportDocItemSaveReqVO();
        item2.setNameEn("Goods B");
        item2.setQuantity(new BigDecimal("10.0"));
        item2.setUnitPrice(new BigDecimal("5.50"));

        newItems.add(item1);
        newItems.add(item2);
        req.setItems(newItems);

        // Act
        exportDocService.updateExportDoc(req);

        // Assert
        verify(exportDocItemMapper).deleteByDocId(10L);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ExportDocItemDO>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(exportDocItemMapper).insertBatch(itemsCaptor.capture());
        List<ExportDocItemDO> insertedItems = itemsCaptor.getValue();
        assertEquals(2, insertedItems.size());
        assertEquals(1, insertedItems.get(0).getLineNo());
        assertEquals(new BigDecimal("10.00"), insertedItems.get(0).getAmount());
        assertEquals(2, insertedItems.get(1).getLineNo());
        assertEquals(new BigDecimal("55.00"), insertedItems.get(1).getAmount());
    }

    @Test
    @DisplayName("删除出口单证套：非 DRAFT 状态拒绝报 EXPORT_DOC_STATUS_NOT_ALLOW")
    void testDeleteExportDoc_Finalized_Reject() {
        ExportDocDO finalizedDoc = new ExportDocDO();
        finalizedDoc.setId(10L);
        finalizedDoc.setStatus("FINALIZED");
        when(exportDocMapper.selectById(10L)).thenReturn(finalizedDoc);

        ServiceException ex = assertThrows(ServiceException.class, () -> exportDocService.deleteExportDoc(10L));
        assertEquals(EXPORT_DOC_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("删除出口单证套：DRAFT 状态级联软删主表与明细")
    void testDeleteExportDoc_Success() {
        ExportDocDO draftDoc = new ExportDocDO();
        draftDoc.setId(10L);
        draftDoc.setStatus("DRAFT");
        when(exportDocMapper.selectById(10L)).thenReturn(draftDoc);

        exportDocService.deleteExportDoc(10L);

        verify(exportDocMapper).deleteById(10L);
        verify(exportDocItemMapper).deleteByDocId(10L);
    }

    @Test
    @DisplayName("获得单证套详情：不存在报 EXPORT_DOC_NOT_EXISTS，存在则组装明细")
    void testGetExportDoc() {
        when(exportDocMapper.selectById(999L)).thenReturn(null);
        ServiceException ex = assertThrows(ServiceException.class, () -> exportDocService.getExportDoc(999L));
        assertEquals(EXPORT_DOC_NOT_EXISTS.getCode(), ex.getCode());

        ExportDocDO doc = new ExportDocDO();
        doc.setId(1L);
        doc.setDocNo("ED20260001");
        when(exportDocMapper.selectById(1L)).thenReturn(doc);

        ExportDocItemDO item = new ExportDocItemDO();
        item.setId(11L);
        item.setDocId(1L);
        item.setNameEn("Fabric");
        when(exportDocItemMapper.selectListByDocId(1L)).thenReturn(Collections.singletonList(item));

        ExportDocRespVO resp = exportDocService.getExportDoc(1L);
        assertNotNull(resp);
        assertEquals("ED20260001", resp.getDocNo());
        assertEquals(1, resp.getItems().size());
        assertEquals("Fabric", resp.getItems().get(0).getNameEn());
    }

    @Test
    @DisplayName("getNextDocNo：根据软删与有效行生成建议单号")
    void testGetNextDocNo() {
        int currentYear = LocalDate.now().getYear();
        String prefix = "ED" + currentYear;
        ExportDocDO row = new ExportDocDO();
        row.setDocNo(prefix + "0010");
        when(exportDocMapper.selectListByDocNoPrefixIncludingDeleted(prefix))
                .thenReturn(Collections.singletonList(row));

        String nextNo = exportDocService.getNextDocNo();
        assertEquals(prefix + "0011", nextNo);
    }

    @Test
    @DisplayName("卖方出口配置：首次保存（不存在时插入）")
    void testSaveSellerConfig_Insert() {
        when(exportSellerConfigMapper.selectByTenant()).thenReturn(null);

        ExportSellerConfigSaveReqVO req = new ExportSellerConfigSaveReqVO();
        req.setSellerName("LUNA OVERSEAS TRADING");
        req.setSellerAddress("Hangzhou, China");
        req.setBankName("BANK OF CHINA");

        exportDocService.saveSellerConfig(req);

        ArgumentCaptor<ExportSellerConfigDO> captor = ArgumentCaptor.forClass(ExportSellerConfigDO.class);
        verify(exportSellerConfigMapper).insert(captor.capture());
        ExportSellerConfigDO saved = captor.getValue();
        assertEquals("LUNA OVERSEAS TRADING", saved.getSellerName());
        assertEquals("Hangzhou, China", saved.getSellerAddress());
        assertEquals("BANK OF CHINA", saved.getBankName());
    }

    @Test
    @DisplayName("卖方出口配置：再次保存（已存在时更新，幂等）")
    void testSaveSellerConfig_Update() {
        ExportSellerConfigDO existing = new ExportSellerConfigDO();
        existing.setId(50L);
        existing.setSellerName("OLD NAME");
        when(exportSellerConfigMapper.selectByTenant()).thenReturn(existing);

        ExportSellerConfigSaveReqVO req = new ExportSellerConfigSaveReqVO();
        req.setSellerName("NEW NAME");
        req.setSellerAddress("New Address");

        exportDocService.saveSellerConfig(req);

        ArgumentCaptor<ExportSellerConfigDO> captor = ArgumentCaptor.forClass(ExportSellerConfigDO.class);
        verify(exportSellerConfigMapper).updateById(captor.capture());
        ExportSellerConfigDO updated = captor.getValue();
        assertEquals(50L, updated.getId());
        assertEquals("NEW NAME", updated.getSellerName());
        assertEquals("New Address", updated.getSellerAddress());
    }

    @Test
    @DisplayName("卖方出口配置：并发/数据库异常时报 EXPORT_DOC_SELLER_CONFIG_SAVE_FAILED")
    void testSaveSellerConfig_ConcurrencyError() {
        when(exportSellerConfigMapper.selectByTenant()).thenReturn(null);
        doThrow(new RuntimeException("duplicate key")).when(exportSellerConfigMapper).insert(any(ExportSellerConfigDO.class));

        ExportSellerConfigSaveReqVO req = new ExportSellerConfigSaveReqVO();
        req.setSellerName("LUNA");

        ServiceException ex = assertThrows(ServiceException.class, () -> exportDocService.saveSellerConfig(req));
        assertEquals(EXPORT_DOC_SELLER_CONFIG_SAVE_FAILED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("获得卖方出口配置：不存在时返回空对象不报错")
    void testGetSellerConfig_NotExists_ReturnsEmptyObject() {
        when(exportSellerConfigMapper.selectByTenant()).thenReturn(null);

        ExportSellerConfigRespVO resp = exportDocService.getSellerConfig();
        assertNotNull(resp);
        assertNull(resp.getSellerName());
        assertNull(resp.getId());
    }

    @Test
    @DisplayName("获得卖方出口配置：存在时正常返回各字段")
    void testGetSellerConfig_Exists() {
        ExportSellerConfigDO config = new ExportSellerConfigDO();
        config.setId(88L);
        config.setSellerName("LUNA EXPORT LTD");
        config.setBankSwift("BKCHCNBJ");
        when(exportSellerConfigMapper.selectByTenant()).thenReturn(config);

        ExportSellerConfigRespVO resp = exportDocService.getSellerConfig();
        assertNotNull(resp);
        assertEquals(88L, resp.getId());
        assertEquals("LUNA EXPORT LTD", resp.getSellerName());
        assertEquals("BKCHCNBJ", resp.getBankSwift());
    }

    @Test
    @DisplayName("创建出口单证套：无卖方配置时降级为空字段不报错")
    void testCreateExportDoc_NoSellerConfig_Fallback() {
        CustomerDO customer = createMockCustomer(1L);
        when(customerMapper.selectById(1L)).thenReturn(customer);
        when(exportSellerConfigMapper.selectByTenant()).thenReturn(null);

        ExportDocSaveReqVO req = new ExportDocSaveReqVO();
        req.setCustomerId(1L);

        Long docId = exportDocService.createExportDoc(req);
        assertEquals(100L, docId);

        ArgumentCaptor<ExportDocDO> docCaptor = ArgumentCaptor.forClass(ExportDocDO.class);
        verify(exportDocMapper).insert(docCaptor.capture());
        ExportDocDO savedDoc = docCaptor.getValue();
        assertNull(savedDoc.getSellerName());
        assertNull(savedDoc.getSellerAddress());
        assertNull(savedDoc.getSellerBankInfo());
    }

    @Test
    @DisplayName("编辑出口单证套：此后编辑不回写卖方配置")
    void testUpdateExportDoc_DoesNotOverwriteSellerConfig() {
        ExportDocDO existing = new ExportDocDO();
        existing.setId(10L);
        existing.setStatus("DRAFT");
        existing.setSellerName("CUSTOM SELLER");
        when(exportDocMapper.selectById(10L)).thenReturn(existing);

        ExportSellerConfigDO newTenantConfig = new ExportSellerConfigDO();
        newTenantConfig.setSellerName("TENANT GLOBAL SELLER");
        when(exportSellerConfigMapper.selectByTenant()).thenReturn(newTenantConfig);

        ExportDocSaveReqVO req = new ExportDocSaveReqVO();
        req.setId(10L);
        req.setSellerName("CUSTOM SELLER MODIFIED");

        exportDocService.updateExportDoc(req);

        // 验证 updateById 保存的 sellerName 仍然是 reqVO 中的值，而不是被 tenantConfig 覆写
        ArgumentCaptor<ExportDocDO> captor = ArgumentCaptor.forClass(ExportDocDO.class);
        verify(exportDocMapper).updateById(captor.capture());
        assertEquals("CUSTOM SELLER MODIFIED", captor.getValue().getSellerName());
        // 验证没有调用 selectByTenant
        verify(exportSellerConfigMapper, never()).selectByTenant();
    }

    @Test
    @DisplayName("租户隔离专测：A 租户读不到 B 租户配置")
    void testTenantIsolation_TenantACannotReadTenantBConfig() {
        // 租户 1 有配置
        ExportSellerConfigDO tenant1Config = new ExportSellerConfigDO();
        tenant1Config.setId(1L);
        tenant1Config.setSellerName("TENANT 1 SELLER");
        tenant1Config.setTenantId(1L);

        // 模拟当前上下文切换为租户 2，Mapper 根据租户拦截器只查询租户 2，返回 null
        TenantContextHolder.setTenantId(2L);
        when(exportSellerConfigMapper.selectByTenant()).thenReturn(null);

        ExportSellerConfigRespVO respTenant2 = exportDocService.getSellerConfig();
        assertNotNull(respTenant2);
        assertNull(respTenant2.getSellerName(), "租户 2 读取不到租户 1 的配置，得到空对象");

        // 租户 2 保存自己的配置
        ExportSellerConfigSaveReqVO reqTenant2 = new ExportSellerConfigSaveReqVO();
        reqTenant2.setSellerName("TENANT 2 SELLER");
        exportDocService.saveSellerConfig(reqTenant2);

        ArgumentCaptor<ExportSellerConfigDO> captor = ArgumentCaptor.forClass(ExportSellerConfigDO.class);
        verify(exportSellerConfigMapper).insert(captor.capture());
        assertEquals("TENANT 2 SELLER", captor.getValue().getSellerName());
    }

    @Test
    @DisplayName("定稿出口单证套：DRAFT 状态且有货物明细，成功定稿为 FINALIZED")
    void testFinalizeExportDoc_Success() {
        ExportDocDO draftDoc = new ExportDocDO();
        draftDoc.setId(10L);
        draftDoc.setStatus("DRAFT");
        when(exportDocMapper.selectById(10L)).thenReturn(draftDoc);

        ExportDocItemDO item = new ExportDocItemDO();
        item.setId(101L);
        when(exportDocItemMapper.selectListByDocId(10L)).thenReturn(Collections.singletonList(item));
        when(exportDocMapper.update(isNull(), any())).thenReturn(1);

        assertDoesNotThrow(() -> exportDocService.finalizeExportDoc(10L));
    }

    @Test
    @DisplayName("定稿出口单证套：无货物明细报 EXPORT_DOC_ITEM_EMPTY")
    void testFinalizeExportDoc_EmptyItems_Reject() {
        ExportDocDO draftDoc = new ExportDocDO();
        draftDoc.setId(10L);
        draftDoc.setStatus("DRAFT");
        when(exportDocMapper.selectById(10L)).thenReturn(draftDoc);
        when(exportDocItemMapper.selectListByDocId(10L)).thenReturn(Collections.emptyList());

        ServiceException ex = assertThrows(ServiceException.class, () -> exportDocService.finalizeExportDoc(10L));
        assertEquals(EXPORT_DOC_ITEM_EMPTY.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("定稿出口单证套：非 DRAFT 状态报 EXPORT_DOC_STATUS_NOT_ALLOW")
    void testFinalizeExportDoc_StatusNotDraft_Reject() {
        ExportDocDO finalizedDoc = new ExportDocDO();
        finalizedDoc.setId(10L);
        finalizedDoc.setStatus("FINALIZED");
        when(exportDocMapper.selectById(10L)).thenReturn(finalizedDoc);

        ServiceException ex = assertThrows(ServiceException.class, () -> exportDocService.finalizeExportDoc(10L));
        assertEquals(EXPORT_DOC_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("撤销定稿出口单证套：FINALIZED 状态成功回退为 DRAFT")
    void testRevokeExportDoc_Success() {
        ExportDocDO finalizedDoc = new ExportDocDO();
        finalizedDoc.setId(10L);
        finalizedDoc.setStatus("FINALIZED");
        when(exportDocMapper.selectById(10L)).thenReturn(finalizedDoc);
        when(exportDocMapper.update(isNull(), any())).thenReturn(1);

        assertDoesNotThrow(() -> exportDocService.revokeExportDoc(10L));
    }

    @Test
    @DisplayName("撤销定稿出口单证套：非 FINALIZED 状态报 EXPORT_DOC_STATUS_NOT_ALLOW")
    void testRevokeExportDoc_StatusNotFinalized_Reject() {
        ExportDocDO draftDoc = new ExportDocDO();
        draftDoc.setId(10L);
        draftDoc.setStatus("DRAFT");
        when(exportDocMapper.selectById(10L)).thenReturn(draftDoc);

        ServiceException ex = assertThrows(ServiceException.class, () -> exportDocService.revokeExportDoc(10L));
        assertEquals(EXPORT_DOC_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    // ==================== Issue 05: 从合同/发货单带出 ====================

    @Test
    @DisplayName("从销售合同带出草稿：正常带出4条款、客户英文名址、行快照，且重量一律留空")
    void testGetDraftFromContract_Success() {
        Long contractId = 201L;
        SalesContractDO contract = new SalesContractDO();
        contract.setId(contractId);
        contract.setContractNo("SC20260001");
        contract.setTradeType("EXPORT");
        contract.setCustomerId(1L);
        contract.setClientName("中文客户名");
        contract.setClientAddress("中文地址");
        contract.setIncoterm("FOB");
        contract.setPortOfLoading("SHANGHAI");
        contract.setPortOfDischarge("LOS ANGELES");
        contract.setPaymentTerm("TT");
        contract.setCurrency("USD");
        contract.setUnit("M");
        when(salesContractMapper.selectById(contractId)).thenReturn(contract);

        CustomerDO customer = createMockCustomer(1L);
        when(customerMapper.selectById(1L)).thenReturn(customer);

        SalesContractItemDO ci1 = new SalesContractItemDO();
        ci1.setLineNo(1);
        ci1.setProductName("Silk Satin");
        ci1.setComposition("100% SILK");
        ci1.setWidth("140CM");
        ci1.setQuantity(new BigDecimal("500.00"));
        ci1.setUnitPrice(new BigDecimal("15.50"));
        ci1.setAmount(new BigDecimal("7750.00"));
        ci1.setWeight("16MM"); // GSM / m/m
        when(salesContractItemMapper.selectListByContractId(contractId)).thenReturn(Collections.singletonList(ci1));

        ExportDocRespVO draft = exportDocService.getDraftFromContract(contractId);

        assertNotNull(draft);
        assertEquals("CONTRACT", draft.getSourceType());
        assertEquals(contractId, draft.getContractId());
        assertEquals("SC20260001", draft.getContractNo());
        assertEquals("FOB", draft.getIncoterm());
        assertEquals("SHANGHAI", draft.getPortOfLoading());
        assertEquals("LOS ANGELES", draft.getPortOfDischarge());
        assertEquals("TT", draft.getPaymentTerm());
        assertEquals("USD", draft.getCurrency());
        assertEquals("ACME GLOBAL CO., LTD.", draft.getBuyerName());
        assertEquals("100 West Lake Rd, Hangzhou, China", draft.getBuyerAddress());
        assertNull(draft.getBuyerBankInfo());

        assertEquals(1, draft.getItems().size());
        ExportDocItemRespVO item = draft.getItems().get(0);
        assertEquals("Silk Satin", item.getNameCn());
        assertNull(item.getNameEn());
        assertEquals("100% SILK 140CM", item.getSpec());
        assertEquals(new BigDecimal("500.00"), item.getQuantity());
        assertEquals("M", item.getUnit());
        assertEquals(new BigDecimal("15.50"), item.getUnitPrice());
        assertEquals(new BigDecimal("7750.00"), item.getAmount());
        // 重量/箱数/体积留空
        assertNull(item.getNetWeight());
        assertNull(item.getGrossWeight());
        assertNull(item.getPackages());
        assertNull(item.getVolume());
    }

    @Test
    @DisplayName("从销售合同带出草稿：非 EXPORT 合同拒绝报 EXPORT_DOC_CONTRACT_NOT_EXPORT")
    void testGetDraftFromContract_NotExport_Reject() {
        SalesContractDO contract = new SalesContractDO();
        contract.setId(202L);
        contract.setTradeType("DOMESTIC");
        when(salesContractMapper.selectById(202L)).thenReturn(contract);

        ServiceException ex = assertThrows(ServiceException.class, () -> exportDocService.getDraftFromContract(202L));
        assertEquals(EXPORT_DOC_CONTRACT_NOT_EXPORT.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("从发货单带出草稿：卷清单净重按明细行 SUM 聚合、实发客户名优先、合同条款联动")
    void testGetDraftFromShipment_Success() {
        Long shipmentId = 301L;
        Long contractId = 201L;
        ShipmentDO shipment = new ShipmentDO();
        shipment.setId(shipmentId);
        shipment.setShipmentNo("SH-20261010-001");
        shipment.setCustomerId(1L);
        shipment.setCustomerName("实发特殊客户名");
        shipment.setSalesContractId(contractId);
        shipment.setContractNo("SC20260001");
        shipment.setCurrency("EUR");
        when(shipmentMapper.selectById(shipmentId)).thenReturn(shipment);

        CustomerDO customer = createMockCustomer(1L);
        customer.setNameEn(null); // 无英文名，回退实发客户名
        when(customerMapper.selectById(1L)).thenReturn(customer);

        SalesContractDO contract = new SalesContractDO();
        contract.setId(contractId);
        contract.setIncoterm("CIF");
        contract.setPortOfLoading("NINGBO");
        contract.setPortOfDischarge("HAMBURG");
        contract.setPaymentTerm("LC");
        when(salesContractMapper.selectById(contractId)).thenReturn(contract);

        ShipmentItemDO si1 = new ShipmentItemDO();
        si1.setId(1001L);
        si1.setShipmentId(shipmentId);
        si1.setProductName("Cotton Twill");
        si1.setColorName("Navy Blue");
        si1.setQuantity(new BigDecimal("120.00"));
        si1.setUnit("KG");
        si1.setUnitPrice(new BigDecimal("8.00"));
        si1.setAmount(new BigDecimal("960.00"));
        when(shipmentItemMapper.selectListByShipmentId(shipmentId)).thenReturn(Collections.singletonList(si1));

        ShipmentRollDO r1 = new ShipmentRollDO();
        r1.setShipmentItemId(1001L);
        r1.setWeight(new BigDecimal("60.25"));
        ShipmentRollDO r2 = new ShipmentRollDO();
        r2.setShipmentItemId(1001L);
        r2.setWeight(new BigDecimal("59.50"));
        when(shipmentRollMapper.selectListByShipmentId(shipmentId)).thenReturn(List.of(r1, r2));

        ExportDocRespVO draft = exportDocService.getDraftFromShipment(shipmentId);

        assertNotNull(draft);
        assertEquals("SHIPMENT", draft.getSourceType());
        assertEquals(shipmentId, draft.getShipmentId());
        assertEquals("SH-20261010-001", draft.getShipmentNo());
        assertEquals("实发特殊客户名", draft.getCustomerName());
        assertEquals("实发特殊客户名", draft.getBuyerName());
        assertEquals("CIF", draft.getIncoterm());
        assertEquals("NINGBO", draft.getPortOfLoading());
        assertEquals("HAMBURG", draft.getPortOfDischarge());
        assertEquals("LC", draft.getPaymentTerm());
        assertEquals("EUR", draft.getCurrency());

        assertEquals(1, draft.getItems().size());
        ExportDocItemRespVO item = draft.getItems().get(0);
        assertEquals("Cotton Twill", item.getNameCn());
        assertEquals("Navy Blue", item.getSpec());
        // 净重按卷清单聚合 SUM: 60.25 + 59.50 = 119.75
        assertEquals(new BigDecimal("119.75"), item.getNetWeight());
        assertNull(item.getGrossWeight());
        assertNull(item.getPackages());
        assertNull(item.getVolume());
    }

    // ==================== Issue 06: 重新同步 ====================

    @Test
    @DisplayName("重新同步：MANUAL 来源拒绝报 EXPORT_DOC_SOURCE_REQUIRED")
    void testResyncFromSource_Manual_Reject() {
        ExportDocDO doc = new ExportDocDO();
        doc.setId(10L);
        doc.setStatus("DRAFT");
        doc.setSourceType("MANUAL");
        when(exportDocMapper.selectById(10L)).thenReturn(doc);

        ServiceException ex = assertThrows(ServiceException.class, () -> exportDocService.resyncFromSource(10L));
        assertEquals(EXPORT_DOC_SOURCE_REQUIRED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("重新同步：非 DRAFT 状态拒绝报 EXPORT_DOC_STATUS_NOT_ALLOW")
    void testResyncFromSource_NotDraft_Reject() {
        ExportDocDO doc = new ExportDocDO();
        doc.setId(10L);
        doc.setStatus("FINALIZED");
        doc.setSourceType("CONTRACT");
        when(exportDocMapper.selectById(10L)).thenReturn(doc);

        ServiceException ex = assertThrows(ServiceException.class, () -> exportDocService.resyncFromSource(10L));
        assertEquals(EXPORT_DOC_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("重新同步：抬头/meta/行三层只填空不覆盖已填值，且追加来源新行")
    void testResyncFromSource_FillEmptyOnly() {
        Long docId = 10L;
        Long contractId = 201L;
        ExportDocDO existingDoc = new ExportDocDO();
        existingDoc.setId(docId);
        existingDoc.setStatus("DRAFT");
        existingDoc.setSourceType("CONTRACT");
        existingDoc.setContractId(contractId);
        // 抬头层：已有 buyerName，缺失 incoterm
        existingDoc.setBuyerName("ALREADY CUSTOM BUYER");
        existingDoc.setIncoterm(null);
        // meta 层：已有 piMeta，缺失 ciMeta
        existingDoc.setPiMeta("{\"customPi\":true}");
        existingDoc.setCiMeta(null);
        when(exportDocMapper.selectById(docId)).thenReturn(existingDoc);

        // 合同源数据
        SalesContractDO contract = new SalesContractDO();
        contract.setId(contractId);
        contract.setContractNo("SC20260001");
        contract.setTradeType("EXPORT");
        contract.setCustomerId(1L);
        contract.setIncoterm("FOB");
        contract.setPortOfLoading("SHANGHAI");
        contract.setUnit("M");
        when(salesContractMapper.selectById(contractId)).thenReturn(contract);

        CustomerDO customer = createMockCustomer(1L);
        when(customerMapper.selectById(1L)).thenReturn(customer);

        // 合同源行：共 2 行
        SalesContractItemDO ci1 = new SalesContractItemDO();
        ci1.setLineNo(1);
        ci1.setProductName("Silk Satin");
        ci1.setComposition("100% SILK");
        ci1.setQuantity(new BigDecimal("500.00"));
        ci1.setUnitPrice(new BigDecimal("15.50"));
        ci1.setAmount(new BigDecimal("7750.00"));

        SalesContractItemDO ci2 = new SalesContractItemDO();
        ci2.setLineNo(2);
        ci2.setProductName("Silk Chiffon");
        ci2.setQuantity(new BigDecimal("200.00"));
        ci2.setUnitPrice(new BigDecimal("10.00"));
        ci2.setAmount(new BigDecimal("2000.00"));
        when(salesContractItemMapper.selectListByContractId(contractId)).thenReturn(List.of(ci1, ci2));

        // 单证套现有明细：仅 1 行，且已手填修改过 spec 与 quantity
        ExportDocItemDO dbItem1 = new ExportDocItemDO();
        dbItem1.setId(101L);
        dbItem1.setDocId(docId);
        dbItem1.setLineNo(1);
        dbItem1.setNameCn("Silk Satin");
        dbItem1.setSpec("CUSTOM MODIFIED SPEC");
        dbItem1.setQuantity(new BigDecimal("600.00"));
        dbItem1.setUnit("M");
        dbItem1.setUnitPrice(new BigDecimal("15.50"));
        dbItem1.setAmount(new BigDecimal("9300.00"));
        when(exportDocItemMapper.selectListByDocId(docId)).thenReturn(Collections.singletonList(dbItem1));

        // Act
        exportDocService.resyncFromSource(docId);

        // Assert 抬头层与 meta 层：
        ArgumentCaptor<ExportDocDO> docCaptor = ArgumentCaptor.forClass(ExportDocDO.class);
        verify(exportDocMapper).updateById(docCaptor.capture());
        ExportDocDO updatedDoc = docCaptor.getValue();
        // 已有字段未被覆盖
        assertEquals("ALREADY CUSTOM BUYER", updatedDoc.getBuyerName());
        assertEquals("{\"customPi\":true}", updatedDoc.getPiMeta());
        // 空白字段被补充
        assertEquals("FOB", updatedDoc.getIncoterm());
        assertEquals("SHANGHAI", updatedDoc.getPortOfLoading());

        // Assert 明细行层：
        verify(exportDocItemMapper).deleteByDocId(docId);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ExportDocItemDO>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(exportDocItemMapper).insertBatch(itemsCaptor.capture());
        List<ExportDocItemDO> savedItems = itemsCaptor.getValue();
        assertEquals(2, savedItems.size());
        // 行 1：已手填的值未被覆盖
        ExportDocItemDO saved1 = savedItems.get(0);
        assertEquals("CUSTOM MODIFIED SPEC", saved1.getSpec());
        assertEquals(new BigDecimal("600.00"), saved1.getQuantity());
        // 行 2：来源新增行被追加
        ExportDocItemDO saved2 = savedItems.get(1);
        assertEquals("Silk Chiffon", saved2.getNameCn());
        assertEquals(new BigDecimal("200.00"), saved2.getQuantity());
    }
}
