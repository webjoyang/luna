package com.luna.lunasrv.module.lunaerp.framework.datapermission.config;

import com.luna.lunasrv.framework.datapermission.core.rule.dept.DeptDataPermissionRuleCustomizer;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salescontract.SalesContractDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentDO;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * lunaerp 模块的数据权限 Configuration
 *
 * 注册销售归属链核心业务表到行级数据权限规则（ADR-0046）：
 * - CustomerDO（客户表）：dept_id / salesperson_user_id
 * - SalesContractDO（销售合同主表）：dept_id / salesperson_user_id
 * - ShipmentDO（发货单主表）：dept_id / salesperson_user_id
 * - QuotationDO 随 05 票规划落地
 * - 联系人表（零列继承客户）及所有子表（明细/布卷）不注册
 *
 * @author luna
 */
@Configuration(proxyBeanMethods = false)
public class LunaerpDataPermissionConfiguration {

    @Bean
    public DeptDataPermissionRuleCustomizer lunaerpDeptDataPermissionRuleCustomizer() {
        return rule -> {
            // 客户
            rule.addDeptColumn(CustomerDO.class, "dept_id");
            rule.addUserColumn(CustomerDO.class, "salesperson_user_id");

            // 销售合同
            rule.addDeptColumn(SalesContractDO.class, "dept_id");
            rule.addUserColumn(SalesContractDO.class, "salesperson_user_id");

            // 发货单
            rule.addDeptColumn(ShipmentDO.class, "dept_id");
            rule.addUserColumn(ShipmentDO.class, "salesperson_user_id");
        };
    }

}
