# Quest Clusters（任务集）

FTB Quests 附属：在同一章节内把任务、任务链接、章节图片组成「任务集」，收起时显示为一个长方形文字框。

- Minecraft 1.21.1 / NeoForge 21.1.x
- FTB Quests `[2101.1.36, 2101.2)`（编译目标 2101.1.38）
- 客户端和服务端都需要安装

## 当前进度：阶段 0–2、4、A、B、C、D、E、F、G（G1–G3）

## 模块（阶段 C 起为多模块仓库）

| 目录 | 模组 ID | 内容 | 状态 |
|---|---|---|---|
| `core/` | `quest_clusters` | 任务集、评分/结算页/排行榜、章节搜索、迁移框架（下文全部功能） | 已发布 |
| `visuals/` | `quest_clusters_visuals` | 任务书视觉增强，替代 Certain Questing Additions（见下）；声明与之不兼容；纯客户端 | 已发布 |
| `editor/` | `quest_clusters_editor` | 任务书编辑增强，替代 Quest Enhance（见下）；声明与之不兼容；客户端和服务端都要装 | 已发布（阶段 E 全部完成） |
| `filters/` | `quest_clusters_filters` | FTB 筛选系统的额外筛选，替代 Smart Filter Extras（见下）；声明与之不兼容 | 已发布 |
| `xmod/` | `quest_clusters_xmod` | FTB XMod Compat 的 FTB Quests 部分（见下）；与 XMod 共存时接管这部分 | 已发布（G1：筛选器、KubeJS；G2：JEI/REI；G3：Jade、文档、阶段） |
| `bundle/` | `quest_clusters_bundle` | 把所有已发布模块嵌套进一个 jar（jar-in-jar） | 已发布 |

发布页提供每个已发布模块的单独 jar，以及包含全部已发布模块的 `quest_clusters_bundle-*.jar`；装单独模块或 bundle 二选一。
模块在各自阶段完成后把 `<模块>/gradle.properties` 里的 `publish` 改为 `true`，就会被 bundle 收录、进入发布页和开发运行。

### 视觉模块（`visuals/`，替代 Certain Questing Additions 1.2.0.5 的行为，未复用其代码和素材）
- **动画**：任务悬停放大到 110% 并发光（0.2 秒），章节栏按钮悬停发光（0.25 秒），任务详情从下方 20px 滑入（0.3 秒），滚轮缩放平滑过渡，可选缓动。
  悬停动画包住原来的绘制而不是替换，其他附属在任务按钮上画的东西照常显示。
- **实体图标**：`entity:<实体ID>[选项]{NBT}`，可用于任务/章节图标、描述图片、章节图片。选项与 CQA 相同：`rotate`、`look`（看向鼠标）；玩家另有 `skin`、`display_name`、`show_name`、`uuid`；其他 `键=值` 合并进 NBT。
- **APNG**：文件名以 `.apng.png` 结尾的图片会播放动画（所有 FTB 图标处都有效）。示例：`quest_clusters_visuals:textures/gui/example.apng.png`。
- **章节着色器背景**：设置里开启 `shader_background` 后生效。资源包中 `assets/quest_clusters_visuals/chapter_shaders.json` 或（为 CQA 写的旧包）`assets/certain_questing_additions/chapter_shaders.json`：
  `{ "default": "命名空间:着色器", "chapters": { "<章节ID十六进制或文件名>": "命名空间:着色器" } }`；
  着色器为 core 着色器，可用 uniform：`size`、`scrollOffset`、`scrollSize`（像素）、`time`（秒）、`zoom`。为 CQA 写的着色器（包括 `#moj_import <certain_questing_additions:background_compat.glsl>`）可直接使用。
- **EMI**：从任务书打开的 EMI 配方界面关闭后回到任务书。
- **设置**：`/qcvisuals` 打开设置界面；文件 `local/quest_clusters_visuals/client-config.snbt`，键名与 CQA 相同。首次启动时如果存在 CQA 的 `local/certain_questing_additions/client-config.snbt`，会自动导入（原文件保留）。

### 编辑器模块（`editor/`，替代 Quest Enhance 3.13 的行为，未复用其代码和素材）
E1（本版）：读取并显示 Quest Enhance 任务书里的全部数据，在游戏里看起来和以前一样。
- **章节背景**（铺满任务界面，不随画布滚动）、**侧栏背景**（章节栏）、**任务框背景**（叠在任务形状上、轮廓和图标下面；任务链接不画）、**任务详情背景**。
- **装饰线**：从每条线的第一个节点连向其他节点；节点可以是任务、任务链接或**辅助点**（由本模块自己绘制成灰色圆点，不需要 Quest Enhance 的贴图）。
  玩家看不到的任务会让整条线对玩家隐藏；节点在收起的任务集里时线连到文字框。
