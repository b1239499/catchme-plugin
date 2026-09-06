# fkcvtp（Folia 1.21.8 相容）

隱身狀態與傳送、保護區繞過權限綁定在一起的巡查插件。

## 邏輯
- `/fkcvanish`：切換隱身。開啟時會隱藏你、附加暫時權限（傳送 + config.yml 設定的保護區 bypass 權限）。
  再次輸入 `/fkcvanish` 預設**不會**關閉隱身（除非你有 `vanishtp.forceoff` 權限），因為規格要求隱身只能透過
  `/tpa` 被接受，或 `/spawn` 才能解除。
- `/fkcvtp <玩家>`：只有在隱身狀態下才能使用，直接非同步傳送到對方身邊。
- 隱身期間會自動取得 config.yml 的 `bypass-permissions` 清單裡的權限節點，讓你能無視保護區的移動/傳送限制
  （實際能不能繞過，取決於你的保護區插件是不是用「權限節點」判斷 bypass，詳見下方）。
- `/tpa <玩家>`：發出傳送請求，對方用 `/tpaccept` 接受 或 `/tpdeny` 拒絕。
  **一旦被接受**，你會被傳送過去，且如果你原本是隱身狀態，會自動解除隱身。
- `/spawn`：傳送回設定好的重生點，同樣會自動解除隱身。
- `/fkcgod`：切換無敵狀態。開啟後完全免疫任何傷害來源；**只要你對其他生物造成傷害，就會自動解除無敵**
  （攻擊生物、玩家、用箭射人都算，只要真的打中造成傷害）。

## 權限節點
| 權限 | 說明 | 預設 |
|---|---|---|
| `vanishtp.use` | 可使用 `/fkcvanish` `/fkcvtp` `/tpa` 等指令 | false（需手動給指定玩家） |
| `vanishtp.see` | 即使對方隱身也能看到（給其他巡查員/管理員） | op |
| `vanishtp.forceoff` | `/fkcvanish` 可強制關閉自己的隱身，略過 tpa/spawn 限制 | false（預設沒人有，包含 OP） |
| `vanishtp.god` | 可以使用 `/fkcgod` 切換無敵狀態 | false（需手動給指定玩家） |

用 LuckPerms 舉例，給某玩家隱身巡查權限：
```
/lp user <玩家名> permission set vanishtp.use true
```

## config.yml 重點
```yaml
bypass-permissions:
  - "worldguard.region.bypass.*"
```
這裡填的是「隱身時暫時附加的權限節點」。**這部分一定要依你實際用的保護區/領地插件調整**：
- 如果你用 **WorldGuard**：`worldguard.region.bypass.*`（或 `worldguard.region.bypass.<世界名>`）
  本來就是官方的「無視所有 region 旗標」權限，包含 move / teleport 相關的旗標，符合你的需求。
- 如果你用的是其他保護區插件（例如自製的、或非 WorldGuard 系統），要去查它是否也提供「permission 形式」的
  bypass 節點；如果它是用白名單/黑名單或事件優先權硬擋、完全不看權限，這個做法就不會生效，需要另外對接
  它的 API（把它的 bypass 方法包一層，在 `VanishManager#vanish/unvanish` 裡呼叫）。

## Folia 相容性重點
- 完全沒有使用 `Bukkit.getScheduler()`（在 Folia 是被停用/no-op 的）。
- 玩家間傳送一律用 `Entity#teleportAsync(Location)`，這是 Paper 為 Folia 設計的跨 region 安全傳送 API，
  回傳 `CompletableFuture<Boolean>`，在 `thenAccept` 裡處理完成後續動作（發訊息、解除隱身）。
- 操作「別人」的 Player 物件（顯示/隱藏）一律透過 `player.getScheduler().run(...)`（Entity Scheduler）
  排程到該玩家自己所屬的 region 執行緒上執行，避免跨執行緒直接存取。
- 延遲任務（tpa 逾時）使用 `Bukkit.getAsyncScheduler().runDelayed(...)`，不影響任何 region 的 tick。

## 建置
```
mvn clean package
```
產生的 `target/fkcvtp.jar` 放到 `plugins/` 資料夾即可。

`pom.xml` 裡 `paper-api` 的版本號 (`1.21.8-R0.1-SNAPSHOT`) 請對照你伺服器實際使用的 build 版本調整，
若編譯抓不到該版本，去 https://repo.papermc.io 確認目前可用的版號。

## 已知需要你確認/調整的地方
1. **`/fkcvtp` 這個指令名稱**：目前已改為自訂名稱，一般不會跟 Essentials 等插件的 `/tp` 衝突。
   若之後還想改別的名字，把 `plugin.yml` 裡的 `fkcvtp:` 節點，以及 `FkcvtpPlugin.java` 中
   `getCommand("fkcvtp")` 那行的字串一起改掉即可，兩處要保持一致。
2. `bypass-permissions` 一定要對照你實際的保護區插件填寫，否則「隱身時無視保護區限制」不會真的生效。
3. 目前隱身用的是 Bukkit 內建的 `hidePlayer`/`showPlayer`，只處理「玩家可見度」，沒有處理聊天訊息前綴、
   Tab 清單、記分板等額外細節，如果你需要這些，可以再加。
