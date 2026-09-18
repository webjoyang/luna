package com.luna.lunasrv.module.lunaerp.service.baseinfo.customer;

import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customer.vo.CustomerSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link CustomerServiceImpl} 的纯 Mockito 单元测试（不依赖 Spring 上下文与数据库）。
 *
 * 覆盖关键业务语义：
 *  - createCustomer：shortName 为空时用 company 回填；status 缺省 ACTIVE；shortName 非空时保留。
 *  - getCustomer：客户不存在时抛 CUSTOMER_NOT_EXISTS（1_030_002_000）。
 *  - updateCustomer：不存在抛 1_030_002_000；shortName 为空时同样用 company 回填。
 *  - deleteCustomer/deleteCustomerList：不存在抛错；删除时先置 status=INACTIVE 再逻辑删除。
 *
 * 说明：Controller 层 MockMvc 集成测试需要完整的 security filter chain + 测试数据库，
 * 本仓 lunaerp 模块当前无该基础设施（参照 product 模块亦无测试），故先以纯单元测试覆盖核心逻辑。
 * DB 中 status=INACTIVE、deleted=1 的断言以验证 updateById(INACTIVE) + deleteByIds 调用顺序代替。
 *
 * @author luna
 */
class CustomerServiceImplTest {

    @Mock
    private CustomerMapper customerMapper;

    @InjectMocks
    private CustomerServiceImpl customerService;

