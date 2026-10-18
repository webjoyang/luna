package com.luna.lunasrv.module.lunaerp.business.salesreturn;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salesreturn.SalesReturnDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salesreturn.SalesReturnItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salesreturn.SalesReturnRollDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.stockin.StockInDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salesreturn.SalesReturnItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salesreturn.SalesReturnMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salesreturn.SalesReturnRollMapper;
import com.luna.lunasrv.module.lunaerp.enums.business.salesreturn.SalesReturnErrorCodeConstants;
import com.luna.lunasrv.module.lunaerp.enums.business.salesreturn.SalesReturnQcResultEnum;
import com.luna.lunasrv.module.lunaerp.enums.business.salesreturn.SalesReturnReasonEnum;
import com.luna.lunasrv.module.lunaerp.enums.business.salesreturn.SalesReturnStatusEnum;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 销售退货领域模型基建与架构规范单测（DO×3 + Mapper×3 + 枚举×3 + 错误码常量 + StockInDO扩展）
 * 覆盖 Issue: .scratch/sales-return/issues/01-sql-domain-foundation.md
 */
public class SalesReturnDomainFoundationTest {

    @Test
    @DisplayName("验证销售退货 DO 实体均继承 TenantBaseDO 并声明正确的 TableName 与 KeySequence")
    void testDataObjectsAnnotationsAndInheritance() {
        Class<?>[] doClasses = {
                SalesReturnDO.class,
                SalesReturnItemDO.class,
                SalesReturnRollDO.class
        };

        for (Class<?> clazz : doClasses) {
            assertTrue(TenantBaseDO.class.isAssignableFrom(clazz), clazz.getSimpleName() + " 必须继承 TenantBaseDO");
            assertTrue(clazz.isAnnotationPresent(TableName.class), clazz.getSimpleName() + " 必须标注 @TableName");
            assertTrue(clazz.isAnnotationPresent(KeySequence.class), clazz.getSimpleName() + " 必须标注 @KeySequence");

            TableName tn = clazz.getAnnotation(TableName.class);
            KeySequence ks = clazz.getAnnotation(KeySequence.class);

            assertTrue(tn.value().startsWith("lunaerp_business_sales_return"), "表名必须以 lunaerp_business_sales_return 开头: " + tn.value());
            assertEquals(tn.value() + "_seq", ks.value(), "KeySequence 必须与表名_seq一致");
        }
    }

    @Test
    @DisplayName("验证销售退货 DO 中无 @TableField(exist = false) 非持久化字段")
    void testDataObjectsHaveNoUnpersistedFields() {
        Class<?>[] doClasses = {
                SalesReturnDO.class,
                SalesReturnItemDO.class,
                SalesReturnRollDO.class
        };

        for (Class<?> clazz : doClasses) {
            for (Field field : clazz.getDeclaredFields()) {
                TableField tableField = field.getAnnotation(TableField.class);
                if (tableField != null) {
                    assertTrue(tableField.exist(), clazz.getSimpleName() + "." + field.getName() + " 严禁标注 @TableField(exist = false)");
                }
            }
        }
    }

    @Test
    @DisplayName("验证 DO 实体可空业务字段具备 FieldStrategy.ALWAYS 允许清空置空 (coding-standards.md §15)")
    void testNullableFieldsHaveAlwaysStrategy() throws NoSuchFieldException {
        // SalesReturnDO nullable update fields
        String[] nullableFieldsMain = {
                "salesContractId", "contractNo", "stockInId",
                "arrivedBy", "arrivedAt", "postedBy", "postedAt",
                "cancelledBy", "cancelledAt", "cancelReason", "remark"
        };
        for (String fieldName : nullableFieldsMain) {
            Field field = SalesReturnDO.class.getDeclaredField(fieldName);
            TableField tf = field.getAnnotation(TableField.class);
            assertNotNull(tf, "SalesReturnDO." + fieldName + " 必须标注 @TableField");
            assertEquals(FieldStrategy.ALWAYS, tf.updateStrategy(), "SalesReturnDO." + fieldName + " updateStrategy 必须为 ALWAYS");
        }

        // SalesReturnItemDO nullable update fields
        String[] nullableFieldsItem = {
                "customerColorNo", "colorName", "vatNo", "restockQty", "scrapQty", "remark"
        };
        for (String fieldName : nullableFieldsItem) {
            Field field = SalesReturnItemDO.class.getDeclaredField(fieldName);
            TableField tf = field.getAnnotation(TableField.class);
            assertNotNull(tf, "SalesReturnItemDO." + fieldName + " 必须标注 @TableField");
            assertEquals(FieldStrategy.ALWAYS, tf.updateStrategy(), "SalesReturnItemDO." + fieldName + " updateStrategy 必须为 ALWAYS");
        }

        // SalesReturnRollDO nullable update fields
        String[] nullableFieldsRoll = {
                "shipmentRollId", "weight", "lengthM", "qcResult", "qcBy", "qcAt", "qcRemark", "locationId", "locationCode", "remark"
        };
        for (String fieldName : nullableFieldsRoll) {
            Field field = SalesReturnRollDO.class.getDeclaredField(fieldName);
            TableField tf = field.getAnnotation(TableField.class);
            assertNotNull(tf, "SalesReturnRollDO." + fieldName + " 必须标注 @TableField");
            assertEquals(FieldStrategy.ALWAYS, tf.updateStrategy(), "SalesReturnRollDO." + fieldName + " updateStrategy 必须为 ALWAYS");
        }
    }

