<template>
  <el-form
    ref="formRef"
    :model="formData"
    label-width="110px"
    :disabled="isReadOnly"
  >
    <!-- 工艺分类卡片 -->
    <el-card shadow="never" class="mb-16px">
      <template #header>
        <div class="font-bold text-14px">工艺分类</div>
      </template>
      <el-row :gutter="24">
        <el-col :span="8">
          <el-form-item label="工序" prop="process">
            <el-select
              v-model="formData.process"
              placeholder="请选择工序（可留空）"
              clearable
              class="!w-full"
            >
              <el-option
                v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_SAMPLE_PROCESS)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="双染工艺" prop="dyeingProcess">
            <el-select
              v-model="formData.dyeingProcess"
              placeholder="请选择双染工艺（可留空）"
              clearable
              class="!w-full"
            >
              <el-option
                v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_SAMPLE_DYEING_PROCESS)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="机型" prop="machineType">
            <el-select
              v-model="formData.machineType"
              placeholder="请选择机型（可留空）"
              clearable
              class="!w-full"
            >
              <el-option
                v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_SAMPLE_MACHINE_TYPE)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
    </el-card>

    <!-- 机台生产工艺参数卡片 -->
    <el-card shadow="never" class="mb-16px">
      <template #header>
        <div class="font-bold text-14px">机台生产工艺参数</div>
      </template>
      <el-row :gutter="24">
        <el-col :span="8">
          <el-form-item label="实测产量" prop="output">
            <el-input v-model="formData.output" placeholder="实测产量（如 120KG/天）" clearable />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="花号" prop="flowerNo">
            <el-input v-model="formData.flowerNo" placeholder="花号" clearable />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="白胚门幅" prop="whiteWidth">
            <el-input v-model="formData.whiteWidth" placeholder="白胚门幅" clearable />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="24">
        <el-col :span="8">
          <el-form-item label="白胚克重" prop="whiteWeight">
            <el-input v-model="formData.whiteWeight" placeholder="白胚克重" clearable />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="花宽" prop="flowerWidth">
            <el-input v-model="formData.flowerWidth" placeholder="花宽" clearable />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="花高" prop="flowerHeight">
            <el-input v-model="formData.flowerHeight" placeholder="花高" clearable />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="24">
        <el-col :span="8">
          <el-form-item label="齿轮" prop="gear">
            <el-input v-model="formData.gear" placeholder="齿轮" clearable />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="布架" prop="clothRack">
            <el-input v-model="formData.clothRack" placeholder="布架" clearable />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="开机转数" prop="startSpeed">
            <el-input v-model="formData.startSpeed" placeholder="开机转数" clearable />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="24">
        <el-col :span="8">
          <el-form-item label="送纱盘" prop="feederDisk">
            <el-input v-model="formData.feederDisk" placeholder="送纱盘" clearable />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="理论产量" prop="theoreticalOutput">
            <el-input v-model="formData.theoreticalOutput" placeholder="理论产量" clearable />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="线长" prop="stitchLength">
            <el-input v-model="formData.stitchLength" placeholder="线长（如 9.6cm/50针）" clearable />
          </el-form-item>
        </el-col>
      </el-row>
    </el-card>

    <!-- 待上线工艺功能占位卡片 -->
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="font-bold text-14px">用纱结构、绕纱与三角排列工艺</span>
          <el-tag type="info" size="small">功能待后续上线</el-tag>
        </div>
      </template>
      <div class="py-20px text-center text-gray-400">
        <Icon icon="ep:setting" class="text-32px mb-8px" />
        <div>纱线规格明细（票 03）、绕纱工艺（票 04）与大圆机三角排列画板（票 05）将在后续票中开放</div>
      </div>
    </el-card>
  </el-form>
</template>

<script lang="ts" setup>
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import type { SampleAnalysisVO } from '@/api/lunaerp-elmui/sample/sample-analysis'

defineProps<{
  isReadOnly: boolean
}>()

const formData = defineModel<SampleAnalysisVO>({ required: true })
</script>
