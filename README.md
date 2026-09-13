# CatchMe

一個簡單的 Folia 安全「背玩家」插件。使用原版的乘客(passenger)機制，
完全同步在主執行緒/對應的 region 執行緒上運作，不使用 ProtocolLib 或任何
封包攔截，所以不會踩到你伺服器目前遇到的那種 `AsyncCatcher` /
`failed main thread check` Folia 相容性問題。

## 指令

- `/catch <玩家名稱>` — 發送背起某玩家的請求（需要在 8 格內、同世界）
- 對方會收到可點擊的 `[接受]` / `[拒絕]` 訊息，或手動輸入：
  - `/catch accept`
  - `/catch deny`
- `/uncatch` — **背著人的一方**或**被背的人**都可以輸入,放下/自己下來
  （被背的人也可以直接按蹲下鍵（Shift），原版乘客系統本來就支援中途下馬）

請求 30 秒沒回應會自動失效。

## 背動物

不用打指令，直接**蹲下 + 空手右鍵點擊動物**就能背起來（跟玩家不同，
動物沒得選，不用經過同意流程）。放下一樣用 `/uncatch`，或讓動物自然
死亡時系統會自動清除背負關係。

可以在 `config.yml` 的 `animal-pickup.allowed-types` 調整哪些生物允許
被背，預設包含豬、雞、兔子、羊、牛、哞菇菇、狼、貓、狐狸、鸚鵡、村民、
熊貓、烏龜。**末影龍、凋零、監守者、巨人這幾種寫死在程式碼裡永遠禁止**，
不管設定檔怎麼改都背不起來，避免有人拿來搞笑造成伺服器負擔或平衡問題。

背動物前也會自動檢查**地皮/領地保護**——如果動物所在的地方有裝
GriefPrevention、WorldGuard、Towny、PlotSquared 這類保護插件，且玩家
沒有權限對該區域的動物動手，就不能背走，會收到「這隻動物受到領地保護」
的提示。

## 權限

- `catchme.use`（預設所有人皆有）— 使用 `/catch` 與 `/uncatch`
- `catchme.animal.use`（預設所有人皆有）— 蹲下右鍵背起動物

## 為什麼這樣設計對 Folia 比較安全

1. **完全不用 ProtocolLib**，不攔截、不送自訂封包，避開你之前一路遇到的
   `ItemsAdder onPacketSending` 那個地雷。
2. 唯一用到的排程（請求逾時）只操作插件自己記憶體裡的 `Map`，不碰任何
   實體，所以用 `GlobalRegionScheduler` 執行完全安全。
3. `addPassenger` / `removePassenger` 都是在**指令執行當下**、也就是該
   玩家本來就所屬的 region 執行緒上呼叫，不會有跨執行緒存取實體的問題。
4. 加了距離檢查（預設 8 格）跟世界檢查，避免玩家在相距很遠、可能落在
   不同 region 的情況下嘗試背人。
5. 監聽了離線、死亡、被動甩下、傳送事件，確保背人關係不會卡在奇怪的
   殘留狀態（這正是你之前遇到 ExcellentCrates NPE 那種「動畫播到一半
   物件消失」問題的同類防呆）。

## 如何編譯

這個沙盒環境沒有對外連到 PaperMC 的 Maven 倉庫，沒辦法在這裡直接幫你
編譯出 `.jar`。請在你自己有網路的電腦上（裝好 Java 21 + Maven）執行：

```bash
mvn clean package
```

編譯完成後，`target/CatchMe.jar` 就是可以直接丟進
`/plugins` 資料夷的檔案。

如果你不想自己裝 Maven，也可以把整個資料夾丟進 IntelliJ IDEA
（安裝 Minecraft Development 外掛更方便），用 IDE 內建的 Maven 面板點
`package` 一樣能編譯出來。

## 之後可以擴充的方向

- 背人時降低移動速度（在 `startCarry` 裡對 carrier 加一個 slowness 效果）
- 支援疊人塔（連續呼叫 `addPassenger`，Bukkit 原生就支援乘客疊乘客）
- 用 PlaceholderAPI 顯示「目前正在背 XXX」
