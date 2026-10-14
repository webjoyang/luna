package com.luna.lunasrv.module.lunaerp.service.production.colorinstruction;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo.ColorInstructionPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo.ColorInstructionRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.colorinstruction.ColorInstructionDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.colorinstruction.color.ColorInstructionColorDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.colorinstruction.ColorInstructionMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.colorinstruction.color.ColorInstructionColorMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo.ColorInstructionColorSaveVO;
import com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo.ColorInstructionSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.product.ProductDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.supplier.SupplierDO;

import com.luna.lunasrv.framework.security.core.LoginUser;
import com.luna.lunasrv.framework.security.core.util.SecurityFrameworkUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo.ColorInstructionConfirmReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo.ColorInstructionVoidReqVO;
import com.luna.lunasrv.module.lunaerp.enums.production.colorinstruction.ColorInstructionConfirmTypeEnum;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.greigeorder.GreigeOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.dyeingorder.DyeingOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.greigeorder.GreigeOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.dyeingorder.DyeingOrderMapper;
import com.luna.lunasrv.module.lunaerp.enums.production.colorinstruction.ColorInstructionLineStatusEnum;
import com.luna.lunasrv.module.lunaerp.enums.production.colorinstruction.ColorInstructionStatusEnum;

import static com.luna.lunasrv.module.lunaerp.enums.ColorInstructionErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link ColorInstructionServiceImpl} 单元测试
 */
@ExtendWith(MockitoExtension.class)
public class ColorInstructionServiceImplTest {

    @Mock
    private ColorInstructionMapper colorInstructionMapper;

    @Mock
    private ColorInstructionColorMapper colorInstructionColorMapper;

    @Mock
    private com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper customerMapper;

    @Mock
    private com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.product.ProductMapper productMapper;

    @Mock
    private com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.supplier.SupplierMapper supplierMapper;

    @Mock
    private GreigeOrderMapper greigeOrderMapper;

    @Mock
    private DyeingOrderMapper dyeingOrderMapper;

    @InjectMocks
    private ColorInstructionServiceImpl colorInstructionService;

