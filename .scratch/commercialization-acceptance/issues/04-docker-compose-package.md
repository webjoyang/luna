# 04: docker compose 私有化一键包（干净机 compose up 全绿）

**What to build:** 一台仅安装 Docker + compose v2 的干净 Linux 机器，clone 仓库、配置 docker.env、执行 `docker compose up -d`，四个服务全部健康、可登录、首屏九分组菜单完整——部署 gate 即通过。替换掉现有 docker 目录里的 MySQL8/Vue2 遗物。

**Blocked by:** 01, 02, 03（一键包启动后要跑 install 总入口与默认 seed，首屏才能完整）

**Status:** ready-for-agent

- [ ] 重写 `luna-srv/script/docker/`：四服务 postgres:15、redis:7、lunasrv-server(JDK17)、luna-ui(nginx 静态托管 + API 转发 + HTTPS 证书位)
- [ ] 干净机（2C4G 起）clone → 配置 docker.env → `compose up -d`：四服务健康检查通过、可登录、首屏 9 分组菜单与默认数据完整
- [ ] 清除全部 MySQL8/Vue2 旧配置残留（镜像、环境变量、卷、代理规则）
- [ ] nginx 含 HTTPS 证书位与 H5 摄像头 HTTPS 说明（HTTP 仅 localhost 可用）；门户/H5 域名转发正确
- [ ] 数据卷与备份入口可挂载；`compose down -v` 后经 install+seed 可重建全新验收库（07 号票重建脚本直接调用本包）
- [ ] docker.env 配置项有注释说明，含最低/推荐配置呼应 05 号安装文档
