# Quest Clusters（任务集）

FTB Quests 附属：在同一章节内把任务、任务链接、章节图片组成「任务集」，收起时显示为一个长方形文字框。

- Minecraft 1.21.1 / NeoForge 21.1.x
- FTB Quests `[2101.1.36, 2101.2)`（编译目标 2101.1.38）
- 客户端和服务端都需要安装

## 当前进度：阶段 0–2、4、A、B、C、D、E1

## 模块（阶段 C 起为多模块仓库）

| 目录 | 模组 ID | 内容 | 状态 |
|---|---|---|---|
| `core/` | `quest_clusters` | 任务集、评分/结算页/排行榜、章节搜索、迁移框架（下文全部功能） | 已发布 |
| `visuals/` | `quest_clusters_visuals` | 任务书视觉增强，替代 Certain Questing Additions（见下）；声明与之不兼容；纯客户端 | 已发布 |
| `editor/` | `quest_clusters_editor` | 任务书编辑增强，替代 Quest Enhance（见下）；声明与之不兼容；客户端和服务端都要装 | 已发布（E1：显示与导入；编辑界面在 E2） |
| `filters/` | `quest_clusters_filters` | 阶段 F：FTB Filter System 的额外筛选 | 占位，未发布 |
| `xmod/` | `quest_clusters_xmod` | 阶段 G：FTB XMod Compat 的 FTB Quests 部分（接管，不声明不兼容） | 占位，未发布 |
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

数据位置：`config/ftbquests/quests/quest_clusters/editor/`，`book.snbt`（侧栏背景）加每章一个 `<章节ID>.snbt`
（章节背景、前置任务、装饰线、隐藏线，以及当前在该章节里的任务和图片的设置）。画布文字和辅助点仍存在章节图片的 `click_action` 里（与 Quest Enhance 相同的格式），卸载本模块后它们只是普通图片。

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
| visuals + editor（默认全部已发布模块） | — | 116 项通过 |
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
./gradlew :core:runClient -PdevModules=filters,xmod   # 额外加载未发布模块（例如检查不兼容声明）
./gradlew :core:runClientDevtest -PskipModules=editor   # 不加载某个已发布模块（例如和 Quest Enhance 一起测）
./gradlew :editor:test -PlegacyBook=<任务书目录>         # 额外用一本本地任务书测试 Quest Enhance 导入
./gradlew :core:runClientTour -PtourBook=<任务书目录>    # 载入这本任务书，逐章截图到 core/run/screenshots/tour_*.png（下次 devtest 会恢复测试任务书）
```

## 构建与发布

- 每次推送到 `main` 或提交 PR，GitHub Actions 会编译并运行单元测试，jar 作为构建产物上传（`.github/workflows/build.yml`）。
- 推送形如 `v0.2.0` 的标签会用这个版本号构建，并自动创建 GitHub Release 附上 jar（`.github/workflows/release.yml`）：
  ```
  git tag v0.2.0 && git push origin v0.2.0
  ```
- 界面自检 `runClientDevtest` 需要图形环境，不在 CI 里跑，发布前在本机跑一次。

## 许可证

MIT，见 [LICENSE](LICENSE)。
