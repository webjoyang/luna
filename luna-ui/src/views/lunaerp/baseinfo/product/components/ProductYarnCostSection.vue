<template>
  <div class="product-yarn-cost-container space-y-4">
    <!-- ========== 4. 纱线规格 ========== -->
    <motion.div
      class="form-section"
      :initial="{ opacity: 0, y: 14 }"
      :animate="{ opacity: 1, y: 0 }"
      :transition="{ duration: 0.35, delay: 0.11, ease: EASE }"
    >
      <div class="section-title">纱线规格</div>

      <!-- 模式与结构切换 -->
      <div class="flex flex-wrap items-center justify-between gap-3 mb-4 pb-3 border-b border-[var(--el-border-color-lighter)]">
        <div class="flex items-center gap-3">
          <span class="text-xs font-bold text-gray-500">织物结构</span>
          <el-radio-group v-model="form.fabricConstruction" size="small" :disabled="disabled" @change="handleStructureChange">
            <el-radio-button label="KNIT">针织</el-radio-button>
            <el-radio-button label="WOVEN">梭织</el-radio-button>
          </el-radio-group>

          <template v-if="isWoven">
            <span class="text-xs font-bold text-gray-500 ml-3">估算方式</span>
            <el-radio-group v-model="form.wovenCalculationMode" size="small" :disabled="disabled">
              <el-radio-button label="SIMPLE">简易估算</el-radio-button>
              <el-radio-button label="PROFESSIONAL">经纬精算</el-radio-button>
            </el-radio-group>
          </template>
        </div>

        <div v-if="!isProfessional" class="flex items-center gap-2">
          <el-button type="primary" link size="small" :disabled="disabled" @click="addYarnRow()">
            <Icon icon="ep:plus" class="mr-1" />添加纱线
          </el-button>
        </div>
      </div>

      <!-- 梭织坯布参数 -->
      <div v-if="isWoven" class="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4 p-3 bg-gray-50/60 dark:bg-gray-800/40 rounded-lg border border-[var(--el-border-color-lighter)]">
        <div>
          <label class="block text-xs font-bold text-gray-500 mb-1">坯布克重（GSM）</label>
          <el-input-number
            v-model="form.greigeWeightGsm"
            :min="0"
            :precision="2"
            :controls="false"
            :disabled="disabled"
            placeholder="请输入坯布克重"
            class="!w-full"
          />
        </div>
        <div>
          <label class="block text-xs font-bold text-gray-500 mb-1">坯布门幅（CM）</label>
          <el-input-number
            v-model="form.greigeWidthCm"
            :min="0"
            :precision="2"
            :controls="false"
            :disabled="disabled"
            placeholder="请输入坯布门幅"
            class="!w-full"
          />
        </div>
        <div>
          <label class="block text-xs font-bold text-gray-500 mb-1">每米重量（KG/M）</label>
          <div class="h-8 flex items-center px-3 text-sm font-mono font-bold text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-900 border border-[var(--el-border-color-lighter)] rounded">
            {{ kgPerMeterDisplay }}
          </div>
        </div>
      </div>

      <!-- 针织 / 梭织简易模式表格 -->
      <div v-if="!isProfessional" class="overflow-x-auto rounded border border-[var(--el-border-color-lighter)]">
        <table class="w-full text-xs text-left">
          <thead class="bg-gray-50/80 dark:bg-gray-800/60 text-gray-500 font-bold border-b border-[var(--el-border-color-lighter)]">
            <tr>
              <th class="py-2.5 px-3 min-w-[220px]">纱线规格</th>
              <th class="py-2.5 px-3 w-[120px]">单价 (元/KG)</th>
              <th class="py-2.5 px-3 w-[110px]">比例%</th>
              <th class="py-2.5 px-3 w-[110px]">损耗率%</th>
              <th class="py-2.5 px-3 w-[120px] text-right">成本贡献</th>
              <th class="py-2.5 px-3 w-[60px] text-center">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-[var(--el-border-color-lighter)]">
            <tr v-if="!yarnRows.length">
              <td colspan="6" class="text-center py-6 text-gray-400">暂无纱线规格明细，点击右上角添加</td>
            </tr>
            <tr v-for="row in yarnRows" :key="row._key" class="hover:bg-gray-50/50 dark:hover:bg-gray-800/30">
              <td class="py-2 px-3">
                <YarnSelect
                  v-model="row.yarnSpecId"
                  v-model:yarn-spec-name="row.yarnSpecName"
                  :disabled="disabled"
                  @change="(_id, opt, name) => handleYarnSpecChange(row, opt, name)"
                />
              </td>
              <td class="py-2 px-3">
                <el-input-number
                  v-model="row.price"
                  :min="0"
                  :precision="2"
                  :controls="false"
                  :disabled="disabled"
                  placeholder="0.00"
                  class="!w-full"
                />
              </td>
              <td class="py-2 px-3">
                <el-input-number
                  v-model="row.ratio"
                  :min="0"
                  :max="100"
                  :precision="2"
                  :controls="false"
                  :disabled="disabled"
                  placeholder="0"
                  class="!w-full"
                />
              </td>
              <td class="py-2 px-3">
                <el-input-number
                  v-model="row.wasteRate"
                  :min="0"
                  :max="99"
                  :precision="2"
                  :controls="false"
                  :disabled="disabled"
                  placeholder="0"
                  class="!w-full"
                />
              </td>
              <td class="py-2 px-3 text-right font-mono font-bold text-gray-700 dark:text-gray-300">
                ¥{{ calcRowContribution(row).toFixed(2) }}
              </td>
              <td class="py-2 px-3 text-center">
                <el-button type="danger" link :disabled="disabled" @click="removeYarnRow(row)">
                  <Icon icon="ep:delete" />
                </el-button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 梭织经纬精算模式表格（按经纱/纬纱分组） -->
      <div v-else class="space-y-4">
        <!-- 经纱分组 -->
        <div class="rounded border border-[var(--el-border-color-lighter)] overflow-hidden">
          <div class="flex items-center justify-between px-3 py-2 bg-gray-50/80 dark:bg-gray-800/60 border-b border-[var(--el-border-color-lighter)]">
            <span class="text-xs font-bold text-gray-700 dark:text-gray-200">经纱明细</span>
            <el-button type="primary" link size="small" :disabled="disabled" @click="addYarnRow('WARP')">
              <Icon icon="ep:plus" class="mr-1" />添加经纱
            </el-button>
          </div>
          <table class="w-full text-xs text-left">
            <thead class="bg-gray-50/40 text-gray-500 font-bold border-b border-[var(--el-border-color-lighter)]">
              <tr>
                <th class="py-2 px-3 min-w-[200px]">纱线规格</th>
                <th class="py-2 px-3 w-[110px]">单价 (元/KG)</th>
                <th class="py-2 px-3 w-[90px]">密度 (根/CM)</th>
                <th class="py-2 px-3 w-[90px]">织缩率%</th>
                <th class="py-2 px-3 w-[90px]">损耗率%</th>
                <th class="py-2 px-3 w-[110px]">每米用量 (KG/M)</th>
                <th class="py-2 px-3 w-[110px] text-right">每米金额</th>
                <th class="py-2 px-3 w-[50px] text-center">操作</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-[var(--el-border-color-lighter)]">
              <tr v-if="!warpRows.length">
                <td colspan="8" class="text-center py-4 text-gray-400">暂无经纱明细</td>
              </tr>
              <tr v-for="row in warpRows" :key="row._key" class="hover:bg-gray-50/50">
                <td class="py-2 px-3">
                  <YarnSelect
                    v-model="row.yarnSpecId"
                    v-model:yarn-spec-name="row.yarnSpecName"
                    :disabled="disabled"
                    @change="(_id, opt, name) => handleYarnSpecChange(row, opt, name)"
                  />
                </td>
                <td class="py-2 px-3">
                  <el-input-number v-model="row.price" :min="0" :precision="2" :controls="false" :disabled="disabled" placeholder="0.00" class="!w-full" />
                </td>
                <td class="py-2 px-3">
                  <el-input-number v-model="row.density" :min="0" :precision="2" :controls="false" :disabled="disabled" placeholder="0" class="!w-full" />
                </td>
                <td class="py-2 px-3">
                  <el-input-number v-model="row.crimpRate" :min="0" :max="99" :precision="2" :controls="false" :disabled="disabled" placeholder="0" class="!w-full" />
                </td>
                <td class="py-2 px-3">
                  <el-input-number v-model="row.wasteRate" :min="0" :max="99" :precision="2" :controls="false" :disabled="disabled" placeholder="0" class="!w-full" />
                </td>
                <td class="py-2 px-3">
                  <el-input-number v-model="row.consumptionKgPerMeter" :min="0" :precision="4" :step="0.001" :controls="false" :disabled="disabled" placeholder="0.0000" class="!w-full" />
                </td>
                <td class="py-2 px-3 text-right font-mono font-bold text-gray-700 dark:text-gray-300">
                  ¥{{ calcRowContribution(row).toFixed(2) }}
                </td>
                <td class="py-2 px-3 text-center">
                  <el-button type="danger" link :disabled="disabled" @click="removeYarnRow(row)">
                    <Icon icon="ep:delete" />
                  </el-button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 纬纱分组 -->
        <div class="rounded border border-[var(--el-border-color-lighter)] overflow-hidden">
          <div class="flex items-center justify-between px-3 py-2 bg-gray-50/80 dark:bg-gray-800/60 border-b border-[var(--el-border-color-lighter)]">
            <span class="text-xs font-bold text-gray-700 dark:text-gray-200">纬纱明细</span>
            <el-button type="primary" link size="small" :disabled="disabled" @click="addYarnRow('WEFT')">
              <Icon icon="ep:plus" class="mr-1" />添加纬纱
            </el-button>
          </div>
          <table class="w-full text-xs text-left">
            <thead class="bg-gray-50/40 text-gray-500 font-bold border-b border-[var(--el-border-color-lighter)]">
              <tr>
                <th class="py-2 px-3 min-w-[200px]">纱线规格</th>
                <th class="py-2 px-3 w-[110px]">单价 (元/KG)</th>
                <th class="py-2 px-3 w-[90px]">密度 (根/CM)</th>
                <th class="py-2 px-3 w-[90px]">织缩率%</th>
                <th class="py-2 px-3 w-[90px]">损耗率%</th>
                <th class="py-2 px-3 w-[110px]">每米用量 (KG/M)</th>
                <th class="py-2 px-3 w-[110px] text-right">每米金额</th>
                <th class="py-2 px-3 w-[50px] text-center">操作</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-[var(--el-border-color-lighter)]">
              <tr v-if="!weftRows.length">
                <td colspan="8" class="text-center py-4 text-gray-400">暂无纬纱明细</td>
              </tr>
              <tr v-for="row in weftRows" :key="row._key" class="hover:bg-gray-50/50">
                <td class="py-2 px-3">
                  <YarnSelect
                    v-model="row.yarnSpecId"
                    v-model:yarn-spec-name="row.yarnSpecName"
                    :disabled="disabled"
                    @change="(_id, opt, name) => handleYarnSpecChange(row, opt, name)"
                  />
                </td>
                <td class="py-2 px-3">
                  <el-input-number v-model="row.price" :min="0" :precision="2" :controls="false" :disabled="disabled" placeholder="0.00" class="!w-full" />
                </td>
                <td class="py-2 px-3">
                  <el-input-number v-model="row.density" :min="0" :precision="2" :controls="false" :disabled="disabled" placeholder="0" class="!w-full" />
                </td>
                <td class="py-2 px-3">
                  <el-input-number v-model="row.crimpRate" :min="0" :max="99" :precision="2" :controls="false" :disabled="disabled" placeholder="0" class="!w-full" />
                </td>
                <td class="py-2 px-3">
                  <el-input-number v-model="row.wasteRate" :min="0" :max="99" :precision="2" :controls="false" :disabled="disabled" placeholder="0" class="!w-full" />
                </td>
                <td class="py-2 px-3">
                  <el-input-number v-model="row.consumptionKgPerMeter" :min="0" :precision="4" :step="0.001" :controls="false" :disabled="disabled" placeholder="0.0000" class="!w-full" />
                </td>
                <td class="py-2 px-3 text-right font-mono font-bold text-gray-700 dark:text-gray-300">
                  ¥{{ calcRowContribution(row).toFixed(2) }}
                </td>
                <td class="py-2 px-3 text-center">
                  <el-button type="danger" link :disabled="disabled" @click="removeYarnRow(row)">
                    <Icon icon="ep:delete" />
                  </el-button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 未指定方向的其他行（防止老数据隐藏） -->
        <div v-if="otherRows.length" class="rounded border border-dashed border-amber-300 overflow-hidden">
          <div class="px-3 py-2 bg-amber-50/60 text-amber-700 text-xs font-bold">未分类纱线行（无经纬方向）</div>
          <table class="w-full text-xs text-left">
            <tbody class="divide-y divide-[var(--el-border-color-lighter)]">
              <tr v-for="row in otherRows" :key="row._key">
                <td class="py-2 px-3 min-w-[200px]">
                  <YarnSelect
                    v-model="row.yarnSpecId"
                    v-model:yarn-spec-name="row.yarnSpecName"
                    :disabled="disabled"
                    @change="(_id, opt, name) => handleYarnSpecChange(row, opt, name)"
                  />
                </td>
                <td class="py-2 px-3 w-[120px]">
                  <el-select v-model="row.direction" size="small" :disabled="disabled">
                    <el-option label="经纱" value="WARP" />
                    <el-option label="纬纱" value="WEFT" />
                  </el-select>
                </td>
                <td class="py-2 px-3 w-[110px]">
                  <el-input-number v-model="row.price" :min="0" :precision="2" :controls="false" :disabled="disabled" class="!w-full" />
                </td>
                <td class="py-2 px-3 text-center w-[60px]">
                  <el-button type="danger" link :disabled="disabled" @click="removeYarnRow(row)">
                    <Icon icon="ep:delete" />
                  </el-button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </motion.div>

    <!-- ========== 5. 成本结构明细 ========== -->
    <motion.div
      class="form-section"
      :initial="{ opacity: 0, y: 14 }"
      :animate="{ opacity: 1, y: 0 }"
      :transition="{ duration: 0.35, delay: 0.14, ease: EASE }"
    >
      <div class="section-title">成本结构明细</div>

      <div class="grid grid-cols-1 lg:grid-cols-[1.3fr_0.9fr] gap-5 mb-4">
        <!-- 左侧：加工费设置与成本项 -->
        <div class="space-y-4">
          <!-- 加工费设置 -->
          <div class="p-3 bg-gray-50/60 dark:bg-gray-800/40 rounded-lg border border-[var(--el-border-color-lighter)]">
            <div class="text-xs font-bold text-gray-600 mb-2">加工费与附加成本</div>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
              <div v-if="!isWoven">
                <label class="block text-xs text-gray-500 mb-1">针织加工费（元/KG）</label>
                <el-input-number
                  v-model="form.knitProcessingFeePerKg"
                  :min="0"
                  :precision="4"
                  :controls="false"
                  :disabled="disabled"
                  placeholder="0.0000"
                  class="!w-full"
                />
              </div>
              <template v-else>
                <div>
                  <label class="block text-xs text-gray-500 mb-1">织造费（元/M）</label>
                  <el-input-number
                    v-model="form.weavingFeePerMeter"
                    :min="0"
                    :precision="4"
                    :controls="false"
                    :disabled="disabled"
                    placeholder="0.0000"
                    class="!w-full"
                  />
                </div>
                <div>
                  <label class="block text-xs text-gray-500 mb-1">其他加工费（元/M）</label>
                  <el-input-number
                    v-model="form.otherProcessingFeePerMeter"
                    :min="0"
                    :precision="4"
                    :controls="false"
                    :disabled="disabled"
                    placeholder="0.0000"
                    class="!w-full"
                  />
                </div>
              </template>
              <div>
                <label class="block text-xs text-gray-500 mb-1">包装费（元）</label>
                <el-input-number
                  v-model="form.packagingFee"
                  :min="0"
                  :precision="4"
                  :controls="false"
                  :disabled="disabled"
                  placeholder="0.0000"
                  class="!w-full"
                />
              </div>
              <div>
                <label class="block text-xs text-gray-500 mb-1">税率</label>
                <el-input-number
                  v-model="form.taxRate"
                  :min="0"
                  :precision="4"
                  :controls="false"
                  :disabled="disabled"
                  placeholder="如 0.13"
                  class="!w-full"
                />
              </div>
            </div>
          </div>

          <!-- 成本项表格 -->
          <div class="rounded border border-[var(--el-border-color-lighter)] overflow-hidden">
            <div class="flex items-center justify-between px-3 py-2 bg-gray-50/80 dark:bg-gray-800/60 border-b border-[var(--el-border-color-lighter)]">
              <span class="text-xs font-bold text-gray-700 dark:text-gray-200">成本项明细</span>
              <el-button type="primary" link size="small" :disabled="disabled" @click="addCostItem()">
                <Icon icon="ep:plus" class="mr-1" />添加成本项
              </el-button>
            </div>
            <table class="w-full text-xs text-left">
              <thead class="bg-gray-50/40 text-gray-500 font-bold border-b border-[var(--el-border-color-lighter)]">
                <tr>
                  <th class="py-2 px-3 min-w-[150px]">供应商</th>
                  <th class="py-2 px-3 min-w-[100px]">工序</th>
                  <th class="py-2 px-3 w-[100px]">单价 (元)</th>
                  <th class="py-2 px-3 w-[90px]">损耗率%</th>
                  <th class="py-2 px-3 w-[50px] text-center">操作</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-[var(--el-border-color-lighter)]">
                <tr v-if="!costRows.length">
                  <td colspan="5" class="text-center py-4 text-gray-400">暂无成本项明细</td>
                </tr>
                <tr v-for="item in costRows" :key="item._key" class="hover:bg-gray-50/50">
                  <td class="py-2 px-3">
                    <el-select
                      v-model="item.supplierId"
                      filterable
                      remote
                      reserve-keyword
                      :remote-method="searchSuppliers"
                      :loading="supplierLoading"
                      :disabled="disabled"
                      placeholder="搜索供应商"
                      class="!w-full"
                      @change="(val) => handleSupplierChange(item, val)"
                    >
                      <el-option
                        v-for="opt in getCombinedSupplierOptions(item)"
                        :key="opt.id!"
                        :label="opt.companyName"
                        :value="opt.id!"
                      />
                    </el-select>
                  </td>
                  <td class="py-2 px-3">
                    <el-input v-model="item.process" placeholder="工序名称" :disabled="disabled" />
                  </td>
                  <td class="py-2 px-3">
                    <el-input-number
                      v-model="item.price"
                      :min="0"
                      :precision="4"
                      :controls="false"
                      :disabled="disabled"
                      placeholder="0.00"
                      class="!w-full"
                    />
                  </td>
                  <td class="py-2 px-3">
                    <el-input-number
                      v-model="item.lossPercent"
                      :min="0"
                      :max="99"
                      :precision="2"
                      :controls="false"
                      :disabled="disabled"
                      placeholder="0"
                      class="!w-full"
                    />
                  </td>
                  <td class="py-2 px-3 text-center">
                    <el-button type="danger" link :disabled="disabled" @click="removeCostItem(item)">
                      <Icon icon="ep:delete" />
                    </el-button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- 右侧：成本汇总面板（暗黑仪表盘风格） -->
        <div class="rounded-xl bg-[#1e2025] text-white p-5 flex flex-col justify-between shadow-lg">
          <div>
            <div class="flex items-center justify-between pb-3 border-b border-white/10 mb-4">
              <span class="text-xs font-bold uppercase tracking-wider text-gray-400">成本核算汇总</span>
              <span class="text-xs px-2 py-0.5 rounded bg-amber-400/20 text-amber-300 font-semibold">
                {{ costResult.basis === 'KG' ? '按公斤计' : '按米长计' }}
              </span>
            </div>

            <div class="space-y-4">
              <div>
                <div class="text-[11px] text-gray-400 mb-0.5">
                  {{ isWoven ? '梭织坯布总成本' : '针织坯布总成本' }}
                </div>
                <div class="text-3xl font-black font-mono text-amber-400">
                  ¥{{ costResult.totalCost.toFixed(2) }}
                  <span class="text-xs font-normal text-gray-400">/ {{ costResult.basis === 'KG' ? 'KG' : 'M' }}</span>
                </div>
              </div>

              <div class="grid grid-cols-2 gap-3 pt-3 border-t border-white/10">
                <div>
                  <div class="text-[10px] text-gray-400">{{ isWoven ? '等效成本' : '纱线成本' }}</div>
                  <div class="text-base font-bold font-mono text-gray-100 mt-0.5">
                    ¥{{ equivalentCostDisplay }}/KG
                  </div>
                </div>
                <div>
                  <div class="text-[10px] text-gray-400">{{ isWoven ? '每公斤约' : '配比合计' }}</div>
                  <div class="text-base font-bold font-mono text-gray-100 mt-0.5">
                    {{ isWoven ? `${costResult.metersPerKg.toFixed(2)} 米` : `${costResult.ratioTotal.toFixed(1)}%` }}
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="mt-4 pt-3 border-t border-white/10 space-y-1">
            <div v-if="costResult.missingPriceCount > 0" class="flex items-center text-xs font-semibold text-amber-300">
              <Icon icon="ep:warning" class="mr-1.5 flex-shrink-0" />
              <span>还有 {{ costResult.missingPriceCount }} 根纱未设置价格，当前成本不完整</span>
            </div>
            <div v-else class="text-xs text-gray-400">
              已完成所有有效纱线成本计算
            </div>
          </div>
        </div>
      </div>
    </motion.div>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, watch, onMounted } from 'vue'
