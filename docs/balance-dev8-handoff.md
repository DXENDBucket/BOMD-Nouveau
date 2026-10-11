# dev.8 费用调整交接

- 工作树：`parallel-workspaces/bomd-balance-dev8/BOMD-Nouveau`，分支 `agent/bomd-balance-dev8`。
- 固定 Conflux：`3eccd65fc569cd43ef960ef3eee77963ef201ac8`，受版本控制文件未修改。
- 共用默认值：彗星两种 200、孢子两种 150、激光每持续秒 120 Mana；伤害和冷却不变。
- 参考审计：`native-payload-audit.md` 的 dev.8 小节；共用资料在
  `MinecraftDev/Ars-Balance-Baselines/`，含实际版本/JAR 哈希、等级基准、配置快照和部署记录。

## 验证

NeoForge Java 21：`:jar :sourcesJar --offline --no-daemon` 成功。
Forge Java 17：`:jar :sourcesJar --offline --no-daemon` 成功，包含 `reobfJar`。
两个平台的 `tools/verify_release.py --artifacts-only`（Forge 加 `--forge`）通过，
包含 11 个配方、本体图标引用、本地化、许可、隔离及 Forge 重映射检查。
已核对配置解析、已有世界/新世界设置、部署文件哈希、每实例唯一 BOMD 附属版本。

这次只改默认费用，没有重跑 GameTest 或启动客户端。
Ars 'n Spells 新增配置的运行时属性叠加及各载荷伤害桥接覆盖范围尚未人工验证。

## 部署

“万法归一”部署 NeoForge dev.8，SHA-256：
`7adfa83c69a053ee8efacba3ab19ad4f340230e0c4d7b7d2ff84ad1ccfc56d11`。

“1.20.1-模组测试”部署 Forge dev.8，SHA-256：
`cfa280167fa4f9eb7297f2cff7bf7b9bc7817e0aaa28f8c3165b03be18af3266`。

旧 JAR/旧配置保留在各实例 `.codex-backups/`，新世界模板放 `defaultconfigs/`。
NeoForge 实例配置 Ars 'n Spells 为 `ars_primary`，开启双方属性桥接，
Iron & Nouveau 的 `use_iron_mana = false`；Forge 只同步 BOMD 费用。
