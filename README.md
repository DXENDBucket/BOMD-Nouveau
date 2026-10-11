# BOMD & Nouveau

把 Bosses of Mass Destruction 的攻击能力编排为 Ars Nouveau 魔符。
当前是 **0.1.0-dev.1，Minecraft 1.21.1 / NeoForge** 的第一批可测试实现。
Mod ID：`bomd_nouveau`。作者 EndXiom，许可 **LGPL v3**。

需要 Ars Nouveau、Ars Conflux 0.1.10 和 BOMD NeoForge 1.3.3，以及它们自己的前置。
Iron、Goety、Not Enough Glyphs、Hex Linker 均不是必需依赖。
公共源码保存在 `common/`；当前尚未制作 1.20.1 Forge 发行包。

## 首批魔符

| 魔符 | 类别 | 默认载荷耗蓝 | 行为 |
| --- | --- | ---: | --- |
| 巫妖飞弹 | 弹射物替换增强 | 40 | 默认 13.5 点基础伤害，保留原生缓慢 III / 5 秒 |
| 花瓣刃 | 弹射物替换增强 | 50 | 默认 18 点基础伤害 |
| 巫妖彗星 | 弹射物替换增强 | 100 | 原生威力 4 的爆炸 |
| 孢子球 | 弹射物替换增强 | 90 | 直接伤害、落地闪烁、1.5 秒后原生孢子喷发和中毒 |
| 彗星爆破 | 效果 | 80 | 在 Ars 触发位置执行同一份彗星爆炸 |
| 孢子喷发 | 效果 | 70 | 在 Ars 触发位置生成同一份延迟喷发 |

全部为 Tier 1；默认没有新增吟唱或冷却。强化的载荷附加费每次为 10 Mana，
原版 Ars 形态、增强及后续效果的费用另算。创造模式免载荷费用。

示例：`弹射物 → 巫妖飞弹 → 增强 → 伤害`，或 `触摸 → 孢子喷发`。
替换载荷不会吞掉后续效果。同一形态段只能选择一种弹射物替换，这是 Conflux 的共用规则。
环绕等反复命中的载体按实际触发逐次付费，空蓝会停止当前载荷及其后续编排。

飞弹、花瓣刃及孢子伤害读取对应 Boss 的配置基础值 ×1.5，以困难模式对玩家
造成的护甲结算前伤害为固定基准，不读取施法者的近战属性。
每级增强增加 `EffectHarm.INSTANCE.AMP_VALUE`，原版默认是 2；修改 Ars「伤害」配置后一起改变。
彗星增强每级增加 0.5 爆炸威力，爆炸伤害仍由 Minecraft 计算。
默认关闭彗星破坏地形，可配置开启。

在 Ars 抄写台消耗 **古老灵魂 / 虚空荆棘 ×1 + 魔源宝石 ×1 + 27 XP** 制作。
巫妖系列使用 `ancient_anima`，虚空之花系列使用 `void_thorn`。
使用原生魔符学习方式、原生配方及 JEI 页面；图标引用对应 BOMD 材料。

## 配置

世界的 `serverconfig/bomd_nouveau-server.toml` 控制六种载荷价格、增强附加费及彗星地形破坏。
不添加独立魔力池；使用 Conflux 的 Ars 账户和触发支付。

## 构建与许可

Java 21；相邻目录默认放置 Ars-Conflux 源码，也可用 `-PconfluxCheckout=...`。
将依赖放入本地 `libs/`，对应文件哈希见 `dependencies.lock.json`。
`./gradlew :jar :sourcesJar` 生成二进制和对应源码发行包。
`./gradlew :runGameTestServer` 运行本批服务端测试。

BOMD 作为单独依赖安装；本模组不打包其 JAR、贴图、模型或声音。
构建产物附带 LGPL、GPL、来源及修改声明，发行时同时提供对应源码。
详见 [原生行为审计](docs/native-payload-audit.md) 和 [第三方声明](THIRD_PARTY_NOTICES.md)。
