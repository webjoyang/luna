package com.luna.lunasrv.module.lunaerp.service.finance.account;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.account.vo.OtherTxnCreateReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.account.vo.OtherTxnPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.account.vo.OtherTxnRespVO;
import jakarta.validation.Valid;

/**
 * 财务管理 - 其他收支服务接口（QT 单）
 *
 * <p>两类单据登记即生效、不可改删（写账凭证，错账红冲二期）。不提供 update/delete 端点。</p>
 *
 * @author luna
 */
public interface OtherTxnService {

    /**
     * 登记其他收支（同事务写经营分录并条件更新账户余额，生成 QT 单）
     *
     * @param createReqVO 其他收支创建请求
     * @return 其他收支单ID
     */
    Long createOtherTxn(@Valid OtherTxnCreateReqVO createReqVO);

    /**
     * 查询其他收支单详情
     *
     * @param id 其他收支单ID
     * @return 其他收支单详情
     */
    OtherTxnRespVO getOtherTxn(Long id);

    /**
     * 分页查询其他收支单
     *
     * @param pageReqVO 分页查询请求
     * @return 分页结果
     */
    PageResult<OtherTxnRespVO> getOtherTxnPage(OtherTxnPageReqVO pageReqVO);

}