import { motion } from 'motion-v'
import type { ProductFormData } from './product-form'
import {
  calculateProductYarnCost,
  calculateYarnRowContribution,
  type ProductYarnCostSettings,
  type YarnDirection
} from './product-yarn-cost'
import YarnSelect from './YarnSelect.vue'
import * as SupplierApi from '@/api/lunaerp-elmui/baseinfo/supplier'
import type { SupplierVO } from '@/api/lunaerp-elmui/baseinfo/supplier'
import type { YarnSpecOptionVO } from '@/api/lunaerp-elmui/production/yarn-master'

defineOptions({ name: 'ProductYarnCostSection' })

const EASE: [number, number, number, number] = [0.16, 1, 0.3, 1]

const props = withDefaults(
  defineProps<{
    modelValue: ProductFormData
    disabled?: boolean
  }>(),
  {
    disabled: false
  }
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: ProductFormData): void
}>()

const form = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const isWoven = computed(() => form.value.fabricConstruction === 'WOVEN')
const isProfessional = computed(() => isWoven.value && form.value.wovenCalculationMode === 'PROFESSIONAL')

interface EditableYarnRow {
  _key: string
  yarnSpecId?: number
  yarnSpecName?: string
  yarnSpec?: string
  spinMethod?: string
  ratio?: number
  price?: number
  wasteRate?: number
  direction?: YarnDirection
  density?: number
  crimpRate?: number
  consumptionKgPerMeter?: number
  remark?: string
}

