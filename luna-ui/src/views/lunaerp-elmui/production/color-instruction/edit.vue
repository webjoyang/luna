<template>
  <ContentWrap>
    <div class="p-20px">
      <el-page-header @back="goBack" :title="'返回列表'">
        <template #content>
          <span class="text-large font-600 mr-3">
            {{ id ? `编辑打色指示单 #${id}` : '新建打色指示单' }}
          </span>
        </template>
      </el-page-header>

      <el-empty
        class="mt-40px"
        description="打色指示单编辑页将在后续票据（02/03 票）完整实现"
      >
        <el-button type="primary" @click="goBack">返回打色指示单列表</el-button>
      </el-empty>
    </div>
  </ContentWrap>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

defineOptions({ name: 'LunaerpElmuiProductionColorInstructionEdit' })

const route = useRoute()
const router = useRouter()

const id = computed(() => {
  const val = route.params.id
  return val ? Number(val) : undefined
})

const goBack = () => {
  router.push('/production/color-instruction')
}
</script>
