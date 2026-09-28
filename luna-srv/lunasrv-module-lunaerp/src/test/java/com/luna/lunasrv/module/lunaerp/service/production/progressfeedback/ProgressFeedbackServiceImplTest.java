package com.luna.lunasrv.module.lunaerp.service.production.progressfeedback;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo.ProgressFeedbackColorSectionRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo.ProgressFeedbackPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo.ProgressFeedbackStatisticsRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo.ProgressFeedbackVatRespVO;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * {@link ProgressFeedbackServiceImpl} 纯 Mockito 单元测试。
 *
 * 覆盖 02 票要求：
 * 1. 骨架过滤（单状态 ACTIVE/COMPLETED 纳入，DRAFT/CANCELLED 与软删单排除）
 * 2. 零缸虚拟节（无缸颜色行渲染空缸列表、覆盖率 0%、非超排）
 * 3. 挂载聚合（多缸数量/匹数累加，零快照实时字段透出）
 * 4. 延期判定三情形（未到期、过期未完成延期、过期已完成不延期）
 * 5. 覆盖率与超排（正常比例、超排预警、零缸、零分母、空分母）
 * 6. 统计条计数口径（未排缸节数、进行中缸数、已完成缸数、延期缸数与筛选联动）
 * 7. 基础状态筛选（NOT_SCHEDULED 与特定工序状态下推）
 *
 * @author luna
 */
class ProgressFeedbackServiceImplTest {

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

