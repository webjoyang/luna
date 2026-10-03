package com.luna.lunasrv.module.lunaerp.service.baseinfo.product;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.product.vo.ProductRespVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.product.vo.ProductSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.product.ProductDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.product.ProductImageMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.product.ProductMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.producttag.ProductTagRelMapper;
import com.luna.lunasrv.module.lunaerp.service.baseinfo.producttag.ProductTagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ProductServiceImpl} 的纯 Mockito 单元测试。
 *
 * 聚焦 ADR-0035 产品信息对齐后的字段契约（ticket 02）：
 * 1. 新增 17 列在 create/update 时随 VO → DO 原样持久化（含 5 个梭织参数与 7 个成本设置）
 * 2. type + typeLabel、fabricType + fabricTypeLabel 成对存储（字典 value + label 冗余）
 * 3. yarnSpecs / costItems 以 JSON 字符串原样存取，后端不解析内部结构
 * 4. 已删除的 output / patternNo / tempCode / conversionRate 不再出现在 DO 与 VO 上
 *
 * 以及可清空列的更新策略（ticket 04）：
 * 5. 前端能清空的列声明 FieldStrategy.ALWAYS，否则 updateById 跳过 null、旧值残留
 * 6. 逻辑删除只改 status，不能把 ALWAYS 列刷成 null
 *
 * @author luna
 */
class ProductServiceImplTest {

    /**
     * ADR-0035 已删除的 4 个字段（DO 与两个 VO 都不应再声明）
     */
    private static final List<String> REMOVED_FIELDS =
            Arrays.asList("output", "patternNo", "tempCode", "conversionRate");

    /**
     * 编辑表单允许清空、因而必须走 FieldStrategy.ALWAYS 的列（ticket 04 表单接管的选填字段）
     */
    private static final List<String> CLEARABLE_FIELDS = Arrays.asList(
            "greigeTypeId", "fabricType", "fabricTypeLabel",
            "wovenTotalEnds", "wovenReedNo",
            "wovenWarpDensity", "wovenWeftDensity", "wovenLoomWeftDensity",
            "fabricConstruction", "wovenCalculationMode",
            "greigeWeightGsm", "greigeWidthCm",
            "knitProcessingFeePerKg", "weavingFeePerMeter", "otherProcessingFeePerMeter",
            "packagingFee", "taxRate", "yarnSpecs", "costItems");

    /**
     * 纱线规格 JSON 原文（针织行 + 梭织经纬精算行的超集，后端不解析）
     */
    private static final String YARN_SPECS_JSON = "[{\"yarnSpecId\":123,\"yarnSpecName\":\"32S 棉\","
            + "\"ratio\":\"60\",\"price\":25.00,\"wasteRate\":3,\"direction\":\"WARP\"}]";

    /**
     * 成本项 JSON 原文（后端不解析）
     */
    private static final String COST_ITEMS_JSON = "[{\"supplierId\":\"uuid-1\",\"supplier\":\"东华纱厂\","
            + "\"process\":\"织造\",\"price\":5.00,\"lossPercent\":2.5}]";

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductImageMapper productImageMapper;

    @Mock
    private ProductTagRelMapper productTagRelMapper;

    @Mock
    private ProductTagService productTagService;

