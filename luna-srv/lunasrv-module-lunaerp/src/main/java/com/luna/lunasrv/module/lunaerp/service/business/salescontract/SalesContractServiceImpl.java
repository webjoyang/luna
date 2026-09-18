package com.luna.lunasrv.module.lunaerp.service.business.salescontract;

import cn.hutool.core.collection.CollUtil;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.business.salescontract.vo.SalesContractItemRespVO;
import com.luna.lunasrv.module.lunaerp.controller.business.salescontract.vo.SalesContractItemSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.business.salescontract.vo.SalesContractPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.business.salescontract.vo.SalesContractRespVO;
import com.luna.lunasrv.module.lunaerp.controller.business.salescontract.vo.SalesContractSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salescontract.SalesContractDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salescontract.SalesContractItemDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salescontract.SalesContractItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salescontract.SalesContractMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
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
    private SalesContractItemMapper salesContractItemMapper;

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
        // 1.6 明细聚合：服务端权威重算行金额与主表汇总（请求体传入的 totalQuantity/totalAmount 一律忽略覆盖）
        List<SalesContractItemDO> itemDOList = buildItemDOList(null, createReqVO.getItems());
        BigDecimal[] totals = recalculateTotals(itemDOList);
        contract.setTotalQuantity(totals[0]);
        contract.setTotalAmount(totals[1]);
        // 1.7 保存主表 → 插入明细（回填 contractId）
        salesContractMapper.insert(contract);
        saveItems(contract.getId(), itemDOList);
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
        // 4. 明细整体替换：先按 contractId 逻辑删除旧明细，再插本次提交的全部明细；
        //    服务端权威重算行金额与主表汇总（请求体传入的 totalQuantity/totalAmount 一律忽略覆盖）
        List<SalesContractItemDO> itemDOList = buildItemDOList(updateReqVO.getId(), updateReqVO.getItems());
        BigDecimal[] totals = recalculateTotals(itemDOList);
        updateObj.setTotalQuantity(totals[0]);
        updateObj.setTotalAmount(totals[1]);
        salesContractMapper.updateById(updateObj);
        salesContractItemMapper.deleteByContractId(updateReqVO.getId());
        saveItems(updateReqVO.getId(), itemDOList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSalesContract(Long id) {
        // 校验存在
        validateSalesContractExists(id);
        // 逻辑删除主表 + 级联逻辑删除全部明细（明细是合同聚合的一部分，ADR-0003）
        salesContractMapper.deleteById(id);
        salesContractItemMapper.deleteByContractId(id);
    }

    @Override
    public SalesContractRespVO getSalesContract(Long id) {
        SalesContractDO contract = salesContractMapper.selectById(id);
        if (contract == null) {
            throw exception(SALES_CONTRACT_NOT_EXISTS);
        }
        SalesContractRespVO respVO = BeanUtils.toBean(contract, SalesContractRespVO.class);
        // 嵌套返回明细（按 line_no 升序）
        List<SalesContractItemDO> itemDOList = salesContractItemMapper.selectListByContractId(id);
        respVO.setItems(BeanUtils.toBean(itemDOList, SalesContractItemRespVO.class));
        return respVO;
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

    @Override
    public String getNextContractNo() {
        // 只读预览、不占号：直接复用自动取号的扫描结果（含已删除行，ADR-0004），
        // 不做撞号查重、不写库；无新建时多次调用返回相同值，保存后再次调用自然顺延
        return generateContractNo();
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

    // ==================== 明细聚合私有方法（ADR-0003） ====================

    /**
     * 将请求明细列表转换为 DO 列表，并按数组顺序重排 line_no（从 1 开始）、
     * 服务端权威重算每行 amount = quantity × unit_price + other_fees（null 数值按 0）。
     *
     * @param contractId 合同ID（创建时可为 null，由 {@link #saveItems} 回填）
     * @param items      请求明细列表（为 null/空表示零明细合同）
     * @return 明细 DO 列表
     */
    private List<SalesContractItemDO> buildItemDOList(Long contractId, List<SalesContractItemSaveReqVO> items) {
        List<SalesContractItemDO> itemDOList = new ArrayList<>();
        if (CollUtil.isEmpty(items)) {
            return itemDOList;
        }
        for (int i = 0; i < items.size(); i++) {
            SalesContractItemSaveReqVO item = items.get(i);
            SalesContractItemDO itemDO = BeanUtils.toBean(item, SalesContractItemDO.class);
            itemDO.setContractId(contractId);
            itemDO.setLineNo(i + 1);
            // 行金额权威重算：数量 × 单价 + 其他费用（null 按 0），统一 scale=2 对齐 numeric(18,2)
            BigDecimal quantity = zeroIfNull(item.getQuantity());
            BigDecimal unitPrice = zeroIfNull(item.getUnitPrice());
            BigDecimal otherFees = zeroIfNull(item.getOtherFees());
            itemDO.setQuantity(quantity.setScale(2, RoundingMode.HALF_UP));
            itemDO.setUnitPrice(unitPrice.setScale(2, RoundingMode.HALF_UP));
            itemDO.setOtherFees(otherFees.setScale(2, RoundingMode.HALF_UP));
            itemDO.setAmount(quantity.multiply(unitPrice).add(otherFees).setScale(2, RoundingMode.HALF_UP));
            itemDOList.add(itemDO);
        }
        return itemDOList;
    }

    /**
     * 按明细列表重算主表汇总：totalQuantity = Σ quantity，totalAmount = Σ amount。
     * 零明细时两者均为 0。
     *
     * @param items 明细 DO 列表（行金额已由 {@link #buildItemDOList} 重算）
     * @return [totalQuantity, totalAmount]
     */
    private BigDecimal[] recalculateTotals(List<SalesContractItemDO> items) {
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        if (items != null) {
            for (SalesContractItemDO item : items) {
                totalQuantity = totalQuantity.add(zeroIfNull(item.getQuantity()));
                totalAmount = totalAmount.add(zeroIfNull(item.getAmount()));
            }
        }
        return new BigDecimal[]{
                totalQuantity.setScale(2, RoundingMode.HALF_UP),
                totalAmount.setScale(2, RoundingMode.HALF_UP)
        };
    }

    /**
     * 批量保存明细（回填 contractId）；零明细时跳过插入。
     */
    private void saveItems(Long contractId, List<SalesContractItemDO> items) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        for (SalesContractItemDO item : items) {
            item.setContractId(contractId);
        }
        salesContractItemMapper.insertBatch(items);
    }

    /**
     * null 安全转 BigDecimal：null → 0
     */
    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
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