    private static final LocalDate FIXED_TODAY = LocalDate.of(2026, 9, 28);

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ProgressFeedbackDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ProgressFeedbackNoteDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), DyeingOrderDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), DyeingColorDO.class);

        // 固定当天日期为 2026-09-28，保证延期判定绝对确定
        progressFeedbackService.setTodayProvider(() -> FIXED_TODAY);
    }

    private DyeingOrderDO createOrder(Long id, String status, Boolean deleted, LocalDate deliveryDate) {
        DyeingOrderDO order = new DyeingOrderDO();
        order.setId(id);
        order.setStatus(status);
        order.setDeleted(deleted);
        order.setContractNo("HT-" + id);
        order.setClientName("客户-" + id);
        order.setDyeingFactoryId(200L);
        order.setDyeingFactoryName("染厂-200");
        order.setDeliveryDate(deliveryDate);
        order.setProductCode("PRD-" + id);
        order.setProductName("面料-" + id);
        order.setOrderDate(FIXED_TODAY.minusDays(5));
        return order;
    }

    private DyeingColorDO createColor(Long id, Long orderId, int lineNo, String colorName, BigDecimal qty, Integer pieces, String unit) {
        DyeingColorDO color = new DyeingColorDO();
        color.setId(id);
        color.setDyeingOrderId(orderId);
        color.setLineNo(lineNo);
        color.setCustomerColorNo("PAN-" + id);
        color.setColorName(colorName);
        color.setQuantity(qty);
        color.setPieces(pieces);
        color.setDyeingInputUnit(unit);
        color.setDeleted(false);
        return color;
    }

    private ProgressFeedbackDO createVat(Long id, Long orderId, Long colorId, String vatNo, BigDecimal qty, Integer pieces, String status) {
        ProgressFeedbackDO vat = new ProgressFeedbackDO();
        vat.setId(id);
        vat.setDyeingOrderId(orderId);
        vat.setDyeingColorId(colorId);
        vat.setVatNo(vatNo);
        vat.setQuantity(qty);
        vat.setPieces(pieces);
        vat.setStatus(status);
        vat.setDeleted(false);
        return vat;
    }

    // ==================== 1. 骨架过滤（单状态与软删除） ====================

    @Test
    @DisplayName("骨架过滤：仅 ACTIVE 与 COMPLETED 染色单的颜色行进入覆盖层，DRAFT/CANCELLED/软删排除")
    void getProgressFeedbackPage_skeletonFilter_activeAndCompletedOnly_excludeDraftCancelledAndDeleted() {
        DyeingOrderDO orderActive = createOrder(1L, "ACTIVE", false, FIXED_TODAY.plusDays(3));
        DyeingOrderDO orderCompleted = createOrder(2L, "COMPLETED", false, FIXED_TODAY.plusDays(3));
        DyeingOrderDO orderDraft = createOrder(3L, "DRAFT", false, FIXED_TODAY.plusDays(3));
        DyeingOrderDO orderCancelled = createOrder(4L, "CANCELLED", false, FIXED_TODAY.plusDays(3));
        DyeingOrderDO orderDeleted = createOrder(5L, "ACTIVE", true, FIXED_TODAY.plusDays(3));

        when(dyeingOrderMapper.selectList(any())).thenReturn(List.of(orderActive, orderCompleted, orderDraft, orderCancelled, orderDeleted));

        DyeingColorDO color1 = createColor(101L, 1L, 1, "黑色", new BigDecimal("500"), 20, "KG");
        DyeingColorDO color2 = createColor(102L, 2L, 1, "白色", new BigDecimal("600"), 25, "KG");
        when(dyeingColorMapper.selectList(any())).thenReturn(List.of(color1, color2));
        when(progressFeedbackMapper.selectListByColorIds(any())).thenReturn(Collections.emptyList());

        ProgressFeedbackPageReqVO reqVO = new ProgressFeedbackPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);

        PageResult<ProgressFeedbackColorSectionRespVO> result = progressFeedbackService.getProgressFeedbackPage(reqVO);

        assertEquals(2, result.getTotal());
        assertEquals(2, result.getList().size());
        assertEquals(101L, result.getList().get(0).getDyeingColorId());
        assertEquals(102L, result.getList().get(1).getDyeingColorId());
    }

    // ==================== 2. 零缸虚拟节 ====================

    @Test
    @DisplayName("零缸虚拟节：没有反馈缸的颜色行返回空缸列表、覆盖率 0%、非超排")
    void getProgressFeedbackPage_virtualZeroVatSection() {
        DyeingOrderDO order = createOrder(1L, "ACTIVE", false, FIXED_TODAY.plusDays(5));
        when(dyeingOrderMapper.selectList(any())).thenReturn(List.of(order));

        DyeingColorDO color = createColor(101L, 1L, 1, "藏青", new BigDecimal("1000.00"), 40, "KG");
        when(dyeingColorMapper.selectList(any())).thenReturn(List.of(color));
        when(progressFeedbackMapper.selectListByColorIds(any())).thenReturn(Collections.emptyList());

        ProgressFeedbackPageReqVO reqVO = new ProgressFeedbackPageReqVO();
        PageResult<ProgressFeedbackColorSectionRespVO> result = progressFeedbackService.getProgressFeedbackPage(reqVO);

        assertEquals(1, result.getList().size());
        ProgressFeedbackColorSectionRespVO section = result.getList().get(0);
        assertNotNull(section.getVats());
        assertTrue(section.getVats().isEmpty(), "零缸颜色行的 vats 列表应为空列表供前端渲染占位卡");
        assertEquals(BigDecimal.ZERO.setScale(2), section.getTotalVatQuantity());
        assertEquals(0, section.getTotalVatPieces());
        assertEquals(new BigDecimal("0.00"), section.getCoveragePercent());
        assertFalse(section.getOverScheduled(), "零缸不应触发超排");
        // 验证实时字段从染色单与颜色行取出（零快照）
        assertEquals("HT-1", section.getContractNo());
        assertEquals("客户-1", section.getClientName());
        assertEquals("染厂-200", section.getDyeingFactoryName());
        assertEquals("面料-1", section.getProductName());
        assertEquals("藏青", section.getColorName());
        assertEquals("PAN-101", section.getCustomerColorNo());
        assertEquals("KG", section.getUnit());
    }

    // ==================== 3. 挂载聚合 ====================

    @Test
    @DisplayName("挂载聚合：正确累加缸数量与匹数，带出缸卡列表")
    void getProgressFeedbackPage_mountVatsAndAggregateQuantity() {
        DyeingOrderDO order = createOrder(1L, "ACTIVE", false, FIXED_TODAY.plusDays(5));
        when(dyeingOrderMapper.selectList(any())).thenReturn(List.of(order));

        DyeingColorDO color = createColor(101L, 1L, 1, "大红", new BigDecimal("1000.00"), 40, "M");
        when(dyeingColorMapper.selectList(any())).thenReturn(List.of(color));

        ProgressFeedbackDO vat1 = createVat(1001L, 1L, 101L, "V01", new BigDecimal("450.00"), 18, "SCHEDULED");
        ProgressFeedbackDO vat2 = createVat(1002L, 1L, 101L, "V02", new BigDecimal("350.00"), 14, "DYEING");
        when(progressFeedbackMapper.selectListByColorIds(any())).thenReturn(List.of(vat1, vat2));

        ProgressFeedbackPageReqVO reqVO = new ProgressFeedbackPageReqVO();
        PageResult<ProgressFeedbackColorSectionRespVO> result = progressFeedbackService.getProgressFeedbackPage(reqVO);

        ProgressFeedbackColorSectionRespVO section = result.getList().get(0);
        assertEquals(2, section.getVats().size());
        assertEquals(new BigDecimal("800.00"), section.getTotalVatQuantity());
        assertEquals(32, section.getTotalVatPieces());
        assertEquals(new BigDecimal("80.00"), section.getCoveragePercent());
        assertFalse(section.getOverScheduled());
        assertEquals("M", section.getUnit(), "缸卡单位跟随颜色行");
    }

    // ==================== 4. 延期判定三情形 ====================

    @Test
    @DisplayName("延期判定：未到期=false、过期未完成=true、过期已完成=false")
    void getProgressFeedbackPage_delayDetection_threeCases() {
        // Order 1: 交期在未来（未到期）
        DyeingOrderDO orderFuture = createOrder(1L, "ACTIVE", false, FIXED_TODAY.plusDays(2));
        // Order 2: 交期在过去（已过期）
        DyeingOrderDO orderPast = createOrder(2L, "ACTIVE", false, FIXED_TODAY.minusDays(2));

        when(dyeingOrderMapper.selectList(any())).thenReturn(List.of(orderFuture, orderPast));

        DyeingColorDO colorFuture = createColor(101L, 1L, 1, "蓝", new BigDecimal("500"), 20, "KG");
        DyeingColorDO colorPast = createColor(102L, 2L, 1, "绿", new BigDecimal("500"), 20, "KG");
        when(dyeingColorMapper.selectList(any())).thenReturn(List.of(colorFuture, colorPast));

        // vat1: 未到期且 DYEING -> delayed=false
        ProgressFeedbackDO vat1 = createVat(1001L, 1L, 101L, "V1", new BigDecimal("500"), 20, "DYEING");
        // vat2: 已过期且 FIXING（非 COMPLETED）-> delayed=true
        ProgressFeedbackDO vat2 = createVat(1002L, 2L, 102L, "V2", new BigDecimal("300"), 10, "FIXING");
        // vat3: 已过期但 COMPLETED -> delayed=false
        ProgressFeedbackDO vat3 = createVat(1003L, 2L, 102L, "V3", new BigDecimal("200"), 10, "COMPLETED");

        when(progressFeedbackMapper.selectListByColorIds(any())).thenReturn(List.of(vat1, vat2, vat3));

        ProgressFeedbackPageReqVO reqVO = new ProgressFeedbackPageReqVO();
        PageResult<ProgressFeedbackColorSectionRespVO> result = progressFeedbackService.getProgressFeedbackPage(reqVO);

        ProgressFeedbackVatRespVO respVat1 = result.getList().get(0).getVats().get(0);
        assertFalse(respVat1.getDelayed(), "交期在未来时即使未完成也不应判定为延期");

        ProgressFeedbackVatRespVO respVat2 = result.getList().get(1).getVats().get(0);
        assertTrue(respVat2.getDelayed(), "交期在过去且未完成时应判定为延期");

        ProgressFeedbackVatRespVO respVat3 = result.getList().get(1).getVats().get(1);
        assertFalse(respVat3.getDelayed(), "交期在过去但状态为已完成（COMPLETED）不应判定为延期");
    }

    // ==================== 5. 覆盖率与超排分支 ====================

    @Test
    @DisplayName("覆盖率计算与超排标记：正常覆盖率、超排预警、零分母、空分母")
    void getProgressFeedbackPage_coverageAndOverScheduled_allBranches() {
        DyeingOrderDO order = createOrder(1L, "ACTIVE", false, FIXED_TODAY.plusDays(5));
        when(dyeingOrderMapper.selectList(any())).thenReturn(List.of(order));

        // 正常覆盖: 1000 下 800 -> 80.00%, overScheduled=false
        DyeingColorDO c1 = createColor(101L, 1L, 1, "色1", new BigDecimal("1000"), 40, "KG");
        // 超排: 1000 下 1200 -> 120.00%, overScheduled=true
        DyeingColorDO c2 = createColor(102L, 1L, 2, "色2", new BigDecimal("1000"), 40, "KG");
        // 零分母: 0 下 100 -> 0.00%, overScheduled=true
        DyeingColorDO c3 = createColor(103L, 1L, 3, "色3", BigDecimal.ZERO, 0, "KG");
        // 空分母: null 下 100 -> 0.00%, overScheduled=true
        DyeingColorDO c4 = createColor(104L, 1L, 4, "色4", null, null, "KG");

        when(dyeingColorMapper.selectList(any())).thenReturn(List.of(c1, c2, c3, c4));

        ProgressFeedbackDO v1 = createVat(1001L, 1L, 101L, "V1", new BigDecimal("800"), 32, "SCHEDULED");
        ProgressFeedbackDO v2 = createVat(1002L, 1L, 102L, "V2", new BigDecimal("1200"), 48, "SCHEDULED");
        ProgressFeedbackDO v3 = createVat(1003L, 1L, 103L, "V3", new BigDecimal("100"), 4, "SCHEDULED");
        ProgressFeedbackDO v4 = createVat(1004L, 1L, 104L, "V4", new BigDecimal("100"), 4, "SCHEDULED");
        when(progressFeedbackMapper.selectListByColorIds(any())).thenReturn(List.of(v1, v2, v3, v4));

        ProgressFeedbackPageReqVO reqVO = new ProgressFeedbackPageReqVO();
        PageResult<ProgressFeedbackColorSectionRespVO> result = progressFeedbackService.getProgressFeedbackPage(reqVO);

        ProgressFeedbackColorSectionRespVO s1 = result.getList().get(0);
        assertEquals(new BigDecimal("80.00"), s1.getCoveragePercent());
        assertFalse(s1.getOverScheduled());

        ProgressFeedbackColorSectionRespVO s2 = result.getList().get(1);
        assertEquals(new BigDecimal("120.00"), s2.getCoveragePercent());
        assertTrue(s2.getOverScheduled(), "已建缸合计大于颜色行数量时应标记超排");

        ProgressFeedbackColorSectionRespVO s3 = result.getList().get(2);
        assertEquals(new BigDecimal("0.00"), s3.getCoveragePercent(), "零分母时覆盖率应为 0.00%");
        assertTrue(s3.getOverScheduled());

        ProgressFeedbackColorSectionRespVO s4 = result.getList().get(3);
        assertEquals(new BigDecimal("0.00"), s4.getCoveragePercent(), "空分母时覆盖率应为 0.00%");
        assertTrue(s4.getOverScheduled());
    }

    // ==================== 6. 统计条计数与筛选联动 ====================

    @Test
    @DisplayName("统计条计数：零缸节数、进行中缸数、已完成缸数、延期缸数统计准确")
    void getProgressFeedbackStatistics_countsConsistent() {
        // Order 1: 交期未到
        DyeingOrderDO o1 = createOrder(1L, "ACTIVE", false, FIXED_TODAY.plusDays(5));
        // Order 2: 交期已过
        DyeingOrderDO o2 = createOrder(2L, "ACTIVE", false, FIXED_TODAY.minusDays(3));

        when(dyeingOrderMapper.selectList(any())).thenReturn(List.of(o1, o2));

        DyeingColorDO c1 = createColor(101L, 1L, 1, "未排缸色", new BigDecimal("100"), 4, "KG");
        DyeingColorDO c2 = createColor(102L, 1L, 2, "进行中色", new BigDecimal("200"), 8, "KG");
        DyeingColorDO c3 = createColor(103L, 2L, 1, "延期与完成色", new BigDecimal("300"), 12, "KG");

        when(dyeingColorMapper.selectList(any())).thenReturn(List.of(c1, c2, c3));

        // c1: 0 缸 -> 计入 unscheduledColorCount
        // c2: vat1 (SCHEDULED), vat2 (DYEING) -> 2 进行中
        ProgressFeedbackDO vat1 = createVat(1001L, 1L, 102L, "V1", new BigDecimal("100"), 4, "SCHEDULED");
        ProgressFeedbackDO vat2 = createVat(1002L, 1L, 102L, "V2", new BigDecimal("100"), 4, "DYEING");
        // c3: vat3 (FIXING, 过期 -> 延期+进行中), vat4 (COMPLETED, 过期 -> 已完成不延期)
        ProgressFeedbackDO vat3 = createVat(1003L, 2L, 103L, "V3", new BigDecimal("150"), 6, "FIXING");
        ProgressFeedbackDO vat4 = createVat(1004L, 2L, 103L, "V4", new BigDecimal("150"), 6, "COMPLETED");

        when(progressFeedbackMapper.selectListByColorIds(any())).thenReturn(List.of(vat1, vat2, vat3, vat4));

        ProgressFeedbackPageReqVO reqVO = new ProgressFeedbackPageReqVO();
        ProgressFeedbackStatisticsRespVO stats = progressFeedbackService.getProgressFeedbackStatistics(reqVO);

        assertEquals(1L, stats.getUnscheduledColorCount(), "未排缸颜色行应为 1");
        assertEquals(3L, stats.getInProgressVatCount(), "进行中缸数应为 3 (SCHEDULED+DYEING+FIXING)");
        assertEquals(1L, stats.getCompletedVatCount(), "已完成缸数应为 1 (COMPLETED)");
        assertEquals(1L, stats.getDelayedVatCount(), "延期缸数应为 1 (vat3 过期且未完成)");
    }

    // ==================== 7. 状态筛选下推 ====================

    @Test
    @DisplayName("状态筛选：NOT_SCHEDULED 仅返回零缸颜色行，DYEING 仅返回含染色中缸的颜色行")
    void getProgressFeedbackPage_statusFilter() {
        DyeingOrderDO o1 = createOrder(1L, "ACTIVE", false, FIXED_TODAY.plusDays(5));
        when(dyeingOrderMapper.selectList(any())).thenReturn(List.of(o1));

        DyeingColorDO c1 = createColor(101L, 1L, 1, "零缸", new BigDecimal("100"), 4, "KG");
        DyeingColorDO c2 = createColor(102L, 1L, 2, "有染色缸", new BigDecimal("200"), 8, "KG");
        when(dyeingColorMapper.selectList(any())).thenReturn(List.of(c1, c2));

        ProgressFeedbackDO vat1 = createVat(1001L, 1L, 102L, "V1", new BigDecimal("200"), 8, "DYEING");
        when(progressFeedbackMapper.selectListByColorIds(any())).thenReturn(List.of(vat1));

        // 筛选未排缸
        ProgressFeedbackPageReqVO reqNotScheduled = new ProgressFeedbackPageReqVO();
        reqNotScheduled.setStatus(ProgressFeedbackStatusEnum.NOT_SCHEDULED.getValue());
        PageResult<ProgressFeedbackColorSectionRespVO> resNotScheduled = progressFeedbackService.getProgressFeedbackPage(reqNotScheduled);
        assertEquals(1, resNotScheduled.getList().size());
        assertEquals(101L, resNotScheduled.getList().get(0).getDyeingColorId());

        // 筛选染色中
        ProgressFeedbackPageReqVO reqDyeing = new ProgressFeedbackPageReqVO();
        reqDyeing.setStatus(ProgressFeedbackStatusEnum.DYEING.getValue());
        PageResult<ProgressFeedbackColorSectionRespVO> resDyeing = progressFeedbackService.getProgressFeedbackPage(reqDyeing);
        assertEquals(1, resDyeing.getList().size());
        assertEquals(102L, resDyeing.getList().get(0).getDyeingColorId());
    }

}
