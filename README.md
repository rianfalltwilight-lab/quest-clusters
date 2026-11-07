# Quest Clusters（任务集）

FTB Quests 附属：在同一章节内把任务、任务链接、章节图片组成「任务集」，收起时显示为一个长方形文字框。

- Minecraft 1.21.1 / NeoForge 21.1.x
- FTB Quests `[2101.1.36, 2101.2)`（编译目标 2101.1.38）
- 客户端和服务端都需要安装

## 当前进度：阶段 0–2、4 + 任务集玩法

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
| 不装联动附属 | — | 80 项通过 |
| Certain Questing Additions | 1.2.0.5+mc1.21.1（Modrinth） | 80 项通过 |
| Quest Enhance | 3.12（从作者 GitHub 源码 `codex/1.21.1-neoforge` 编译） | 89 项通过 |
| 两者同时安装 | 同上 | 88 项通过（唯一的 ERROR 是测试故意制造循环前置时 FTB Quests 自己的提示） |

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
./gradlew build      # 编译 + 单元测试
./gradlew runClient  # 开发客户端
./gradlew runClientDevtest  # 脚本化界面自检：进入 run/saves/devtest，截图到 run/screenshots/qc_*.png，自动退出
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
