package com.luna.lunasrv.module.lunaerp.service.finance.account;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.collection.CollectionUtils;
import com.luna.lunasrv.module.lunaerp.controller.finance.account.vo.AccountPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.account.vo.AccountRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.account.vo.AccountSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.account.vo.AccountUpdateReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.account.vo.FinanceAccountSimpleRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.account.AccountDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.account.AccountMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants;
import com.luna.lunasrv.module.lunaerp.service.finance.ledger.LedgerService;
import com.mzt.logapi.starter.annotation.LogRecord;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.List;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

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
    public static final String DEFAULT_NATURE = "PUBLIC";

    @Resource
    private AccountMapper accountMapper;

    @Resource
    private LedgerService ledgerService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "FINANCE_ACCOUNT", subType = "CREATE", bizNo = "{{#result}}",
            success = "创建资金账户【{{#createReqVO.accountName}}】")
    public Long createAccount(AccountSaveReqVO createReqVO) {
        // 1. 账号按类型条件必填（BANK/WECHAT/ALIPAY 必填，CASH 可空，020_105）
        validateAccountNo(createReqVO.getAccountType(), createReqVO.getAccountNo());

        // 2. 校验账户名称唯一性（含已删除行，020_102）
        validateAccountNameUnique(null, createReqVO.getAccountName());

        // 3. 构建实体
        AccountDO account = new AccountDO();
        account.setAccountName(createReqVO.getAccountName());
        account.setAccountType(createReqVO.getAccountType());
        account.setAccountNo(createReqVO.getAccountNo());
        account.setAccountNature(StrUtil.isNotBlank(createReqVO.getAccountNature()) ? createReqVO.getAccountNature() : DEFAULT_NATURE);
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
    @LogRecord(type = "FINANCE_ACCOUNT",
            subType = "{{#updateReqVO.status != null ? 'STATUS_CHANGE' : (#updateReqVO.openingBalance != null ? 'OPENING_UPDATE' : 'UPDATE')}}",
            bizNo = "{{#updateReqVO.id}}",
            success = "修改资金账户【{{#updateReqVO.id}}】")
    public void updateAccount(AccountUpdateReqVO updateReqVO) {
        // 1. 校验账户存在
        AccountDO existing = validateAccountExists(updateReqVO.getId());

        // 2. 校验名称唯一性（含已删除行，020_102）
        if (StrUtil.isNotBlank(updateReqVO.getAccountName()) && !updateReqVO.getAccountName().equals(existing.getAccountName())) {
            validateAccountNameUnique(updateReqVO.getId(), updateReqVO.getAccountName());
            existing.setAccountName(updateReqVO.getAccountName());
        }

        // 3. 账号条件校验（BANK/WECHAT/ALIPAY 必填，020_105）
        if (updateReqVO.getAccountNo() != null) {
            validateAccountNo(existing.getAccountType(), updateReqVO.getAccountNo());
            existing.setAccountNo(updateReqVO.getAccountNo());
        }

        // 4. 账户性质更新
        if (StrUtil.isNotBlank(updateReqVO.getAccountNature())) {
            existing.setAccountNature(updateReqVO.getAccountNature());
        }

        // 5. 期初余额修改守卫：仅无分录可改，否则 020_104
        if (updateReqVO.getOpeningBalance() != null) {
            BigDecimal oldOpening = existing.getOpeningBalance() != null ? existing.getOpeningBalance() : BigDecimal.ZERO;
            if (updateReqVO.getOpeningBalance().compareTo(oldOpening) != 0) {
                Long entryCount = ledgerService.countEntriesByAccountId(existing.getId());
                if (entryCount != null && entryCount > 0) {
                    log.warn("[AccountService] 账户存在经营分录，禁止修改期初余额: accountId={}, entryCount={}", existing.getId(), entryCount);
                    throw exception(FinanceErrorCodeConstants.ACCOUNT_OPENING_BALANCE_IMMUTABLE);
                }
                existing.setOpeningBalance(updateReqVO.getOpeningBalance());
                existing.setCurrentBalance(updateReqVO.getOpeningBalance()); // 零分录时期初与当前余额同步
            }
        }

        // 6. 状态与备注更新（防快捷操作置空历史备注，coding-standards.md §15.3）
        if (StrUtil.isNotBlank(updateReqVO.getStatus())) {
            String status = updateReqVO.getStatus().trim();
            if (!STATUS_ENABLE.equals(status) && !"DISABLE".equals(status)) {
                throw invalidParamException("账户状态必须为 ENABLE 或 DISABLE");
            }
            existing.setStatus(status);
        }
        if (updateReqVO.getRemark() != null) {
            existing.setRemark(updateReqVO.getRemark());
        }

        accountMapper.updateById(existing);
        log.info("[AccountService] 成功修改资金账户: id={}, name={}, status={}",
                existing.getId(), existing.getAccountName(), existing.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "FINANCE_ACCOUNT", subType = "DELETE", bizNo = "{{#id}}",
            success = "删除资金账户【{{#id}}】")
    public void deleteAccount(Long id) {
        // 1. 校验账户存在
        validateAccountExists(id);

        // 2. 检查是否有分录引用（有分录引用报 1_030_020_103 友好提示"已有分录请停用"）
        Long entryCount = ledgerService.countEntriesByAccountId(id);
        if (entryCount != null && entryCount > 0) {
            log.warn("[AccountService] 资金账户存在分录引用，禁止删除: id={}, entryCount={}", id, entryCount);
            throw exception(FinanceErrorCodeConstants.ACCOUNT_HAS_ENTRIES_DELETE_FORBIDDEN);
        }

        accountMapper.deleteById(id);
        log.info("[AccountService] 成功删除资金账户进入回收站: id={}", id);
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
        respVO.setAccountNo(account.getAccountNo());
        respVO.setAccountNature(account.getAccountNature());
        respVO.setCurrency(account.getCurrency());
        respVO.setOpeningBalance(account.getOpeningBalance());
        respVO.setCurrentBalance(account.getCurrentBalance());
        respVO.setStatus(account.getStatus());
        respVO.setRemark(account.getRemark());
        respVO.setCreateTime(account.getCreateTime());
        return respVO;
    }

    @Override
    public PageResult<AccountRespVO> getAccountPage(AccountPageReqVO pageReqVO) {
        LambdaQueryWrapper<AccountDO> wrapper = new LambdaQueryWrapper<AccountDO>()
                .like(StrUtil.isNotBlank(pageReqVO.getAccountName()), AccountDO::getAccountName, pageReqVO.getAccountName())
                .eq(StrUtil.isNotBlank(pageReqVO.getAccountType()), AccountDO::getAccountType, pageReqVO.getAccountType())
                .eq(StrUtil.isNotBlank(pageReqVO.getAccountNature()), AccountDO::getAccountNature, pageReqVO.getAccountNature())
                .eq(StrUtil.isNotBlank(pageReqVO.getCurrency()), AccountDO::getCurrency, pageReqVO.getCurrency())
                .eq(StrUtil.isNotBlank(pageReqVO.getStatus()), AccountDO::getStatus, pageReqVO.getStatus())
                .orderByDesc(AccountDO::getId);
        PageResult<AccountDO> page = accountMapper.selectPage(pageReqVO, wrapper);
        return CollectionUtils.convertPage(page, account -> {
            AccountRespVO respVO = new AccountRespVO();
            respVO.setId(account.getId());
            respVO.setAccountName(account.getAccountName());
            respVO.setAccountType(account.getAccountType());
            respVO.setAccountNo(account.getAccountNo());
            respVO.setAccountNature(account.getAccountNature());
            respVO.setCurrency(account.getCurrency());
            respVO.setOpeningBalance(account.getOpeningBalance());
            respVO.setCurrentBalance(account.getCurrentBalance());
            respVO.setStatus(account.getStatus());
            respVO.setRemark(account.getRemark());
            respVO.setCreateTime(account.getCreateTime());
            return respVO;
        });
    }


    @Override
    public List<FinanceAccountSimpleRespVO> getSimpleAccountList() {
        List<AccountDO> list = accountMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AccountDO>()
                .eq(AccountDO::getStatus, STATUS_ENABLE)
                .orderByAsc(AccountDO::getId));
        return com.luna.lunasrv.framework.common.util.collection.CollectionUtils.convertList(list, a -> {
            FinanceAccountSimpleRespVO vo = new FinanceAccountSimpleRespVO();
            vo.setId(a.getId());
            vo.setAccountName(a.getAccountName());
            vo.setAccountType(a.getAccountType());
            vo.setCurrency(a.getCurrency());
            vo.setStatus(a.getStatus());
            vo.setCurrentBalance(a.getCurrentBalance());
            return vo;
        });
    }

    private void validateAccountNo(String accountType, String accountNo) {
        if (StrUtil.isBlank(accountType)) {
            return;
        }
        String upper = accountType.trim().toUpperCase();
        if ("BANK".equals(upper) || "WECHAT".equals(upper) || "ALIPAY".equals(upper)) {
            if (StrUtil.isBlank(accountNo)) {
                throw exception(FinanceErrorCodeConstants.ACCOUNT_NO_REQUIRED);
            }
        }
    }

    private void validateAccountNameUnique(Long id, String accountName) {
        if (StrUtil.isBlank(accountName)) {
            return;
        }
        AccountDO existing = accountMapper.selectByAccountNameIncludingDeleted(accountName);
        if (existing == null) {
            return;
        }
        if (id == null || !existing.getId().equals(id)) {
            throw exception(FinanceErrorCodeConstants.ACCOUNT_NAME_DUPLICATE);
        }
    }

}
