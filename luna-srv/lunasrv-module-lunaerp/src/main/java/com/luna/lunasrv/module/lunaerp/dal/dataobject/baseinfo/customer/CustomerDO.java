package com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 基本信息-客户表 DO
 *
 * 表：lunaerp_baseinfo_customer
 *
 * 字段范围：仅 A 组基础档案（companyName/shortName/phone/address/clientType/industry/
 * scale/source/cooperationStatus/unifiedSocialCreditCode/remark/status）。
 * 不引入 code、businessOwnership，亦无业务唯一约束（公司名允许重名）。
 *
 * @author luna
 */
@TableName("lunaerp_baseinfo_customer")
@KeySequence("lunaerp_baseinfo_customer_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
public class CustomerDO extends TenantBaseDO {

    /**
     * 客户ID
     */
    private Long id;
    /**
     * 公司名称（必填）
     */
    private String companyName;
    /**
     * 简称（为空时后端用 companyName 回填）
     */
    private String shortName;
    /**
     * 联系电话
     */
    private String phone;
    /**
     * 公司地址
     */
    private String address;
    /**
     * 客户类型（字典 lunaerp_customer_client_type：POTENTIAL 潜在 / FORMAL 正式 / CHURNED 流失）
     */
    private String clientType;
    /**
     * 所属行业（字典 lunaerp_customer_industry）
     */
    private String industry;
    /**
     * 企业规模（字典 lunaerp_customer_scale）
     */
    private String scale;
    /**
     * 客户来源（字典 lunaerp_customer_source）
     */
    private String source;
    /**
     * 合作状态（字典 lunaerp_customer_cooperation_status）
     */
    private String cooperationStatus;
    /**
     * 统一社会信用代码
     */
    private String unifiedSocialCreditCode;
    /**
     * 备注
     */
    private String remark;
    /**
     * 状态（ACTIVE 启用 / INACTIVE 停用），创建缺省 ACTIVE
     */
    private String status;

}