    @InjectMocks
    private ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(productMapper.insert(any(ProductDO.class))).thenAnswer(invocation -> {
            ProductDO product = invocation.getArgument(0);
            product.setId(1024L);
            return 1;
        });
        when(productImageMapper.selectListByProductIds(anyList())).thenReturn(Collections.emptyList());
        when(productTagService.getProductTagMap(anyCollection())).thenReturn(Collections.emptyMap());
    }

    @Test
    void createProduct_persistsAllAdr0035Fields() {
        ProductSaveReqVO reqVO = buildFullSaveReqVO();

        Long id = productService.createProduct(reqVO);

        assertEquals(1024L, id);
        ProductDO saved = captureInserted();
        // 字典 value + label 成对存储
        assertEquals("FINISHED", saved.getType());
        assertEquals("成品", saved.getTypeLabel());
        assertEquals("KNIT", saved.getFabricType());
        assertEquals("针织", saved.getFabricTypeLabel());
        // 织物结构与梭织估算方式
        assertEquals("WOVEN", saved.getFabricConstruction());
        assertEquals("PROFESSIONAL", saved.getWovenCalculationMode());
        // 梭织参数 5 列
        assertEquals(new BigDecimal("8880.00"), saved.getWovenTotalEnds());
        assertEquals(new BigDecimal("144.50"), saved.getWovenReedNo());
        assertEquals(new BigDecimal("52.00"), saved.getWovenWarpDensity());
        assertEquals(new BigDecimal("28.00"), saved.getWovenWeftDensity());
        assertEquals(new BigDecimal("30.00"), saved.getWovenLoomWeftDensity());
        // 坯布参数 2 列
        assertEquals(new BigDecimal("180.00"), saved.getGreigeWeightGsm());
        assertEquals(new BigDecimal("165.00"), saved.getGreigeWidthCm());
        // 加工费 3 列 + 包装费 + 税率
        assertEquals(new BigDecimal("6.5000"), saved.getKnitProcessingFeePerKg());
        assertEquals(new BigDecimal("1.2500"), saved.getWeavingFeePerMeter());
        assertEquals(new BigDecimal("0.8000"), saved.getOtherProcessingFeePerMeter());
        assertEquals(new BigDecimal("0.3000"), saved.getPackagingFee());
        assertEquals(new BigDecimal("0.1300"), saved.getTaxRate());
        // JSON 原文按字符串原样落库
        assertEquals(YARN_SPECS_JSON, saved.getYarnSpecs());
        assertEquals(COST_ITEMS_JSON, saved.getCostItems());
        // 供应商样号（原"原厂编号"标签更正，列名不变）
        assertEquals("SUP-001", saved.getSupplierCode());
        // 未显式传状态时缺省 ACTIVE
        assertEquals("ACTIVE", saved.getStatus());
    }

    @Test
    void updateProduct_persistsAllAdr0035Fields() {
        ProductSaveReqVO reqVO = buildFullSaveReqVO();
        reqVO.setId(1024L);
        when(productMapper.selectById(1024L)).thenReturn(new ProductDO());
        when(productMapper.selectByCode(reqVO.getCode())).thenReturn(null);

        productService.updateProduct(reqVO);

        ProductDO updated = captureUpdated();
        assertEquals(1024L, updated.getId());
        assertEquals("FINISHED", updated.getType());
        assertEquals("成品", updated.getTypeLabel());
        assertEquals("KNIT", updated.getFabricType());
        assertEquals("针织", updated.getFabricTypeLabel());
        assertEquals(new BigDecimal("8880.00"), updated.getWovenTotalEnds());
        assertEquals(new BigDecimal("30.00"), updated.getWovenLoomWeftDensity());
        assertEquals(new BigDecimal("6.5000"), updated.getKnitProcessingFeePerKg());
        assertEquals(new BigDecimal("0.1300"), updated.getTaxRate());
        assertEquals(YARN_SPECS_JSON, updated.getYarnSpecs());
        assertEquals(COST_ITEMS_JSON, updated.getCostItems());
    }

    @Test
    void getProduct_returnsNewLabelFieldsAndRawJson() {
        ProductDO product = new ProductDO();
        product.setId(1024L);
        product.setCode("P0001");
        product.setType("GREIGE");
        product.setTypeLabel("胚布");
        product.setFabricType("WOVEN");
        product.setFabricTypeLabel("梭织");
        product.setFabricConstruction("WOVEN");
        product.setWovenCalculationMode("SIMPLE");
        product.setWovenWarpDensity(new BigDecimal("52.00"));
        product.setGreigeWeightGsm(new BigDecimal("180.00"));
        product.setPackagingFee(new BigDecimal("0.3000"));
        product.setYarnSpecs(YARN_SPECS_JSON);
        product.setCostItems(COST_ITEMS_JSON);
        when(productMapper.selectById(1024L)).thenReturn(product);

        ProductRespVO respVO = productService.getProduct(1024L);

        // typeLabel / fabricTypeLabel 回传前端展示
        assertEquals("GREIGE", respVO.getType());
        assertEquals("胚布", respVO.getTypeLabel());
        assertEquals("WOVEN", respVO.getFabricType());
        assertEquals("梭织", respVO.getFabricTypeLabel());
        assertEquals("WOVEN", respVO.getFabricConstruction());
        assertEquals("SIMPLE", respVO.getWovenCalculationMode());
        assertEquals(new BigDecimal("52.00"), respVO.getWovenWarpDensity());
        assertEquals(new BigDecimal("180.00"), respVO.getGreigeWeightGsm());
        assertEquals(new BigDecimal("0.3000"), respVO.getPackagingFee());
        // JSON 原文原样返回，不做解析
        assertEquals(YARN_SPECS_JSON, respVO.getYarnSpecs());
        assertEquals(COST_ITEMS_JSON, respVO.getCostItems());
    }

    @Test
    void removedFields_absentFromDoAndVos() {
        for (Class<?> clazz : Arrays.asList(ProductDO.class, ProductSaveReqVO.class, ProductRespVO.class)) {
            List<String> declared = Arrays.stream(clazz.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toList());
            for (String removed : REMOVED_FIELDS) {
                assertFalse(declared.contains(removed),
                        clazz.getSimpleName() + " 仍声明已删除字段 " + removed);
            }
        }
    }

    @Test
    void productDo_declaresAllAdr0035Columns() {
        // DO 靠 map-underscore-to-camel-case 映射列名（@TableField 只声明更新策略、不覆盖列名），字段名拼错即静默丢列，此处兜底校验
        List<String> expectedColumns = Arrays.asList(
                "fabric_type", "fabric_type_label", "type_label",
                "woven_total_ends", "woven_reed_no", "woven_warp_density",
                "woven_weft_density", "woven_loom_weft_density",
                "fabric_construction", "woven_calculation_mode",
                "greige_weight_gsm", "greige_width_cm",
                "knit_processing_fee_per_kg", "weaving_fee_per_meter",
                "other_processing_fee_per_meter", "packaging_fee", "tax_rate");
        List<String> declared = Arrays.stream(ProductDO.class.getDeclaredFields())
                .map(field -> camelToSnake(field.getName())).collect(Collectors.toList());
        assertTrue(declared.containsAll(expectedColumns),
                "ProductDO 缺少 ADR-0035 列映射，实际：" + declared);
        // 已删列不应残留
        for (String removedColumn : Arrays.asList("output", "pattern_no", "temp_code", "conversion_rate")) {
            assertFalse(declared.contains(removedColumn), "ProductDO 仍映射已删除列 " + removedColumn);
        }
    }

    @Test
    void clearableColumns_useAlwaysUpdateStrategy() throws NoSuchFieldException {
        // 默认策略 NOT_NULL 会跳过 null：前端清空数字输入框后提交 null，库里旧值残留、重开又回显
        for (String fieldName : CLEARABLE_FIELDS) {
            TableField tableField = ProductDO.class.getDeclaredField(fieldName).getAnnotation(TableField.class);
            assertNotNull(tableField,
                    fieldName + " 缺少 @TableField(updateStrategy = FieldStrategy.ALWAYS)，用户清空后旧值会残留");
            assertEquals(FieldStrategy.ALWAYS, tableField.updateStrategy(),
                    fieldName + " 的更新策略应为 ALWAYS");
        }
    }

    @Test
    void deleteProduct_marksDeletedWithoutWipingClearableColumns() {
        ProductDO existing = new ProductDO();
        existing.setId(1024L);
        existing.setCode("P0001");
        existing.setStatus("ACTIVE");
        existing.setGreigeTypeId("gt-001");
        existing.setFabricType("WOVEN");
        existing.setFabricTypeLabel("梭织");
        existing.setWovenTotalEnds(new BigDecimal("8880.00"));
        existing.setWovenLoomWeftDensity(new BigDecimal("30.00"));
        when(productMapper.selectById(1024L)).thenReturn(existing);
        when(productMapper.selectByIds(anyCollection())).thenReturn(Collections.singletonList(existing));

        productService.deleteProduct(1024L);

        // 逻辑删除只置 status，不能借局部 DO 把 ALWAYS 列刷成 null
        ProductDO updated = captureUpdated();
        assertEquals("DELETED", updated.getStatus());
        assertEquals("gt-001", updated.getGreigeTypeId());
        assertEquals("WOVEN", updated.getFabricType());
        assertEquals("梭织", updated.getFabricTypeLabel());
        assertEquals(new BigDecimal("8880.00"), updated.getWovenTotalEnds());
        assertEquals(new BigDecimal("30.00"), updated.getWovenLoomWeftDensity());
    }

    // ==================== 私有方法 ====================

    private ProductDO captureInserted() {
        ArgumentCaptor<ProductDO> captor = ArgumentCaptor.forClass(ProductDO.class);
        verify(productMapper).insert(captor.capture());
        return captor.getValue();
    }

    private ProductDO captureUpdated() {
        ArgumentCaptor<ProductDO> captor = ArgumentCaptor.forClass(ProductDO.class);
        verify(productMapper).updateById(captor.capture());
        return captor.getValue();
    }

    /**
     * 构造含 ADR-0035 全部新增字段的保存入参（梭织经纬精算口径）
     */
    private ProductSaveReqVO buildFullSaveReqVO() {
        ProductSaveReqVO reqVO = new ProductSaveReqVO();
        reqVO.setCode("P0001");
        reqVO.setName("莫代尔汗布");
        reqVO.setType("FINISHED");
        reqVO.setTypeLabel("成品");
        reqVO.setComposition("NR");
        reqVO.setWidthCm(new BigDecimal("160"));
        reqVO.setWeightGsm(new BigDecimal("260"));
        reqVO.setSupplierCode("SUP-001");
        reqVO.setFabricType("KNIT");
        reqVO.setFabricTypeLabel("针织");
        reqVO.setFabricConstruction("WOVEN");
        reqVO.setWovenCalculationMode("PROFESSIONAL");
        reqVO.setWovenTotalEnds(new BigDecimal("8880.00"));
        reqVO.setWovenReedNo(new BigDecimal("144.50"));
        reqVO.setWovenWarpDensity(new BigDecimal("52.00"));
        reqVO.setWovenWeftDensity(new BigDecimal("28.00"));
        reqVO.setWovenLoomWeftDensity(new BigDecimal("30.00"));
        reqVO.setGreigeWeightGsm(new BigDecimal("180.00"));
        reqVO.setGreigeWidthCm(new BigDecimal("165.00"));
        reqVO.setKnitProcessingFeePerKg(new BigDecimal("6.5000"));
        reqVO.setWeavingFeePerMeter(new BigDecimal("1.2500"));
        reqVO.setOtherProcessingFeePerMeter(new BigDecimal("0.8000"));
        reqVO.setPackagingFee(new BigDecimal("0.3000"));
        reqVO.setTaxRate(new BigDecimal("0.1300"));
        reqVO.setYarnSpecs(YARN_SPECS_JSON);
        reqVO.setCostItems(COST_ITEMS_JSON);
        return reqVO;
    }

    /**
     * camelCase → snake_case，与 MyBatis-Plus map-underscore-to-camel-case 的映射口径一致
     */
    private static String camelToSnake(String name) {
        StringBuilder builder = new StringBuilder(name.length() + 8);
        for (char c : name.toCharArray()) {
            if (Character.isUpperCase(c)) {
                builder.append('_').append(Character.toLowerCase(c));
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }

}