    @Test
    @DisplayName("验证 StockInDO 成功扩列 sourceDocType 与 sourceDocId 且具备 ALWAYS 更新策略")
    void testStockInDOAlteration() throws NoSuchFieldException {
        Field sourceDocTypeField = StockInDO.class.getDeclaredField("sourceDocType");
        Field sourceDocIdField = StockInDO.class.getDeclaredField("sourceDocId");

        assertEquals(String.class, sourceDocTypeField.getType(), "sourceDocType 必须为 String");
        assertEquals(Long.class, sourceDocIdField.getType(), "sourceDocId 必须为 Long");

        TableField tfType = sourceDocTypeField.getAnnotation(TableField.class);
        assertNotNull(tfType, "StockInDO.sourceDocType 必须标注 @TableField");
        assertEquals(FieldStrategy.ALWAYS, tfType.updateStrategy());

        TableField tfId = sourceDocIdField.getAnnotation(TableField.class);
        assertNotNull(tfId, "StockInDO.sourceDocId 必须标注 @TableField");
        assertEquals(FieldStrategy.ALWAYS, tfId.updateStrategy());
    }

    @Test
    @DisplayName("验证 MyBatis-Plus TableInfoHelper 初始化 3 个退货 DO 及 StockInDO 无异常")
    void testTableInfoInitialization() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");

        TableInfo tableInfoMain = TableInfoHelper.initTableInfo(assistant, SalesReturnDO.class);
        assertNotNull(tableInfoMain);
        assertEquals("lunaerp_business_sales_return", tableInfoMain.getTableName());

        TableInfo tableInfoItem = TableInfoHelper.initTableInfo(assistant, SalesReturnItemDO.class);
        assertNotNull(tableInfoItem);
        assertEquals("lunaerp_business_sales_return_item", tableInfoItem.getTableName());

        TableInfo tableInfoRoll = TableInfoHelper.initTableInfo(assistant, SalesReturnRollDO.class);
        assertNotNull(tableInfoRoll);
        assertEquals("lunaerp_business_sales_return_roll", tableInfoRoll.getTableName());

