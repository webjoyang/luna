package com.luna.lunasrv.module.lunaerp.service.production.progressfeedback;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo.ProgressFeedbackCalendarReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo.ProgressFeedbackCalendarRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo.ProgressFeedbackCalendarVatVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.dyeingorder.DyeingOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.dyeingorder.color.DyeingColorDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.progressfeedback.ProgressFeedbackDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.progressfeedback.note.ProgressFeedbackNoteDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.dyeingorder.DyeingOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.dyeingorder.color.DyeingColorMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.progressfeedback.ProgressFeedbackMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.progressfeedback.note.ProgressFeedbackNoteMapper;
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
import static org.mockito.Mockito.when;

/**
 * {@link ProgressFeedbackServiceImpl#getCalendar(ProgressFeedbackCalendarReqVO)} 纯 Mockito 单元测试。
 *
 * 覆盖 Testing Decisions 16 & Issue 06 要求：
 * 1. month 区间只返回 scheduled_date 落在月内的缸；from/to 区间筛选生效；
 * 2. status 筛选与 dyeingFactoryId 承染厂筛选生效；
 * 3. COMPLETED 缸默认返回（用于历史回看）且可被 status 过滤；
 * 4. 染色单状态仅 ACTIVE 与 COMPLETED 纳入，CANCELLED/DRAFT 与软删单的缸不返回；
 * 5. 快照字段实时 join 正确（客户色号/色名/合同号/染厂ID及名称/投染单位）；
 * 6. 软删缸排除。
 *
 * @author luna
 */