    @BeforeEach
    void setUp() {
        // 初始化 TableInfo 缓存，使 LambdaQueryWrapper 的 SFunction 列名解析在无 Spring/MyBatis 容器环境下可用
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), ColorInstructionDO.class);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), ColorInstructionColorDO.class);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), GreigeOrderDO.class);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), DyeingOrderDO.class);
    }

    @Test
    @DisplayName("分页查询：筛选条件由 ColorInstructionMapper.buildQueryWrapper 正确构造，包含租户隔离与 deleted=false")
    void testBuildQueryWrapper_filtersAndTenantHandling() {
        try {
            // 1. 设置当前租户为 10086
            com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder.setTenantId(10086L);

            ColorInstructionPageReqVO reqVO = new ColorInstructionPageReqVO();
            reqVO.setInstructionNo("DS2026");
            reqVO.setCustomerId(101L);
            reqVO.setProductCodeOrName("纯棉");
            reqVO.setCustomerColorNo("C01");
            reqVO.setStatus("PENDING");
            reqVO.setInstructionDateRange(new LocalDate[]{
                    LocalDate.of(2026, 10, 1),
                    LocalDate.of(2026, 10, 31)
            });

            LambdaQueryWrapperX<ColorInstructionDO> wrapper = ColorInstructionMapper.buildQueryWrapper(reqVO);
            String sqlSegment = wrapper.getSqlSegment();

            assertTrue(sqlSegment.contains("LOWER(instruction_no) LIKE"), "instructionNo 应做模糊匹配");
            assertTrue(sqlSegment.contains("customer_id ="), "customerId 应做等值匹配");
            assertTrue(sqlSegment.contains("LOWER(product_code) LIKE") && sqlSegment.contains("LOWER(product_name) LIKE"),
                    "productCodeOrName 应覆盖编码和名称");
            assertTrue(sqlSegment.contains("lunaerp_production_color_instruction_color") && sqlSegment.contains("customer_color_no"),
                    "customerColorNo 应通过子查询过滤，主查询不联表");
            assertTrue(sqlSegment.contains("tenant_id ="), "子查询应带上当前租户 ID 字段");
            assertTrue(wrapper.getParamNameValuePairs().containsValue(10086L), "子查询参数应包含当前租户 ID 10086");
            assertTrue(sqlSegment.contains("deleted = false"), "子查询应过滤未删除行且兼容 PostgreSQL 布尔类型");
            assertTrue(sqlSegment.contains("status ="), "status 应做等值匹配");
            assertTrue(sqlSegment.contains("instruction_date BETWEEN") || (sqlSegment.contains("instruction_date >=") && sqlSegment.contains("instruction_date <=")),
                    "instructionDateRange 应做日期区间过滤");
            assertTrue(sqlSegment.contains("ORDER BY instruction_date DESC"), "默认应按单据日期倒序");

            // 2. 清除租户，测试系统租户或无租户上下文情况
            com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder.clear();
            LambdaQueryWrapperX<ColorInstructionDO> wrapperNoTenant = ColorInstructionMapper.buildQueryWrapper(reqVO);
            assertFalse(wrapperNoTenant.getSqlSegment().contains("tenant_id ="), "无租户上下文时不拼接 tenant_id 条件");
            assertTrue(wrapperNoTenant.getSqlSegment().contains("deleted = false"), "子查询仍应包含 deleted = false 过滤");
        } finally {
            com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder.clear();
        }
    }

    @Test
    @DisplayName("分页查询服务：调用 Mapper 分页，空列表直接返回且不触发颜色行查询")
    void testGetColorInstructionPage_emptyList() {
        ColorInstructionPageReqVO reqVO = new ColorInstructionPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);

        when(colorInstructionMapper.selectPage(eq(reqVO)))
                .thenReturn(PageResult.empty());

        PageResult<ColorInstructionRespVO> result = colorInstructionService.getColorInstructionPage(reqVO);
        assertNotNull(result);
        assertTrue(result.getList().isEmpty());

        // 空列表不触发颜色子表查询
        verify(colorInstructionColorMapper, never()).selectListByInstructionIds(any());
    }

    @Test
    @DisplayName("分页查询防 N+1：多条主单记录只触发一次颜色子表批量查询，并正确组装 chips 摘要")
    void testGetColorInstructionPage_preventsNPlusOne() {
        ColorInstructionPageReqVO reqVO = new ColorInstructionPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);

        ColorInstructionDO do1 = new ColorInstructionDO();
        do1.setId(1001L);
        do1.setInstructionNo("DS20261007001");
        do1.setClientName("优衣库");
        do1.setProductCode("P-01");
        do1.setProductName("双面布");
        do1.setStatus("PENDING");
        do1.setInstructionDate(LocalDate.of(2026, 10, 7));

        ColorInstructionDO do2 = new ColorInstructionDO();
        do2.setId(1002L);
        do2.setInstructionNo("DS20261007002");
        do2.setClientName("耐克");
        do2.setProductCode("P-02");
        do2.setProductName("卫衣布");
        do2.setStatus("CONFIRMED");
        do2.setInstructionDate(LocalDate.of(2026, 10, 8));

        when(colorInstructionMapper.selectPage(eq(reqVO)))
                .thenReturn(new PageResult<>(List.of(do1, do2), 2L));

        ColorInstructionColorDO c1 = new ColorInstructionColorDO();
        c1.setId(1L);
        c1.setInstructionId(1001L);
        c1.setCustomerColorNo("C01");
        c1.setColorName("藏青");
        c1.setColorHex("#000080");
        c1.setRound(1);
        c1.setStatus("PENDING");

        ColorInstructionColorDO c2 = new ColorInstructionColorDO();
        c2.setId(2L);
        c2.setInstructionId(1001L);
        c2.setCustomerColorNo("C02");
        c2.setColorName("酒红");
        c2.setColorHex("#800000");
        c2.setRound(2);
        c2.setStatus("APPROVED");

        ColorInstructionColorDO c3 = new ColorInstructionColorDO();
        c3.setId(3L);
        c3.setInstructionId(1002L);
        c3.setCustomerColorNo("N01");
        c3.setColorName("纯黑");
        c3.setColorHex("#000000");
        c3.setRound(1);
        c3.setStatus("APPROVED");

        // 模拟批量查询返回全部颜色行
        when(colorInstructionColorMapper.selectListByInstructionIds(List.of(1001L, 1002L)))
                .thenReturn(List.of(c1, c2, c3));

        PageResult<ColorInstructionRespVO> result = colorInstructionService.getColorInstructionPage(reqVO);

        // 验证主表查询 1 次，颜色行批量查询恰好 1 次（防 N+1）
        verify(colorInstructionColorMapper, times(1)).selectListByInstructionIds(List.of(1001L, 1002L));

        assertNotNull(result);
        assertEquals(2, result.getList().size());

        ColorInstructionRespVO resp1 = result.getList().get(0);
        assertEquals("DS20261007001", resp1.getInstructionNo());
        assertNotNull(resp1.getColorSummaries());
        assertEquals(2, resp1.getColorSummaries().size());
        assertEquals("C01", resp1.getColorSummaries().get(0).getCustomerColorNo());
        assertEquals("藏青", resp1.getColorSummaries().get(0).getColorName());
        assertEquals("#000080", resp1.getColorSummaries().get(0).getColorHex());
        assertEquals(1, resp1.getColorSummaries().get(0).getRound());
        assertEquals("PENDING", resp1.getColorSummaries().get(0).getStatus());

        assertEquals("C02", resp1.getColorSummaries().get(1).getCustomerColorNo());
        assertEquals(2, resp1.getColorSummaries().get(1).getRound());
        assertEquals("APPROVED", resp1.getColorSummaries().get(1).getStatus());

        ColorInstructionRespVO resp2 = result.getList().get(1);
        assertEquals("DS20261007002", resp2.getInstructionNo());
        assertNotNull(resp2.getColorSummaries());
        assertEquals(1, resp2.getColorSummaries().size());
        assertEquals("N01", resp2.getColorSummaries().get(0).getCustomerColorNo());
    }

    @Test
    @DisplayName("详情查询：不存在或跨租户查空时抛出 COLOR_INSTRUCTION_NOT_EXISTS 异常")
    void testGetColorInstruction_notFound() {
        when(colorInstructionMapper.selectById(9999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                colorInstructionService.getColorInstruction(9999L));
        assertEquals(COLOR_INSTRUCTION_NOT_EXISTS.getCode(), ex.getCode());
        assertEquals("打色单不存在", ex.getMessage());
    }

    @Test
    @DisplayName("详情查询：存在时返回单据头及嵌套颜色行列表（含 REJECTED 历史轮次行，按 lineNo 排序）与摘要")
    void testGetColorInstruction_success() {
        ColorInstructionDO do1 = new ColorInstructionDO();
        do1.setId(1001L);
        do1.setInstructionNo("DS20261007001");
        do1.setClientName("优衣库");
        do1.setStatus("PENDING");
        when(colorInstructionMapper.selectById(1001L)).thenReturn(do1);

        ColorInstructionColorDO c1 = new ColorInstructionColorDO();
        c1.setId(1L);
        c1.setInstructionId(1001L);
        c1.setCustomerColorNo("C01");
        c1.setColorName("藏青第2轮");
        c1.setRound(2);
        c1.setLineNo(1);
        c1.setStatus("PENDING");

        ColorInstructionColorDO c2 = new ColorInstructionColorDO();
        c2.setId(2L);
        c2.setInstructionId(1001L);
        c2.setCustomerColorNo("C01");
        c2.setColorName("藏青第1轮");
        c2.setRound(1);
        c2.setLineNo(2);
        c2.setStatus("REJECTED");
        c2.setRejectReason("色偏暗");

        ColorInstructionColorDO c3 = new ColorInstructionColorDO();
        c3.setId(3L);
        c3.setInstructionId(1001L);
        c3.setCustomerColorNo("C02");
        c3.setColorName("军绿");
        c3.setRound(1);
        c3.setLineNo(3);
        c3.setStatus("APPROVED");

        when(colorInstructionColorMapper.selectListByInstructionId(1001L)).thenReturn(List.of(c1, c2, c3));

        ColorInstructionRespVO resp = colorInstructionService.getColorInstruction(1001L);
        assertNotNull(resp);
        assertEquals("DS20261007001", resp.getInstructionNo());

        // 验证返回包含全部颜色行，包含 REJECTED 历史轮次行，且按 lineNo 排序
        assertNotNull(resp.getColors());
        assertEquals(3, resp.getColors().size());
        assertEquals(1L, resp.getColors().get(0).getId());
        assertEquals(1, resp.getColors().get(0).getLineNo());
        assertEquals("PENDING", resp.getColors().get(0).getStatus());

        assertEquals(2L, resp.getColors().get(1).getId());
        assertEquals(2, resp.getColors().get(1).getLineNo());
        assertEquals("REJECTED", resp.getColors().get(1).getStatus());
        assertEquals("色偏暗", resp.getColors().get(1).getRejectReason());

        assertEquals(3L, resp.getColors().get(2).getId());
        assertEquals(3, resp.getColors().get(2).getLineNo());
        assertEquals("APPROVED", resp.getColors().get(2).getStatus());

        // 验证摘要列表
        assertEquals(3, resp.getColorSummaries().size());
    }

    private ColorInstructionColorSaveVO createValidColor() {
        ColorInstructionColorSaveVO color = new ColorInstructionColorSaveVO();
        color.setCustomerColorNo("C01");
        color.setColorName("黑色");
        color.setLightPrimary("D65");
        return color;
    }

    @Test
    @DisplayName("预览下一个单号：当天无单据时从 001 起号")
    void testGetNextInstructionNo_emptyCandidates() {
        String todayStr = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDate.now());
        String expectedPrefix = "DS" + todayStr;
        when(colorInstructionMapper.selectListByNoPrefixIncludingDeleted(expectedPrefix))
                .thenReturn(java.util.Collections.emptyList());

        String nextNo = colorInstructionService.getNextInstructionNo();
        assertEquals(expectedPrefix + "001", nextNo);
    }

    @Test
    @DisplayName("预览下一个单号：跨天重置（历史存在昨天单据，今天依然按当天日期从 001 开始）")
    void testGetNextInstructionNo_crossDayReset() {
        String todayStr = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDate.now());
        String todayPrefix = "DS" + todayStr;
        // 今天前缀查出来为空
        when(colorInstructionMapper.selectListByNoPrefixIncludingDeleted(todayPrefix))
                .thenReturn(java.util.Collections.emptyList());

        String nextNo = colorInstructionService.getNextInstructionNo();
        assertEquals(todayPrefix + "001", nextNo);
    }

    @Test
    @DisplayName("预览下一个单号：扫描含已删除单据并取 max+1，超999自然扩位")
    void testGetNextInstructionNo_withExistingAndDeleted() {
        String todayStr = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDate.now());
        String expectedPrefix = "DS" + todayStr;

        ColorInstructionDO d1 = new ColorInstructionDO();
        d1.setInstructionNo(expectedPrefix + "001");
        ColorInstructionDO d2 = new ColorInstructionDO();
        d2.setInstructionNo(expectedPrefix + "005"); // 存在已删或跳号的 005
        d2.setDeleted(true);

        when(colorInstructionMapper.selectListByNoPrefixIncludingDeleted(expectedPrefix))
                .thenReturn(List.of(d1, d2));

        String nextNo = colorInstructionService.getNextInstructionNo();
        assertEquals(expectedPrefix + "006", nextNo);
    }

    @Test
    @DisplayName("新建打色单：颜色明细为空时抛出 COLOR_INSTRUCTION_COLOR_NOT_EXISTS")
    void testCreateColorInstruction_emptyColors_throwsException() {
        ColorInstructionSaveReqVO reqVO = new ColorInstructionSaveReqVO();
        reqVO.setCustomerId(101L);
        reqVO.setProductId(201L);
        reqVO.setColors(java.util.Collections.emptyList());

        ServiceException ex = assertThrows(ServiceException.class, () ->
                colorInstructionService.createColorInstruction(reqVO));
        assertEquals(COLOR_INSTRUCTION_COLOR_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("新建打色单：手填单号撞号（含已删除）抛 COLOR_INSTRUCTION_NO_DUPLICATE 不顺延")
    void testCreateColorInstruction_manualDuplicate_throwsException() {
        ColorInstructionSaveReqVO reqVO = new ColorInstructionSaveReqVO();
        reqVO.setInstructionNo("DS20261013008");
        reqVO.setCustomerId(101L);
        reqVO.setProductId(201L);
        reqVO.setColors(List.of(createValidColor()));

        ColorInstructionDO existing = new ColorInstructionDO();
        existing.setId(99L);
        existing.setInstructionNo("DS20261013008");
        existing.setDeleted(true);

        when(colorInstructionMapper.selectByInstructionNoIncludingDeleted("DS20261013008"))
                .thenReturn(existing);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                colorInstructionService.createColorInstruction(reqVO));
        assertEquals(COLOR_INSTRUCTION_NO_DUPLICATE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("新建打色单：客户不存在时抛出 COLOR_INSTRUCTION_REFERENCE_NOT_FOUND")
    void testCreateColorInstruction_customerNotFound_throwsException() {
        ColorInstructionSaveReqVO reqVO = new ColorInstructionSaveReqVO();
        reqVO.setCustomerId(101L);
        reqVO.setProductId(201L);
        reqVO.setColors(List.of(createValidColor()));

        when(customerMapper.selectById(101L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                colorInstructionService.createColorInstruction(reqVO));
        assertEquals(COLOR_INSTRUCTION_REFERENCE_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("新建打色单：产品不存在时抛出 COLOR_INSTRUCTION_REFERENCE_NOT_FOUND")
    void testCreateColorInstruction_productNotFound_throwsException() {
        ColorInstructionSaveReqVO reqVO = new ColorInstructionSaveReqVO();
        reqVO.setCustomerId(101L);
        reqVO.setProductId(201L);
        reqVO.setColors(List.of(createValidColor()));

        CustomerDO customer = new CustomerDO();
        customer.setId(101L);
        customer.setCompanyName("客户A");
        when(customerMapper.selectById(101L)).thenReturn(customer);
        when(productMapper.selectById(201L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                colorInstructionService.createColorInstruction(reqVO));
        assertEquals(COLOR_INSTRUCTION_REFERENCE_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("新建打色单：打色承接方供应商不存在时抛出 COLOR_INSTRUCTION_REFERENCE_NOT_FOUND")
    void testCreateColorInstruction_labFactoryNotFound_throwsException() {
        ColorInstructionSaveReqVO reqVO = new ColorInstructionSaveReqVO();
        reqVO.setCustomerId(101L);
        reqVO.setProductId(201L);
        reqVO.setLabFactoryId(301L);
        reqVO.setColors(List.of(createValidColor()));

        CustomerDO customer = new CustomerDO();
        customer.setId(101L);
        customer.setCompanyName("客户A");
        when(customerMapper.selectById(101L)).thenReturn(customer);

        ProductDO product = new ProductDO();
        product.setId(201L);
        product.setCode("P001");
        product.setName("纯棉斜纹");
        when(productMapper.selectById(201L)).thenReturn(product);

        when(supplierMapper.selectById(301L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                colorInstructionService.createColorInstruction(reqVO));
        assertEquals(COLOR_INSTRUCTION_REFERENCE_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("新建打色单：本厂打色房（承接方为空）放行通过，labFactoryId 与 labFactoryName 为空")
    void testCreateColorInstruction_withoutLabFactory_success() {
        String todayStr = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDate.now());
        String expectedPrefix = "DS" + todayStr;

        ColorInstructionSaveReqVO reqVO = new ColorInstructionSaveReqVO();
        reqVO.setCustomerId(101L);
        reqVO.setProductId(201L);
        reqVO.setLabFactoryId(null);
        reqVO.setColors(List.of(createValidColor()));

        CustomerDO customer = new CustomerDO();
        customer.setId(101L);
        customer.setCompanyName("优衣库");
        when(customerMapper.selectById(101L)).thenReturn(customer);

        ProductDO product = new ProductDO();
        product.setId(201L);
        product.setCode("P001");
        product.setName("纯棉");
        when(productMapper.selectById(201L)).thenReturn(product);

        when(colorInstructionMapper.selectListByNoPrefixIncludingDeleted(expectedPrefix))
                .thenReturn(java.util.Collections.emptyList());
        when(colorInstructionMapper.selectByInstructionNoIncludingDeleted(expectedPrefix + "001"))
                .thenReturn(null);

        doAnswer(invocation -> {
            ColorInstructionDO insertDO = invocation.getArgument(0);
            insertDO.setId(888L);
            return 1;
        }).when(colorInstructionMapper).insert(any(ColorInstructionDO.class));

        Long id = colorInstructionService.createColorInstruction(reqVO);
        assertEquals(888L, id);

        verify(supplierMapper, never()).selectById(any());
        verify(colorInstructionMapper).insert(argThat((ColorInstructionDO d) ->
                d.getLabFactoryId() == null && d.getLabFactoryName() == null
        ));
    }

    @Test
    @DisplayName("新建打色单：并发插入主表撞唯一约束抛 DuplicateKeyException 时映射为 COLOR_INSTRUCTION_NO_DUPLICATE")
    void testCreateColorInstruction_duplicateKeyException_mapped() {
        ColorInstructionSaveReqVO reqVO = new ColorInstructionSaveReqVO();
        reqVO.setCustomerId(101L);
        reqVO.setProductId(201L);
        reqVO.setColors(List.of(createValidColor()));

        CustomerDO customer = new CustomerDO();
        customer.setId(101L);
        customer.setCompanyName("优衣库");
        when(customerMapper.selectById(101L)).thenReturn(customer);

        ProductDO product = new ProductDO();
        product.setId(201L);
        product.setCode("P001");
        product.setName("纯棉");
        when(productMapper.selectById(201L)).thenReturn(product);

        String todayStr = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDate.now());
        String expectedPrefix = "DS" + todayStr;
        when(colorInstructionMapper.selectListByNoPrefixIncludingDeleted(expectedPrefix))
                .thenReturn(java.util.Collections.emptyList());
        when(colorInstructionMapper.selectByInstructionNoIncludingDeleted(expectedPrefix + "001"))
                .thenReturn(null);

        doThrow(new org.springframework.dao.DuplicateKeyException("duplicate key value violates unique constraint"))
                .when(colorInstructionMapper).insert(any(ColorInstructionDO.class));

        ServiceException ex = assertThrows(ServiceException.class, () ->
                colorInstructionService.createColorInstruction(reqVO));
        assertEquals(COLOR_INSTRUCTION_NO_DUPLICATE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("新建打色单：主灯源非法时抛出 COLOR_INSTRUCTION_INVALID_CONFIRM_TYPE_OR_LIGHT")
    void testCreateColorInstruction_invalidLightPrimary_throwsException() {
        ColorInstructionSaveReqVO reqVO = new ColorInstructionSaveReqVO();
        reqVO.setCustomerId(101L);
        reqVO.setProductId(201L);

        CustomerDO customer = new CustomerDO();
        customer.setId(101L);
        when(customerMapper.selectById(101L)).thenReturn(customer);

        ProductDO product = new ProductDO();
        product.setId(201L);
        when(productMapper.selectById(201L)).thenReturn(product);

        ColorInstructionColorSaveVO color = new ColorInstructionColorSaveVO();
        color.setCustomerColorNo("C01");
        color.setColorName("黑色");
        color.setLightPrimary("NON_EXISTING_LIGHT");
        reqVO.setColors(List.of(color));

        ServiceException ex = assertThrows(ServiceException.class, () ->
                colorInstructionService.createColorInstruction(reqVO));
        assertEquals(COLOR_INSTRUCTION_INVALID_CONFIRM_TYPE_OR_LIGHT.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("新建打色单：辅灯源非空但非法时抛出 COLOR_INSTRUCTION_INVALID_CONFIRM_TYPE_OR_LIGHT")
    void testCreateColorInstruction_invalidLightSecondary_throwsException() {
        ColorInstructionSaveReqVO reqVO = new ColorInstructionSaveReqVO();
        reqVO.setCustomerId(101L);
        reqVO.setProductId(201L);

        CustomerDO customer = new CustomerDO();
        customer.setId(101L);
        when(customerMapper.selectById(101L)).thenReturn(customer);

        ProductDO product = new ProductDO();
        product.setId(201L);
        when(productMapper.selectById(201L)).thenReturn(product);

        ColorInstructionColorSaveVO color = new ColorInstructionColorSaveVO();
        color.setCustomerColorNo("C01");
        color.setColorName("黑色");
        color.setLightPrimary("D65");
        color.setLightSecondary("INVALID_LIGHT_2");
        reqVO.setColors(List.of(color));

        ServiceException ex = assertThrows(ServiceException.class, () ->
                colorInstructionService.createColorInstruction(reqVO));
        assertEquals(COLOR_INSTRUCTION_INVALID_CONFIRM_TYPE_OR_LIGHT.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("新建打色单：自动取号、缺省值（日期/状态/round=1）、快照回填与颜色行级联插入")
    void testCreateColorInstruction_success() {
        String todayStr = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDate.now());
        String expectedPrefix = "DS" + todayStr;

        ColorInstructionSaveReqVO reqVO = new ColorInstructionSaveReqVO();
        reqVO.setCustomerId(101L);
        reqVO.setProductId(201L);
        reqVO.setLabFactoryId(301L);
        reqVO.setContactPerson("张工 13900000000");
        reqVO.setRemark("急单");

        CustomerDO customer = new CustomerDO();
        customer.setId(101L);
        customer.setCompanyName("客户集团");
        when(customerMapper.selectById(101L)).thenReturn(customer);

        ProductDO product = new ProductDO();
        product.setId(201L);
        product.setCode("P100");
        product.setName("特级牛津布");
        product.setComposition("100% 涤纶");
        when(productMapper.selectById(201L)).thenReturn(product);

        SupplierDO labFactory = new SupplierDO();
        labFactory.setId(301L);
        labFactory.setCompanyName("优染打色房");
        when(supplierMapper.selectById(301L)).thenReturn(labFactory);

        // 自动取号 mock
        when(colorInstructionMapper.selectListByNoPrefixIncludingDeleted(expectedPrefix))
                .thenReturn(java.util.Collections.emptyList());
        when(colorInstructionMapper.selectByInstructionNoIncludingDeleted(expectedPrefix + "001"))
                .thenReturn(null);

        ColorInstructionColorSaveVO c1 = new ColorInstructionColorSaveVO();
        c1.setCustomerColorNo("C01");
        c1.setColorName("藏青");
        c1.setColorHex("#000080");
        c1.setLightPrimary("D65");
        c1.setLightSecondary("TL84");
        c1.setColorCardRef("TCX-01");
        c1.setCopies(2);
        c1.setSamplePhotos(List.of("https://luna.com/p1.png"));

        ColorInstructionColorSaveVO c2 = new ColorInstructionColorSaveVO();
        c2.setCustomerColorNo("C02");
        c2.setColorName("浅灰");
        c2.setLightPrimary("CWF");

        reqVO.setColors(List.of(c1, c2));

        // 模拟 insert 主表回填 ID
        doAnswer(invocation -> {
            ColorInstructionDO insertDO = invocation.getArgument(0);
            insertDO.setId(888L);
            return 1;
        }).when(colorInstructionMapper).insert(any(ColorInstructionDO.class));

        Long newId = colorInstructionService.createColorInstruction(reqVO);
        assertEquals(888L, newId);

        // 验证主表 DO
        ArgumentCaptor<ColorInstructionDO> masterCaptor = ArgumentCaptor.forClass(ColorInstructionDO.class);
        verify(colorInstructionMapper).insert(masterCaptor.capture());
        ColorInstructionDO savedMaster = masterCaptor.getValue();
        assertEquals(expectedPrefix + "001", savedMaster.getInstructionNo());
        assertEquals("客户集团", savedMaster.getClientName());
        assertEquals("P100", savedMaster.getProductCode());
        assertEquals("特级牛津布", savedMaster.getProductName());
        assertEquals("100% 涤纶", savedMaster.getComposition());
        assertEquals("优染打色房", savedMaster.getLabFactoryName());
        assertEquals("PENDING", savedMaster.getStatus());
        assertEquals(LocalDate.now(), savedMaster.getInstructionDate());

        // 验证颜色行插入 2 次
        ArgumentCaptor<ColorInstructionColorDO> colorCaptor = ArgumentCaptor.forClass(ColorInstructionColorDO.class);
        verify(colorInstructionColorMapper, times(2)).insert(colorCaptor.capture());
        List<ColorInstructionColorDO> savedColors = colorCaptor.getAllValues();

        assertEquals(888L, savedColors.get(0).getInstructionId());
        assertEquals(1, savedColors.get(0).getLineNo());
        assertEquals("C01", savedColors.get(0).getCustomerColorNo());
        assertEquals("D65", savedColors.get(0).getLightPrimary());
        assertEquals("TL84", savedColors.get(0).getLightSecondary());
        assertEquals(1, savedColors.get(0).getRound());
        assertEquals("PENDING", savedColors.get(0).getStatus());

        assertEquals(888L, savedColors.get(1).getInstructionId());
        assertEquals(2, savedColors.get(1).getLineNo());
        assertEquals("C02", savedColors.get(1).getCustomerColorNo());
        assertEquals("CWF", savedColors.get(1).getLightPrimary());
        assertNull(savedColors.get(1).getLightSecondary());
        assertEquals(1, savedColors.get(1).getRound());
        assertEquals("PENDING", savedColors.get(1).getStatus());
    }

    @Test
    @DisplayName("删除打色单：目标单据不存在抛 COLOR_INSTRUCTION_NOT_EXISTS")
    void testDeleteColorInstruction_notExists_throwsException() {
        when(colorInstructionMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                colorInstructionService.deleteColorInstruction(999L));
        assertEquals(COLOR_INSTRUCTION_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("删除打色单：非 PENDING 状态不允许删除，抛 COLOR_INSTRUCTION_STATUS_NOT_ALLOWED")
    void testDeleteColorInstruction_statusNotAllowed_throwsException() {
        ColorInstructionDO order = new ColorInstructionDO();
        order.setId(1001L);
        order.setStatus("IN_PROGRESS");
        when(colorInstructionMapper.selectById(1001L)).thenReturn(order);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                colorInstructionService.deleteColorInstruction(1001L));
        assertEquals(COLOR_INSTRUCTION_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("删除打色单：PENDING 状态成功级联软删除主表与全部颜色行")
    void testDeleteColorInstruction_success() {
        ColorInstructionDO order = new ColorInstructionDO();
        order.setId(1001L);
        order.setStatus("PENDING");
        when(colorInstructionMapper.selectById(1001L)).thenReturn(order);

        colorInstructionService.deleteColorInstruction(1001L);

        // 验证级联删除主表与子表
        verify(colorInstructionMapper, times(1)).deleteById(1001L);
        verify(colorInstructionColorMapper, times(1)).deleteByInstructionId(1001L);
    }

    // ==================== updateColorInstruction: 修改（03 票） ====================

    @Test
    @DisplayName("更新打色单：单据不存在抛 COLOR_INSTRUCTION_NOT_EXISTS")
    void testUpdateColorInstruction_notExists_throwsException() {
        ColorInstructionSaveReqVO reqVO = new ColorInstructionSaveReqVO();
        reqVO.setId(999L);
        when(colorInstructionMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                colorInstructionService.updateColorInstruction(reqVO));
        assertEquals(COLOR_INSTRUCTION_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("更新打色单：终态或非可编辑状态（CONFIRMED/IN_PRODUCTION/VOID）抛 COLOR_INSTRUCTION_STATUS_NOT_ALLOWED")
    void testUpdateColorInstruction_statusNotAllowed_throwsException() {
        for (String status : List.of("CONFIRMED", "IN_PRODUCTION", "VOID")) {
            ColorInstructionDO order = new ColorInstructionDO();
            order.setId(1001L);
            order.setStatus(status);
            when(colorInstructionMapper.selectById(1001L)).thenReturn(order);

            ColorInstructionSaveReqVO reqVO = new ColorInstructionSaveReqVO();
            reqVO.setId(1001L);
            ServiceException ex = assertThrows(ServiceException.class, () ->
                    colorInstructionService.updateColorInstruction(reqVO));
            assertEquals(COLOR_INSTRUCTION_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
        }
    }

    @Test
    @DisplayName("更新打色单：手填单号撞其他单据（含已删除）抛 COLOR_INSTRUCTION_NO_DUPLICATE")
    void testUpdateColorInstruction_instructionNoDuplicate_throwsException() {
        ColorInstructionDO current = new ColorInstructionDO();
        current.setId(1001L);
        current.setStatus("PENDING");
        current.setInstructionNo("DS20261013001");
        when(colorInstructionMapper.selectById(1001L)).thenReturn(current);

        ColorInstructionDO duplicate = new ColorInstructionDO();
        duplicate.setId(2002L);
        duplicate.setInstructionNo("DS20261013999");
        when(colorInstructionMapper.selectByInstructionNoIncludingDeleted("DS20261013999")).thenReturn(duplicate);

        ColorInstructionSaveReqVO reqVO = new ColorInstructionSaveReqVO();
        reqVO.setId(1001L);
        reqVO.setInstructionNo("DS20261013999");
        ColorInstructionColorSaveVO color = new ColorInstructionColorSaveVO();
        color.setCustomerColorNo("C01");
        color.setColorName("藏青");
        color.setLightPrimary("D65");
        reqVO.setColors(List.of(color));

        ServiceException ex = assertThrows(ServiceException.class, () ->
                colorInstructionService.updateColorInstruction(reqVO));
        assertEquals(COLOR_INSTRUCTION_NO_DUPLICATE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("更新打色单：客户/产品/供应商引用校验非法抛 COLOR_INSTRUCTION_REFERENCE_NOT_FOUND")
    void testUpdateColorInstruction_referenceNotFound_throwsException() {
        ColorInstructionDO current = new ColorInstructionDO();
        current.setId(1001L);
        current.setStatus("PENDING");
        when(colorInstructionMapper.selectById(1001L)).thenReturn(current);

        ColorInstructionColorSaveVO color = new ColorInstructionColorSaveVO();
        color.setCustomerColorNo("C01");
        color.setColorName("藏青");
        color.setLightPrimary("D65");

        // 1. 客户不存在
        ColorInstructionSaveReqVO reqVO1 = new ColorInstructionSaveReqVO();
        reqVO1.setId(1001L);
        reqVO1.setCustomerId(999L);
        reqVO1.setProductId(2001L);
        reqVO1.setColors(List.of(color));
        when(customerMapper.selectById(999L)).thenReturn(null);
        ServiceException ex1 = assertThrows(ServiceException.class, () ->
                colorInstructionService.updateColorInstruction(reqVO1));
        assertEquals(COLOR_INSTRUCTION_REFERENCE_NOT_FOUND.getCode(), ex1.getCode());

        // 2. 产品不存在
        CustomerDO customer = new CustomerDO();
        customer.setId(101L);
        customer.setCompanyName("优质客户");
        when(customerMapper.selectById(101L)).thenReturn(customer);

        ColorInstructionSaveReqVO reqVO2 = new ColorInstructionSaveReqVO();
        reqVO2.setId(1001L);
        reqVO2.setCustomerId(101L);
        reqVO2.setProductId(999L);
        reqVO2.setColors(List.of(color));
        when(productMapper.selectById(999L)).thenReturn(null);
        ServiceException ex2 = assertThrows(ServiceException.class, () ->
                colorInstructionService.updateColorInstruction(reqVO2));
        assertEquals(COLOR_INSTRUCTION_REFERENCE_NOT_FOUND.getCode(), ex2.getCode());

        // 3. 打色供应商不存在
        ProductDO product = new ProductDO();
        product.setId(201L);
        product.setCode("P01");
        product.setName("平纹布");
        when(productMapper.selectById(201L)).thenReturn(product);

        ColorInstructionSaveReqVO reqVO3 = new ColorInstructionSaveReqVO();
        reqVO3.setId(1001L);
        reqVO3.setCustomerId(101L);
        reqVO3.setProductId(201L);
        reqVO3.setLabFactoryId(888L);
        reqVO3.setColors(List.of(color));
        when(supplierMapper.selectById(888L)).thenReturn(null);
        ServiceException ex3 = assertThrows(ServiceException.class, () ->
                colorInstructionService.updateColorInstruction(reqVO3));
        assertEquals(COLOR_INSTRUCTION_REFERENCE_NOT_FOUND.getCode(), ex3.getCode());
    }

    @Test
    @DisplayName("更新打色单：灯源非法抛 COLOR_INSTRUCTION_INVALID_CONFIRM_TYPE_OR_LIGHT")
    void testUpdateColorInstruction_invalidLight_throwsException() {
        ColorInstructionDO current = new ColorInstructionDO();
        current.setId(1001L);
        current.setStatus("IN_PROGRESS");
        when(colorInstructionMapper.selectById(1001L)).thenReturn(current);

        CustomerDO customer = new CustomerDO();
        customer.setId(101L);
        customer.setCompanyName("客户");
        when(customerMapper.selectById(101L)).thenReturn(customer);

        ProductDO product = new ProductDO();
        product.setId(201L);
        product.setCode("P01");
        product.setName("面料");
        when(productMapper.selectById(201L)).thenReturn(product);

        ColorInstructionSaveReqVO reqVO = new ColorInstructionSaveReqVO();
        reqVO.setId(1001L);
        reqVO.setCustomerId(101L);
        reqVO.setProductId(201L);
        ColorInstructionColorSaveVO color = new ColorInstructionColorSaveVO();
        color.setCustomerColorNo("C01");
        color.setColorName("藏青");
        color.setLightPrimary("INVALID_LIGHT");
        reqVO.setColors(List.of(color));

        ServiceException ex = assertThrows(ServiceException.class, () ->
                colorInstructionService.updateColorInstruction(reqVO));
        assertEquals(COLOR_INSTRUCTION_INVALID_CONFIRM_TYPE_OR_LIGHT.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("更新打色单：跨父非法行ID（提交行ID不属于本单）报错")
    void testUpdateColorInstruction_crossParentColorId_throwsException() {
        ColorInstructionDO current = new ColorInstructionDO();
        current.setId(1001L);
        current.setStatus("PENDING");
        when(colorInstructionMapper.selectById(1001L)).thenReturn(current);

        CustomerDO customer = new CustomerDO();
        customer.setId(101L);
        when(customerMapper.selectById(101L)).thenReturn(customer);
        ProductDO product = new ProductDO();
        product.setId(201L);
        when(productMapper.selectById(201L)).thenReturn(product);

        // DB 只有行 501L
        ColorInstructionColorDO dbColor = new ColorInstructionColorDO();
        dbColor.setId(501L);
        dbColor.setInstructionId(1001L);
        dbColor.setStatus("PENDING");
        when(colorInstructionColorMapper.selectListByInstructionId(1001L)).thenReturn(List.of(dbColor));

        ColorInstructionSaveReqVO reqVO = new ColorInstructionSaveReqVO();
        reqVO.setId(1001L);
        reqVO.setCustomerId(101L);
        reqVO.setProductId(201L);
        // 提交跨父 ID 999L
        ColorInstructionColorSaveVO color = new ColorInstructionColorSaveVO();
        color.setId(999L);
        color.setCustomerColorNo("C01");
        color.setColorName("藏青");
        color.setLightPrimary("D65");
        reqVO.setColors(List.of(color));

        assertThrows(ServiceException.class, () ->
                colorInstructionService.updateColorInstruction(reqVO));
    }

    @Test
    @DisplayName("更新打色单：提交列表中存在重复行ID报错")
    void testUpdateColorInstruction_duplicateColorIds_throwsException() {
        ColorInstructionDO current = new ColorInstructionDO();
        current.setId(1001L);
        current.setStatus("PENDING");
        when(colorInstructionMapper.selectById(1001L)).thenReturn(current);

        CustomerDO customer = new CustomerDO();
        customer.setId(101L);
        when(customerMapper.selectById(101L)).thenReturn(customer);
        ProductDO product = new ProductDO();
        product.setId(201L);
        when(productMapper.selectById(201L)).thenReturn(product);

        ColorInstructionColorDO dbColor = new ColorInstructionColorDO();
        dbColor.setId(501L);
        dbColor.setInstructionId(1001L);
        dbColor.setStatus("PENDING");
        when(colorInstructionColorMapper.selectListByInstructionId(1001L)).thenReturn(List.of(dbColor));

        ColorInstructionSaveReqVO reqVO = new ColorInstructionSaveReqVO();
        reqVO.setId(1001L);
        reqVO.setCustomerId(101L);
        reqVO.setProductId(201L);
        ColorInstructionColorSaveVO c1 = new ColorInstructionColorSaveVO();
        c1.setId(501L);
        c1.setCustomerColorNo("C01");
        c1.setColorName("藏青");
        c1.setLightPrimary("D65");
        ColorInstructionColorSaveVO c2 = new ColorInstructionColorSaveVO();
        c2.setId(501L);
        c2.setCustomerColorNo("C01");
        c2.setColorName("藏青");
        c2.setLightPrimary("D65");
        reqVO.setColors(List.of(c1, c2));

        assertThrows(ServiceException.class, () ->
                colorInstructionService.updateColorInstruction(reqVO));
    }

    @Test
    @DisplayName("更新打色单：冻结行（APPROVED/REJECTED）提交字段变更抛 COLOR_INSTRUCTION_LINE_FROZEN (1_030_032_006)")
    void testUpdateColorInstruction_frozenLineFieldModified_throwsLineFrozen() {
        ColorInstructionDO current = new ColorInstructionDO();
        current.setId(1001L);
        current.setStatus("IN_PROGRESS");
        when(colorInstructionMapper.selectById(1001L)).thenReturn(current);

        CustomerDO customer = new CustomerDO();
        customer.setId(101L);
        when(customerMapper.selectById(101L)).thenReturn(customer);
        ProductDO product = new ProductDO();
        product.setId(201L);
        when(productMapper.selectById(201L)).thenReturn(product);

        // 1. APPROVED 冻结行修改 colorName
        ColorInstructionColorDO approvedLine = new ColorInstructionColorDO();
        approvedLine.setId(501L);
        approvedLine.setInstructionId(1001L);
        approvedLine.setCustomerColorNo("C01");
        approvedLine.setColorName("藏青");
        approvedLine.setLightPrimary("D65");
        approvedLine.setRound(1);
        approvedLine.setStatus("APPROVED");

        when(colorInstructionColorMapper.selectListByInstructionId(1001L)).thenReturn(List.of(approvedLine));

        ColorInstructionSaveReqVO reqVO = new ColorInstructionSaveReqVO();
        reqVO.setId(1001L);
        reqVO.setCustomerId(101L);
        reqVO.setProductId(201L);
        ColorInstructionColorSaveVO vo = new ColorInstructionColorSaveVO();
        vo.setId(501L);
        vo.setCustomerColorNo("C01");
        vo.setColorName("藏青_篡改");
        vo.setLightPrimary("D65");
        vo.setRound(1);
        vo.setStatus("APPROVED");
        reqVO.setColors(List.of(vo));

        ServiceException ex1 = assertThrows(ServiceException.class, () ->
                colorInstructionService.updateColorInstruction(reqVO));
        assertEquals(COLOR_INSTRUCTION_LINE_FROZEN.getCode(), ex1.getCode());

        // 2. REJECTED 冻结行修改 lightPrimary
        ColorInstructionColorDO rejectedLine = new ColorInstructionColorDO();
        rejectedLine.setId(502L);
        rejectedLine.setInstructionId(1001L);
        rejectedLine.setCustomerColorNo("C02");
        rejectedLine.setColorName("咖啡");
        rejectedLine.setLightPrimary("D65");
        rejectedLine.setRound(1);
        rejectedLine.setStatus("REJECTED");
        rejectedLine.setRejectReason("色偏浅");
        when(colorInstructionColorMapper.selectListByInstructionId(1001L)).thenReturn(List.of(rejectedLine));

        ColorInstructionColorSaveVO vo2 = new ColorInstructionColorSaveVO();
        vo2.setId(502L);
        vo2.setCustomerColorNo("C02");
        vo2.setColorName("咖啡");
        vo2.setLightPrimary("TL84"); // 篡改灯源
        vo2.setRound(1);
        vo2.setStatus("REJECTED");
        vo2.setRejectReason("色偏浅");
        reqVO.setColors(List.of(vo2));

        ServiceException ex2 = assertThrows(ServiceException.class, () ->
                colorInstructionService.updateColorInstruction(reqVO));
        assertEquals(COLOR_INSTRUCTION_LINE_FROZEN.getCode(), ex2.getCode());
    }

    @Test
    @DisplayName("更新打色单：显式提交删除冻结行（deletedColorIds 或 deleted=true）抛 COLOR_INSTRUCTION_LINE_FROZEN (1_030_032_006)")
    void testUpdateColorInstruction_frozenLineExplicitlyDeleted_throwsLineFrozen() {
        ColorInstructionDO current = new ColorInstructionDO();
        current.setId(1001L);
        current.setStatus("IN_PROGRESS");
        when(colorInstructionMapper.selectById(1001L)).thenReturn(current);

        CustomerDO customer = new CustomerDO();
        customer.setId(101L);
        when(customerMapper.selectById(101L)).thenReturn(customer);
        ProductDO product = new ProductDO();
        product.setId(201L);
        when(productMapper.selectById(201L)).thenReturn(product);

        ColorInstructionColorDO approvedLine = new ColorInstructionColorDO();
        approvedLine.setId(501L);
        approvedLine.setInstructionId(1001L);
        approvedLine.setCustomerColorNo("C01");
        approvedLine.setColorName("藏青");
        approvedLine.setLightPrimary("D65");
        approvedLine.setStatus("APPROVED");

        ColorInstructionColorDO rejectedLine = new ColorInstructionColorDO();
        rejectedLine.setId(502L);
        rejectedLine.setInstructionId(1001L);
        rejectedLine.setCustomerColorNo("C01");
        rejectedLine.setColorName("藏青第1轮");
        rejectedLine.setLightPrimary("D65");
        rejectedLine.setStatus("REJECTED");

        ColorInstructionColorDO pendingLine = new ColorInstructionColorDO();
        pendingLine.setId(503L);
        pendingLine.setInstructionId(1001L);
        pendingLine.setCustomerColorNo("C02");
        pendingLine.setColorName("白色");
        pendingLine.setLightPrimary("D65");
        pendingLine.setStatus("PENDING");

        when(colorInstructionColorMapper.selectListByInstructionId(1001L)).thenReturn(List.of(approvedLine, rejectedLine, pendingLine));

        ColorInstructionColorSaveVO pendingVO = new ColorInstructionColorSaveVO();
        pendingVO.setId(503L);
        pendingVO.setCustomerColorNo("C02");
        pendingVO.setColorName("白色");
        pendingVO.setLightPrimary("D65");

        // 1. deletedColorIds 中显式删除 APPROVED 行 (501L)
        ColorInstructionSaveReqVO reqVO1 = new ColorInstructionSaveReqVO();
        reqVO1.setId(1001L);
        reqVO1.setCustomerId(101L);
        reqVO1.setProductId(201L);
        reqVO1.setColors(List.of(pendingVO));
        reqVO1.setDeletedColorIds(List.of(501L));

        ServiceException ex1 = assertThrows(ServiceException.class, () ->
                colorInstructionService.updateColorInstruction(reqVO1));
        assertEquals(COLOR_INSTRUCTION_LINE_FROZEN.getCode(), ex1.getCode());

        // 2. deletedColorIds 中显式删除 REJECTED 行 (502L)
        ColorInstructionSaveReqVO reqVO2 = new ColorInstructionSaveReqVO();
        reqVO2.setId(1001L);
        reqVO2.setCustomerId(101L);
        reqVO2.setProductId(201L);
        reqVO2.setColors(List.of(pendingVO));
        reqVO2.setDeletedColorIds(List.of(502L));

        ServiceException ex2 = assertThrows(ServiceException.class, () ->
                colorInstructionService.updateColorInstruction(reqVO2));
        assertEquals(COLOR_INSTRUCTION_LINE_FROZEN.getCode(), ex2.getCode());
    }

    @Test
    @DisplayName("更新打色单：差异保存三分流（未改行不更新、改行更新、新增行插入、缺失PENDING行删除、缺失冻结行保留）")
    void testUpdateColorInstruction_differentialSave_success() {
        ColorInstructionDO current = new ColorInstructionDO();
        current.setId(1001L);
        current.setStatus("IN_PROGRESS");
        current.setInstructionNo("DS20261013001");
        when(colorInstructionMapper.selectById(1001L)).thenReturn(current);

        CustomerDO customer = new CustomerDO();
        customer.setId(101L);
        customer.setCompanyName("优客集团");
        when(customerMapper.selectById(101L)).thenReturn(customer);

        ProductDO product = new ProductDO();
        product.setId(201L);
        product.setCode("P-101");
        product.setName("双面布");
        product.setComposition("100% 棉");
        when(productMapper.selectById(201L)).thenReturn(product);

        // 库内存量 4 行：
        // 行 1: APPROVED 冻结行，提交无改动（应保留、不触发 updateById）
        ColorInstructionColorDO row1 = new ColorInstructionColorDO();
        row1.setId(101L);
        row1.setInstructionId(1001L);
        row1.setLineNo(1);
        row1.setCustomerColorNo("C01");
        row1.setColorName("藏青");
        row1.setLightPrimary("D65");
        row1.setRound(1);
        row1.setStatus("APPROVED");

        // 行 2: PENDING 行，提交有改动（修改 remark，应触发 updateById）
        ColorInstructionColorDO row2 = new ColorInstructionColorDO();
        row2.setId(102L);
        row2.setInstructionId(1001L);
        row2.setLineNo(2);
        row2.setCustomerColorNo("C01");
        row2.setColorName("藏青");
        row2.setLightPrimary("D65");
        row2.setRound(2);
        row2.setStatus("PENDING");
        row2.setRemark("旧备注");

        // 行 3: PENDING 行，请求中缺失（应触发 deleteBatchIds）
        ColorInstructionColorDO row3 = new ColorInstructionColorDO();
        row3.setId(103L);
        row3.setInstructionId(1001L);
        row3.setLineNo(3);
        row3.setCustomerColorNo("C02");
        row3.setColorName("黑色");
        row3.setLightPrimary("D65");
        row3.setRound(1);
        row3.setStatus("PENDING");

        // 行 4: REJECTED 冻结行，请求中缺失（应保留在库中，不触发删除）
        ColorInstructionColorDO row4 = new ColorInstructionColorDO();
        row4.setId(104L);
        row4.setInstructionId(1001L);
        row4.setLineNo(4);
        row4.setCustomerColorNo("C03");
        row4.setColorName("灰色");
        row4.setLightPrimary("D65");
        row4.setRound(1);
        row4.setStatus("REJECTED");
        row4.setRejectReason("色偏暗");

        when(colorInstructionColorMapper.selectListByInstructionId(1001L)).thenReturn(List.of(row1, row2, row3, row4));

        // 提交参数：
        // 1) 未改动的 APPROVED 行 (101L)
        ColorInstructionColorSaveVO vo1 = new ColorInstructionColorSaveVO();
        vo1.setId(101L);
        vo1.setCustomerColorNo("C01");
        vo1.setColorName("藏青");
        vo1.setLightPrimary("D65");
        vo1.setRound(1);
        vo1.setStatus("APPROVED");

        // 2) 改动 remark 的 PENDING 行 (102L)
        ColorInstructionColorSaveVO vo2 = new ColorInstructionColorSaveVO();
        vo2.setId(102L);
        vo2.setCustomerColorNo("C01");
        vo2.setColorName("藏青");
        vo2.setLightPrimary("D65");
        vo2.setRound(2);
        vo2.setStatus("PENDING");
        vo2.setRemark("新备注");

        // 3) 新增行 (id=null, C04, 红色)
        ColorInstructionColorSaveVO voNew = new ColorInstructionColorSaveVO();
        voNew.setId(null);
        voNew.setCustomerColorNo("C04");
        voNew.setColorName("红色");
        voNew.setLightPrimary("TL84");

        ColorInstructionSaveReqVO reqVO = new ColorInstructionSaveReqVO();
        reqVO.setId(1001L);
        reqVO.setCustomerId(101L);
        reqVO.setProductId(201L);
        reqVO.setColors(List.of(vo1, vo2, voNew));

        colorInstructionService.updateColorInstruction(reqVO);

        // 验证主表更新
        verify(colorInstructionMapper, times(1)).updateById(any(ColorInstructionDO.class));

        // 验证删除：仅缺失的 PENDING 行 103L 被删除；缺失的 REJECTED 行 104L 不被删除
        ArgumentCaptor<List<Long>> deleteCaptor = ArgumentCaptor.forClass(List.class);
        verify(colorInstructionColorMapper, times(1)).deleteBatchIds(deleteCaptor.capture());
        assertEquals(List.of(103L), deleteCaptor.getValue());

        // 验证新增行插入：round=1, status=PENDING
        ArgumentCaptor<ColorInstructionColorDO> insertCaptor = ArgumentCaptor.forClass(ColorInstructionColorDO.class);
        verify(colorInstructionColorMapper, times(1)).insert(insertCaptor.capture());
        ColorInstructionColorDO inserted = insertCaptor.getValue();
        assertEquals("C04", inserted.getCustomerColorNo());
        assertEquals(1, inserted.getRound());
        assertEquals("PENDING", inserted.getStatus());
        assertEquals("TL84", inserted.getLightPrimary());

        // 验证更新行：
        // vo2 (102L) remark 改变，执行了 updateById
        // vo1 (101L) 未改动且处于正确顺序，不应调用 updateById（审计字段不变）
        ArgumentCaptor<ColorInstructionColorDO> updateCaptor = ArgumentCaptor.forClass(ColorInstructionColorDO.class);
        verify(colorInstructionColorMapper, atLeastOnce()).updateById(updateCaptor.capture());
        List<ColorInstructionColorDO> updatedList = updateCaptor.getAllValues();
        assertTrue(updatedList.stream().anyMatch(u -> u.getId().equals(102L) && "新备注".equals(u.getRemark())));
    }

    @Test
    @DisplayName("更新打色单：服务端按 customerColorNo 分组重排 lineNo（同色号多轮相邻、最新轮在前、从 1 连续编号）")
    void testUpdateColorInstruction_lineNoReorder_success() {
        ColorInstructionDO current = new ColorInstructionDO();
        current.setId(1001L);
        current.setStatus("PENDING");
        current.setInstructionNo("DS20261013001");
        when(colorInstructionMapper.selectById(1001L)).thenReturn(current);

        CustomerDO customer = new CustomerDO();
        customer.setId(101L);
        customer.setCompanyName("客户");
        when(customerMapper.selectById(101L)).thenReturn(customer);
        ProductDO product = new ProductDO();
        product.setId(201L);
        product.setCode("P01");
        product.setName("产品");
        when(productMapper.selectById(201L)).thenReturn(product);

        // 库内有色号 C01 的两轮行：
        // 轮 1 (REJECTED, id=101L, lineNo=1)
        ColorInstructionColorDO r1 = new ColorInstructionColorDO();
        r1.setId(101L);
        r1.setInstructionId(1001L);
        r1.setLineNo(2);
        r1.setCustomerColorNo("C01");
        r1.setColorName("藏青");
        r1.setLightPrimary("D65");
        r1.setRound(1);
        r1.setStatus("REJECTED");

        // 轮 2 (PENDING, id=102L, 初始 lineNo=1)
        ColorInstructionColorDO r2 = new ColorInstructionColorDO();
        r2.setId(102L);
        r2.setInstructionId(1001L);
        r2.setLineNo(1);
        r2.setCustomerColorNo("C01");
        r2.setColorName("藏青");
        r2.setLightPrimary("D65");
        r2.setRound(2);
        r2.setStatus("PENDING");

        when(colorInstructionColorMapper.selectListByInstructionId(1001L)).thenReturn(List.of(r1, r2));

        // 用户提交：
        // 先提交色号 C02 的新行（无 id）
        ColorInstructionColorSaveVO c02 = new ColorInstructionColorSaveVO();
        c02.setId(null);
        c02.setCustomerColorNo("C02");
        c02.setColorName("米白");
        c02.setLightPrimary("D65");

        // 接着提交 C01 轮 1 和 轮 2（注意提交顺序故意写反）
        ColorInstructionColorSaveVO c01r1 = new ColorInstructionColorSaveVO();
        c01r1.setId(101L);
        c01r1.setCustomerColorNo("C01");
        c01r1.setColorName("藏青");
        c01r1.setLightPrimary("D65");
        c01r1.setRound(1);
        c01r1.setStatus("REJECTED");

        ColorInstructionColorSaveVO c01r2 = new ColorInstructionColorSaveVO();
        c01r2.setId(102L);
        c01r2.setCustomerColorNo("C01");
        c01r2.setColorName("藏青");
        c01r2.setLightPrimary("D65");
        c01r2.setRound(2);
        c01r2.setStatus("PENDING");

        ColorInstructionSaveReqVO reqVO = new ColorInstructionSaveReqVO();
        reqVO.setId(1001L);
        reqVO.setCustomerId(101L);
        reqVO.setProductId(201L);
        // 提交顺序：C02 先，然后 C01 轮 1，再 C01 轮 2
        reqVO.setColors(List.of(c02, c01r1, c01r2));

        colorInstructionService.updateColorInstruction(reqVO);

        // 校验新增的 C02 行：排在组首，lineNo 应为 1
        ArgumentCaptor<ColorInstructionColorDO> insertCaptor = ArgumentCaptor.forClass(ColorInstructionColorDO.class);
        verify(colorInstructionColorMapper).insert(insertCaptor.capture());
        ColorInstructionColorDO inserted = insertCaptor.getValue();
        assertEquals("C02", inserted.getCustomerColorNo());
        assertEquals(1, inserted.getLineNo());

        // 校验 C01 的多轮行：最新轮（round=2）在前（lineNo=2），旧轮（round=1）在后（lineNo=3）
        ArgumentCaptor<ColorInstructionColorDO> updateCaptor = ArgumentCaptor.forClass(ColorInstructionColorDO.class);
        verify(colorInstructionColorMapper, atLeastOnce()).updateById(updateCaptor.capture());
        List<ColorInstructionColorDO> updatedRows = updateCaptor.getAllValues();

        // r2 (round=2) lineNo 应为 2
        ColorInstructionColorDO updatedR2 = updatedRows.stream().filter(u -> u.getId().equals(102L)).findFirst().orElse(null);
        assertNotNull(updatedR2);
        assertEquals(2, updatedR2.getLineNo());

        // r1 (round=1) lineNo 原来是 1，现在重排为 3
        ColorInstructionColorDO updatedR1 = updatedRows.stream().filter(u -> u.getId().equals(101L)).findFirst().orElse(null);
        assertNotNull(updatedR1);
        assertEquals(3, updatedR1.getLineNo());
    }

    @Test
    @DisplayName("更新打色单：颜色行 ID 同时出现在更新列表与显式删除列表中报参数非法异常")
    void testUpdateColorInstruction_conflictDeleteAndUpdateThrowsException() {
        ColorInstructionDO order = new ColorInstructionDO();
        order.setId(100L);
        order.setInstructionNo("DS20261013001");
        order.setStatus("PENDING");
        when(colorInstructionMapper.selectById(100L)).thenReturn(order);

        CustomerDO customer = new CustomerDO();
        customer.setId(1001L);
        customer.setCompanyName("客户A");
        when(customerMapper.selectById(1001L)).thenReturn(customer);

        ProductDO product = new ProductDO();
        product.setId(2001L);
        product.setCode("P001");
        product.setName("双面布");
        when(productMapper.selectById(2001L)).thenReturn(product);

        ColorInstructionColorDO row1 = new ColorInstructionColorDO();
        row1.setId(11L);
        row1.setInstructionId(100L);
        row1.setStatus("PENDING");
        row1.setCustomerColorNo("C01");
        when(colorInstructionColorMapper.selectListByInstructionId(100L)).thenReturn(List.of(row1));

        ColorInstructionSaveReqVO updateReq = new ColorInstructionSaveReqVO();
        updateReq.setId(100L);
        updateReq.setCustomerId(1001L);
        updateReq.setProductId(2001L);

        ColorInstructionColorSaveVO vo1 = new ColorInstructionColorSaveVO();
        vo1.setId(11L);
        vo1.setCustomerColorNo("C01");
        vo1.setColorName("黑色");
        vo1.setLightPrimary("D65");
        updateReq.setColors(List.of(vo1));
        updateReq.setDeletedColorIds(List.of(11L));

        assertThrows(ServiceException.class, () -> colorInstructionService.updateColorInstruction(updateReq));
    }

    // ==================== Issue 04: 开始打色 (Slice 1) ====================

    @Test
    @DisplayName("开始打色：单据不存在抛 COLOR_INSTRUCTION_NOT_EXISTS (1_030_032_000)")
    void testStartColorInstruction_notExists() {
        when(colorInstructionMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> colorInstructionService.startColorInstruction(999L));
        assertEquals(COLOR_INSTRUCTION_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("开始打色：正常流转 PENDING -> IN_PROGRESS，走条件更新")
    void testStartColorInstruction_success() {
        ColorInstructionDO order = new ColorInstructionDO();
        order.setId(100L);
        order.setInstructionNo("DS20261013001");
        order.setStatus("PENDING");
        when(colorInstructionMapper.selectById(100L)).thenReturn(order);
        when(colorInstructionMapper.update(any(), any())).thenReturn(1);

        colorInstructionService.startColorInstruction(100L);

        verify(colorInstructionMapper).update(any(), any());
    }

    @Test
    @DisplayName("开始打色：非 PENDING 状态或条件更新影响 0 行抛 COLOR_INSTRUCTION_STATUS_NOT_ALLOWED (1_030_032_002)")
    void testStartColorInstruction_statusNotAllowed() {
        ColorInstructionDO order = new ColorInstructionDO();
        order.setId(100L);
        order.setInstructionNo("DS20261013001");
        order.setStatus("CONFIRMED");
        when(colorInstructionMapper.selectById(100L)).thenReturn(order);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> colorInstructionService.startColorInstruction(100L));
        assertEquals(COLOR_INSTRUCTION_STATUS_NOT_ALLOWED.getCode(), ex.getCode());

        // 测试 PENDING 态但在条件更新时并发被改（影响行数 0）
        order.setStatus("PENDING");
        when(colorInstructionMapper.update(any(), any())).thenReturn(0);

        ServiceException ex2 = assertThrows(ServiceException.class,
                () -> colorInstructionService.startColorInstruction(100L));
        assertEquals(COLOR_INSTRUCTION_STATUS_NOT_ALLOWED.getCode(), ex2.getCode());
    }

    // ==================== Issue 04: 通过颜色行 (Slice 2) ====================

    @Test
    @DisplayName("通过颜色行：行不存在抛 COLOR_INSTRUCTION_COLOR_NOT_EXISTS (1_030_032_100)")
    void testApproveColorLine_lineNotExists() {
        when(colorInstructionColorMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> colorInstructionService.approveColorLine(999L));
        assertEquals(COLOR_INSTRUCTION_COLOR_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("通过颜色行：主单不存在抛 COLOR_INSTRUCTION_NOT_EXISTS (1_030_032_000)")
    void testApproveColorLine_orderNotExists() {
        ColorInstructionColorDO row = new ColorInstructionColorDO();
        row.setId(11L);
        row.setInstructionId(100L);
        row.setStatus("PENDING");
        when(colorInstructionColorMapper.selectById(11L)).thenReturn(row);
        when(colorInstructionMapper.selectById(100L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> colorInstructionService.approveColorLine(11L));
        assertEquals(COLOR_INSTRUCTION_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("通过颜色行：主单状态为 CONFIRMED/VOID 等不可编辑态抛 COLOR_INSTRUCTION_STATUS_NOT_ALLOWED (1_030_032_002)")
    void testApproveColorLine_orderStatusNotAllowed() {
        ColorInstructionColorDO row = new ColorInstructionColorDO();
        row.setId(11L);
        row.setInstructionId(100L);
        row.setStatus("PENDING");
        when(colorInstructionColorMapper.selectById(11L)).thenReturn(row);

        ColorInstructionDO order = new ColorInstructionDO();
        order.setId(100L);
        order.setStatus("CONFIRMED");
        when(colorInstructionMapper.selectById(100L)).thenReturn(order);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> colorInstructionService.approveColorLine(11L));
        assertEquals(COLOR_INSTRUCTION_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("通过颜色行：单据仍 PENDING 时自动触发便利规则转 IN_PROGRESS")
    void testApproveColorLine_convenienceRuleOrderPendingToInProgress() {
        ColorInstructionColorDO row = new ColorInstructionColorDO();
        row.setId(11L);
        row.setInstructionId(100L);
        row.setCustomerColorNo("C01");
        row.setColorName("黑色");
        row.setStatus("PENDING");
        when(colorInstructionColorMapper.selectById(11L)).thenReturn(row);

        ColorInstructionDO order = new ColorInstructionDO();
        order.setId(100L);
        order.setInstructionNo("DS20261013001");
        order.setStatus("PENDING");
        when(colorInstructionMapper.selectById(100L)).thenReturn(order);

        when(colorInstructionColorMapper.update(any(), any())).thenReturn(1);
        when(colorInstructionMapper.update(any(), any())).thenReturn(1);

        colorInstructionService.approveColorLine(11L);

        // 验证行状态被条件更新
        verify(colorInstructionColorMapper).update(any(), any());
        // 验证主单状态被条件更新转 IN_PROGRESS
        verify(colorInstructionMapper).update(any(), any());
    }

    @Test
    @DisplayName("通过颜色行：单据已是 IN_PROGRESS 时无需流转主单")
    void testApproveColorLine_orderAlreadyInProgress() {
        ColorInstructionColorDO row = new ColorInstructionColorDO();
        row.setId(11L);
        row.setInstructionId(100L);
        row.setCustomerColorNo("C01");
        row.setColorName("黑色");
        row.setStatus("PENDING");
        when(colorInstructionColorMapper.selectById(11L)).thenReturn(row);

        ColorInstructionDO order = new ColorInstructionDO();
        order.setId(100L);
        order.setInstructionNo("DS20261013001");
        order.setStatus("IN_PROGRESS");
        when(colorInstructionMapper.selectById(100L)).thenReturn(order);

        when(colorInstructionColorMapper.update(any(), any())).thenReturn(1);

        colorInstructionService.approveColorLine(11L);

        verify(colorInstructionColorMapper).update(any(), any());
        verify(colorInstructionMapper, never()).update(any(), any());
    }

    @Test
    @DisplayName("通过颜色行：重复 approve 或条件更新影响 0 行抛 COLOR_INSTRUCTION_STATUS_NOT_ALLOWED (1_030_032_002)")
    void testApproveColorLine_idempotentAndZeroRowsUpdated() {
        ColorInstructionColorDO row = new ColorInstructionColorDO();
        row.setId(11L);
        row.setInstructionId(100L);
        row.setCustomerColorNo("C01");
        row.setStatus("APPROVED"); // 已经是通过状态
        when(colorInstructionColorMapper.selectById(11L)).thenReturn(row);

        ColorInstructionDO order = new ColorInstructionDO();
        order.setId(100L);
        order.setStatus("IN_PROGRESS");
        when(colorInstructionMapper.selectById(100L)).thenReturn(order);

        // 状态不是 PENDING 直接抛出状态不允许
        ServiceException ex = assertThrows(ServiceException.class,
                () -> colorInstructionService.approveColorLine(11L));
        assertEquals(COLOR_INSTRUCTION_STATUS_NOT_ALLOWED.getCode(), ex.getCode());

        // 测试虽然查询为 PENDING，但并发更新返回 0 行
        row.setStatus("PENDING");
        when(colorInstructionColorMapper.update(any(), any())).thenReturn(0);

        ServiceException ex2 = assertThrows(ServiceException.class,
                () -> colorInstructionService.approveColorLine(11L));
        assertEquals(COLOR_INSTRUCTION_STATUS_NOT_ALLOWED.getCode(), ex2.getCode());
    }

    // ==================== Issue 04: 驳回重打颜色行 (Slice 3) ====================

    @Test
    @DisplayName("驳回重打：行不存在抛 COLOR_INSTRUCTION_COLOR_NOT_EXISTS (1_030_032_100)")
    void testRejectRestrikeColorLine_lineNotExists() {
        when(colorInstructionColorMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> colorInstructionService.rejectRestrikeColorLine(999L, "颜色偏浅"));
        assertEquals(COLOR_INSTRUCTION_COLOR_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("驳回重打：主单不存在抛 COLOR_INSTRUCTION_NOT_EXISTS (1_030_032_000)")
    void testRejectRestrikeColorLine_orderNotExists() {
        ColorInstructionColorDO row = new ColorInstructionColorDO();
        row.setId(11L);
        row.setInstructionId(100L);
        row.setStatus("PENDING");
        when(colorInstructionColorMapper.selectById(11L)).thenReturn(row);
        when(colorInstructionMapper.selectById(100L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> colorInstructionService.rejectRestrikeColorLine(11L, "颜色偏浅"));
        assertEquals(COLOR_INSTRUCTION_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("驳回重打：主单状态为 CONFIRMED/VOID 等不可编辑态抛 COLOR_INSTRUCTION_STATUS_NOT_ALLOWED (1_030_032_002)")
    void testRejectRestrikeColorLine_orderStatusNotAllowed() {
        ColorInstructionColorDO row = new ColorInstructionColorDO();
        row.setId(11L);
        row.setInstructionId(100L);
        row.setStatus("PENDING");
        when(colorInstructionColorMapper.selectById(11L)).thenReturn(row);

        ColorInstructionDO order = new ColorInstructionDO();
        order.setId(100L);
        order.setStatus("CONFIRMED");
        when(colorInstructionMapper.selectById(100L)).thenReturn(order);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> colorInstructionService.rejectRestrikeColorLine(11L, "颜色偏浅"));
        assertEquals(COLOR_INSTRUCTION_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("驳回重打：旧行状态已为 REJECTED 冻结行，不可再重打，抛 COLOR_INSTRUCTION_LINE_FROZEN (1_030_032_006)")
    void testRejectRestrikeColorLine_rejectedLineFrozenCannotRestrike() {
        ColorInstructionColorDO row = new ColorInstructionColorDO();
        row.setId(11L);
        row.setInstructionId(100L);
        row.setStatus("REJECTED");
        when(colorInstructionColorMapper.selectById(11L)).thenReturn(row);

        ColorInstructionDO order = new ColorInstructionDO();
        order.setId(100L);
        order.setStatus("IN_PROGRESS");
        when(colorInstructionMapper.selectById(100L)).thenReturn(order);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> colorInstructionService.rejectRestrikeColorLine(11L, "再次驳回"));
        assertEquals(COLOR_INSTRUCTION_LINE_FROZEN.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("驳回重打：合法流转 PENDING -> REJECTED + 克隆新行（round+1、样布照片不继承、重排 lineNo）")
    void testRejectRestrikeColorLine_pendingToRejectedAndClone() {
        ColorInstructionColorDO oldRow = new ColorInstructionColorDO();
        oldRow.setId(11L);
        oldRow.setInstructionId(100L);
        oldRow.setLineNo(1);
        oldRow.setCustomerColorNo("C01");
        oldRow.setColorName("藏青");
        oldRow.setFactoryColorNo("F-01");
        oldRow.setColorHex("#000080");
        oldRow.setLightPrimary("D65");
        oldRow.setLightSecondary("TL84");
        oldRow.setColorCardRef("Pantone TPX 19-4052");
        oldRow.setSamplePhotos(List.of("https://oss.example.com/photo1.jpg"));
        oldRow.setCopies(3);
        oldRow.setRound(1);
        oldRow.setStatus("PENDING");
        when(colorInstructionColorMapper.selectById(11L)).thenReturn(oldRow);

        ColorInstructionDO order = new ColorInstructionDO();
        order.setId(100L);
        order.setInstructionNo("DS20261013001");
        order.setStatus("PENDING");
        when(colorInstructionMapper.selectById(100L)).thenReturn(order);

        // 条件更新旧行成功
        when(colorInstructionColorMapper.update(any(), any())).thenReturn(1);
        // 主单条件更新转 IN_PROGRESS 成功
        when(colorInstructionMapper.update(any(), any())).thenReturn(1);

        // 模拟插入克隆新行后生成 ID
        doAnswer(invocation -> {
            ColorInstructionColorDO inserted = invocation.getArgument(0);
            inserted.setId(12L);
            return 1;
        }).when(colorInstructionColorMapper).insert(any(ColorInstructionColorDO.class));

        // 模拟全量查询存量颜色行用于重排（插入前库内仅有旧行）
        when(colorInstructionColorMapper.selectListByInstructionId(100L))
                .thenReturn(new java.util.ArrayList<>(List.of(oldRow)));

        colorInstructionService.rejectRestrikeColorLine(11L, "颜色偏深，需调浅半度");

        // 1. 验证克隆行继承字段与特性
        ArgumentCaptor<ColorInstructionColorDO> insertCaptor = ArgumentCaptor.forClass(ColorInstructionColorDO.class);
        verify(colorInstructionColorMapper).insert(insertCaptor.capture());
        ColorInstructionColorDO cloned = insertCaptor.getValue();
        assertEquals("C01", cloned.getCustomerColorNo());
        assertEquals("藏青", cloned.getColorName());
        assertEquals("F-01", cloned.getFactoryColorNo());
        assertEquals("#000080", cloned.getColorHex());
        assertEquals("D65", cloned.getLightPrimary());
        assertEquals("TL84", cloned.getLightSecondary());
        assertEquals("Pantone TPX 19-4052", cloned.getColorCardRef());
        assertEquals(3, cloned.getCopies());
        assertEquals(2, cloned.getRound(), "round 应当自增为 旧行 round + 1 = 2");
        assertEquals("PENDING", cloned.getStatus());
        assertEquals(1, cloned.getLineNo(), "最新轮应当排在前面并赋 lineNo = 1");
        assertNull(cloned.getSamplePhotos(), "samplePhotos 必须不继承");
        assertNull(cloned.getRejectReason(), "克隆新行不能继承驳回原因");

        // 验证旧行被更新为 lineNo = 2
        ArgumentCaptor<ColorInstructionColorDO> updateOldCaptor = ArgumentCaptor.forClass(ColorInstructionColorDO.class);
        verify(colorInstructionColorMapper).updateById(updateOldCaptor.capture());
        assertEquals(11L, updateOldCaptor.getValue().getId());
        assertEquals(2, updateOldCaptor.getValue().getLineNo(), "旧行应被重排为 lineNo = 2");

        // 2. 验证主单触发便利规则 PENDING -> IN_PROGRESS
        verify(colorInstructionMapper).update(any(), any());
    }

    @Test
    @DisplayName("驳回重打：合法流转 APPROVED -> REJECTED + 克隆新行（电话先通过后改口场景）")
    void testRejectRestrikeColorLine_approvedToRejectedAndClone() {
        ColorInstructionColorDO oldRow = new ColorInstructionColorDO();
        oldRow.setId(11L);
        oldRow.setInstructionId(100L);
        oldRow.setLineNo(1);
        oldRow.setCustomerColorNo("C01");
        oldRow.setColorName("藏青");
        oldRow.setLightPrimary("D65");
        oldRow.setRound(1);
        oldRow.setStatus("APPROVED"); // 已经通过的行
        when(colorInstructionColorMapper.selectById(11L)).thenReturn(oldRow);

        ColorInstructionDO order = new ColorInstructionDO();
        order.setId(100L);
        order.setInstructionNo("DS20261013001");
        order.setStatus("IN_PROGRESS");
        when(colorInstructionMapper.selectById(100L)).thenReturn(order);

        when(colorInstructionColorMapper.update(any(), any())).thenReturn(1);
        doAnswer(inv -> {
            ColorInstructionColorDO inserted = inv.getArgument(0);
            inserted.setId(12L);
            return 1;
        }).when(colorInstructionColorMapper).insert(any(ColorInstructionColorDO.class));

        // 插入前库内仅有旧行
        when(colorInstructionColorMapper.selectListByInstructionId(100L))
                .thenReturn(new java.util.ArrayList<>(List.of(oldRow)));

        colorInstructionService.rejectRestrikeColorLine(11L, "对色室二次复核偏红，驳回重打");

        ArgumentCaptor<ColorInstructionColorDO> insertCaptor = ArgumentCaptor.forClass(ColorInstructionColorDO.class);
        verify(colorInstructionColorMapper).insert(insertCaptor.capture());
        ColorInstructionColorDO cloned = insertCaptor.getValue();
        assertEquals(2, cloned.getRound());
        assertEquals("PENDING", cloned.getStatus());
        assertEquals(1, cloned.getLineNo());
    }

    @Test
    @DisplayName("驳回重打：并发更新旧行影响 0 行时抛异常")
    void testRejectRestrikeColorLine_concurrencyZeroRowsUpdated() {
        ColorInstructionColorDO oldRow = new ColorInstructionColorDO();
        oldRow.setId(11L);
        oldRow.setInstructionId(100L);
        oldRow.setCustomerColorNo("C01");
        oldRow.setStatus("PENDING");
        when(colorInstructionColorMapper.selectById(11L)).thenReturn(oldRow);

        ColorInstructionDO order = new ColorInstructionDO();
        order.setId(100L);
        order.setStatus("IN_PROGRESS");
        when(colorInstructionMapper.selectById(100L)).thenReturn(order);

        // 条件更新返回 0 行
        when(colorInstructionColorMapper.update(any(), any())).thenReturn(0);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> colorInstructionService.rejectRestrikeColorLine(11L, "驳回意见"));
        assertEquals(COLOR_INSTRUCTION_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("切面注解：start / approve / rejectRestrike 正确标注 @LogRecord")
    void testLogRecordAnnotations() throws NoSuchMethodException {
        var startMethod = ColorInstructionServiceImpl.class.getMethod("startColorInstruction", Long.class);
        var startLog = startMethod.getAnnotation(com.mzt.logapi.starter.annotation.LogRecord.class);
        assertNotNull(startLog, "startColorInstruction 必须标注 @LogRecord");
        assertEquals("COLOR_INSTRUCTION", startLog.type());
        assertEquals("START", startLog.subType());
        assertEquals("{{#id}}", startLog.bizNo());

        var approveMethod = ColorInstructionServiceImpl.class.getMethod("approveColorLine", Long.class);
        var approveLog = approveMethod.getAnnotation(com.mzt.logapi.starter.annotation.LogRecord.class);
        assertNotNull(approveLog, "approveColorLine 必须标注 @LogRecord");
        assertEquals("COLOR_INSTRUCTION", approveLog.type());
        assertEquals("APPROVE_LINE", approveLog.subType());
        assertEquals("{{#orderId}}", approveLog.bizNo());

        var rejectMethod = ColorInstructionServiceImpl.class.getMethod("rejectRestrikeColorLine", Long.class, String.class);
        var rejectLog = rejectMethod.getAnnotation(com.mzt.logapi.starter.annotation.LogRecord.class);
        assertNotNull(rejectLog, "rejectRestrikeColorLine 必须标注 @LogRecord");
        assertEquals("COLOR_INSTRUCTION", rejectLog.type());
        assertEquals("REJECT_RESTRIKE_LINE", rejectLog.subType());
        assertEquals("{{#orderId}}", rejectLog.bizNo());
    }

    // ==================== Issue 05: 确认动作与三道守卫 ====================

    @Test
    @DisplayName("确认打色指示单：单据不存在抛 COLOR_INSTRUCTION_NOT_EXISTS (1_030_032_000)")
    void testConfirmColorInstruction_notExists_throwsException() {
        ColorInstructionConfirmReqVO reqVO = new ColorInstructionConfirmReqVO();
        reqVO.setId(999L);
        reqVO.setConfirmType("CUSTOMER");

        when(colorInstructionMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                colorInstructionService.confirmColorInstruction(reqVO));
        assertEquals(COLOR_INSTRUCTION_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("确认打色指示单：守卫一——单据非 IN_PROGRESS 状态（PENDING/CONFIRMED重复确认/VOID）抛 COLOR_INSTRUCTION_STATUS_NOT_ALLOWED (1_030_032_002)")
    void testConfirmColorInstruction_guard1_notInProgress_throwsException() {
        ColorInstructionConfirmReqVO reqVO = new ColorInstructionConfirmReqVO();
        reqVO.setId(100L);
        reqVO.setConfirmType("CUSTOMER");

        // 1. PENDING 状态
        ColorInstructionDO pendingOrder = new ColorInstructionDO();
        pendingOrder.setId(100L);
        pendingOrder.setStatus("PENDING");
        when(colorInstructionMapper.selectById(100L)).thenReturn(pendingOrder);

        ServiceException ex1 = assertThrows(ServiceException.class, () ->
                colorInstructionService.confirmColorInstruction(reqVO));
        assertEquals(COLOR_INSTRUCTION_STATUS_NOT_ALLOWED.getCode(), ex1.getCode());

        // 2. CONFIRMED 重复确认
        ColorInstructionDO confirmedOrder = new ColorInstructionDO();
        confirmedOrder.setId(100L);
        confirmedOrder.setStatus("CONFIRMED");
        when(colorInstructionMapper.selectById(100L)).thenReturn(confirmedOrder);

        ServiceException ex2 = assertThrows(ServiceException.class, () ->
                colorInstructionService.confirmColorInstruction(reqVO));
        assertEquals(COLOR_INSTRUCTION_STATUS_NOT_ALLOWED.getCode(), ex2.getCode());

        // 3. VOID 作废状态
        ColorInstructionDO voidOrder = new ColorInstructionDO();
        voidOrder.setId(100L);
        voidOrder.setStatus("VOID");
        when(colorInstructionMapper.selectById(100L)).thenReturn(voidOrder);

        ServiceException ex3 = assertThrows(ServiceException.class, () ->
                colorInstructionService.confirmColorInstruction(reqVO));
        assertEquals(COLOR_INSTRUCTION_STATUS_NOT_ALLOWED.getCode(), ex3.getCode());
    }

    @Test
    @DisplayName("确认打色指示单：守卫二——存在 PENDING 行抛 COLOR_INSTRUCTION_CONFIRM_CONDITION_NOT_MET (1_030_032_004)")
    void testConfirmColorInstruction_guard2_hasPendingLine_throwsException() {
        ColorInstructionConfirmReqVO reqVO = new ColorInstructionConfirmReqVO();
        reqVO.setId(100L);
        reqVO.setConfirmType("CUSTOMER");

        ColorInstructionDO order = new ColorInstructionDO();
        order.setId(100L);
        order.setStatus("IN_PROGRESS");
        when(colorInstructionMapper.selectById(100L)).thenReturn(order);

        // 行1 APPROVED，行2 PENDING（存在未决行）
        ColorInstructionColorDO line1 = new ColorInstructionColorDO();
        line1.setId(1L);
        line1.setStatus("APPROVED");
        ColorInstructionColorDO line2 = new ColorInstructionColorDO();
        line2.setId(2L);
        line2.setStatus("PENDING");

        when(colorInstructionColorMapper.selectList(any())).thenReturn(List.of(line1, line2));

        ServiceException ex = assertThrows(ServiceException.class, () ->
                colorInstructionService.confirmColorInstruction(reqVO));
        assertEquals(COLOR_INSTRUCTION_CONFIRM_CONDITION_NOT_MET.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("确认打色指示单：守卫三——无 APPROVED 行（全部 REJECTED 或无颜色行）抛 COLOR_INSTRUCTION_CONFIRM_CONDITION_NOT_MET (1_030_032_004)")
    void testConfirmColorInstruction_guard3_allRejectedNoApproved_throwsException() {
        ColorInstructionConfirmReqVO reqVO = new ColorInstructionConfirmReqVO();
        reqVO.setId(100L);
        reqVO.setConfirmType("CUSTOMER");

        ColorInstructionDO order = new ColorInstructionDO();
        order.setId(100L);
        order.setStatus("IN_PROGRESS");
        when(colorInstructionMapper.selectById(100L)).thenReturn(order);

        // 情况1：行全为 REJECTED
        ColorInstructionColorDO line1 = new ColorInstructionColorDO();
        line1.setId(1L);
        line1.setStatus("REJECTED");
        ColorInstructionColorDO line2 = new ColorInstructionColorDO();
        line2.setId(2L);
        line2.setStatus("REJECTED");

        when(colorInstructionColorMapper.selectList(any())).thenReturn(List.of(line1, line2));

        ServiceException ex1 = assertThrows(ServiceException.class, () ->
                colorInstructionService.confirmColorInstruction(reqVO));
        assertEquals(COLOR_INSTRUCTION_CONFIRM_CONDITION_NOT_MET.getCode(), ex1.getCode());

        // 情况2：无颜色行
        when(colorInstructionColorMapper.selectList(any())).thenReturn(List.of());
        ServiceException ex2 = assertThrows(ServiceException.class, () ->
                colorInstructionService.confirmColorInstruction(reqVO));
        assertEquals(COLOR_INSTRUCTION_CONFIRM_CONDITION_NOT_MET.getCode(), ex2.getCode());
    }

    @Test
    @DisplayName("确认打色指示单：confirmType 非法枚举报 COLOR_INSTRUCTION_INVALID_CONFIRM_TYPE_OR_LIGHT (1_030_032_007)")
    void testConfirmColorInstruction_invalidConfirmType_throwsException() {
        ColorInstructionConfirmReqVO reqVO = new ColorInstructionConfirmReqVO();
        reqVO.setId(100L);
        reqVO.setConfirmType("UNKNOWN_TYPE"); // 非法枚举

        ColorInstructionDO order = new ColorInstructionDO();
        order.setId(100L);
        order.setStatus("IN_PROGRESS");
        when(colorInstructionMapper.selectById(100L)).thenReturn(order);

        ColorInstructionColorDO line1 = new ColorInstructionColorDO();
        line1.setId(1L);
        line1.setStatus("APPROVED");
        when(colorInstructionColorMapper.selectList(any())).thenReturn(List.of(line1));

        ServiceException ex = assertThrows(ServiceException.class, () ->
                colorInstructionService.confirmColorInstruction(reqVO));
        assertEquals(COLOR_INSTRUCTION_INVALID_CONFIRM_TYPE_OR_LIGHT.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("确认打色指示单：条件更新防并发覆盖（影响 0 行时抛 1_030_032_002）")
    void testConfirmColorInstruction_concurrencyZeroRowsUpdated_throwsException() {
        ColorInstructionConfirmReqVO reqVO = new ColorInstructionConfirmReqVO();
        reqVO.setId(100L);
        reqVO.setConfirmType("INTERNAL");

        ColorInstructionDO order = new ColorInstructionDO();
        order.setId(100L);
        order.setStatus("IN_PROGRESS");
        when(colorInstructionMapper.selectById(100L)).thenReturn(order);

        ColorInstructionColorDO line1 = new ColorInstructionColorDO();
        line1.setId(1L);
        line1.setStatus("APPROVED");
        when(colorInstructionColorMapper.selectList(any())).thenReturn(List.of(line1));

        // 模拟条件更新因并发或状态已变化返回 0 行
        when(colorInstructionMapper.update(any(ColorInstructionDO.class), any())).thenReturn(0);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                colorInstructionService.confirmColorInstruction(reqVO));
        assertEquals(COLOR_INSTRUCTION_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("确认打色指示单：三道守卫通过，写入 confirmType/customerFeedback/confirmedBy/confirmedAt 四字段，status=CONFIRMED")
    void testConfirmColorInstruction_success_writesFourFieldsAndConfirmed() {
        try {
            Long testUserId = 888L;
            LoginUser loginUser = new LoginUser();
            loginUser.setId(testUserId);
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(loginUser, null, List.of())
            );

            ColorInstructionConfirmReqVO reqVO = new ColorInstructionConfirmReqVO();
            reqVO.setId(100L);
            reqVO.setConfirmType("CUSTOMER");
            reqVO.setCustomerFeedback("客户确认通过，打色光泽度符合要求");

            ColorInstructionDO order = new ColorInstructionDO();
            order.setId(100L);
            order.setInstructionNo("DS20261013001");
            order.setStatus("IN_PROGRESS");
            when(colorInstructionMapper.selectById(100L)).thenReturn(order);

            // 行1 APPROVED，行2 REJECTED（无 PENDING，至少 1 行 APPROVED）
            ColorInstructionColorDO line1 = new ColorInstructionColorDO();
            line1.setId(1L);
            line1.setStatus("APPROVED");
            ColorInstructionColorDO line2 = new ColorInstructionColorDO();
            line2.setId(2L);
            line2.setStatus("REJECTED");
            when(colorInstructionColorMapper.selectList(any())).thenReturn(List.of(line1, line2));

            when(colorInstructionMapper.update(any(ColorInstructionDO.class), any())).thenReturn(1);

            colorInstructionService.confirmColorInstruction(reqVO);

            // 捕获更新对象与条件
            ArgumentCaptor<ColorInstructionDO> updateCaptor = ArgumentCaptor.forClass(ColorInstructionDO.class);
            ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.Wrapper<ColorInstructionDO>> wrapperCaptor =
                    ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.Wrapper.class);
            verify(colorInstructionMapper).update(updateCaptor.capture(), wrapperCaptor.capture());

            ColorInstructionDO updated = updateCaptor.getValue();
            assertEquals("CONFIRMED", updated.getStatus(), "单据状态应更新为 CONFIRMED");
            assertEquals("CUSTOMER", updated.getConfirmType(), "确认类型应写入 CUSTOMER");
            assertEquals("客户确认通过，打色光泽度符合要求", updated.getCustomerFeedback(), "客户反馈意见应正确写入");
            assertEquals(testUserId, updated.getConfirmedBy(), "confirmedBy 应写入当前登录用户 ID");
            assertNotNull(updated.getConfirmedAt(), "confirmedAt 应写入当前时间");

            // 检查条件更新 Wrapper 包含 IN_PROGRESS 状态守卫
            com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ColorInstructionDO> wrapper =
                    (com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ColorInstructionDO>) wrapperCaptor.getValue();
            String sqlSegment = wrapper.getSqlSegment();
            assertTrue(sqlSegment.contains("status ="), "条件更新必须包含 status='IN_PROGRESS'");
            assertTrue(wrapper.getParamNameValuePairs().containsValue("IN_PROGRESS"), "条件参数中必须包含 IN_PROGRESS 状态");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("切面注解：confirm 接口正确标注 @LogRecord(subType = 'CONFIRM')")
    void testConfirmLogRecordAnnotation() throws NoSuchMethodException {
        var confirmMethod = ColorInstructionServiceImpl.class.getMethod("confirmColorInstruction", ColorInstructionConfirmReqVO.class);
        var confirmLog = confirmMethod.getAnnotation(com.mzt.logapi.starter.annotation.LogRecord.class);
        assertNotNull(confirmLog, "confirmColorInstruction 必须标注 @LogRecord");
        assertEquals("COLOR_INSTRUCTION", confirmLog.type());
        assertEquals("CONFIRM", confirmLog.subType());
        assertEquals("{{#reqVO.id}}", confirmLog.bizNo());
        assertEquals("确认打色指示单 {{#instructionNo}}", confirmLog.success());
    }

    @Test
    @DisplayName("打色单列表 Wrapper：keyword 4字段模糊 OR 与 statusList 多选过滤")
    void testBuildQueryWrapper_keywordAndStatusListFilters() {
        ColorInstructionPageReqVO reqVO = new ColorInstructionPageReqVO();
        reqVO.setKeyword("ABC");
        reqVO.setStatusList(List.of("CONFIRMED", "IN_PRODUCTION"));

        LambdaQueryWrapperX<ColorInstructionDO> wrapper = ColorInstructionMapper.buildQueryWrapper(reqVO);
        String sql = wrapper.getSqlSegment();
        assertNotNull(sql);
        assertTrue(sql.contains("LOWER(instruction_no) LIKE"), "Wrapper 必须包含 instruction_no 模糊匹配");
        assertTrue(sql.contains("LOWER(client_name) LIKE"), "Wrapper 必须包含 client_name 模糊匹配");
        assertTrue(sql.contains("LOWER(product_code) LIKE"), "Wrapper 必须包含 product_code 模糊匹配");
        assertTrue(sql.contains("LOWER(product_name) LIKE"), "Wrapper 必须包含 product_name 模糊匹配");
        assertTrue(sql.contains("status IN"), "Wrapper 必须包含 status IN 集合过滤");

        var paramValues = wrapper.getParamNameValuePairs().values();
        assertTrue(paramValues.contains("%ABC%"), "参数必须包含 %ABC%");
    }

    // ==================== 下游引用通用校验（validateColorInstructionForDownstream） ====================

    @Test
    @DisplayName("下游引用校验：colorInstructionId 为空直接返回 null")
    void testValidateColorInstructionForDownstream_nullId_returnsNull() {
        var result = colorInstructionService.validateColorInstructionForDownstream(
                null, 100L, 200L, "P001",
                new com.luna.lunasrv.framework.common.exception.ErrorCode(1_030_006_002, "胚布单关联引用不存在: %s"));
        assertNull(result);
    }

    @Test
    @DisplayName("下游引用校验：打色单不存在抛下游错误码")
    void testValidateColorInstructionForDownstream_notExists_throwsException() {
        when(colorInstructionMapper.selectById(999L)).thenReturn(null);
        var errCode = new com.luna.lunasrv.framework.common.exception.ErrorCode(1_030_006_002, "胚布单关联引用不存在: %s");

        ServiceException ex = assertThrows(ServiceException.class, () ->
                colorInstructionService.validateColorInstructionForDownstream(999L, 100L, 200L, "P001", errCode));
        assertEquals(1_030_006_002, ex.getCode());
    }

    @Test
    @DisplayName("下游引用校验：跨租户打色单抛下游错误码")
    void testValidateColorInstructionForDownstream_crossTenant_throwsException() {
        try {
            com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder.setTenantId(1L);
            ColorInstructionDO order = new ColorInstructionDO();
            order.setId(501L);
            order.setTenantId(2L); // 租户不匹配
            order.setStatus("CONFIRMED");
            order.setDeleted(false);
            when(colorInstructionMapper.selectById(501L)).thenReturn(order);

            var errCode = new com.luna.lunasrv.framework.common.exception.ErrorCode(1_030_006_002, "胚布单关联引用不存在: %s");
            ServiceException ex = assertThrows(ServiceException.class, () ->
                    colorInstructionService.validateColorInstructionForDownstream(501L, null, null, null, errCode));
            assertEquals(1_030_006_002, ex.getCode());
        } finally {
            com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder.clear();
        }
    }

    @Test
    @DisplayName("下游引用校验：已软删打色单抛下游错误码")
    void testValidateColorInstructionForDownstream_deleted_throwsException() {
        ColorInstructionDO order = new ColorInstructionDO();
        order.setId(501L);
        order.setStatus("CONFIRMED");
        order.setDeleted(true); // 已软删
        when(colorInstructionMapper.selectById(501L)).thenReturn(order);

        var errCode = new com.luna.lunasrv.framework.common.exception.ErrorCode(1_030_006_002, "胚布单关联引用不存在: %s");
        ServiceException ex = assertThrows(ServiceException.class, () ->
                colorInstructionService.validateColorInstructionForDownstream(501L, null, null, null, errCode));
        assertEquals(1_030_006_002, ex.getCode());
    }

    @Test
    @DisplayName("下游引用校验：状态非 CONFIRMED/IN_PRODUCTION 抛下游错误码")
    void testValidateColorInstructionForDownstream_invalidStatus_throwsException() {
        var errCode = new com.luna.lunasrv.framework.common.exception.ErrorCode(1_030_006_002, "胚布单关联引用不存在: %s");
        for (String invalidStatus : List.of("PENDING", "IN_PROGRESS", "VOID")) {
            ColorInstructionDO order = new ColorInstructionDO();
            order.setId(501L);
            order.setStatus(invalidStatus);
            order.setDeleted(false);
            when(colorInstructionMapper.selectById(501L)).thenReturn(order);

            ServiceException ex = assertThrows(ServiceException.class, () ->
                    colorInstructionService.validateColorInstructionForDownstream(501L, null, null, null, errCode));
            assertEquals(1_030_006_002, ex.getCode());
        }
    }

    @Test
    @DisplayName("下游引用校验：客户不一致抛下游错误码")
    void testValidateColorInstructionForDownstream_customerMismatch_throwsException() {
        ColorInstructionDO order = new ColorInstructionDO();
        order.setId(501L);
        order.setStatus("CONFIRMED");
        order.setDeleted(false);
        order.setCustomerId(100L);
        when(colorInstructionMapper.selectById(501L)).thenReturn(order);

        var errCode = new com.luna.lunasrv.framework.common.exception.ErrorCode(1_030_006_002, "胚布单关联引用不存在: %s");
        ServiceException ex = assertThrows(ServiceException.class, () ->
                colorInstructionService.validateColorInstructionForDownstream(501L, 200L, null, null, errCode));
        assertEquals(1_030_006_002, ex.getCode());
    }

    @Test
    @DisplayName("下游引用校验：产品ID或产品编码不一致抛下游错误码")
    void testValidateColorInstructionForDownstream_productMismatch_throwsException() {
        ColorInstructionDO order = new ColorInstructionDO();
        order.setId(501L);
        order.setStatus("CONFIRMED");
        order.setDeleted(false);
        order.setCustomerId(100L);
        order.setProductId(10L);
        order.setProductCode("P001");
        when(colorInstructionMapper.selectById(501L)).thenReturn(order);

        var errCode = new com.luna.lunasrv.framework.common.exception.ErrorCode(1_030_006_002, "胚布单关联引用不存在: %s");

        // 1. productId 不匹配
        ServiceException ex1 = assertThrows(ServiceException.class, () ->
                colorInstructionService.validateColorInstructionForDownstream(501L, 100L, 99L, null, errCode));
        assertEquals(1_030_006_002, ex1.getCode());

        // 2. productCode 不匹配
        ServiceException ex2 = assertThrows(ServiceException.class, () ->
                colorInstructionService.validateColorInstructionForDownstream(501L, 100L, 10L, "P999", errCode));
        assertEquals(1_030_006_002, ex2.getCode());
    }

    @Test
    @DisplayName("下游引用校验：有效打色单校验成功返回 DO（CONFIRMED / IN_PRODUCTION）")
    void testValidateColorInstructionForDownstream_success() {
        var errCode = new com.luna.lunasrv.framework.common.exception.ErrorCode(1_030_006_002, "胚布单关联引用不存在: %s");
        for (String validStatus : List.of("CONFIRMED", "IN_PRODUCTION")) {
            ColorInstructionDO order = new ColorInstructionDO();
            order.setId(501L);
            order.setInstructionNo("DS20261013001");
            order.setStatus(validStatus);
            order.setDeleted(false);
            order.setCustomerId(100L);
            order.setProductId(10L);
            order.setProductCode("P001");
            when(colorInstructionMapper.selectById(501L)).thenReturn(order);

            ColorInstructionDO result = colorInstructionService.validateColorInstructionForDownstream(
                    501L, 100L, 10L, "P001", errCode);
            assertNotNull(result);
            assertEquals("DS20261013001", result.getInstructionNo());
        }
    }

    // ==================== Issue 07: 投产自动触发 (markInProduction) ====================

    @Test
    @DisplayName("自动标记投产：id 为空时安全返回不执行任何数据库操作")
    void testMarkInProduction_nullId_safelyReturns() {
        colorInstructionService.markInProduction(null);
        verify(colorInstructionMapper, never()).update(any(), any());
    }

    @Test
    @DisplayName("自动标记投产：CONFIRMED 态单据条件更新成功，status 置为 IN_PRODUCTION")
    void testMarkInProduction_confirmedStatus_successUpdatesToInProduction() {
        when(colorInstructionMapper.update(any(ColorInstructionDO.class), any())).thenReturn(1);

        colorInstructionService.markInProduction(501L);

        ArgumentCaptor<ColorInstructionDO> updateCaptor = ArgumentCaptor.forClass(ColorInstructionDO.class);
        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.Wrapper<ColorInstructionDO>> wrapperCaptor =
                ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.Wrapper.class);
        verify(colorInstructionMapper).update(updateCaptor.capture(), wrapperCaptor.capture());

        ColorInstructionDO updated = updateCaptor.getValue();
        assertEquals(ColorInstructionStatusEnum.IN_PRODUCTION.getValue(), updated.getStatus());

        com.baomidou.mybatisplus.core.conditions.Wrapper<ColorInstructionDO> wrapper = wrapperCaptor.getValue();
        assertNotNull(wrapper);
        String sqlSegment = wrapper.getCustomSqlSegment();
        assertTrue(sqlSegment.contains("status") || sqlSegment.contains("CONFIRMED"));
    }

    @Test
    @DisplayName("自动标记投产：更新影响行数 0 时幂等静默不报错（已投产/已作废/已回退均安全）")
    void testMarkInProduction_zeroRowsUpdated_idempotentSilentNotThrowException() {
        when(colorInstructionMapper.update(any(ColorInstructionDO.class), any())).thenReturn(0);

        assertDoesNotThrow(() -> colorInstructionService.markInProduction(501L));
        verify(colorInstructionMapper).update(any(ColorInstructionDO.class), any());
    }

    // ==================== Issue 08: 下游引用查询服务 (hasActiveDownstreamRef) ====================

    @Test
    @DisplayName("下游引用查询：id 为空时返回 false 且不查询任何下游表")
    void testHasActiveDownstreamRef_nullId_returnsFalse() {
        assertFalse(colorInstructionService.hasActiveDownstreamRef(null));
        verify(greigeOrderMapper, never()).selectCount(any());
        verify(dyeingOrderMapper, never()).selectCount(any());
    }

    @Test
    @DisplayName("下游引用查询：存在有效胚布单引用（status≠CANCELLED且未删）时返回 true，短路跳过染色单查询")
    void testHasActiveDownstreamRef_activeGreigeOrder_returnsTrue() {
        when(greigeOrderMapper.selectCount(any())).thenReturn(1L);

        boolean hasRef = colorInstructionService.hasActiveDownstreamRef(501L);

        assertTrue(hasRef);
        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GreigeOrderDO>> captor =
                ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class);
        verify(greigeOrderMapper).selectCount(captor.capture());
        String sql = captor.getValue().getCustomSqlSegment();
        assertTrue(sql.contains("color_instruction_id"));
        assertTrue(sql.contains("status"));
        assertTrue(captor.getValue().getParamNameValuePairs().containsValue(ColorInstructionServiceImpl.DOWNSTREAM_STATUS_CANCELLED));
        verify(dyeingOrderMapper, never()).selectCount(any());
    }

    @Test
    @DisplayName("下游引用查询：无有效胚布单但存在有效染色单引用（含DRAFT态，status≠CANCELLED且未删）时返回 true")
    void testHasActiveDownstreamRef_activeDyeingOrder_returnsTrue() {
        when(greigeOrderMapper.selectCount(any())).thenReturn(0L);
        when(dyeingOrderMapper.selectCount(any())).thenReturn(2L);

        boolean hasRef = colorInstructionService.hasActiveDownstreamRef(501L);

        assertTrue(hasRef);
        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DyeingOrderDO>> captor =
                ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class);
        verify(dyeingOrderMapper).selectCount(captor.capture());
        String sql = captor.getValue().getCustomSqlSegment();
        assertTrue(sql.contains("color_instruction_id"));
        assertTrue(sql.contains("status"));
        assertTrue(captor.getValue().getParamNameValuePairs().containsValue(ColorInstructionServiceImpl.DOWNSTREAM_STATUS_CANCELLED));
    }

    @Test
    @DisplayName("下游引用查询：胚布单和染色单均无有效引用时返回 false")
    void testHasActiveDownstreamRef_noDownstreamRef_returnsFalse() {
        when(greigeOrderMapper.selectCount(any())).thenReturn(0L);
        when(dyeingOrderMapper.selectCount(any())).thenReturn(0L);

        boolean hasRef = colorInstructionService.hasActiveDownstreamRef(501L);

        assertFalse(hasRef);
    }

    // ==================== Issue 08: 打色单回退 (rollbackColorInstruction) ====================

    @Test
    @DisplayName("打色单回退：id 为空或单据不存在时抛 1_030_032_000")
    void testRollbackColorInstruction_orderNotFound_throwsNotExists() {
        ServiceException exNull = assertThrows(ServiceException.class, () ->
                colorInstructionService.rollbackColorInstruction(null));
        assertEquals(COLOR_INSTRUCTION_NOT_EXISTS.getCode(), exNull.getCode());

        when(colorInstructionMapper.selectById(999L)).thenReturn(null);
        ServiceException exNotFound = assertThrows(ServiceException.class, () ->
                colorInstructionService.rollbackColorInstruction(999L));
        assertEquals(COLOR_INSTRUCTION_NOT_EXISTS.getCode(), exNotFound.getCode());
    }

    @Test
    @DisplayName("打色单回退：非 CONFIRMED 态（PENDING/IN_PROGRESS/IN_PRODUCTION/VOID）回退抛 1_030_032_002")
    void testRollbackColorInstruction_nonConfirmedStatus_throwsStatusNotAllowed() {
        for (String status : List.of("PENDING", "IN_PROGRESS", "IN_PRODUCTION", "VOID")) {
            ColorInstructionDO order = new ColorInstructionDO();
            order.setId(501L);
            order.setStatus(status);
            when(colorInstructionMapper.selectById(501L)).thenReturn(order);

            ServiceException ex = assertThrows(ServiceException.class, () ->
                    colorInstructionService.rollbackColorInstruction(501L));
            assertEquals(COLOR_INSTRUCTION_STATUS_NOT_ALLOWED.getCode(), ex.getCode(),
                    "状态 " + status + " 下回退应抛 1_030_032_002");
        }
    }

    @Test
    @DisplayName("打色单回退：存在有效下游引用（胚布单或染色单有效引用）时抛 1_030_032_005")
    void testRollbackColorInstruction_hasActiveDownstreamRef_throwsHasDownstreamRef() {
        ColorInstructionDO order = new ColorInstructionDO();
        order.setId(501L);
        order.setInstructionNo("DS20261013001");
        order.setStatus("CONFIRMED");
        when(colorInstructionMapper.selectById(501L)).thenReturn(order);
        // 模拟存在有效胚布单引用
        when(greigeOrderMapper.selectCount(any())).thenReturn(1L);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                colorInstructionService.rollbackColorInstruction(501L));
        assertEquals(COLOR_INSTRUCTION_HAS_DOWNSTREAM_REFERENCE.getCode(), ex.getCode());
        verify(colorInstructionMapper, never()).update(any(), any());
    }

    @Test
    @DisplayName("打色单回退：无下游引用成功回退 CONFIRMED -> IN_PROGRESS，条件更新带 CONFIRMED 态防并发，颜色行状态不回退")
    void testRollbackColorInstruction_success_updatesToInProgressAndKeepsLineStatus() {
        ColorInstructionDO order = new ColorInstructionDO();
        order.setId(501L);
        order.setInstructionNo("DS20261013001");
        order.setStatus("CONFIRMED");
        order.setConfirmType("CUSTOMER");
        order.setCustomerFeedback("客户满意");
        order.setConfirmedBy(100L);
        order.setConfirmedAt(java.time.LocalDateTime.now());
        when(colorInstructionMapper.selectById(501L)).thenReturn(order);
        // 模拟无下游引用
        when(greigeOrderMapper.selectCount(any())).thenReturn(0L);
        when(dyeingOrderMapper.selectCount(any())).thenReturn(0L);
        when(colorInstructionMapper.update(any(ColorInstructionDO.class), any())).thenReturn(1);

        colorInstructionService.rollbackColorInstruction(501L);

        ArgumentCaptor<ColorInstructionDO> updateCaptor = ArgumentCaptor.forClass(ColorInstructionDO.class);
        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ColorInstructionDO>> wrapperCaptor =
                ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class);
        verify(colorInstructionMapper).update(updateCaptor.capture(), any());

        ColorInstructionDO updated = updateCaptor.getValue();
        assertEquals(ColorInstructionStatusEnum.IN_PROGRESS.getValue(), updated.getStatus());
        assertNull(updated.getConfirmType(), "回退后确认类型应清空");
        assertNull(updated.getCustomerFeedback(), "回退后客户反馈应清空");
        assertNull(updated.getConfirmedBy(), "回退后确认人应清空");
        assertNull(updated.getConfirmedAt(), "回退后确认时间应清空");

        // 颜色行状态不回退：未对颜色行子表做任何更新操作
        verify(colorInstructionColorMapper, never()).update(any(), any());
        verify(colorInstructionColorMapper, never()).delete(any());
    }

    @Test
    @DisplayName("打色单回退：并发更新影响行数 0 时抛 1_030_032_002 防并发冲突")
    void testRollbackColorInstruction_concurrencyConflict_throwsStatusNotAllowed() {
        ColorInstructionDO order = new ColorInstructionDO();
        order.setId(501L);
        order.setStatus("CONFIRMED");
        when(colorInstructionMapper.selectById(501L)).thenReturn(order);
        when(greigeOrderMapper.selectCount(any())).thenReturn(0L);
        when(dyeingOrderMapper.selectCount(any())).thenReturn(0L);
        // 并发影响行数 0
        when(colorInstructionMapper.update(any(ColorInstructionDO.class), any())).thenReturn(0);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                colorInstructionService.rollbackColorInstruction(501L));
        assertEquals(COLOR_INSTRUCTION_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

    // ==================== Issue 08: 打色单作废 (voidColorInstruction) ====================

    @Test
    @DisplayName("打色单作废：reqVO 为空或 id 为空或单据不存在时抛 1_030_032_000")
    void testVoidColorInstruction_nullOrNotFound_throwsNotExists() {
        ServiceException exNullReq = assertThrows(ServiceException.class, () ->
                colorInstructionService.voidColorInstruction(null));
        assertEquals(COLOR_INSTRUCTION_NOT_EXISTS.getCode(), exNullReq.getCode());

        ColorInstructionVoidReqVO reqNullId = new ColorInstructionVoidReqVO();
        reqNullId.setVoidReason("测试原因");
        ServiceException exNullId = assertThrows(ServiceException.class, () ->
                colorInstructionService.voidColorInstruction(reqNullId));
        assertEquals(COLOR_INSTRUCTION_NOT_EXISTS.getCode(), exNullId.getCode());

        when(colorInstructionMapper.selectById(999L)).thenReturn(null);
        ColorInstructionVoidReqVO reqNotFound = new ColorInstructionVoidReqVO();
        reqNotFound.setId(999L);
        reqNotFound.setVoidReason("测试原因");
        ServiceException exNotFound = assertThrows(ServiceException.class, () ->
                colorInstructionService.voidColorInstruction(reqNotFound));
        assertEquals(COLOR_INSTRUCTION_NOT_EXISTS.getCode(), exNotFound.getCode());
    }

    @Test
    @DisplayName("打色单作废：IN_PRODUCTION 禁作废（终态守卫抛 1_030_032_002），VOID 态拒绝二次作废（抛 1_030_032_002）")
    void testVoidColorInstruction_inProductionOrVoidStatus_throwsStatusNotAllowed() {
        for (String status : List.of("IN_PRODUCTION", "VOID")) {
            ColorInstructionDO order = new ColorInstructionDO();
            order.setId(501L);
            order.setStatus(status);
            when(colorInstructionMapper.selectById(501L)).thenReturn(order);

            ColorInstructionVoidReqVO reqVO = new ColorInstructionVoidReqVO();
            reqVO.setId(501L);
            reqVO.setVoidReason("需求变更");

            ServiceException ex = assertThrows(ServiceException.class, () ->
                    colorInstructionService.voidColorInstruction(reqVO));
            assertEquals(COLOR_INSTRUCTION_STATUS_NOT_ALLOWED.getCode(), ex.getCode(),
                    "状态 " + status + " 下作废应抛 1_030_032_002");
        }
    }

    @Test
    @DisplayName("打色单作废：存在有效下游引用（包含下游 DRAFT 染色单）时抛 1_030_032_005")
    void testVoidColorInstruction_hasActiveDownstreamRef_throwsHasDownstreamRef() {
        ColorInstructionDO order = new ColorInstructionDO();
        order.setId(501L);
        order.setInstructionNo("DS20261013001");
        order.setStatus("CONFIRMED");
        when(colorInstructionMapper.selectById(501L)).thenReturn(order);
        // 模拟下游存在有效染色单（例如处于 DRAFT 状态）
        when(greigeOrderMapper.selectCount(any())).thenReturn(0L);
        when(dyeingOrderMapper.selectCount(any())).thenReturn(1L);

        ColorInstructionVoidReqVO reqVO = new ColorInstructionVoidReqVO();
        reqVO.setId(501L);
        reqVO.setVoidReason("客户取消");

        ServiceException ex = assertThrows(ServiceException.class, () ->
                colorInstructionService.voidColorInstruction(reqVO));
        assertEquals(COLOR_INSTRUCTION_HAS_DOWNSTREAM_REFERENCE.getCode(), ex.getCode());
        verify(colorInstructionMapper, never()).update(any(), any());
    }

    @Test
    @DisplayName("打色单作废：各允许状态（PENDING/IN_PROGRESS/CONFIRMED）无下游引用时成功作废，写入 voidReason 与 status=VOID")
    void testVoidColorInstruction_allowedStatuses_successUpdatesToVoid() {
        for (String status : List.of("PENDING", "IN_PROGRESS", "CONFIRMED")) {
            ColorInstructionDO order = new ColorInstructionDO();
            order.setId(501L);
            order.setInstructionNo("DS20261013001");
            order.setStatus(status);
            when(colorInstructionMapper.selectById(501L)).thenReturn(order);
            when(greigeOrderMapper.selectCount(any())).thenReturn(0L);
            when(dyeingOrderMapper.selectCount(any())).thenReturn(0L);
            when(colorInstructionMapper.update(any(ColorInstructionDO.class), any())).thenReturn(1);

            ColorInstructionVoidReqVO reqVO = new ColorInstructionVoidReqVO();
            reqVO.setId(501L);
            reqVO.setVoidReason("  打色多次不合格，客户取消需求  ");

            colorInstructionService.voidColorInstruction(reqVO);

            ArgumentCaptor<ColorInstructionDO> updateCaptor = ArgumentCaptor.forClass(ColorInstructionDO.class);
            verify(colorInstructionMapper, atLeastOnce()).update(updateCaptor.capture(), any());

            ColorInstructionDO updated = updateCaptor.getValue();
            assertEquals(ColorInstructionStatusEnum.VOID.getValue(), updated.getStatus());
            assertEquals("打色多次不合格，客户取消需求", updated.getVoidReason());
        }
    }

    @Test
    @DisplayName("打色单作废：并发更新影响行数 0 时抛 1_030_032_002 防并发冲突")
    void testVoidColorInstruction_concurrencyConflict_throwsStatusNotAllowed() {
        ColorInstructionDO order = new ColorInstructionDO();
        order.setId(501L);
        order.setStatus("IN_PROGRESS");
        when(colorInstructionMapper.selectById(501L)).thenReturn(order);
        when(greigeOrderMapper.selectCount(any())).thenReturn(0L);
        when(dyeingOrderMapper.selectCount(any())).thenReturn(0L);
        // 并发影响行数 0
        when(colorInstructionMapper.update(any(ColorInstructionDO.class), any())).thenReturn(0);

        ColorInstructionVoidReqVO reqVO = new ColorInstructionVoidReqVO();
        reqVO.setId(501L);
        reqVO.setVoidReason("客户取消");

        ServiceException ex = assertThrows(ServiceException.class, () ->
                colorInstructionService.voidColorInstruction(reqVO));
        assertEquals(COLOR_INSTRUCTION_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

    // ==================== Issue 08: 终态守卫强化 (IN_PRODUCTION / VOID 拒绝一切写操作) ====================

    @Test
    @DisplayName("终态守卫：IN_PRODUCTION 和 VOID 单据拒绝一切写操作（update/delete/void/approve/reject-restrike），均抛 1_030_032_002")
    void testTerminalGuard_inProductionAndVoid_rejectAllWriteOperations() {
        for (String terminalStatus : List.of("IN_PRODUCTION", "VOID")) {
            ColorInstructionDO order = new ColorInstructionDO();
            order.setId(501L);
            order.setStatus(terminalStatus);
            when(colorInstructionMapper.selectById(501L)).thenReturn(order);

            // 1. update
            ColorInstructionSaveReqVO updateReq = new ColorInstructionSaveReqVO();
            updateReq.setId(501L);
            ServiceException exUpdate = assertThrows(ServiceException.class, () ->
                    colorInstructionService.updateColorInstruction(updateReq));
            assertEquals(COLOR_INSTRUCTION_STATUS_NOT_ALLOWED.getCode(), exUpdate.getCode());

            // 2. delete
            ServiceException exDelete = assertThrows(ServiceException.class, () ->
                    colorInstructionService.deleteColorInstruction(501L));
            assertEquals(COLOR_INSTRUCTION_STATUS_NOT_ALLOWED.getCode(), exDelete.getCode());

            // 3. void
            ColorInstructionVoidReqVO voidReq = new ColorInstructionVoidReqVO();
            voidReq.setId(501L);
            voidReq.setVoidReason("终态尝试作废");
            ServiceException exVoid = assertThrows(ServiceException.class, () ->
                    colorInstructionService.voidColorInstruction(voidReq));
            assertEquals(COLOR_INSTRUCTION_STATUS_NOT_ALLOWED.getCode(), exVoid.getCode());

            // 4. approve line
            ColorInstructionColorDO colorLine = new ColorInstructionColorDO();
            colorLine.setId(11L);
            colorLine.setInstructionId(501L);
            colorLine.setStatus("PENDING");
            when(colorInstructionColorMapper.selectById(11L)).thenReturn(colorLine);

            ServiceException exApprove = assertThrows(ServiceException.class, () ->
                    colorInstructionService.approveColorLine(11L));
            assertEquals(COLOR_INSTRUCTION_STATUS_NOT_ALLOWED.getCode(), exApprove.getCode());

            // 5. reject-restrike line
            ServiceException exReject = assertThrows(ServiceException.class, () ->
                    colorInstructionService.rejectRestrikeColorLine(11L, "颜色不符"));
            assertEquals(COLOR_INSTRUCTION_STATUS_NOT_ALLOWED.getCode(), exReject.getCode());
        }
    }

    @Test
    @DisplayName("软删守卫：CONFIRMED 态单据软删拦截报 1_030_032_002（CONFIRMED 取消走作废）")
    void testDeleteColorInstruction_confirmedStatus_throwsStatusNotAllowed() {
        ColorInstructionDO order = new ColorInstructionDO();
        order.setId(501L);
        order.setStatus("CONFIRMED");
        when(colorInstructionMapper.selectById(501L)).thenReturn(order);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                colorInstructionService.deleteColorInstruction(501L));
        assertEquals(COLOR_INSTRUCTION_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("软删守卫：PENDING 态单据若存在有效下游引用，软删被拦截报 1_030_032_005")
    void testDeleteColorInstruction_pendingWithActiveDownstreamRef_throwsHasDownstreamRef() {
        ColorInstructionDO order = new ColorInstructionDO();
        order.setId(501L);
        order.setStatus("PENDING");
        when(colorInstructionMapper.selectById(501L)).thenReturn(order);
        // 模拟存在有效下游引用
        when(greigeOrderMapper.selectCount(any())).thenReturn(1L);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                colorInstructionService.deleteColorInstruction(501L));
        assertEquals(COLOR_INSTRUCTION_HAS_DOWNSTREAM_REFERENCE.getCode(), ex.getCode());
        verify(colorInstructionMapper, never()).deleteById(any());
    }

}