- **隐藏前置线**：编辑模式下画成紫色；玩家模式下不画，设置了“悬停显示”时鼠标移到任一端才画。打开任务详情时不画原生连线（避免透到详情面板上）。
- **画布文字**（`text`/`text_v2`/`text_v3`：字体、缩放、旋转、颜色、透明度）；GIF 和视频元素在 E1 先显示为占位框（E2/E4 播放）。
- **章节前置任务**：前置任务全部完成前，玩家看不到这个章节（服务端和客户端都生效，编辑模式不受影响）。
- **禁用滚轮翻页**（按任务）、**框选时排除**（按章节图片）。

**从 Quest Enhance 迁移**：删掉 Quest Enhance 的 jar，装上本模块（或 bundle），启动一次即可。
服务器先把整个任务书备份到 `config/ftbquests/quest_clusters_backups/`，再把数据导入本模块自己的文件；原来的 `quest_enhance_*` 字段不动，FTB Quests 下次保存章节时会自己丢掉。
1.20 时代任务书里存在旧 `click` 字段中的画布文字和辅助点（FTB Quests 2101 读不出来，会变成普通图片），导入时一并恢复。
请在**同一次重启**里完成“删 Quest Enhance + 装本模块”：如果中间先不带本模块启动过、FTB Quests 又保存了章节，旧字段就没了，需要从 FTB Quests 的备份里恢复后再启动。
用真实任务书（21 章、547 条装饰线、697 个任务框背景、93 个辅助点）测试过，导入和显示与 Quest Enhance 一致。

E2（本版）：编辑界面（编辑模式下右键）。每次修改都由服务端检查权限，记入 FTB Quests 的撤销历史（Ctrl+Z / Ctrl+Y，修改日志里可见）。
- **章节**（右键章节栏里的章节）：设置章节背景图；章节前置任务（添加 / 逐个移除 / 清空，只能选其他章节的任务）。
- **侧栏**：右键章节栏顶部的任务书标题，设置侧栏背景图。
- **任务**：设置任务框背景图、任务详情背景图；开关“禁用滚轮翻页”；**编辑前置连线**：列出这个任务（或任务链接）的每条前置线，可设为显示 / 隐藏 / 隐藏但悬停时显示。
- **多选**：全是任务时可批量设置两种背景；选中的都是任务、任务链接或辅助点时可添加 / 移除装饰线（按选中顺序，第一个是中心）；**变换选区**：顺时针 / 逆时针旋转 90°、水平 / 垂直镜像（快捷键 Ctrl+Shift+R、Ctrl+Alt+R、Ctrl+Shift+M、Ctrl+Alt+M）。
- **画布空白处**：添加画布文字（文字、字体、缩放）、添加辅助点、添加 GIF（列出资源包中 `textures/` 下所有 `.gif`，自带示例 `quest_clusters_editor:textures/gui/example.gif`）；剪贴板里有多选复制的内容时可直接粘贴。
- **章节图片**：开关“框选时排除”；画布文字可再编辑，GIF 可更换。
- **多选复制粘贴**：选中多个任务 / 图片按 Ctrl+C，再 Ctrl+V 按相对位置整体粘贴（选区包含完整任务集时，任务集也会一起重建）；复制任务链接时复制它指向的任务。
- 复制辅助点时自动换新 ID，装饰线不会混淆。GIF 现在会播放动画。

E3（本版）：任务描述与图标。
- **描述表格**：`{quest_enhance_table:...}`（与 Quest Enhance 相同的格式，旧任务书里的表格原样显示）在任务详情里画成表格，支持合并单元格、表头、对齐、列宽、颜色、单元格换行。
  编辑模式下右键表格可编辑或删除；**可视化表格编辑器**：点击单元格输入（Shift+Enter 换行），拖动多选，右键合并 / 拆分、插入删除行列、单元格设置、表格设置，Ctrl+Z / Ctrl+Y 撤销重做。
- **描述 GIF**：`{quest_enhance_gif:<资源位置> width:.. height:.. align:.. fit:true text:..}`。另外任何以 `.gif` 结尾的图标字符串都会播放动画（章节图片、描述图片等）。
- **插入组件**：描述编辑器工具栏新增“+”按钮：网页链接、打开指定任务页、打开 Ponder（装了 Ponder 时）、点击复制、开发者命令（带确认）、网络图片、物品图标、物品提示、GIF、悬停文字、自定义字体、本地化文字、按键绑定、玩家持久数据、表格、乱码文字。
  在任务详情里编辑这些行时会重新打开对应的页面（不认识的行仍交给 FTB Quests 自己的编辑框）。
