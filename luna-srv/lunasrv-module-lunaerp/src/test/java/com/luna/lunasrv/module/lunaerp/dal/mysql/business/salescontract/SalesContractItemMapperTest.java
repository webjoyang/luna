package com.luna.lunasrv.module.lunaerp.dal.mysql.business.salescontract;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("销售合同明细 Mapper 测试")
class SalesContractItemMapperTest {

    @Test
    @DisplayName("updateReleasedQtyIncrement 方法签名、注解与 SQL 契约校验")
    void testUpdateReleasedQtyIncrementSignatureAndAnnotation() throws NoSuchMethodException {
        Method method = SalesContractItemMapper.class.getMethod("updateReleasedQtyIncrement", Long.class, BigDecimal.class);
        assertThat(method).isNotNull();
        assertThat(method.getReturnType()).isEqualTo(int.class);

        Update updateAnnotation = method.getAnnotation(Update.class);
        assertThat(updateAnnotation).isNotNull();
        assertThat(updateAnnotation.value()).containsExactly(
                "UPDATE lunaerp_business_sales_contract_item SET released_qty = released_qty + #{delta} WHERE id = #{id} AND released_qty + #{delta} <= quantity AND deleted = 0"
        );

        Param idParam = method.getParameters()[0].getAnnotation(Param.class);
        assertThat(idParam).isNotNull();
        assertThat(idParam.value()).isEqualTo("id");

        Param deltaParam = method.getParameters()[1].getAnnotation(Param.class);
        assertThat(deltaParam).isNotNull();
        assertThat(deltaParam.value()).isEqualTo("delta");
    }

}