@DisplayName("生产管理 - 排缸日历后端查询单测 (Issue 06)")
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

    private static final LocalDate FIXED_TODAY = LocalDate.of(2026, 10, 15);

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ProgressFeedbackDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ProgressFeedbackNoteDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), DyeingOrderDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), DyeingColorDO.class);

        progressFeedbackService.setTodayProvider(() -> FIXED_TODAY);
    }

    private ProgressFeedbackDO createVat(Long id, Long orderId, Long colorId, String vatNo,
                                         LocalDate scheduledDate, String status,
                                         BigDecimal quantity, Integer pieces, Boolean deleted) {
        ProgressFeedbackDO vat = new ProgressFeedbackDO();
        vat.setId(id);
        vat.setDyeingOrderId(orderId);
        vat.setDyeingColorId(colorId);
        vat.setVatNo(vatNo);
        vat.setScheduledDate(scheduledDate);
        vat.setStatus(status);
        vat.setQuantity(quantity);
        vat.setPieces(pieces);
        vat.setDeleted(deleted);
        return vat;
    }

    private DyeingOrderDO createOrder(Long id, String status, String contractNo,
                                      Long factoryId, String factoryName, Boolean deleted) {
        DyeingOrderDO order = new DyeingOrderDO();
        order.setId(id);
        order.setStatus(status);
        order.setContractNo(contractNo);
        order.setDyeingFactoryId(factoryId);
        order.setDyeingFactoryName(factoryName);
        order.setDeleted(deleted);
        return order;
    }

    private DyeingColorDO createColor(Long id, Long orderId, String colorNo,
                                      String colorName, String unit, Boolean deleted) {
        DyeingColorDO color = new DyeingColorDO();
        color.setId(id);
        color.setDyeingOrderId(orderId);
        color.setCustomerColorNo(colorNo);
        color.setColorName(colorName);
        color.setDyeingInputUnit(unit);
        color.setDeleted(deleted);
        return color;
    }

    @Test
    @DisplayName("场景 1：month 区间只返回排缸日落月内的缸，月外缸不返回")
    void testMonthRange_OnlyReturnsVatsInMonth() {
        // Arrange
        ProgressFeedbackDO vatIn1 = createVat(1L, 10L, 100L, "VAT-01", LocalDate.of(2026, 10, 1), "SCHEDULED", new BigDecimal("300"), 10, false);
        ProgressFeedbackDO vatIn2 = createVat(2L, 10L, 100L, "VAT-02", LocalDate.of(2026, 10, 31), "DYEING", new BigDecimal("400"), 15, false);
        ProgressFeedbackDO vatOutBefore = createVat(3L, 10L, 100L, "VAT-03", LocalDate.of(2026, 9, 30), "SCHEDULED", new BigDecimal("200"), 8, false);
        ProgressFeedbackDO vatOutAfter = createVat(4L, 10L, 100L, "VAT-04", LocalDate.of(2026, 11, 1), "SCHEDULED", new BigDecimal("500"), 20, false);

        DyeingOrderDO order = createOrder(10L, "ACTIVE", "HT-2026001", 501L, "永盛染厂", false);
        DyeingColorDO color = createColor(100L, 10L, "C001", "藏青", "KG", false);

        when(progressFeedbackMapper.selectCalendarList(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), null))
                .thenReturn(List.of(vatIn1, vatIn2, vatOutBefore, vatOutAfter));
        when(dyeingOrderMapper.selectListByIds(any())).thenReturn(List.of(order));
        when(dyeingColorMapper.selectListByIds(any())).thenReturn(List.of(color));

        // Act
        ProgressFeedbackCalendarReqVO reqVO = new ProgressFeedbackCalendarReqVO();
        reqVO.setMonth("2026-10");
        ProgressFeedbackCalendarRespVO respVO = progressFeedbackService.getCalendar(reqVO);

        // Assert
        assertNotNull(respVO);
        assertEquals(2, respVO.getVats().size());
        assertEquals("VAT-01", respVO.getVats().get(0).getVatNo());
        assertEquals("VAT-02", respVO.getVats().get(1).getVatNo());
    }

    @Test
    @DisplayName("场景 2：from/to 自定义区间筛选生效")
    void testFromToRange_ReturnsVatsInRange() {
        // Arrange
        ProgressFeedbackDO vat1 = createVat(1L, 10L, 100L, "VAT-01", LocalDate.of(2026, 10, 5), "DYEING", new BigDecimal("300"), 10, false);
        ProgressFeedbackDO vat2 = createVat(2L, 10L, 100L, "VAT-02", LocalDate.of(2026, 10, 15), "DYEING", new BigDecimal("400"), 15, false);
        ProgressFeedbackDO vat3 = createVat(3L, 10L, 100L, "VAT-03", LocalDate.of(2026, 10, 25), "DYEING", new BigDecimal("500"), 20, false);

        DyeingOrderDO order = createOrder(10L, "ACTIVE", "HT-2026001", 501L, "永盛染厂", false);
        DyeingColorDO color = createColor(100L, 10L, "C001", "藏青", "KG", false);

        when(progressFeedbackMapper.selectCalendarList(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 20), null))
                .thenReturn(List.of(vat1, vat2, vat3));
        when(dyeingOrderMapper.selectListByIds(any())).thenReturn(List.of(order));
        when(dyeingColorMapper.selectListByIds(any())).thenReturn(List.of(color));

        // Act
        ProgressFeedbackCalendarReqVO reqVO = new ProgressFeedbackCalendarReqVO();
        reqVO.setFrom(LocalDate.of(2026, 10, 10));
        reqVO.setTo(LocalDate.of(2026, 10, 20));
        ProgressFeedbackCalendarRespVO respVO = progressFeedbackService.getCalendar(reqVO);

        // Assert
        assertNotNull(respVO);
        assertEquals(1, respVO.getVats().size());
        assertEquals("VAT-02", respVO.getVats().get(0).getVatNo());
    }

    @Test
    @DisplayName("场景 3：dyeingFactoryId 承染厂过滤生效")
    void testDyeingFactoryIdFilter() {
        // Arrange
        ProgressFeedbackDO vat1 = createVat(1L, 10L, 100L, "VAT-01", LocalDate.of(2026, 10, 10), "SCHEDULED", new BigDecimal("300"), 10, false);
        ProgressFeedbackDO vat2 = createVat(2L, 20L, 200L, "VAT-02", LocalDate.of(2026, 10, 10), "SCHEDULED", new BigDecimal("400"), 15, false);

        DyeingOrderDO order1 = createOrder(10L, "ACTIVE", "HT-001", 501L, "永盛染厂", false);
        DyeingOrderDO order2 = createOrder(20L, "ACTIVE", "HT-002", 502L, "华泰染厂", false);
        DyeingColorDO color1 = createColor(100L, 10L, "C001", "藏青", "KG", false);
        DyeingColorDO color2 = createColor(200L, 20L, "C002", "黑色", "M", false);

        when(progressFeedbackMapper.selectCalendarList(any(), any(), any())).thenReturn(List.of(vat1, vat2));
        when(dyeingOrderMapper.selectListByIds(any())).thenReturn(List.of(order1, order2));
        when(dyeingColorMapper.selectListByIds(any())).thenReturn(List.of(color1, color2));

        // Act - 筛选 501L
        ProgressFeedbackCalendarReqVO reqVO = new ProgressFeedbackCalendarReqVO();
        reqVO.setMonth("2026-10");
        reqVO.setDyeingFactoryId(501L);
        ProgressFeedbackCalendarRespVO respVO = progressFeedbackService.getCalendar(reqVO);

        // Assert
        assertNotNull(respVO);
        assertEquals(1, respVO.getVats().size());
        assertEquals("VAT-01", respVO.getVats().get(0).getVatNo());
        assertEquals(501L, respVO.getVats().get(0).getDyeingFactoryId());
        assertEquals("永盛染厂", respVO.getVats().get(0).getDyeingFactoryName());
    }

    @Test
    @DisplayName("场景 4：status 状态过滤生效")
    void testStatusFilter() {
        // Arrange
        ProgressFeedbackDO vat1 = createVat(1L, 10L, 100L, "VAT-01", LocalDate.of(2026, 10, 10), "DYEING", new BigDecimal("300"), 10, false);
        ProgressFeedbackDO vat2 = createVat(2L, 10L, 100L, "VAT-02", LocalDate.of(2026, 10, 11), "FIXING", new BigDecimal("400"), 15, false);

        DyeingOrderDO order = createOrder(10L, "ACTIVE", "HT-001", 501L, "永盛染厂", false);
        DyeingColorDO color = createColor(100L, 10L, "C001", "藏青", "KG", false);

        when(progressFeedbackMapper.selectCalendarList(any(), any(), any())).thenReturn(List.of(vat1, vat2));
        when(dyeingOrderMapper.selectListByIds(any())).thenReturn(List.of(order));
        when(dyeingColorMapper.selectListByIds(any())).thenReturn(List.of(color));

        // Act
        ProgressFeedbackCalendarReqVO reqVO = new ProgressFeedbackCalendarReqVO();
        reqVO.setMonth("2026-10");
        reqVO.setStatus("DYEING");
        ProgressFeedbackCalendarRespVO respVO = progressFeedbackService.getCalendar(reqVO);

        // Assert
        assertNotNull(respVO);
        assertEquals(1, respVO.getVats().size());
        assertEquals("VAT-01", respVO.getVats().get(0).getVatNo());
        assertEquals("DYEING", respVO.getVats().get(0).getStatus());
    }

    @Test
    @DisplayName("场景 5：COMPLETED 缸默认返回支持回看，且可被 status 显式筛选")
    void testCompletedVats_DefaultIncludedAndFilterable() {
        // Arrange
        ProgressFeedbackDO vatProgress = createVat(1L, 10L, 100L, "VAT-01", LocalDate.of(2026, 10, 10), "DYEING", new BigDecimal("300"), 10, false);
        ProgressFeedbackDO vatCompleted = createVat(2L, 10L, 100L, "VAT-02", LocalDate.of(2026, 10, 12), "COMPLETED", new BigDecimal("500"), 20, false);

        DyeingOrderDO order = createOrder(10L, "ACTIVE", "HT-001", 501L, "永盛染厂", false);
        DyeingColorDO color = createColor(100L, 10L, "C001", "藏青", "KG", false);

        when(progressFeedbackMapper.selectCalendarList(any(), any(), any())).thenReturn(List.of(vatProgress, vatCompleted));
        when(dyeingOrderMapper.selectListByIds(any())).thenReturn(List.of(order));
        when(dyeingColorMapper.selectListByIds(any())).thenReturn(List.of(color));

        // Act 1: 默认不传 status，COMPLETED 缸返回
        ProgressFeedbackCalendarReqVO reqVOAll = new ProgressFeedbackCalendarReqVO();
        reqVOAll.setMonth("2026-10");
        ProgressFeedbackCalendarRespVO respAll = progressFeedbackService.getCalendar(reqVOAll);
        assertEquals(2, respAll.getVats().size());

        // Act 2: 显式筛选 status = COMPLETED，只返回已完成缸
        ProgressFeedbackCalendarReqVO reqVOCompleted = new ProgressFeedbackCalendarReqVO();
        reqVOCompleted.setMonth("2026-10");
        reqVOCompleted.setStatus("COMPLETED");
        ProgressFeedbackCalendarRespVO respCompleted = progressFeedbackService.getCalendar(reqVOCompleted);
        assertEquals(1, respCompleted.getVats().size());
        assertEquals("VAT-02", respCompleted.getVats().get(0).getVatNo());
        assertEquals("COMPLETED", respCompleted.getVats().get(0).getStatus());
    }

    @Test
    @DisplayName("场景 6：染色单状态判定——ACTIVE/COMPLETED 纳入，CANCELLED/DRAFT/软删单排除")
    void testDyeingOrderStatus_ActiveAndCompletedIncluded_CancelledAndDraftExcluded() {
        // Arrange
        ProgressFeedbackDO vat1 = createVat(1L, 10L, 100L, "VAT-ACTIVE", LocalDate.of(2026, 10, 5), "DYEING", new BigDecimal("300"), 10, false);
        ProgressFeedbackDO vat2 = createVat(2L, 20L, 200L, "VAT-COMPLETED", LocalDate.of(2026, 10, 6), "COMPLETED", new BigDecimal("400"), 15, false);
        ProgressFeedbackDO vat3 = createVat(3L, 30L, 300L, "VAT-CANCELLED", LocalDate.of(2026, 10, 7), "SCHEDULED", new BigDecimal("200"), 8, false);
        ProgressFeedbackDO vat4 = createVat(4L, 40L, 400L, "VAT-DRAFT", LocalDate.of(2026, 10, 8), "SCHEDULED", new BigDecimal("200"), 8, false);
        ProgressFeedbackDO vat5 = createVat(5L, 50L, 500L, "VAT-DELETED", LocalDate.of(2026, 10, 9), "SCHEDULED", new BigDecimal("200"), 8, false);

        DyeingOrderDO orderActive = createOrder(10L, "ACTIVE", "HT-001", 501L, "染厂A", false);
        DyeingOrderDO orderCompleted = createOrder(20L, "COMPLETED", "HT-002", 501L, "染厂A", false);
        DyeingOrderDO orderCancelled = createOrder(30L, "CANCELLED", "HT-003", 501L, "染厂A", false);
        DyeingOrderDO orderDraft = createOrder(40L, "DRAFT", "HT-004", 501L, "染厂A", false);
        DyeingOrderDO orderDeleted = createOrder(50L, "ACTIVE", "HT-005", 501L, "染厂A", true);

        DyeingColorDO color1 = createColor(100L, 10L, "C001", "藏青", "KG", false);
        DyeingColorDO color2 = createColor(200L, 20L, "C002", "黑色", "KG", false);
        DyeingColorDO color3 = createColor(300L, 30L, "C003", "红色", "KG", false);
        DyeingColorDO color4 = createColor(400L, 40L, "C004", "白色", "KG", false);
        DyeingColorDO color5 = createColor(500L, 50L, "C005", "黄色", "KG", false);

        when(progressFeedbackMapper.selectCalendarList(any(), any(), any()))
                .thenReturn(List.of(vat1, vat2, vat3, vat4, vat5));
        when(dyeingOrderMapper.selectListByIds(any()))
                .thenReturn(List.of(orderActive, orderCompleted, orderCancelled, orderDraft, orderDeleted));
        when(dyeingColorMapper.selectListByIds(any()))
                .thenReturn(List.of(color1, color2, color3, color4, color5));

        // Act
        ProgressFeedbackCalendarReqVO reqVO = new ProgressFeedbackCalendarReqVO();
        reqVO.setMonth("2026-10");
        ProgressFeedbackCalendarRespVO respVO = progressFeedbackService.getCalendar(reqVO);

        // Assert
        assertNotNull(respVO);
        assertEquals(2, respVO.getVats().size());
        assertEquals("VAT-ACTIVE", respVO.getVats().get(0).getVatNo());
        assertEquals("VAT-COMPLETED", respVO.getVats().get(1).getVatNo());
    }

    @Test
    @DisplayName("场景 7：展示快照字段 join 正确")
    void testSnapshotFields_JoinedCorrectly() {
        // Arrange
        ProgressFeedbackDO vat = createVat(3L, 9L, 21L, "A-12", LocalDate.of(2026, 10, 9), "DYEING", new BigDecimal("500.00"), 20, false);
        DyeingOrderDO order = createOrder(9L, "ACTIVE", "CH2026-001", 5L, "永盛染厂", false);
        DyeingColorDO color = createColor(21L, 9L, "C101", "藏青", "KG", false);

        when(progressFeedbackMapper.selectCalendarList(any(), any(), any())).thenReturn(List.of(vat));
        when(dyeingOrderMapper.selectListByIds(any())).thenReturn(List.of(order));
        when(dyeingColorMapper.selectListByIds(any())).thenReturn(List.of(color));

        // Act
        ProgressFeedbackCalendarReqVO reqVO = new ProgressFeedbackCalendarReqVO();
        reqVO.setMonth("2026-10");
        ProgressFeedbackCalendarRespVO respVO = progressFeedbackService.getCalendar(reqVO);

        // Assert
        assertNotNull(respVO);
        assertEquals(1, respVO.getVats().size());
        ProgressFeedbackCalendarVatVO vo = respVO.getVats().get(0);
        assertEquals(3L, vo.getFeedbackId());
        assertEquals(9L, vo.getDyeingOrderId());
        assertEquals(21L, vo.getDyeingColorId());
        assertEquals("A-12", vo.getVatNo());
        assertEquals(LocalDate.of(2026, 10, 9), vo.getScheduledDate());
        assertEquals("DYEING", vo.getStatus());
        assertEquals(new BigDecimal("500.00"), vo.getQuantity());
        assertEquals(20, vo.getPieces());
        assertEquals("KG", vo.getUnit());
        assertEquals("C101", vo.getCustomerColorNo());
        assertEquals("藏青", vo.getColorName());
        assertEquals("CH2026-001", vo.getContractNo());
        assertEquals(5L, vo.getDyeingFactoryId());
        assertEquals("永盛染厂", vo.getDyeingFactoryName());
    }

    @Test
    @DisplayName("场景 8：软删除反馈缸排除")
    void testSoftDeletedVats_Excluded() {
        // Arrange
        ProgressFeedbackDO vatNormal = createVat(1L, 10L, 100L, "VAT-01", LocalDate.of(2026, 10, 10), "SCHEDULED", new BigDecimal("300"), 10, false);
        ProgressFeedbackDO vatDeleted = createVat(2L, 10L, 100L, "VAT-02", LocalDate.of(2026, 10, 11), "SCHEDULED", new BigDecimal("400"), 15, true);

        DyeingOrderDO order = createOrder(10L, "ACTIVE", "HT-001", 501L, "永盛染厂", false);
        DyeingColorDO color = createColor(100L, 10L, "C001", "藏青", "KG", false);

        when(progressFeedbackMapper.selectCalendarList(any(), any(), any())).thenReturn(List.of(vatNormal, vatDeleted));
        when(dyeingOrderMapper.selectListByIds(any())).thenReturn(List.of(order));
        when(dyeingColorMapper.selectListByIds(any())).thenReturn(List.of(color));

        // Act
        ProgressFeedbackCalendarReqVO reqVO = new ProgressFeedbackCalendarReqVO();
        reqVO.setMonth("2026-10");
        ProgressFeedbackCalendarRespVO respVO = progressFeedbackService.getCalendar(reqVO);

        // Assert
        assertEquals(1, respVO.getVats().size());
        assertEquals("VAT-01", respVO.getVats().get(0).getVatNo());
    }

    @Test
    @DisplayName("场景 9：反转日期区间返回空列表，非法月份格式抛出异常")
    void testInvalidDateRanges() {
        // from > to 返回空列表
        ProgressFeedbackCalendarReqVO reqVOReversed = new ProgressFeedbackCalendarReqVO();
        reqVOReversed.setFrom(LocalDate.of(2026, 10, 20));
        reqVOReversed.setTo(LocalDate.of(2026, 10, 10));
        ProgressFeedbackCalendarRespVO respReversed = progressFeedbackService.getCalendar(reqVOReversed);
        assertNotNull(respReversed);
        assertTrue(respReversed.getVats().isEmpty());

        // 非法月份格式抛出 ServiceException
        ProgressFeedbackCalendarReqVO reqVOInvalidMonth = new ProgressFeedbackCalendarReqVO();
        reqVOInvalidMonth.setMonth("invalid-month");
        assertThrows(ServiceException.class, () -> progressFeedbackService.getCalendar(reqVOInvalidMonth));
    }

}