- **玩家持久数据占位符**：`{"translate":"quest_enhance.player_persistent_data","with":["key:<键>","i18n:false"]}` 显示该玩家持久数据里的值（服务端 NeoForge 数据，其次 KubeJS 数据）。命令 `/questclusters persistent set <玩家> <键> <值>`（也可用 `/questenhance persistent set ...`，旧脚本不用改）。
- **任务模型**：任务属性里的“模型”选一个实体，任务按钮、详情标题和完成提示都显示这个实体模型（存法与 Quest Enhance 相同：图标是刷怪蛋并带 `quest_enhance_entity_model`）。击杀任务、以及没有图标且只有一个击杀任务的任务，自动显示被击杀实体的模型。
- **进度任务**：左键点击进度任务打开原版进度界面并切到对应页。
- **任务目标“禁用提示”**：完成这个目标而完成整个任务时，任务本身也不弹提示。
- **批量编辑**：多选任务后“批量编辑任务”：编辑第一个，改动过的属性复制到其余任务；“批量添加任务目标”：给所有选中任务添加同一个目标。
- 客户端设置 `config/quest_clusters_editor-client.toml`：`render_kill_task_entity_models`、`open_advancement_tasks`（首次启动时从 Quest Enhance 的 `quest_enhance-client.toml` 导入）。

E4（本版）：脚本、视频、粘贴图片。
- **KubeJS**：绑定 `QuestUtils`、客户端事件 `QuestEnhanceEvents.click`（点击画布文字时触发），名字和方法与 Quest Enhance 相同，旧脚本不用改。
  所有修改都走 FTB Quests 自己的修改记录：会保存、同步给所有玩家，编辑模式里可撤销。Quest Enhance 的画布脚本函数在 1.21.1 上会把数据写进旧的 `click` 字段（被 FTB Quests 丢掉），这里已经修正。
  `descriptionItemIcon` 输出 FTB Quests 自己的物品图标写法（Quest Enhance 那种格式从来显示不出来）。
- **视频**（需要 WATERMeDIA 3.x，可选）：任务属性“节点视频”（任务按钮右下角出现播放按钮）、画布“视频背景”、描述里的视频链接（插入组件 → 视频）。
  播放器：进度条、播放 / 暂停 / 重播、倍速、音量、静音、全屏；空格、左右方向键、F、Esc。视频放在 `config/quest_clusters_editor/videos/`（也会读 Quest Enhance 的 `config/quest_enhance/assets/quest_enhance/videos/ftb/`），或资源包的 `<命名空间>:videos/`（写成 `assets/<路径>`）。
  没装 WATERMeDIA 时点击视频只会提示需要安装。注意：WATERMeDIA 3 改用 FFmpeg，接口与 Quest Enhance 用的 2.x 不同；实际解码播放没有在开发环境里测过。
- **粘贴剪贴板图片**：画布上 Ctrl+V（或右键“粘贴剪贴板图片”）把图片放成章节图片；描述编辑器里 Ctrl+V 插入图片行。Windows、macOS、Linux（需要 wl-paste 或 xclip）都支持。
  图片存到 `config/quest_clusters_editor/assets/quest_clusters_editor/textures/pasted/`，作为常开资源包加载，粘贴后立即显示；Quest Enhance 粘贴过的图片（`config/quest_enhance/`）照常显示。
  图片只在粘贴的那台电脑上：多人游戏请把文件放进资源包发给玩家。`/qceditor cleanup`（或 `/questenhance cleanup`）删除没有任何任务书引用的粘贴图片。

数据位置：`config/ftbquests/quests/quest_clusters/editor/`，`book.snbt`（侧栏背景）加每章一个 `<章节ID>.snbt`
（章节背景、前置任务、装饰线、隐藏线，以及当前在该章节里的任务和图片的设置）。画布文字和辅助点仍存在章节图片的 `click_action` 里（与 Quest Enhance 相同的格式），卸载本模块后它们只是普通图片。

### 筛选模块（`filters/`，替代 Smart Filter Extras 3.0.1 的行为，未复用其代码和素材）
给 FTB 筛选系统（FFS 21.1.4 起）的智能筛选器加四种筛选。**筛选写法与 Smart Filter Extras 完全相同**：筛选字符串存在物品、机器、管道和任务里，无法统一迁移，所以沿用原写法，两者写下的筛选可以互相读取。