interface EditableCostItem {
  _key: string
  supplierId?: number
  supplier?: string
  process?: string
  price?: number
  lossPercent?: number
}

let keyCounter = 0
const generateKey = () => `row_${Date.now()}_${++keyCounter}`

const yarnRows = ref<EditableYarnRow[]>([])
const costRows = ref<EditableCostItem[]>([])

const toNum = (v: unknown): number | undefined => {
  if (v === undefined || v === null || v === '') return undefined
  const p = Number(v)
  return Number.isFinite(p) ? p : undefined
}

/** 解析已有的 yarnSpecs JSON 字符串并回显 */
const parseYarnSpecs = () => {
  if (form.value.yarnSpecs) {
    try {
      const parsed = JSON.parse(form.value.yarnSpecs)
      if (Array.isArray(parsed)) {
        yarnRows.value = parsed.map((item) => ({
          _key: generateKey(),
          yarnSpecId: toNum(item.yarnSpecId),
          yarnSpecName: item.yarnSpecName || '',
          yarnSpec: item.yarnSpec || item.yarnSpecName || '',
          spinMethod: item.spinMethod || '',
          ratio: toNum(item.ratio),
          price: toNum(item.price),
          wasteRate: toNum(item.wasteRate),
          direction: item.direction || (isWoven.value ? 'WARP' : 'GENERAL'),
          density: toNum(item.density),
          crimpRate: toNum(item.crimpRate),
          consumptionKgPerMeter: toNum(item.consumptionKgPerMeter),
          remark: item.remark || ''
        }))
      }
    } catch (e) {
      console.error('解析 yarnSpecs 失败', e)
    }
  } else if (!form.value.yarnSpecs && yarnRows.value.length) {
    yarnRows.value = []
  }
}

