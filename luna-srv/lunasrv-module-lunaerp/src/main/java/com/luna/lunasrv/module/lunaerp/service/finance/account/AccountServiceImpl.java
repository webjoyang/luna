package com.luna.lunasrv.module.lunaerp.service.finance.account;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.finance.account.vo.AccountPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.account.vo.AccountSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.account.AccountDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.account.AccountMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceAccountStatusEnum;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceAccountTypeEnum;
import com.luna.lunasrv.module.lunaerp.service.finance.ledger.LedgerService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants.*;

/**
 * 财务管理 - 资金账户服务实现类（域公共最小集）
 *
 * 核心守卫：
 * 1. opening_balance 仅创建时录入一次，后续只读（试图篡改抛 1_030_020_104 ACCOUNT_OPENING_BALANCE_IMMUTABLE）；
 * 2. 余额变更唯一通道为条件更新原子 SQL；
 * 3. 账户被分录引用时禁止物理/逻辑删除（抛 1_030_020_103 ACCOUNT_HAS_ENTRIES_DELETE_FORBIDDEN）；
 * 4. 账户名称租户内唯一（重复抛 1_030_020_102 ACCOUNT_NAME_DUPLICATE）；
 * 5. 出账余额不足或停用硬拦截。
 *
 * @author luna
 */
@Slf4j
@Service
@Validated
public class AccountServiceImpl implements AccountService {

    @Resource
    private AccountMapper accountMapper;

    @Resource
    private LedgerService ledgerService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAccount(AccountSaveReqVO reqVO) {
        // 1. 校验账户名称唯一性（含已删除）
        validateAccountNameUnique(null, reqVO.getAccountName());

        // 2. 期初余额与当前余额初始化
        BigDecimal opening = reqVO.getOpeningBalance() != null
                ? reqVO.getOpeningBalance().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);

        String status = StrUtil.isNotBlank(reqVO.getStatus())
                ? reqVO.getStatus()
                : FinanceAccountStatusEnum.ENABLE.getValue();

        String currency = StrUtil.isNotBlank(reqVO.getCurrency())
                ? reqVO.getCurrency()
                : "CNY";

        AccountDO account = AccountDO.builder()
                .accountName(reqVO.getAccountName())
                .accountType(reqVO.getAccountType())
                .currency(currency)
                .openingBalance(opening)
                .currentBalance(opening)
                .status(status)
                .remark(reqVO.getRemark())
                .build();

        accountMapper.insert(account);
        log.info("[createAccount] 创建资金账户成功: id={}, name={}, openingBalance={}",
                account.getId(), account.getAccountName(), account.getOpeningBalance());
        return account.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAccount(AccountSaveReqVO reqVO) {
        if (reqVO.getId() == null) {
            throw exception(ACCOUNT_NOT_EXISTS);
        }
        AccountDO existing = accountMapper.selectById(reqVO.getId());
        if (existing == null) {
            throw exception(ACCOUNT_NOT_EXISTS);
        }

        // 1. 期初余额只读守卫：若前端传递不同 openingBalance 则拒绝
        if (reqVO.getOpeningBalance() != null && existing.getOpeningBalance() != null
                && reqVO.getOpeningBalance().compareTo(existing.getOpeningBalance()) != 0) {
            throw exception(ACCOUNT_OPENING_BALANCE_IMMUTABLE);
        }

        // 2. 校验账户名称唯一性（修改时查重）
        if (!Objects.equals(existing.getAccountName(), reqVO.getAccountName())) {
            validateAccountNameUnique(existing.getId(), reqVO.getAccountName());
        }

        // 3. 仅更新允许修改的字段（名称、类型、状态、备注）
        AccountDO updateObj = new AccountDO();
        updateObj.setId(existing.getId());
        updateObj.setAccountName(reqVO.getAccountName());
        updateObj.setAccountType(reqVO.getAccountType());
        if (StrUtil.isNotBlank(reqVO.getStatus())) {
            updateObj.setStatus(reqVO.getStatus());
        }
        updateObj.setRemark(reqVO.getRemark());

        accountMapper.updateById(updateObj);
        log.info("[updateAccount] 更新资金账户成功: id={}, name={}, status={}",
                existing.getId(), reqVO.getAccountName(), reqVO.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAccount(Long id) {
        AccountDO existing = accountMapper.selectById(id);
        if (existing == null) {
            throw exception(ACCOUNT_NOT_EXISTS);
        }

        // 校验是否有分录引用该账户
        long count = ledgerService.countEntriesByAccountId(id);
        if (count > 0) {
            log.warn("[deleteAccount] 资金账户已被分录引用，禁止删除: id={}, entryCount={}", id, count);
            throw exception(ACCOUNT_HAS_ENTRIES_DELETE_FORBIDDEN);
        }

        accountMapper.deleteById(id);
        log.info("[deleteAccount] 删除资金账户成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBalance(Long id, BigDecimal amt) {
        if (id == null) {
            throw exception(ACCOUNT_NOT_EXISTS);
        }
        if (amt == null || amt.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        // 执行条件更新
        int rows = accountMapper.updateBalanceConditional(id, amt);
        if (rows > 0) {
            log.info("[updateBalance] 资金账户余额条件更新成功: id={}, amt={}", id, amt);
            return;
        }

        // 更新未影响行数，诊断原因并抛出精确业务异常
        AccountDO existing = accountMapper.selectById(id);
        if (existing == null) {
            throw exception(ACCOUNT_NOT_EXISTS);
        }
        if (!FinanceAccountStatusEnum.ENABLE.getValue().equalsIgnoreCase(existing.getStatus())) {
            log.warn("[updateBalance] 资金账户已停用: id={}, status={}", id, existing.getStatus());
            throw exception(ACCOUNT_DISABLED);
        }
        if (amt.compareTo(BigDecimal.ZERO) < 0) {
            BigDecimal afterBalance = existing.getCurrentBalance().add(amt);
            if (afterBalance.compareTo(BigDecimal.ZERO) < 0) {
                log.warn("[updateBalance] 资金账户余额不足: id={}, current={}, deduct={}",
                        id, existing.getCurrentBalance(), amt.abs());
                throw exception(PAYMENT_ACCOUNT_BALANCE_NOT_ENOUGH);
            }
        }

        // 其他并发冲突或异常
        throw exception(PAYMENT_ACCOUNT_NOT_AVAILABLE);
    }

    @Override
    public AccountDO getAccount(Long id) {
        return accountMapper.selectById(id);
    }

    @Override
    public List<AccountDO> getAccountList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return accountMapper.selectBatchIds(ids);
    }

    @Override
    public List<AccountDO> getSimpleAccountList() {
        return accountMapper.selectListByStatusEnable();
    }

    @Override
    public PageResult<AccountDO> getAccountPage(AccountPageReqVO reqVO) {
        return accountMapper.selectPage(reqVO, new LambdaQueryWrapperX<AccountDO>()
                .likeIfPresent(AccountDO::getAccountName, reqVO.getAccountName())
                .eqIfPresent(AccountDO::getAccountType, reqVO.getAccountType())
                .eqIfPresent(AccountDO::getStatus, reqVO.getStatus())
                .orderByDesc(AccountDO::getId));
    }

    private void validateAccountNameUnique(Long id, String accountName) {
        if (StrUtil.isBlank(accountName)) {
            return;
        }
        AccountDO candidate = accountMapper.selectByAccountNameIncludingDeleted(accountName);
        if (candidate != null && !candidate.getId().equals(id)) {
            throw exception(ACCOUNT_NAME_DUPLICATE);
        }
    }

}