| 筛选 | 写法 | 作用 |
|---|---|---|
| 物品列表 | `smart_filter_extras:items(minecraft:stone,minecraft:potion[minecraft:potion_contents={potion:"minecraft:healing"}])` | 任一物品（最多 256 项）；可以是具体变种（写法同 `/give`，只比较列出的组件）；不存在的 ID 保留但不匹配 |
| 物品标签（多选） | `smart_filter_extras:item_tag(c:ingots,c:gems)` | 属于任一标签（最多 64 个） |
| 方块标签 | `smart_filter_extras:block_tag(minecraft:logs,minecraft:crops)` | 放置的方块属于任一标签（种子会放置作物） |
| 燃料值 | `smart_filter_extras:fuel(>=1600)` | 熔炉燃烧时间（刻）比较，取自 NeoForge 的 `furnace_fuels` 数据 |

- **旧写法照常读取**：`smartfilterextras:*`（2.x）、`ftbfiltersystemaddons:block_tag`（1.0.0）、逗号写法 `item(a,b)`（1.0.0）；下次保存时改成新写法。单个物品的 `item(a)` 仍是 FFS 自己的筛选。旧 ID 不会在“添加”菜单里重复出现。
- **配置界面**：
  - 物品列表：网格（全部物品 / 背包两个页签），单击加入或移除、右键移除、双击选中并保存、Shift 单击 / 右键批量加入 / 移除当前显示的物品；“只看已选”“区分变种”开关；已选行翻页（滚轮也可）；复制、粘贴（ID 列表或筛选字符串）、清空；缺失 ID 黄色警告（单击移除）；`@模组` 搜索，装了 Just Enough Characters 时支持拼音；加入前检查整个筛选不超过 FFS 的 32767 字符上限。
  - 标签（物品 / 方块共用）：多选勾选、每个标签的数量、悬停预览前 27 个、按方块 / 物品反查、可直接输入未加载的长标签 ID、只看已勾选、复制 / 粘贴 / 全部取消。
  - 燃料值：比较符按钮、刻数（滚轮 ±100，Shift ±1）、“= 可烧 N 个”、预设（数值取自游戏数据）、实时匹配的燃料列表。
  - 通用：回车保存；有修改时按 Esc / 取消会先确认；在界面任意处打字进入搜索；搜索框未编辑时 Ctrl+C / Ctrl+V 作用于已选内容。
- **JEI**（可选）：配置界面旁显示 JEI 列表，可把物品拖进物品列表或反查格。EMI 的拖放也已注册（EMI 1.1.24 目前只在容器界面显示侧栏）。
- **卸载前**：`/qcfilters downgrade`（或 `/smart_filter_extras downgrade`）把主手筛选器里能转换的部分改写成 FFS 自带写法（物品列表 → `item` / `or(item...)` / `and(item component(fuzzy:...))`，物品标签 → `item_tag` / `or(item_tag...)`）；方块标签、燃料值没有等价写法，保持不变并提示。
- 服务端和客户端都要装（只有一端装时 NeoForge 会拒绝连接）。没装 FTB 筛选系统时本模块什么也不做，所以 bundle 不强制要求 FFS。

### 跨模组联动模块（`xmod/`，替代 FTB XMod Compat 21.1.14 的 FTB Quests 部分，未复用其代码）
- **与 XMod 共存**：装了 FTB XMod Compat 时，只关掉它的 FTB Quests 部分并由本模块提供，XMod 的其他功能（FTB Chunks、FTB Teams、阶段 / 权限 / 货币实现选择等）不变。
  接管针对 XMod 21.1.14 编写，模组信息里声明了版本范围 `[21.1.14,21.2)`：版本不对时启动时就会明确提示，而不是静默地重复注册。
- **任务里的筛选器**（G1）：FTB 任务的物品任务可以用 FTB 筛选系统的智能筛选器作为物品，背包里任一匹配的物品都会计入（Item Filters 模组没有 1.21.1 版本，所以只支持 FTB 筛选系统）。
- **KubeJS**（G1）：事件组 `FTBQuestsEvents`（`customTask`、`customReward`、`completed`、`started`、客户端 `customFilterItem`）、绑定 `FTBQuests`、`player.data.ftbquests`，名字和方法与 XMod 相同，脚本不用改。
  - `completed` / `started` 可按对象 ID 或 `#标签` 过滤；不带过滤的监听器每次只触发一次（XMod 对每个标签都会再触发一次）。
  - FTB 任务在客户端为本地副本也会触发这些事件，现在只把服务端真实的完成 / 开始交给服务端脚本。
