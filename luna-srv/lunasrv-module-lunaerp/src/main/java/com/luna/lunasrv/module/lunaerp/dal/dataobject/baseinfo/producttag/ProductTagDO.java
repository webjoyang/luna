package com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.producttag;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 基本信息-产品自定义标签表 DO（租户级字典）
 *
 * 表：lunaerp_baseinfo_product_tag
 *
 * @author luna
 */
@TableName("lunaerp_baseinfo_product_tag")
@KeySequence("lunaerp_baseinfo_product_tag_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductTagDO extends TenantBaseDO {

    /**
     * 标签ID
     */
    private Long id;
    /**
     * 标签名称（同租户内同名共用颜色）
     */
    private String name;
    /**
     * 标签颜色（默认 graphite）
     */
    private String color;
    /**
     * 创建人ID
     */
    private String createdById;

}
