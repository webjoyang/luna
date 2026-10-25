package com.luna.lunasrv.module.lunaerp.service.production.progressfeedback;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo.ProgressFeedbackCalendarReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo.ProgressFeedbackCalendarRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.dyeingorder.DyeingOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.dyeingorder.color.DyeingColorDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.progressfeedback.ProgressFeedbackDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.progressfeedback.note.ProgressFeedbackNoteDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.dyeingorder.DyeingOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.dyeingorder.color.DyeingColorMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.progressfeedback.ProgressFeedbackMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.progressfeedback.note.ProgressFeedbackNoteMapper;
import com.luna.lunasrv.module.lunaerp.enums.production.progressfeedback.ProgressFeedbackStatusEnum;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

/**
 * {@link ProgressFeedbackServiceImpl#getProgressFeedbackCalendar} 纯 Mockito 单元测试。
 *
 * 覆盖 Issue 06 场景：
 * 1. month 区间只返回排缸日落月内的缸；status/dyeingFactoryId 筛选生效
 * 2. COMPLETED 缸默认返回可回看且可被筛选；CANCELLED 染色单的缸不返回
 * 3. 快照字段 join 正确（客户色号/色名/合同号/染厂名）
 * 4. 各种日期校验异常场景
 *
 * @author luna
 */
class ProgressFeedbackCalendarTest {

    @Mock
    private ProgressFeedbackMapper progressFeedbackMapper;

    @Mock
    private ProgressFeedbackNoteMapper progressFeedbackNoteMapper;

    @Mock
    private DyeingOrderMapper dyeingOrderMapper;

    @Mock
    private DyeingColorMapper dyeingColorMapper;

