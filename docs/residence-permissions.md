# Residence 权限系统文档

> 本文档用于 ResidenceList 插件权限管理 UI 的设计与实现参考。
> 基于 Residence 插件源码 `com.bekvon.bukkit.residence.containers.Flags` 枚举整理。

## 一、权限模式（FlagMode）

每个权限标志（Flag）都有一个 `FlagMode`，决定它可以通过哪些命令设置：

| FlagMode | 可使用的命令 | 作用对象 |
|----------|-------------|---------|
| `Residence` | `/res set` | 领地全局，影响所有玩家 |
| `Both` | `/res set` + `/res pset` + `/res gset` | 既可全局，也可针对个人/组 |
| `Player` | `/res pset` + `/res gset` | 只能针对个人/组 |

权限值三态：

- `true` — 允许/开启
- `false` — 拒绝/关闭
- `remove` / `neither` — 移除显式设置，回退到默认值或继承值

## 二、全局领地权限（`/res set`）— FlagMode: Residence

> 仅能全局设置，影响领地内所有玩家。

### 1. 建造与破坏

| 权限 | 说明 | 默认 |
|------|------|------|
| `craft` | 同时赋予 table、enchant、brew 权限 | true |

### 2. 交互与使用

> 本分类下 Residence 模式权限为空，全部在 Both 模式中。

### 3. 物品与掉落

| 权限 | 说明 | 默认 |
|------|------|------|
| `nodurability` | 阻止物品耐久损耗 | false |

### 4. 移动与传送

| 权限 | 说明 | 默认 |
|------|------|------|
| `wspeed1` | 改变玩家行走速度（%1） | true |
| `wspeed2` | 改变玩家行走速度（%2） | true |
| `jump2` | 允许跳 2 格高 | false |
| `jump3` | 允许跳 3 格高 | false |

### 5. 生物与实体

| 权限 | 说明 | 默认 |
|------|------|------|
| `animals` | 动物生成 | true |
| `canimals` | 自定义动物生成 | true |
| `cmonsters` | 自定义怪物生成 | true |
| `nanimals` | 自然动物生成 | true |
| `nmonsters` | 自然怪物生成 | true |
| `nomobs` | 阻止怪物进入（需 AutoMobRemoval） | true |
| `sanimals` | 刷怪蛋/刷怪笼动物生成 | true |
| `smonsters` | 刷怪蛋/刷怪笼怪物生成 | true |
| `monsters` | 怪物生成 | true |
| `creeper` | 苦力怕爆炸 | true |
| `dragongrief` | 阻止末影龙破坏方块 | true |
| `witherspawn` | 凋灵生成 | true |
| `phantomspawn` | 幻翼生成 | true |
| `witherdamage` | 凋灵伤害 | true |
| `witherdestruction` | 凋灵破坏方块 | true |
| `mobexpdrop` | 怪物掉落经验 | true |
| `mobitemdrop` | 怪物掉落物品 | true |
| `boarding` | 动物登船/上车 | true |
| `raid` | 触发原版袭击 | true |

### 6. 环境与物理

| 权限 | 说明 | 默认 |
|------|------|------|
| `anvilbreak` | 铁砧损坏 | true |
| `flow` | 液体流动 | true |
| `waterflow` | 水流（覆盖 flow） | true |
| `lavaflow` | 熔岩流（覆盖 flow） | true |
| `ignite` | 点火 | false |
| `explode` | 爆炸 | true |
| `tnt` | TNT 爆炸 | false |
| `piston` | 活塞推动/拉动方块 | true |
| `pistonprotection` | 活塞方块移入/移出领地 | true |
| `decay` | 树叶凋落 | true |
| `grow` | 作物生长 | true |
| `spread` | 方块蔓延 | true |
| `skulk` | 幽匿催化剂蔓延 | true |
| `iceform` | 冰形成 | true |
| `icemelt` | 冰融化 | true |
| `dryup` | 土地干涸 | true |
| `coraldryup` | 珊瑚干涸 | true |
| `copperoxidation` | 铜氧化 | true |
| `fallinprotection` | 阻止方块落入领地 | true |
| `flowinprotection` | 阻止液体流入领地 | true |
| `snowtrail` | 雪傀儡雪迹 | true |
| `trample` | 踩踏作物 | true |
| `golemopenchest` | 铁傀儡开箱 | true |
| `burn` | 怪物燃烧 | true |
| `fireball` | 火球 | true |
| `firespread` | 火焰蔓延 | true |

### 7. 战斗与保护

| 权限 | 说明 | 默认 |
|------|------|------|
| `pvp` | PvP | false |
| `damage` | 所有实体伤害 | false |
| `falldamage` | 摔落伤害 | true |
| `safezone` | 清除负面效果 | false |
| `shoot` | 发射投射物 | true |
| `snowball` | 雪球击退 | true |
| `hotfloor` | 岩浆块伤害 | true |
| `keepinv` | 死亡保留物品栏 | false |
| `keepexp` | 死亡保留经验 | false |
| `respawn` | 自动重生 | false |
| `healing` | 治疗 | true |
| `feed` | 饱食 | true |

### 8. 视觉效果

