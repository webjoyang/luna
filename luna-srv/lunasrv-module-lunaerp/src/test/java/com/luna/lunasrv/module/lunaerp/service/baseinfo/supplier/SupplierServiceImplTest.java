package com.luna.lunasrv.module.lunaerp.service.baseinfo.supplier;

import cn.idev.excel.FastExcelFactory;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.common.dataimport.ExcelImportResult;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.supplier.vo.SupplierImportVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.supplier.vo.SupplierPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.supplier.vo.SupplierRespVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.supplier.vo.SupplierSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.supplier.SupplierDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.supplier.SupplierMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link SupplierServiceImpl} 的纯 Mockito 单元测试（不依赖 Spring 上下文与数据库）。
 *
 * 覆盖关键业务语义：
 *  - createSupplier：自动号 SUP+年份+4位序号、序号 max+1；扫描与查重入口含已删除行（ADR-0005）；
 *    手填号撞已删除/未删除供应商均抛 1_030_005_001；自动号撞号有限次顺延重试；
 *    shortName 为空用 companyName 回填；status 缺省 ACTIVE。
 *  - getSupplier/updateSupplier：不存在抛 1_030_005_000；update 编号变更撞号抛 1_030_005_001；
 *    编号留空置 null 跳过该列。
 *  - deleteSupplier/deleteSupplierList：不存在抛错；删除时先置 status=INACTIVE 再逻辑删除。
 *  - getSupplierPage：search/supplierType/status 透传到 mapper。
 *  - getNextSupplierNo：复用扫描入口、不调用 insert（不占号）。
 *
 * 说明：Controller 层 MockMvc 集成测试需要完整的 security filter chain + 测试数据库，
 * 本仓 lunaerp 模块当前无该基础设施，故以纯单元测试覆盖核心逻辑。
 *
 * @author luna
 */
class SupplierServiceImplTest {

    @Mock
    private SupplierMapper supplierMapper;

    @InjectMocks
    private SupplierServiceImpl supplierService;

