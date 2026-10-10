package com.luna.lunasrv.module.lunaerp.service.production.yarnprice;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.common.dataimport.ExcelImportResult;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnprice.vo.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 生产管理 - 纱价主档 Service 接口
 *
 * @author luna
 */
public interface YarnPriceService {

    /**
     * 创建纱价记录
     */
    Long createYarnPrice(YarnPriceSaveReqVO createReqVO);

    /**
     * 修改纱价记录
     */
    void updateYarnPrice(YarnPriceSaveReqVO updateReqVO);

    /**
     * 删除纱价记录
     */
    void deleteYarnPrice(Long id);

    /**
     * 获得纱价记录
     */
    YarnPriceRespVO getYarnPrice(Long id);

    /**
     * 分页查询纱价
     */
    PageResult<YarnPriceRespVO> getYarnPricePage(YarnPricePageReqVO pageReqVO);

    /**
     * 查询指定规格的全部价格记录（按日期倒序）
     */
    List<YarnPriceRespVO> listBySpec(Long yarnSpecId, String priceType);

    /**
     * 查询最近一条已审核/存在的采购明细单价（只读聚合）
     */
    BigDecimal getLatestPurchasedPrice(Long yarnSpecId);

    /**
     * 内部取价核心方法：取不晚于指定日期的最新一条生效价，无记录回退 ref_price
     */
    YarnPriceEffectiveRespVO getEffectivePrice(Long yarnSpecId, String priceType, LocalDate date);

    /**
     * 获得纱价 Excel 导入模板
     */
    List<YarnPriceImportVO> getImportTemplate();

    /**
     * 批量导入纱价
     */
    ExcelImportResult importYarnPrice(MultipartFile file) throws IOException;

}