/** 解析已有的 costItems JSON 字符串并回显 */
const parseCostItems = () => {
  if (form.value.costItems) {
    try {
      const parsed = JSON.parse(form.value.costItems)
      if (Array.isArray(parsed)) {
        costRows.value = parsed.map((item) => ({
          _key: generateKey(),
          supplierId: toNum(item.supplierId),
          supplier: item.supplier || '',
          process: item.process || '',
          price: toNum(item.price),
          lossPercent: toNum(item.lossPercent)
        }))
      }
    } catch (e) {
      console.error('解析 costItems 失败', e)
    }
  } else if (!form.value.costItems && costRows.value.length) {
    costRows.value = []
  }
}

/** 序列化到 formData 的 JSON 字符串 */
const syncToForm = () => {
  if (yarnRows.value.length) {
    const cleanYarn = yarnRows.value.map(({ _key, ...rest }) => rest)
    form.value.yarnSpecs = JSON.stringify(cleanYarn)
  } else {
    form.value.yarnSpecs = ''
  }

  if (costRows.value.length) {
    const cleanCost = costRows.value.map(({ _key, ...rest }) => rest)
    form.value.costItems = JSON.stringify(cleanCost)
  } else {
    form.value.costItems = ''
  }
}

watch(
  [yarnRows, costRows],
  () => {
    syncToForm()
  },
  { deep: true }
)

