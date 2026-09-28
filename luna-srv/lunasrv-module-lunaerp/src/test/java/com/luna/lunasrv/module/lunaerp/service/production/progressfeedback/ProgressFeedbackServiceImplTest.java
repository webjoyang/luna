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
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo.ProgressFeedbackAdvanceReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo.ProgressFeedbackCreateReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo.ProgressFeedbackDetailRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo.ProgressFeedbackUpdateReqVO;
import com.luna.lunasrv.module.lunaerp.enums.production.progressfeedback.ProgressFeedbackStatusEnum;
import org.apache.ibatis.builder.MapperBuilderAssistant;
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

import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

    // ==================== 8. 建缸逻辑（03 票） ====================

    @Test
    @DisplayName("建缸成功：缺省状态 SCHEDULED、缺省排缸日为今天、字段正确落库")
    void createProgressFeedback_success_withDefaultStatusAndScheduledDate() {
        DyeingOrderDO order = createOrder(1L, "ACTIVE", false, FIXED_TODAY.plusDays(10));
        when(dyeingOrderMapper.selectById(1L)).thenReturn(order);

        DyeingColorDO color = createColor(101L, 1L, 1, "黑色", new BigDecimal("500.00"), 10, "KG");
        when(dyeingColorMapper.selectById(101L)).thenReturn(color);

        when(progressFeedbackMapper.selectByColorIdAndVatNo(101L, "V20260901")).thenReturn(null);

        ProgressFeedbackCreateReqVO reqVO = ProgressFeedbackCreateReqVO.builder()
                .dyeingOrderId(1L)
                .dyeingColorId(101L)
                .vatNo("V20260901")
                .quantity(new BigDecimal("250.00"))
                .pieces(5)
                .scheduledDate(null)
                .remark("首缸加急")
                .build();

        progressFeedbackService.createProgressFeedback(reqVO);

        ArgumentCaptor<ProgressFeedbackDO> captor = ArgumentCaptor.forClass(ProgressFeedbackDO.class);
        verify(progressFeedbackMapper).insert(captor.capture());
        ProgressFeedbackDO inserted = captor.getValue();

        assertEquals(1L, inserted.getDyeingOrderId());
        assertEquals(101L, inserted.getDyeingColorId());
        assertEquals("V20260901", inserted.getVatNo());
        assertEquals(new BigDecimal("250.00"), inserted.getQuantity());
        assertEquals(5, inserted.getPieces());
        assertEquals(ProgressFeedbackStatusEnum.SCHEDULED.getValue(), inserted.getStatus(), "初始状态必须为 SCHEDULED");
        assertEquals(FIXED_TODAY, inserted.getScheduledDate(), "缺省排缸日期应为今天");
        assertEquals("首缸加急", inserted.getRemark());
    }

    @Test
    @DisplayName("建缸成功：传入自定义排缸日优先")
    void createProgressFeedback_success_withCustomScheduledDate() {
        DyeingOrderDO order = createOrder(2L, "COMPLETED", false, FIXED_TODAY.plusDays(10));
        when(dyeingOrderMapper.selectById(2L)).thenReturn(order);

        DyeingColorDO color = createColor(201L, 2L, 1, "大红", new BigDecimal("300.00"), 6, "KG");
        when(dyeingColorMapper.selectById(201L)).thenReturn(color);

        when(progressFeedbackMapper.selectByColorIdAndVatNo(201L, "V20260902")).thenReturn(null);

        LocalDate customDate = LocalDate.of(2026, 10, 5);
        ProgressFeedbackCreateReqVO reqVO = ProgressFeedbackCreateReqVO.builder()
                .dyeingOrderId(2L)
                .dyeingColorId(201L)
                .vatNo("V20260902")
                .quantity(new BigDecimal("300.00"))
                .pieces(6)
                .scheduledDate(customDate)
                .remark("补缸")
                .build();

        progressFeedbackService.createProgressFeedback(reqVO);

        ArgumentCaptor<ProgressFeedbackDO> captor = ArgumentCaptor.forClass(ProgressFeedbackDO.class);
        verify(progressFeedbackMapper).insert(captor.capture());
        ProgressFeedbackDO inserted = captor.getValue();

        assertEquals(customDate, inserted.getScheduledDate(), "应优先采用传入的排缸日");
        assertEquals(ProgressFeedbackStatusEnum.SCHEDULED.getValue(), inserted.getStatus());
    }

    @Test
    @DisplayName("建缸校验：同单同色缸号重复抛出 001")
    void createProgressFeedback_vatNoDuplicate_throws001() {
        DyeingOrderDO order = createOrder(1L, "ACTIVE", false, FIXED_TODAY.plusDays(5));
        when(dyeingOrderMapper.selectById(1L)).thenReturn(order);

        DyeingColorDO color = createColor(101L, 1L, 1, "黑色", new BigDecimal("500.00"), 10, "KG");
        when(dyeingColorMapper.selectById(101L)).thenReturn(color);

        ProgressFeedbackDO existingVat = createVat(999L, 1L, 101L, "V01", new BigDecimal("100"), 2, "SCHEDULED");
        when(progressFeedbackMapper.selectByColorIdAndVatNo(101L, "V01")).thenReturn(existingVat);

        ProgressFeedbackCreateReqVO reqVO = ProgressFeedbackCreateReqVO.builder()
                .dyeingOrderId(1L)
                .dyeingColorId(101L)
                .vatNo("V01")
                .quantity(new BigDecimal("100.00"))
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> progressFeedbackService.createProgressFeedback(reqVO));
        assertEquals(PROGRESS_FEEDBACK_VAT_NO_DUPLICATE.getCode(), ex.getCode());
        verify(progressFeedbackMapper, never()).insert(any(ProgressFeedbackDO.class));
    }

    @Test
    @DisplayName("建缸校验：染色单不存在或状态不允许抛出 002 (null/deleted/DRAFT/CANCELLED)")
    void createProgressFeedback_orderNotActiveOrNotFound_throws002() {
        ProgressFeedbackCreateReqVO reqVO = ProgressFeedbackCreateReqVO.builder()
                .dyeingOrderId(1L)
                .dyeingColorId(101L)
                .vatNo("V01")
                .quantity(new BigDecimal("100.00"))
                .build();

        // 单据不存在
        when(dyeingOrderMapper.selectById(1L)).thenReturn(null);
        ServiceException ex1 = assertThrows(ServiceException.class, () -> progressFeedbackService.createProgressFeedback(reqVO));
        assertEquals(PROGRESS_FEEDBACK_DYEING_ORDER_STATUS_NOT_ALLOWED.getCode(), ex1.getCode());

        // 单据软删
        DyeingOrderDO deletedOrder = createOrder(1L, "ACTIVE", true, FIXED_TODAY.plusDays(5));
        when(dyeingOrderMapper.selectById(1L)).thenReturn(deletedOrder);
        ServiceException ex2 = assertThrows(ServiceException.class, () -> progressFeedbackService.createProgressFeedback(reqVO));
        assertEquals(PROGRESS_FEEDBACK_DYEING_ORDER_STATUS_NOT_ALLOWED.getCode(), ex2.getCode());

        // 单据状态为 DRAFT
        DyeingOrderDO draftOrder = createOrder(1L, "DRAFT", false, FIXED_TODAY.plusDays(5));
        when(dyeingOrderMapper.selectById(1L)).thenReturn(draftOrder);
        ServiceException ex3 = assertThrows(ServiceException.class, () -> progressFeedbackService.createProgressFeedback(reqVO));
        assertEquals(PROGRESS_FEEDBACK_DYEING_ORDER_STATUS_NOT_ALLOWED.getCode(), ex3.getCode());

        // 单据状态为 CANCELLED
        DyeingOrderDO cancelledOrder = createOrder(1L, "CANCELLED", false, FIXED_TODAY.plusDays(5));
        when(dyeingOrderMapper.selectById(1L)).thenReturn(cancelledOrder);
        ServiceException ex4 = assertThrows(ServiceException.class, () -> progressFeedbackService.createProgressFeedback(reqVO));
        assertEquals(PROGRESS_FEEDBACK_DYEING_ORDER_STATUS_NOT_ALLOWED.getCode(), ex4.getCode());

        verify(progressFeedbackMapper, never()).insert(any(ProgressFeedbackDO.class));
    }

    @Test
    @DisplayName("建缸校验：颜色行不存在或不归属该单抛出 003")
    void createProgressFeedback_colorNotFoundOrNotBelongToOrder_throws003() {
        DyeingOrderDO order = createOrder(1L, "ACTIVE", false, FIXED_TODAY.plusDays(5));
        when(dyeingOrderMapper.selectById(1L)).thenReturn(order);

        ProgressFeedbackCreateReqVO reqVO = ProgressFeedbackCreateReqVO.builder()
                .dyeingOrderId(1L)
                .dyeingColorId(101L)
                .vatNo("V01")
                .quantity(new BigDecimal("100.00"))
                .build();

        // 颜色行不存在
        when(dyeingColorMapper.selectById(101L)).thenReturn(null);
        ServiceException ex1 = assertThrows(ServiceException.class, () -> progressFeedbackService.createProgressFeedback(reqVO));
        assertEquals(PROGRESS_FEEDBACK_DYEING_COLOR_NOT_EXISTS.getCode(), ex1.getCode());

        // 颜色行软删
        DyeingColorDO deletedColor = createColor(101L, 1L, 1, "黑色", new BigDecimal("100"), 2, "KG");
        deletedColor.setDeleted(true);
        when(dyeingColorMapper.selectById(101L)).thenReturn(deletedColor);
        ServiceException ex2 = assertThrows(ServiceException.class, () -> progressFeedbackService.createProgressFeedback(reqVO));
        assertEquals(PROGRESS_FEEDBACK_DYEING_COLOR_NOT_EXISTS.getCode(), ex2.getCode());

        // 颜色行归属另外一个染色单 (orderId=2L)
        DyeingColorDO otherOrderColor = createColor(101L, 2L, 1, "黑色", new BigDecimal("100"), 2, "KG");
        when(dyeingColorMapper.selectById(101L)).thenReturn(otherOrderColor);
        ServiceException ex3 = assertThrows(ServiceException.class, () -> progressFeedbackService.createProgressFeedback(reqVO));
        assertEquals(PROGRESS_FEEDBACK_DYEING_COLOR_NOT_EXISTS.getCode(), ex3.getCode());

        verify(progressFeedbackMapper, never()).insert(any(ProgressFeedbackDO.class));
    }

    @Test
    @DisplayName("建缸校验：数量为空或小于等于0时拦截")
    void createProgressFeedback_quantityInvalid_throwsException() {
        ProgressFeedbackCreateReqVO nullQtyReq = ProgressFeedbackCreateReqVO.builder()
                .dyeingOrderId(1L)
                .dyeingColorId(101L)
                .vatNo("V01")
                .quantity(null)
                .build();
        assertThrows(ServiceException.class, () -> progressFeedbackService.createProgressFeedback(nullQtyReq));

        ProgressFeedbackCreateReqVO zeroQtyReq = ProgressFeedbackCreateReqVO.builder()
                .dyeingOrderId(1L)
                .dyeingColorId(101L)
                .vatNo("V01")
                .quantity(BigDecimal.ZERO)
                .build();
        assertThrows(ServiceException.class, () -> progressFeedbackService.createProgressFeedback(zeroQtyReq));

        ProgressFeedbackCreateReqVO negativeQtyReq = ProgressFeedbackCreateReqVO.builder()
                .dyeingOrderId(1L)
                .dyeingColorId(101L)
                .vatNo("V01")
                .quantity(new BigDecimal("-10.00"))
                .build();
        assertThrows(ServiceException.class, () -> progressFeedbackService.createProgressFeedback(negativeQtyReq));

        verify(progressFeedbackMapper, never()).insert(any(ProgressFeedbackDO.class));
    }

    // ==================== 9. 工序推进逻辑（04 票） ====================

    @Test
    @DisplayName("工序推进：SCHEDULED -> DYEING，盖染色开始日期戳")
    void advanceProgressFeedback_scheduledToDyeing_success() {
        ProgressFeedbackDO vat = createVat(1001L, 1L, 101L, "V01", new BigDecimal("500.00"), 10, "SCHEDULED");
        when(progressFeedbackMapper.selectById(1001L)).thenReturn(vat);

        ProgressFeedbackAdvanceReqVO reqVO = ProgressFeedbackAdvanceReqVO.builder()
                .id(1001L)
                .targetStatus("DYEING")
                .build();

        progressFeedbackService.advanceProgressFeedback(reqVO);

        ArgumentCaptor<ProgressFeedbackDO> captor = ArgumentCaptor.forClass(ProgressFeedbackDO.class);
        verify(progressFeedbackMapper).updateById(captor.capture());
        ProgressFeedbackDO updated = captor.getValue();

        assertEquals(ProgressFeedbackStatusEnum.DYEING.getValue(), updated.getStatus());
        assertEquals(FIXED_TODAY, updated.getDyeingStartedDate(), "推进到染色中应自动盖当天日期戳");
    }

    @Test
    @DisplayName("工序推进：DYEING -> FIXING，盖固色开始日期戳")
    void advanceProgressFeedback_dyeingToFixing_success() {
        ProgressFeedbackDO vat = createVat(1001L, 1L, 101L, "V01", new BigDecimal("500.00"), 10, "DYEING");
        vat.setDyeingStartedDate(FIXED_TODAY.minusDays(2));
        when(progressFeedbackMapper.selectById(1001L)).thenReturn(vat);

        ProgressFeedbackAdvanceReqVO reqVO = ProgressFeedbackAdvanceReqVO.builder()
                .id(1001L)
                .build(); // targetStatus 省略时自动推进到下一格

        progressFeedbackService.advanceProgressFeedback(reqVO);

        ArgumentCaptor<ProgressFeedbackDO> captor = ArgumentCaptor.forClass(ProgressFeedbackDO.class);
        verify(progressFeedbackMapper).updateById(captor.capture());
        ProgressFeedbackDO updated = captor.getValue();

        assertEquals(ProgressFeedbackStatusEnum.FIXING.getValue(), updated.getStatus());
        assertEquals(FIXED_TODAY, updated.getFixingStartedDate(), "推进到固色中应自动盖当天日期戳");
        assertEquals(FIXED_TODAY.minusDays(2), updated.getDyeingStartedDate(), "原有工序日期不受影响");
    }

    @Test
    @DisplayName("工序推进：FIXING -> QUALITY_CHECK，盖质检日期戳")
    void advanceProgressFeedback_fixingToQualityCheck_success() {
        ProgressFeedbackDO vat = createVat(1001L, 1L, 101L, "V01", new BigDecimal("500.00"), 10, "FIXING");
        when(progressFeedbackMapper.selectById(1001L)).thenReturn(vat);

        ProgressFeedbackAdvanceReqVO reqVO = ProgressFeedbackAdvanceReqVO.builder()
                .id(1001L)
                .targetStatus("QUALITY_CHECK")
                .build();

        progressFeedbackService.advanceProgressFeedback(reqVO);

        ArgumentCaptor<ProgressFeedbackDO> captor = ArgumentCaptor.forClass(ProgressFeedbackDO.class);
        verify(progressFeedbackMapper).updateById(captor.capture());
        ProgressFeedbackDO updated = captor.getValue();

        assertEquals(ProgressFeedbackStatusEnum.QUALITY_CHECK.getValue(), updated.getStatus());
        assertEquals(FIXED_TODAY, updated.getQualityCheckDate(), "推进到质检中应自动盖当天日期戳");
    }

    @Test
    @DisplayName("工序推进：QUALITY_CHECK -> COMPLETED，盖完成日期戳")
    void advanceProgressFeedback_qualityCheckToCompleted_success() {
        ProgressFeedbackDO vat = createVat(1001L, 1L, 101L, "V01", new BigDecimal("500.00"), 10, "QUALITY_CHECK");
        when(progressFeedbackMapper.selectById(1001L)).thenReturn(vat);

        ProgressFeedbackAdvanceReqVO reqVO = ProgressFeedbackAdvanceReqVO.builder()
                .id(1001L)
                .targetStatus("COMPLETED")
                .build();

        progressFeedbackService.advanceProgressFeedback(reqVO);

        ArgumentCaptor<ProgressFeedbackDO> captor = ArgumentCaptor.forClass(ProgressFeedbackDO.class);
        verify(progressFeedbackMapper).updateById(captor.capture());
        ProgressFeedbackDO updated = captor.getValue();

        assertEquals(ProgressFeedbackStatusEnum.COMPLETED.getValue(), updated.getStatus());
        assertEquals(FIXED_TODAY, updated.getCompletedDate(), "推进到已完成应自动盖当天日期戳");
    }

    @Test
    @DisplayName("工序推进校验：跳格推进被拒（抛出 005）")
    void advanceProgressFeedback_skipStep_throws005() {
        ProgressFeedbackDO vat = createVat(1001L, 1L, 101L, "V01", new BigDecimal("500.00"), 10, "SCHEDULED");
        when(progressFeedbackMapper.selectById(1001L)).thenReturn(vat);

        // 试图从 SCHEDULED 直接跳到 FIXING
        ProgressFeedbackAdvanceReqVO reqVO = ProgressFeedbackAdvanceReqVO.builder()
                .id(1001L)
                .targetStatus("FIXING")
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> progressFeedbackService.advanceProgressFeedback(reqVO));
        assertEquals(PROGRESS_FEEDBACK_STATUS_ADVANCE_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("工序推进校验：COMPLETED 缸无法再推进（抛出 005）")
    void advanceProgressFeedback_fromCompleted_throws005() {
        ProgressFeedbackDO vat = createVat(1001L, 1L, 101L, "V01", new BigDecimal("500.00"), 10, "COMPLETED");
        when(progressFeedbackMapper.selectById(1001L)).thenReturn(vat);

        ProgressFeedbackAdvanceReqVO reqVO = ProgressFeedbackAdvanceReqVO.builder()
                .id(1001L)
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> progressFeedbackService.advanceProgressFeedback(reqVO));
        assertEquals(PROGRESS_FEEDBACK_STATUS_ADVANCE_NOT_ALLOWED.getCode(), ex.getCode());
    }

    // ==================== 10. 缸详情编辑与回退（04 票） ====================

    @Test
    @DisplayName("编辑缸：修改五个阶段日期与备注延期原因生效")
    void updateProgressFeedback_modifyDatesAndRemark_success() {
        ProgressFeedbackDO vat = createVat(1001L, 1L, 101L, "V01", new BigDecimal("500.00"), 10, "DYEING");
        when(progressFeedbackMapper.selectById(1001L)).thenReturn(vat);

        LocalDate d1 = LocalDate.of(2026, 9, 20);
        LocalDate d2 = LocalDate.of(2026, 9, 21);
        LocalDate d3 = LocalDate.of(2026, 9, 22);
        LocalDate d4 = LocalDate.of(2026, 9, 23);
        LocalDate d5 = LocalDate.of(2026, 9, 24);

        ProgressFeedbackUpdateReqVO reqVO = ProgressFeedbackUpdateReqVO.builder()
                .id(1001L)
                .vatNo("V01")
                .quantity(new BigDecimal("520.00"))
                .pieces(11)
                .status("DYEING")
                .scheduledDate(d1)
                .dyeingStartedDate(d2)
                .fixingStartedDate(d3)
                .qualityCheckDate(d4)
                .completedDate(d5)
                .delayReason("设备修缮")
                .remark("人工校准")
                .build();

        progressFeedbackService.updateProgressFeedback(reqVO);

        ArgumentCaptor<ProgressFeedbackDO> captor = ArgumentCaptor.forClass(ProgressFeedbackDO.class);
        verify(progressFeedbackMapper).updateById(captor.capture());
        ProgressFeedbackDO updated = captor.getValue();

        assertEquals(new BigDecimal("520.00"), updated.getQuantity());
        assertEquals(11, updated.getPieces());
        assertEquals(d1, updated.getScheduledDate());
        assertEquals(d2, updated.getDyeingStartedDate());
        assertEquals(d3, updated.getFixingStartedDate());
        assertEquals(d4, updated.getQualityCheckDate());
        assertEquals(d5, updated.getCompletedDate());
        assertEquals("设备修缮", updated.getDelayReason());
        assertEquals("人工校准", updated.getRemark());
    }

    @Test
    @DisplayName("编辑缸：COMPLETED 终态缸仍可修正日期与备注（不设严格终态锁）")
    void updateProgressFeedback_modifyCompletedVatDatesAndRemark_success() {
        ProgressFeedbackDO vat = createVat(1001L, 1L, 101L, "V01", new BigDecimal("500.00"), 10, "COMPLETED");
        vat.setCompletedDate(FIXED_TODAY.minusDays(1));
        when(progressFeedbackMapper.selectById(1001L)).thenReturn(vat);

        LocalDate newCompletedDate = FIXED_TODAY;
        ProgressFeedbackUpdateReqVO reqVO = ProgressFeedbackUpdateReqVO.builder()
                .id(1001L)
                .vatNo("V01")
                .quantity(new BigDecimal("500.00"))
                .completedDate(newCompletedDate)
                .remark("补录实际出库完成日")
                .build();

        progressFeedbackService.updateProgressFeedback(reqVO);

        ArgumentCaptor<ProgressFeedbackDO> captor = ArgumentCaptor.forClass(ProgressFeedbackDO.class);
        verify(progressFeedbackMapper).updateById(captor.capture());
        ProgressFeedbackDO updated = captor.getValue();

        assertEquals(newCompletedDate, updated.getCompletedDate());
        assertEquals("补录实际出库完成日", updated.getRemark());
        assertEquals(ProgressFeedbackStatusEnum.COMPLETED.getValue(), updated.getStatus());
    }

    @Test
    @DisplayName("编辑缸状态回退：允许回退到上一工序且不强制清除已有日期")
    void updateProgressFeedback_rollbackOneStep_success() {
        ProgressFeedbackDO vat = createVat(1001L, 1L, 101L, "V01", new BigDecimal("500.00"), 10, "COMPLETED");
        vat.setScheduledDate(FIXED_TODAY.minusDays(4));
        vat.setDyeingStartedDate(FIXED_TODAY.minusDays(3));
        vat.setFixingStartedDate(FIXED_TODAY.minusDays(2));
        vat.setQualityCheckDate(FIXED_TODAY.minusDays(1));
        vat.setCompletedDate(FIXED_TODAY);
        when(progressFeedbackMapper.selectById(1001L)).thenReturn(vat);

        // 回退一格：COMPLETED -> QUALITY_CHECK
        ProgressFeedbackUpdateReqVO reqVO = ProgressFeedbackUpdateReqVO.builder()
                .id(1001L)
                .vatNo("V01")
                .quantity(new BigDecimal("500.00"))
                .status("QUALITY_CHECK")
                .scheduledDate(vat.getScheduledDate())
                .dyeingStartedDate(vat.getDyeingStartedDate())
                .fixingStartedDate(vat.getFixingStartedDate())
                .qualityCheckDate(vat.getQualityCheckDate())
                .completedDate(vat.getCompletedDate()) // 回退不强制清日期，日期以传入为准
                .build();

        progressFeedbackService.updateProgressFeedback(reqVO);

        ArgumentCaptor<ProgressFeedbackDO> captor = ArgumentCaptor.forClass(ProgressFeedbackDO.class);
        verify(progressFeedbackMapper).updateById(captor.capture());
        ProgressFeedbackDO updated = captor.getValue();

        assertEquals(ProgressFeedbackStatusEnum.QUALITY_CHECK.getValue(), updated.getStatus(), "状态成功回退到质检中");
        assertEquals(FIXED_TODAY, updated.getCompletedDate(), "回退不强制清除已盖日期戳");
    }

    @Test
    @DisplayName("编辑缸状态回退校验：已排缸无法再回退（抛出 006）")
    void updateProgressFeedback_rollbackScheduled_throws006() {
        ProgressFeedbackDO vat = createVat(1001L, 1L, 101L, "V01", new BigDecimal("500.00"), 10, "SCHEDULED");
        when(progressFeedbackMapper.selectById(1001L)).thenReturn(vat);

        ProgressFeedbackUpdateReqVO reqVO = ProgressFeedbackUpdateReqVO.builder()
                .id(1001L)
                .vatNo("V01")
                .quantity(new BigDecimal("500.00"))
                .status("NOT_SCHEDULED")
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> progressFeedbackService.updateProgressFeedback(reqVO));
        assertEquals(PROGRESS_FEEDBACK_STATUS_ROLLBACK_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("编辑缸状态回退校验：跳级回退被拒（抛出 006）")
    void updateProgressFeedback_rollbackSkipStep_throws006() {
        ProgressFeedbackDO vat = createVat(1001L, 1L, 101L, "V01", new BigDecimal("500.00"), 10, "COMPLETED");
        when(progressFeedbackMapper.selectById(1001L)).thenReturn(vat);

        // 试图从 COMPLETED 直接回退到 DYEING
        ProgressFeedbackUpdateReqVO reqVO = ProgressFeedbackUpdateReqVO.builder()
                .id(1001L)
                .vatNo("V01")
                .quantity(new BigDecimal("500.00"))
                .status("DYEING")
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> progressFeedbackService.updateProgressFeedback(reqVO));
        assertEquals(PROGRESS_FEEDBACK_STATUS_ROLLBACK_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("编辑缸校验：改缸号与同单同色其它缸撞重抛出 001")
    void updateProgressFeedback_vatNoDuplicate_throws001() {
        ProgressFeedbackDO vat = createVat(1001L, 1L, 101L, "V01", new BigDecimal("500.00"), 10, "SCHEDULED");
        when(progressFeedbackMapper.selectById(1001L)).thenReturn(vat);

        // 存在另一张缸 V02 (id=1002L)
        ProgressFeedbackDO anotherVat = createVat(1002L, 1L, 101L, "V02", new BigDecimal("300.00"), 6, "SCHEDULED");
        when(progressFeedbackMapper.selectByColorIdAndVatNo(101L, "V02")).thenReturn(anotherVat);

        ProgressFeedbackUpdateReqVO reqVO = ProgressFeedbackUpdateReqVO.builder()
                .id(1001L)
                .vatNo("V02")
                .quantity(new BigDecimal("500.00"))
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> progressFeedbackService.updateProgressFeedback(reqVO));
        assertEquals(PROGRESS_FEEDBACK_VAT_NO_DUPLICATE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("编辑缸：保留相同缸号放行通过")
    void updateProgressFeedback_sameVatNo_success() {
        ProgressFeedbackDO vat = createVat(1001L, 1L, 101L, "V01", new BigDecimal("500.00"), 10, "SCHEDULED");
        when(progressFeedbackMapper.selectById(1001L)).thenReturn(vat);
        when(progressFeedbackMapper.selectByColorIdAndVatNo(101L, "V01")).thenReturn(vat);

        ProgressFeedbackUpdateReqVO reqVO = ProgressFeedbackUpdateReqVO.builder()
                .id(1001L)
                .vatNo("V01")
                .quantity(new BigDecimal("600.00"))
                .build();

        progressFeedbackService.updateProgressFeedback(reqVO);

        ArgumentCaptor<ProgressFeedbackDO> captor = ArgumentCaptor.forClass(ProgressFeedbackDO.class);
        verify(progressFeedbackMapper).updateById(captor.capture());
        assertEquals(new BigDecimal("600.00"), captor.getValue().getQuantity());
    }

    // ==================== 11. 缸详情查询（04 票） ====================

    @Test
    @DisplayName("缸详情查询：包含缸字段、五阶段日期、单据/颜色行实时信息与延期/覆盖率")
    void getProgressFeedback_detail_returnsOrderColorAndDerivedFields() {
        DyeingOrderDO order = createOrder(1L, "ACTIVE", false, FIXED_TODAY.minusDays(1)); // 已过期
        order.setContractNo("HT-888");
        order.setClientName("东方纺织");
        order.setDyeingFactoryName("第一染厂");
        when(dyeingOrderMapper.selectById(1L)).thenReturn(order);

        DyeingColorDO color = createColor(101L, 1L, 1, "湖蓝", new BigDecimal("1000.00"), 40, "KG");
        color.setCustomerColorNo("PAN-LAKE-BLUE");
        when(dyeingColorMapper.selectById(101L)).thenReturn(color);

        ProgressFeedbackDO vat = createVat(1001L, 1L, 101L, "VAT-01", new BigDecimal("600.00"), 24, "DYEING");
        vat.setScheduledDate(FIXED_TODAY.minusDays(5));
        vat.setDyeingStartedDate(FIXED_TODAY.minusDays(3));
        vat.setDelayReason("待催化助剂");
        vat.setRemark("重点跟踪");
        when(progressFeedbackMapper.selectById(1001L)).thenReturn(vat);

        // 同颜色行下所有缸（计算总数量与覆盖率）
        ProgressFeedbackDO vat2 = createVat(1002L, 1L, 101L, "VAT-02", new BigDecimal("200.00"), 8, "SCHEDULED");
        when(progressFeedbackMapper.selectListByColorIds(any())).thenReturn(List.of(vat, vat2));

        // 未决问题
        ProgressFeedbackNoteDO unresolvedNote = new ProgressFeedbackNoteDO();
        unresolvedNote.setId(901L);
        unresolvedNote.setFeedbackId(1001L);
        unresolvedNote.setIssueFlag(true);
        unresolvedNote.setResolvedAt(null);
        unresolvedNote.setDeleted(false);
        when(progressFeedbackNoteMapper.selectUnresolvedListByFeedbackIds(any())).thenReturn(List.of(unresolvedNote));

        ProgressFeedbackDetailRespVO detail = progressFeedbackService.getProgressFeedback(1001L);

        // 缸字段
        assertEquals(1001L, detail.getId());
        assertEquals("VAT-01", detail.getVatNo());
        assertEquals(new BigDecimal("600.00"), detail.getQuantity());
        assertEquals(24, detail.getPieces());
        assertEquals("DYEING", detail.getStatus());
        assertEquals(FIXED_TODAY.minusDays(5), detail.getScheduledDate());
        assertEquals(FIXED_TODAY.minusDays(3), detail.getDyeingStartedDate());
        assertEquals("待催化助剂", detail.getDelayReason());
        assertEquals("重点跟踪", detail.getRemark());

        // 实时信息（零快照）
        assertEquals("HT-888", detail.getContractNo());
        assertEquals("东方纺织", detail.getClientName());
        assertEquals("第一染厂", detail.getDyeingFactoryName());
        assertEquals(order.getDeliveryDate(), detail.getDeliveryDate());
        assertEquals("湖蓝", detail.getColorName());
        assertEquals("PAN-LAKE-BLUE", detail.getCustomerColorNo());
        assertEquals(new BigDecimal("1000.00"), detail.getColorQuantity());
        assertEquals("KG", detail.getUnit());

        // 派生指标
        assertTrue(detail.getDelayed(), "交期已过且未完成应延期");
        assertEquals(1, detail.getUnresolvedIssueCount());
        assertEquals(new BigDecimal("800.00"), detail.getTotalVatQuantity());
        assertEquals(32, detail.getTotalVatPieces());
        assertEquals(new BigDecimal("80.00"), detail.getCoveragePercent());
        assertFalse(detail.getOverScheduled());
    }

    // ==================== 10. 删除反馈缸（任意状态可删 + 级联软删记录） ====================

    @Test
    @DisplayName("删除排缸态反馈缸成功：级联软删反馈记录")
    void testDeleteProgressFeedback_scheduledVat_success() {
        Long vatId = 1001L;
        ProgressFeedbackDO vat = createVat(vatId, 1L, 101L, "VAT-01", new BigDecimal("500.00"), 20, "SCHEDULED");
        vat.setDeleted(false);
        when(progressFeedbackMapper.selectById(vatId)).thenReturn(vat);

        progressFeedbackService.deleteProgressFeedback(vatId);

        // 验证级联软删反馈记录与删除缸自身
        verify(progressFeedbackNoteMapper).delete(any(LambdaQueryWrapperX.class));
        verify(progressFeedbackMapper).deleteById(vatId);
    }

    @Test
    @DisplayName("删除完成态反馈缸成功：任意工序状态均可删除")
    void testDeleteProgressFeedback_completedVat_success() {
        Long vatId = 1002L;
        ProgressFeedbackDO vat = createVat(vatId, 1L, 101L, "VAT-02", new BigDecimal("300.00"), 12, "COMPLETED");
        vat.setDeleted(false);
        when(progressFeedbackMapper.selectById(vatId)).thenReturn(vat);

        progressFeedbackService.deleteProgressFeedback(vatId);

        verify(progressFeedbackNoteMapper).delete(any(LambdaQueryWrapperX.class));
        verify(progressFeedbackMapper).deleteById(vatId);
    }

    @Test
    @DisplayName("删除不存在的反馈缸：抛 1-030-017-000 (PROGRESS_FEEDBACK_VAT_NOT_EXISTS)")
    void testDeleteProgressFeedback_notExists_throws000() {
        Long vatId = 9999L;
        when(progressFeedbackMapper.selectById(vatId)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> progressFeedbackService.deleteProgressFeedback(vatId));
        assertEquals(PROGRESS_FEEDBACK_VAT_NOT_EXISTS.getCode(), ex.getCode());

        verify(progressFeedbackNoteMapper, never()).delete(any(LambdaQueryWrapperX.class));
        verify(progressFeedbackMapper, never()).deleteById(any());
    }

    // ==================== 12. 07 票：增强筛选与预警视图 ====================

    @Test
    @DisplayName("仅看未决问题：仅返回含未闭环问题记录反馈缸的颜色行节")
    void getProgressFeedbackPage_unresolvedOnlyFilter() {
        DyeingOrderDO order = createOrder(1L, "ACTIVE", false, FIXED_TODAY.plusDays(5));
        when(dyeingOrderMapper.selectList(any())).thenReturn(List.of(order));

        DyeingColorDO c1 = createColor(101L, 1L, 1, "有未决问题", new BigDecimal("500"), 20, "KG");
        DyeingColorDO c2 = createColor(102L, 1L, 2, "已闭环问题", new BigDecimal("500"), 20, "KG");
        DyeingColorDO c3 = createColor(103L, 1L, 3, "无问题正常缸", new BigDecimal("500"), 20, "KG");
        when(dyeingColorMapper.selectList(any())).thenReturn(List.of(c1, c2, c3));

        ProgressFeedbackDO vat1 = createVat(1001L, 1L, 101L, "V01", new BigDecimal("500"), 20, "DYEING");
        ProgressFeedbackDO vat2 = createVat(1002L, 1L, 102L, "V02", new BigDecimal("500"), 20, "DYEING");
        ProgressFeedbackDO vat3 = createVat(1003L, 1L, 103L, "V03", new BigDecimal("500"), 20, "DYEING");
        when(progressFeedbackMapper.selectListByColorIds(any())).thenReturn(List.of(vat1, vat2, vat3));

        // 仅 vat1 有未闭环问题
        ProgressFeedbackNoteDO unresolvedNote = new ProgressFeedbackNoteDO();
        unresolvedNote.setId(1L);
        unresolvedNote.setFeedbackId(1001L);
        unresolvedNote.setIssueFlag(true);
        unresolvedNote.setResolvedAt(null);
        unresolvedNote.setDeleted(false);

        when(progressFeedbackNoteMapper.selectUnresolvedListByFeedbackIds(any())).thenReturn(List.of(unresolvedNote));

        ProgressFeedbackPageReqVO reqVO = new ProgressFeedbackPageReqVO();
        reqVO.setUnresolvedOnly(true);

        PageResult<ProgressFeedbackColorSectionRespVO> result = progressFeedbackService.getProgressFeedbackPage(reqVO);

        assertEquals(1, result.getTotal());
        assertEquals(1, result.getList().size());
        assertEquals(101L, result.getList().get(0).getDyeingColorId(), "仅含未闭环问题的颜色行节应被筛出");
    }

    @Test
    @DisplayName("含已完成开关：默认隐藏全缸 COMPLETED 节，开启后显示全量，部分完成节不受影响")
    void getProgressFeedbackPage_includeCompletedFilter() {
        DyeingOrderDO order = createOrder(1L, "ACTIVE", false, FIXED_TODAY.plusDays(5));
        when(dyeingOrderMapper.selectList(any())).thenReturn(List.of(order));

        DyeingColorDO cAllCompleted = createColor(101L, 1L, 1, "全完成色", new BigDecimal("500"), 20, "KG");
        DyeingColorDO cPartialCompleted = createColor(102L, 1L, 2, "部分完成色", new BigDecimal("600"), 25, "KG");
        DyeingColorDO cZeroVat = createColor(103L, 1L, 3, "未排缸色", new BigDecimal("300"), 12, "KG");
        when(dyeingColorMapper.selectList(any())).thenReturn(List.of(cAllCompleted, cPartialCompleted, cZeroVat));

        ProgressFeedbackDO vat1 = createVat(1001L, 1L, 101L, "V01", new BigDecimal("500"), 20, "COMPLETED");
        ProgressFeedbackDO vat2 = createVat(1002L, 1L, 102L, "V02", new BigDecimal("300"), 12, "COMPLETED");
        ProgressFeedbackDO vat3 = createVat(1003L, 1L, 102L, "V03", new BigDecimal("300"), 13, "DYEING");
        when(progressFeedbackMapper.selectListByColorIds(any())).thenReturn(List.of(vat1, vat2, vat3));

        // 1. 默认 includeCompleted=false：全完成节被隐藏，部分完成节与未排缸节保留
        ProgressFeedbackPageReqVO defaultReq = new ProgressFeedbackPageReqVO();
        defaultReq.setIncludeCompleted(false);
        PageResult<ProgressFeedbackColorSectionRespVO> defaultResult = progressFeedbackService.getProgressFeedbackPage(defaultReq);
        assertEquals(2, defaultResult.getTotal());
        assertEquals(List.of(102L, 103L), defaultResult.getList().stream().map(ProgressFeedbackColorSectionRespVO::getDyeingColorId).toList());

        // 2. 开启 includeCompleted=true：全完成节显示
        ProgressFeedbackPageReqVO includeReq = new ProgressFeedbackPageReqVO();
        includeReq.setIncludeCompleted(true);
        PageResult<ProgressFeedbackColorSectionRespVO> includeResult = progressFeedbackService.getProgressFeedbackPage(includeReq);
        assertEquals(3, includeResult.getTotal());
        assertEquals(List.of(101L, 102L, 103L), includeResult.getList().stream().map(ProgressFeedbackColorSectionRespVO::getDyeingColorId).toList());

        // 3. 当筛选 status=COMPLETED 时：即使 includeCompleted=false 也应能查出全完成节
        ProgressFeedbackPageReqVO statusCompletedReq = new ProgressFeedbackPageReqVO();
        statusCompletedReq.setStatus("COMPLETED");
        statusCompletedReq.setIncludeCompleted(false);
        PageResult<ProgressFeedbackColorSectionRespVO> statusCompletedResult = progressFeedbackService.getProgressFeedbackPage(statusCompletedReq);
        assertEquals(2, statusCompletedResult.getTotal());
        assertEquals(List.of(101L, 102L), statusCompletedResult.getList().stream().map(ProgressFeedbackColorSectionRespVO::getDyeingColorId).toList());
    }

    @Test
    @DisplayName("含已失效开关：软删单残留缸默认隐藏，开启后可查")
    void getProgressFeedbackPage_includeDeletedFilter() {
        DyeingOrderDO activeOrder = createOrder(1L, "ACTIVE", false, FIXED_TODAY.plusDays(5));
        DyeingOrderDO deletedOrder = createOrder(2L, "ACTIVE", true, FIXED_TODAY.plusDays(5));

        DyeingColorDO c1 = createColor(101L, 1L, 1, "正常单颜色", new BigDecimal("500"), 20, "KG");
        DyeingColorDO c2 = createColor(102L, 2L, 1, "软删单残留颜色", new BigDecimal("500"), 20, "KG");

        when(dyeingColorMapper.selectList(any())).thenReturn(List.of(c1, c2));

        ProgressFeedbackDO vat1 = createVat(1001L, 1L, 101L, "V01", new BigDecimal("500"), 20, "SCHEDULED");
        ProgressFeedbackDO vat2 = createVat(1002L, 2L, 102L, "V02", new BigDecimal("500"), 20, "SCHEDULED");
        when(progressFeedbackMapper.selectListByColorIds(any())).thenReturn(List.of(vat1, vat2));

        // 默认 includeDeleted=false：软删单不纳入 eligibleOrders
        when(dyeingOrderMapper.selectList(any())).thenReturn(List.of(activeOrder));
        ProgressFeedbackPageReqVO defaultReq = new ProgressFeedbackPageReqVO();
        defaultReq.setIncludeDeleted(false);
        PageResult<ProgressFeedbackColorSectionRespVO> defaultResult = progressFeedbackService.getProgressFeedbackPage(defaultReq);
        assertEquals(1, defaultResult.getTotal());
        assertEquals(101L, defaultResult.getList().get(0).getDyeingColorId());

        // 开启 includeDeleted=true：软删单纳入 eligibleOrders
        when(dyeingOrderMapper.selectListIncludingDeleted(any(), any(), any())).thenReturn(List.of(activeOrder, deletedOrder));
        when(dyeingColorMapper.selectListByOrderIdsIncludingDeleted(any())).thenReturn(List.of(c1, c2));
        ProgressFeedbackPageReqVO includeReq = new ProgressFeedbackPageReqVO();
        includeReq.setIncludeDeleted(true);
        PageResult<ProgressFeedbackColorSectionRespVO> includeResult = progressFeedbackService.getProgressFeedbackPage(includeReq);
        assertEquals(2, includeResult.getTotal());
    }

    @Test
    @DisplayName("关键词搜索：支持颜色名称、客户色号、缸号不区分大小写模糊匹配")
    void getProgressFeedbackPage_keywordFilter_matchesAllFields() {
        DyeingOrderDO order = createOrder(1L, "ACTIVE", false, FIXED_TODAY.plusDays(5));
        when(dyeingOrderMapper.selectList(any())).thenReturn(List.of(order));

        DyeingColorDO c1 = createColor(101L, 1L, 1, "藏青深蓝", new BigDecimal("500"), 20, "KG");
        c1.setCustomerColorNo("PANTONE 19-4052");
        DyeingColorDO c2 = createColor(102L, 1L, 2, "象牙白", new BigDecimal("500"), 20, "KG");
        c2.setCustomerColorNo("PAN-IVORY");
        when(dyeingColorMapper.selectList(any())).thenReturn(List.of(c1, c2));

        ProgressFeedbackDO vat1 = createVat(1001L, 1L, 101L, "VAT-NAVY-01", new BigDecimal("500"), 20, "SCHEDULED");
        ProgressFeedbackDO vat2 = createVat(1002L, 1L, 102L, "VAT-WHITE-01", new BigDecimal("500"), 20, "SCHEDULED");
        when(progressFeedbackMapper.selectListByColorIds(any())).thenReturn(List.of(vat1, vat2));

        // 匹配颜色名称 "深蓝"
        ProgressFeedbackPageReqVO kwColor = new ProgressFeedbackPageReqVO();
        kwColor.setKeyword("深蓝");
        PageResult<ProgressFeedbackColorSectionRespVO> resColor = progressFeedbackService.getProgressFeedbackPage(kwColor);
        assertEquals(1, resColor.getTotal());
        assertEquals(101L, resColor.getList().get(0).getDyeingColorId());

        // 匹配客户色号小写 "pantone"
        ProgressFeedbackPageReqVO kwPantone = new ProgressFeedbackPageReqVO();
        kwPantone.setKeyword("pantone");
        PageResult<ProgressFeedbackColorSectionRespVO> resPantone = progressFeedbackService.getProgressFeedbackPage(kwPantone);
        assertEquals(1, resPantone.getTotal());
        assertEquals(101L, resPantone.getList().get(0).getDyeingColorId());

        // 匹配缸号 "white"
        ProgressFeedbackPageReqVO kwVat = new ProgressFeedbackPageReqVO();
        kwVat.setKeyword("white");
        PageResult<ProgressFeedbackColorSectionRespVO> resVat = progressFeedbackService.getProgressFeedbackPage(kwVat);
        assertEquals(1, resVat.getTotal());
        assertEquals(102L, resVat.getList().get(0).getDyeingColorId());
    }

    @Test
    @DisplayName("统计条增强：包含未决问题缸数且与关键词同口径一致")
    void getProgressFeedbackStatistics_withUnresolvedAndEnhancedFilters() {
        DyeingOrderDO order = createOrder(1L, "ACTIVE", false, FIXED_TODAY.minusDays(2)); // 已延期
        when(dyeingOrderMapper.selectList(any())).thenReturn(List.of(order));

        DyeingColorDO c1 = createColor(101L, 1L, 1, "藏青色", new BigDecimal("500"), 20, "KG");
        DyeingColorDO c2 = createColor(102L, 1L, 2, "象牙白", new BigDecimal("500"), 20, "KG");
        when(dyeingColorMapper.selectList(any())).thenReturn(List.of(c1, c2));

        ProgressFeedbackDO vat1 = createVat(1001L, 1L, 101L, "VAT-01", new BigDecimal("500"), 20, "DYEING"); // 延期 + 未决
        ProgressFeedbackDO vat2 = createVat(1002L, 1L, 102L, "VAT-02", new BigDecimal("500"), 20, "COMPLETED"); // 已完成，不延期
        when(progressFeedbackMapper.selectListByColorIds(any())).thenReturn(List.of(vat1, vat2));

        ProgressFeedbackNoteDO unresolvedNote = new ProgressFeedbackNoteDO();
        unresolvedNote.setId(1L);
        unresolvedNote.setFeedbackId(1001L);
        unresolvedNote.setIssueFlag(true);
        unresolvedNote.setResolvedAt(null);
        unresolvedNote.setDeleted(false);
        when(progressFeedbackNoteMapper.selectUnresolvedListByFeedbackIds(any())).thenReturn(List.of(unresolvedNote));

        // 查全量统计
        ProgressFeedbackPageReqVO reqVO = new ProgressFeedbackPageReqVO();
        ProgressFeedbackStatisticsRespVO stats = progressFeedbackService.getProgressFeedbackStatistics(reqVO);

        assertEquals(0L, stats.getUnscheduledColorCount());
        assertEquals(1L, stats.getInProgressVatCount());
        assertEquals(1L, stats.getCompletedVatCount());
        assertEquals(1L, stats.getDelayedVatCount());
        assertEquals(1L, stats.getUnresolvedVatCount(), "未决问题缸数应为 1");

        // 传入关键词 "象牙白"
        ProgressFeedbackPageReqVO kwReq = new ProgressFeedbackPageReqVO();
        kwReq.setKeyword("象牙白");
        ProgressFeedbackStatisticsRespVO kwStats = progressFeedbackService.getProgressFeedbackStatistics(kwReq);
        assertEquals(0L, kwStats.getUnscheduledColorCount());
        assertEquals(0L, kwStats.getInProgressVatCount());
        assertEquals(1L, kwStats.getCompletedVatCount());
        assertEquals(0L, kwStats.getDelayedVatCount());
        assertEquals(0L, kwStats.getUnresolvedVatCount(), "关键词过滤后未决问题缸数应同口径为 0");
    }

    @Test
    @DisplayName("人工回归用例：造延期缸、未闭环问题缸、已完成节、软删单残留缸四类数据逐一验证组合与统计联动")
    void regression_verifyAllFourScenarios_delayed_unresolved_completed_softDeleted() {
        // 场景数据造数：
        // 1. Order 1 (正常单，已延期交期)：下含延期缸
        DyeingOrderDO orderDelayed = createOrder(1L, "ACTIVE", false, FIXED_TODAY.minusDays(3));
        // 2. Order 2 (正常单，未延期交期)：下含未决问题缸
        DyeingOrderDO orderUnresolved = createOrder(2L, "ACTIVE", false, FIXED_TODAY.plusDays(5));
        // 3. Order 3 (正常单，未延期交期)：下含已完成节
        DyeingOrderDO orderCompleted = createOrder(3L, "ACTIVE", false, FIXED_TODAY.plusDays(10));
        // 4. Order 4 (软删除单)：残留缸
        DyeingOrderDO orderDeleted = createOrder(4L, "ACTIVE", true, FIXED_TODAY.plusDays(5));

        // 颜色行：
        DyeingColorDO colorDelayed = createColor(101L, 1L, 1, "延期颜色", new BigDecimal("500"), 20, "KG");
        DyeingColorDO colorUnresolved = createColor(102L, 2L, 1, "未决问题颜色", new BigDecimal("500"), 20, "KG");
        DyeingColorDO colorCompleted = createColor(103L, 3L, 1, "已完成颜色", new BigDecimal("500"), 20, "KG");
        DyeingColorDO colorDeletedOrder = createColor(104L, 4L, 1, "软删单残留颜色", new BigDecimal("500"), 20, "KG");

        // 缸数据：
        // 缸1：延期缸（DYEING 态，Order1 交期已过）
        ProgressFeedbackDO vatDelayed = createVat(1001L, 1L, 101L, "VAT-DELAYED", new BigDecimal("500"), 20, "DYEING");
        // 缸2：未决问题缸（SCHEDULED 态，Order2 交期未过，有未决问题记录）
        ProgressFeedbackDO vatUnresolved = createVat(1002L, 2L, 102L, "VAT-UNRESOLVED", new BigDecimal("500"), 20, "SCHEDULED");
        // 缸3：已完成缸（COMPLETED 态）
        ProgressFeedbackDO vatCompleted = createVat(1003L, 3L, 103L, "VAT-COMPLETED", new BigDecimal("500"), 20, "COMPLETED");
        // 缸4：软删单残留缸（SCHEDULED 态）
        ProgressFeedbackDO vatDeleted = createVat(1004L, 4L, 104L, "VAT-DELETED-RESIDUAL", new BigDecimal("500"), 20, "SCHEDULED");

        when(dyeingOrderMapper.selectList(any())).thenReturn(List.of(orderDelayed, orderUnresolved, orderCompleted));
        when(dyeingOrderMapper.selectListIncludingDeleted(any(), any(), any()))
                .thenReturn(List.of(orderDelayed, orderUnresolved, orderCompleted, orderDeleted));

        when(dyeingColorMapper.selectList(any())).thenReturn(List.of(colorDelayed, colorUnresolved, colorCompleted));
        when(dyeingColorMapper.selectListByOrderIdsIncludingDeleted(any()))
                .thenReturn(List.of(colorDelayed, colorUnresolved, colorCompleted, colorDeletedOrder));

        when(progressFeedbackMapper.selectListByColorIds(any()))
                .thenReturn(List.of(vatDelayed, vatUnresolved, vatCompleted, vatDeleted));

        ProgressFeedbackNoteDO unresolvedNote = new ProgressFeedbackNoteDO();
        unresolvedNote.setId(1L);
        unresolvedNote.setFeedbackId(1002L);
        unresolvedNote.setIssueFlag(true);
        unresolvedNote.setResolvedAt(null);
        unresolvedNote.setDeleted(false);
        when(progressFeedbackNoteMapper.selectUnresolvedListByFeedbackIds(any())).thenReturn(List.of(unresolvedNote));

        // 验证 1：默认进行中视图（includeCompleted=false, includeDeleted=false）
        ProgressFeedbackPageReqVO defaultReq = new ProgressFeedbackPageReqVO();
        PageResult<ProgressFeedbackColorSectionRespVO> defaultResult = progressFeedbackService.getProgressFeedbackPage(defaultReq);
        // 全完成节(103)隐藏，软删单节(104)隐藏，展示 101 和 102
        assertEquals(2, defaultResult.getTotal());
        assertEquals(List.of(101L, 102L), defaultResult.getList().stream().map(ProgressFeedbackColorSectionRespVO::getDyeingColorId).toList());

        // 验证 2：仅看延期（delayedOnly=true）-> 仅返回 101 节，且缸列表中仅含延期缸
        ProgressFeedbackPageReqVO delayedReq = new ProgressFeedbackPageReqVO();
        delayedReq.setDelayedOnly(true);
        PageResult<ProgressFeedbackColorSectionRespVO> delayedResult = progressFeedbackService.getProgressFeedbackPage(delayedReq);
        assertEquals(1, delayedResult.getTotal());
        assertEquals(101L, delayedResult.getList().get(0).getDyeingColorId());
        assertEquals(1, delayedResult.getList().get(0).getVats().size());
        assertTrue(Boolean.TRUE.equals(delayedResult.getList().get(0).getVats().get(0).getDelayed()));

        // 验证 3：仅看未决问题（unresolvedOnly=true）-> 仅返回 102 节，且缸列表中仅含未决问题缸
        ProgressFeedbackPageReqVO unresolvedReq = new ProgressFeedbackPageReqVO();
        unresolvedReq.setUnresolvedOnly(true);
        PageResult<ProgressFeedbackColorSectionRespVO> unresolvedResult = progressFeedbackService.getProgressFeedbackPage(unresolvedReq);
        assertEquals(1, unresolvedResult.getTotal());
        assertEquals(102L, unresolvedResult.getList().get(0).getDyeingColorId());
        assertEquals(1, unresolvedResult.getList().get(0).getVats().size());
        assertEquals(1, unresolvedResult.getList().get(0).getVats().get(0).getUnresolvedIssueCount());

        // 验证 4：开启含已完成（includeCompleted=true）-> 展现全完成节 103
        ProgressFeedbackPageReqVO incCompReq = new ProgressFeedbackPageReqVO();
        incCompReq.setIncludeCompleted(true);
        PageResult<ProgressFeedbackColorSectionRespVO> incCompResult = progressFeedbackService.getProgressFeedbackPage(incCompReq);
        assertEquals(3, incCompResult.getTotal());
        assertTrue(incCompResult.getList().stream().anyMatch(s -> s.getDyeingColorId().equals(103L)));

        // 验证 5：开启含已失效（includeDeleted=true）-> 展现软删单残留节 104
        ProgressFeedbackPageReqVO incDelReq = new ProgressFeedbackPageReqVO();
        incDelReq.setIncludeDeleted(true);
        PageResult<ProgressFeedbackColorSectionRespVO> incDelResult = progressFeedbackService.getProgressFeedbackPage(incDelReq);
        assertEquals(3, incDelResult.getTotal()); // 101, 102, 104 (103 因 includeCompleted=false 隐藏)
        assertTrue(incDelResult.getList().stream().anyMatch(s -> s.getDyeingColorId().equals(104L)));

        // 验证 6：统计条计数全口径联动校验
        ProgressFeedbackStatisticsRespVO stats = progressFeedbackService.getProgressFeedbackStatistics(defaultReq);
        assertEquals(0L, stats.getUnscheduledColorCount());
        assertEquals(2L, stats.getInProgressVatCount()); // vatDelayed + vatUnresolved
        assertEquals(1L, stats.getCompletedVatCount()); // vatCompleted
        assertEquals(1L, stats.getDelayedVatCount()); // vatDelayed
        assertEquals(1L, stats.getUnresolvedVatCount()); // vatUnresolved
    }

}


