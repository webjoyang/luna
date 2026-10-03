<template>
  <el-cascader
    v-model="cascaderValue"
    :options="cascaderOptions"
    :props="cascaderProps"
    :placeholder="displayPlaceholder"
    :disabled="disabled"
    :clearable="clearable"
    filterable
    class="yarn-select w-full"
    @change="handleChange"
    @clear="handleClear"
  />
</template>

<script lang="ts" setup>
import { ref, computed, watch, onMounted } from 'vue'
import type { CascaderOption, CascaderProps } from 'element-plus'
import {
  getYarnSpecCascadeOptions,
  type YarnSpecCascadeOptionVO,
  type YarnSpecOptionVO
} from '@/api/lunaerp-elmui/production/yarn-master'

defineOptions({ name: 'YarnSelect' })

const props = withDefaults(
  defineProps<{
    modelValue?: number | string | null
    yarnSpecName?: string
    placeholder?: string
    disabled?: boolean
    clearable?: boolean
  }>(),
  {
    modelValue: null,
    yarnSpecName: '',
    placeholder: '请选择纱线规格',
    disabled: false,
    clearable: true
  }
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: number | null): void
  (e: 'update:yarnSpecName', value: string): void
  (e: 'change', specId: number | null, option?: YarnSpecOptionVO, specName?: string): void
}>()

// 全局/模块级缓存，避免多个行重复调用接口
let cascadeCache: YarnSpecCascadeOptionVO[] | null = null
let cascadeLoadingPromise: Promise<YarnSpecCascadeOptionVO[]> | null = null

const getSharedCascadeOptions = async (): Promise<YarnSpecCascadeOptionVO[]> => {
  if (cascadeCache) {
    return cascadeCache
  }
  if (!cascadeLoadingPromise) {
    cascadeLoadingPromise = getYarnSpecCascadeOptions().then((res) => {
      cascadeCache = res || []
      cascadeLoadingPromise = null
      return cascadeCache
    }).catch((err) => {
      cascadeLoadingPromise = null
      throw err
    })
  }
  return cascadeLoadingPromise
}

const rawList = ref<YarnSpecCascadeOptionVO[]>([])
const cascaderValue = ref<[number, number] | []>([])

const cascaderProps: CascaderProps = {
  expandTrigger: 'hover',
  emitPath: true,
  checkStrictly: false
}

const cascaderOptions = computed<CascaderOption[]>(() => {
  return rawList.value.map((type) => ({
    value: type.yarnTypeId,
    label: type.yarnTypeName,
    children: (type.specs || []).map((spec) => ({
      value: spec.id,
      label: spec.specName + (spec.luster ? ` (${spec.luster})` : '')
    }))
  }))
})

const displayPlaceholder = computed(() => {
  if (props.yarnSpecName) {
    return props.yarnSpecName
  }
  return props.placeholder
})

/** 根据 yarnSpecId 在 rawList 中查找对应的 [yarnTypeId, specId] */
const syncCascaderValue = () => {
  const specId = props.modelValue !== undefined && props.modelValue !== null && props.modelValue !== ''
    ? Number(props.modelValue)
    : null

  if (!specId || !rawList.value.length) {
    cascaderValue.value = []
    return
  }

  for (const type of rawList.value) {
    const spec = (type.specs || []).find((s) => s.id === specId)
    if (spec) {
      cascaderValue.value = [type.yarnTypeId, spec.id]
      return
    }
  }

  cascaderValue.value = []
}

watch(
  () => [props.modelValue, rawList.value],
  () => {
    syncCascaderValue()
  },
  { deep: true }
)

const handleChange = (val: any) => {
  if (!val || !Array.isArray(val) || val.length < 2) {
    handleClear()
    return
  }

  const [typeId, specId] = val as [number, number]
  const type = rawList.value.find((t) => t.yarnTypeId === typeId)
  const spec = type?.specs?.find((s) => s.id === specId)

  if (spec) {
    emit('update:modelValue', spec.id)
    emit('update:yarnSpecName', spec.specName)
    emit('change', spec.id, spec, spec.specName)
  } else {
    handleClear()
  }
}

const handleClear = () => {
  cascaderValue.value = []
  emit('update:modelValue', null)
  emit('update:yarnSpecName', '')
  emit('change', null, undefined, '')
}

onMounted(async () => {
  try {
    rawList.value = await getSharedCascadeOptions()
    syncCascaderValue()
  } catch (e) {
    console.error('加载纱线规格数据源失败', e)
  }
})
</script>

<style scoped>
.yarn-select {
  width: 100%;
}
</style>
