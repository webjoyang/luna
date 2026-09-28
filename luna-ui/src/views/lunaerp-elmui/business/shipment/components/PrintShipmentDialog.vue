<template>
  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="960px"
    top="4vh"
    :close-on-click-modal="false"
    destroy-on-close
    class="shipment-print-dialog"
  >
    <!-- 工具栏：版式切换 + 打印操作 -->
    <div class="flex items-center justify-between mb-16px no-print">
      <div class="flex items-center gap-3">
        <span class="text-14px font-medium text-gray-700">打印版式：</span>
        <el-radio-group v-model="printMode" size="default">
          <el-radio-button value="shipment">
            <Icon icon="ep:document" class="mr-1" /> 随货发货单
          </el-radio-button>
          <el-radio-button value="packing">
            <Icon icon="ep:box" class="mr-1" /> 装箱清单（码单）
          </el-radio-button>
        </el-radio-group>
      </div>

      <div class="flex items-center gap-2">
        <el-button type="primary" :loading="loading" @click="handlePrint">
          <Icon icon="ep:printer" class="mr-1" /> 立即打印
        </el-button>
        <el-button @click="dialogVisible = false">关闭</el-button>
      </div>
    </div>

    <!-- 纸张预览容器（在屏幕上呈现 A4 纸张效果） -->
    <div v-loading="loading" class="print-preview-wrapper bg-gray-100 p-20px rounded overflow-y-auto max-h-70vh">
      <div id="shipmentPrintArea" ref="printAreaRef" class="print-paper bg-white shadow-md mx-auto p-30px text-gray-900">
        <!-- ==================== 1. 发货单版式 ==================== -->
        <template v-if="printMode === 'shipment'">
          <div class="print-header text-center mb-16px">
            <div class="text-22px font-bold tracking-widest text-black mb-4px">销售发货单</div>
            <div class="text-11px text-gray-500 uppercase tracking-wider">随货同行凭单 / DELIVERY NOTE</div>
          </div>

          <!-- 单据核心元数据 -->
          <div class="meta-box mb-12px text-12px border border-gray-800 p-8px bg-gray-50">
            <div class="grid grid-cols-3 gap-y-6px gap-x-12px">
              <div>
                <span class="text-gray-600">发货单号：</span>
                <span class="font-mono font-bold">{{ shipment?.shipmentNo || '—' }}</span>
              </div>
              <div>
                <span class="text-gray-600">发货日期：</span>
                <span class="font-medium">{{ shipment?.shippingDate || '—' }}</span>
              </div>
              <div>
                <span class="text-gray-600">销售合同：</span>
                <span class="font-mono font-medium">{{ shipment?.contractNo || '—' }}</span>
              </div>

              <div>
                <span class="text-gray-600">客户名称：</span>
                <span class="font-bold">{{ shipment?.customerName || '—' }}</span>
              </div>
              <div>
                <span class="text-gray-600">发货仓库：</span>
                <span class="font-medium">{{ shipment?.warehouseName || '—' }}</span>
              </div>
              <div>
                <span class="text-gray-600">业务人员：</span>
                <span>{{ shipment?.salespersonName || '—' }}</span>
              </div>

              <div>
                <span class="text-gray-600">承运物流：</span>
                <span>{{ shipment?.carrier || '—' }}</span>
              </div>
              <div class="col-span-2">
                <span class="text-gray-600">物流运单号：</span>
                <span class="font-mono">{{ shipment?.trackingNumber || '—' }}</span>
              </div>

              <div class="col-span-3">
                <span class="text-gray-600">发货地址：</span>
                <span>{{ shipment?.shippingAddress || '—' }}</span>
              </div>
              <div class="col-span-3">
                <span class="text-gray-600">收货地址：</span>
                <span class="font-medium">{{ shipment?.receiveAddress || '—' }}</span>
              </div>
            </div>
          </div>

          <!-- 明细表格 -->
          <table class="print-table w-full text-11px border-collapse mb-12px">
            <thead>
              <tr class="bg-gray-100 font-bold">
                <th class="w-32px text-center">#</th>
                <th class="text-left">产品名称</th>
                <th class="w-85px text-left">产品编码</th>
                <th class="w-75px text-left">客户色号</th>
                <th class="w-65px text-left">颜色</th>
                <th class="w-75px text-left">缸号/批次</th>
                <th class="w-75px text-right">发货数量</th>
                <th class="w-45px text-center">单位</th>
                <th class="w-50px text-right">匹数</th>
                <th class="w-65px text-right">单价(元)</th>
                <th class="w-80px text-right">金额(元)</th>
                <th class="w-80px text-left">备注</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(item, idx) in shipment?.items || []" :key="item.id || idx">
                <td class="text-center">{{ idx + 1 }}</td>
                <td class="font-medium">{{ item.productName || '—' }}</td>
                <td class="font-mono">{{ item.productCode || '—' }}</td>
                <td>{{ item.customerColorNo || '—' }}</td>
                <td>{{ item.colorName || '—' }}</td>
                <td class="font-mono">{{ item.vatNo || '—' }}</td>
                <td class="text-right font-mono font-medium">{{ item.quantity != null ? item.quantity : '—' }}</td>
                <td class="text-center">{{ item.unit || '—' }}</td>
                <td class="text-right font-mono">{{ item.pieces != null ? `${item.pieces}` : '—' }}</td>
                <td class="text-right font-mono">{{ item.unitPrice != null ? Number(item.unitPrice).toFixed(2) : '—' }}</td>
                <td class="text-right font-mono font-bold">{{ item.amount != null ? Number(item.amount).toFixed(2) : '0.00' }}</td>
                <td>{{ item.remark || '' }}</td>
              </tr>
              <tr v-if="!shipment?.items?.length">
                <td colspan="12" class="text-center py-12px text-gray-400">暂无明细数据</td>
              </tr>
            </tbody>
            <tfoot>
              <tr class="bg-gray-50 font-bold border-t-2 border-gray-800">
                <td colspan="6" class="text-center">合计</td>
                <td class="text-right font-mono">{{ totalQuantityText }}</td>
                <td></td>
                <td class="text-right font-mono">{{ totalPiecesCount }}</td>
                <td></td>
                <td class="text-right font-mono text-12px">¥ {{ (shipment?.totalAmount || itemsTotalAmount).toFixed(2) }}</td>
                <td></td>
              </tr>
            </tfoot>
          </table>

          <!-- 费用与大写金额汇总栏 -->
          <div class="summary-box border border-gray-800 p-8px mb-16px text-12px bg-gray-50">
            <div class="flex justify-between items-center mb-6px">
              <div>
                <span class="text-gray-600">总匹数：</span>
                <span class="font-bold mr-16px">{{ totalPiecesCount }} 匹</span>
                <span class="text-gray-600">货物金额：</span>
                <span class="font-bold mr-16px">¥ {{ (shipment?.totalAmount || itemsTotalAmount).toFixed(2) }}</span>
                <span class="text-gray-600">运费：</span>
                <span class="font-bold mr-16px">¥ {{ Number(shipment?.freightFee || 0).toFixed(2) }}</span>
                <span class="text-gray-600">总计金额（含运费）：</span>
                <span class="font-bold text-14px text-blue-700">¥ {{ grandTotalAmount.toFixed(2) }}</span>
              </div>
            </div>
            <div class="text-12px">
              <span class="text-gray-600">金额大写：</span>
              <span class="font-bold tracking-wider">{{ uppercaseAmount }}</span>
            </div>
            <div v-if="shipment?.remark" class="text-11px text-gray-600 mt-4px">
              <span>单据备注：</span>{{ shipment.remark }}
            </div>
          </div>

          <!-- 签字留痕栏 -->
          <div class="sign-block mt-24px text-12px">
            <div class="grid grid-cols-4 gap-12px pt-10px">
              <div>
                <span class="text-gray-600">制单人：</span>
                <span class="font-medium underline decoration-gray-400 underline-offset-4">{{ shipment?.creator || '系统' }}</span>
              </div>
              <div>
                <span class="text-gray-600">发货仓管：</span>
                <span class="inline-block w-100px border-b border-gray-700"></span>
              </div>
              <div>
                <span class="text-gray-600">承运司机：</span>
                <span class="inline-block w-100px border-b border-gray-700"></span>
              </div>
              <div>
                <span class="text-gray-600">客户签收：</span>
                <span class="inline-block w-100px border-b border-gray-700"></span>
              </div>
            </div>
            <div class="flex justify-between items-center text-10px text-gray-500 mt-20px pt-6px border-t border-dashed border-gray-300">
              <div>签收日期：_____年____月____日</div>
              <div>随货同行白联：客户留存 / 红联：财务记账 / 黄联：仓库留存</div>
              <div>打印时间：{{ printTimeStr }}</div>
            </div>
          </div>
        </template>

        <!-- ==================== 2. 装箱清单（码单）版式 ==================== -->
        <template v-else>
          <div class="print-header text-center mb-16px">
            <div class="text-22px font-bold tracking-widest text-black mb-4px">发货装箱清单</div>
            <div class="text-11px text-gray-500 uppercase tracking-wider">细码清单 / PACKING LIST</div>
          </div>

          <!-- 装箱清单元数据 -->
          <div class="meta-box mb-12px text-12px border border-gray-800 p-8px bg-gray-50">
            <div class="grid grid-cols-3 gap-y-6px gap-x-12px">
              <div>
                <span class="text-gray-600">发货单号：</span>
                <span class="font-mono font-bold">{{ shipment?.shipmentNo || '—' }}</span>
              </div>
              <div>
                <span class="text-gray-600">发货日期：</span>
                <span class="font-medium">{{ shipment?.shippingDate || '—' }}</span>
              </div>
              <div>
                <span class="text-gray-600">销售合同：</span>
                <span class="font-mono font-medium">{{ shipment?.contractNo || '—' }}</span>
              </div>

              <div>
                <span class="text-gray-600">客户名称：</span>
                <span class="font-bold">{{ shipment?.customerName || '—' }}</span>
              </div>
              <div>
                <span class="text-gray-600">发货仓库：</span>
                <span class="font-medium">{{ shipment?.warehouseName || '—' }}</span>
              </div>
              <div>
                <span class="text-gray-600">承运商/单号：</span>
                <span>{{ shipment?.carrier || '—' }} {{ shipment?.trackingNumber ? `(${shipment.trackingNumber})` : '' }}</span>
              </div>
            </div>
          </div>

          <!-- 逐卷细码表格 -->
          <table class="print-table w-full text-11px border-collapse mb-12px">
            <thead>
              <tr class="bg-gray-100 font-bold">
                <th class="w-40px text-center">#</th>
                <th class="w-110px text-left">卷号</th>
                <th class="text-left">所属产品</th>
                <th class="w-80px text-left">客户色号</th>
                <th class="w-80px text-left">缸号/批次</th>
                <th class="w-85px text-right">重量 (KG)</th>
                <th class="w-85px text-right">米数 (M)</th>
                <th class="w-110px text-left">备注</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(roll, idx) in flatRollList" :key="idx">
                <td class="text-center">{{ idx + 1 }}</td>
                <td class="font-mono font-bold">{{ roll.rollNo }}</td>
                <td>{{ roll.productName }} <span v-if="roll.productCode" class="text-gray-400">({{ roll.productCode }})</span></td>
                <td>{{ roll.customerColorNo || '—' }}</td>
                <td class="font-mono">{{ roll.vatNo || '—' }}</td>
                <td class="text-right font-mono font-medium">{{ roll.weight != null ? Number(roll.weight).toFixed(2) : '—' }}</td>
                <td class="text-right font-mono">{{ roll.lengthM != null ? Number(roll.lengthM).toFixed(2) : '—' }}</td>
                <td>{{ roll.remark || '' }}</td>
              </tr>
              <tr v-if="!flatRollList.length">
                <td colspan="8" class="text-center py-16px text-gray-400">
                  发货单明细未录入细卷清单数据
                </td>
              </tr>
            </tbody>
            <tfoot>
              <tr class="bg-gray-50 font-bold border-t-2 border-gray-800">
                <td colspan="5" class="text-center">合计</td>
                <td class="text-right font-mono">{{ totalRollsWeight.toFixed(2) }}</td>
                <td class="text-right font-mono">{{ totalRollsLength.toFixed(2) }}</td>
                <td>共 {{ flatRollList.length }} 卷</td>
              </tr>
            </tfoot>
          </table>

          <!-- 汇总信息 -->
          <div class="summary-box border border-gray-800 p-8px mb-16px text-12px bg-gray-50 flex justify-between items-center">
            <div>
              <span class="text-gray-600">装箱总卷数：</span>
              <span class="font-bold mr-20px">{{ flatRollList.length }} 卷</span>
              <span class="text-gray-600">累计总重量：</span>
              <span class="font-bold mr-20px">{{ totalRollsWeight.toFixed(2) }} KG</span>
              <span class="text-gray-600">累计总米数：</span>
              <span class="font-bold">{{ totalRollsLength.toFixed(2) }} M</span>
            </div>
            <div class="text-gray-500 text-11px">
              细码数据均取自出库卷清单登记
            </div>
          </div>

          <!-- 签字留痕栏 -->
          <div class="sign-block mt-24px text-12px">
            <div class="grid grid-cols-4 gap-12px pt-10px">
              <div>
                <span class="text-gray-600">装箱/打包：</span>
                <span class="inline-block w-100px border-b border-gray-700"></span>
              </div>
              <div>
                <span class="text-gray-600">复核人：</span>
                <span class="inline-block w-100px border-b border-gray-700"></span>
              </div>
              <div>
                <span class="text-gray-600">仓库发货：</span>
                <span class="inline-block w-100px border-b border-gray-700"></span>
              </div>
              <div>
                <span class="text-gray-600">客户验收入库：</span>
                <span class="inline-block w-100px border-b border-gray-700"></span>
              </div>
            </div>
            <div class="flex justify-between items-center text-10px text-gray-500 mt-20px pt-6px border-t border-dashed border-gray-300">
              <div>验收日期：_____年____月____日</div>
              <div>请当面验视包裹外观及卷数，确认无误后签字归档</div>
              <div>打印时间：{{ printTimeStr }}</div>
            </div>
          </div>
        </template>
      </div>
    </div>
  </el-dialog>
