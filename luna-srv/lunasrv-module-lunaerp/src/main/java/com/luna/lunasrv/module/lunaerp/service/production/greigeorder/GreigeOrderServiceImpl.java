package com.luna.lunasrv.module.lunaerp.service.production.greigeorder;

import cn.hutool.core.collection.CollUtil;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.production.greigeorder.vo.GreigeOrderColorRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.greigeorder.vo.GreigeOrderColorSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.greigeorder.vo.GreigeOrderPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.greigeorder.vo.GreigeOrderRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.greigeorder.vo.GreigeOrderSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.greigetype.GreigeTypeDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.supplier.SupplierDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salescontract.SalesContractDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.greigeorder.GreigeOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.greigeorder.color.GreigeOrderColorDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.greigetype.GreigeTypeMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.supplier.SupplierMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salescontract.SalesContractMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.greigeorder.GreigeOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.greigeorder.color.GreigeOrderColorMapper;
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
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.GREIGE_ORDER_NOT_EXISTS;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.GREIGE_ORDER_NO_DUPLICATE;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.GREIGE_ORDER_REFERENCE_NOT_FOUND;

/**
 * 生产管理-胚布单 Service 实现类
 *
 * 编号规则见 ADR-0006：PB + 月日 + 3 位日流水、按天取号、全量唯一（含已删除行）、软删不释放；
 * 取号扫描与查重范围包含已删除行。坯布厂/染厂引用供应商档案不做类型强过滤（ADR-0007）。
 * 颜色分配（04）随单聚合保存：事务内按 order_id 删旧行、插新行并重排 line_no，
 * 投胚需求 = 成品量 ÷ (1 − 损耗率) 服务端权威重算；删除（05）由后续票据追加。
 *
 * @author luna
 */
@Service
public class GreigeOrderServiceImpl implements GreigeOrderService {

    /**
     * 胚布单号前缀（PB + 两位月 + 两位日）
     */
    private static final String ORDER_NO_PREFIX = "PB";
    /**
     * 编号数字部分最小补零位数（超 999 自然扩位）
     */
    private static final int NO_MIN_DIGITS = 3;
    /**
     * 自动取号撞唯一索引时的最大顺延重试次数
     */
    private static final int MAX_GENERATE_RETRY = 3;
    /**
     * 编号前缀（PB+月日）后的数字部分
     */
    private static final Pattern NO_SUFFIX_NUMBER_PATTERN = Pattern.compile("^\\d+");

    /**
     * 单据默认状态
     */
    private static final String DEFAULT_STATUS = "ACTIVE";
    /**
     * 匹重缺省值 KG
     */
    private static final BigDecimal DEFAULT_PIECE_WEIGHT = new BigDecimal("25");
    /**
     * 预设损耗缺省值（小数）
     */
    private static final BigDecimal DEFAULT_SUGGESTED_LOSS = BigDecimal.ZERO;
    /**
     * 数量字段统一精度（对齐 numeric(18,2)）
     */
    private static final int QTY_SCALE = 2;
    /**
     * 损耗率字段统一精度（对齐 numeric(6,4)）
     */
    private static final int LOSS_SCALE = 4;

    @Resource
    private GreigeOrderMapper greigeOrderMapper;

    @Resource
    private GreigeOrderColorMapper greigeOrderColorMapper;

    @Resource
    private SupplierMapper supplierMapper;

    @Resource
    private SalesContractMapper salesContractMapper;

    @Resource
    private GreigeTypeMapper greigeTypeMapper;