// 经纱/纬纱分组
const warpRows = computed(() => yarnRows.value.filter((r) => r.direction === 'WARP'))
const weftRows = computed(() => yarnRows.value.filter((r) => r.direction === 'WEFT'))
const otherRows = computed(() =>
  yarnRows.value.filter((r) => r.direction !== 'WARP' && r.direction !== 'WEFT')
)

/** 切换结构时的处理（保留已有行，仅更新方向缺省） */
const handleStructureChange = (val: any) => {
  if (val === 'WOVEN') {
    // 切换到梭织时，若行无方向且切为经纬精算，默认给经纱
    yarnRows.value.forEach((row) => {
      if (!row.direction || row.direction === 'GENERAL') {
        row.direction = 'WARP'
      }
    })
  }
}

const addYarnRow = (direction: YarnDirection = isWoven.value ? 'WARP' : 'GENERAL') => {
  yarnRows.value.push({
    _key: generateKey(),
    yarnSpecId: undefined,
    yarnSpecName: '',
    yarnSpec: '',
    spinMethod: '',
    ratio: undefined,
    price: undefined,
    wasteRate: undefined,
    direction,
    density: undefined,
    crimpRate: undefined,
    consumptionKgPerMeter: undefined
  })
}

const removeYarnRow = (row: EditableYarnRow) => {
  const idx = yarnRows.value.findIndex((r) => r._key === row._key)
  if (idx !== -1) {
    yarnRows.value.splice(idx, 1)
  }
}

