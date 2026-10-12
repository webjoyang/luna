package com.luna.lunasrv.module.lunaerp.service.finance.account;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.account.vo.AccountPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.account.vo.AccountSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.account.AccountDO;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

/**
 * 财务管理 - 资金账户服务接口（域公共最小集）
 *
 * @author luna
 */
public interface AccountService {

    /**
     * 创建资金账户
     * 规则：opening_balance 仅在此处可录入（current_balance = opening_balance），后续只读
     *
     * @param reqVO 创建请求
     * @return 账户ID
     */
    Long createAccount(AccountSaveReqVO reqVO);

    /**
     * 更新资金账户
     * 规则：仅允许修改名称、类型、备注、状态（ENABLE/DISABLE）；期初余额与当前余额不可通过此端点修改
     *
     * @param reqVO 更新请求
     */
    void updateAccount(AccountSaveReqVO reqVO);

    /**
     * 删除资金账户
     * 规则：若已被分录引用，禁止删除，抛出 ACCOUNT_HAS_ENTRIES_DELETE_FORBIDDEN (1_030_020_103)
     *
     * @param id 账户ID
     */
    void deleteAccount(Long id);

    /**
     * 条件原子更新资金账户余额（单据过账唯一余额变更通道）
     * 规则：
     * UPDATE lunaerp_finance_account SET current_balance = current_balance + #{amt}
     * WHERE id=#{id} AND status='ENABLE' AND (current_balance + #{amt} >= 0 OR #{amt} >= 0)
     *
     * @param id 账户ID
     * @param amt 增量金额（正数为入账，负数为出账）
     */
    void updateBalance(Long id, BigDecimal amt);

    /**
     * 获取资金账户详情
     *
     * @param id 账户ID
     * @return 账户DO
     */
    AccountDO getAccount(Long id);

    /**
     * 批量获取资金账户
     *
     * @param ids 账户ID集合
     * @return 账户列表
     */
    List<AccountDO> getAccountList(Collection<Long> ids);

    /**
     * 获取启用状态的资金账户简要列表（下拉选单）
     *
     * @return 启用账户列表
     */
    List<AccountDO> getSimpleAccountList();

    /**
     * 分页查询资金账户
     *
     * @param reqVO 分页条件
     * @return 分页结果
     */
    PageResult<AccountDO> getAccountPage(AccountPageReqVO reqVO);

}