- **JEI / REI**（G2）：任务和战利品箱作为配方显示，FTB 任务的“查看配方”和书签按键也走这里。有 JEI 时用 JEI，没有 JEI 而有 REI 时用 REI（顺序与 XMod 相同）。
  - “任务”分类：左边 3×3 是任务要求（物品任务按任务自身的匹配规则显示，筛选器展开成所有匹配物品，流体任务显示为流体，带数量），右边是奖励（物品奖励带数量；随机奖励表列出其内容，表格设了“隐藏提示”时只显示一个占位物品）。超过 9 项时最后一格轮播其余项并提示数量。点标题打开该任务。
  - 只显示队伍已能开始的任务（避免剧透）、不在“总是隐藏”章节里、没有勾选“在 JEI 中禁用”、且有可见奖励的任务，与 XMod 一致。
  - 查“用途”时按任务自己的匹配判断（标签、筛选器、组件），所以查铁锭能找到“任意铁锭标签”的任务；流体也能查用途（XMod 只支持物品）。
  - “战利品箱”分类：箱子、名称和各类生物掉落几率在上，内容在 8×6 网格里按稀有度排序，悬停显示几率；超过 48 种时轮播。每种战利品箱是单独的 JEI/REI 条目，任务书里增删箱子时条目随之更新。
  - 书签：JEI 书签；REI 收藏（XMod 的 REI 部分不支持）。装了 Too Many Recipe Viewers 时不提供 JEI 书签（与 XMod 相同）。
  - 分类 ID 仍是 `ftbquests:quest` / `ftbquests:loot_crate`，JEI/REI 里隐藏分类等设置不受影响；背景图用 FTB 任务自带的贴图。
  - 与 XMod 共存时，XMod 的配方查看器入口、它的 JEI 插件和 REI 插件都被关掉（XMod 的设置入口整体由本模块接管）。
- **Jade**（G3）：伪装成别的方块的任务屏障 / 阶段屏障，在不能编辑任务的玩家眼里名称、图标、所属模组都显示为伪装方块；编辑模式下显示真实方块，并额外显示由哪个任务 / 阶段打开、对自己是否已打开（XMod 不显示这些）。Jade 配置里的开关 ID 仍是 `ftbquests:barrier`。
- **文档链接**（G3）：FTB 任务图片点击动作“显示文档”（`<模组>,<书>[,<页>[#锚点]]`）支持 GuideME 和 Patchouli。
  - GuideME：会记住打开前的界面，关掉后回到任务书。
  - Patchouli：锚点是条目内的页码（为空或不是数字时打开第一页，XMod 此时什么都不做）；不写条目时打开整本书（XMod 会传空条目）；关掉后回到任务书。
- **阶段**（G3）：
  - 没装 XMod 时由本模块选择 FTB 库的阶段实现：装了 KubeJS 用 KubeJS 阶段，否则装了 Game Stages 用 Game Stages，否则保持 FTB 库默认（实体标签）。装了 XMod 时仍按 XMod 的配置选择。
  - 通过 FTB 库改变阶段（阶段奖励、命令等）以及 Game Stages 自己的阶段事件，会立即重新检查阶段任务；其他途径（例如 KubeJS 脚本直接改阶段）仍按 FTB 任务的每秒检查。XMod 21.1.14 里这部分（Game Stages 事件刷新）是注释掉的。
  - Game Stages 官方没有 1.21.1 版本；非官方移植 “Game Stages Reborn” 保留了模组 ID 和 API，本模块通过反射调用它，不把任何移植版作为构建依赖。**这条路径尚未实际运行测试**（找不到可下载的构建）。
- 与 XMod 共存时，XMod 的 Jade 屏障设置、GuideME 和 Patchouli 注册也被关掉；XMod 的阶段实现选择属于它的通用部分，保持不变。
- 开发测试：`-PdevModules=xmod -PwithKubejs -PwithXmod -PwithJei`，或用 `-PwithRei` 换成 REI。测试任务书里的 `recipe_test` 章节（含阶段任务）和 `qc_test_crate` 战利品箱专供这部分检查；`-PwithJade`、`-PwithDocs`（GuideME + Patchouli）加载 G3 相关模组。
  - 已知未解决：开发环境下 REI 里的流体格子显示为深色方块（JEI 显示正常，REI 自己查水的用途也能找到该任务），尚未确认是否为 REI 本身的问题。

