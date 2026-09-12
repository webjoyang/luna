<template>
  <div :class="prefixCls">
    <div :class="`${prefixCls}__aurora`" aria-hidden="true"></div>

    <div :class="`${prefixCls}__inner`">
      <WorkbenchTopBar
        :active="scope"
        :tabs="scopeTabs"
        :user-name="nickname"
        @change="handleScopeChange"
      />

      <template v-if="scope === 'overview'">
        <OverviewHero :summary="summary" :stats="stats" @stat-click="handleStatClick" />
        <ActionCards :cards="cards" @card-click="handleCardClick" />
        <WidgetSection />
      </template>

      <div v-else :class="`${prefixCls}__empty`">
        <el-empty :description="emptyText" />
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { useDesign } from '@/hooks/web/useDesign'
import { useUserStore } from '@/store/modules/user'
import WorkbenchTopBar from './components/WorkbenchTopBar.vue'
import OverviewHero from './components/OverviewHero.vue'
import ActionCards from './components/ActionCards.vue'
import WidgetSection from './components/WidgetSection.vue'
import {
  scopeTabs,
  workbenchCards as cards,
  workbenchStats as stats,
  workbenchSummary as summary
} from './data/mockWorkbench'
import type { HomeScope, StatKey } from './data/types'

defineOptions({ name: 'Index' })

const { getPrefixCls } = useDesign()
const prefixCls = getPrefixCls('workbench')
const { info } = useMessage()

const scope = ref<HomeScope>('overview')

const userStore = useUserStore()
const nickname = computed(() => userStore.getUser?.nickname || '主理人')

const emptyText = computed(() => scopeTabs.find((tab) => tab.key === scope.value)?.emptyText ?? '')

const handleScopeChange = (value: string | number | boolean) => {
  scope.value = value as HomeScope
}

const STAT_TIP: Record<StatKey, string> = {
  approve: '「要你批」列表为演示数据，暂未接入后台',
  follow: '「要你跟」清单为演示数据，暂未接入后台',
  read: '消息中心为演示数据，暂未接入后台',
  orders: '订单进度为演示数据，暂未接入后台'
}

const handleStatClick = (key: StatKey) => info(STAT_TIP[key])

const CARD_TIP: Record<'approve' | 'follow' | 'read', string> = {
  approve: '审批中心为演示数据，暂未接入后台',
  follow: '待办清单为演示数据，暂未接入后台',
  read: '消息中心为演示数据，暂未接入后台'
}

const handleCardClick = (key: 'approve' | 'follow' | 'read') => info(CARD_TIP[key])
</script>

<style lang="scss" scoped>
$prefix-cls: #{$namespace}-workbench;

.#{$prefix-cls} {
  /* 沿用 oldweb 员工端工作台的主题变量名，作用域仅限本页，避免影响后台其它页面 */
  --primary-color: #0f172a;
  --primary-color-hover: #334155;
  --primary-color-light: #6366f1;
  --home-bg-gradient: linear-gradient(
    to bottom,
    #ebecee 0%,
    #e9e6ea 30%,
    #f5dde3 70%,
    #f5c6d1 100%
  );
  --border-radius-card: 24px;

  position: relative;
  min-height: calc(100vh - var(--tags-view-height) - var(--app-footer-height));
  padding: 8px 16px 48px;
  font-family: 'PingFang SC', 'Helvetica Neue', Roboto, sans-serif;
  color: #1d1d1f;
  background: var(--home-bg-gradient);

  &__aurora {
    position: absolute;
    inset: 0;
    pointer-events: none;
    background:
      radial-gradient(880px 420px at 10% -8%, rgb(99 102 241 / 16%), transparent 62%),
      radial-gradient(760px 380px at 94% 2%, rgb(245 198 209 / 55%), transparent 60%);
  }

  &__inner {
    position: relative;
    z-index: 1;
    max-width: 1600px;
    margin: 0 auto;
  }

  &__empty {
    display: flex;
    align-items: center;
    justify-content: center;
    min-height: 52vh;
    padding: 40px 0 120px;
  }
}

@media (width >= 768px) {
  .#{$prefix-cls} {
    padding: 12px 24px 56px;
  }
}

@media (width >= 1200px) {
  .#{$prefix-cls} {
    padding: 16px 32px 64px;
  }
}
</style>