const handleYarnSpecChange = (row: EditableYarnRow, opt?: YarnSpecOptionVO, name?: string) => {
  if (opt) {
    row.yarnSpecId = opt.id
    row.yarnSpecName = opt.specName
    row.yarnSpec = opt.specName
    if (opt.refPrice !== undefined && opt.refPrice !== null) {
      row.price = opt.refPrice
    }
  } else {
    row.yarnSpec = name || ''
  }
}

// 成本设置对象
const costSettings = computed<ProductYarnCostSettings>(() => ({
  fabricConstruction: form.value.fabricConstruction || 'KNIT',
  wovenCalculationMode: form.value.wovenCalculationMode || 'SIMPLE',
  greigeWeightGsm: form.value.greigeWeightGsm || 0,
  greigeWidthCm: form.value.greigeWidthCm || 0,
  knitProcessingFeePerKg: form.value.knitProcessingFeePerKg || 0,
  weavingFeePerMeter: form.value.weavingFeePerMeter || 0,
  otherProcessingFeePerMeter: form.value.otherProcessingFeePerMeter || 0
}))

// 成本汇总计算结果
const costResult = computed(() => calculateProductYarnCost(yarnRows.value, costSettings.value))

const kgPerMeterDisplay = computed(() => {
  return costResult.value.kgPerMeter.toFixed(3)
})