    @Override
    public PageResult<GreigeOrderRespVO> getGreigeOrderPage(GreigeOrderPageReqVO reqVO) {
        // 构造查询条件
        LambdaQueryWrapperX<GreigeOrderDO> wrapper = buildPageWrapper(reqVO);
        wrapper.orderByDesc(GreigeOrderDO::getOrderDate);
        // 执行查询
        PageResult<GreigeOrderDO> pageResult = greigeOrderMapper.selectPage(reqVO, wrapper);
        return BeanUtils.toBean(pageResult, GreigeOrderRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createGreigeOrder(GreigeOrderSaveReqVO createReqVO) {
        // 1. 引用校验：坯布厂必填；染厂/销售合同/胚布品种非空时逐一校验存在（ADR-0007 不校验供应商类型）
        ValidatedRefs refs = validateReferences(createReqVO);
        // 2. 组装 DO
        GreigeOrderDO order = BeanUtils.toBean(createReqVO, GreigeOrderDO.class);
        // 3. 编号：手填查重（含已删除行，撞号直接报错不顺延）；留空自动生成（撞号有限次顺延重试）
        if (StringUtils.hasText(order.getOrderNo())) {
            validateOrderNoUnique(order.getOrderNo());
        } else {
            order.setOrderNo(generateOrderNoWithRetry());
        }
        // 4. 快照回填 + 缺省值（先行执行，颜色行损耗缺省取主单预设损耗）
        fillSnapshotsAndDefaults(order, refs.greigeFactory(), refs.dyeingFactory(), refs.contract());
        // 5. 颜色投胚分配：按提交顺序重排 line_no、权威重算投胚需求（零颜色合法）
        List<GreigeOrderColorDO> colorDOList = buildColorDOList(null, createReqVO.getColors(),
                order.getSuggestedLoss());
        // 6. 保存主表 → 插入颜色行（回填 orderId）
        greigeOrderMapper.insert(order);
        saveColors(order.getId(), colorDOList);
        return order.getId();
    }

    @Override
    public GreigeOrderRespVO getGreigeOrder(Long id) {
        GreigeOrderDO order = greigeOrderMapper.selectById(id);
        if (order == null) {
            throw exception(GREIGE_ORDER_NOT_EXISTS);
        }
        GreigeOrderRespVO respVO = BeanUtils.toBean(order, GreigeOrderRespVO.class);
        // 嵌套返回颜色行（按 line_no 升序）
        List<GreigeOrderColorDO> colorDOList = greigeOrderColorMapper.selectListByOrderId(id);
        respVO.setColors(BeanUtils.toBean(colorDOList, GreigeOrderColorRespVO.class));
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateGreigeOrder(GreigeOrderSaveReqVO updateReqVO) {
        // 1. 存在性校验：目标单据不存在报 GREIGE_ORDER_NOT_EXISTS
        validateGreigeOrderExists(updateReqVO.getId());
        // 2. 引用校验（与创建一致：坯布厂必填；染厂/销售合同/胚布品种非空时逐一校验存在，ADR-0007 不校验供应商类型）
        ValidatedRefs refs = validateReferences(updateReqVO);
        // 3. 组装 DO
        GreigeOrderDO order = BeanUtils.toBean(updateReqVO, GreigeOrderDO.class);
        // 4. 编号：手填查重（排除自身，扫描含已删除行 ADR-0006）；留空自动生成（有限次顺延重试）
        if (StringUtils.hasText(order.getOrderNo())) {
            GreigeOrderDO duplicate = greigeOrderMapper.selectByOrderNoIncludingDeleted(order.getOrderNo());
            if (duplicate != null && !duplicate.getId().equals(order.getId())) {
                throw exception(GREIGE_ORDER_NO_DUPLICATE, order.getOrderNo());
            }
        } else {
            order.setOrderNo(generateOrderNoWithRetry());
        }
        // 5. 快照回填 + 缺省值（与创建一致；先行执行，颜色行损耗缺省取主单预设损耗）
        fillSnapshotsAndDefaults(order, refs.greigeFactory(), refs.dyeingFactory(), refs.contract());
        // 6. 整体保存：可空字段清空随 DO 的 updateStrategy=ALWAYS 一并落库
        greigeOrderMapper.updateById(order);
        // 7. 颜色投胚分配整体替换：事务内先按 order_id 删旧行，再插本次提交的全部颜色行
        //    （零颜色也执行删除，保证"清空全部颜色行"语义；line_no 重排、投胚需求权威重算）
        List<GreigeOrderColorDO> colorDOList = buildColorDOList(updateReqVO.getId(),
                updateReqVO.getColors(), order.getSuggestedLoss());
        greigeOrderColorMapper.deleteByOrderId(updateReqVO.getId());
        saveColors(updateReqVO.getId(), colorDOList);
    }

    @Override
    public String getNextGreigeOrderNo() {
        // 只读预览、不占号：直接复用自动取号的扫描结果（含已删除行，ADR-0006），
        // 不做撞号查重、不写库；无新建时多次调用返回相同值，保存后再次调用自然顺延
        return generateOrderNo();
    }

    // ==================== 私有方法 ====================

    /**
     * 构造分页查询条件：keyword 对 order_no/contract_no/product_name/product_code 做 OR 模糊
     * （PostgreSQL 用 LOWER(...) LIKE LOWER({0}) 实现 ILIKE 语义，占位符走预编译），
     * status 等值过滤。
     */
    private LambdaQueryWrapperX<GreigeOrderDO> buildPageWrapper(GreigeOrderPageReqVO reqVO) {
        LambdaQueryWrapperX<GreigeOrderDO> wrapper = new LambdaQueryWrapperX<>();
        // 等值过滤（为空则不过滤，便于"全部"场景）
        wrapper.eqIfPresent(GreigeOrderDO::getStatus, reqVO.getStatus());
        // keyword：命中 order_no / contract_no / product_name / product_code 之一
        String keyword = reqVO.getKeyword() == null ? "" : reqVO.getKeyword().trim();
        if (!keyword.isEmpty()) {
            String like = "%" + keyword + "%";
            wrapper.and(w -> w
                    .apply("LOWER(order_no) LIKE LOWER({0})", like)
                    .or().apply("LOWER(contract_no) LIKE LOWER({0})", like)
                    .or().apply("LOWER(product_name) LIKE LOWER({0})", like)
                    .or().apply("LOWER(product_code) LIKE LOWER({0})", like));
        }
        return wrapper;
    }

    /**
     * 校验胚布单存在，不存在抛 1_030_006_000
     */
    private void validateGreigeOrderExists(Long id) {
        if (id == null || greigeOrderMapper.selectById(id) == null) {
            throw exception(GREIGE_ORDER_NOT_EXISTS);
        }
    }

    /**
     * 引用校验结果：坯布厂/染厂供应商档案与销售合同档案，供快照回填
     */
    private record ValidatedRefs(SupplierDO greigeFactory, SupplierDO dyeingFactory, SalesContractDO contract) {
    }

    /**
     * 引用校验（创建/更新共用）：坯布厂必填；染厂/销售合同/胚布品种非空时逐一校验存在
     * （ADR-0007 不校验供应商类型），任一不过抛 GREIGE_ORDER_REFERENCE_NOT_FOUND。
     */
    private ValidatedRefs validateReferences(GreigeOrderSaveReqVO reqVO) {
        SupplierDO greigeFactory = validateSupplierExists(reqVO.getGreigeFactoryId(), "坯布厂");
        SupplierDO dyeingFactory = reqVO.getDyeingFactoryId() == null
                ? null : validateSupplierExists(reqVO.getDyeingFactoryId(), "指定染厂");
        SalesContractDO contract = null;
        if (reqVO.getSalesContractId() != null) {
            contract = salesContractMapper.selectById(reqVO.getSalesContractId());
            if (contract == null) {
                throw exception(GREIGE_ORDER_REFERENCE_NOT_FOUND, "销售合同");
            }
        }
        validateGreigeTypeExists(reqVO.getGreigeTypeId());
        return new ValidatedRefs(greigeFactory, dyeingFactory, contract);
    }

    /**
     * 快照回填 + 缺省值（创建/更新共用）：
     * 名称/合同号快照为空时用档案当前名称回填（选中供应商后档案改名的场景以前端传值为准）；
     * 单据日期缺省今天、状态缺省 ACTIVE、匹重缺省 25、预设损耗缺省 0。
     */
    private void fillSnapshotsAndDefaults(GreigeOrderDO order, SupplierDO greigeFactory,
                                          SupplierDO dyeingFactory, SalesContractDO contract) {
        if (!StringUtils.hasText(order.getGreigeFactoryName()) && greigeFactory != null) {
            order.setGreigeFactoryName(greigeFactory.getCompanyName());
        }
        if (order.getDyeingFactoryId() != null && !StringUtils.hasText(order.getDyeingFactoryName())
                && dyeingFactory != null) {
            order.setDyeingFactoryName(dyeingFactory.getCompanyName());
        }
        if (order.getSalesContractId() != null && contract != null && !StringUtils.hasText(order.getContractNo())) {
            order.setContractNo(contract.getContractNo());
        }
        if (order.getOrderDate() == null) {
            order.setOrderDate(LocalDate.now());
        }
        if (!StringUtils.hasText(order.getStatus())) {
            order.setStatus(DEFAULT_STATUS);
        }
        if (order.getPieceWeight() == null) {
            order.setPieceWeight(DEFAULT_PIECE_WEIGHT);
        }
        if (order.getSuggestedLoss() == null) {
            order.setSuggestedLoss(DEFAULT_SUGGESTED_LOSS);
        }
    }

    // ==================== 颜色投胚分配聚合私有方法（ADR-0003） ====================

    /**
     * 将请求颜色行列表转换为 DO 列表：按数组顺序重排 line_no（从 1 开始）、
     * 行损耗缺省取主单预设损耗（再缺省 0）、成品量 null 按 0、
     * 投胚需求服务端权威重算 = 成品量 ÷ (1 − 损耗率)（前端传值一律忽略覆盖）。
     *
     * @param orderId            胚布单ID（创建时可为 null，由 {@link #saveColors} 回填）
     * @param colors             请求颜色行列表（为 null/空表示零颜色单据，合法）
     * @param orderSuggestedLoss 主单预设损耗（已经 fillSnapshotsAndDefaults 缺省为 0）
     * @return 颜色行 DO 列表
     */
    private List<GreigeOrderColorDO> buildColorDOList(Long orderId, List<GreigeOrderColorSaveReqVO> colors,
                                                      BigDecimal orderSuggestedLoss) {
        List<GreigeOrderColorDO> colorDOList = new ArrayList<>();
        if (CollUtil.isEmpty(colors)) {
            return colorDOList;
        }
        for (int i = 0; i < colors.size(); i++) {
            GreigeOrderColorSaveReqVO color = colors.get(i);
            GreigeOrderColorDO colorDO = BeanUtils.toBean(color, GreigeOrderColorDO.class);
            colorDO.setOrderId(orderId);
            colorDO.setLineNo(i + 1);
            // 行损耗缺省链：行损耗 → 主单预设损耗 → 0；统一 scale=4 对齐 numeric(6,4)
            BigDecimal lossRate = color.getLossRate() != null
                    ? color.getLossRate() : zeroIfNull(orderSuggestedLoss);
            BigDecimal finishedQty = zeroIfNull(color.getFinishedQty());
            BigDecimal roundedLoss = lossRate.setScale(LOSS_SCALE, RoundingMode.HALF_UP);
            BigDecimal roundedQty = finishedQty.setScale(QTY_SCALE, RoundingMode.HALF_UP);
            colorDO.setLossRate(roundedLoss);
            colorDO.setFinishedQty(roundedQty);
            // 投胚需求用取整后的成品量/损耗率计算，保证与落库存储值口径自洽
            colorDO.setDemandQty(calculateDemandQty(roundedQty, roundedLoss));
            colorDOList.add(colorDO);
        }
        return colorDOList;
    }

    /**
     * 投胚需求权威重算口径：成品量 ÷ (1 − 损耗率)，统一 scale=2 对齐 numeric(18,2)。
     * 损耗率 ≥ 100%（除数 ≤ 0）属于无放大意义的异常口径，按不放大处理（需求 = 成品量），避免除零。
     *
     * @param finishedQty 成品下单量 KG（null 已按 0 处理）
     * @param lossRate    损耗率（小数）
     * @return 投胚需求 KG
     */
    private BigDecimal calculateDemandQty(BigDecimal finishedQty, BigDecimal lossRate) {
        BigDecimal divisor = BigDecimal.ONE.subtract(lossRate);
        if (divisor.compareTo(BigDecimal.ZERO) <= 0) {
            return finishedQty.setScale(QTY_SCALE, RoundingMode.HALF_UP);
        }
        return finishedQty.divide(divisor, QTY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 批量保存颜色行（回填 orderId）；零颜色时跳过插入。
     */
    private void saveColors(Long orderId, List<GreigeOrderColorDO> colors) {
        if (CollUtil.isEmpty(colors)) {
            return;
        }
        for (GreigeOrderColorDO color : colors) {
            color.setOrderId(orderId);
        }
        greigeOrderColorMapper.insertBatch(colors);
    }

    // ==================== 编号（ADR-0006） ====================

    /**
     * 自动取号（撞号有限次顺延重试）。
     *
     * @return 生成的胚布单号
     */
    private String generateOrderNoWithRetry() {
        for (int i = 0; i < MAX_GENERATE_RETRY; i++) {
            String candidate = generateOrderNo();
            // 查重含已删除行（ADR-0006）；命中说明并发取号或历史数据占用，顺延重试
            if (greigeOrderMapper.selectByOrderNoIncludingDeleted(candidate) == null) {
                return candidate;
            }
        }
        // 连续撞号放弃取号，提示用户手填，避免无限循环
        throw exception(GREIGE_ORDER_NO_DUPLICATE, generateOrderNo());
    }

    /**
     * 生成胚布单号：PB + 两位月 + 两位日 + 顺序号 3 位起（超 999 自然扩位）。
     * 以服务器当天日期构造前缀，按天取号；扫描当前租户同前缀的全部单据（含已删除行，ADR-0006），
     * 按前缀后的数字部分取 max + 1，字符串排序会取错最大值（如 PB091910 > PB09199）。
     */
    private String generateOrderNo() {
        LocalDate today = LocalDate.now();
        String prefix = ORDER_NO_PREFIX + String.format("%02d%02d", today.getMonthValue(), today.getDayOfMonth());
        List<GreigeOrderDO> candidates = greigeOrderMapper.selectListByOrderNoPrefixIncludingDeleted(prefix);
        long maxNum = 0L;
        int maxDigits = NO_MIN_DIGITS;
        for (GreigeOrderDO candidate : candidates) {
            String no = candidate.getOrderNo();
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
            return prefix + String.format("%0" + NO_MIN_DIGITS + "d", 1);
        }
        return prefix + String.format("%0" + maxDigits + "d", maxNum + 1);
    }

    /**
     * 校验单号未被占用，扫描范围含已删除行（ADR-0006）：
     * 手填撞号（无论目标是否已删除）一律报 GREIGE_ORDER_NO_DUPLICATE，不顺延。
     */
    private void validateOrderNoUnique(String orderNo) {
        if (greigeOrderMapper.selectByOrderNoIncludingDeleted(orderNo) != null) {
            throw exception(GREIGE_ORDER_NO_DUPLICATE, orderNo);
        }
    }

    // ==================== 引用校验（ADR-0007） ====================

    /**
     * 校验供应商存在（存在性 + 租户归属由租户拦截器保证；不限状态、不校验供应商类型，见 ADR-0007）
     *
     * @param supplierId 供应商ID
     * @param roleName   引用角色名（坯布厂/指定染厂），用于错误信息
     * @return 供应商（供公司名快照回填）
     */
    private SupplierDO validateSupplierExists(Long supplierId, String roleName) {
        SupplierDO supplier = supplierId == null ? null : supplierMapper.selectById(supplierId);
        if (supplier == null) {
            throw exception(GREIGE_ORDER_REFERENCE_NOT_FOUND, roleName);
        }
        return supplier;
    }

    /**
     * 校验胚布品种存在（可空引用；档案 id 为数值，字符串形态无法解析视同不存在）
     *
     * @param greigeTypeId 胚布品种档案 id（字符串形态）
     */
    private void validateGreigeTypeExists(String greigeTypeId) {
        if (!StringUtils.hasText(greigeTypeId)) {
            return;
        }
        Long id;
        try {
            id = Long.parseLong(greigeTypeId.trim());
        } catch (NumberFormatException e) {
            throw exception(GREIGE_ORDER_REFERENCE_NOT_FOUND, "胚布品种");
        }
        GreigeTypeDO greigeType = greigeTypeMapper.selectById(id);
        if (greigeType == null) {
            throw exception(GREIGE_ORDER_REFERENCE_NOT_FOUND, "胚布品种");
        }
    }

    /**
     * null 安全转 BigDecimal：null → 0
     */
    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

}