| 权限 | 说明 | 默认 |
|------|------|------|
| `day` | 领地内为白天 | true |
| `night` | 领地内为夜晚 | true |
| `rain` | 领地内下雨 | true |
| `sun` | 领地内晴天 | true |
| `glow` | 玩家进入发光 | true |
| `title` | 显示进/出提示 | true |
| `visualizer` | 粒子可视化 | true |
| `coords` | 隐藏坐标 | true |
| `hidden` | 隐藏领地 | false |

### 9. 经济与领地

| 权限 | 说明 | 默认 |
|------|------|------|
| `shop` | 商店领地 | true |
| `backup` | 备份还原（需 WorldEdit） | false |

---

## 三、双重权限（`/res set` + `/res pset`）— FlagMode: Both

> 既可全局设置（影响所有人），也可针对特定玩家设置。

### 1. 建造与破坏

| 权限 | 说明 | 默认 |
|------|------|------|
| `build` | 建造 | true |
| `place` | 放置方块（覆盖 build） | true |
| `destroy` | 破坏方块（覆盖 build） | true |
| `container` | 容器使用 | true |

### 2. 交互与使用

| 权限 | 说明 | 默认 |
|------|------|------|
| `use` | 门/拉杆/按钮等 | true |
| `door` | 门与活板门 | true |
| `button` | 按钮 | true |
| `lever` | 拉杆 | true |
| `pressure` | 压力板 | true |
| `diode` | 红石中继器 | true |
| `note` | 音符盒 | true |
| `table` | 工作台 | true |
| `enchant` | 附魔台 | true |
| `brew` | 酿造台 | true |
| `anvil` | 铁砧 | true |
| `beacon` | 信标 | true |
| `bed` | 床 | true |
| `cake` | 蛋糕 | true |
| `flowerpot` | 花盆 | true |
| `egg` | 龙蛋 | true |
| `honey` | 获取蜂蜜 | true |
| `honeycomb` | 获取蜜脾 | true |
| `copper` | 修改铜方块 | true |
| `brush` | 刷子 | true |
| `goathorn` | 山羊角 | true |
| `anchor` | 重生锚 | true |
| `commandblock` | 命令方块交互 | false |
| `command` | 在领地使用命令 | false |

### 3. 物品与掉落

| 权限 | 说明 | 默认 |
|------|------|------|
| `itemdrop` | 丢弃物品 | true |
| `itempickup` | 拾取物品 | true |

### 4. 移动与传送

| 权限 | 说明 | 默认 |
|------|------|------|
| `move` | 移动 | true |
| `tp` | 传送到领地 | true |
| `enderpearl` | 末影珍珠传送 | true |
| `chorustp` | 紫颂果传送 | true |
| `fly` | 飞行 | false |
| `nofly` | 禁飞 | false |
| `elytra` | 鞘翅使用 | false |

### 5. 生物与实体

| 权限 | 说明 | 默认 |
|------|------|------|
| `mobkilling` | 杀怪物 | true |
| `animalkilling` | 杀动物 | true |
| `vehicledestroy` | 破坏载具 | true |
| `vehicleplacing` | 放置载具 | true |
| `riding` | 骑乘 | true |
| `leash` | 拴绳 | true |
| `shear` | 剪羊毛 | true |
| `dye` | 染色 | true |
| `animalfeeding` | 喂养动物 | true |
| `nametag` | 命名牌 | true |
| `harvest` | 收获 | true |
| `trade` | 村民交易 | true |
| `hook` | 钓鱼竿钩实体 | true |

### 6. 经济与领地

| 权限 | 说明 | 默认 |
|------|------|------|
| `bank` | 领地银行 | true |
| `subzone` | 创建子领地 | true |
| `chat` | 加入领地聊天室 | true |

---

## 四、个人专属权限（`/res pset` / `/res gset`）— FlagMode: Player

> 只能针对特定玩家或权限组设置，不能全局设置。

| 权限 | 说明 | 默认 |
|------|------|------|
| `admin` | 允许玩家修改领地权限 | true |
| `friendlyfire` | 友军伤害 | false |

---

## 五、权限管理 UI 方案

### 5.1 入口

在 **Java 版 `ResidenceManageUI`** 和 **基岩版 `BedrockResidenceManageUI`** 中新增"权限管理"入口按钮。

仅当玩家为领地主人或拥有 `residence.admin` 权限时显示。

### 5.2 Java 版 UI 树

