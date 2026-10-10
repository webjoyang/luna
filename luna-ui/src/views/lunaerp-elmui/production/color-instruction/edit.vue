<template>
  <ContentWrap>
    <div class="edit-header mb-4">
      <el-page-header @back="handleBack">
        <template #content>
          <span class="header-title">{{ pageTitle }}</span>
        </template>
      </el-page-header>
    </div>
    <el-empty description="打色指示单编辑页面（功能将在 Issue 02 落地）" />
  </ContentWrap>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

defineOptions({ name: 'LunaerpElmuiProductionColorInstructionEdit' })

const route = useRoute()
const router = useRouter()

const id = computed(() => route.params.id)
const pageTitle = computed(() => (id.value ? '编辑打色指示单' : '新建打色指示单'))

const handleBack = () => {
  router.push('/production/color-instruction')
}
</script>

<style scoped>
.header-title {
  font-weight: 600;
  font-size: 16px;
}
</style>
