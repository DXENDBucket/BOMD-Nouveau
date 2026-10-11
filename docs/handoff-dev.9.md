# dev.9：BOMD Ars 伤害成长

- 固定 Core 0.1.16：`b24f18d72e64a902ff6c966040b0b83ea3cc6226`，受版本控制文件未改。
- 本轮开始于 `1bff9c3f7046f5dbf425d61af83eb2ba82777c3d`；本文件所在提交为最终功能提交。
- NeoForge 1.21.1：Ars 5.13.1／BOMD 1.3.3；Forge 1.20.1：Ars 4.12.7／BOMD 1.1.2。共享 VERSION 0.1.0-dev.9。

## 行为

原生载荷、孢子延迟喷发、三个地面攻击、激光及彗星爆炸获得施法者 Ars SPELL_DAMAGE_BONUS 固定加成，保留原有强化，再经过 Ars Pre／成功 Post。保留原生伤害类型／无敌帧／状态／穿盾／爆炸；只改绑定本附属上下文的攻击，普通原模组攻击和普通爆炸不变。每次原生伤害判定接入，数量、价格及冷却不改。

爆炸用短时作用域，结束和异常后立即清理；原生子攻击使用原始 Ars 上下文。事件可取消或修改，防止重复叠加装备与桥接。无需 Iron；可选 Ars ’n Spells 从其 Ars 事件获得法强，附属不重复读取 Iron 属性。平台总线与 Holder 差异从 Neo 模板生成 Forge 实现，未引入 Neo 客户端硬依赖。

## 验证

- NeoForge `:runGameTestServer :build -PwithArsSpells=true` 通过，全部 8 项 GameTest；真实 Ars ’n Spells 3.3.5／Iron 3.16.3 共存，测试仅关闭魔力统一以保留原测试池，伤害桥接开启，未改人工实例。
- Forge Java 17 `:runGameTestServer :jar :sourcesJar` 通过，全部 8 项 GameTest；该环境没有 Iron／Ars ’n Spells，验证可独立工作。
- 日志各平台 `build/payload-tests.log`。NOTICE 更新后仅补打包，没有为声明文件重新运行战斗测试。
- 新增检查覆盖装备属性及事件修改／取消、延迟孢子上下文、原生爆炸来源与作用域隔离。延迟孢子测试按 Post 单次喷发数值核对，避免把之后原生中毒的 2 点掉血误判为重复加成。
- `python tools/verify_release.py` 及 `--forge` 通过；配方、图标、语言、许可、发行／源码隔离与 Forge refmap 均检查。
- 原有客户端特效没有改动，仍未进行本轮人工客户端验证。保留 LGPL v3 和第三方声明，新增修改说明写入 NOTICE。

## 产物

- `build/libs/bomd-nouveau-0.1.0-dev.9-mc1.21.1-neoforge.jar`
  - SHA256 `d9c2cd7f4bf69ff4e1618e2f46818415d5320791387cbe9dd9d67cb396400ac3`
- `build/libs/bomd-nouveau-0.1.0-dev.9-sources.jar`
  - SHA256 `b770ac86c6b23f80bb2efb1c642bd2bbbe5cdfbec9968d230d2d078a08006c44`
- `forge/build/libs/bomd-nouveau-0.1.0-dev.9-mc1.20.1-forge.jar`
  - SHA256 `78f53a05c1d95545a2c3bc7bbbc64e6e908c0c401d3a5207124808b7f084b60d`
- `forge/build/libs/bomd-nouveau-0.1.0-dev.9-sources.jar`
  - SHA256 `4c42e5dda5fed99abc58a9520d409cf8acfbcf2afa12c83ab1741045c78548a0`

工作树：`parallel-workspaces/bomd-ars-damage/BOMD-Nouveau`。本地提交／标签后由协调者串行部署；不推送或发布远程。