        TableInfo tableInfoStockIn = TableInfoHelper.initTableInfo(assistant, StockInDO.class);
        assertNotNull(tableInfoStockIn);
        assertEquals("lunaerp_stock_in", tableInfoStockIn.getTableName());
    }

    @Test
    @DisplayName("验证 Mapper 接口纯洁性：继承 BaseMapperX 且无 default 方法 (coding-standards.md §4)")
    void testMappersPureness() {
        Class<?>[] mappers = {
                SalesReturnMapper.class,
                SalesReturnItemMapper.class,
                SalesReturnRollMapper.class
        };

        for (Class<?> clazz : mappers) {
            assertTrue(clazz.isInterface(), clazz.getSimpleName() + " 必须是接口");
            assertTrue(BaseMapperX.class.isAssignableFrom(clazz), clazz.getSimpleName() + " 必须继承 BaseMapperX");

            for (Method method : clazz.getDeclaredMethods()) {
                assertFalse(method.isDefault(), clazz.getSimpleName() + "." + method.getName() + " 严禁定义 default 方法");
            }
        }
    }

    @Test
    @DisplayName("验证 3 个业务枚举的定义与有效性判定 (Status/Reason/QcResult)")
    void testSalesReturnEnums() {
        // StatusEnum
        assertEquals(5, SalesReturnStatusEnum.values().length);
        assertEquals("DRAFT", SalesReturnStatusEnum.DRAFT.getValue());
        assertEquals("APPROVED", SalesReturnStatusEnum.APPROVED.getValue());
        assertEquals("RECEIVED", SalesReturnStatusEnum.RECEIVED.getValue());
        assertEquals("POSTED", SalesReturnStatusEnum.POSTED.getValue());
        assertEquals("CANCELLED", SalesReturnStatusEnum.CANCELLED.getValue());
        assertTrue(SalesReturnStatusEnum.isValid("DRAFT"));
        assertFalse(SalesReturnStatusEnum.isValid("UNKNOWN"));

        // ReasonEnum
        assertEquals(5, SalesReturnReasonEnum.values().length);
        assertEquals("QUALITY", SalesReturnReasonEnum.QUALITY.getValue());
        assertEquals("WRONG", SalesReturnReasonEnum.WRONG.getValue());
        assertEquals("EXCESS", SalesReturnReasonEnum.EXCESS.getValue());
        assertEquals("CUSTOMER", SalesReturnReasonEnum.CUSTOMER.getValue());
        assertEquals("OTHER", SalesReturnReasonEnum.OTHER.getValue());
        assertTrue(SalesReturnReasonEnum.isValid("QUALITY"));
        assertFalse(SalesReturnReasonEnum.isValid("BAD"));

        // QcResultEnum
        assertEquals(2, SalesReturnQcResultEnum.values().length);
        assertEquals("RESTOCK", SalesReturnQcResultEnum.RESTOCK.getValue());
        assertEquals("SCRAP", SalesReturnQcResultEnum.SCRAP.getValue());
        assertTrue(SalesReturnQcResultEnum.isValid("RESTOCK"));
        assertTrue(SalesReturnQcResultEnum.isValid("SCRAP"));
        assertFalse(SalesReturnQcResultEnum.isValid("INVALID"));
    }

    @Test
    @DisplayName("验证销售退货错误码段位 1-030-037-000 ~ 014 定义完整性")
    void testSalesReturnErrorCodes() {
        assertEquals(1_030_037_000, SalesReturnErrorCodeConstants.SALES_RETURN_NOT_EXISTS.getCode());
        assertEquals(1_030_037_001, SalesReturnErrorCodeConstants.SALES_RETURN_NO_DUPLICATE.getCode());
        assertEquals(1_030_037_002, SalesReturnErrorCodeConstants.SALES_RETURN_STATUS_NOT_ALLOWED.getCode());
        assertEquals(1_030_037_003, SalesReturnErrorCodeConstants.SALES_RETURN_SHIPMENT_NOT_COMPLETED.getCode());
        assertEquals(1_030_037_004, SalesReturnErrorCodeConstants.SALES_RETURN_ITEM_LINEAGE_INVALID.getCode());
        assertEquals(1_030_037_005, SalesReturnErrorCodeConstants.SALES_RETURN_QTY_EXCEED_AVAILABLE.getCode());
        assertEquals(1_030_037_006, SalesReturnErrorCodeConstants.SALES_RETURN_AMOUNT_EXCEED_AVAILABLE.getCode());
        assertEquals(1_030_037_007, SalesReturnErrorCodeConstants.SALES_RETURN_ROLL_INVALID.getCode());
        assertEquals(1_030_037_008, SalesReturnErrorCodeConstants.SALES_RETURN_APPROVAL_NOT_APPROVED.getCode());
        assertEquals(1_030_037_009, SalesReturnErrorCodeConstants.SALES_RETURN_STOCK_IN_STATUS_INVALID.getCode());
        assertEquals(1_030_037_010, SalesReturnErrorCodeConstants.SALES_RETURN_ROLL_QC_UNRESOLVED.getCode());
        assertEquals(1_030_037_011, SalesReturnErrorCodeConstants.SALES_RETURN_RESTOCK_QTY_MISMATCH.getCode());
        assertEquals(1_030_037_012, SalesReturnErrorCodeConstants.SALES_RETURN_STOCK_IN_NOT_COMPLETED.getCode());
        assertEquals(1_030_037_013, SalesReturnErrorCodeConstants.SALES_RETURN_CURRENCY_MISMATCH.getCode());
        assertEquals(1_030_037_014, SalesReturnErrorCodeConstants.SALES_RETURN_EXCHANGE_RATE_MISSING.getCode());
        assertEquals(1_030_037_015, SalesReturnErrorCodeConstants.SALES_RETURN_REASON_EMPTY.getCode());
    }

}
