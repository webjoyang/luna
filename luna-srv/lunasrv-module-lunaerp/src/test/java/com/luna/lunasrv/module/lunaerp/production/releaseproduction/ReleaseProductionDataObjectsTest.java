package com.luna.lunasrv.module.lunaerp.production.releaseproduction;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salescontract.SalesContractItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.dyeingorder.color.DyeingColorDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.greigeorder.color.GreigeOrderColorDO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 针对合同「下达生产」涉及的 3 个 DO 实体新增字段的单元测试
 * 覆盖 issue: .scratch/release-production/issues/01-sql-and-do-fields.md
 */
public class ReleaseProductionDataObjectsTest {

    @Test
    @DisplayName("验证 SalesContractItemDO 包含 releasedQty 字段及 Getter/Setter")
    void testSalesContractItemDOReleasedQty() throws NoSuchFieldException {
        Field field = SalesContractItemDO.class.getDeclaredField("releasedQty");
        assertNotNull(field, "SalesContractItemDO 必须包含 releasedQty 字段");
        assertEquals(BigDecimal.class, field.getType(), "releasedQty 字段类型必须为 BigDecimal");

        SalesContractItemDO item = new SalesContractItemDO();
        item.setReleasedQty(new BigDecimal("150.00"));
        assertEquals(new BigDecimal("150.00"), item.getReleasedQty(), "releasedQty getter/setter 必须正常工作");
    }

    @Test
    @DisplayName("验证 GreigeOrderColorDO 包含 contractLineId/contractLineNo 字段及 Getter/Setter")
    void testGreigeOrderColorDOContractLineFields() throws NoSuchFieldException {
        Field lineIdField = GreigeOrderColorDO.class.getDeclaredField("contractLineId");
        assertNotNull(lineIdField, "GreigeOrderColorDO 必须包含 contractLineId 字段");
        assertEquals(Long.class, lineIdField.getType(), "contractLineId 字段类型必须为 Long");

        Field lineNoField = GreigeOrderColorDO.class.getDeclaredField("contractLineNo");
        assertNotNull(lineNoField, "GreigeOrderColorDO 必须包含 contractLineNo 字段");
        assertEquals(Integer.class, lineNoField.getType(), "contractLineNo 字段类型必须为 Integer");

        GreigeOrderColorDO color = new GreigeOrderColorDO();
        color.setContractLineId(1001L);
        color.setContractLineNo(1);
        assertEquals(1001L, color.getContractLineId());
        assertEquals(1, color.getContractLineNo());
    }

    @Test
    @DisplayName("验证 DyeingColorDO 包含 contractLineId/contractLineNo 字段及 ALWAYS 更新策略")
    void testDyeingColorDOContractLineFields() throws NoSuchFieldException {
        Field lineIdField = DyeingColorDO.class.getDeclaredField("contractLineId");
        assertNotNull(lineIdField, "DyeingColorDO 必须包含 contractLineId 字段");
        assertEquals(Long.class, lineIdField.getType(), "contractLineId 字段类型必须为 Long");

        TableField lineIdAnnotation = lineIdField.getAnnotation(TableField.class);
        assertNotNull(lineIdAnnotation, "contractLineId 必须声明 @TableField 注解");
        assertEquals(FieldStrategy.ALWAYS, lineIdAnnotation.updateStrategy(), "contractLineId updateStrategy 必须为 ALWAYS");

        Field lineNoField = DyeingColorDO.class.getDeclaredField("contractLineNo");
        assertNotNull(lineNoField, "DyeingColorDO 必须包含 contractLineNo 字段");
        assertEquals(Integer.class, lineNoField.getType(), "contractLineNo 字段类型必须为 Integer");

        TableField lineNoAnnotation = lineNoField.getAnnotation(TableField.class);
        assertNotNull(lineNoAnnotation, "contractLineNo 必须声明 @TableField 注解");
        assertEquals(FieldStrategy.ALWAYS, lineNoAnnotation.updateStrategy(), "contractLineNo updateStrategy 必须为 ALWAYS");

        DyeingColorDO color = new DyeingColorDO();
        color.setContractLineId(2001L);
        color.setContractLineNo(2);
        assertEquals(2001L, color.getContractLineId());
        assertEquals(2, color.getContractLineNo());
    }

}
