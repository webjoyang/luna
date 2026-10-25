<template>
  <el-form
    ref="formRef"
    :model="formData"
    label-width="120px"
    class="py-16px max-w-1100px"
    :disabled="isReadOnly"
  >
    <!-- 1. 卖方信息（Header） -->
    <el-card shadow="never" class="!mb-16px">
      <template #header>
        <div class="flex justify-between items-center">
          <span class="font-bold text-14px">卖方出口资料 (Seller Information)</span>
          <el-button
            v-if="!isReadOnly"
            type="primary"
            link
            @click="fillSellerDefault"
          >
            使用租户默认配置
          </el-button>
        </div>
      </template>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="卖方英文名称" prop="sellerName">
            <el-input
              v-model="formData.sellerName"
              placeholder="请输入卖方英文抬头"
              maxlength="256"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="卖方英文地址" prop="sellerAddress">
            <el-input
              v-model="formData.sellerAddress"
              placeholder="请输入卖方英文地址"
              maxlength="512"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="24">
          <el-form-item label="收款银行信息" prop="sellerBankInfo">
            <el-input
              v-model="formData.sellerBankInfo"
              placeholder="银行名、账号、SWIFT 码等汇总信息（可在单证专属页签覆盖）"
              maxlength="512"
            />
          </el-form-item>
        </el-col>
      </el-row>
    </el-card>

    <!-- 2. 买方信息 (Buyer) -->
    <el-card shadow="never" class="!mb-16px">
      <template #header>
        <span class="font-bold text-14px">买方客户信息 (Buyer Information)</span>
      </template>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="客户名称" prop="customerId" required>
            <el-input
              v-if="isReadOnly || !formData.customerId"
              v-model="formData.customerName"
              placeholder="客户名称"
              :disabled="true"
            />
            <span v-else class="font-bold">{{ formData.customerName || '已选客户' }}</span>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="买方英文名称" prop="buyerName">
            <el-input
              v-model="formData.buyerName"
              placeholder="请输入买方英文名称（To: Messrs）"
              maxlength="256"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="买方英文地址" prop="buyerAddress">
            <el-input
              v-model="formData.buyerAddress"
              type="textarea"
              :rows="2"
              placeholder="买方英文详细地址"
              maxlength="512"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="买方银行信息" prop="buyerBankInfo">
            <el-input
              v-model="formData.buyerBankInfo"
              type="textarea"
              :rows="2"
              placeholder="买方开户行信息（如有）"
              maxlength="512"
            />
          </el-form-item>
        </el-col>
      </el-row>
    </el-card>

    <!-- 3. 贸易与航运条款 -->
    <el-card shadow="never" class="!mb-16px">
      <template #header>
        <span class="font-bold text-14px">贸易与航运条款 (Terms & Shipping)</span>
      </template>

      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="关联合同号" prop="contractNo">
            <el-input v-model="formData.contractNo" placeholder="销售合同号" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="关联发货单号" prop="shipmentNo">
            <el-input v-model="formData.shipmentNo" placeholder="发货单号" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="币种" prop="currency" required>
            <el-select v-model="formData.currency" class="!w-full">
              <el-option label="USD 美元" value="USD" />
              <el-option label="EUR 欧元" value="EUR" />
              <el-option label="RMB 人民币" value="RMB" />
              <el-option label="GBP 英镑" value="GBP" />
              <el-option label="HKD 港币" value="HKD" />
              <el-option label="JPY 日元" value="JPY" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="贸易术语" prop="incoterm">
            <el-select v-model="formData.incoterm" placeholder="选择术语" clearable class="!w-full">
              <el-option label="FOB" value="FOB" />
              <el-option label="CIF" value="CIF" />
              <el-option label="CFR" value="CFR" />
              <el-option label="EXW" value="EXW" />
              <el-option label="DDP" value="DDP" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="起运港" prop="portOfLoading">
            <el-input v-model="formData.portOfLoading" placeholder="例如：SHANGHAI" maxlength="64" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="目的港" prop="portOfDischarge">
            <el-input v-model="formData.portOfDischarge" placeholder="例如：LOS ANGELES" maxlength="64" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="目的国" prop="countryOfDest">
            <el-input v-model="formData.countryOfDest" placeholder="例如：USA" maxlength="64" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="运输方式" prop="transportMode">
            <el-select v-model="formData.transportMode" placeholder="运输方式" clearable class="!w-full">
              <el-option label="SEA 海运" value="SEA" />
              <el-option label="AIR 空运" value="AIR" />
              <el-option label="LAND 陆运" value="LAND" />
              <el-option label="EXPRESS 快递" value="EXPRESS" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="付款方式" prop="paymentTerm">
            <el-select v-model="formData.paymentTerm" placeholder="付款方式" clearable class="!w-full">
              <el-option label="TT 电汇" value="TT" />
              <el-option label="LC 信用证" value="LC" />
              <el-option label="DP 付款交单" value="DP" />
              <el-option label="DA 承兑交单" value="DA" />
              <el-option label="OA 赊销" value="OA" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
    </el-card>

    <!-- 4. 唛头与整单备注 -->
    <el-card shadow="never">
      <template #header>
        <span class="font-bold text-14px">唛头与备注 (Shipping Marks & Remarks)</span>
      </template>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="唛头 (Mark)" prop="shippingMark">
            <el-input
              v-model="formData.shippingMark"
              type="textarea"
              :rows="3"
              placeholder="请输入箱唛头文字，为空时模板默认显示 N/M"
              maxlength="500"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="整单备注" prop="remarks">
            <el-input
              v-model="formData.remarks"
              type="textarea"
              :rows="3"
              placeholder="单据内部或打印补充备注"
              maxlength="1000"
            />
          </el-form-item>
        </el-col>
      </el-row>
    </el-card>
  </el-form>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import type { ExportDocVO } from '@/api/lunaerp-elmui/business/export-doc'
import { getSellerConfig } from '@/api/lunaerp-elmui/business/export-doc'

defineOptions({ name: 'ExportDocBasicInfoTab' })

const props = defineProps<{
  formData: ExportDocVO
  isReadOnly: boolean
}>()

/** 一键填充租户默认卖方资料 */
const fillSellerDefault = async () => {
  try {
    const config = await getSellerConfig()
    if (!config || !config.sellerName) {
      ElMessage.warning('尚未配置租户卖方出口资料，请在列表页右上角配置')
      return
    }
    props.formData.sellerName = config.sellerName
    if (config.sellerAddress) {
      props.formData.sellerAddress = config.sellerAddress
    }
    const bankParts = [
      config.bankName,
      config.bankAccount ? `A/C: ${config.bankAccount}` : '',
      config.bankSwift ? `SWIFT: ${config.bankSwift}` : '',
      config.bankAddress
    ].filter(Boolean)
    if (bankParts.length > 0) {
      props.formData.sellerBankInfo = bankParts.join(' ')
    }
    ElMessage.success('已填入租户默认卖方出口资料')
  } catch (err: any) {
    ElMessage.error(err?.message || '获取配置失败')
  }
}
</script>
