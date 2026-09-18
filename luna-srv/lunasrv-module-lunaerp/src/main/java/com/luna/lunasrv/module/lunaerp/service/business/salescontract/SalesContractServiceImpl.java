package com.luna.lunasrv.module.lunaerp.service.business.salescontract;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.business.salescontract.vo.SalesContractPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.business.salescontract.vo.SalesContractRespVO;
import com.luna.lunasrv.module.lunaerp.controller.business.salescontract.vo.SalesContractSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salescontract.SalesContractDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salescontract.SalesContractMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.CUSTOMER_NOT_EXISTS;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.SALES_CONTRACT_NOT_EXISTS;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.SALES_CONTRACT_NO_DUPLICATE;

/**
 * 业务管理-销售合同 Service 实现类
 *
 * 编号生成沿用产品模块 getNextCode 的数字后缀扫描方式（不引 Redis）；
 * 取号扫描与查重范围包含已删除行，见 ADR-0004。
 *
 * @author luna
 */
@Service
public class SalesContractServiceImpl implements SalesContractService {

    /**
     * 合同编号前缀
     */
    private static final String CONTRACT_NO_PREFIX = "CH";
    /**
     * 编号数字部分最小补零位数（超 9999 自然扩位）
     */
    private static final int NO_MIN_DIGITS = 4;
    /**
     * 自动取号撞唯一索引时的最大顺延重试次数
     */
    private static final int MAX_GENERATE_RETRY = 3;
    /**
     * 编号前缀（CH+年份）后的数字部分
     */
    private static final Pattern NO_SUFFIX_NUMBER_PATTERN = Pattern.compile("^\\d+");

    /**
     * 合同默认状态
     */
    private static final String DEFAULT_STATUS = "ACTIVE";
    /**
     * 贸易类型缺省：内销
     */
    private static final String DEFAULT_TRADE_TYPE = "DOMESTIC";
    /**
     * 订单属性缺省：大货
     */
    private static final String DEFAULT_ORDER_TYPE = "BULK";
    /**
     * 单位缺省：千克
     */
    private static final String DEFAULT_UNIT = "KG";
    /**
     * 币种缺省：人民币
     */
    private static final String DEFAULT_CURRENCY = "RMB";

    @Resource
    private SalesContractMapper salesContractMapper;