</template>

<script lang="ts" setup>
import { ref, computed } from 'vue'
import { useMessage } from '@/hooks/web/useMessage'
import * as ShipmentApi from '@/api/lunaerp-elmui/business/shipment'
import type { ShipmentVO } from '@/api/lunaerp-elmui/business/shipment'

defineOptions({ name: 'PrintShipmentDialog' })

const message = useMessage()

const dialogVisible = ref(false)
const loading = ref(false)
const printMode = ref<'shipment' | 'packing'>('shipment')
const shipment = ref<ShipmentVO | null>(null)
const printIframeRef = ref<HTMLIFrameElement | null>(null)

const dialogTitle = computed(() => {
  const no = shipment.value?.shipmentNo ? ` - ${shipment.value.shipmentNo}` : ''
  return printMode.value === 'shipment' ? `打印发货单${no}` : `打印装箱清单${no}`
})

const printTimeStr = computed(() => {
  const d = new Date()
  const pad = (n: number) => (n < 10 ? `0${n}` : `${n}`)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
})

// 金额与匹数计算
const itemsTotalAmount = computed(() => {
  const items = shipment.value?.items || []
  return items.reduce((acc, cur) => acc + (cur.amount || 0), 0)
})

const grandTotalAmount = computed(() => {
  const total = shipment.value?.totalAmount != null ? shipment.value.totalAmount : itemsTotalAmount.value
  const freight = shipment.value?.freightFee || 0
  return total + freight
})

