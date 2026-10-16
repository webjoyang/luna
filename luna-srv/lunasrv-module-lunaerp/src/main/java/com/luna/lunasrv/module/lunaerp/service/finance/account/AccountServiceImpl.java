package com.luna.lunasrv.module.lunaerp.service.finance.account;

import cn.hutool.core.util.StrUtil;
import com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil;
import com.luna.lunasrv.module.lunaerp.controller.finance.account.vo.AccountRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.account.vo.AccountSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.account.vo.AccountUpdateReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.account.AccountDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.account.AccountMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants;
import com.luna.lunasrv.module.lunaerp.service.finance.ledger.LedgerService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;

/**
 * 财务管理 - 资金账户 Service 实现类
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class AccountServiceImpl implements AccountService {

    public static final String STATUS_ENABLE = "ENABLE";
    public static final String STATUS_DISABLE = "DISABLE";
    public static final String DEFAULT_CURRENCY = "RMB";

    @Resource
    private AccountMapper accountMapper;

    @Resource
    private LedgerService ledgerService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAccount(AccountSaveReqVO createReqVO) {
        // 1. 校验账户名称唯一性
        validateAccountNameUnique(null, createReqVO.getAccountName());

        // 2. 构建实体
        AccountDO account = new AccountDO();
        account.setAccountName(createReqVO.getAccountName());
        account.setAccountType(createReqVO.getAccountType());
        account.setCurrency(StrUtil.isNotBlank(createReqVO.getCurrency()) ? createReqVO.getCurrency() : DEFAULT_CURRENCY);
        BigDecimal opening = createReqVO.getOpeningBalance() != null ? createReqVO.getOpeningBalance() : BigDecimal.ZERO;
        account.setOpeningBalance(opening);
        account.setCurrentBalance(opening); // 物化当前余额初始化为期初余额
        account.setStatus(STATUS_ENABLE);
        account.setRemark(createReqVO.getRemark());

        accountMapper.insert(account);
        log.info("[AccountService] 成功创建资金账户: id={}, name={}, type={}, openingBalance={}",
                account.getId(), account.getAccountName(), account.getAccountType(), account.getOpeningBalance());
        return account.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAccount(AccountUpdateReqVO updateReqVO) {
        // 1. 校验账户存在
        AccountDO existing = validateAccountExists(updateReqVO.getId());

        // 2. 校验账户名称唯一性
        validateAccountNameUnique(updateReqVO.getId(), updateReqVO.getAccountName());

        // 3. 仅更新备注、状态、名称（期初与当前余额只读不可变）
        existing.setAccountName(updateReqVO.getAccountName());
        existing.setStatus(updateReqVO.getStatus());
        existing.setRemark(updateReqVO.getRemark());

        accountMapper.updateById(existing);
        log.info("[AccountService] 成功修改资金账户: id={}, name={}, status={}",
                existing.getId(), existing.getAccountName(), existing.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAccount(Long id) {
        // 1. 校验账户存在
        validateAccountExists(id);

        // 2. 检查是否有分录引用（有分录引用报 1_030_020_103 ACCOUNT_HAS_ENTRIES_DELETE_FORBIDDEN）
        Long entryCount = ledgerService.countEntriesByAccountId(id);
        if (entryCount != null && entryCount > 0) {
            log.warn("[AccountService] 资金账户存在分录引用，禁止删除: id={}, entryCount={}", id, entryCount);
            throw exception(FinanceErrorCodeConstants.ACCOUNT_HAS_ENTRIES_DELETE_FORBIDDEN);
        }

        accountMapper.deleteById(id);
        log.info("[AccountService] 成功删除资金账户: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBalance(Long id, BigDecimal amountDelta) {
        if (amountDelta == null || amountDelta.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        int affectedRows;
        if (amountDelta.compareTo(BigDecimal.ZERO) > 0) {
            // 流入
            affectedRows = accountMapper.updateBalanceIncrease(id, amountDelta);
        } else {
            // 流出：校验当前余额 >= abs(amountDelta)
            BigDecimal absAmount = amountDelta.abs();
            affectedRows = accountMapper.updateBalanceDecrease(id, amountDelta, absAmount);
        }

        if (affectedRows == 0) {
            // 条件更新失败，定位原因抛出精准业务异常
            AccountDO account = validateAccountExists(id);
            if (!STATUS_ENABLE.equals(account.getStatus())) {
                log.warn("[AccountService] 资金账户已停用，条件更新失败: id={}, status={}", id, account.getStatus());
                throw exception(FinanceErrorCodeConstants.ACCOUNT_DISABLED);
            }
            if (amountDelta.compareTo(BigDecimal.ZERO) < 0) {
                log.warn("[AccountService] 资金账户余额不足，条件更新失败: id={}, currentBalance={}, required={}",
                        id, account.getCurrentBalance(), amountDelta.abs());
                throw exception(FinanceErrorCodeConstants.PAYMENT_ACCOUNT_BALANCE_NOT_ENOUGH);
            }
            // 其他未知受阻
            throw exception(FinanceErrorCodeConstants.ACCOUNT_DISABLED);
        }

        log.info("[AccountService] 资金账户余额条件更新成功: id={}, amountDelta={}", id, amountDelta);
    }

    @Override
    public AccountDO validateAccountExists(Long id) {
        if (id == null) {
            throw exception(FinanceErrorCodeConstants.ACCOUNT_NOT_EXISTS);
        }
        AccountDO account = accountMapper.selectById(id);
        if (account == null) {
            throw exception(FinanceErrorCodeConstants.ACCOUNT_NOT_EXISTS);
        }
        return account;
    }

    @Override
    public AccountDO validateAccountAvailable(Long id) {
        AccountDO account = validateAccountExists(id);
        if (!STATUS_ENABLE.equals(account.getStatus())) {
            log.warn("[AccountService] 资金账户不可用（已停用）: id={}, status={}", id, account.getStatus());
            throw exception(FinanceErrorCodeConstants.ACCOUNT_DISABLED);
        }
        return account;
    }

    @Override
    public AccountRespVO getAccount(Long id) {
        AccountDO account = validateAccountExists(id);
        AccountRespVO respVO = new AccountRespVO();
        respVO.setId(account.getId());
        respVO.setAccountName(account.getAccountName());
        respVO.setAccountType(account.getAccountType());
        respVO.setCurrency(account.getCurrency());
        respVO.setOpeningBalance(account.getOpeningBalance());
        respVO.setCurrentBalance(account.getCurrentBalance());
        respVO.setStatus(account.getStatus());
        respVO.setRemark(account.getRemark());
        respVO.setCreateTime(account.getCreateTime());
        return respVO;
    }

    private void validateAccountNameUnique(Long id, String accountName) {
        if (StrUtil.isBlank(accountName)) {
            return;
        }
        AccountDO existing = accountMapper.selectByAccountName(accountName);
        if (existing == null) {
            return;
        }
        if (id == null || !existing.getId().equals(id)) {
            throw exception(FinanceErrorCodeConstants.ACCOUNT_NAME_DUPLICATE);
        }
    }

}