    @Resource
    private CustomerMapper customerMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSalesContract(SalesContractSaveReqVO createReqVO) {
        // 1.1 校验客户存在（customerId 必填由 VO @NotNull 保证，此处兜底 + 快照回填来源）
        CustomerDO customer = validateCustomerExists(createReqVO.getCustomerId());
        // 1.2 组装 DO
        SalesContractDO contract = BeanUtils.toBean(createReqVO, SalesContractDO.class);
        // 1.3 编号：手填查重（含已删除行，撞号直接报错不顺延）；留空自动生成（撞号有限次顺延重试）
        if (StringUtils.hasText(contract.getContractNo())) {
            validateContractNoUnique(null, contract.getContractNo());
        } else {
            contract.setContractNo(generateContractNoWithRetry());
        }
        // 1.4 客户名称快照：为空时用客户公司名称回填
        if (!StringUtils.hasText(contract.getClientName())) {
            contract.setClientName(customer.getCompany());
        }
        // 1.5 缺省值
        if (contract.getSignDate() == null) {
            contract.setSignDate(LocalDate.now());
        }
        if (!StringUtils.hasText(contract.getStatus())) {
            contract.setStatus(DEFAULT_STATUS);
        }
        if (!StringUtils.hasText(contract.getTradeType())) {
            contract.setTradeType(DEFAULT_TRADE_TYPE);
        }
        if (!StringUtils.hasText(contract.getOrderType())) {
            contract.setOrderType(DEFAULT_ORDER_TYPE);
        }
        if (!StringUtils.hasText(contract.getUnit())) {
            contract.setUnit(DEFAULT_UNIT);
        }
        if (!StringUtils.hasText(contract.getCurrency())) {
            contract.setCurrency(DEFAULT_CURRENCY);
        }
        // 1.6 本票无明细，总额固定 0（02 票接管重算）
        salesContractMapper.insert(contract);
        return contract.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSalesContract(SalesContractSaveReqVO updateReqVO) {
        // 1. 校验合同存在
        validateSalesContractExists(updateReqVO.getId());
        // 2. 校验客户存在（customerId 可编辑换客户）
        CustomerDO customer = validateCustomerExists(updateReqVO.getCustomerId());
        // 3. 组装并更新（clientName 为空时用客户公司名称回填，与创建一致）
        SalesContractDO updateObj = BeanUtils.toBean(updateReqVO, SalesContractDO.class);
        if (StringUtils.hasText(updateObj.getContractNo())) {
            // 编号变更时查重（排除自身，含已删除行）
            validateContractNoUnique(updateObj.getId(), updateObj.getContractNo());
        } else {
            // 编号留空视为"不修改"：置 null 使 MyBatis-Plus 跳过该列，
            // 避免把 contract_no 写成空串绕过编号全量唯一语义（ADR-0004）
            updateObj.setContractNo(null);
        }
        if (!StringUtils.hasText(updateObj.getClientName())) {
            updateObj.setClientName(customer.getCompany());
        }
        salesContractMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSalesContract(Long id) {
        // 校验存在
        validateSalesContractExists(id);
        // 逻辑删除主表（本票无明细，不做状态联动、不区分当前状态）
        salesContractMapper.deleteById(id);
    }

    @Override
    public SalesContractRespVO getSalesContract(Long id) {
        SalesContractDO contract = salesContractMapper.selectById(id);
        if (contract == null) {
            throw exception(SALES_CONTRACT_NOT_EXISTS);
        }
        return BeanUtils.toBean(contract, SalesContractRespVO.class);
    }

    @Override
    public PageResult<SalesContractRespVO> getSalesContractPage(SalesContractPageReqVO reqVO) {
        // 构造查询条件
        LambdaQueryWrapperX<SalesContractDO> wrapper = buildPageWrapper(reqVO);
        wrapper.orderByDesc(SalesContractDO::getSignDate);
        // 执行查询
        PageResult<SalesContractDO> pageResult = salesContractMapper.selectPage(reqVO, wrapper);
        return BeanUtils.toBean(pageResult, SalesContractRespVO.class);
    }

    // ==================== 私有方法 ====================

    /**
     * 构造分页查询条件：keyword 对 contract_no/client_name/client_contract_no 做 OR 模糊
     * （PostgreSQL 用 LOWER(...) LIKE LOWER({0}) 实现 ILIKE 语义，占位符走预编译），
     * status/tradeType/customerId 等值过滤。
     */
    private LambdaQueryWrapperX<SalesContractDO> buildPageWrapper(SalesContractPageReqVO reqVO) {
        LambdaQueryWrapperX<SalesContractDO> wrapper = new LambdaQueryWrapperX<>();
        // 等值过滤（为空则不过滤，便于"全部"场景）
        wrapper.eqIfPresent(SalesContractDO::getStatus, reqVO.getStatus());
        wrapper.eqIfPresent(SalesContractDO::getTradeType, reqVO.getTradeType());
        // customerId 预留透传（本票无 UI 入口）
        wrapper.eqIfPresent(SalesContractDO::getCustomerId, reqVO.getCustomerId());
        // keyword：命中 contract_no / client_name / client_contract_no 之一
        String keyword = reqVO.getKeyword() == null ? "" : reqVO.getKeyword().trim();
        if (!keyword.isEmpty()) {
            String like = "%" + keyword + "%";
            wrapper.and(w -> w
                    .apply("LOWER(contract_no) LIKE LOWER({0})", like)
                    .or().apply("LOWER(client_name) LIKE LOWER({0})", like)
                    .or().apply("LOWER(client_contract_no) LIKE LOWER({0})", like));
        }
        return wrapper;
    }

    /**
     * 自动取号（撞号有限次顺延重试）。
     *
     * @return 生成的合同编号
     */
    private String generateContractNoWithRetry() {
        for (int i = 0; i < MAX_GENERATE_RETRY; i++) {
            String candidate = generateContractNo();
            // 查重含已删除行（ADR-0004）；命中说明并发取号或历史数据占用，顺延重试
            if (salesContractMapper.selectByContractNoIncludingDeleted(candidate) == null) {
                return candidate;
            }
        }
        // 连续撞号放弃取号，提示用户手填，避免无限循环
        throw exception(SALES_CONTRACT_NO_DUPLICATE);
    }

    /**
     * 生成合同编号：CH + 当前年份 + 4 位顺序号（超 9999 自然扩位）。
     * 扫描当前租户同前缀的全部合同（含已删除行，ADR-0004），按前缀后的数字部分取 max + 1，
     * 字符串排序会取错最大值（如 CH9 > CH10）。
     */
    private String generateContractNo() {
        String prefix = CONTRACT_NO_PREFIX + LocalDate.now().getYear();
        List<SalesContractDO> candidates = salesContractMapper.selectListByContractNoPrefixIncludingDeleted(prefix);
        long maxNum = 0L;
        int maxDigits = NO_MIN_DIGITS;
        for (SalesContractDO candidate : candidates) {
            String no = candidate.getContractNo();
            if (no == null || no.length() <= prefix.length()) {
                continue;
            }
            String suffix = no.substring(prefix.length());
            Matcher matcher = NO_SUFFIX_NUMBER_PATTERN.matcher(suffix);
            if (!matcher.find()) {
                continue;
            }
            String digits = matcher.group();
            long num;
            try {
                num = Long.parseLong(digits);
            } catch (NumberFormatException ignore) {
                // 手填的超长/异常数字后缀不参与取号比较，避免取号被单条脏数据打断
                continue;
            }
            if (num > maxNum) {
                maxNum = num;
                maxDigits = Math.max(NO_MIN_DIGITS, digits.length());
            }
        }
        if (maxNum == 0L) {
            return prefix + "0001";
        }
        return prefix + String.format("%0" + maxDigits + "d", maxNum + 1);
    }

    /**
     * 校验合同编号未被占用（排除 excludeId 自身），扫描范围含已删除行（ADR-0004）：
     * 手填撞号（无论目标是否已删除）一律报 SALES_CONTRACT_NO_DUPLICATE，不顺延。
     */
    private void validateContractNoUnique(Long excludeId, String contractNo) {
        if (!StringUtils.hasText(contractNo)) {
            return;
        }
        SalesContractDO exist = salesContractMapper.selectByContractNoIncludingDeleted(contractNo);
        if (exist != null && !exist.getId().equals(excludeId)) {
            throw exception(SALES_CONTRACT_NO_DUPLICATE);
        }
    }

    /**
     * 校验合同存在，不存在抛 1_030_003_000
     */
    private void validateSalesContractExists(Long id) {
        if (id == null || salesContractMapper.selectById(id) == null) {
            throw exception(SALES_CONTRACT_NOT_EXISTS);
        }
    }

    /**
     * 校验客户存在（引用客户档案库），customerId 为空或客户不存在抛 1_030_004_000
     *
     * @return 客户（供 clientName 快照回填）
     */
    private CustomerDO validateCustomerExists(Long customerId) {
        CustomerDO customer = customerId == null ? null : customerMapper.selectById(customerId);
        if (customer == null) {
            throw exception(CUSTOMER_NOT_EXISTS);
        }
        return customer;
    }

}