const totalPiecesCount = computed(() => {
  if (shipment.value?.totalPieces != null) {
    return shipment.value.totalPieces
  }
  const items = shipment.value?.items || []
  return items.reduce((acc, cur) => acc + (cur.pieces || 0), 0)
})

const totalQuantityText = computed(() => {
  const items = shipment.value?.items || []
  if (!items.length) return '0'
  const unitMap: Record<string, number> = {}
  for (const item of items) {
    const itemUnit = item.unit || 'KG'
    unitMap[itemUnit] = (unitMap[itemUnit] || 0) + (item.quantity || 0)
  }
  return Object.entries(unitMap)
    .map(([unit, quantity]) => `${quantity.toFixed(2)} ${unit}`)
    .join(' / ')
})

// 人民币金额大写转换（采用整分计算，杜绝浮点精度丢失）
const uppercaseAmount = computed(() => {
  const totalAmount = grandTotalAmount.value
  if (totalAmount == null || isNaN(totalAmount) || totalAmount === 0) return '零元整'
  const digits = ['零', '壹', '贰', '叁', '肆', '伍', '陆', '柒', '捌', '玖']
  const bigUnits = [
    ['元', '万', '亿'],
    ['', '拾', '佰', '仟']
  ]
  const prefix = totalAmount < 0 ? '负' : ''
  const totalCents = Math.round(Math.abs(totalAmount) * 100)
  const integerPart = Math.floor(totalCents / 100)
  const jiao = Math.floor(totalCents / 10) % 10
  const fen = totalCents % 10

  let fractionStr = ''
  if (jiao === 0 && fen === 0) {
    fractionStr = '整'
  } else {
    fractionStr =
      (jiao > 0 ? digits[jiao] + '角' : integerPart > 0 ? '零' : '') +
      (fen > 0 ? digits[fen] + '分' : '')
  }

  let integerStr = ''
  let remainingInteger = integerPart
  for (let i = 0; i < bigUnits[0].length && remainingInteger > 0; i++) {
    let sectionStr = ''
    for (let j = 0; j < bigUnits[1].length && remainingInteger > 0; j++) {
      sectionStr = digits[remainingInteger % 10] + bigUnits[1][j] + sectionStr
      remainingInteger = Math.floor(remainingInteger / 10)
    }
    integerStr =
      sectionStr.replace(/(零.)*零$/, '').replace(/^$/, '零') + bigUnits[0][i] + integerStr
  }

  const result =
    prefix +
    integerStr.replace(/(零.)*零元/, '元').replace(/(零.)+/g, '零').replace(/^整$/, '零元整') +
    fractionStr
  return result || '零元整'
})

