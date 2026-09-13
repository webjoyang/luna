package com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.product;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 基本信息-产品图片表 DO
 *
 * 表：lunaerp_baseinfo_product_image
 *
 * @author luna
 */
@TableName("lunaerp_baseinfo_product_image")
@KeySequence("lunaerp_baseinfo_product_image_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductImageDO extends TenantBaseDO {

    /**
     * 图片ID
     */
    private Long id;
    /**
     * 产品ID（历史 UUID 字符串；新产品为产品主键的字符串形式）
     */
    private String productId;
    /**
     * 图片URL
     */
    private String url;
    /**
     * 排序序号（详情页展示顺序）
     */
    private Integer sortOrder;

}