    @InjectMocks
    private ProgressFeedbackServiceImpl progressFeedbackService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ProgressFeedbackDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ProgressFeedbackNoteDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), DyeingOrderDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), DyeingColorDO.class);
    }

    private DyeingOrderDO createOrder(Long id, String status, Boolean deleted, Long factoryId, String factoryName, String contractNo) {
        DyeingOrderDO order = new DyeingOrderDO();
        order.setId(id);
        order.setStatus(status);
        order.setDeleted(deleted);
        order.setDyeingFactoryId(factoryId);
        order.setDyeingFactoryName(factoryName);
        order.setContractNo(contractNo);
        return order;
    }

    private DyeingColorDO createColor(Long id, Long orderId, String customerColorNo, String colorName, String unit, Boolean deleted) {
        DyeingColorDO color = new DyeingColorDO();
        color.setId(id);
        color.setDyeingOrderId(orderId);
        color.setCustomerColorNo(customerColorNo);
        color.setColorName(colorName);
        color.setDyeingInputUnit(unit);
        color.setDeleted(deleted);
        return color;
    }

    private ProgressFeedbackDO createVat(Long id, Long orderId, Long colorId, String vatNo,
                                         LocalDate scheduledDate, String status, BigDecimal qty, Integer pieces) {
        ProgressFeedbackDO vat = new ProgressFeedbackDO();
        vat.setId(id);
        vat.setDyeingOrderId(orderId);
        vat.setDyeingColorId(colorId);
        vat.setVatNo(vatNo);
        vat.setScheduledDate(scheduledDate);
        vat.setStatus(status);
        vat.setQuantity(qty);
        vat.setPieces(pieces);
        vat.setDeleted(false);
        return vat;
    }

    @Test
    @DisplayName("month 入参解析正确：落月内的缸返回，超出月区间的缸被过滤")
    void testMonthParam_OnlyReturnsVatsWithinMonth() {
        ProgressFeedbackCalendarReqVO reqVO = ProgressFeedbackCalendarReqVO.builder()
                .month("2026-10")
                .build();

        DyeingOrderDO order = createOrder(1L, "ACTIVE", false, 5L, "永盛染厂", "CH2026-001");
        DyeingColorDO color = createColor(10L, 1L, "C101", "藏青", "KG", false);

        ProgressFeedbackDO vatInMonth1 = createVat(101L, 1L, 10L, "A-12", LocalDate.of(2026, 10, 9), "DYEING", new BigDecimal("500.00"), 20);
        ProgressFeedbackDO vatInMonth2 = createVat(102L, 1L, 10L, "A-13", LocalDate.of(2026, 10, 31), "SCHEDULED", new BigDecimal("300.00"), 12);
        ProgressFeedbackDO vatOutMonth = createVat(103L, 1L, 10L, "A-14", LocalDate.of(2026, 9, 30), "SCHEDULED", new BigDecimal("200.00"), 8);

        when(progressFeedbackMapper.selectListByScheduledDateRange(
                eq(LocalDate.of(2026, 10, 1)),
                eq(LocalDate.of(2026, 10, 31)),
                nullable(String.class)
        )).thenReturn(List.of(vatInMonth1, vatInMonth2, vatOutMonth));

        when(dyeingOrderMapper.selectBatchIds(any())).thenReturn(List.of(order));
        when(dyeingColorMapper.selectBatchIds(any())).thenReturn(List.of(color));

        ProgressFeedbackCalendarRespVO result = progressFeedbackService.getProgressFeedbackCalendar(reqVO);

        assertNotNull(result);
        assertNotNull(result.getVats());
        assertEquals(2, result.getVats().size());
        assertEquals("A-12", result.getVats().get(0).getVatNo());
        assertEquals("A-13", result.getVats().get(1).getVatNo());
    }

    @Test
    @DisplayName("from/to 入参解析正确：落在区间内的缸返回")
    void testFromToParam_Success() {
        ProgressFeedbackCalendarReqVO reqVO = ProgressFeedbackCalendarReqVO.builder()
                .from(LocalDate.of(2026, 10, 5))
                .to(LocalDate.of(2026, 10, 15))
                .build();

        DyeingOrderDO order = createOrder(1L, "ACTIVE", false, 5L, "永盛染厂", "CH2026-001");
        DyeingColorDO color = createColor(10L, 1L, "C101", "藏青", "KG", false);

        ProgressFeedbackDO vat = createVat(101L, 1L, 10L, "A-12", LocalDate.of(2026, 10, 9), "DYEING", new BigDecimal("500.00"), 20);

        when(progressFeedbackMapper.selectListByScheduledDateRange(
                eq(LocalDate.of(2026, 10, 5)),
                eq(LocalDate.of(2026, 10, 15)),
                nullable(String.class)
        )).thenReturn(List.of(vat));

        when(dyeingOrderMapper.selectBatchIds(any())).thenReturn(List.of(order));
        when(dyeingColorMapper.selectBatchIds(any())).thenReturn(List.of(color));

        ProgressFeedbackCalendarRespVO result = progressFeedbackService.getProgressFeedbackCalendar(reqVO);

        assertNotNull(result);
        assertEquals(1, result.getVats().size());
        assertEquals(101L, result.getVats().get(0).getFeedbackId());
    }

    @Test
    @DisplayName("参数校验：未提供 month 且未提供完整 from/to 时抛出异常")
    void testValidation_MissingRangeParams_ThrowsException() {
        ProgressFeedbackCalendarReqVO reqVO = new ProgressFeedbackCalendarReqVO();
        assertThrows(ServiceException.class, () -> progressFeedbackService.getProgressFeedbackCalendar(reqVO));

        ProgressFeedbackCalendarReqVO reqOnlyFrom = ProgressFeedbackCalendarReqVO.builder()
                .from(LocalDate.of(2026, 10, 1))
                .build();
        assertThrows(ServiceException.class, () -> progressFeedbackService.getProgressFeedbackCalendar(reqOnlyFrom));
    }

    @Test
    @DisplayName("参数校验：month 格式非法时抛出异常")
    void testValidation_InvalidMonthFormat_ThrowsException() {
        ProgressFeedbackCalendarReqVO reqVO = ProgressFeedbackCalendarReqVO.builder()
                .month("2026-13")
                .build();
        assertThrows(ServiceException.class, () -> progressFeedbackService.getProgressFeedbackCalendar(reqVO));
    }

    @Test
    @DisplayName("参数校验：from 晚于 to 时抛出异常")
    void testValidation_StartAfterEnd_ThrowsException() {
        ProgressFeedbackCalendarReqVO reqVO = ProgressFeedbackCalendarReqVO.builder()
                .from(LocalDate.of(2026, 10, 20))
                .to(LocalDate.of(2026, 10, 10))
                .build();
        assertThrows(ServiceException.class, () -> progressFeedbackService.getProgressFeedbackCalendar(reqVO));
    }

    @Test
    @DisplayName("status 筛选生效：指定 status 时只返回匹配状态的缸")
    void testStatusFilter_OnlyMatchingStatusReturned() {
        ProgressFeedbackCalendarReqVO reqVO = ProgressFeedbackCalendarReqVO.builder()
                .month("2026-10")
                .status("DYEING")
                .build();

        DyeingOrderDO order = createOrder(1L, "ACTIVE", false, 5L, "永盛染厂", "CH2026-001");
        DyeingColorDO color = createColor(10L, 1L, "C101", "藏青", "KG", false);

        ProgressFeedbackDO vatDyeing = createVat(101L, 1L, 10L, "A-12", LocalDate.of(2026, 10, 9), "DYEING", new BigDecimal("500.00"), 20);
        ProgressFeedbackDO vatScheduled = createVat(102L, 1L, 10L, "A-13", LocalDate.of(2026, 10, 10), "SCHEDULED", new BigDecimal("300.00"), 12);

        when(progressFeedbackMapper.selectListByScheduledDateRange(
                eq(LocalDate.of(2026, 10, 1)),
                eq(LocalDate.of(2026, 10, 31)),
                eq("DYEING")
        )).thenReturn(List.of(vatDyeing, vatScheduled)); // 模拟包含多态，验证 Service 内存防御过滤

        when(dyeingOrderMapper.selectBatchIds(any())).thenReturn(List.of(order));
        when(dyeingColorMapper.selectBatchIds(any())).thenReturn(List.of(color));

        ProgressFeedbackCalendarRespVO result = progressFeedbackService.getProgressFeedbackCalendar(reqVO);

        assertNotNull(result);
        assertEquals(1, result.getVats().size());
        assertEquals("DYEING", result.getVats().get(0).getStatus());
        assertEquals("A-12", result.getVats().get(0).getVatNo());
    }

    @Test
    @DisplayName("COMPLETED 缸默认返回用于回看，且可被显式筛选")
    void testCompletedVats_DefaultIncludedAndFilterable() {
        DyeingOrderDO order = createOrder(1L, "ACTIVE", false, 5L, "永盛染厂", "CH2026-001");
        DyeingColorDO color = createColor(10L, 1L, "C101", "藏青", "KG", false);

        ProgressFeedbackDO vatCompleted = createVat(101L, 1L, 10L, "A-12", LocalDate.of(2026, 10, 9),
                ProgressFeedbackStatusEnum.COMPLETED.getValue(), new BigDecimal("500.00"), 20);

        when(dyeingOrderMapper.selectBatchIds(any())).thenReturn(List.of(order));
        when(dyeingColorMapper.selectBatchIds(any())).thenReturn(List.of(color));

        // 场景 A：不传 status，默认包含 COMPLETED 缸回看
        when(progressFeedbackMapper.selectListByScheduledDateRange(any(), any(), nullable(String.class)))
                .thenReturn(List.of(vatCompleted));
        ProgressFeedbackCalendarReqVO reqAll = ProgressFeedbackCalendarReqVO.builder().month("2026-10").build();
        ProgressFeedbackCalendarRespVO resultAll = progressFeedbackService.getProgressFeedbackCalendar(reqAll);
        assertEquals(1, resultAll.getVats().size());
        assertEquals("COMPLETED", resultAll.getVats().get(0).getStatus());

        // 场景 B：显式筛选 COMPLETED
        when(progressFeedbackMapper.selectListByScheduledDateRange(any(), any(), eq("COMPLETED")))
                .thenReturn(List.of(vatCompleted));
        ProgressFeedbackCalendarReqVO reqCompleted = ProgressFeedbackCalendarReqVO.builder()
                .month("2026-10")
                .status("COMPLETED")
                .build();
        ProgressFeedbackCalendarRespVO resultCompleted = progressFeedbackService.getProgressFeedbackCalendar(reqCompleted);
        assertEquals(1, resultCompleted.getVats().size());
        assertEquals("COMPLETED", resultCompleted.getVats().get(0).getStatus());
    }

    @Test
    @DisplayName("dyeingFactoryId 筛选生效：只返回指定染厂的缸")
    void testDyeingFactoryIdFilter_OnlyReturnsMatchingFactory() {
        ProgressFeedbackCalendarReqVO reqVO = ProgressFeedbackCalendarReqVO.builder()
                .month("2026-10")
                .dyeingFactoryId(5L)
                .build();

        DyeingOrderDO orderFactory5 = createOrder(1L, "ACTIVE", false, 5L, "永盛染厂", "CH2026-001");
        DyeingOrderDO orderFactory8 = createOrder(2L, "ACTIVE", false, 8L, "另一染厂", "CH2026-002");
        DyeingColorDO color1 = createColor(10L, 1L, "C101", "藏青", "KG", false);
        DyeingColorDO color2 = createColor(20L, 2L, "C102", "米白", "KG", false);

        ProgressFeedbackDO vatFactory5 = createVat(101L, 1L, 10L, "A-12", LocalDate.of(2026, 10, 9), "DYEING", new BigDecimal("500.00"), 20);
        ProgressFeedbackDO vatFactory8 = createVat(102L, 2L, 20L, "B-01", LocalDate.of(2026, 10, 9), "DYEING", new BigDecimal("300.00"), 10);

        when(progressFeedbackMapper.selectListByScheduledDateRange(any(), any(), nullable(String.class)))
                .thenReturn(List.of(vatFactory5, vatFactory8));
        when(dyeingOrderMapper.selectBatchIds(any())).thenReturn(List.of(orderFactory5, orderFactory8));
        when(dyeingColorMapper.selectBatchIds(any())).thenReturn(List.of(color1, color2));

        ProgressFeedbackCalendarRespVO result = progressFeedbackService.getProgressFeedbackCalendar(reqVO);

        assertNotNull(result);
        assertEquals(1, result.getVats().size());
        assertEquals(5L, result.getVats().get(0).getDyeingFactoryId());
        assertEquals("永盛染厂", result.getVats().get(0).getDyeingFactoryName());
        assertEquals("A-12", result.getVats().get(0).getVatNo());
    }

    @Test
    @DisplayName("染色单状态口径：仅 ACTIVE/COMPLETED 纳入，CANCELLED/DRAFT/软删染色单的缸均被排除")
    void testDyeingOrderStatus_ExcludesCancelledAndDraftAndSoftDeleted() {
        ProgressFeedbackCalendarReqVO reqVO = ProgressFeedbackCalendarReqVO.builder()
                .month("2026-10")
                .build();

        DyeingOrderDO orderActive = createOrder(1L, "ACTIVE", false, 5L, "永盛染厂", "CH-01");
        DyeingOrderDO orderCompleted = createOrder(2L, "COMPLETED", false, 5L, "永盛染厂", "CH-02");
        DyeingOrderDO orderCancelled = createOrder(3L, "CANCELLED", false, 5L, "永盛染厂", "CH-03");
        DyeingOrderDO orderDraft = createOrder(4L, "DRAFT", false, 5L, "永盛染厂", "CH-04");
        DyeingOrderDO orderDeleted = createOrder(5L, "ACTIVE", true, 5L, "永盛染厂", "CH-05");

        DyeingColorDO color1 = createColor(11L, 1L, "C01", "黑", "KG", false);
        DyeingColorDO color2 = createColor(12L, 2L, "C02", "白", "KG", false);
        DyeingColorDO color3 = createColor(13L, 3L, "C03", "灰", "KG", false);
        DyeingColorDO color4 = createColor(14L, 4L, "C04", "蓝", "KG", false);
        DyeingColorDO color5 = createColor(15L, 5L, "C05", "红", "KG", false);

        ProgressFeedbackDO vatActive = createVat(101L, 1L, 11L, "V-01", LocalDate.of(2026, 10, 1), "SCHEDULED", new BigDecimal("100"), 5);
        ProgressFeedbackDO vatCompleted = createVat(102L, 2L, 12L, "V-02", LocalDate.of(2026, 10, 2), "SCHEDULED", new BigDecimal("100"), 5);
        ProgressFeedbackDO vatCancelled = createVat(103L, 3L, 13L, "V-03", LocalDate.of(2026, 10, 3), "SCHEDULED", new BigDecimal("100"), 5);
        ProgressFeedbackDO vatDraft = createVat(104L, 4L, 14L, "V-04", LocalDate.of(2026, 10, 4), "SCHEDULED", new BigDecimal("100"), 5);
        ProgressFeedbackDO vatDeleted = createVat(105L, 5L, 15L, "V-05", LocalDate.of(2026, 10, 5), "SCHEDULED", new BigDecimal("100"), 5);

        when(progressFeedbackMapper.selectListByScheduledDateRange(any(), any(), nullable(String.class)))
                .thenReturn(List.of(vatActive, vatCompleted, vatCancelled, vatDraft, vatDeleted));
        when(dyeingOrderMapper.selectBatchIds(any()))
                .thenReturn(List.of(orderActive, orderCompleted, orderCancelled, orderDraft, orderDeleted));
        when(dyeingColorMapper.selectBatchIds(any()))
                .thenReturn(List.of(color1, color2, color3, color4, color5));

        ProgressFeedbackCalendarRespVO result = progressFeedbackService.getProgressFeedbackCalendar(reqVO);

        assertNotNull(result);
        assertEquals(2, result.getVats().size());
        assertEquals("V-01", result.getVats().get(0).getVatNo());
        assertEquals("V-02", result.getVats().get(1).getVatNo());
    }

    @Test
    @DisplayName("颜色明细软删防御：软删颜色行下的缸被排除")
    void testSoftDeletedColor_Excluded() {
        ProgressFeedbackCalendarReqVO reqVO = ProgressFeedbackCalendarReqVO.builder().month("2026-10").build();

        DyeingOrderDO order = createOrder(1L, "ACTIVE", false, 5L, "永盛染厂", "CH-01");
        DyeingColorDO deletedColor = createColor(10L, 1L, "C01", "黑", "KG", true);

        ProgressFeedbackDO vat = createVat(101L, 1L, 10L, "V-01", LocalDate.of(2026, 10, 1), "SCHEDULED", new BigDecimal("100"), 5);

        when(progressFeedbackMapper.selectListByScheduledDateRange(any(), any(), nullable(String.class)))
                .thenReturn(List.of(vat));
        when(dyeingOrderMapper.selectBatchIds(any())).thenReturn(List.of(order));
        when(dyeingColorMapper.selectBatchIds(any())).thenReturn(List.of(deletedColor));

        ProgressFeedbackCalendarRespVO result = progressFeedbackService.getProgressFeedbackCalendar(reqVO);

        assertNotNull(result);
        assertTrue(result.getVats().isEmpty());
    }

    @Test
    @DisplayName("展示快照字段 join 正确：客户色号/色名/单位/合同号/染厂ID/染厂名实时透出")
    void testSnapshotFields_JoinedCorrectly() {
        ProgressFeedbackCalendarReqVO reqVO = ProgressFeedbackCalendarReqVO.builder()
                .month("2026-10")
                .build();

        DyeingOrderDO order = createOrder(9L, "ACTIVE", false, 5L, "永盛染厂", "CH2026-001");
        DyeingColorDO color = createColor(21L, 9L, "C101", "藏青", "KG", false);

        ProgressFeedbackDO vat = createVat(3L, 9L, 21L, "A-12",
                LocalDate.of(2026, 10, 9), "DYEING", new BigDecimal("500.00"), 20);

        when(progressFeedbackMapper.selectListByScheduledDateRange(any(), any(), nullable(String.class)))
                .thenReturn(List.of(vat));
        when(dyeingOrderMapper.selectBatchIds(any())).thenReturn(List.of(order));
        when(dyeingColorMapper.selectBatchIds(any())).thenReturn(List.of(color));

        ProgressFeedbackCalendarRespVO result = progressFeedbackService.getProgressFeedbackCalendar(reqVO);

        assertNotNull(result);
        assertEquals(1, result.getVats().size());
        ProgressFeedbackCalendarRespVO.VatVO item = result.getVats().get(0);

        assertEquals(3L, item.getFeedbackId());
        assertEquals(9L, item.getDyeingOrderId());
        assertEquals(21L, item.getDyeingColorId());
        assertEquals("A-12", item.getVatNo());
        assertEquals(LocalDate.of(2026, 10, 9), item.getScheduledDate());
        assertEquals("DYEING", item.getStatus());
        assertEquals(new BigDecimal("500.00"), item.getQuantity());
        assertEquals(20, item.getPieces());
        assertEquals("KG", item.getUnit());
        assertEquals("C101", item.getCustomerColorNo());
        assertEquals("藏青", item.getColorName());
        assertEquals("CH2026-001", item.getContractNo());
        assertEquals(5L, item.getDyeingFactoryId());
        assertEquals("永盛染厂", item.getDyeingFactoryName());
    }

    @Test
    @DisplayName("空缸数据防御：区间内无缸返回空列表")
    void testEmptyVats_ReturnsEmptyList() {
        ProgressFeedbackCalendarReqVO reqVO = ProgressFeedbackCalendarReqVO.builder().month("2026-10").build();

        when(progressFeedbackMapper.selectListByScheduledDateRange(any(), any(), nullable(String.class)))
                .thenReturn(Collections.emptyList());

        ProgressFeedbackCalendarRespVO result = progressFeedbackService.getProgressFeedbackCalendar(reqVO);

        assertNotNull(result);
        assertNotNull(result.getVats());
        assertTrue(result.getVats().isEmpty());
    }

}