const equivalentCostDisplay = computed(() => {
  if (isWoven.value) {
    return costResult.value.kgPerMeter > 0
      ? (costResult.value.totalCost / costResult.value.kgPerMeter).toFixed(2)
      : '0.00'
  }
  return costResult.value.yarnCostPerKg.toFixed(2)
})

const calcRowContribution = (row: EditableYarnRow): number => {
  return calculateYarnRowContribution(
    row,
    form.value.fabricConstruction || 'KNIT',
    form.value.wovenCalculationMode || 'SIMPLE',
    costResult.value.kgPerMeter
  )
}

// ========== 成本项与供应商 ==========
const supplierLoading = ref(false)
const supplierOptions = ref<SupplierVO[]>([])

const searchSuppliers = async (query: string) => {
  if (!query) {
    supplierOptions.value = []
    return
  }
  supplierLoading.value = true
  try {
    const res = await SupplierApi.getSupplierPage({
      pageNo: 1,
      pageSize: 20,
      search: query,
      status: 'ACTIVE'
    })
    supplierOptions.value = (res as any)?.list || []
  } catch (e) {
    console.error('搜索供应商失败', e)
  } finally {
    supplierLoading.value = false
  }
}

/** 合并当前行已有供应商和搜索结果，防止回显时 label 丢失 */
const getCombinedSupplierOptions = (item: EditableCostItem): SupplierVO[] => {
  const map = new Map<number, SupplierVO>()
  if (item.supplierId && item.supplier) {
    map.set(item.supplierId, {
      id: item.supplierId,
      companyName: item.supplier
    })
  }
  for (const opt of supplierOptions.value) {
    if (opt.id) {
      map.set(opt.id, opt)
    }
  }
  return Array.from(map.values())
}