```
ResidenceManageUI（领地管理页）
├── [已有] 传送 / 编辑信息 / 公开私有 / 评价管理
└── [新增] 权限管理按钮 (slot 12)
     │
     ├── 全局权限设置 (/res set)
     │    └── ResidenceFlagUI（AutoPagedGUI，按分类分页）
     │         ├── 建造与破坏
     │         ├── 交互与使用
     │         ├── 物品与掉落
     │         ├── 移动与传送
     │         ├── 生物与实体
     │         ├── 环境与物理
     │         ├── 战斗与保护
     │         ├── 视觉效果
     │         └── 经济与领地
     │         （每页显示一个分类的 flag，每个 flag：左键=true，右键=false，Shift+左键=remove）
     │
     ├── 玩家权限管理 (/res pset)
     │    └── ResidencePlayerListUI（AutoPagedGUI）
     │         ├── [+ 添加信任玩家] → 铁砧输入玩家名 → /res padd
     │         ├── 玩家A（头颅）→ 点击进入
     │         │    └── ResidencePlayerFlagUI（AutoPagedGUI）
     │         │         └── 显示该玩家可设置的 Both + Player 类权限
     │         ├── 玩家B → ...
     │         └── [移除玩家全部权限] → /res pset <玩家> removeall
     │
     ├── 重命名领地 (/res rename)
     │    └── 铁砧输入新名称 → renameResidence()
     │
     ├── 镜像权限 (/res mirror)
     │    └── 列出自己拥有的其他领地 → 选择源领地 → applyTemplate()
     │
     ├── 进出提示消息 (/res message)
     │    ├── 设置进入消息（enter）→ 铁砧输入
     │    └── 设置离开消息（leave）→ 铁砧输入
     │
     └── 重置全部权限 (/res reset)
          └── 确认对话框 → applyDefaultFlags()
```

### 5.3 基岩版 UI 树

```
BedrockResidenceManageUI（领地管理页）
├── [已有] 编辑昵称 / 描述 / 图标 / 公开私有 / 传送 / 传送点 / 评价管理
└── [新增] 权限管理 (SimpleForm 按钮)
     │
     ├── 全局权限设置 (SimpleForm → 分类选择)
     │    └── 分类列表 (SimpleForm)
     │         └── 每个分类 → CustomForm（toggle 开关）
     │             （开=true，关=false，不支持 remove 态）
     │
     ├── 玩家权限管理 (SimpleForm)
     │    ├── 添加信任玩家 → CustomForm (input)
     │    ├── 玩家A → CustomForm（toggles）
     │    ├── 玩家B → CustomForm（toggles）
     │    └── 移除玩家全部权限 → SimpleForm 选择玩家 → ModalForm 确认
     │
     ├── 重命名领地 → CustomForm (input)
     ├── 镜像权限 → SimpleForm 选择源领地 → ModalForm 确认
     ├── 进出提示消息 → CustomForm（2 个 input）
     └── 重置全部权限 → ModalForm 确认
```

### 5.4 交互规则

#### Java 版

- 左键点击 flag 图标 → 设为 `true`
- 右键点击 flag 图标 → 设为 `false`
- Shift + 左键 → 设为 `remove`（移除显式设置）
- 视觉状态：
  - `true`：绿色玻璃 / 附魔光效
  - `false`：红色玻璃
  - `remove`（未设置）：灰色玻璃 / 屏障

#### 基岩版

- `CustomForm.toggle("权限名", 当前值)`
- 提交后根据 toggle 状态调用 API 设置为 `true` 或 `false`
- 不支持 `remove` 态，默认回退由 Residence 内部处理

### 5.5 对接的 Residence API

| 功能 | API |
|------|-----|
| 读取全局 flag | `res.getPermissions().has(Flags, FlagCombo.OnlyTrue/OnlyFalse)` |
| 设置全局 flag | `res.getPermissions().setFlag(sender, flag, FlagState, resadmin, inform)` |
| 读取玩家 flag | `res.getPermissions().playerHas(UUID, Flags, FlagCombo.OnlyTrue/OnlyFalse)` |
| 设置玩家 flag | `res.getPermissions().setPlayerFlag(sender, UUID, flag, FlagState, resadmin, show, checkFlagAccess)` |
| 添加信任玩家 | `setPlayerFlag(..., "trusted", FlagState.TRUE, ...)` |
| 移除玩家全部权限 | `res.getPermissions().removeAllPlayerFlags(sender, UUID, resadmin)` |
| 重置全部权限 | `res.getPermissions().applyDefaultFlags()` |
| 重命名 | `Residence.getInstance().getResidenceManager().renameResidence(sender, res, newName, resadmin)` |
| 镜像权限 | `target.getPermissions().applyTemplate(player, source.getPermissions(), resadmin)` |
| 进入消息 | `res.getEnterMessage()` / `res.setEnterMessage(msg)` |
| 离开消息 | `res.getLeaveMessage()` / `res.setLeaveMessage(msg)` |

---

## 六、实现文件清单

### 新增文件

| 文件 | 用途 |
|------|------|
| `plugin/.../utils/ResidenceFlagCategory.java` | 权限分类枚举（供双端共享） |
| `plugin/.../ui/ResidenceFlagUI.java` | Java 全局权限编辑 GUI |
| `plugin/.../ui/ResidencePlayerPermUI.java` | Java 玩家权限管理 GUI |
| `plugin/.../bedrock/BedrockPermissionUI.java` | 基岩版权限管理表单 |

### 修改文件

| 文件 | 改动 |
|------|------|
| `plugin/.../utils/ResidenceUtils.java` | 新增权限操作辅助方法 |
| `plugin/.../ui/ResidenceManageUI.java` | slot 12 新增"权限管理"按钮 |
| `plugin/.../bedrock/BedrockResidenceManageUI.java` | 新增"权限管理"按钮 |
