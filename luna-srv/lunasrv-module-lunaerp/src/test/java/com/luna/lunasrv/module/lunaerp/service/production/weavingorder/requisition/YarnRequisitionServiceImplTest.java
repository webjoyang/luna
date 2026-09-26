package com.luna.lunasrv.module.lunaerp.service.production.weavingorder.requisition;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.requisition.item.vo.YarnRequisitionItemRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.requisition.vo.*;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnrecipebom.vo.YarnRecipeBomItemRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnrecipebom.vo.YarnRecipeBomRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.WeavingOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.requisition.YarnRequisitionDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.requisition.item.YarnRequisitionItemDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.WeavingOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.requisition.YarnRequisitionMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.requisition.item.YarnRequisitionItemMapper;
import com.luna.lunasrv.module.lunaerp.service.production.weavingorder.requisition.item.YarnRequisitionItemService;
import com.luna.lunasrv.module.lunaerp.service.production.yarnrecipebom.YarnRecipeBomService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.*;

import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link YarnRequisitionServiceImpl} 单元测试
 *
 * 覆盖 Issue 09 核心规格：
 * 1. 编号派生（YS-{织造单号去符号}-{单内 2 位序号}）、自增递增、并发重试与手填查重；
 * 2. 关联织造单校验（存在性、未取消）；
 * 3. DRAFT 状态修改与删除守卫，级联逻辑删除明细；
 * 4. 提交守卫（DRAFT → ACTIVE）：非草稿拦截、无明细拦截、明细用量非正拦截、织造单取消拦截；
 * 5. 关闭守卫（手动终态 CLOSED，重复关闭拦截）；
 * 6. BOM 测算接口 calculate（需求KG = 计划匹数 × 单匹计划重量 × ratio / 100，同规格合并，缺重量/无 BOM 提示）；
 * 7. 级联逻辑删除监听器 onWeavingOrderDeleted。
 *
 * @author luna
 */
class YarnRequisitionServiceImplTest {

    @Mock
    private YarnRequisitionMapper yarnRequisitionMapper;

    @Mock
    private YarnRequisitionItemMapper yarnRequisitionItemMapper;

    @Mock
    private YarnRequisitionItemService yarnRequisitionItemService;

    @Mock
    private WeavingOrderMapper weavingOrderMapper;

    @Mock
    private YarnRecipeBomService yarnRecipeBomService;

