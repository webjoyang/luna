package com.luna.lunasrv.module.lunaerp.service.business.salescontract;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.business.salescontract.vo.SalesContractPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.business.salescontract.vo.SalesContractRespVO;
import com.luna.lunasrv.module.lunaerp.controller.business.salescontract.vo.SalesContractSaveReqVO;

/**
 * 业务管理-销售合同 Service 接口
 *
 * @author luna
 */
public interface SalesContractService {

    /**
     * 创建销售合同：contractNo 为空时自动生成（CH+年份+4位顺序号），
     * customerId 校验客户存在，clientName 为空时用客户公司名称回填，
     * signDate 缺省当天，status/tradeType/orderType/unit/currency 缺省值见实现类。
     *
     * @param createReqVO 合同
     * @return 合同ID
     */
    Long createSalesContract(SalesContractSaveReqVO createReqVO);

    /**
     * 更新销售合同：校验合同存在、客户存在；contractNo 变更时查重（含已删除行，见 ADR-0004）。
     *
     * @param updateReqVO 合同
     */
    void updateSalesContract(SalesContractSaveReqVO updateReqVO);

    /**
     * 删除销售合同：仅逻辑删除主表（本票无明细），不做状态联动。
     *
     * @param id 合同ID
     */
    void deleteSalesContract(Long id);

    /**
     * 获得销售合同详情
     *
     * @param id 合同ID
     * @return 合同
     */
    SalesContractRespVO getSalesContract(Long id);

    /**
     * 获得销售合同分页：keyword 对 contractNo/clientName/clientContractNo OR 模糊，
     * status/tradeType/customerId 等值过滤，默认 sign_date 倒序。
     *
     * @param reqVO 分页条件
     * @return 合同分页
     */
    PageResult<SalesContractRespVO> getSalesContractPage(SalesContractPageReqVO reqVO);

}
