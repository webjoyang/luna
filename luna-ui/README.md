# luna 前端技术栈
| 技术栈 | 说明 | 版本 |
| :--- | :--- | :--- |
| Vue | Vue 框架 | 3.3.8 |
| Vite | 开发与构建工具 | 4.5.0 |
| Element Plus | Element Plus | 2.4.2 |
| TypeScript | JavaScript 的超集 | 5.2.2 |
| pinia | Vue 存储库 替代 vuex5 | 2.1.7 |
| vueuse | 常用工具集 | 10.6.1 |
| vue-i18n | 国际化 | 9.6.5 |
| vue-router | Vue 路由 | 4.2.5 |
| unocss | 原子 css | 0.57.4 |
| iconify | 在线图标库 | 3.1.1 |
| wangeditor | 富文本编辑器 | 5.1.23 |

## 🚀 快速启动（本地开发）

### 环境要求

| 组件      | 版本要求                            |
|---------|---------------------------------|
| Node.js | >= 20.19.0                      |
| pnpm    | >= 8.6.0（仓库使用 `pnpm-lock.yaml`，推荐） |
| npm     | >= 8.0.0（随 Node.js 一起安装，可选）      |

### 启动步骤

#### 方式一：pnpm（推荐）

1. 安装依赖：

   ```bash
   pnpm install
   ```

2. 启动开发服务：

   ```bash
   pnpm dev
   ```

#### 方式二：npm

1. 安装依赖：

   ```bash
   npm install
   ```

2. 启动开发服务：

   ```bash
   npm run dev
   ```

3. 浏览器访问 http://localhost 。

> 脚本定义在 `package.json` 的 `scripts` 中，`pnpm xxx` 与 `npm run xxx` 一一对应，
> 例如 `pnpm dev` 等价于 `npm run dev`，均执行 `vite --mode env.local`。
> 注意：npm 不会读取 `pnpm-lock.yaml`，依赖版本可能与 pnpm 安装结果存在差异，
> 团队协作建议统一使用 pnpm。

### 启动信息

| 项目      | 值                                                                   |
|---------|---------------------------------------------------------------------|
| 启动命令    | `pnpm dev` / `npm run dev`（等价于 `vite --mode env.local`）              |
| 服务端口    | `80`，由 `.env` 中的 `VITE_PORT` 控制                                     |
| 后端地址    | `http://localhost:48080`，由 `.env.local` 中的 `VITE_BASE_URL` 控制         |
| 接口前缀    | `/admin-api`，由 `.env.local` 中的 `VITE_API_URL` 控制                     |
| 默认登录信息  | 租户 `luna`，账号 `admin` / `admin123`，由 `.env` 中的 `VITE_APP_DEFAULT_LOGIN_*` 控制 |

> 本地开发同时使用 `.env`（通用配置）与 `.env.local`（本地环境变量）两份配置。

### 其他常用命令

| pnpm 命令            | npm 等价命令               | 说明                            |
|--------------------|------------------------|-------------------------------|
| `pnpm dev`         | `npm run dev`          | 本地环境启动开发服务                    |
| `pnpm dev-server`  | `npm run dev-server`   | 以 dev 环境启动开发服务                |
| `pnpm build:local` | `npm run build:local`  | 本地环境打包                        |
| `pnpm build:dev`   | `npm run build:dev`    | dev 环境打包                      |
| `pnpm build:test`  | `npm run build:test`   | test 环境打包                     |
| `pnpm build:stage` | `npm run build:stage`  | stage 环境打包                    |
| `pnpm build:prod`  | `npm run build:prod`   | 生产环境打包                        |
| `pnpm ts:check`    | `npm run ts:check`     | TypeScript 类型检查               |
| `pnpm lint`        | `npm run lint`         | ESLint + Stylelint + Prettier 检查 |

### 停止服务

- 前台运行时：在终端按 `Ctrl + C`（推荐，可确保 Vite 正常退出）。
- 后台运行时，可按端口号结束进程（`80` 为 `VITE_PORT` 默认值）：

  ```powershell
  # Windows PowerShell
  Get-NetTCPConnection -LocalPort 80 -State Listen | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }
  ```

  ```bash
  # Windows CMD
  netstat -ano | findstr :80
  taskkill /F /PID <PID>

  # Linux / macOS
  lsof -ti:80 | xargs kill -9
  ```

> 若修改过 `VITE_PORT`，请将上述命令中的端口替换为实际端口。

## 注意事项

- 启动前需先启动后端 `lunasrv-server`（见 `luna-srv/README.md`）。前端直接请求 `VITE_BASE_URL`，未走 Vite 代理，依赖后端已开启跨域支持。
- 若 80 端口被占用，修改 `.env` 中的 `VITE_PORT` 即可。
