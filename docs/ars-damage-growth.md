## Ars 伤害成长（dev.10）

两版的联动伤害都接入 Ars `SPELL_DAMAGE_BONUS` 固定属性和 `SpellDamageEvent.Pre`／`Post`。每次原生伤害先加施法者的装备／药水属性，再交给 Pre；取消事件不造成伤害，事件可改伤害或来源，只有成功 hurt 发 Post。保留原生缓慢、中毒、穿盾、爆炸、无敌帧、伤害类型及既有强化，不改变费用／冷却或攻击数量。

直接飞弹／花瓣、延迟孢子、三种地面攻击、巨拳激光与彗星爆炸都携带原始 Ars 上下文。爆炸仅在联动调用作用域内接入，普通爆炸和原生 Boss 攻击不变；环境火焰或后续药水中毒不重复读取法术伤害属性。属性在实际命中时读取，延迟与持续攻击使用当前施法者属性。

无需 Iron 或 Ars ’n Spells；安装后者时按其原有 Ars 伤害事件桥接，不手动再叠一次 Iron 法强。NeoForge 的可选验证环境用 `-PwithArsSpells=true` 从忽略的 `libs/ars-growth` 读取本地依赖，不打包它们。该环境仅关闭魔力统一以维持测试池，伤害桥接启用；人工测试实例配置未改。

NeoForge 和 Forge 的伤害规则共享设计。通用伤害处理、事件总线和属性 Holder 差异由 Conflux 0.1.18 提供，新增代码不依赖 NeoForge 客户端类型。测试覆盖装备加成、一次事件修改／取消、延迟喷发、原生爆炸伤害源及作用域清理；孢子原生中毒可能继续扣血，因此测试核对 Post 的单次喷发伤害，不能把后续中毒算成重复加成。

公共接口为 `dev.arsconflux.api.damage.NativeSpellDamage`。附属显式 bind/context、hurt 和 during；不再安装自己的 LivingEntity 爆炸包装器。多个附属共用同一重入保护和作用域。旧版 Core 不支持新接口，因此两版最低依赖均提升到 0.1.18。
