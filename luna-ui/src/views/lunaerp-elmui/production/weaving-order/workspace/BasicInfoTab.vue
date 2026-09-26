<template>
  <div class="basic-info-tab" v-loading="loading">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="120px"
      :disabled="isReadOnly"
    >
      <el-row :gutter="24">
        <el-col :span="8">
          <el-form-item label="织造单号" prop="code">
            <el-input
              v-model="formData.code"
              :placeholder="isCreateMode ? (previewCode || '系统自动派生') : '系统自动派生'"
              :disabled="!isCreateMode"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="关联胚布单" prop="greigeOrderId">
            <!-- 新建态：可搜索/选择自织胚布单 -->
            <el-select
              v-if="isCreateMode"
              v-model="formData.greigeOrderId"
              placeholder="请选择自织胚布单 (SELF)"
              filterable
              remote
              reserve-keyword
              :remote-method="searchGreigeOrders"
              :loading="greigeLoading"
              class="w-full"
              @change="handleGreigeOrderChange"
            >
              <el-option
                v-for="item in greigeOrderOptions"
                :key="item.id"
                :label="`${item.orderNo} - ${item.productName || ''} (${item.pieceCount ?? 0}匹)`"
                :value="item.id!"
              />
            </el-select>
            <!-- 详情/编辑态：显示快照 -->
            <el-input v-else v-model="formData.greigeOrderNo" disabled />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="状态" prop="status">
            <template v-if="formData.status">
              <DictTag
                :type="DICT_TYPE.LUNAERP_WEAVING_ORDER_STATUS"
                :value="formData.status"
              />
            </template>
            <span v-else class="text-gray-400">待创建 (DRAFT)</span>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="24">
        <el-col :span="8">
          <el-form-item label="关联合同" prop="contractNo">
            <el-input v-model="formData.contractNo" disabled placeholder="选择胚布单自动带入" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="产品编号" prop="productCode">
            <el-input v-model="formData.productCode" disabled placeholder="选择胚布单自动带入" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="产品名称" prop="productName">
            <el-input v-model="formData.productName" disabled placeholder="选择胚布单自动带入" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="24">
        <el-col :span="8">
          <el-form-item label="计划匹数" prop="plannedQty">
            <el-input-number
              v-model="formData.plannedQty"
              :min="1"
              :step="1"
              controls-position="right"
              class="!w-full"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="计划总重(KG)" prop="plannedWeight">
            <el-input-number
              v-model="formData.plannedWeight"
              :min="0.01"
              :precision="2"
              :step="10"
              controls-position="right"
              class="!w-full"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="完工匹数">
            <el-input :model-value="formData.completedQty ?? 0" disabled />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="24">
        <el-col :span="8">
          <el-form-item label="成分" prop="composition">
            <el-input v-model="formData.composition" placeholder="选择胚布单带入，可修改" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="克重" prop="gsm">
            <el-input v-model="formData.gsm" placeholder="选择胚布单带入，可修改" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="门幅" prop="width">
            <el-input v-model="formData.width" placeholder="选择胚布单带入，可修改" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="24">
        <el-col :span="24">
          <el-form-item label="备注" prop="remark">
            <el-input
              v-model="formData.remark"
              type="textarea"
              :rows="3"
              placeholder="请输入备注说明"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 超量软警告提示条 -->
      <el-alert
        v-if="formData.warning"
        :title="formData.warning"
        type="warning"
        show-icon
        :closable="false"
        class="mb-16px"
      />

      <div v-if="!isReadOnly" class="flex justify-end mt-4">
        <el-button type="primary" :loading="saveLoading" @click="handleSave">
          {{ isCreateMode ? '创建织造单' : '保存基本信息' }}
        </el-button>
      </div>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, FormInstance } from 'element-plus'
import { DICT_TYPE } from '@/utils/dict'
import {
  WeavingOrderVO,
  getWeavingOrder,
  createWeavingOrder,
  updateWeavingOrder,
  getNextWeavingOrderCode
} from '@/api/lunaerp-elmui/production/weaving-order'
import {
  GreigeOrderVO,
  getGreigeOrderPage,
  getGreigeOrder
} from '@/api/lunaerp-elmui/production/greige-order'

const props = defineProps<{
  orderId?: number
  isViewMode?: boolean
}>()

const emit = defineEmits<{
  (e: 'loaded', data: WeavingOrderVO): void
  (e: 'saved', data: WeavingOrderVO): void
}>()

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const saveLoading = ref(false)
const greigeLoading = ref(false)
const formRef = ref<FormInstance>()
const formData = ref<WeavingOrderVO>({})
const previewCode = ref('')
const greigeOrderOptions = ref<GreigeOrderVO[]>([])

