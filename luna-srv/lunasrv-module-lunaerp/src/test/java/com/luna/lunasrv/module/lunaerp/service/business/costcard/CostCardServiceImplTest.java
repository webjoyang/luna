package com.luna.lunasrv.module.lunaerp.service.business.costcard;

import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.controller.business.costcard.vo.*;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnprice.vo.YarnPriceEffectiveRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnrecipebom.vo.YarnRecipeBomItemRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnrecipebom.vo.YarnRecipeBomRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.costcard.CostCardDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.costcard.CostCardFeeDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.costcard.CostCardYarnDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnspec.YarnSpecDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.product.ProductMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.costcard.CostCardFeeMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.costcard.CostCardMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.costcard.CostCardYarnMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnspec.YarnSpecMapper;
import com.luna.lunasrv.module.lunaerp.service.production.yarnprice.YarnPriceService;
import com.luna.lunasrv.module.lunaerp.service.production.yarnrecipebom.YarnRecipeBomService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.luna.lunasrv.module.lunaerp.enums.CostCardQuotationErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CostCardServiceImplTest {

    @Mock
    private CostCardMapper costCardMapper;

    @Mock
    private CostCardYarnMapper costCardYarnMapper;

    @Mock
    private CostCardFeeMapper costCardFeeMapper;

    @Mock
    private YarnPriceService yarnPriceService;

    @Mock
    private YarnSpecMapper yarnSpecMapper;

    @Mock
    private YarnRecipeBomService yarnRecipeBomService;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private CostCardServiceImpl costCardService;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    private YarnRecipeBomRespVO mockValidBom(Long productId) {
        YarnRecipeBomItemRespVO item = new YarnRecipeBomItemRespVO();
        item.setId(10L);
        item.setYarnSpecId(101L);
        item.setRatio(new BigDecimal("100.00"));
        YarnRecipeBomRespVO bom = new YarnRecipeBomRespVO();
        bom.setId(1L);
        bom.setProductId(productId);
        bom.setItems(Collections.singletonList(item));
        return bom;
    }

    @Test
    @DisplayName("创建成本卡：自动取号、重排line_no与服务端权威算价落库")
    void testCreateCostCard_AutoNumber_Success() {
        Long productId = 100L;
        when(yarnRecipeBomService.getYarnRecipeBomByProductId(productId)).thenReturn(mockValidBom(productId));
        when(costCardMapper.selectListByCardNoPrefixIncludingDeleted(any())).thenReturn(Collections.emptyList());
        when(costCardMapper.selectByCardNoIncludingDeleted(any())).thenReturn(null);

        // 纱价取价
        when(yarnPriceService.getEffectivePrice(eq(101L), eq("PURCHASE"), any(LocalDate.class)))
                .thenReturn(YarnPriceEffectiveRespVO.builder()
                        .price(new BigDecimal("30.0000"))
                        .priceType("PURCHASE")
                        .effectiveDate(LocalDate.now())
                        .build());

        CostCardSaveReqVO req = CostCardSaveReqVO.builder()
                .productId(productId)
                .productCode("P001")
                .productName("测试面料")
                .fabricConstruction("KNIT")
                .yarnPriceType("PURCHASE")
                .basis("KG")
                .taxRate(new BigDecimal("0.13"))
                .yarnItems(Collections.singletonList(
                        CostCardYarnItemVO.builder()
                                .yarnSpecId(101L)
                                .yarnSpecName("32S 棉")
                                .ratio(new BigDecimal("100.00"))
                                .wasteRate(new BigDecimal("5.00"))
                                .build()
                ))
                .feeItems(Collections.singletonList(
                        CostCardFeeItemVO.builder()
                                .feeType("WEAVING")
                                .feeUnit("PER_KG")
                                .price(new BigDecimal("6.0000"))
                                .lossRate(new BigDecimal("2.00"))
                                .build()
                ))
                .build();

        doAnswer(inv -> {
            CostCardDO c = inv.getArgument(0);
            c.setId(888L);
            return 1;
        }).when(costCardMapper).insert(any(CostCardDO.class));

        Long id = costCardService.createCostCard(req);
        assertEquals(888L, id);

        ArgumentCaptor<CostCardDO> cardCaptor = ArgumentCaptor.forClass(CostCardDO.class);
        verify(costCardMapper).insert(cardCaptor.capture());
        CostCardDO saved = cardCaptor.getValue();
        assertEquals("DRAFT", saved.getStatus());
        assertTrue(saved.getCardNo().startsWith("CC"));
        assertEquals(0, saved.getMissingPriceCount());
        assertNotNull(saved.getYarnCostPerKg());
        assertNotNull(saved.getTotalFeePerKg());
        assertNotNull(saved.getUnitCostPerKg());

        verify(costCardYarnMapper).insertBatch(anyList());
        verify(costCardFeeMapper).insertBatch(anyList());
    }

    @Test
    @DisplayName("创建成本卡：手填卡号重复报 1_030_034_001")
    void testCreateCostCard_ManualDuplicateNumber_Throws001() {
        String manualNo = "CC20268888";
        when(costCardMapper.selectByCardNoIncludingDeleted(manualNo)).thenReturn(
                CostCardDO.builder().id(999L).cardNo(manualNo).build()
        );

        CostCardSaveReqVO req = CostCardSaveReqVO.builder()
                .cardNo(manualNo)
                .productId(100L)
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> costCardService.createCostCard(req));
        assertEquals(COST_CARD_NO_DUPLICATE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("创建成本卡：产品无有效 BOM 报 1_030_034_004")
    void testCreateCostCard_MissingRecipeBom_Throws004() {
        Long productId = 100L;
        when(costCardMapper.selectListByCardNoPrefixIncludingDeleted(any())).thenReturn(Collections.emptyList());
        when(costCardMapper.selectByCardNoIncludingDeleted(any())).thenReturn(null);
        when(yarnRecipeBomService.getYarnRecipeBomByProductId(productId)).thenReturn(null);

        CostCardSaveReqVO req = CostCardSaveReqVO.builder()
                .productId(productId)
                .productName("无BOM面料")
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> costCardService.createCostCard(req));
        assertEquals(COST_CARD_RECIPE_BOM_MISSING.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("更新成本卡：草稿状态成功整体替换明细")
    void testUpdateCostCard_DraftSuccess() {
        Long cardId = 10L;
        CostCardDO existing = CostCardDO.builder()
                .id(cardId)
                .cardNo("CC20260001")
                .status("DRAFT")
                .productId(100L)
                .build();
        when(costCardMapper.selectById(cardId)).thenReturn(existing);
        when(yarnRecipeBomService.getYarnRecipeBomByProductId(100L)).thenReturn(mockValidBom(100L));

        CostCardSaveReqVO req = CostCardSaveReqVO.builder()
                .id(cardId)
                .cardNo("CC20260001")
                .productId(100L)
                .fabricConstruction("KNIT")
                .yarnPriceType("PURCHASE")
                .basis("KG")
                .build();

        costCardService.updateCostCard(req);

        verify(costCardMapper).updateById(any(CostCardDO.class));
        verify(costCardYarnMapper).deleteByCardId(cardId);
        verify(costCardFeeMapper).deleteByCardId(cardId);
    }

    @Test
    @DisplayName("更新成本卡：非草稿状态报 1_030_034_002")
    void testUpdateCostCard_NotDraft_Throws002() {
        Long cardId = 10L;
        CostCardDO existing = CostCardDO.builder()
                .id(cardId)
                .status("EFFECTIVE")
                .build();
        when(costCardMapper.selectById(cardId)).thenReturn(existing);

        CostCardSaveReqVO req = CostCardSaveReqVO.builder().id(cardId).build();

        ServiceException ex = assertThrows(ServiceException.class, () -> costCardService.updateCostCard(req));
        assertEquals(COST_CARD_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("删除成本卡：仅草稿可删，级联删除两子表")
    void testDeleteCostCard_DraftSuccess() {
        Long cardId = 10L;
        CostCardDO card = CostCardDO.builder().id(cardId).status("DRAFT").build();
        when(costCardMapper.selectById(cardId)).thenReturn(card);

        costCardService.deleteCostCard(cardId);

        verify(costCardMapper).deleteById(cardId);
        verify(costCardYarnMapper).deleteByCardId(cardId);
        verify(costCardFeeMapper).deleteByCardId(cardId);
    }

    @Test
    @DisplayName("删除成本卡：生效卡删除报 1_030_034_002")
    void testDeleteCostCard_Effective_Throws002() {
        Long cardId = 10L;
        CostCardDO card = CostCardDO.builder().id(cardId).status("EFFECTIVE").build();
        when(costCardMapper.selectById(cardId)).thenReturn(card);

        ServiceException ex = assertThrows(ServiceException.class, () -> costCardService.deleteCostCard(cardId));
        assertEquals(COST_CARD_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("生效成本卡：同事务顶旧生效卡并成功生效当前卡")
    void testEffectCostCard_Success() {
        Long cardId = 10L;
        Long productId = 100L;
        CostCardDO card = CostCardDO.builder()
                .id(cardId)
                .productId(productId)
                .status("DRAFT")
                .missingPriceCount(0)
                .build();
        when(costCardMapper.selectById(cardId)).thenReturn(card);
        when(yarnRecipeBomService.getYarnRecipeBomByProductId(productId)).thenReturn(mockValidBom(productId));
        when(costCardMapper.effectCardById(eq(cardId), any(), any(), any())).thenReturn(1);

        costCardService.effectCostCard(cardId);

        verify(costCardMapper).expireEffectiveCardsByProductId(eq(productId), any(), any());
        verify(costCardMapper).effectCardById(eq(cardId), any(), any(), any());
    }

    @Test
    @DisplayName("生效成本卡：缺价报错 1_030_034_003")
    void testEffectCostCard_MissingPrices_Throws003() {
        Long cardId = 10L;
        CostCardDO card = CostCardDO.builder()
                .id(cardId)
                .status("DRAFT")
                .missingPriceCount(2)
                .build();
        when(costCardMapper.selectById(cardId)).thenReturn(card);

        ServiceException ex = assertThrows(ServiceException.class, () -> costCardService.effectCostCard(cardId));
        assertEquals(COST_CARD_MISSING_PRICE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("生效成本卡：唯一索引冲突转业务错 1_030_034_002")
    void testEffectCostCard_DuplicateKeyException_Throws002() {
        Long cardId = 10L;
        Long productId = 100L;
        CostCardDO card = CostCardDO.builder()
                .id(cardId)
                .productId(productId)
                .status("DRAFT")
                .missingPriceCount(0)
                .build();
        when(costCardMapper.selectById(cardId)).thenReturn(card);
        when(yarnRecipeBomService.getYarnRecipeBomByProductId(productId)).thenReturn(mockValidBom(productId));
        when(costCardMapper.effectCardById(eq(cardId), any(), any(), any()))
                .thenThrow(new DuplicateKeyException("uk_lbcc_product_effective"));

        ServiceException ex = assertThrows(ServiceException.class, () -> costCardService.effectCostCard(cardId));
        assertEquals(COST_CARD_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("失效成本卡：仅 EFFECTIVE 可失效")
    void testExpireCostCard_Success() {
        Long cardId = 10L;
        CostCardDO card = CostCardDO.builder().id(cardId).status("EFFECTIVE").build();
        when(costCardMapper.selectById(cardId)).thenReturn(card);
        when(costCardMapper.expireCardById(eq(cardId), any(), any())).thenReturn(1);

        costCardService.expireCostCard(cardId);
        verify(costCardMapper).expireCardById(eq(cardId), any(), any());
    }

    @Test
    @DisplayName("复制成本卡：生成新草稿、新卡号、标记 COPY_SNAPSHOT")
    void testCopyCostCard_Success() {
        Long cardId = 10L;
        CostCardDO source = CostCardDO.builder()
                .id(cardId)
                .cardNo("CC20260001")
                .productId(100L)
                .status("EFFECTIVE")
                .build();
        when(costCardMapper.selectById(cardId)).thenReturn(source);
        when(costCardMapper.selectListByCardNoPrefixIncludingDeleted(any())).thenReturn(Collections.emptyList());
        when(costCardMapper.selectByCardNoIncludingDeleted(any())).thenReturn(null);

        CostCardYarnDO y = CostCardYarnDO.builder()
                .yarnSpecId(101L)
                .price(new BigDecimal("30.0000"))
                .build();
        when(costCardYarnMapper.selectListByCardId(cardId)).thenReturn(Collections.singletonList(y));
        when(costCardFeeMapper.selectListByCardId(cardId)).thenReturn(Collections.emptyList());

        doAnswer(inv -> {
            CostCardDO c = inv.getArgument(0);
            c.setId(999L);
            return 1;
        }).when(costCardMapper).insert(any(CostCardDO.class));

        Long newId = costCardService.copyCostCard(cardId);
        assertEquals(999L, newId);

        ArgumentCaptor<List<CostCardYarnDO>> yarnCaptor = ArgumentCaptor.forClass(List.class);
        verify(costCardYarnMapper).insertBatch(yarnCaptor.capture());
        List<CostCardYarnDO> copiedYarns = yarnCaptor.getValue();
        assertEquals(1, copiedYarns.size());
        assertEquals("COPY_SNAPSHOT", copiedYarns.get(0).getPriceSource());
    }

    @Test
    @DisplayName("纯内存试算：返回双口径与明细行贡献")
    void testTrialCostCard_KnitTrial() {
        CostCardTrialReqVO req = new CostCardTrialReqVO();
        req.setFabricConstruction("KNIT");
        req.setBasis("KG");
        req.setTaxRate(new BigDecimal("0.13"));
        req.setGreigeWeightGsm(new BigDecimal("200.00"));
        req.setGreigeWidthCm(new BigDecimal("180.00"));
        req.setYarnItems(Collections.singletonList(
                CostCardYarnItemVO.builder()
                        .yarnSpecId(101L)
                        .yarnSpecName("32S棉")
                        .ratio(new BigDecimal("100.00"))
                        .price(new BigDecimal("30.0000"))
                        .wasteRate(new BigDecimal("5.00"))
                        .build()
        ));
        req.setFeeItems(Collections.singletonList(
                CostCardFeeItemVO.builder()
                        .feeType("WEAVING")
                        .feeUnit("PER_KG")
                        .price(new BigDecimal("5.0000"))
                        .lossRate(BigDecimal.ZERO)
                        .build()
        ));

        CostCardTrialRespVO res = costCardService.trialCostCard(req);
        assertNotNull(res);
        assertEquals("KG", res.getBasis());
        assertEquals(0, res.getMissingPriceCount());
        assertNotNull(res.getYarnCostPerKg());
        assertNotNull(res.getYarnCostPerMeter());
        assertNotNull(res.getUnitCostPerKg());
        assertNotNull(res.getUnitCostPerKgWithTax());
    }

}
