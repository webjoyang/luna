<script lang="tsx">
import { computed, defineComponent, provide } from 'vue'
import { useAppStore } from '@/store/modules/app'
import { Backtop } from '@/components/Backtop'
import { Setting } from '@/layout/components/Setting'
import { useRenderLunaLayout } from './useRenderLunaLayout'
import { useDesign } from '@/hooks/web/useDesign'
import { LUNA_AUTO_COLLAPSE_KEY, useLunaAutoCollapse } from './useLunaAutoCollapse'

const { getPrefixCls } = useDesign()

const prefixCls = getPrefixCls('layout')

const appStore = useAppStore()

// 是否是移动端
const mobile = computed(() => appStore.getMobile)

// 菜单折叠
const collapse = computed(() => appStore.getCollapse)

const handleClickOutside = () => {
  appStore.setCollapse(true)
}

export default defineComponent({
  name: 'LunaLayout',
  setup() {
    const autoCollapse = useLunaAutoCollapse()
    provide(LUNA_AUTO_COLLAPSE_KEY, autoCollapse)

    const { renderLuna } = useRenderLunaLayout(autoCollapse)
    return () => (
      <section class={[prefixCls, `${prefixCls}__classic`, 'w-[100%] h-[100%] relative']}>
        {mobile.value && !collapse.value ? (
          <div
            class="absolute left-0 top-0 z-99 h-full w-full bg-[var(--el-color-black)] opacity-30"
            onClick={handleClickOutside}
          ></div>
        ) : undefined}

        {renderLuna()}

        <Backtop></Backtop>

        <Setting></Setting>
      </section>
    )
  }
})
</script>

<style lang="scss" scoped>
$prefix-cls: #{$namespace}-layout;

.#{$prefix-cls} {
  background-color: var(--app-content-bg-color);
}
</style>
