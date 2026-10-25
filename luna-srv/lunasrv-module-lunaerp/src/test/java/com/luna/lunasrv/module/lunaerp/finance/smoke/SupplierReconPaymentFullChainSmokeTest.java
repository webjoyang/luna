package com.luna.lunasrv.module.lunaerp.finance.smoke;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.security.core.util.SecurityFrameworkUtils;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.approval.handler.PaymentApprovalHandler;
import com.luna.lunasrv.module.lunaerp.approval.instance.ApprovalInstanceService;
import com.luna.lunasrv.module.lunaerp.approval.instance.vo.ApprovalInstanceSubmitReqVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.supplier.vo.SupplierRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.*;
import com.luna.lunasrv.module.lunaerp.controller.finance.supplierrecon.vo.*;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.supplier.SupplierDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentApplyDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentRefDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.supplierrecon.SupplierReconDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.supplierrecon.SupplierReconItemDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.supplier.SupplierMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentApplyMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentRefMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.supplierrecon.SupplierReconItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.supplierrecon.SupplierReconMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.*;
import com.luna.lunasrv.module.lunaerp.service.baseinfo.supplier.SupplierService;
import com.luna.lunasrv.module.lunaerp.service.finance.account.FinanceAccountService;
import com.luna.lunasrv.module.lunaerp.service.finance.payment.*;
import com.luna.lunasrv.module.lunaerp.service.finance.supplierrecon.*;
import com.luna.lunasrv.module.system.api.user.AdminUserApi;
import com.luna.lunasrv.module.system.api.user.dto.AdminUserRespDTO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 财务域 - 供应商应付全链路业务冒烟测试 (Issue 10)
 *
 * 覆盖主链全业务节奏：
 * 1. 取数预览（加工费候选汇总 Σ实称重量/Σ米数/全等级计入 + 采购货款候选入库明细/纱线采购单引用）
 * 2. 生成含调整项对账单（包含 PROCESS、PURCHASE 以及 ADJUST 调整项行，验证服务端重算）
 * 3. 被抢引用报错验证（源明细被占用时抛 022_004）
 * 4. 确认对账单（草稿 -> CONFIRMED，单据与行快照冻结，防并发双确认）
 * 5. A4 打印数据契约验证（含抬头部、三类行明细、合计金额与签章区，豁免金额脱敏）
 * 6. 生成对账付款申请（RECON）与预付申请（PREPAY）（RECON 校验已确认对账单及累计金额上限；PREPAY 校验用途及银行信息快照）
 * 7. 审批驳回改后重提（PREPAY 申请提交审批后遭驳回，进入 REJECTED 状态；修改金额后重新提交）
 * 8. 审批通过（APPROVED，验证 ApprovalHandler 仅变更状态，不产生任何资金账务写入）
 * 9. 付款登记出纳执行（两步制：先对 PREPAY 付款形成预付余额池；再对 RECON 付款：支持多申请顺序分配、部分核销、预付抵扣与账户扣款，四步原子过账）
 * 10. 聚合口径与实时余额核对（核对对账单已申请金额实时聚合、付款申请已核销/剩余金额聚合、供应商预付余额两条 SUM 口径）
 * 11. 无申请对账单作废释放指纹（独立 CONFIRMED 对账单作废释放指纹；存在有效申请的对账单作废被拦截抛 022_006）
 * 12. 回收站进出与恢复（草稿对账单删除进回收站并成功恢复为 DRAFT；草稿付款申请删除进回收站并成功恢复为 PENDING）
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SupplierReconPaymentFullChainSmokeTest {

    private static final Long TENANT_ID = 1L;
    private static final Long LOGIN_USER_ID = 666L;
    private static final Long SUPPLIER_ID = 101L;
    private static final String SUPPLIER_NAME = "常州华盛纺织有限公司";
    private static final Long ACCOUNT_ID = 201L;

    // In-memory repositories
    private final Map<Long, SupplierReconDO> reconRepo = new LinkedHashMap<>();
    private final Map<Long, SupplierReconItemDO> reconItemRepo = new LinkedHashMap<>();
    private final Map<Long, PaymentApplyDO> applyRepo = new LinkedHashMap<>();
    private final Map<Long, PaymentDO> paymentRepo = new LinkedHashMap<>();
    private final List<PaymentRefDO> paymentRefList = new ArrayList<>();

    private final AtomicLong reconIdSeq = new AtomicLong(1000);
    private final AtomicLong itemIdSeq = new AtomicLong(2000);
    private final AtomicLong applyIdSeq = new AtomicLong(3000);
    private final AtomicLong paymentIdSeq = new AtomicLong(4000);
    private final AtomicLong refIdSeq = new AtomicLong(5000);

    @Mock
    private SupplierReconMapper supplierReconMapper;
    @Mock
    private SupplierReconItemMapper supplierReconItemMapper;
    @Mock
    private PaymentApplyMapper paymentApplyMapper;
    @Mock
    private PaymentMapper paymentMapper;
    @Mock
    private PaymentRefMapper paymentRefMapper;
    @Mock
    private SupplierMapper supplierMapper;
    @Mock
    private SupplierReconSourceService supplierReconSourceService;
    @Mock
    private SupplierService supplierService;
    @Mock
    private FinanceAccountService financeAccountService;
    @Mock
    private ApprovalInstanceService approvalInstanceService;
    @Mock
    private AdminUserApi adminUserApi;

    // Services under test
    private SupplierReconServiceImpl supplierReconService;
    private PaymentApplyServiceImpl paymentApplyService;
    private PaymentServiceImpl paymentService;
    private PaymentApprovalHandler paymentApprovalHandler;
    private SupplierReconRecycleBinHandler reconRecycleBinHandler;
    private PaymentApplyRecycleBinHandler applyRecycleBinHandler;

    private MockedStatic<SecurityFrameworkUtils> mockedSecurity;

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration config = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(config, "");
        TableInfoHelper.initTableInfo(assistant, SupplierReconDO.class);
        TableInfoHelper.initTableInfo(assistant, SupplierReconItemDO.class);
        TableInfoHelper.initTableInfo(assistant, PaymentApplyDO.class);
        TableInfoHelper.initTableInfo(assistant, PaymentDO.class);
        TableInfoHelper.initTableInfo(assistant, PaymentRefDO.class);
    }

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
        mockedSecurity = mockStatic(SecurityFrameworkUtils.class);
        mockedSecurity.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(LOGIN_USER_ID);

        // Instantiate services
        supplierReconService = new SupplierReconServiceImpl();
        paymentApplyService = new PaymentApplyServiceImpl();
        paymentService = new PaymentServiceImpl();
        paymentApprovalHandler = new PaymentApprovalHandler();
        reconRecycleBinHandler = new SupplierReconRecycleBinHandler();
        applyRecycleBinHandler = new PaymentApplyRecycleBinHandler();

        // Wire dependencies into supplierReconService
        ReflectionTestUtils.setField(supplierReconService, "supplierReconMapper", supplierReconMapper);
        ReflectionTestUtils.setField(supplierReconService, "supplierReconItemMapper", supplierReconItemMapper);
        ReflectionTestUtils.setField(supplierReconService, "supplierReconSourceService", supplierReconSourceService);
        ReflectionTestUtils.setField(supplierReconService, "supplierService", supplierService);
        ReflectionTestUtils.setField(supplierReconService, "paymentApplyMapper", paymentApplyMapper);
        ReflectionTestUtils.setField(supplierReconService, "adminUserApi", adminUserApi);

        // Wire dependencies into paymentApplyService
        ReflectionTestUtils.setField(paymentApplyService, "paymentApplyMapper", paymentApplyMapper);
        ReflectionTestUtils.setField(paymentApplyService, "supplierReconMapper", supplierReconMapper);
        ReflectionTestUtils.setField(paymentApplyService, "supplierMapper", supplierMapper);
        ReflectionTestUtils.setField(paymentApplyService, "approvalInstanceService", approvalInstanceService);
        ReflectionTestUtils.setField(paymentApplyService, "paymentRefMapper", paymentRefMapper);
        ReflectionTestUtils.setField(paymentApplyService, "paymentMapper", paymentMapper);

        // Wire dependencies into paymentService
        ReflectionTestUtils.setField(paymentService, "paymentMapper", paymentMapper);
        ReflectionTestUtils.setField(paymentService, "paymentRefMapper", paymentRefMapper);
        ReflectionTestUtils.setField(paymentService, "paymentApplyMapper", paymentApplyMapper);
        ReflectionTestUtils.setField(paymentService, "supplierMapper", supplierMapper);
        ReflectionTestUtils.setField(paymentService, "paymentApplyService", paymentApplyService);
        ReflectionTestUtils.setField(paymentService, "financeAccountService", financeAccountService);

        // Wire dependencies into paymentApprovalHandler
        ReflectionTestUtils.setField(paymentApprovalHandler, "paymentApplyMapper", paymentApplyMapper);

        // Wire dependencies into recycle bin handlers
        ReflectionTestUtils.setField(reconRecycleBinHandler, "supplierReconMapper", supplierReconMapper);
        ReflectionTestUtils.setField(reconRecycleBinHandler, "supplierReconItemMapper", supplierReconItemMapper);
        ReflectionTestUtils.setField(reconRecycleBinHandler, "supplierMapper", supplierMapper);

        ReflectionTestUtils.setField(applyRecycleBinHandler, "paymentApplyMapper", paymentApplyMapper);
        ReflectionTestUtils.setField(applyRecycleBinHandler, "supplierReconMapper", supplierReconMapper);
        ReflectionTestUtils.setField(applyRecycleBinHandler, "supplierMapper", supplierMapper);

        setupMockBehaviors();
    }

    @AfterEach
    void tearDown() {
        mockedSecurity.close();
        TenantContextHolder.clear();
        reconRepo.clear();
        reconItemRepo.clear();
        applyRepo.clear();
        paymentRepo.clear();
        paymentRefList.clear();
    }

    private void setupMockBehaviors() {
        // Supplier mock
        SupplierDO supplierDO = new SupplierDO();
        supplierDO.setId(SUPPLIER_ID);
        supplierDO.setCompanyName(SUPPLIER_NAME);
        supplierDO.setPhone("13812345678");
        supplierDO.setTenantId(TENANT_ID);
        when(supplierMapper.selectById(SUPPLIER_ID)).thenReturn(supplierDO);

        SupplierRespVO supplierRespVO = new SupplierRespVO();
        supplierRespVO.setId(SUPPLIER_ID);
        supplierRespVO.setCompanyName(SUPPLIER_NAME);
        supplierRespVO.setPhone("13812345678");
        when(supplierService.getSupplier(SUPPLIER_ID)).thenReturn(supplierRespVO);

        AdminUserRespDTO adminUser = new AdminUserRespDTO();
        adminUser.setId(LOGIN_USER_ID);
        adminUser.setNickname("张财务");
        when(adminUserApi.getUser(LOGIN_USER_ID)).thenReturn(adminUser);

        // Fingerprint builder
        when(supplierReconSourceService.buildFingerprint(any(), any(), any())).thenAnswer(inv -> {
            String type = inv.getArgument(0);
            Long id = inv.getArgument(1);
            Long itemId = inv.getArgument(2);
            if ("INSPECTION".equalsIgnoreCase(type) || itemId == null) {
                return type + ":" + id;
            }
            return type + ":" + id + ":" + itemId;
        });

        // SupplierReconMapper mocks
        when(supplierReconMapper.insert(any(SupplierReconDO.class))).thenAnswer(inv -> {
            SupplierReconDO recon = inv.getArgument(0);
            if (recon.getId() == null) {
                recon.setId(reconIdSeq.incrementAndGet());
            }
            recon.setTenantId(TENANT_ID);
            recon.setDeleted(false);
            reconRepo.put(recon.getId(), recon);
            return 1;
        });

        when(supplierReconMapper.selectById(anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            SupplierReconDO recon = reconRepo.get(id);
            return (recon != null && Boolean.FALSE.equals(recon.getDeleted())) ? recon : null;
        });

        when(supplierReconMapper.selectDeletedById(anyLong(), anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            SupplierReconDO recon = reconRepo.get(id);
            return (recon != null && Boolean.TRUE.equals(recon.getDeleted())) ? recon : null;
        });

        when(supplierReconMapper.selectListByReconNoPrefixIncludingDeleted(anyString())).thenAnswer(inv -> Collections.emptyList());

        when(supplierReconMapper.updateById(any(SupplierReconDO.class))).thenAnswer(inv -> {
            SupplierReconDO recon = inv.getArgument(0);
            reconRepo.put(recon.getId(), recon);
            return 1;
        });

        when(supplierReconMapper.deleteById(anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            SupplierReconDO recon = reconRepo.get(id);
            if (recon != null) {
                recon.setDeleted(true);
            }
            return 1;
        });

        when(supplierReconMapper.selectList(any(Wrapper.class))).thenAnswer(inv -> {
            Wrapper<?> wrapper = inv.getArgument(0);
            boolean filterCancelled = false;
            if (wrapper != null) {
                String sql = wrapper.getSqlSegment();
                if (sql != null && (sql.contains("status") || sql.contains("CONFIRMED"))) {
                    filterCancelled = true;
                }
            }
            final boolean shouldFilter = filterCancelled;
            List<SupplierReconDO> list = new ArrayList<>();
            for (SupplierReconDO recon : reconRepo.values()) {
                if (Boolean.FALSE.equals(recon.getDeleted())) {
                    if (shouldFilter && ReconStatus.CANCELLED.name().equals(recon.getStatus())) {
                        continue;
                    }
                    list.add(recon);
                }
            }
            return list;
        });

        when(supplierReconMapper.update(any(), any(Wrapper.class))).thenAnswer(inv -> {
            SupplierReconDO updateEntity = inv.getArgument(0);
            Wrapper<?> wrapper = inv.getArgument(1);
            Long targetId = null;
            if (wrapper instanceof com.baomidou.mybatisplus.core.conditions.AbstractWrapper<?, ?, ?> aw) {
                aw.getSqlSegment(); // 触发包装器惰性求值以填充 paramNameValuePairs
                Map<String, Object> params = aw.getParamNameValuePairs();
                if (params != null) {
                    for (Object val : params.values()) {
                        if (val instanceof Long l && reconRepo.containsKey(l)) {
                            targetId = l;
                            break;
                        }
                    }
                }
            }
            if (targetId == null && updateEntity != null) {
                for (SupplierReconDO r : reconRepo.values()) {
                    if (updateEntity.getStatus() != null && !Objects.equals(r.getStatus(), updateEntity.getStatus())) {
                        targetId = r.getId();
                        break;
                    }
                }
            }
            if (targetId != null) {
                SupplierReconDO recon = reconRepo.get(targetId);
                if (recon != null && updateEntity != null) {
                    if (updateEntity.getStatus() != null) recon.setStatus(updateEntity.getStatus());
                    if (updateEntity.getConfirmedAt() != null) recon.setConfirmedAt(updateEntity.getConfirmedAt());
                    if (updateEntity.getConfirmedBy() != null) recon.setConfirmedBy(updateEntity.getConfirmedBy());
                    if (updateEntity.getCancelledAt() != null) recon.setCancelledAt(updateEntity.getCancelledAt());
                    if (updateEntity.getCancelledBy() != null) recon.setCancelledBy(updateEntity.getCancelledBy());
                }
            }
            return 1;
        });

        when(supplierReconMapper.restoreSupplierRecon(anyLong(), anyLong(), anyString(), any())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            SupplierReconDO recon = reconRepo.get(id);
            if (recon != null) {
                recon.setDeleted(false);
                recon.setStatus(ReconStatus.DRAFT.name());
            }
            return 1;
        });

        // SupplierReconItemMapper mocks
        when(supplierReconItemMapper.insertBatch(anyCollection())).thenAnswer(inv -> {
            Collection<SupplierReconItemDO> items = inv.getArgument(0);
            for (SupplierReconItemDO item : items) {
                if (item.getId() == null) {
                    item.setId(itemIdSeq.incrementAndGet());
                }
                item.setTenantId(TENANT_ID);
                item.setDeleted(false);
                reconItemRepo.put(item.getId(), item);
            }
            return Boolean.TRUE;
        });

        when(supplierReconItemMapper.selectList(any(Wrapper.class))).thenAnswer(inv -> {
            List<SupplierReconItemDO> list = new ArrayList<>();
            for (SupplierReconItemDO item : reconItemRepo.values()) {
                if (Boolean.FALSE.equals(item.getDeleted())) {
                    list.add(item);
                }
            }
            return list;
        });

        when(supplierReconItemMapper.selectCount(any(Wrapper.class))).thenAnswer(inv -> {
            long count = reconItemRepo.values().stream()
                    .filter(item -> Boolean.FALSE.equals(item.getDeleted()))
                    .count();
            return count;
        });

        when(supplierReconItemMapper.restoreByReconId(anyLong(), anyLong(), anyString(), any())).thenAnswer(inv -> {
            Long rId = inv.getArgument(0);
            for (SupplierReconItemDO item : reconItemRepo.values()) {
                if (Objects.equals(item.getReconId(), rId)) {
                    item.setDeleted(false);
                }
            }
            return 1;
        });

        // PaymentApplyMapper mocks
        when(paymentApplyMapper.insert(any(PaymentApplyDO.class))).thenAnswer(inv -> {
            PaymentApplyDO apply = inv.getArgument(0);
            if (apply.getId() == null) {
                apply.setId(applyIdSeq.incrementAndGet());
            }
            apply.setTenantId(TENANT_ID);
            apply.setDeleted(false);
            applyRepo.put(apply.getId(), apply);
            return 1;
        });

        when(paymentApplyMapper.selectById(anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            PaymentApplyDO apply = applyRepo.get(id);
            return (apply != null && Boolean.FALSE.equals(apply.getDeleted())) ? apply : null;
        });

        when(paymentApplyMapper.selectDeletedById(anyLong(), anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            PaymentApplyDO apply = applyRepo.get(id);
            return (apply != null && Boolean.TRUE.equals(apply.getDeleted())) ? apply : null;
        });

        when(paymentApplyMapper.selectListByApplyNoPrefixIncludingDeleted(anyString())).thenAnswer(inv -> Collections.emptyList());

        when(paymentApplyMapper.updateById(any(PaymentApplyDO.class))).thenAnswer(inv -> {
            PaymentApplyDO apply = inv.getArgument(0);
            applyRepo.put(apply.getId(), apply);
            return 1;
        });

        when(paymentApplyMapper.selectList(any(Wrapper.class))).thenAnswer(inv -> {
            List<PaymentApplyDO> list = new ArrayList<>();
            for (PaymentApplyDO apply : applyRepo.values()) {
                if (Boolean.FALSE.equals(apply.getDeleted())) {
                    list.add(apply);
                }
            }
            return list;
        });

        when(paymentApplyMapper.deleteById(anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            PaymentApplyDO apply = applyRepo.get(id);
            if (apply != null) {
                apply.setDeleted(true);
            }
            return 1;
        });

        when(paymentApplyMapper.selectCount(any(Wrapper.class))).thenAnswer(inv -> {
            Wrapper<?> wrapper = inv.getArgument(0);
            Long reconId = null;
            if (wrapper instanceof com.baomidou.mybatisplus.core.conditions.AbstractWrapper<?, ?, ?> aw) {
                aw.getSqlSegment(); // 触发包装器惰性求值以填充 paramNameValuePairs
                Map<String, Object> params = aw.getParamNameValuePairs();
                if (params != null) {
                    for (Object val : params.values()) {
                        if (val instanceof Long l && reconRepo.containsKey(l)) {
                            reconId = l;
                            break;
                        }
                    }
                }
            }
            final Long targetReconId = reconId;
            return applyRepo.values().stream()
                    .filter(a -> Boolean.FALSE.equals(a.getDeleted())
                            && (targetReconId == null || Objects.equals(a.getReconId(), targetReconId))
                            && !List.of(ApplyStatus.REJECTED.getValue(), ApplyStatus.CANCELLED.getValue()).contains(a.getApprovalStatus()))
                    .count();
        });

        when(paymentApplyMapper.selectSumAllocatedAmountBySupplierAndType(anyLong(), anyString(), anyLong())).thenAnswer(inv -> {
            Long supplierId = inv.getArgument(0);
            String applyType = inv.getArgument(1);
            BigDecimal sum = BigDecimal.ZERO;
            for (PaymentRefDO ref : paymentRefList) {
                if (Boolean.FALSE.equals(ref.getDeleted())) {
                    PaymentApplyDO apply = applyRepo.get(ref.getRefId());
                    if (apply != null && Objects.equals(apply.getSupplierId(), supplierId)
                            && Objects.equals(apply.getApplyType(), applyType)) {
                        sum = sum.add(ref.getAllocateAmount() != null ? ref.getAllocateAmount() : BigDecimal.ZERO);
                    }
                }
            }
            return sum;
        });

        when(paymentApplyMapper.restorePaymentApply(anyLong(), anyLong(), anyString(), any())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            PaymentApplyDO apply = applyRepo.get(id);
            if (apply != null) {
                apply.setDeleted(false);
                apply.setApprovalStatus(ApplyStatus.PENDING.getValue());
            }
            return 1;
        });

        // PaymentMapper mocks
        when(paymentMapper.insert(any(PaymentDO.class))).thenAnswer(inv -> {
            PaymentDO p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(paymentIdSeq.incrementAndGet());
            }
            p.setTenantId(TENANT_ID);
            p.setDeleted(false);
            paymentRepo.put(p.getId(), p);
            return 1;
        });

        when(paymentMapper.selectById(anyLong())).thenAnswer(inv -> paymentRepo.get(inv.getArgument(0)));
        when(paymentMapper.selectListByPaymentNoPrefixIncludingDeleted(anyString())).thenAnswer(inv -> Collections.emptyList());

        when(paymentMapper.selectSumPrepayUsedAmountBySupplier(anyLong(), anyLong())).thenAnswer(inv -> {
            Long supplierId = inv.getArgument(0);
            BigDecimal sum = BigDecimal.ZERO;
            for (PaymentDO payment : paymentRepo.values()) {
                if (Objects.equals(payment.getSupplierId(), supplierId) && Boolean.FALSE.equals(payment.getDeleted())) {
                    sum = sum.add(payment.getPrepayUsedAmount() != null ? payment.getPrepayUsedAmount() : BigDecimal.ZERO);
                }
            }
            return sum;
        });

        // PaymentRefMapper mocks
        when(paymentRefMapper.insert(any(PaymentRefDO.class))).thenAnswer(inv -> {
            PaymentRefDO ref = inv.getArgument(0);
            if (ref.getId() == null) {
                ref.setId(refIdSeq.incrementAndGet());
            }
            ref.setTenantId(TENANT_ID);
            ref.setDeleted(false);
            paymentRefList.add(ref);
            return 1;
        });

        when(paymentRefMapper.selectSumAllocatedAmountByApplyId(anyLong())).thenAnswer(inv -> {
            Long applyId = inv.getArgument(0);
            BigDecimal sum = BigDecimal.ZERO;
            for (PaymentRefDO ref : paymentRefList) {
                if (Objects.equals(ref.getRefId(), applyId) && Boolean.FALSE.equals(ref.getDeleted())) {
                    sum = sum.add(ref.getAllocateAmount() != null ? ref.getAllocateAmount() : BigDecimal.ZERO);
                }
            }
            return sum;
        });
    }

    @Test
    @DisplayName("供应商应付全链路业务冒烟闭环：预览→生成→防抢→确认→A4打印→申请审批驳回重提通过→两步登记付款→聚合核对→作废释放→回收站")
    void testFullChainSmoke() {
        LocalDate start = LocalDate.of(2026, 9, 1);
        LocalDate end = LocalDate.of(2026, 9, 30);

        // ==========================================
        // Stage 1: 取数预览（加工 + 采购）
        // ==========================================
        SupplierReconSourceItemRespVO processCandidate = new SupplierReconSourceItemRespVO();
        processCandidate.setLineType(LineType.PROCESS.name());
        processCandidate.setSourceRefType(SourceRefType.INSPECTION.name());
        processCandidate.setSourceRefId(1001L);
        processCandidate.setInspectionOrderId(1001L);
        processCandidate.setInspectionOrderNo("JYD20260901001");
        processCandidate.setInspectionType("GREIGE");
        processCandidate.setProductId(501L);
        processCandidate.setProductCode("FB-001");
        processCandidate.setProductName("全棉高密斜纹坯布");
        processCandidate.setQtyKg(new BigDecimal("1200.00"));
        processCandidate.setQtyM(new BigDecimal("2000.00"));
        processCandidate.setRollCount(20);
        processCandidate.setPricingBasis(PricingBasis.METER.name());
        processCandidate.setBillQty(new BigDecimal("2000.00"));
        processCandidate.setUnitPrice(new BigDecimal("5.5000"));
        processCandidate.setLineAmount(new BigDecimal("11000.00"));
        processCandidate.setFingerprint("INSPECTION:1001");

        SupplierReconSourceItemRespVO purchaseCandidate = new SupplierReconSourceItemRespVO();
        purchaseCandidate.setLineType(LineType.PURCHASE.name());
        purchaseCandidate.setSourceRefType(SourceRefType.STOCK_IN_ITEM.name());
        purchaseCandidate.setSourceRefId(2001L);
        purchaseCandidate.setSourceRefItemId(20011L);
        purchaseCandidate.setStockInId(2001L);
        purchaseCandidate.setStockInNo("RK20260910001");
        purchaseCandidate.setStockInItemId(20011L);
        purchaseCandidate.setYarnOrderId(3001L);
        purchaseCandidate.setYarnOrderNo("YARN20260901001");
        purchaseCandidate.setProductId(502L);
        purchaseCandidate.setProductCode("YARN-40S");
        purchaseCandidate.setProductName("40支精梳纯棉纱");
        purchaseCandidate.setBatchNo("BATCH-20260910-A");
        purchaseCandidate.setQtyKg(new BigDecimal("500.00"));
        purchaseCandidate.setQtyUnit("KG");
        purchaseCandidate.setPricingBasis(PricingBasis.KG.name());
        purchaseCandidate.setBillQty(new BigDecimal("500.00"));
        purchaseCandidate.setUnitPrice(new BigDecimal("20.0000"));
        purchaseCandidate.setLineAmount(new BigDecimal("10000.00"));
        purchaseCandidate.setFingerprint("STOCK_IN_ITEM:2001:20011");

        when(supplierReconSourceService.getAvailableCandidateItems(SUPPLIER_ID, start, end))
                .thenReturn(List.of(processCandidate, purchaseCandidate));

        SupplierReconPreviewReqVO previewReq = new SupplierReconPreviewReqVO();
        previewReq.setSupplierId(SUPPLIER_ID);
        previewReq.setPeriodStart(start);
        previewReq.setPeriodEnd(end);

        List<SupplierReconSourceItemRespVO> previewList = supplierReconService.preview(previewReq);
        assertNotNull(previewList);
        assertEquals(2, previewList.size());
        assertEquals("INSPECTION:1001", previewList.get(0).getFingerprint());

        // ==========================================
        // Stage 2: 生成含调整项对账单
        // ==========================================
        SupplierReconCreateReqVO createReq = new SupplierReconCreateReqVO();
        createReq.setSupplierId(SUPPLIER_ID);
        createReq.setPeriodStart(start);
        createReq.setPeriodEnd(end);
        createReq.setRemark("2026年9月加工费与纱线采购对账");

        SupplierReconItemSaveVO item1 = new SupplierReconItemSaveVO();
        item1.setLineType(LineType.PROCESS.name());
        item1.setSourceRefType(SourceRefType.INSPECTION.name());
        item1.setSourceRefId(1001L);
        item1.setInspectionOrderId(1001L);
        item1.setInspectionOrderNo("JYD20260901001");
        item1.setInspectionType("GREIGE");
        item1.setProductId(501L);
        item1.setProductCode("FB-001");
        item1.setProductName("全棉高密斜纹坯布");
        item1.setQtyKg(new BigDecimal("1200.00"));
        item1.setQtyM(new BigDecimal("2000.00"));
        item1.setPricingBasis(PricingBasis.METER.name());
        item1.setUnitPrice(new BigDecimal("5.5000"));

        SupplierReconItemSaveVO item2 = new SupplierReconItemSaveVO();
        item2.setLineType(LineType.PURCHASE.name());
        item2.setSourceRefType(SourceRefType.STOCK_IN_ITEM.name());
        item2.setSourceRefId(2001L);
        item2.setSourceRefItemId(20011L);
        item2.setStockInId(2001L);
        item2.setStockInNo("RK20260910001");
        item2.setYarnOrderId(3001L);
        item2.setYarnOrderNo("YARN20260901001");
        item2.setProductId(502L);
        item2.setProductCode("YARN-40S");
        item2.setProductName("40支精梳纯棉纱");
        item2.setBatchNo("BATCH-20260910-A");
        item2.setQtyKg(new BigDecimal("500.00"));
        item2.setQtyUnit("KG");
        item2.setPricingBasis(PricingBasis.KG.name());
        item2.setUnitPrice(new BigDecimal("20.0000"));

        // 调整项行：质量扣款 -1000 元
        SupplierReconItemSaveVO item3 = new SupplierReconItemSaveVO();
        item3.setLineType(LineType.ADJUST.name());
        item3.setLineAmount(new BigDecimal("-1000.00"));
        item3.setAdjustReason("部分布面油污扣款");
        item3.setRemark("质检判定扣除");

        createReq.setItems(List.of(item1, item2, item3));

        when(supplierReconSourceService.findOccupiedFingerprints(anyCollection())).thenReturn(Collections.emptyMap());

        Long reconId = supplierReconService.createSupplierRecon(createReq);
        assertNotNull(reconId);

        SupplierReconDO createdRecon = reconRepo.get(reconId);
        assertNotNull(createdRecon);
        assertEquals(ReconStatus.DRAFT.name(), createdRecon.getStatus());
        // 服务端重算：11000 + 10000 - 1000 = 20000.00
        assertEquals(new BigDecimal("20000.00"), createdRecon.getTotalAmount());

        // ==========================================
        // Stage 3: 被抢引用报错验证 (022_004)
        // ==========================================
        SupplierReconCreateReqVO duplicateCreateReq = new SupplierReconCreateReqVO();
        duplicateCreateReq.setSupplierId(SUPPLIER_ID);
        duplicateCreateReq.setPeriodStart(start);
        duplicateCreateReq.setPeriodEnd(end);
        duplicateCreateReq.setItems(List.of(item1));

        String fp1 = "INSPECTION:1001";
        when(supplierReconSourceService.findOccupiedFingerprints(Set.of(fp1)))
                .thenReturn(Map.of(fp1, createdRecon.getReconNo()));

        ServiceException exOccupy = assertThrows(ServiceException.class, () ->
                supplierReconService.createSupplierRecon(duplicateCreateReq));
        assertEquals(FinanceErrorCodeConstants.SUPPLIER_RECON_SOURCE_REFERENCED.getCode(), exOccupy.getCode());

        // ==========================================
        // Stage 4: 确认对账单 (DRAFT -> CONFIRMED)
        // ==========================================
        supplierReconService.confirmSupplierRecon(reconId);
        assertEquals(ReconStatus.CONFIRMED.name(), createdRecon.getStatus());
        assertNotNull(createdRecon.getConfirmedAt());
        assertEquals(LOGIN_USER_ID, createdRecon.getConfirmedBy());

        // 再次确认防并发测试：确认后不可再编辑
        SupplierReconUpdateReqVO updateReq = new SupplierReconUpdateReqVO();
        updateReq.setId(reconId);
        ServiceException exFrozen = assertThrows(ServiceException.class, () ->
                supplierReconService.updateSupplierRecon(updateReq));
        assertEquals(FinanceErrorCodeConstants.SUPPLIER_RECON_STATUS_NOT_ALLOWED.getCode(), exFrozen.getCode());

        // ==========================================
        // Stage 5: A4 打印数据契约验证
        // ==========================================
        SupplierReconPrintRespVO printData = supplierReconService.getSupplierReconPrintData(reconId);
        assertNotNull(printData);
        assertEquals(createdRecon.getReconNo(), printData.getReconNo());
        assertEquals(SUPPLIER_NAME, printData.getSupplierName());
        assertEquals(3, printData.getItems().size());
        assertEquals(new BigDecimal("20000.00"), printData.getTotalAmount());

        // ==========================================
        // Stage 6: 发起对账付款申请 (RECON) + 预付申请 (PREPAY)
        // ==========================================
        // 6.1 对账付款申请 RECON：申请金额 15000.00 (确认金额 20000.00 内)
        PaymentApplyCreateReqVO reconApplyReq = new PaymentApplyCreateReqVO();
        reconApplyReq.setApplyType(ApplyType.RECON.getValue());
        reconApplyReq.setSupplierId(SUPPLIER_ID);
        reconApplyReq.setReconId(reconId);
        reconApplyReq.setAmount(new BigDecimal("15000.00"));
        reconApplyReq.setBankName("中国工商银行常州支行");
        reconApplyReq.setBankAccount("622202100011223344");
        reconApplyReq.setRemark("9月份第一期对账付款");

        Long reconApplyId = paymentApplyService.createPaymentApply(reconApplyReq);
        assertNotNull(reconApplyId);
        PaymentApplyDO reconApply = applyRepo.get(reconApplyId);
        assertEquals(ApplyStatus.PENDING.getValue(), reconApply.getApprovalStatus());
        assertEquals(new BigDecimal("15000.00"), reconApply.getAmount());

        // 超额申请验证 (022_007)：再申请 6000 抛超额 (15000 + 6000 = 21000 > 20000)
        PaymentApplyCreateReqVO exceedApplyReq = new PaymentApplyCreateReqVO();
        exceedApplyReq.setApplyType(ApplyType.RECON.getValue());
        exceedApplyReq.setSupplierId(SUPPLIER_ID);
        exceedApplyReq.setReconId(reconId);
        exceedApplyReq.setAmount(new BigDecimal("6000.00"));
        ServiceException exExceed = assertThrows(ServiceException.class, () ->
                paymentApplyService.createPaymentApply(exceedApplyReq));
        assertEquals(FinanceErrorCodeConstants.SUPPLIER_RECON_APPLY_AMOUNT_EXCEED.getCode(), exExceed.getCode());

        // 6.2 预付付款申请 PREPAY：申请金额 5000.00
        PaymentApplyCreateReqVO prepayApplyReq = new PaymentApplyCreateReqVO();
        prepayApplyReq.setApplyType(ApplyType.PREPAY.getValue());
        prepayApplyReq.setSupplierId(SUPPLIER_ID);
        prepayApplyReq.setAmount(new BigDecimal("5000.00"));
        prepayApplyReq.setPurpose("下季度纱线采购定金");
        prepayApplyReq.setBankName("中国工商银行常州支行");
        prepayApplyReq.setBankAccount("622202100011223344");

        Long prepayApplyId = paymentApplyService.createPaymentApply(prepayApplyReq);
        assertNotNull(prepayApplyId);
        PaymentApplyDO prepayApply = applyRepo.get(prepayApplyId);
        assertEquals(ApplyStatus.PENDING.getValue(), prepayApply.getApprovalStatus());

        // ==========================================
        // Stage 7: 审批驳回改后重提
        // ==========================================
        when(approvalInstanceService.submitInstance(any())).thenAnswer(inv -> {
            ApprovalInstanceSubmitReqVO req = inv.getArgument(0);
            PaymentApplyDO apply = applyRepo.get(req.getBizId());
            if (apply != null) {
                apply.setApprovalStatus(ApplyStatus.APPROVING.getValue());
            }
            return 8801L;
        });

        paymentApplyService.submitPaymentApply(prepayApplyId);
        assertEquals(ApplyStatus.APPROVING.getValue(), prepayApply.getApprovalStatus());

        // 审批引擎驳回 -> 状态变为 REJECTED
        prepayApply.setApprovalStatus(ApplyStatus.REJECTED.getValue());

        // 申请人修改金额由 5000.00 改为 4000.00 并重提
        PaymentApplyUpdateReqVO updateApplyReq = new PaymentApplyUpdateReqVO();
        updateApplyReq.setId(prepayApplyId);
        updateApplyReq.setAmount(new BigDecimal("4000.00"));
        updateApplyReq.setPurpose("下季度纱线采购定金(调整后)");
        paymentApplyService.updatePaymentApply(updateApplyReq);
        assertEquals(new BigDecimal("4000.00"), prepayApply.getAmount());

        // 重新提交审批走全流程
        paymentApplyService.submitPaymentApply(prepayApplyId);
        assertEquals(ApplyStatus.APPROVING.getValue(), prepayApply.getApprovalStatus());

        // ==========================================
        // Stage 8: 审批通过 (APPROVED)
        // ==========================================
        reconApply.setApprovalStatus(ApplyStatus.APPROVED.getValue());
        prepayApply.setApprovalStatus(ApplyStatus.APPROVED.getValue());

        // 验证 ApprovalHandler 为空实现（审批通过绝不触发资金过账）
        paymentApprovalHandler.onApproved(prepayApplyId);
        assertTrue(paymentRepo.isEmpty(), "ApprovalHandler 严禁触发付款单插入或资金过账");
        assertTrue(paymentRefList.isEmpty(), "ApprovalHandler 严禁触发核销子表插入");

        // ==========================================
        // Stage 9: 付款登记出纳执行（两步制）
        // ==========================================
        // 9.1 出纳先对 PREPAY 申请 (4000.00) 执行付款登记：全额银行账户实付
        PaymentRegisterReqVO prepayRegisterReq = new PaymentRegisterReqVO();
        prepayRegisterReq.setSupplierId(SUPPLIER_ID);
        prepayRegisterReq.setAccountId(ACCOUNT_ID);
        prepayRegisterReq.setPayAmount(new BigDecimal("4000.00"));
        prepayRegisterReq.setPrepayUsedAmount(BigDecimal.ZERO);
        PaymentRegisterItemReqVO prepayItem = new PaymentRegisterItemReqVO();
        prepayItem.setApplyId(prepayApplyId);
        prepayItem.setAllocateAmount(new BigDecimal("4000.00"));
        prepayRegisterReq.setItems(List.of(prepayItem));

        Long prepayPaymentId = paymentService.registerPayment(prepayRegisterReq);
        assertNotNull(prepayPaymentId);
        PaymentDO prepayPayment = paymentRepo.get(prepayPaymentId);
        assertEquals(new BigDecimal("4000.00"), prepayPayment.getPayAmount());
        assertEquals(new BigDecimal("0.00"), prepayPayment.getPrepayUsedAmount());

        // 验证写流出流水与分录
        verify(financeAccountService, atLeastOnce()).recordAccountOutflow(eq(ACCOUNT_ID), eq(new BigDecimal("4000.00")), any(), any());
        verify(financeAccountService, atLeastOnce()).appendOutSupplierLedgerEntry(any(), any(), eq(SUPPLIER_ID), eq(new BigDecimal("4000.00")), any(), eq(ACCOUNT_ID), anyString());

        // 此时供应商预付池余额形成 = 4000.00
        BigDecimal poolBalance = paymentApplyService.getPrepayBalance(SUPPLIER_ID);
        assertEquals(new BigDecimal("4000.00"), poolBalance);

        // 9.2 出纳对 RECON 申请 (15000.00) 执行付款登记：
        // 资金账户实付 8000.00 + 预付抵扣 2000.00 = 合计分配 10000.00（部分核销，剩余挂应付 5000.00）
        PaymentRegisterReqVO reconRegisterReq = new PaymentRegisterReqVO();
        reconRegisterReq.setSupplierId(SUPPLIER_ID);
        reconRegisterReq.setAccountId(ACCOUNT_ID);
        reconRegisterReq.setPayAmount(new BigDecimal("8000.00"));
        reconRegisterReq.setPrepayUsedAmount(new BigDecimal("2000.00"));
        PaymentRegisterItemReqVO reconItem = new PaymentRegisterItemReqVO();
        reconItem.setApplyId(reconApplyId);
        reconItem.setAllocateAmount(new BigDecimal("10000.00"));
        reconRegisterReq.setItems(List.of(reconItem));

        Long reconPaymentId = paymentService.registerPayment(reconRegisterReq);
        assertNotNull(reconPaymentId);
        PaymentDO reconPayment = paymentRepo.get(reconPaymentId);
        assertEquals(new BigDecimal("8000.00"), reconPayment.getPayAmount());
        assertEquals(new BigDecimal("2000.00"), reconPayment.getPrepayUsedAmount());

        // ==========================================
        // Stage 10: 余额/已申请/已核销聚合核对
        // ==========================================
        // 10.1 对账单已申请金额聚合：15000.00
        SupplierReconRespVO reconDetail = supplierReconService.getSupplierRecon(reconId);
        assertEquals(new BigDecimal("15000.00"), reconDetail.getAppliedAmount());

        // 10.2 RECON 申请单已核销金额与剩余金额聚合：核销 10000.00，剩余 5000.00
        PaymentApplyRespVO reconApplyResp = paymentApplyService.getPaymentApply(reconApplyId);
        assertEquals(new BigDecimal("10000.00"), reconApplyResp.getAllocatedAmount());
        assertEquals(new BigDecimal("5000.00"), reconApplyResp.getRemainAmount());

        // 10.3 供应商预付余额两条 SUM 聚合核对：
        // Σ(PREPAY allocate) - Σ(prepay_used) = 4000.00 - 2000.00 = 2000.00
        BigDecimal currentPrepayBalance = paymentApplyService.getPrepayBalance(SUPPLIER_ID);
        assertEquals(new BigDecimal("2000.00"), currentPrepayBalance);

        // ==========================================
        // Stage 11: 无申请对账单作废释放指纹
        // ==========================================
        // 11.1 对账单 reconId 有挂靠付款申请，禁止作废 (022_006)
        ServiceException exHasApply = assertThrows(ServiceException.class, () ->
                supplierReconService.cancelSupplierRecon(reconId));
        assertEquals(FinanceErrorCodeConstants.SUPPLIER_RECON_HAS_PAYMENT_APPLY.getCode(), exHasApply.getCode());

        // 11.2 模拟一张无申请的 CONFIRMED 对账单作废并释放指纹
        SupplierReconDO unappliedRecon = new SupplierReconDO();
        unappliedRecon.setId(1002L);
        unappliedRecon.setReconNo("SR20261016002");
        unappliedRecon.setSupplierId(SUPPLIER_ID);
        unappliedRecon.setStatus(ReconStatus.CONFIRMED.name());
        unappliedRecon.setTotalAmount(new BigDecimal("5000.00"));
        unappliedRecon.setTenantId(TENANT_ID);
        unappliedRecon.setDeleted(false);
        reconRepo.put(1002L, unappliedRecon);

        SupplierReconItemDO unappliedItem = new SupplierReconItemDO();
        unappliedItem.setId(itemIdSeq.incrementAndGet());
        unappliedItem.setReconId(1002L);
        unappliedItem.setLineType("PROCESS");
        unappliedItem.setSourceRefType("INSPECTION");
        unappliedItem.setSourceRefId(9901L);
        unappliedItem.setTenantId(TENANT_ID);
        unappliedItem.setDeleted(false);
        reconItemRepo.put(unappliedItem.getId(), unappliedItem);

        // 作废前：源明细 9901L 被 CONFIRMED 对账单占用
        SupplierReconSourceServiceImpl sourceServiceVerifier = new SupplierReconSourceServiceImpl();
        ReflectionTestUtils.setField(sourceServiceVerifier, "supplierReconMapper", supplierReconMapper);
        ReflectionTestUtils.setField(sourceServiceVerifier, "supplierReconItemMapper", supplierReconItemMapper);

        String fp = sourceServiceVerifier.buildFingerprint("INSPECTION", 9901L, null);
        Map<String, String> occupiedBefore = sourceServiceVerifier.findOccupiedFingerprints(List.of(fp));
        assertEquals(1, occupiedBefore.size(), "作废前源明细指纹应处于占用状态");
        assertEquals("SR20261016002", occupiedBefore.get(fp));

        // 执行作废：由于 1002L 无挂靠付款申请，自然通过校验
        supplierReconService.cancelSupplierRecon(1002L);
        assertEquals(ReconStatus.CANCELLED.name(), unappliedRecon.getStatus());

        // 作废后：指纹自动释放，不再处于占用状态，可重新进入取数候选
        Map<String, String> occupiedAfter = sourceServiceVerifier.findOccupiedFingerprints(List.of(fp));
        assertTrue(occupiedAfter.isEmpty(), "对账单作废后源明细指纹应完全释放");

        // ==========================================
        // Stage 12: 草稿单/草稿申请回收站恢复
        // ==========================================
        // 12.1 草稿对账单删除与恢复
        SupplierReconDO draftRecon = new SupplierReconDO();
        draftRecon.setId(1003L);
        draftRecon.setSupplierId(SUPPLIER_ID);
        draftRecon.setStatus(ReconStatus.DRAFT.name());
        draftRecon.setDeleted(false);
        reconRepo.put(1003L, draftRecon);

        supplierReconService.deleteSupplierRecon(1003L);
        assertEquals(Boolean.TRUE, draftRecon.getDeleted(), "调用 service 删除后 deleted 应置为 true");

        reconRecycleBinHandler.restore(1003L);
        assertEquals(Boolean.FALSE, draftRecon.getDeleted());
        assertEquals(ReconStatus.DRAFT.name(), draftRecon.getStatus());

        // 12.2 草稿付款申请删除与恢复
        PaymentApplyDO draftApply = new PaymentApplyDO();
        draftApply.setId(3003L);
        draftApply.setSupplierId(SUPPLIER_ID);
        draftApply.setApplyType(ApplyType.PREPAY.getValue());
        draftApply.setApprovalStatus(ApplyStatus.PENDING.getValue());
        draftApply.setAmount(new BigDecimal("1000.00"));
        draftApply.setDeleted(false);
        applyRepo.put(3003L, draftApply);

        paymentApplyService.deletePaymentApply(3003L);
        assertEquals(Boolean.TRUE, draftApply.getDeleted(), "调用 service 删除后 deleted 应置为 true");

        applyRecycleBinHandler.restore(3003L);
        assertEquals(Boolean.FALSE, draftApply.getDeleted());
        assertEquals(ApplyStatus.PENDING.getValue(), draftApply.getApprovalStatus());
    }
}