### 迁移框架（`core/.../migration`）
- 模块用 `MigrationRegistry.register(...)` 注册一次性迁移；服务器读完任务书后、本附属加载自己的数据前执行。
- 迁移直接读磁盘上的原始章节文件（`MigrationContext.rawChapters()`），所以已卸载附属留下的字段仍能读到——FTB Quests 下次保存章节时会丢掉这些字段。
- 有待执行的迁移时，先把整个 `config/ftbquests/quests` 打包到 `config/ftbquests/quest_clusters_backups/`（保留最近 10 份），再执行。
- 成功的记录在 `quests/quest_clusters/migrations.snbt`，不再重复；失败的不记录，下次启动重试。
- `/questclusters migrate status` 查看，`/questclusters migrate backup` 手动备份（需要 OP）。

| 部分 | 位置 |
|---|---|
| 数据模型、校验、清理（纯 Java，可单测） | `data/` |
| 独立存档、服务端状态、权限检查 | `server/` |
| 网络包（全量同步 / 增量 / 编辑请求） | `net/` |
| 客户端副本 | `client/ClientClusters` |
| FTB Quests 生命周期挂钩（仅 HEAD/TAIL） | `mixin/ServerQuestFileMixin` |
| 收起的文字框、展开的区域框、汇总依赖线 | `client/gui/` |
| 玩家展开状态（按服务器/存档记忆） | `client/ClusterViewState`，存于 `<游戏目录>/quest_clusters/view_state.json` |

**玩家**：点击文字框展开，点击区域框标题栏收起；跳转到组内任务时自动展开。

**编辑模式**（`client/gui/ClusterEditor`）：
- 右键任务、任务链接或章节图片：创建任务集 / 加入任务集 / 移出任务集
- 多选后右键：用选中对象创建任务集 / 加入任务集 / 移出所属任务集
- 右键文字框或区域框标题栏：编辑属性（标题、副标题、文字框/标题/副标题/边框/区域框颜色、默认展开、文字框尺寸）、展开/收起、选中全部成员、
  文字框居中到成员、解散（保留任务）、删除任务集和全部成员（二次确认）
- 拖动文字框、区域框标题栏或区域框边缘：整组移动，按 1/8 格吸附，按住 Shift 不吸附（一步撤销）
- 用 FTB Quests 自己的方式移动成员（多选移动、方向键、撤销）时，如果全部成员一起移动，文字框会跟着移动
- 框选不会选中被收起的成员
- 右键任务集 →「设置背景图片…」：用 FTB Quests 的图片选择器选任意贴图，铺法可选拉伸 / 平铺 / 九宫格（边框不变形），可同时用于展开后的区域框

**任务集玩法**（全部基于 FTB Quests 原生的任务、前置和奖励，不改它的进度判定）：
- **「完成任务集」任务类型**：在画布空白处右键 → 选「完成任务集」，就会在那里新建一个带这个任务的「里程碑任务」。
  在任务设置里选任务集和需要完成的数量（0 = 全部）。组内任务完成到要求时它自动完成（每秒检查一次，登录时也检查）。
  只计任务（不计任务链接和图片），里程碑任务本身不计入。
  - **整组奖励**：给里程碑任务加奖励。
  - **整组作为前置**：让其他任务依赖里程碑任务。
- **整组解锁**：先选中要作为前置的任务，再右键任务集 →「把选中的任务设为所有成员的前置」（也可以批量移除）。
  和 FTB Quests 自己的「添加前置」一样，会产生循环的成员会被跳过并提示。

**章节搜索**：章节栏顶部的搜索框（Ctrl+F）。按任务标题、副标题、任务目标、描述和任务集名搜索，点击结果直接跳转（收起的任务集会自动展开）。
玩家看不到的任务和章节不会被搜到；「可开始前隐藏详情」的任务只按标题匹配。装了 Just Enough Characters 时支持拼音。

**评分、结算页、排行榜**（按队伍统计，根据 FTB Quests 的完成记录实时计算，不另存进度：重置进度、改分值、删任务后结果自动正确，旧存档直接可用）：
- 右键任务 →「评分与结算节点…」：分值（可为负）、结算节点文字（支持 FTB 文本格式）、「完成时打开结算页」。多选后可批量设分值。
- 结算页：任务书右上角的「结算」按钮，或 `/questclusters settlement [玩家]`，或完成设了「完成时打开结算页」的任务时自动打开。
  像片尾字幕一样滚动：标题、队伍名、开头文字、按完成顺序排列的节点、各章节得分、总分与百分比、评级、结尾文字。按住空格或鼠标加速，滚轮移动。
- 编辑模式下右键「结算」按钮编辑结算页：标题、开头/结尾文字、评级（每行 `百分比=名称`，如 `90=&6S`）、是否显示满分、是否启用排行榜。
- 排行榜：右上角按钮或 `/questclusters leaderboard`。按完成任务数、再按分数排序；玩家加入队伍后，其旧的个人队伍不重复计入。
- 数据存于 `config/ftbquests/quests/quest_clusters/scoring.snbt`；修改都可撤销。API/KubeJS：`setScore`、`getScore`、`teamScore`、`openSettlement`。