const isCreateMode = computed(() => !props.orderId)

const isReadOnly = computed(() => {
  if (isCreateMode.value) return false
  return props.isViewMode || formData.value.status !== 'DRAFT'
})

const formRules = {
  greigeOrderId: [{ required: true, message: '请选择自织胚布单', trigger: 'change' }],
  plannedQty: [{ required: true, message: '计划匹数不能为空且必须为正整数', trigger: 'blur' }]
}

// 搜索自织胚布单 (productionMode=SELF)
const searchGreigeOrders = async (query?: string) => {
  greigeLoading.value = true
  try {
    const res = await getGreigeOrderPage({
      pageNo: 1,
      pageSize: 50,
      productionMode: 'SELF',
      keyword: query ? query.trim() : undefined
    })
    greigeOrderOptions.value = res.list || []
  } catch (e) {
    console.error('搜索自织胚布单失败', e)
  } finally {
    greigeLoading.value = false
  }
}

// 切换选择胚布单，回填快照
const handleGreigeOrderChange = async (greigeOrderId?: number) => {
  if (!greigeOrderId) return
  try {
    const greigeOrder = await getGreigeOrder(greigeOrderId)
    formData.value.contractNo = greigeOrder.contractNo
    formData.value.productId = greigeOrder.productId || undefined
    formData.value.productCode = greigeOrder.productCode
    formData.value.productName = greigeOrder.productName
    formData.value.composition = greigeOrder.composition
    formData.value.gsm = greigeOrder.gsm
    formData.value.width = greigeOrder.width
    formData.value.plannedQty = greigeOrder.pieceCount || 1
    formData.value.plannedWeight = greigeOrder.plannedQty || undefined

    // 预览编号
    try {
      const code = await getNextWeavingOrderCode(greigeOrderId)
      previewCode.value = code
    } catch (err) {
      console.warn('获取预览单号失败', err)
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '读取胚布单信息失败')
  }
}

const loadData = async () => {
  if (!props.orderId) return
  loading.value = true
  try {
    const res = await getWeavingOrder(props.orderId)
    formData.value = res
    emit('loaded', res)
  } catch (e: any) {
    ElMessage.error(e?.message || '加载织造单失败')
  } finally {
    loading.value = false
  }
}

const handleSave = async () => {
  if (!formRef.value) return
  await formRef.value.validate()

  saveLoading.value = true
  try {
    if (isCreateMode.value) {
      const res = await createWeavingOrder({
        code: formData.value.code ? formData.value.code.trim() : undefined,
        greigeOrderId: formData.value.greigeOrderId,
        plannedQty: formData.value.plannedQty,
        plannedWeight: formData.value.plannedWeight,
        composition: formData.value.composition,
        gsm: formData.value.gsm,
        width: formData.value.width,
        remark: formData.value.remark
      })
      ElMessage.success('织造单创建成功')
      if (res.warning) {
        ElMessage.warning(res.warning)
      }
      // 跳转到新建好的工作台详情
      router.replace(`/production/weaving-order/workspace/${res.id}`)
    } else {
      const res = await updateWeavingOrder({
        id: formData.value.id,
        code: formData.value.code,
        greigeOrderId: formData.value.greigeOrderId,
        plannedQty: formData.value.plannedQty,
        plannedWeight: formData.value.plannedWeight,
        composition: formData.value.composition,
        gsm: formData.value.gsm,
        width: formData.value.width,
        remark: formData.value.remark
      })
      ElMessage.success('保存成功')
      if (res.warning) {
        ElMessage.warning(res.warning)
      }
      await loadData()
      emit('saved', formData.value)
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saveLoading.value = false
  }
}

// 检查是否从胚布单携带 query.greigeOrderId 进入新建
const initFromGreigeOrderQuery = async () => {
  const greigeOrderIdStr = route.query.greigeOrderId as string
  if (isCreateMode.value && greigeOrderIdStr) {
    const gid = Number(greigeOrderIdStr)
    if (gid) {
      await searchGreigeOrders()
      formData.value.greigeOrderId = gid
      await handleGreigeOrderChange(gid)
    }
  }
}

watch(
  () => props.orderId,
  (id) => {
    if (id) {
      loadData()
    } else {
      formData.value = {
        plannedQty: 1
      }
      initFromGreigeOrderQuery()
    }
  },
  { immediate: true }
)

onMounted(() => {
  if (isCreateMode.value) {
    searchGreigeOrders()
  }
})

defineExpose({
  loadData
})
</script>

<style scoped>
.basic-info-tab {
  padding: 16px 0;
}
</style>
