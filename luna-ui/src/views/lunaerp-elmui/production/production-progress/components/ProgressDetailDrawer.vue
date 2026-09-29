<template>
  <el-drawer
    v-model="visible"
    :title="`生产进度 - ${detail?.contractNo || ''}`"
    size="900px"
    direction="rtl"
    destroy-on-close
    class="progress-detail-drawer"
  >
    <div v-loading="loading" class="drawer-content">
      <template v-if="detail">
        <!-- 1. 头部：合同信息 + 预警标签 -->
        <div class="contract-header">
          <el-descriptions :column="3" border size="small">
            <el-descriptions-item label="合同号">
              <span class="contract-no">{{ detail.contractNo || '-' }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="客户">
              {{ detail.customerName || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="业务员">
              {{ detail.salespersonName || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="交货日期">
              <span :class="deliveryClass">
                {{ detail.deliveryDate || '未设定' }}
                <span v-if="remainingDaysLabel" class="sub-text">（{{ remainingDaysLabel }}）</span>
              </span>
            </el-descriptions-item>
            <el-descriptions-item label="合同状态">
              <DictTag
                :type="DICT_TYPE.LUNAERP_SALES_CONTRACT_STATUS"
                :value="detail.status || ''"
              />
            </el-descriptions-item>
            <el-descriptions-item label="计量单位">
              {{ detail.unit || 'KG' }}
            </el-descriptions-item>
          </el-descriptions>

          <div class="alert-bar">
            <template v-if="alerts.length > 0">
              <el-tag
                v-for="alert in alerts"
                :key="alert"
                :type="alertTagType(alert)"
                effect="dark"
                size="small"
              >
                <Icon :icon="alert === '临期' ? 'ep:alarm-clock' : 'ep:warning'" class="mr-2px" />
                {{ alert }}
              </el-tag>
            </template>
            <el-tag v-else type="success" effect="plain" size="small">
              <Icon icon="ep:circle-check" class="mr-2px" />无预警
            </el-tag>
            <el-button class="ml-auto" size="small" text type="primary" @click="loadDetail">
              <Icon icon="ep:refresh" class="mr-2px" />刷新
            </el-button>
          </div>

          <!-- M 单位合同：明细原样展示但不进 KG 合计（ADR-0034 单位口径） -->
          <el-alert
            v-if="isMeterUnit"
            type="info"
            :closable="false"
            show-icon
            class="mt-8px"
            title="该合同按 M 计量：合同需求与未发量在 KG 口径下无定义（不做克重折算），其余环节数值为 KG 流水与执行单据的真实聚合"
          />
        </div>

        <!-- 2. 六环节分区卡片（进度对照） -->
        <div class="section-grid">
          <div v-for="section in sections" :key="section.key" class="section-card">
            <div class="section-head">
              <Icon :icon="section.icon" class="section-icon" />
              <span class="section-title">{{ section.title }}</span>
            </div>

            <div class="metric-list">
              <div v-for="metric in section.metrics" :key="metric.label" class="metric-item">
                <span class="metric-label">{{ metric.label }}</span>
                <span class="metric-value" :class="{ 'is-emphasize': metric.emphasize }">
                  {{ metric.value }}
                </span>
              </div>
            </div>

            <div v-if="section.bars?.length" class="bar-list">
              <div v-for="bar in section.bars" :key="bar.label" class="bar-item">
                <div class="bar-head">
                  <span class="bar-label">{{ bar.label }}</span>
                  <span class="bar-value">{{ bar.value }}</span>
                </div>
                <el-progress
                  :percentage="bar.percent"
                  :stroke-width="8"
                  :color="bar.color"
                  :show-text="true"
                />
              </div>
            </div>

            <div v-if="section.tags?.length" class="tag-list">
              <el-tag
                v-for="tag in section.tags"
                :key="tag.label"
                :type="tag.type"
                size="small"
                effect="light"
              >
                {{ tag.label }} {{ tag.count }}
              </el-tag>
            </div>

            <div v-if="section.empty" class="section-empty">{{ section.empty }}</div>
          </div>
        </div>

        <!-- 3. 下钻：发染记录列表（口径 = 胚布环节已发染量） -->
        <div class="drill-section">
          <div class="drill-head">
            <span class="drill-title">发染记录下钻</span>
            <span class="drill-tip">
              合计 {{ formatKg(dispatchTotalKg) }} KG，与「胚布环节-已发染」同源同值
            </span>
          </div>
          <el-table :data="dispatchRecords" border stripe size="small">
            <el-table-column label="发染日期" prop="dispatchDate" width="105" align="center" />
            <el-table-column label="产品" min-width="150" show-overflow-tooltip>
              <template #default="{ row }">
                {{ row.productName || '-' }}
                <span v-if="row.productCode" class="sub-text">({{ row.productCode }})</span>
              </template>
            </el-table-column>
            <el-table-column
              label="出库仓库"
              prop="warehouseName"
              min-width="110"
              show-overflow-tooltip
            />
            <el-table-column label="数量(KG)" prop="quantity" width="105" align="right">
              <template #default="{ row }">{{ formatKg(row.quantity) }}</template>
            </el-table-column>
            <el-table-column label="匹数" prop="pieces" width="70" align="center">
              <template #default="{ row }">{{ row.pieces ?? '-' }}</template>
            </el-table-column>
            <el-table-column
              label="承染染厂"
              prop="dyeingFactoryName"
              min-width="130"
              show-overflow-tooltip
            >
              <template #default="{ row }">{{ row.dyeingFactoryName || '未指定' }}</template>
            </el-table-column>
            <el-table-column label="染色单" width="90" align="center">
              <template #default="{ row }">
                {{ row.dyeingOrderId ? `#${row.dyeingOrderId}` : '未补填' }}
              </template>
            </el-table-column>
            <el-table-column label="备注" prop="remark" min-width="120" show-overflow-tooltip>
              <template #default="{ row }">{{ row.remark || '-' }}</template>
            </el-table-column>
            <template #empty>
              <el-empty description="该合同暂无发染记录" :image-size="70" />
            </template>
          </el-table>
        </div>
      </template>

      <el-empty v-else-if="!loading" description="暂无进度数据" :image-size="100" />
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { DICT_TYPE, getDictObj } from '@/utils/dict'
import {
  getProductionProgress,
  type ProductionProgressDetailVO
} from '@/api/lunaerp-elmui/production/production-progress'
import type { GreigeDispatchRecordVO } from '@/api/lunaerp-elmui/production/greige-dispatch-record'
import {
  alertTagType,
  deliveryDateClass,
  formatKg,
  remainingDaysText,
  type ProgressTagType
} from '../utils'

defineOptions({ name: 'LunaerpElmuiProductionProgressDetailDrawer' })

interface SectionMetric {
  label: string
  value: string
  emphasize?: boolean
}

interface SectionBar {
  label: string
  value: string
  percent: number
  color?: string
}

interface SectionTag {
  label: string
  count: number
  type: ProgressTagType
}

interface SectionCard {
  key: string
  title: string
  icon: string
  metrics: SectionMetric[]
  bars?: SectionBar[]
  tags?: SectionTag[]
  empty?: string
}

const visible = ref(false)
const loading = ref(false)
const contractId = ref<number | null>(null)
const detail = ref<ProductionProgressDetailVO | null>(null)

/** 占比：分母为 0 时返回 0（M 单位合同与无需求合同不显示假进度） */
const ratio = (part?: number, whole?: number) => {
  const denominator = Number(whole ?? 0)
  if (denominator <= 0) return 0
  const value = (Number(part ?? 0) / denominator) * 100
  return Math.max(0, Math.min(100, Math.round(value * 10) / 10))
}

const isMeterUnit = computed(() => detail.value?.unit === 'M')
const alerts = computed(() => detail.value?.alerts || [])

/** 交期着色与剩余天数表述：与列表行共用 utils 口径 */
const deliveryClass = computed(() => deliveryDateClass(detail.value?.deliveryDate))
const remainingDaysLabel = computed(() => remainingDaysText(detail.value?.deliveryDate))

/** 取字典配色，缺配时退化为 info */
const dictTagType = (dictType: string, value: string): ProgressTagType => {
  const colorType = getDictObj(dictType, value)?.colorType
  return (colorType || 'info') as ProgressTagType
}

/** 字典汇总（缸状态 / 检验等级）→ 标签列表，无数据不产生条目 */
const toTags = (summary: Record<string, number> | undefined, dictType: string): SectionTag[] => {
  return Object.entries(summary || {})
    .filter(([, count]) => Number(count) > 0)
    .map(([value, count]) => ({
      label: getDictObj(dictType, value)?.label || value,
      count: Number(count),
      type: dictTagType(dictType, value)
    }))
}

const dispatchRecords = computed<GreigeDispatchRecordVO[]>(
  () => detail.value?.greigeDispatchRecords || []
)

/** 发染记录合计：与后端 sentToDyeingKg 同源，前端合计仅供口径核对 */
const dispatchTotalKg = computed(() =>
  dispatchRecords.value.reduce((sum, record) => sum + (Number(record.quantity) || 0), 0)
)

/** 六环节分区卡片（胚布 / 织造 / 染色 / 检验 / 入库 / 出货） */
const sections = computed<SectionCard[]>(() => {
  const data = detail.value
  if (!data) return []
  const greige = data.greige
  const dyeing = data.dyeing
  const inspection = data.inspection
  const stock = data.stock
  const shipment = data.shipment

  const demandKg = Number(greige?.demandKg ?? 0)
  const coverage = Number(dyeing?.vatCoverage ?? 0)

  const weavingPlanned = Number(greige?.weavingPlannedPieces ?? 0)
  const weavingCompleted = Number(greige?.weavingCompletedPieces ?? 0)

  // M 单位合同（ADR-0034 单位口径）：合同需求不进 KG 合计，未发量 = 需求 - 已发 因而也无定义，
  // 这两项显示占位；其余环节的 KG 量来自执行单据与 KG 流水，是真实聚合值，原样展示不抹除。
  // 以 demandKg 为分母的对照条一律不渲染，避免出现假 0% 进度。
  const meterUnit = isMeterUnit.value
  const kg = (value?: number) => `${formatKg(value)} KG`
  const kgDemand = (value?: number) => (meterUnit ? '—' : `${formatKg(value)} KG`)
  const kgBar = (label: string, part?: number, color?: string): SectionBar[] =>
    meterUnit
      ? []
      : [
          {
            label,
            value: `${formatKg(part)} / ${formatKg(demandKg)}`,
            percent: ratio(part, demandKg),
            color
          }
        ]

  return [
    {
      key: 'greige',
      title: '胚布环节',
      icon: 'ep:box',
      metrics: [
        { label: '合同需求', value: kgDemand(greige?.demandKg), emphasize: true },
        { label: '计划投胚', value: kg(greige?.plannedKg) },
        { label: '胚布入库', value: kg(greige?.stockedInKg) },
        { label: '已发染厂', value: kg(greige?.sentToDyeingKg) }
      ],
      bars: [
        ...kgBar('计划投胚 / 合同需求', greige?.plannedKg),
        ...kgBar('已发染 / 合同需求', greige?.sentToDyeingKg)
      ]
    },
    {
      key: 'weaving',
      title: '织造环节',
      icon: 'ep:tickets',
      metrics: [
        { label: '生产模式', value: greige?.selfWeaving ? '自织' : '外发', emphasize: true },
        { label: '计划匹数', value: greige?.selfWeaving ? `${weavingPlanned} 匹` : '-' },
        { label: '完工匹数', value: greige?.selfWeaving ? `${weavingCompleted} 匹` : '-' }
      ],
      bars: greige?.selfWeaving
        ? [
            {
              label: '织造完工 / 计划',
              value: `${weavingCompleted} / ${weavingPlanned} 匹`,
              percent: ratio(weavingCompleted, weavingPlanned)
            }
          ]
        : undefined,
      empty: greige?.selfWeaving ? undefined : '外发胚布无自织进度，织造环节由坯布厂承担'
    },
    {
      key: 'dyeing',
      title: '染色环节',
      icon: 'ep:brush',
      metrics: [
        { label: '投染量', value: kg(dyeing?.plannedKg), emphasize: true },
        { label: '排产覆盖率', value: `${coverage.toFixed(2)}%` },
        { label: '延期缸数', value: `${Number(dyeing?.delayedVatCount ?? 0)} 缸` }
      ],
      bars: [
        {
          label: '排产覆盖率',
          value: `${coverage.toFixed(2)}%`,
          percent: Math.max(0, Math.min(100, coverage)),
          color: coverage >= 100 ? '#67c23a' : coverage > 0 ? '#409eff' : '#e6a23c'
        }
      ],
      tags: toTags(dyeing?.vatStatusSummary, DICT_TYPE.LUNAERP_PROGRESS_FEEDBACK_STATUS)
    },
    {
      key: 'inspection',
      title: '检验环节',
      icon: 'ep:circle-check',
      metrics: [
        { label: '录检明细', value: `${Number(inspection?.totalItems ?? 0)} 条`, emphasize: true }
      ],
      tags: toTags(inspection?.gradeSummary, DICT_TYPE.LUNAERP_INSPECTION_GRADE),
      empty: Number(inspection?.totalItems ?? 0) > 0 ? undefined : '暂无录检明细'
    },
    {
      key: 'stock',
      title: '入库环节',
      icon: 'ep:download',
      metrics: [
        { label: '胚布入库', value: kg(stock?.greigeInKg), emphasize: true },
        { label: '成品入库', value: kg(stock?.finishedInKg), emphasize: true }
      ],
      bars: kgBar('成品入库 / 合同需求', stock?.finishedInKg)
    },
    {
      key: 'shipment',
      title: '出货环节',
      icon: 'ep:van',
      metrics: [
        { label: '已发货', value: kg(shipment?.shippedKg), emphasize: true },
        { label: '未发量', value: kgDemand(shipment?.unshippedKg), emphasize: true },
        { label: '发货单数', value: `${Number(shipment?.shipmentCount ?? 0)} 单` }
      ],
      bars: kgBar(
        '已发货 / 合同需求',
        shipment?.shippedKg,
        Number(shipment?.unshippedKg ?? 0) > 0 ? '#e6a23c' : '#67c23a'
      )
    }
  ]
})

/** 加载单合同全链路进度 */
const loadDetail = async () => {
  if (!contractId.value) return
  loading.value = true
  try {
    detail.value = await getProductionProgress(contractId.value)
  } catch {
    detail.value = null
  } finally {
    loading.value = false
  }
}

/** 打开抽屉 */
const open = async (id: number) => {
  contractId.value = id
  detail.value = null
  visible.value = true
  await loadDetail()
}

defineExpose({ open })
</script>

<style scoped lang="scss">
.drawer-content {
  min-height: 200px;
}

.contract-header {
  .contract-no {
    font-weight: 600;
    color: #409eff;
  }
}

.alert-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
}

.text-muted {
  color: #c0c4cc;
}

.text-danger {
  font-weight: 600;
  color: #f56c6c;
}

.text-warning {
  font-weight: 600;
  color: #e6a23c;
}

.sub-text {
  font-size: 12px;
  color: #909399;
}

.section-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-top: 16px;

  @media (width <= 768px) {
    grid-template-columns: 1fr;
  }
}

.section-card {
  padding: 12px 14px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;

  .section-head {
    display: flex;
    align-items: center;
    gap: 6px;
    padding-bottom: 8px;
    margin-bottom: 8px;
    border-bottom: 1px solid #f2f6fc;

    .section-icon {
      font-size: 16px;
      color: #409eff;
    }

    .section-title {
      font-size: 14px;
      font-weight: 600;
      color: #303133;
    }
  }

  .metric-list {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 6px 12px;

    .metric-item {
      display: flex;
      align-items: baseline;
      justify-content: space-between;
      gap: 8px;
      font-size: 13px;

      .metric-label {
        color: #909399;
        flex-shrink: 0;
      }

      .metric-value {
        font-weight: 500;
        color: #303133;
        text-align: right;

        &.is-emphasize {
          font-weight: 700;
          color: #409eff;
        }
      }
    }
  }

  .bar-list {
    margin-top: 10px;

    .bar-item + .bar-item {
      margin-top: 8px;
    }

    .bar-head {
      display: flex;
      margin-bottom: 2px;
      font-size: 12px;
      align-items: baseline;
      justify-content: space-between;

      .bar-label {
        color: #909399;
      }

      .bar-value {
        font-family: monospace;
        color: #606266;
      }
    }
  }

  .tag-list {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    margin-top: 10px;
  }

  .section-empty {
    margin-top: 8px;
    font-size: 12px;
    color: #c0c4cc;
  }
}

.drill-section {
  margin-top: 16px;

  .drill-head {
    display: flex;
    align-items: baseline;
    gap: 10px;
    margin-bottom: 8px;

    .drill-title {
      font-size: 14px;
      font-weight: 600;
      color: #303133;
    }

    .drill-tip {
      font-size: 12px;
      color: #909399;
    }
  }
}
</style>
