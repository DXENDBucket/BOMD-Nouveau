# BOMD & Nouveau dev.10：接入 Core 伤害 API

- 工作树：`parallel-workspaces/bomd-ars-damage/BOMD-Nouveau`，分支 `agent/bomd-ars-damage`；起点 `af84321`（dev.9）。
- 固定 Core：`dab8b9f6f1f92cfa0a78d56768f6a01a57901714`（0.1.18）；通过 confluxCheckout 指向 `parallel-workspaces/conflux-native-damage/Ars-Conflux`（Forge 指其 forge 子目录），旧 Core 副本受版本控制文件未改。
- 两版本同步为 dev.10，最低依赖 Core 0.1.18。原生钩子调用 NativeSpellDamage；删除重复伤害实现与 LivingEntity 爆炸包装器。原生伤害类型、数值、费用、冷却不改。
- Neo：`runGameTestServer build -PwithArsSpells=true -PwithCataclysm=true`，9/9 通过。实际灾变 dev.16／本体 3.33、Iron 3.16.3 和 Ars ’n Spells 3.3.5 同时加载；跨提供者作用域 + 灾变原生海浪命中得到 (9+6)*1.5=22.5，只有一组 Pre/Post。
- Forge：`runGameTestServer jar sourcesJar`，9/9 通过（跨灾变测试因平台无灾变附属而直接结束）；无 Iron／Ars ’n Spells，8 项真实 BOMD 行为检查通过。发行包已 reobfJar。
- 各平台 `build/payload-tests.log`；verify_release.py 两平台通过，检查配方、原生图标路径、LGPL、Java 字节码、Forge refmap、依赖下限及旧处理类未残留。
- `withCataclysm` 仅为独立可选 GameTest runtime，不是本附属前置；依赖 JAR 不打包。测试环境仅关闭 Ars ’n Spells 魔力统一，伤害桥接保持启用；不改人工实例配置。
- 客户端外观未人工重测。

## 产物 SHA-256

- `build/libs/bomd-nouveau-0.1.0-dev.10-mc1.21.1-neoforge.jar`：`383eb929b5bfee7d73c49c22135f4893497f7864641bd4e9fc8b5e9f13cc8beb`
- `build/libs/bomd-nouveau-0.1.0-dev.10-sources.jar`：`2c6c26968201a3b274a652467f6f8d421d3ec116845e076be993767503c52fc3`
- `forge/build/libs/bomd-nouveau-0.1.0-dev.10-mc1.20.1-forge.jar`：`7bcde4a7e39cd039ececf798064b3fb9553c968617c2217233be64ceb93f86be`
- `forge/build/libs/bomd-nouveau-0.1.0-dev.10-sources.jar`：`918c8a2e5fce5db0809bfb99572e5afc5ce10680866203216136e721c818419d`