// 装箱清单（扁平化卷清单）
interface FlatRoll {
  rollNo: string
  productName: string
  productCode: string
  customerColorNo?: string
  vatNo?: string
  weight?: number
  lengthM?: number
  remark?: string
}

const flatRollList = computed<FlatRoll[]>(() => {
  const items = shipment.value?.items || []
  const list: FlatRoll[] = []
  for (const item of items) {
    if (item.rolls && item.rolls.length > 0) {
      for (const roll of item.rolls) {
        list.push({
          rollNo: roll.rollNo || '—',
          productName: item.productName || '—',
          productCode: item.productCode || '',
          customerColorNo: item.customerColorNo,
          vatNo: item.vatNo,
          weight: roll.weight,
          lengthM: roll.lengthM,
          remark: roll.remark || item.remark
        })
      }
    } else {
      // 若明细未录入细卷，以整批一行呈现
      list.push({
        rollNo: `整批发货 (${item.pieces || 1}匹)`,
        productName: item.productName || '—',
        productCode: item.productCode || '',
        customerColorNo: item.customerColorNo,
        vatNo: item.vatNo,
        weight: item.quantity,
        lengthM: undefined,
        remark: item.remark ? `明细备注：${item.remark}` : '未细分单卷'
      })
    }
  }
  return list
})