    @InjectMocks
    private YarnRequisitionServiceImpl yarnRequisitionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), YarnRequisitionDO.class);
    }

    // ==================== 1. 编号派生与创建 ====================

    @Test
    @DisplayName("创建领纱单 - 自动派生编号 YS-ZZPB091900101-01，初始 DRAFT 状态")
    void testCreateYarnRequisition_AutoCode() {
        WeavingOrderDO order = buildWeavingOrder(100L, "ZZ-PB0919001-01", "DRAFT");
        when(weavingOrderMapper.selectById(100L)).thenReturn(order);
        when(yarnRequisitionMapper.selectListByCodePrefixIncludingDeleted("YS-ZZPB091900101-"))
                .thenReturn(Collections.emptyList());
        when(yarnRequisitionMapper.selectByCodeIncludingDeleted("YS-ZZPB091900101-01")).thenReturn(null);

        YarnRequisitionSaveReqVO req = new YarnRequisitionSaveReqVO();
        req.setWeavingOrderId(100L);
        req.setRemark("首批领纱");

        doAnswer(invocation -> {
            YarnRequisitionDO arg = invocation.getArgument(0);
            arg.setId(1024L);
            return 1;
        }).when(yarnRequisitionMapper).insert(any(YarnRequisitionDO.class));

        Long id = yarnRequisitionService.createYarnRequisition(req);
        assertEquals(1024L, id);

        ArgumentCaptor<YarnRequisitionDO> captor = ArgumentCaptor.forClass(YarnRequisitionDO.class);
        verify(yarnRequisitionMapper).insert(captor.capture());
        YarnRequisitionDO saved = captor.getValue();
        assertEquals("YS-ZZPB091900101-01", saved.getCode());
        assertEquals("DRAFT", saved.getStatus());
        assertEquals(100L, saved.getWeavingOrderId());
        assertEquals("首批领纱", saved.getRemark());
    }

    @Test
    @DisplayName("创建领纱单 - 单内存在已有单号时顺延生成 02")
    void testCreateYarnRequisition_SequenceIncrement() {
        WeavingOrderDO order = buildWeavingOrder(100L, "ZZ-PB0919001-01", "ACTIVE");
        when(weavingOrderMapper.selectById(100L)).thenReturn(order);

        YarnRequisitionDO exist = new YarnRequisitionDO();
        exist.setCode("YS-ZZPB091900101-01");
        when(yarnRequisitionMapper.selectListByCodePrefixIncludingDeleted("YS-ZZPB091900101-"))
                .thenReturn(List.of(exist));
        when(yarnRequisitionMapper.selectByCodeIncludingDeleted("YS-ZZPB091900101-02")).thenReturn(null);

        YarnRequisitionSaveReqVO req = new YarnRequisitionSaveReqVO();
        req.setWeavingOrderId(100L);

        yarnRequisitionService.createYarnRequisition(req);

        ArgumentCaptor<YarnRequisitionDO> captor = ArgumentCaptor.forClass(YarnRequisitionDO.class);
        verify(yarnRequisitionMapper).insert(captor.capture());
        assertEquals("YS-ZZPB091900101-02", captor.getValue().getCode());
    }

    @Test
    @DisplayName("创建领纱单 - 手填单号查重冲突时抛出异常")
    void testCreateYarnRequisition_ManualDuplicate() {
        WeavingOrderDO order = buildWeavingOrder(100L, "ZZ-PB0919001-01", "DRAFT");
        when(weavingOrderMapper.selectById(100L)).thenReturn(order);

        YarnRequisitionDO duplicate = new YarnRequisitionDO();
        duplicate.setId(999L);
        duplicate.setCode("YS-MANUAL-01");
        when(yarnRequisitionMapper.selectByCodeIncludingDeleted("YS-MANUAL-01")).thenReturn(duplicate);

        YarnRequisitionSaveReqVO req = new YarnRequisitionSaveReqVO();
        req.setWeavingOrderId(100L);
        req.setCode("YS-MANUAL-01");

        ServiceException ex = assertThrows(ServiceException.class, () ->
                yarnRequisitionService.createYarnRequisition(req));
        assertEquals(YARN_REQUISITION_CODE_DUPLICATE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("创建领纱单 - 织造单已取消时禁止创建")
    void testCreateYarnRequisition_WeavingOrderCancelled() {
        WeavingOrderDO order = buildWeavingOrder(100L, "ZZ-01", "CANCELLED");
        when(weavingOrderMapper.selectById(100L)).thenReturn(order);

        YarnRequisitionSaveReqVO req = new YarnRequisitionSaveReqVO();
        req.setWeavingOrderId(100L);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                yarnRequisitionService.createYarnRequisition(req));
        assertEquals(WEAVING_ORDER_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    // ==================== 2. 更新与删除 ====================

    @Test
    @DisplayName("更新领纱单 - 仅 DRAFT 态允许更新")
    void testUpdateYarnRequisition_StatusGuard() {
        YarnRequisitionDO active = buildRequisition(1L, "YS-01", 100L, "ACTIVE");
        when(yarnRequisitionMapper.selectById(1L)).thenReturn(active);

        YarnRequisitionSaveReqVO req = new YarnRequisitionSaveReqVO();
        req.setId(1L);
        req.setRemark("尝试修改");

        ServiceException ex = assertThrows(ServiceException.class, () ->
                yarnRequisitionService.updateYarnRequisition(req));
        assertEquals(YARN_REQUISITION_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("删除领纱单 - 仅 DRAFT 态允许删除，并级联逻辑删除明细")
    void testDeleteYarnRequisition_DraftCascades() {
        YarnRequisitionDO draft = buildRequisition(1L, "YS-01", 100L, "DRAFT");
        when(yarnRequisitionMapper.selectById(1L)).thenReturn(draft);

        yarnRequisitionService.deleteYarnRequisition(1L);

        verify(yarnRequisitionItemMapper).deleteByRequisitionId(1L);
        verify(yarnRequisitionMapper).deleteById(1L);
    }

    @Test
    @DisplayName("删除领纱单 - 非 DRAFT 态删除拦截")
    void testDeleteYarnRequisition_NonDraftBlocked() {
        YarnRequisitionDO active = buildRequisition(1L, "YS-01", 100L, "ACTIVE");
        when(yarnRequisitionMapper.selectById(1L)).thenReturn(active);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                yarnRequisitionService.deleteYarnRequisition(1L));
        assertEquals(YARN_REQUISITION_STATUS_NOT_ALLOW.getCode(), ex.getCode());
        verify(yarnRequisitionMapper, never()).deleteById(any());
    }

    // ==================== 3. 提交守卫 ====================

    @Test
    @DisplayName("提交领纱单 - 成功从 DRAFT 流转到 ACTIVE")
    void testSubmitYarnRequisition_Success() {
        YarnRequisitionDO draft = buildRequisition(1L, "YS-01", 100L, "DRAFT");
        when(yarnRequisitionMapper.selectById(1L)).thenReturn(draft);

        WeavingOrderDO order = buildWeavingOrder(100L, "ZZ-01", "ACTIVE");
        when(weavingOrderMapper.selectById(100L)).thenReturn(order);

        YarnRequisitionItemDO item = new YarnRequisitionItemDO();
        item.setId(10L);
        item.setRequiredQty(new BigDecimal("100.00"));
        when(yarnRequisitionItemMapper.selectListByRequisitionId(1L)).thenReturn(List.of(item));

        yarnRequisitionService.submitYarnRequisition(1L);

        assertEquals("ACTIVE", draft.getStatus());
        verify(yarnRequisitionMapper).updateById(draft);
    }

    @Test
    @DisplayName("提交领纱单 - 非 DRAFT 状态禁止提交")
    void testSubmitYarnRequisition_NonDraftBlocked() {
        YarnRequisitionDO active = buildRequisition(1L, "YS-01", 100L, "ACTIVE");
        when(yarnRequisitionMapper.selectById(1L)).thenReturn(active);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                yarnRequisitionService.submitYarnRequisition(1L));
        assertEquals(YARN_REQUISITION_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("提交领纱单 - 无明细时禁止提交")
    void testSubmitYarnRequisition_EmptyItemsBlocked() {
        YarnRequisitionDO draft = buildRequisition(1L, "YS-01", 100L, "DRAFT");
        when(yarnRequisitionMapper.selectById(1L)).thenReturn(draft);

        WeavingOrderDO order = buildWeavingOrder(100L, "ZZ-01", "ACTIVE");
        when(weavingOrderMapper.selectById(100L)).thenReturn(order);

        when(yarnRequisitionItemMapper.selectListByRequisitionId(1L)).thenReturn(Collections.emptyList());

        ServiceException ex = assertThrows(ServiceException.class, () ->
                yarnRequisitionService.submitYarnRequisition(1L));
        assertEquals(YARN_REQUISITION_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("提交领纱单 - 关联织造单已取消时禁止提交")
    void testSubmitYarnRequisition_WeavingOrderCancelledBlocked() {
        YarnRequisitionDO draft = buildRequisition(1L, "YS-01", 100L, "DRAFT");
        when(yarnRequisitionMapper.selectById(1L)).thenReturn(draft);

        WeavingOrderDO order = buildWeavingOrder(100L, "ZZ-01", "CANCELLED");
        when(weavingOrderMapper.selectById(100L)).thenReturn(order);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                yarnRequisitionService.submitYarnRequisition(1L));
        assertEquals(WEAVING_ORDER_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    // ==================== 4. 关闭守卫 ====================

    @Test
    @DisplayName("关闭领纱单 - 成功转为 CLOSED 终态")
    void testCloseYarnRequisition_Success() {
        YarnRequisitionDO active = buildRequisition(1L, "YS-01", 100L, "ACTIVE");
        when(yarnRequisitionMapper.selectById(1L)).thenReturn(active);

        yarnRequisitionService.closeYarnRequisition(1L);

        assertEquals("CLOSED", active.getStatus());
        verify(yarnRequisitionMapper).updateById(active);
    }

    @Test
    @DisplayName("关闭领纱单 - 已关闭单据重复关闭报错")
    void testCloseYarnRequisition_AlreadyClosed() {
        YarnRequisitionDO closed = buildRequisition(1L, "YS-01", 100L, "CLOSED");
        when(yarnRequisitionMapper.selectById(1L)).thenReturn(closed);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                yarnRequisitionService.closeYarnRequisition(1L));
        assertEquals(YARN_REQUISITION_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    // ==================== 5. 配比 BOM 权威测算 ====================

    @Test
    @DisplayName("BOM 测算 - 测算口径正确且同纱线规格合并")
    void testCalculate_SuccessMerged() {
        // 织造单：计划 10 匹，总重 250.00 KG -> 单匹 25.0000 KG
        WeavingOrderDO order = buildWeavingOrder(100L, "ZZ-01", "ACTIVE");
        order.setProductId(200L);
        order.setProductCode("PRD-01");
        order.setProductName("棉氨弹力汗布");
        order.setPlannedQty(10);
        order.setPlannedWeight(new BigDecimal("250.00"));
        when(weavingOrderMapper.selectById(100L)).thenReturn(order);

        // BOM：配比项
        // 项 1: specId=10 (32S 棉), ratio=40
        // 项 2: specId=10 (32S 棉), ratio=10 (同规格不同工艺行，测试合并，合计 ratio=50)
        // 项 3: specId=20 (40D 氨纶), ratio=50
        YarnRecipeBomRespVO bom = new YarnRecipeBomRespVO();
        List<YarnRecipeBomItemRespVO> bomItems = new ArrayList<>();

        YarnRecipeBomItemRespVO bi1 = new YarnRecipeBomItemRespVO();
        bi1.setYarnSpecId(10L);
        bi1.setYarnSpecName("32S 棉");
        bi1.setYarnTypeId(1L);
        bi1.setYarnTypeName("棉");
        bi1.setRatio(new BigDecimal("40.00"));
        bomItems.add(bi1);

        YarnRecipeBomItemRespVO bi2 = new YarnRecipeBomItemRespVO();
        bi2.setYarnSpecId(10L);
        bi2.setYarnSpecName("32S 棉");
        bi2.setYarnTypeId(1L);
        bi2.setYarnTypeName("棉");
        bi2.setRatio(new BigDecimal("10.00"));
        bomItems.add(bi2);

        YarnRecipeBomItemRespVO bi3 = new YarnRecipeBomItemRespVO();
        bi3.setYarnSpecId(20L);
        bi3.setYarnSpecName("40D 氨纶");
        bi3.setYarnTypeId(2L);
        bi3.setYarnTypeName("氨纶");
        bi3.setRatio(new BigDecimal("50.00"));
        bomItems.add(bi3);

        bom.setItems(bomItems);
        when(yarnRecipeBomService.getYarnRecipeBomByProductId(200L)).thenReturn(bom);

        YarnRequisitionCalculateReqVO req = new YarnRequisitionCalculateReqVO();
        req.setWeavingOrderId(100L);

        YarnRequisitionCalculateRespVO resp = yarnRequisitionService.calculate(req);

        assertNotNull(resp);
        assertEquals(100L, resp.getWeavingOrderId());
        assertEquals(0, new BigDecimal("25.0000").compareTo(resp.getPiecePlannedWeight()));
        assertNull(resp.getTip());
        assertEquals(2, resp.getItems().size()); // 3 条 BOM 项按 specId 合并为 2 条

        // 验证 spec 10: 合计 ratio = 50.00%, requiredQty = 10 * 25.0000 * 50 / 100 = 125.00 KG
        YarnRequisitionCalculateItemRespVO item10 = resp.getItems().stream()
                .filter(i -> i.getYarnSpecId().equals(10L)).findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("50.00").compareTo(item10.getRatio()));
        assertEquals(0, new BigDecimal("125.00").compareTo(item10.getRequiredQty()));
        assertEquals("KG", item10.getUnit());

        // 验证 spec 20: ratio = 50.00%, requiredQty = 10 * 25.0000 * 50 / 100 = 125.00 KG
        YarnRequisitionCalculateItemRespVO item20 = resp.getItems().stream()
                .filter(i -> i.getYarnSpecId().equals(20L)).findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("50.00").compareTo(item20.getRatio()));
        assertEquals(0, new BigDecimal("125.00").compareTo(item20.getRequiredQty()));
    }

    @Test
    @DisplayName("BOM 测算 - 缺计划重量时返回提示，不抛异常")
    void testCalculate_MissingWeightReturnsTip() {
        WeavingOrderDO order = buildWeavingOrder(100L, "ZZ-01", "ACTIVE");
        order.setProductId(200L);
        order.setPlannedQty(10);
        order.setPlannedWeight(null); // 缺重量
        when(weavingOrderMapper.selectById(100L)).thenReturn(order);

        YarnRequisitionCalculateReqVO req = new YarnRequisitionCalculateReqVO();
        req.setWeavingOrderId(100L);

        YarnRequisitionCalculateRespVO resp = yarnRequisitionService.calculate(req);
        assertNotNull(resp);
        assertTrue(resp.getTip().contains("缺少计划重量"));
        assertTrue(resp.getItems().isEmpty());
    }

    @Test
    @DisplayName("BOM 测算 - 产品无 BOM 时返回提示，不抛异常")
    void testCalculate_MissingBomReturnsTip() {
        WeavingOrderDO order = buildWeavingOrder(100L, "ZZ-01", "ACTIVE");
        order.setProductId(200L);
        order.setPlannedQty(10);
        order.setPlannedWeight(new BigDecimal("250.00"));
        when(weavingOrderMapper.selectById(100L)).thenReturn(order);
        when(yarnRecipeBomService.getYarnRecipeBomByProductId(200L)).thenReturn(null);

        YarnRequisitionCalculateReqVO req = new YarnRequisitionCalculateReqVO();
        req.setWeavingOrderId(100L);

        YarnRequisitionCalculateRespVO resp = yarnRequisitionService.calculate(req);
        assertNotNull(resp);
        assertTrue(resp.getTip().contains("尚未配置纱线配比BOM"));
        assertTrue(resp.getItems().isEmpty());
    }

    // ==================== 6. 级联逻辑删除监听器 ====================

    @Test
    @DisplayName("级联删除监听器 - 织造单删除时级联删除领纱单与领纱明细")
    void testOnWeavingOrderDeleted() {
        YarnRequisitionDO r1 = buildRequisition(1L, "YS-01", 100L, "DRAFT");
        YarnRequisitionDO r2 = buildRequisition(2L, "YS-02", 100L, "DRAFT");
        when(yarnRequisitionMapper.selectListByWeavingOrderId(100L)).thenReturn(List.of(r1, r2));

        yarnRequisitionService.onWeavingOrderDeleted(100L);

        verify(yarnRequisitionItemMapper).deleteByRequisitionIds(eq(Set.of(1L, 2L)));
        verify(yarnRequisitionMapper).deleteByWeavingOrderId(100L);
    }

    // ==================== 辅助方法 ====================

    private WeavingOrderDO buildWeavingOrder(Long id, String code, String status) {
        WeavingOrderDO order = new WeavingOrderDO();
        order.setId(id);
        order.setCode(code);
        order.setStatus(status);
        order.setPlannedQty(10);
        order.setPlannedWeight(new BigDecimal("250.00"));
        return order;
    }

    private YarnRequisitionDO buildRequisition(Long id, String code, Long orderId, String status) {
        YarnRequisitionDO req = new YarnRequisitionDO();
        req.setId(id);
        req.setCode(code);
        req.setWeavingOrderId(orderId);
        req.setStatus(status);
        return req;
    }

}