    private static final String COMPANY_NAME = "绍兴纱线厂";
    private static final String SHORT_NAME = "绍兴纱线";
    private static final String YEAR_PREFIX = "SUP" + LocalDate.now().getYear();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // 初始化 TableInfo 缓存，使 LambdaQueryWrapper 的 SFunction 列名解析在无 MyBatis 环境下可用
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), SupplierDO.class);
        // 模拟 insert 时回填主键（@KeySequence 在真实环境由 MyBatis-Plus 填充）
        when(supplierMapper.insert(any(SupplierDO.class))).thenAnswer(invocation -> {
            SupplierDO dobj = invocation.getArgument(0);
            dobj.setId(1024L);
            return 1;
        });
    }

    private SupplierSaveReqVO buildCreateReqVO() {
        SupplierSaveReqVO req = new SupplierSaveReqVO();
        req.setCompanyName(COMPANY_NAME);
        return req;
    }

    // ==================== createSupplier：自动编号 ====================

    @Test
    void createSupplier_noSupplierNo_generatesSupYearSeq() {
        // 无历史供应商 → 生成 SUP + 当前年份 + 0001
        SupplierSaveReqVO req = buildCreateReqVO();
        req.setSupplierNo(null);
        when(supplierMapper.selectListBySupplierNoPrefixIncludingDeleted(anyString()))
                .thenReturn(Collections.emptyList());
        when(supplierMapper.selectBySupplierNoIncludingDeleted(anyString())).thenReturn(null);

        Long id = supplierService.createSupplier(req);

        assertEquals(1024L, id);
        ArgumentCaptor<SupplierDO> captor = ArgumentCaptor.forClass(SupplierDO.class);
        verify(supplierMapper).insert(captor.capture());
        assertEquals(YEAR_PREFIX + "0001", captor.getValue().getSupplierNo(),
                "编号缺省应为 SUP+当前年份+4 位顺序号 0001");
    }

    @Test
    void createSupplier_existingMax_incrementsSequenceByOne() {
        // 已有 SUP{year}0003（已删除，deleted=1）→ 取号扫描含已删除行，生成 0004
        SupplierDO deletedMax = new SupplierDO();
        deletedMax.setSupplierNo(YEAR_PREFIX + "0003");
        deletedMax.setDeleted(true); // 已删除行同样参与取号扫描（ADR-0005）
        when(supplierMapper.selectListBySupplierNoPrefixIncludingDeleted(YEAR_PREFIX))
                .thenReturn(Collections.singletonList(deletedMax));
        when(supplierMapper.selectBySupplierNoIncludingDeleted(anyString())).thenReturn(null);

        SupplierSaveReqVO req = buildCreateReqVO();
        req.setSupplierNo(null);
        supplierService.createSupplier(req);

        // 取号扫描入口必须是"包含已删除行"的方法（ADR-0005），且前缀为 SUP+年份
        verify(supplierMapper).selectListBySupplierNoPrefixIncludingDeleted(YEAR_PREFIX);
        ArgumentCaptor<SupplierDO> captor = ArgumentCaptor.forClass(SupplierDO.class);
        verify(supplierMapper).insert(captor.capture());
        assertEquals(YEAR_PREFIX + "0004", captor.getValue().getSupplierNo(),
                "应取含已删除行在内的最大序号 +1");
    }

    @Test
    void createSupplier_autoNoCollides_retriesWithBumpedNumber() {
        // 第一次取号得 0002 撞号，扫描结果更新后第二次顺延为 0003
        SupplierDO firstScan = new SupplierDO();
        firstScan.setSupplierNo(YEAR_PREFIX + "0001");
        SupplierDO secondScan = new SupplierDO();
        secondScan.setSupplierNo(YEAR_PREFIX + "0002");
        when(supplierMapper.selectListBySupplierNoPrefixIncludingDeleted(YEAR_PREFIX))
                .thenReturn(Collections.singletonList(firstScan))
                .thenReturn(Collections.singletonList(secondScan));
        // 第一次查重命中（撞号），第二次放行
        when(supplierMapper.selectBySupplierNoIncludingDeleted(YEAR_PREFIX + "0002"))
                .thenReturn(new SupplierDO());
        when(supplierMapper.selectBySupplierNoIncludingDeleted(YEAR_PREFIX + "0003"))
                .thenReturn(null);

        SupplierSaveReqVO req = buildCreateReqVO();
        req.setSupplierNo(null);
        supplierService.createSupplier(req);

        ArgumentCaptor<SupplierDO> captor = ArgumentCaptor.forClass(SupplierDO.class);
        verify(supplierMapper).insert(captor.capture());
        assertEquals(YEAR_PREFIX + "0003", captor.getValue().getSupplierNo(),
                "自动号撞号应顺延重试后取下一个可用号");
    }

    @Test
    void createSupplier_autoNoAlwaysCollides_throwsDuplicateAfterLimitedRetries() {
        // 持续撞号 → 有限次顺延后放弃并报 1_030_005_001，避免无限循环
        when(supplierMapper.selectListBySupplierNoPrefixIncludingDeleted(anyString()))
                .thenReturn(Collections.emptyList());
        when(supplierMapper.selectBySupplierNoIncludingDeleted(anyString()))
                .thenReturn(new SupplierDO());

        SupplierSaveReqVO req = buildCreateReqVO();
        req.setSupplierNo(null);
        ServiceException ex = assertThrows(ServiceException.class,
                () -> supplierService.createSupplier(req));
        assertEquals(1_030_005_001, ex.getCode());
        verify(supplierMapper, times(3)).selectListBySupplierNoPrefixIncludingDeleted(anyString());
        verify(supplierMapper, never()).insert(any(SupplierDO.class));
    }

    @Test
    void createSupplier_exceeds9999_expandsDigitsNaturally() {
        // 已有 SUP{year}10000（5 位）→ 生成 10001，自然扩位不补零到 4 位
        SupplierDO bigMax = new SupplierDO();
        bigMax.setSupplierNo(YEAR_PREFIX + "10000");
        when(supplierMapper.selectListBySupplierNoPrefixIncludingDeleted(YEAR_PREFIX))
                .thenReturn(Collections.singletonList(bigMax));
        when(supplierMapper.selectBySupplierNoIncludingDeleted(anyString())).thenReturn(null);

        SupplierSaveReqVO req = buildCreateReqVO();
        req.setSupplierNo(null);
        supplierService.createSupplier(req);

        ArgumentCaptor<SupplierDO> captor = ArgumentCaptor.forClass(SupplierDO.class);
        verify(supplierMapper).insert(captor.capture());
        assertEquals(YEAR_PREFIX + "10001", captor.getValue().getSupplierNo(),
                "超 9999 应自然扩位，不强制补零到 4 位");
    }

    // ==================== createSupplier：手填编号 ====================

    @Test
    void createSupplier_manualNoCollidesWithDeletedSupplier_throwsCode1030005001() {
        // 手填编号与已删除供应商撞号（ADR-0005：软删不释放编号）→ 报 1_030_005_001，不顺延
        SupplierDO deleted = new SupplierDO();
        deleted.setId(1L);
        deleted.setSupplierNo("SUP20260001");
        deleted.setDeleted(true);
        when(supplierMapper.selectBySupplierNoIncludingDeleted("SUP20260001"))
                .thenReturn(deleted);

        SupplierSaveReqVO req = buildCreateReqVO();
        req.setSupplierNo("SUP20260001");
        ServiceException ex = assertThrows(ServiceException.class,
                () -> supplierService.createSupplier(req));
        assertEquals(1_030_005_001, ex.getCode());
        verify(supplierMapper, never()).insert(any(SupplierDO.class));
    }

    @Test
    void createSupplier_manualNoCollidesWithActiveSupplier_throwsCode1030005001() {
        // 手填编号与在用供应商撞号 → 同样报 1_030_005_001
        SupplierDO active = new SupplierDO();
        active.setId(2L);
        active.setSupplierNo("SUP20260002");
        when(supplierMapper.selectBySupplierNoIncludingDeleted("SUP20260002"))
                .thenReturn(active);

        SupplierSaveReqVO req = buildCreateReqVO();
        req.setSupplierNo("SUP20260002");
        ServiceException ex = assertThrows(ServiceException.class,
                () -> supplierService.createSupplier(req));
        assertEquals(1_030_005_001, ex.getCode());
        verify(supplierMapper, never()).insert(any(SupplierDO.class));
    }

    @Test
    void createSupplier_manualNoUnique_acceptedAndNoScan() {
        when(supplierMapper.selectBySupplierNoIncludingDeleted("SUP20269999")).thenReturn(null);

        SupplierSaveReqVO req = buildCreateReqVO();
        req.setSupplierNo("SUP20269999");
        supplierService.createSupplier(req);

        // 手填编号不触发取号扫描
        verify(supplierMapper, never()).selectListBySupplierNoPrefixIncludingDeleted(anyString());
        ArgumentCaptor<SupplierDO> captor = ArgumentCaptor.forClass(SupplierDO.class);
        verify(supplierMapper).insert(captor.capture());
        assertEquals("SUP20269999", captor.getValue().getSupplierNo(), "手填编号应按原值保存");
    }

    // ==================== createSupplier：回填与默认值 ====================

    @Test
    void createSupplier_shortNameBlank_backfilledFromCompany() {
        SupplierSaveReqVO req = buildCreateReqVO();
        req.setSupplierNo(null);
        req.setStatus(null);
        when(supplierMapper.selectListBySupplierNoPrefixIncludingDeleted(anyString()))
                .thenReturn(Collections.emptyList());
        when(supplierMapper.selectBySupplierNoIncludingDeleted(anyString())).thenReturn(null);

        supplierService.createSupplier(req);

        ArgumentCaptor<SupplierDO> captor = ArgumentCaptor.forClass(SupplierDO.class);
        verify(supplierMapper).insert(captor.capture());
        SupplierDO saved = captor.getValue();
        assertEquals(COMPANY_NAME, saved.getShortName(), "shortName 为空时应用 companyName 回填");
        assertEquals("ACTIVE", saved.getStatus(), "status 缺省应为 ACTIVE");
    }

    @Test
    void createSupplier_shortNameAndStatusProvided_preserved() {
        SupplierSaveReqVO req = buildCreateReqVO();
        req.setShortName(SHORT_NAME);
        req.setStatus("INACTIVE");
        when(supplierMapper.selectListBySupplierNoPrefixIncludingDeleted(anyString()))
                .thenReturn(Collections.emptyList());
        when(supplierMapper.selectBySupplierNoIncludingDeleted(anyString())).thenReturn(null);

        supplierService.createSupplier(req);

        ArgumentCaptor<SupplierDO> captor = ArgumentCaptor.forClass(SupplierDO.class);
        verify(supplierMapper).insert(captor.capture());
        SupplierDO saved = captor.getValue();
        assertEquals(SHORT_NAME, saved.getShortName(), "shortName 非空时应保留原值");
        assertEquals("INACTIVE", saved.getStatus(), "status 显式传入时应保留");
    }

    // ==================== getSupplier / updateSupplier / deleteSupplier ====================

    @Test
    void getSupplier_notExists_throwsCode1030005000() {
        when(supplierMapper.selectById(9999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> supplierService.getSupplier(9999L));
        assertEquals(1_030_005_000, ex.getCode());
    }

    @Test
    void updateSupplier_notExists_throwsCode1030005000() {
        SupplierSaveReqVO req = new SupplierSaveReqVO();
        req.setId(9999L);
        req.setCompanyName(COMPANY_NAME);
        when(supplierMapper.selectById(9999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> supplierService.updateSupplier(req));
        assertEquals(1_030_005_000, ex.getCode());
        verify(supplierMapper, never()).updateById(any(SupplierDO.class));
    }

    @Test
    void updateSupplier_noChangedToOtherSupplier_throwsDuplicate() {
        // 编号改为他人供应商（含已删除行）的编号 → 报 1_030_005_001
        SupplierDO other = new SupplierDO();
        other.setId(2L);
        other.setSupplierNo("SUP20260002");
        when(supplierMapper.selectById(1024L)).thenReturn(new SupplierDO());
        when(supplierMapper.selectBySupplierNoIncludingDeleted("SUP20260002")).thenReturn(other);

        SupplierSaveReqVO req = new SupplierSaveReqVO();
        req.setId(1024L);
        req.setCompanyName(COMPANY_NAME);
        req.setSupplierNo("SUP20260002");
        ServiceException ex = assertThrows(ServiceException.class,
                () -> supplierService.updateSupplier(req));
        assertEquals(1_030_005_001, ex.getCode());
        verify(supplierMapper, never()).updateById(any(SupplierDO.class));
    }

    @Test
    void updateSupplier_blankSupplierNo_keepsExistingNumber() {
        // 编号留空视为"不修改"：置 null 跳过该列更新，避免写空串绕过编号全量唯一语义（ADR-0005）
        when(supplierMapper.selectById(1024L)).thenReturn(new SupplierDO());

        SupplierSaveReqVO req = new SupplierSaveReqVO();
        req.setId(1024L);
        req.setCompanyName(COMPANY_NAME);
        req.setSupplierNo("");
        supplierService.updateSupplier(req);

        ArgumentCaptor<SupplierDO> captor = ArgumentCaptor.forClass(SupplierDO.class);
        verify(supplierMapper).updateById(captor.capture());
        assertNull(captor.getValue().getSupplierNo(),
                "编号留空时应置 null 跳过该列，不应写空串");
        // 留空编号也不应触发取号扫描
        verify(supplierMapper, never()).selectListBySupplierNoPrefixIncludingDeleted(anyString());
    }

    @Test
    void updateSupplier_shortNameBlank_backfilledFromCompany() {
        when(supplierMapper.selectById(1024L)).thenReturn(new SupplierDO());

        SupplierSaveReqVO req = new SupplierSaveReqVO();
        req.setId(1024L);
        req.setCompanyName(COMPANY_NAME);
        req.setSupplierNo("SUP20260001");
        when(supplierMapper.selectBySupplierNoIncludingDeleted("SUP20260001")).thenReturn(null);

        supplierService.updateSupplier(req);

        ArgumentCaptor<SupplierDO> captor = ArgumentCaptor.forClass(SupplierDO.class);
        verify(supplierMapper).updateById(captor.capture());
        assertEquals(COMPANY_NAME, captor.getValue().getShortName(),
                "update 时 shortName 为空也应用 companyName 回填");
    }

    @Test
    void deleteSupplier_notExists_throwsCode1030005000() {
        when(supplierMapper.selectById(9999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> supplierService.deleteSupplier(9999L));
        assertEquals(1_030_005_000, ex.getCode());
        verify(supplierMapper, never()).deleteByIds(anyList());
    }

    @Test
    void deleteSupplier_exists_setsInactiveThenLogicalDelete() {
        SupplierDO supplier = new SupplierDO();
        supplier.setId(1024L);
        supplier.setStatus("ACTIVE");
        when(supplierMapper.selectById(1024L)).thenReturn(supplier);
        when(supplierMapper.selectByIds(anyList())).thenReturn(Collections.singletonList(supplier));

        supplierService.deleteSupplier(1024L);

        // 1. 先置 status=INACTIVE
        ArgumentCaptor<SupplierDO> captor = ArgumentCaptor.forClass(SupplierDO.class);
        verify(supplierMapper).updateById(captor.capture());
        SupplierDO statusUpdate = captor.getValue();
        assertEquals(1024L, statusUpdate.getId());
        assertEquals("INACTIVE", statusUpdate.getStatus(), "删除时应先将 status 置为 INACTIVE");
        // 2. 再逻辑删除（deleted=1）
        verify(supplierMapper).deleteByIds(Collections.singletonList(1024L));
    }

    @Test
    void deleteSupplierList_batch_success() {
        SupplierDO s1 = new SupplierDO();
        s1.setId(1L);
        SupplierDO s2 = new SupplierDO();
        s2.setId(2L);
        SupplierDO s3 = new SupplierDO();
        s3.setId(3L);
        when(supplierMapper.selectByIds(anyList())).thenReturn(Arrays.asList(s1, s2, s3));

        supplierService.deleteSupplierList(Arrays.asList(1L, 2L, 3L));

        // 每条先置 INACTIVE（逐条断言 status 与 id），再整体逻辑删除
        ArgumentCaptor<SupplierDO> captor = ArgumentCaptor.forClass(SupplierDO.class);
        verify(supplierMapper, times(3)).updateById(captor.capture());
        assertEquals(3, captor.getAllValues().size());
        for (SupplierDO statusUpdate : captor.getAllValues()) {
            assertEquals("INACTIVE", statusUpdate.getStatus(), "批量删除时每条都应先置 INACTIVE");
        }
        verify(supplierMapper).deleteByIds(Arrays.asList(1L, 2L, 3L));
    }

    // ==================== getSupplierPage：查询条件透传 ====================

    @Test
    void getSupplierPage_conditionsPassedThrough() {
        SupplierPageReqVO reqVO = new SupplierPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        reqVO.setSearch("绍兴");
        reqVO.setSupplierType("YARN_MILL");
        reqVO.setStatus("ACTIVE");
        when(supplierMapper.selectPage(reqVO)).thenReturn(PageResult.empty());

        supplierService.getSupplierPage(reqVO);

        // search/supplierType/status 由 mapper 默认方法构造 wrapper，service 透传 reqVO
        verify(supplierMapper).selectPage(reqVO);
    }

    // ==================== getNextSupplierNo：取号预览（只读不占号） ====================

    @Test
    void getNextSupplierNo_noSuppliers_returnsSupYear0001() {
        when(supplierMapper.selectListBySupplierNoPrefixIncludingDeleted(YEAR_PREFIX))
                .thenReturn(Collections.emptyList());

        String nextNo = supplierService.getNextSupplierNo();

        assertEquals(YEAR_PREFIX + "0001", nextNo);
        // 只读不占号：不做撞号查重、不写库
        verify(supplierMapper, never()).selectBySupplierNoIncludingDeleted(anyString());
        verify(supplierMapper, never()).insert(any(SupplierDO.class));
    }

    @Test
    void getNextSupplierNo_deletedRowOccupiesSeq_returnsIncrementedSameAsAutoNo() {
        // 最大序号来自已删除行（ADR-0005：软删序号仍占用）→ 预览同样 +1
        SupplierDO deletedMax = new SupplierDO();
        deletedMax.setSupplierNo(YEAR_PREFIX + "0007");
        deletedMax.setDeleted(true);
        when(supplierMapper.selectListBySupplierNoPrefixIncludingDeleted(YEAR_PREFIX))
                .thenReturn(Collections.singletonList(deletedMax));

        assertEquals(YEAR_PREFIX + "0008", supplierService.getNextSupplierNo());
    }

    // ==================== Excel 导入相关测试 ====================

    private static final List<String> SUPPLIER_HEADERS = Arrays.asList(
            "供应商编号", "企业名称", "简称", "供应商类型", "机型", "备注"
    );

    private byte[] createRawExcelBytes(List<String> headers, List<List<Object>> dataList) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<List<String>> headList = headers.stream()
                .map(Collections::singletonList)
                .toList();
        FastExcelFactory.write(out)
                .head(headList)
                .sheet("供应商导入")
                .doWrite(dataList);
        return out.toByteArray();
    }

    @Test
    void getImportTemplate_returnsSampleList() {
        List<SupplierImportVO> template = supplierService.getImportTemplate();
        assertNotNull(template);
        assertEquals(1, template.size());
        assertEquals("SUP20260001", template.get(0).getSupplierNo());
        assertEquals("示例供应商A有限公司", template.get(0).getCompanyName());
    }

    @Test
    void importSupplier_success_batchInsertsWithActiveStatusAndShortNameBackfill() throws IOException {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(Arrays.asList("SUP20260001", "绍兴第一印染", "第一印染", "", "染缸", "备注1"));
        rows.add(Arrays.asList("SUP20260002", "杭州织造二厂", "", "", "大圆机", ""));

        byte[] bytes = createRawExcelBytes(SUPPLIER_HEADERS, rows);
        MockMultipartFile file = new MockMultipartFile("file", "suppliers.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        when(supplierMapper.selectListBySupplierNosIncludingDeleted(any())).thenReturn(Collections.emptyList());

        ExcelImportResult result = supplierService.importSupplier(file);

        assertTrue(result.isSuccess());
        assertEquals(2, result.getSuccessCount());
        assertNull(result.getErrorStream());

        ArgumentCaptor<List<SupplierDO>> captor = ArgumentCaptor.forClass(List.class);
        verify(supplierMapper).insertBatch(captor.capture());
        List<SupplierDO> inserted = captor.getValue();
        assertEquals(2, inserted.size());

        SupplierDO s1 = inserted.get(0);
        assertEquals("SUP20260001", s1.getSupplierNo());
        assertEquals("绍兴第一印染", s1.getCompanyName());
        assertEquals("第一印染", s1.getShortName());
        assertEquals("ACTIVE", s1.getStatus());

        SupplierDO s2 = inserted.get(1);
        assertEquals("SUP20260002", s2.getSupplierNo());
        assertEquals("杭州织造二厂", s2.getCompanyName());
        assertEquals("杭州织造二厂", s2.getShortName(), "简称为空时应由企业名称自动回填");
        assertEquals("ACTIVE", s2.getStatus());
    }

    @Test
    void importSupplier_fileDuplicate_generatesErrorExcelWithoutDbWrite() throws IOException {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(Arrays.asList("SUP20260001", "供应商A", "简称A", "", "", ""));
        rows.add(Arrays.asList("SUP20260001", "供应商B", "简称B", "", "", ""));

        byte[] bytes = createRawExcelBytes(SUPPLIER_HEADERS, rows);
        MockMultipartFile file = new MockMultipartFile("file", "duplicate.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        when(supplierMapper.selectListBySupplierNosIncludingDeleted(any())).thenReturn(Collections.emptyList());

        ExcelImportResult result = supplierService.importSupplier(file);

        assertFalse(result.isSuccess());
        assertEquals(0, result.getSuccessCount());
        assertNotNull(result.getErrorStream());
        verify(supplierMapper, never()).insertBatch(anyList());
    }

    @Test
    void importSupplier_dbDuplicateIncludingDeleted_generatesErrorExcelWithoutDbWrite() throws IOException {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(Arrays.asList("SUP20260001", "已有供应商", "", "", "", ""));

        byte[] bytes = createRawExcelBytes(SUPPLIER_HEADERS, rows);
        MockMultipartFile file = new MockMultipartFile("file", "db_duplicate.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        SupplierDO existing = new SupplierDO();
        existing.setSupplierNo("SUP20260001");
        existing.setDeleted(true); // 即使是已删除记录，撞号也应拦截
        when(supplierMapper.selectListBySupplierNosIncludingDeleted(any())).thenReturn(Collections.singletonList(existing));

        ExcelImportResult result = supplierService.importSupplier(file);

        assertFalse(result.isSuccess());
        assertEquals(0, result.getSuccessCount());
        assertNotNull(result.getErrorStream());
        verify(supplierMapper, never()).insertBatch(anyList());
    }

}
