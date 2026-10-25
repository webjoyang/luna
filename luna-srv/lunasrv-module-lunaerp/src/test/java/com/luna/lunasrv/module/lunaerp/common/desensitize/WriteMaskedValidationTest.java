package com.luna.lunasrv.module.lunaerp.common.desensitize;

import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.exception.enums.GlobalErrorCodeConstants;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customer.vo.CustomerSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customercontact.vo.CustomerContactSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.product.vo.ProductSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.supplier.vo.SupplierSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.suppliercontact.vo.SupplierContactSaveReqVO;
import com.luna.lunasrv.module.lunaerp.service.baseinfo.customer.CustomerServiceImpl;
import com.luna.lunasrv.module.lunaerp.service.baseinfo.customercontact.CustomerContactServiceImpl;
import com.luna.lunasrv.module.lunaerp.service.baseinfo.product.ProductServiceImpl;
import com.luna.lunasrv.module.lunaerp.service.baseinfo.supplier.SupplierServiceImpl;
import com.luna.lunasrv.module.lunaerp.service.baseinfo.suppliercontact.SupplierContactServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 校验写接口不接受脱敏占位符（"****" 或含 "*"）入库
 */
@ExtendWith(MockitoExtension.class)
public class WriteMaskedValidationTest {

    @InjectMocks
    private CustomerContactServiceImpl customerContactService;

    @InjectMocks
    private SupplierContactServiceImpl supplierContactService;

    @InjectMocks
    private CustomerServiceImpl customerService;

    @InjectMocks
    private SupplierServiceImpl supplierService;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    public void testCustomerContact_RejectMasked() {
        CustomerContactSaveReqVO vo1 = new CustomerContactSaveReqVO();
        vo1.setName("张*");
        vo1.setCustomerId(1L);
        ServiceException ex1 = assertThrows(ServiceException.class, () -> customerContactService.createCustomerContact(vo1));
        assertEquals(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), ex1.getCode());

        CustomerContactSaveReqVO vo2 = new CustomerContactSaveReqVO();
        vo2.setName("张三");
        vo2.setPhone("138****1234");
        vo2.setCustomerId(1L);
        ServiceException ex2 = assertThrows(ServiceException.class, () -> customerContactService.createCustomerContact(vo2));
        assertEquals(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), ex2.getCode());

        CustomerContactSaveReqVO vo3 = new CustomerContactSaveReqVO();
        vo3.setName("张三");
        vo3.setPhone("13812345678");
        vo3.setEmail("t****@test.com");
        vo3.setCustomerId(1L);
        ServiceException ex3 = assertThrows(ServiceException.class, () -> customerContactService.createCustomerContact(vo3));
        assertEquals(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), ex3.getCode());
    }

    @Test
    public void testSupplierContact_RejectMasked() {
        SupplierContactSaveReqVO vo1 = new SupplierContactSaveReqVO();
        vo1.setName("李*");
        vo1.setSupplierId(1L);
        ServiceException ex1 = assertThrows(ServiceException.class, () -> supplierContactService.createSupplierContact(vo1));
        assertEquals(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), ex1.getCode());

        SupplierContactSaveReqVO vo2 = new SupplierContactSaveReqVO();
        vo2.setName("李四");
        vo2.setPhone("138****0000");
        vo2.setSupplierId(1L);
        ServiceException ex2 = assertThrows(ServiceException.class, () -> supplierContactService.createSupplierContact(vo2));
        assertEquals(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), ex2.getCode());
    }

    @Test
    public void testCustomer_RejectMaskedPhone() {
        CustomerSaveReqVO vo = new CustomerSaveReqVO();
        vo.setCompanyName("测试企业");
        vo.setClientType("FORMAL");
        vo.setPhone("0571*******99");
        ServiceException ex = assertThrows(ServiceException.class, () -> customerService.createCustomer(vo));
        assertEquals(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), ex.getCode());
    }

    @Test
    public void testSupplier_RejectMaskedPhone() {
        SupplierSaveReqVO vo = new SupplierSaveReqVO();
        vo.setCompanyName("测试供应商");
        vo.setPhone("0575*******99");
        ServiceException ex = assertThrows(ServiceException.class, () -> supplierService.createSupplier(vo));
        assertEquals(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), ex.getCode());
    }

    @Test
    public void testProduct_RejectMaskedCostItems() {
        ProductSaveReqVO vo = new ProductSaveReqVO();
        vo.setCode("P9999");
        vo.setName("测试面料");
        vo.setCostItems("[{\"price\":\"****\",\"process\":\"织造\"}]");
        ServiceException ex = assertThrows(ServiceException.class, () -> productService.createProduct(vo));
        assertEquals(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), ex.getCode());
    }
}
