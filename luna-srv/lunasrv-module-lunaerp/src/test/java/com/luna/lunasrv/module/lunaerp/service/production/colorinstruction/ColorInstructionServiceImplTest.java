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

    @InjectMocks
    private ColorInstructionServiceImpl colorInstructionService;

    @BeforeEach
    void setUp() {
        // 初始化 TableInfo 缓存，使 LambdaQueryWrapper 的 SFunction 列名解析在无 Spring/MyBatis 容器环境下可用
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), ColorInstructionDO.class);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), ColorInstructionColorDO.class);
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
    @DisplayName("详情查询：存在时返回单据头及颜色行摘要")
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
        c1.setColorName("藏青");
        c1.setRound(1);
        c1.setStatus("PENDING");
        when(colorInstructionColorMapper.selectListByInstructionId(1001L)).thenReturn(List.of(c1));

        ColorInstructionRespVO resp = colorInstructionService.getColorInstruction(1001L);
        assertNotNull(resp);
        assertEquals("DS20261007001", resp.getInstructionNo());
        assertEquals(1, resp.getColorSummaries().size());
        assertEquals("C01", resp.getColorSummaries().get(0).getCustomerColorNo());
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

}
