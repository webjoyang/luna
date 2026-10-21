import pluginVue from 'eslint-plugin-vue'
import tseslint from 'typescript-eslint'
import unocss from '@unocss/eslint-config/flat'
import autoImportGlobals from './.eslintrc-auto-import.json' with { type: 'json' }

export default tseslint.config(
  // Global ignores (replaces .eslintignore)
  {
    ignores: [
      'build/',
      'config/',
      'dist/',
      'dist*/',
      '*.js',
      '*.mjs',
      '!eslint.config.mjs',
      'test/unit/coverage/',
      'node_modules/',
      'src/main.ts',
      'src/types/auto-components.d.ts',
      'src/components/Tinyflow/ui/**'
    ]
  },

  // Base TypeScript config
  ...tseslint.configs.recommended,

  // Vue recommended config
  ...pluginVue.configs['flat/recommended'],

  // UnoCSS config
  unocss,

  // Vue files use vue-eslint-parser with TypeScript parser
  {
    files: ['**/*.vue'],
    languageOptions: {
      parserOptions: {
        parser: tseslint.parser
      }
    }
  },

  // API 层架构边界守护：严禁从视图层逆向引入或重导出
  {
    files: ['src/api/**/*.{ts,js,mts,cts}'],
    rules: {
      'no-restricted-imports': [
        'error',
        {
          patterns: [
            {
              group: ['@/views/**', '**/views/**'],
              message: 'API 层严禁逆向引入或重导出 View 层模块（应保持 View 单向依赖 API/Model）'
            }
          ]
        }
      ]
    }
  },

  // 纯领域模型架构边界守护：严禁直接依赖 API 层、Axios 或 Router，确保纯 Node 单测环境无污染
  {
    files: ['src/views/**/model/**/*.{ts,js,mts,cts}'],
    rules: {
      'no-restricted-imports': [
        'error',
        {
          patterns: [
            {
              group: ['@/api/**', '**/api/**', '@/config/**', '@/router/**'],
              message: '纯领域模型（model/*.ts）严禁直接依赖 API 层、Axios 或 Router，确保纯 Node 单测环境无污染'
            }
          ]
        }
      ]
    }
  },

  // 移动子树架构边界守护：严禁引用 lunaerp-elmui/** PC 组件与 ElementPlus 业务组件（隔离依赖方向，保持 PC/H5 互不干扰，ADR-0045）
  {
    files: ['src/views/h5/**/*.{vue,ts,js,mts,cts}'],
    rules: {
      'no-restricted-imports': [
        'error',
        {
          patterns: [
            {
              group: ['@/views/lunaerp-elmui/**', '**/views/lunaerp-elmui/**'],
              message: '移动子树（/h5/**）严禁引用 lunaerp-elmui/** PC 视图与业务组件（ADR-0045 隔离依赖方向）'
            },
            {
              group: ['element-plus', 'element-plus/**'],
              message: '移动子树（/h5/**）严禁直接引用 ElementPlus 组件与类型（ADR-0045 移动端采用 Vant 4）'
            }
          ]
        }
      ]
    }
  },

  // 时区日期初始化守卫：严禁使用 toISOString() 截取业务日期（存在 UTC 时区偏移），统一使用 toLocaleDateString('en-CA') 或 dayjs()
  {
    files: ['src/views/lunaerp-elmui/**/*.{vue,ts,js,mts,cts}'],
    rules: {
      'no-restricted-syntax': [
        'error',
        {
          selector: "CallExpression[callee.property.name='toISOString']",
          message:
            "严禁使用 toISOString() 初始化业务日期（存在 UTC 偏移，东八区早晨会回退前一天），请使用 new Date().toLocaleDateString('en-CA') 或 dayjs().format('YYYY-MM-DD')"
        }
      ]
    }
  },

  // Main rules config
  {
    languageOptions: {
      ecmaVersion: 2020,
      sourceType: 'module',
      globals: {
        ...autoImportGlobals.globals
      },
      parserOptions: {
        ecmaFeatures: {
          jsx: true
        }
      }
    },
    rules: {
      // Vue rules
      'vue/no-reserved-component-names': 'off',
      'vue/custom-event-name-casing': 'off',
      'vue/attributes-order': 'off',
      'vue/one-component-per-file': 'off',
      'vue/html-closing-bracket-newline': 'off',
      'vue/html-indent': 'off', // Vue 模板缩进交给 Prettier，避免格式化规则互相拉扯。
      'vue/max-attributes-per-line': 'off',
      'vue/multiline-html-element-content-newline': 'off',
      'vue/singleline-html-element-content-newline': 'off',
      'vue/attribute-hyphenation': 'off',
      'vue/require-default-prop': 'off',
      'vue/require-explicit-emits': 'off',
      'vue/require-toggle-inside-transition': 'off',
      'vue/html-self-closing': [
        'error',
        {
          html: {
            void: 'always',
            normal: 'never',
            component: 'always'
          },
          svg: 'always',
          math: 'always'
        }
      ],
      'vue/multi-word-component-names': 'off',
      'vue/no-v-html': 'off',

      // TypeScript rules
      '@typescript-eslint/ban-ts-comment': 'off',
      '@typescript-eslint/explicit-function-return-type': 'off',
      '@typescript-eslint/no-explicit-any': 'off',
      '@typescript-eslint/no-empty-function': 'off',
      '@typescript-eslint/no-non-null-assertion': 'off',
      '@typescript-eslint/explicit-module-boundary-types': 'off',
      '@typescript-eslint/no-unused-vars': 'off',
      '@typescript-eslint/no-require-imports': 'off',
      '@typescript-eslint/no-unused-expressions': 'off',
      '@typescript-eslint/no-unsafe-function-type': 'off',
      '@typescript-eslint/no-wrapper-object-types': 'off',
      '@typescript-eslint/no-this-alias': 'off',
      '@typescript-eslint/no-empty-object-type': 'off',
      'vue/no-ref-as-operand': 'off',
      'vue/no-mutating-props': 'off',
      'vue/no-side-effects-in-computed-properties': 'off',
      'no-use-before-define': 'off',
      '@typescript-eslint/no-use-before-define': 'off',
      'no-unused-vars': 'off',
      'space-before-function-paren': 'off',

      // UnoCSS rules - 芋艿：禁用 unocss 顺序提示
      '@unocss/order': 'off',
      '@unocss/order-attributify': 'off',
      'unocss/order': 'off',
      'unocss/order-attributify': 'off'
    }
  }
)