**标题格式**：标题和副标题与 FTB Quests 的任务标题用同一个解析器，支持 `&` 颜色/格式代码（如 `&6金色&l加粗`）、JSON 文本（如 `{"text":"矿石","color":"gold"}`）和 `{some.lang.key}` 翻译。

**动画**：文字框悬停时放大并发光，收起/展开时淡入。装了 CQA 时跟随它的「quest_hover」「quest_hover_glow_alpha」「smooth_animations」设置。

**KubeJS**（装了 KubeJS 时自动注册，全局名 `QuestClusters`；不装也能用 Java 调 `com.questclusters.api.QuestClustersAPI`）：
```js
// server_scripts：修改只能在服务端；跳过编辑模式权限检查，但校验和同步与编辑器相同
QuestClusters.create('0123456789ABCDEF', '{mypack.cluster.ores}', ['...', '...'])
QuestClusters.ofMember(questId)          // 所属任务集或 null
QuestClusters.inChapter(chapterId)       // 列表
QuestClusters.setMembers(clusterId, [...])
QuestClusters.setTitle(clusterId, '标题', '副标题')
QuestClusters.setDefaultExpanded(clusterId, false)
QuestClusters.dissolve(clusterId)
```

**撤销/重做**：创建、编辑、移动、解散、粘贴任务集都进 FTB Quests 自己的撤销历史（编辑模式的撤销/重做按钮），并显示在它的变更日志里。
拖动整个任务集是一步撤销。「删除任务集和成员」后撤销，任务和任务集都会恢复（被删除的成员 ID 恢复时会自动回到原任务集）。

已知限制：不装 Quest Enhance 时 FTB Quests 只能复制单个对象，所以不会带上任务集；KubeJS/API 的修改不进撤销历史。

### 兼容设计
- 收起的成员不从面板删除，只让 `shouldDraw/isEnabled` 返回 false：画布布局不跳动，FTB Quests 和 CQA 的连线渲染会自动跳过它们。
- 不注入 `QuestButton.draw`、`renderConnection`、`drawOffsetBackground`（CQA 和 Quest Enhance 会改写这些）。
- 汇总依赖线由我们自己的控件绘制：使用章节主题的连线纹理、粗细、流动速度和悬停高亮颜色，外观和原生连线一致（直线，不支持贝塞尔控制点）。

### 已实测的兼容
| 组合 | 版本 | 自检 |
|---|---|---|
| 不装联动附属 | — | 85 项通过（不含 visuals） |
| Certain Questing Additions | 1.2.0.5+mc1.21.1（Modrinth） | 80 项通过 |
| Quest Enhance | 3.12（从作者 GitHub 源码 `codex/1.21.1-neoforge` 编译） | 89 项通过 |
| visuals + Quest Enhance | 同上 | 105 项通过 |
| 只装 visuals | — | 96 项通过 |
| 两者同时安装（不含 visuals） | 同上 | 93 项通过（唯一的 ERROR 是测试故意制造循环前置时 FTB Quests 自己的提示） |
| 默认全部已发布模块（visuals + editor + filters，含 JEI） | — | 185 项通过 |
| 同上但不装 FTB 筛选系统 | — | 全部通过（filters 自动停用） |
| xmod + KubeJS + FTB XMod Compat 21.1.14 + JEI（接管测试） | KubeJS 2101.7.2，JEI 19.70.0.469 | 214 项全部通过：事件只触发一次、只有一个筛选适配器、只有本模块的两个 FTB 任务分类和配方查看器入口 |
| 默认全部已发布模块（含 xmod 配方和阶段检查） | — | 206 项通过 |
| xmod + KubeJS + Jade + GuideME + Patchouli（不装 XMod） | Jade 15.10.6，GuideME 21.1.22，Patchouli 1.21.1-93 | 全部通过：选用 KubeJS 阶段，加阶段后阶段任务立即完成，Jade 伪装 / 编辑视图正确 |
| 再加 FTB XMod Compat 21.1.14 + JEI | 同上 | 222 项全部通过：文档目标各只注册一次（本模块的），Jade 无重复提供者 |
| REI（不装 JEI），带和不带 FTB XMod Compat | REI 16.0.799 | 全部通过 |
| visuals + Quest Enhance（不带 editor，`-PskipModules=editor`） | 3.12 | 105 项通过 |

