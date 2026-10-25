package com.luna.lunasrv.module.lunaerp.service.finance.account;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.account.vo.TransferCreateReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.account.vo.TransferPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.account.vo.TransferRespVO;
import jakarta.validation.Valid;

/**
 * 财务管理 - 资金账户间转账服务接口（ZZ 单）
 *
 * <p>两类单据登记即生效、不可改删（写账凭证，错账红冲二期）。不提供 update/delete 端点。</p>
 *
 * @author luna
 */
public interface TransferService {

    /**
     * 登记账户间转账（同事务写两条 TRANSFER 分录并双边条件更新余额，生成 ZZ 单）
     *
     * @param createReqVO 转账创建请求
     * @return 转账单ID
     */
    Long createTransfer(@Valid TransferCreateReqVO createReqVO);

    /**
     * 查询转账单详情
     *
     * @param id 转账单ID
     * @return 转账单详情
     */
    TransferRespVO getTransfer(Long id);

    /**
     * 分页查询转账单
     *
     * @param pageReqVO 分页查询请求
     * @return 分页结果
     */
    PageResult<TransferRespVO> getTransferPage(TransferPageReqVO pageReqVO);

}