const handleSupplierChange = (item: EditableCostItem, val: any) => {
  if (!val) {
    item.supplierId = undefined
    item.supplier = ''
    return
  }
  const target = supplierOptions.value.find((s) => s.id === val)
  if (target) {
    item.supplierId = target.id
    item.supplier = target.companyName
  }
}

const addCostItem = () => {
  costRows.value.push({
    _key: generateKey(),
    supplierId: undefined,
    supplier: '',
    process: '',
    price: undefined,
    lossPercent: undefined
  })
}

const removeCostItem = (item: EditableCostItem) => {
  const idx = costRows.value.findIndex((c) => c._key === item._key)
  if (idx !== -1) {
    costRows.value.splice(idx, 1)
  }
}

onMounted(() => {
  parseYarnSpecs()
  parseCostItems()
})

watch(
  () => form.value.yarnSpecs,
  (newVal, oldVal) => {
    if (newVal !== oldVal && !yarnRows.value.length) {
      parseYarnSpecs()
    }
  }
)

watch(
  () => form.value.costItems,
  (newVal, oldVal) => {
    if (newVal !== oldVal && !costRows.value.length) {
      parseCostItems()
    }
  }
)
</script>

<style lang="scss" scoped>
.form-section {
  padding: 14px 16px 14px;
  background: var(--el-fill-color-blank);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;
}

.section-title {
  display: flex;
  padding-left: 10px;
  margin-bottom: 14px;
  font-size: 14px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  border-left: 3px solid var(--el-color-primary);
}
</style>