const totalRollsWeight = computed(() => {
  return flatRollList.value.reduce((acc, cur) => acc + (cur.weight || 0), 0)
})

const totalRollsLength = computed(() => {
  return flatRollList.value.reduce((acc, cur) => acc + (cur.lengthM || 0), 0)
})

/** 打开弹窗并载入发货单数据 */
const open = async (target: ShipmentVO | number, mode: 'shipment' | 'packing' = 'shipment') => {
  printMode.value = mode
  dialogVisible.value = true
  loading.value = true

  try {
    if (typeof target === 'number') {
      const res = await ShipmentApi.getShipment(target)
      shipment.value = res
    } else if (target && target.id) {
      // 若传入对象无 items 或 rolls，则重新获取完整数据
      if (!target.items || target.items.length === 0) {
        const res = await ShipmentApi.getShipment(target.id)
        shipment.value = res
      } else {
        shipment.value = target
      }
    } else {
      shipment.value = target
    }
  } catch (e: any) {
    message.error('加载发货单详情失败')
  } finally {
    loading.value = false
  }
}

/** 触发独立 iframe 打印（纯前端、不干扰宿主页面） */
const handlePrint = async () => {
  const printArea = document.getElementById('shipmentPrintArea')
  if (!printArea) {
    message.warning('打印区域内容未就绪')
    return
  }

  const title = printMode.value === 'shipment'
    ? `销售发货单_${shipment.value?.shipmentNo || ''}`
    : `发货装箱清单_${shipment.value?.shipmentNo || ''}`

  const printHtml = `
    <!DOCTYPE html>
    <html>
      <head>
        <meta charset="utf-8" />
        <title>${title}</title>
        <style>
          @page {
            size: portrait;
            margin: 10mm;
          }
          * {
            box-sizing: border-box;
            -webkit-print-color-adjust: exact;
            print-color-adjust: exact;
          }
          body {
            margin: 0;
            padding: 0;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "PingFang SC", "Microsoft YaHei", sans-serif;
            color: #000;
            background: #fff;
            font-size: 11px;
            line-height: 1.4;
          }
          .print-paper {
            width: 100%;
            margin: 0 auto;
          }
          .print-header {
            text-align: center;
            margin-bottom: 12px;
          }
          .print-header .font-bold {
            font-size: 20px;
            font-weight: bold;
            letter-spacing: 2px;
            margin-bottom: 2px;
          }
          .print-header .uppercase {
            font-size: 10px;
            color: #666;
            letter-spacing: 1px;
          }
          .meta-box {
            border: 1px solid #000;
            padding: 6px 10px;
            margin-bottom: 10px;
            background: #fafafa;
            font-size: 11px;
          }
          .grid {
            display: grid;
          }
          .grid-cols-3 {
            grid-template-columns: repeat(3, 1fr);
          }
          .grid-cols-4 {
            grid-template-columns: repeat(4, 1fr);
          }
          .col-span-2 { grid-column: span 2; }
          .col-span-3 { grid-column: span 3; }
          .gap-y-6px { row-gap: 5px; }
          .gap-x-12px { column-gap: 10px; }
          .gap-12px { gap: 10px; }
          .text-gray-600 { color: #555; }
          .font-mono { font-family: monospace, Courier, sans-serif; }
          .font-bold { font-weight: bold; }
          .font-medium { font-weight: 500; }
          .print-table {
            width: 100%;
            border-collapse: collapse;
            font-size: 10.5px;
            margin-bottom: 10px;
            page-break-inside: auto;
          }
          .print-table tr {
            page-break-inside: avoid;
            page-break-after: auto;
          }
          .print-table th, .print-table td {
            border: 1px solid #000;
            padding: 4px 6px;
            line-height: 1.3;
          }
          .print-table th {
            background-color: #f0f0f0 !important;
            font-weight: bold;
          }
          .print-table tfoot td {
            font-weight: bold;
            background-color: #fafafa !important;
          }
          .text-center { text-align: center; }
          .text-left { text-align: left; }
          .text-right { text-align: right; }
          .summary-box {
            border: 1px solid #000;
            padding: 6px 10px;
            margin-bottom: 12px;
            background: #fafafa;
            font-size: 11px;
          }
          .flex { display: flex; }
          .justify-between { justify-content: space-between; }
          .items-center { align-items: center; }
          .sign-block {
            margin-top: 18px;
            page-break-inside: avoid;
            font-size: 11px;
          }
          .border-b { border-bottom: 1px solid #000; }
          .inline-block { display: inline-block; }
          .w-100px { width: 100px; }
          .border-t { border-top: 1px solid #000; }
          .border-dashed { border-style: dashed; }
          .border-gray-300 { border-color: #aaa; }
          .text-10px { font-size: 9.5px; }
          .text-gray-500 { color: #666; }
          .pt-6px { padding-top: 5px; }
          .mt-20px { margin-top: 16px; }
        </style>
      </head>
      <body>
        <div class="print-paper">
          ${printArea.innerHTML}
        </div>
      </body>
    </html>
  `

  let iframe = printIframeRef.value
  if (!iframe) {
    iframe = document.createElement('iframe')
    iframe.style.position = 'fixed'
    iframe.style.right = '0'
    iframe.style.bottom = '0'
    iframe.style.width = '0'
    iframe.style.height = '0'
    iframe.style.border = '0'
    document.body.appendChild(iframe)
    printIframeRef.value = iframe
  }

  const doc = iframe.contentDocument || iframe.contentWindow?.document
  if (!doc) return
  doc.open()
  doc.write(printHtml)
  doc.close()

  await doc.fonts?.ready
  iframe.contentWindow?.focus()
  iframe.contentWindow?.print()
}

defineExpose({ open })
</script>

<style scoped>
.print-paper {
  width: 100%;
  max-width: 820px;
  min-height: 500px;
}

.print-table th,
.print-table td {
  border: 1px solid #333;
  padding: 4px 6px;
}
</style>