联动内容（都通过反射读取，对方不在或 API 变化时自动关闭，不会崩溃）：
- **Quest Enhance 单条前置线隐藏**：汇总线背后的真实连线全部被隐藏时，汇总线对玩家隐藏（设置了悬停显示则悬停时显示）；编辑模式下用 Quest Enhance 的隐藏线颜色画出。
- **Quest Enhance 多选复制粘贴**：选区完整包含一个任务集时，粘贴后会在新副本上重建这个任务集（位置按相同偏移）。
- **CQA 动画设置**：见上方「动画」。
- **Quest Enhance 装饰线**：只服务于组内连线的辅助点会跟着任务集一起移动（同一步撤销），收起时隐藏；收起后连到组内任务的装饰线改连到文字框。
  注意：同时装 CQA 时，CQA 会取消 Quest Enhance 的装饰线绘制（上游冲突，与本附属无关）。

## 存档格式

`config/ftbquests/quests/quest_clusters/<章节ID>.snbt`，每个章节一个文件，和 FTB Quests 自己的章节文件分开：
卸载本附属不会影响任务数据，FTB Quests 重新保存章节也不会丢掉任务集数据。

```
{
	version: 1
	chapter: "0123456789ABCDEF"
	clusters: [{
		id: "1A2B3C4D5E6F7081"
		title: "矿石处理"
		subtitle: "可选"
		color: "#FF3B4252"
		default_expanded: true
		x: 4.0d, y: 2.0d, width: 3.0d, height: 1.0d
		members: ["...", "..."]
	}]
}
```

规则：一个对象最多属于一个任务集；成员必须和任务集在同一章节。成员被删除、移到其他章节（包括撤销/重做）后，
会在下一个服务端 tick 自动移除；任务集没有成员或章节被删除时整体移除。

## 构建

```
./gradlew build                     # 编译全部模块 + 单元测试
./gradlew :core:runClient           # 开发客户端（core + 已发布模块）
./gradlew :core:runClientDevtest    # 脚本化界面自检：进入 core/run/saves/devtest，截图到 core/run/screenshots/qc_*.png，自动退出
./gradlew :core:runClient -PdevModules=xmod   # 额外加载未发布模块（例如检查不兼容声明）
./gradlew :core:runClientDevtest -PwithJei -PnoFfs   # 带 JEI 运行；不带 FTB 筛选系统运行
./gradlew :core:runClientDevtest -PskipModules=editor   # 不加载某个已发布模块（例如和 Quest Enhance 一起测）
./gradlew :editor:test -PlegacyBook=<任务书目录>         # 额外用一本本地任务书测试 Quest Enhance 导入
./gradlew :core:runClientTour -PtourBook=<任务书目录>    # 载入这本任务书，逐章截图到 core/run/screenshots/tour_*.png（下次 devtest 会恢复测试任务书）
scripts/devtest-matrix.sh                 # 依次跑所有模组组合的界面自检，最后输出汇总表
scripts/devtest-matrix.sh default rei     # 只跑指定组合；--list 列出全部组合
```

`devtest-matrix.sh` 的组合：`default`（已发布模块）、`core-only`、`no-ffs`、`addons`（KubeJS + Jade + GuideME + Patchouli）、`xmod-jei`（FTB XMod Compat 接管测试）、`rei`。
每个组合的日志在 `build/devtest-matrix/<组合>.log`，汇总在 `build/devtest-matrix/summary.md`。Gradle 在检查失败时也返回 0，所以脚本按日志末尾的 `ALL CHECKS PASSED` 判断；没有这一行（崩溃、超时）也算失败。
和 Quest Enhance、Certain Questing Additions 一起的组合需要手动放入它们的 jar，不在脚本里。
本机没有 `devtest` 存档时（例如新克隆的仓库），自检会按原测试世界的设置自动创建一个（生存、和平、不开作弊、固定种子）。

## 构建与发布

- 每次推送到 `main` 或提交 PR，GitHub Actions 会编译并运行单元测试，jar 作为构建产物上传（`.github/workflows/build.yml`）。
- 推送形如 `v0.2.0` 的标签会用这个版本号构建，并自动创建 GitHub Release 附上 jar（`.github/workflows/release.yml`）：
  ```
  git tag v0.2.0 && git push origin v0.2.0
  ```
- 界面自检也在 CI 里跑（`.github/workflows/devtest.yml`）：在虚拟显示器 + 软件 OpenGL 下启动真实客户端，上面六个组合并行各跑一次；日志、截图和崩溃报告作为构建产物上传，汇总表显示在运行页面上。

## 许可证

MIT，见 [LICENSE](LICENSE)。