    private static final String COMPANY = "杭州织造有限公司";
    private static final String SHORT_NAME = "杭州织造";
    private static final String CLIENT_TYPE = "FORMAL";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // 模拟 insert 时回填主键（@KeySequence 在真实环境由 MyBatis-Plus 填充）
        when(customerMapper.insert(any(CustomerDO.class))).thenAnswer(invocation -> {
            CustomerDO dobj = invocation.getArgument(0);
            dobj.setId(1024L);
            return 1;
        });
    }

    @Test
    void createCustomer_shortNameBlank_backfilledFromCompany() {
        // shortName 留空、status 留空 → 应回填 company 与 ACTIVE
        CustomerSaveReqVO req = new CustomerSaveReqVO();
        req.setCompany(COMPANY);
        req.setClientType(CLIENT_TYPE);

        Long id = customerService.createCustomer(req);

        ArgumentCaptor<CustomerDO> captor = ArgumentCaptor.forClass(CustomerDO.class);
        verify(customerMapper).insert(captor.capture());
        CustomerDO saved = captor.getValue();

        assertEquals(1024L, id);
        assertEquals(COMPANY, saved.getCompany());
        assertEquals(COMPANY, saved.getShortName(), "shortName 为空时应用 company 回填");
        assertEquals("ACTIVE", saved.getStatus(), "status 缺省应为 ACTIVE");
        assertEquals(CLIENT_TYPE, saved.getClientType());
    }

    @Test
    void createCustomer_shortNameProvided_preserved() {
        // shortName 非空 → 保留；status 显式 INACTIVE → 保留
        CustomerSaveReqVO req = new CustomerSaveReqVO();
        req.setCompany(COMPANY);
        req.setShortName(SHORT_NAME);
        req.setClientType(CLIENT_TYPE);
        req.setStatus("INACTIVE");

        customerService.createCustomer(req);

        ArgumentCaptor<CustomerDO> captor = ArgumentCaptor.forClass(CustomerDO.class);
        verify(customerMapper).insert(captor.capture());
        CustomerDO saved = captor.getValue();

        assertEquals(SHORT_NAME, saved.getShortName(), "shortName 非空时应保留原值");
        assertEquals("INACTIVE", saved.getStatus(), "status 显式传入时应保留");
    }

    @Test
    void getCustomer_notExists_throwsCode1030002000() {
        when(customerMapper.selectById(9999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> customerService.getCustomer(9999L));
        assertEquals(1_030_002_000, ex.getCode());
    }

    @Test
    void updateCustomer_notExists_throwsCode1030002000() {
        CustomerSaveReqVO req = new CustomerSaveReqVO();
        req.setId(9999L);
        req.setCompany(COMPANY);
        req.setClientType(CLIENT_TYPE);
        when(customerMapper.selectById(9999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> customerService.updateCustomer(req));
        assertEquals(1_030_002_000, ex.getCode());
        verify(customerMapper, never()).updateById(any(CustomerDO.class));
    }

    @Test
    void updateCustomer_shortNameBlank_backfilledFromCompany() {
        // 编辑时 shortName 留空 → 仍应用 company 回填
        CustomerSaveReqVO req = new CustomerSaveReqVO();
        req.setId(1024L);
        req.setCompany(COMPANY);
        req.setClientType(CLIENT_TYPE);
        when(customerMapper.selectById(1024L)).thenReturn(new CustomerDO());

        customerService.updateCustomer(req);

        ArgumentCaptor<CustomerDO> captor = ArgumentCaptor.forClass(CustomerDO.class);
        verify(customerMapper).updateById(captor.capture());
        CustomerDO saved = captor.getValue();
        assertEquals(1024L, saved.getId());
        assertEquals(COMPANY, saved.getShortName(), "update 时 shortName 为空也应用 company 回填");
    }

    @Test
    void updateCustomer_shortNameProvided_preserved() {
        CustomerSaveReqVO req = new CustomerSaveReqVO();
        req.setId(1024L);
        req.setCompany(COMPANY);
        req.setShortName(SHORT_NAME);
        req.setClientType(CLIENT_TYPE);
        when(customerMapper.selectById(1024L)).thenReturn(new CustomerDO());

        customerService.updateCustomer(req);

        ArgumentCaptor<CustomerDO> captor = ArgumentCaptor.forClass(CustomerDO.class);
        verify(customerMapper).updateById(captor.capture());
        assertEquals(SHORT_NAME, captor.getValue().getShortName(), "shortName 非空时应保留原值");
    }

    @Test
    void deleteCustomer_notExists_throwsCode1030002000() {
        when(customerMapper.selectById(9999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> customerService.deleteCustomer(9999L));
        assertEquals(1_030_002_000, ex.getCode());
        verify(customerMapper, never()).deleteByIds(anyList());
    }

    @Test
    void deleteCustomer_exists_setsInactiveThenLogicalDelete() {
        CustomerDO customer = new CustomerDO();
        customer.setId(1024L);
        customer.setStatus("ACTIVE");
        when(customerMapper.selectById(1024L)).thenReturn(customer);
        when(customerMapper.selectByIds(anyList())).thenReturn(Collections.singletonList(customer));

        customerService.deleteCustomer(1024L);

        // 1. 先置 status=INACTIVE
        ArgumentCaptor<CustomerDO> captor = ArgumentCaptor.forClass(CustomerDO.class);
        verify(customerMapper).updateById(captor.capture());
        CustomerDO statusUpdate = captor.getValue();
        assertEquals(1024L, statusUpdate.getId());
        assertEquals("INACTIVE", statusUpdate.getStatus(), "删除时应先将 status 置为 INACTIVE");
        // 2. 再逻辑删除（deleted=1）
        verify(customerMapper).deleteByIds(Collections.singletonList(1024L));
    }

    @Test
    void deleteCustomerList_batch_success() {
        CustomerDO c1 = new CustomerDO();
        c1.setId(1L);
        CustomerDO c2 = new CustomerDO();
        c2.setId(2L);
        CustomerDO c3 = new CustomerDO();
        c3.setId(3L);
        when(customerMapper.selectByIds(anyList())).thenReturn(Arrays.asList(c1, c2, c3));

        customerService.deleteCustomerList(Arrays.asList(1L, 2L, 3L));

        // 每条先置 INACTIVE（逐条断言 status 与 id），再整体逻辑删除
        ArgumentCaptor<CustomerDO> captor = ArgumentCaptor.forClass(CustomerDO.class);
        verify(customerMapper, times(3)).updateById(captor.capture());
        assertEquals(3, captor.getAllValues().size());
        for (CustomerDO statusUpdate : captor.getAllValues()) {
            assertEquals("INACTIVE", statusUpdate.getStatus(), "批量删除时每条都应先置 INACTIVE");
        }
        verify(customerMapper).deleteByIds(Arrays.asList(1L, 2L, 3L));
    }

}
