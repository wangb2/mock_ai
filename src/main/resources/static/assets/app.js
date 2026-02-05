window.__appVersion = "appjs-v20260203-2";
console.log("app.js loaded:", window.__appVersion);
try {
  const badge = document.getElementById("appVersionBadge");
  if (badge) badge.textContent = (window.__indexVersion || "index") + " + " + window.__appVersion;
} catch (e) {}
const endpointInput = document.getElementById("endpoint");
      const fileInput = document.getElementById("fileInput");
      const sendBtn = document.getElementById("sendBtn");
      const copyBtn = document.getElementById("copyBtn");
      const refreshBtn = document.getElementById("refreshBtn");
      const output = document.getElementById("output");
      const status = document.getElementById("status");
      const items = document.getElementById("items");
      const history = document.getElementById("history");
      const uploadProgress = document.getElementById("uploadProgress");
      const docs = document.getElementById("docs");
      const mockUrlInput = document.getElementById("mockUrl");
      const mockBody = document.getElementById("mockBody");
      const mockResponse = document.getElementById("mockResponse");
      const aiLoadingOverlay = document.getElementById("aiLoadingOverlay");
      const sendMockBtn = document.getElementById("sendMockBtn");
      const ruleHint = document.getElementById("ruleHint");
      const debugMethodSelect = document.getElementById("debugMethodSelect");
      const methodToggle = document.getElementById("methodToggle");
      const debugCurl = document.getElementById("debugCurl");
      const headersTable = document.getElementById("headersTable");
      const queryTable = document.getElementById("queryTable");
      const responseHeadersTable = document.getElementById("responseHeadersTable");
      const addHeaderRow = document.getElementById("addHeaderRow");
      const addQueryRow = document.getElementById("addQueryRow");
      const reqBodyPanel = document.getElementById("reqBodyPanel");
      const reqHeadersPanel = document.getElementById("reqHeadersPanel");
      const reqQueryPanel = document.getElementById("reqQueryPanel");
      const respBodyPanel = document.getElementById("respBodyPanel");
      const respHeadersPanel = document.getElementById("respHeadersPanel");
      const debugStatus = document.getElementById("debugStatus");
      const debugTime = document.getElementById("debugTime");
      const debugSize = document.getElementById("debugSize");
      const copyCurlBtn = document.getElementById("copyCurlBtn");
      const apiSearch = document.getElementById("apiSearch");
      const apiCount = document.getElementById("apiCount");
      const apiSceneFilter = document.getElementById("apiSceneFilter");
      const manualCreateBtn = document.getElementById("manualCreateBtn");
      const refreshLogsBtn = document.getElementById("refreshLogsBtn");
      const logTableBody = document.getElementById("logTableBody");
      const logUpdatedAt = document.getElementById("logUpdatedAt");
      const totalEndpointCount = document.getElementById("totalEndpointCount");
      const todayNewEndpointCount = document.getElementById("todayNewEndpointCount");
      const logModal = document.getElementById("logModal");
      const closeLogModal = document.getElementById("closeLogModal");
      const logModalContent = document.getElementById("logModalContent");
      const jsonModal = document.getElementById("jsonModal");
      const closeJsonModal = document.getElementById("closeJsonModal");
      const jsonReq = document.getElementById("jsonReq");
      const jsonResp = document.getElementById("jsonResp");
      const jsonErr = document.getElementById("jsonErr");
      const editModal = document.getElementById("editModal");
      const closeEditModal = document.getElementById("closeEditModal");
      const confirmModal = document.getElementById("confirmModal");
      const closeConfirmModal = document.getElementById("closeConfirmModal");
      const confirmTitle = document.getElementById("confirmTitle");
      const confirmMessage = document.getElementById("confirmMessage");
      const confirmCancelBtn = document.getElementById("confirmCancelBtn");
      const confirmOkBtn = document.getElementById("confirmOkBtn");
      const editReq = document.getElementById("editReq");
      const editResp = document.getElementById("editResp");
      const editErr = document.getElementById("editErr");
      const editDelayMs = document.getElementById("editDelayMs");
      const editResponseMode = document.getElementById("editResponseMode");
      const editResponseScript = document.getElementById("editResponseScript");
      const editResponseScriptWrap = document.getElementById("editResponseScriptWrap");
      const saveEditBtn = document.getElementById("saveEditBtn");
      const sceneSelect = document.getElementById("sceneSelect");
      const sceneList = document.getElementById("sceneList");
      const sceneCreateBtn = document.getElementById("sceneCreateBtn");
      const sceneNameInput = document.getElementById("sceneNameInput");
      const sceneDescInput = document.getElementById("sceneDescInput");
      const sceneKeywordsInput = document.getElementById("sceneKeywordsInput");
      const sceneSaveBtn = document.getElementById("sceneSaveBtn");
      const sceneCancelBtn = document.getElementById("sceneCancelBtn");
      const sceneFormTitle = document.getElementById("sceneFormTitle");
      const sceneFormModal = document.getElementById("sceneFormModal");
      const closeSceneFormModal = document.getElementById("closeSceneFormModal");
      const sceneDetailModal = document.getElementById("sceneDetailModal");
      const closeSceneDetailModal = document.getElementById("closeSceneDetailModal");
      const sceneDetailList = document.getElementById("sceneDetailList");
      const sceneDetailMeta = document.getElementById("sceneDetailMeta");
      const sceneEndpointStats = document.getElementById("sceneEndpointStats");
      const manualModal = document.getElementById("manualModal");
      const closeManualModal = document.getElementById("closeManualModal");
      const manualTitleInput = document.getElementById("manualTitleInput");
      const manualMethodSelect = document.getElementById("manualMethodSelect");
      const manualSceneSelect = document.getElementById("manualSceneSelect");
      const manualReqHeadersTable = document.getElementById("manualReqHeadersTable");
      const manualReqQueryTable = document.getElementById("manualReqQueryTable");
      const manualReqBodyTable = document.getElementById("manualReqBodyTable");
      const manualRespHeadersTable = document.getElementById("manualRespHeadersTable");
      const manualRespBodyTable = document.getElementById("manualRespBodyTable");
      const manualErrBodyTable = document.getElementById("manualErrBodyTable");
      const manualErrStatusInput = document.getElementById("manualErrStatusInput");
      const manualDelayMsInput = document.getElementById("manualDelayMsInput");
      const manualReqJson = document.getElementById("manualReqJson");
      const manualRespJson = document.getElementById("manualRespJson");
      const manualErrJson = document.getElementById("manualErrJson");
      const manualTableWrap = document.getElementById("manualTableWrap");
      const manualJsonWrap = document.getElementById("manualJsonWrap");
      const manualModeTable = document.getElementById("manualModeTable");
      const manualModeJson = document.getElementById("manualModeJson");
      const manualModeManual = document.getElementById("manualModeManual");
      const manualModeChat = document.getElementById("manualModeChat");
      const manualChatWrap = document.getElementById("manualChatWrap");
      const manualChatHistory = document.getElementById("manualChatHistory");
      const manualChatInput = document.getElementById("manualChatInput");
      const manualChatGenerateBtn = document.getElementById("manualChatGenerateBtn");
      const manualChatClearBtn = document.getElementById("manualChatClearBtn");
      const manualChatProvider = document.getElementById("manualChatProvider");
      const manualRequired = document.getElementById("manualRequired");
      const manualSaveBtn = document.getElementById("manualSaveBtn");
      const manualResponseMode = document.getElementById("manualResponseMode");
      const manualResponseScript = document.getElementById("manualResponseScript");
      const manualResponseScriptWrap = document.getElementById("manualResponseScriptWrap");
      const debugScriptWrap = document.getElementById("debugScriptWrap");
      const debugScriptTextarea = document.getElementById("debugScriptTextarea");
      let currentDebugScriptMode = false;
      let currentDebugMockId = null;
      const manualAddReqHeader = document.getElementById("manualAddReqHeader");
      const manualAddReqQuery = document.getElementById("manualAddReqQuery");
      const manualAddReqBody = document.getElementById("manualAddReqBody");
      const manualAddRespHeader = document.getElementById("manualAddRespHeader");
      const manualAddRespBody = document.getElementById("manualAddRespBody");
      const manualAddErrBody = document.getElementById("manualAddErrBody");
      let editingMockId = null;
      let editingSceneId = null;
      let __scenesCache = null;
      let __historyCache = null;
      let __apiExpandedSceneId = ""; // for merged scene->endpoints accordion on api page (default: all collapsed)
      const uploadArea = document.getElementById("uploadArea");
      const endpointCallTop10 = document.getElementById("endpointCallTop10");
      let currentMethod = "POST";
      let selectedFile = null;

      function setStatus(text) {
        status.textContent = text;
      }

      let toastTimer = null;
      function showToast(message, type = "success", duration = 1200) {
        const el = document.getElementById("toast");
        if (!el) return;
        el.textContent = message || "";
        el.classList.remove("toast--success", "toast--error", "toast--copy");
        if (type === "error") el.classList.add("toast--error");
        else if (type === "copy") el.classList.add("toast--copy");
        else el.classList.add("toast--success");
        el.classList.add("show");
        if (toastTimer) clearTimeout(toastTimer);
        toastTimer = setTimeout(() => {
          el.classList.remove("show");
        }, duration);
      }

      // Theme toggle (dark/light)
      const __THEME_KEY__ = "uiThemeV1";
      function getPreferredTheme() {
        try {
          const saved = localStorage.getItem(__THEME_KEY__);
          if (saved === "light" || saved === "dark") return saved;
        } catch (e) {
          // ignore
        }
        try {
          if (window.matchMedia && window.matchMedia("(prefers-color-scheme: light)").matches) {
            return "light";
          }
        } catch (e) {
          // ignore
        }
        return "dark";
      }

      function applyTheme(theme) {
        const t = theme === "light" ? "light" : "dark";
        document.documentElement.setAttribute("data-theme", t);
        try {
          localStorage.setItem(__THEME_KEY__, t);
        } catch (e) {
          // ignore
        }
        const btn = document.getElementById("themeToggle");
        if (btn) {
          btn.setAttribute("aria-pressed", t === "light" ? "true" : "false");
          btn.textContent = t === "light" ? "深色" : "浅色";
        }
      }

      function initThemeToggle() {
        applyTheme(getPreferredTheme());
        window.addEventListener("pageshow", (e) => {
          if (e.persisted) applyTheme(getPreferredTheme());
        });
        const btn = document.getElementById("themeToggle");
        if (!btn) return;
        btn.addEventListener("click", () => {
          const current = document.documentElement.getAttribute("data-theme") || "dark";
          applyTheme(current === "light" ? "dark" : "light");
        });
      }

      function initSettingsMenu() {
        const settings = document.querySelector(".settings");
        const toggle = document.getElementById("settingsToggle");
        const menu = document.getElementById("settingsMenu");
        if (!settings || !toggle || !menu) return;
        const closeMenu = () => {
          if (!settings.classList.contains("open")) return;
          settings.classList.remove("open");
          toggle.setAttribute("aria-expanded", "false");
        };
        const openMenu = () => {
          settings.classList.add("open");
          toggle.setAttribute("aria-expanded", "true");
        };
        toggle.addEventListener("click", (event) => {
          event.stopPropagation();
          if (settings.classList.contains("open")) {
            closeMenu();
          } else {
            openMenu();
          }
        });
        menu.addEventListener("click", (event) => {
          event.stopPropagation();
          const loginLinkEl = document.getElementById("navLoginLink");
          if (!loginLinkEl || !loginLinkEl.contains(event.target)) return;
          const isLogout = (loginLinkEl.textContent || "").trim() === "退出";
          if (isLogout) {
            event.preventDefault();
            fetch("/auth/logout", { method: "POST" }).catch(() => {});
            window.location.replace("/login.html");
          }
        });
        document.addEventListener("click", (event) => {
          if (!settings.contains(event.target)) {
            closeMenu();
          }
        });
        document.addEventListener("keydown", (event) => {
          if (event.key === "Escape") {
            closeMenu();
          }
        });
      }

      // Cache uploaded files list for instant render after navigation
      const __UPLOADED_FILES_CACHE_KEY__ = "uploadedFilesCacheV1";

      function saveUploadedFilesCache(files) {
        try {
          if (!Array.isArray(files)) return;
          localStorage.setItem(__UPLOADED_FILES_CACHE_KEY__, JSON.stringify({
            ts: Date.now(),
            files
          }));
        } catch (e) {
          // ignore
        }
      }

      function loadUploadedFilesCache() {
        try {
          const raw = localStorage.getItem(__UPLOADED_FILES_CACHE_KEY__);
          if (!raw) return null;
          const parsed = JSON.parse(raw);
          if (!parsed || !Array.isArray(parsed.files)) return null;
          return parsed.files;
        } catch (e) {
          return null;
        }
      }

      // Carry debug preset across pages (multi-page navigation)
      const __DEBUG_PRESET_KEY__ = "debugPresetV1";
      // Home -> API: open manual entry modal after navigation
      const __MANUAL_OPEN_KEY__ = "manualOpenV1";

      function saveDebugPreset(item, errorMode) {
        try {
          sessionStorage.setItem(__DEBUG_PRESET_KEY__, JSON.stringify({
            ts: Date.now(),
            errorMode: !!errorMode,
            item: item || null
          }));
        } catch (e) {
          // ignore
        }
      }

      function consumeDebugPreset() {
        try {
          const raw = sessionStorage.getItem(__DEBUG_PRESET_KEY__);
          if (!raw) return null;
          sessionStorage.removeItem(__DEBUG_PRESET_KEY__);
          const parsed = JSON.parse(raw);
          if (!parsed || !parsed.item) return null;
          return parsed;
        } catch (e) {
          try { sessionStorage.removeItem(__DEBUG_PRESET_KEY__); } catch (_) {}
          return null;
        }
      }

      function saveManualOpenIntent() {
        try {
          sessionStorage.setItem(__MANUAL_OPEN_KEY__, JSON.stringify({ ts: Date.now() }));
        } catch (e) {
          // ignore
        }
      }

      function consumeManualOpenIntent() {
        try {
          const raw = sessionStorage.getItem(__MANUAL_OPEN_KEY__);
          if (!raw) return false;
          sessionStorage.removeItem(__MANUAL_OPEN_KEY__);
          return true;
        } catch (e) {
          try { sessionStorage.removeItem(__MANUAL_OPEN_KEY__); } catch (_) {}
          return false;
        }
      }

      function setSceneForm(scene) {
        if (!sceneNameInput || !sceneDescInput || !sceneKeywordsInput || !sceneFormTitle || !sceneSaveBtn || !sceneCancelBtn) {
          return;
        }
        if (!scene) {
          editingSceneId = null;
          sceneFormTitle.textContent = "新增场景";
          sceneSaveBtn.textContent = "保存场景";
          // 新增态：与“取消编辑”一致的按钮样式
          sceneSaveBtn.classList.add("secondary");
          sceneSaveBtn.classList.remove("btn-primary");
          sceneNameInput.value = "";
          sceneDescInput.value = "";
          sceneKeywordsInput.value = "";
          sceneCancelBtn.classList.add("hidden");
          return;
        }
        editingSceneId = scene.id;
        sceneFormTitle.textContent = "编辑场景";
        sceneSaveBtn.textContent = "保存修改";
        // 编辑态：保存修改按钮样式与“取消编辑”一致
        sceneSaveBtn.classList.add("secondary");
        sceneSaveBtn.classList.remove("btn-primary");
        sceneNameInput.value = scene.name || "";
        sceneDescInput.value = scene.description || "";
        sceneKeywordsInput.value = scene.keywords || "";
        sceneCancelBtn.classList.remove("hidden");
      }

      function openSceneForm(scene) {
        if (!sceneFormModal) return;
        setSceneForm(scene || null);
        sceneFormModal.classList.add("open");
        setTimeout(() => {
          if (sceneNameInput && typeof sceneNameInput.focus === "function") {
            try {
              sceneNameInput.focus({ preventScroll: true });
            } catch (e) {
              sceneNameInput.focus();
            }
          }
        }, 0);
      }

      function closeSceneForm() {
        if (sceneFormModal) sceneFormModal.classList.remove("open");
        setSceneForm(null);
      }

      async function loadChatConfig() {
        if (!manualChatProvider) return;
        try {
          const res = await fetch("/parse/endpoint/chat-config");
          if (!res.ok) return;
          const config = await res.json();
          if (config && config.defaultProvider) {
            manualChatProvider.value = config.defaultProvider;
          }
        } catch (err) {
          // ignore, use default value
        }
      }

      async function loadScenes(keepSelection = true) {
        if (!sceneList && !sceneSelect && !apiSceneFilter && !manualSceneSelect) return;
        const currentSelection = keepSelection ? sceneSelect.value : "";
        const apiSelection = keepSelection && apiSceneFilter ? apiSceneFilter.value : "";
        const manualSelection = keepSelection && manualSceneSelect ? manualSceneSelect.value : "";
        try {
          const res = await fetch("/parse/scenes");
          if (!res.ok) return;
          const list = await res.json();
          const scenes = sortScenes(Array.isArray(list) ? list : []);
          // 接口文档管理：不显示"请选择场景"占位项，直接默认选中
          if (sceneSelect) {
            updateSceneSelect(sceneSelect, scenes, currentSelection, "", false, false);
          }
          if (apiSceneFilter) {
            // 接口管理中的场景选择默认保持"全部场景"（空值），不自动选择
            updateSceneSelect(apiSceneFilter, scenes, apiSelection, "全部场景", true);
          }
          if (manualSceneSelect) {
            updateSceneSelect(manualSceneSelect, scenes, manualSelection, "请选择场景");
          }

          __scenesCache = scenes;
          if (isMergedApiPage()) {
            refreshMergedApiView();
            return;
          }
          if (sceneList) {
            renderSceneList(scenes);
          }
        } catch (err) {
          // ignore
        }
      }

      function isMergedApiPage() {
        return !!(window.__initialTab === "api" && sceneList && sceneList.classList && sceneList.classList.contains("merged"));
      }

      function refreshMergedApiView() {
        if (!isMergedApiPage()) return;
        if (!__scenesCache || !__historyCache) return;
        renderMergedSceneAccordion(__scenesCache, __historyCache);
      }

      function sortScenes(list) {
        const arr = Array.isArray(list) ? list.slice() : [];
        const isDefault = (s) => String((s && s.name) || "").includes("默认");
        const nameKey = (s) => String((s && s.name) || "").toLowerCase();
        arr.sort((a, b) => {
          const da = isDefault(a);
          const db = isDefault(b);
          if (da !== db) return da ? -1 : 1; // 默认场景放第一个
          const an = nameKey(a);
          const bn = nameKey(b);
          if (an < bn) return -1;
          if (an > bn) return 1;
          return 0;
        });
        return arr;
      }

      function updateSceneSelect(selectEl, list, selectedId = "", placeholder = "请选择场景", keepEmpty = false, includePlaceholder = true) {
        if (!selectEl) return;
        selectEl.innerHTML = includePlaceholder ? `<option value="">${placeholder}</option>` : "";
        list.forEach((scene) => {
          const option = document.createElement("option");
          option.value = scene.id;
          option.textContent = scene.name || "未命名场景";
          if (selectedId && selectedId === scene.id) {
            option.selected = true;
          }
          selectEl.appendChild(option);
        });
        // 如果 keepEmpty 为 true，保持为空（用于"全部场景"）
        if (!keepEmpty && !selectedId && list.length) {
          const defaultScene = list.find((scene) => String(scene.name || "").includes("默认"));
          if (defaultScene) {
            selectEl.value = defaultScene.id;
          } else {
            // 没有“默认”命名时，默认选第一个，避免必须手动选择
            selectEl.value = list[0].id;
          }
        }
      }

      function renderSceneList(list) {
        if (!sceneList) return;
        if (!list.length) {
          sceneList.innerHTML = "<div class='hint'>暂无场景，请先新增场景</div>";
          return;
        }
        if (isMergedApiPage()) {
          __scenesCache = list;
          refreshMergedApiView();
          return;
        }
        sceneList.innerHTML = "";
        const currentApiScene = apiSceneFilter ? String(apiSceneFilter.value || "") : "";
        list.forEach((scene) => {
          const keywords = String(scene.keywords || "")
            .split(",")
            .map((s) => s.trim())
            .filter(Boolean)
            .slice(0, 8);
          const div = document.createElement("div");
          div.className = "scene-item";
          div.setAttribute("data-scene-id", String(scene.id || ""));
          if (currentApiScene && String(scene.id || "") === currentApiScene) {
            div.classList.add("active");
          }
          div.innerHTML = `
            <div class="scene-head">
              <div>
                <div class="name">${escapeHtml(scene.name || "未命名场景")}</div>
                <div class="scene-desc">${escapeHtml(scene.description || "暂无描述")}</div>
              </div>
              <div class="scene-actions">
                <button class="action-btn" data-scene-detail="${escapeHtml(scene.id || "")}">详情</button>
                <button class="action-btn" data-scene-edit="${escapeHtml(scene.id || "")}">编辑</button>
                <button class="action-btn danger" data-scene-delete="${escapeHtml(scene.id || "")}">删除</button>
              </div>
            </div>
            <div class="scene-kws">
              ${keywords.length ? keywords.map((k) => `<span class="pill">${escapeHtml(k)}</span>`).join("") : `<span class="pill">无关键词</span>`}
            </div>
            <div class="meta">场景ID: ${escapeHtml(scene.id || "-")}</div>
          `;
          sceneList.appendChild(div);
        });

        sceneList.querySelectorAll("[data-scene-detail]").forEach((btn) => {
          btn.addEventListener("click", async () => {
            const id = btn.getAttribute("data-scene-detail");
            if (!id) return;
            await openSceneDetail(id);
          });
        });

        sceneList.querySelectorAll("[data-scene-edit]").forEach((btn) => {
          btn.addEventListener("click", () => {
            const id = btn.getAttribute("data-scene-edit");
            const scene = list.find((s) => s.id === id);
            if (scene) {
              openSceneForm(scene);
            }
          });
        });

        sceneList.querySelectorAll("[data-scene-delete]").forEach((btn) => {
          btn.addEventListener("click", async () => {
            const id = btn.getAttribute("data-scene-delete");
            if (!id) return;
            const ok = await openConfirmModal({
              title: "确认删除",
              message: "确定删除该场景吗？该场景下的接口将变为未归类。",
              okText: "确认删除",
              danger: true
            });
            if (!ok) return;
            try {
              btn.disabled = true;
              const res = await fetch(`/parse/scenes/${id}`, { method: "DELETE" });
              if (res.ok) {
                setStatus("已删除场景");
                showToast("删除成功", "success");
                closeSceneForm();
                await loadScenes(false);
                await loadHistory();
              } else {
                setStatus("删除失败");
                showToast("删除失败", "error");
              }
            } catch (err) {
              setStatus("删除异常");
              showToast("删除异常", "error");
            } finally {
              btn.disabled = false;
            }
          });
        });
      }

      function renderMergedSceneAccordion(scenes, historyItems) {
        if (!sceneList) return;
        const keyword = (apiSearch ? apiSearch.value.trim().toLowerCase() : "");

        const allItems = Array.isArray(historyItems) ? historyItems : [];

        // Filter items by keyword (same rule as existing renderHistory)
        const filteredItems = keyword
          ? allItems.filter((item) => {
              const text = `${item.title || ""} ${item.mockUrl || ""} ${item.sourceFileName || ""} ${item.sceneName || ""}`.toLowerCase();
              return text.includes(keyword);
            })
          : allItems;

        // Group by sceneId (empty string = unclassified)
        const bySceneId = new Map();
        filteredItems.forEach((it) => {
          const sid = String(it.sceneId || "");
          if (!bySceneId.has(sid)) bySceneId.set(sid, []);
          bySceneId.get(sid).push(it);
        });

        const getSceneCount = (sid) => (bySceneId.get(String(sid || "")) || []).length;
        const totalCount = filteredItems.length;
        if (apiCount) apiCount.textContent = String(totalCount);

        const rows = [];
        (Array.isArray(scenes) ? scenes : []).forEach((scene) => {
          const sid = String(scene.id || "");
          rows.push({ kind: "scene", scene, sid, count: getSceneCount(sid) });
        });
        const unclassifiedCount = getSceneCount("");
        if (unclassifiedCount) {
          rows.push({
            kind: "unclassified",
            scene: { id: "", name: "未归类", description: "没有归入任何场景的接口", keywords: "" },
            sid: "",
            count: unclassifiedCount
          });
        }

        sceneList.innerHTML = "";
        if (!rows.length) {
          sceneList.innerHTML = "<div class='hint'>暂无场景，请先新增场景</div>";
          return;
        }

        rows.forEach((row) => {
          const scene = row.scene;
          const sid = row.sid;
          const count = row.count;
          const keywords = String(scene.keywords || "")
            .split(",")
            .map((s) => s.trim())
            .filter(Boolean)
            .slice(0, 8);

          const wrap = document.createElement("div");
          wrap.className = "scene-item scene-acc";
          wrap.setAttribute("data-scene-id", sid);
          const expanded = !!__apiExpandedSceneId && String(__apiExpandedSceneId) === String(sid || "");
          wrap.classList.toggle("open", expanded);

          const actionsHtml =
            row.kind === "unclassified"
              ? ``
              : `
                <button class="action-btn" data-scene-detail="${escapeHtml(scene.id || "")}">详情</button>
                <button class="action-btn" data-scene-edit="${escapeHtml(scene.id || "")}">编辑</button>
                <button class="action-btn danger" data-scene-delete="${escapeHtml(scene.id || "")}">删除</button>
              `;

          wrap.innerHTML = `
            <div class="scene-head scene-acc-head">
              <div style="min-width:0;">
                <div class="name">
                  ${escapeHtml(scene.name || "未命名场景")}
                  <span class="scene-acc-count">${count} 个接口</span>
                </div>
                <div class="scene-desc">${escapeHtml(scene.description || "暂无描述")}</div>
              </div>
              <div class="scene-actions">
                ${actionsHtml}
                <span class="scene-acc-caret" aria-hidden="true"></span>
              </div>
            </div>
            <div class="scene-kws">
              ${keywords.length ? keywords.map((k) => `<span class="pill">${escapeHtml(k)}</span>`).join("") : `<span class="pill">无关键词</span>`}
            </div>
            <div class="scene-acc-body">
              <div class="scene-acc-body-inner" data-endpoints-wrap="1"></div>
            </div>
          `;

          const head = wrap.querySelector(".scene-acc-head");
          if (head) {
            head.addEventListener("click", async (e) => {
              if (e && e.target && e.target.closest && e.target.closest(".scene-actions")) return;
              const isOpen = wrap.classList.contains("open");
              // single-open accordion
              sceneList.querySelectorAll(".scene-acc").forEach((el) => el.classList.remove("open"));
              if (!isOpen) {
                wrap.classList.add("open");
                __apiExpandedSceneId = sid;
                renderMergedEndpointsInto(wrap, sid, bySceneId);
              } else {
                __apiExpandedSceneId = "";
              }
            });
          }

          // initial endpoints render for default open
          if (expanded) {
            renderMergedEndpointsInto(wrap, sid, bySceneId);
          }

          const detailBtn = wrap.querySelector("[data-scene-detail]");
          if (detailBtn) {
            detailBtn.addEventListener("click", async (e) => {
              e.stopPropagation();
              const id = detailBtn.getAttribute("data-scene-detail");
              if (!id) return;
              await openSceneDetail(id);
            });
          }
          const editBtn = wrap.querySelector("[data-scene-edit]");
          if (editBtn) {
            editBtn.addEventListener("click", (e) => {
              e.stopPropagation();
              const id = editBtn.getAttribute("data-scene-edit");
              const s = (Array.isArray(scenes) ? scenes : []).find((x) => String(x.id) === String(id));
              if (s) openSceneForm(s);
            });
          }
          const delBtn = wrap.querySelector("[data-scene-delete]");
          if (delBtn) {
            delBtn.addEventListener("click", async (e) => {
              e.stopPropagation();
              const id = delBtn.getAttribute("data-scene-delete");
              if (!id) return;
              const ok = await openConfirmModal({
                title: "确认删除",
                message: "确定删除该场景吗？该场景下的接口将变为未归类。",
                okText: "确认删除",
                danger: true
              });
              if (!ok) return;
              try {
                delBtn.disabled = true;
                const res = await fetch(`/parse/scenes/${id}`, { method: "DELETE" });
                if (res.ok) {
                  setStatus("已删除场景");
                  showToast("删除成功", "success");
                  closeSceneForm();
                  await loadScenes(false);
                  await loadHistory();
                } else {
                  setStatus("删除失败");
                  showToast("删除失败", "error");
                }
              } catch (err) {
                setStatus("删除异常");
                showToast("删除异常", "error");
              } finally {
                delBtn.disabled = false;
              }
            });
          }

          sceneList.appendChild(wrap);
        });
      }

      function renderMergedEndpointsInto(sceneWrap, sceneId, bySceneId) {
        if (!sceneWrap) return;
        const host = sceneWrap.querySelector("[data-endpoints-wrap]");
        if (!host) return;
        host.innerHTML = "";
        const list = (bySceneId && bySceneId.get(String(sceneId || ""))) || [];
        if (!list.length) {
          host.innerHTML = "<div class='hint' style='margin-top:10px;'>该场景暂无接口</div>";
          return;
        }
        list.forEach((item) => {
          host.appendChild(buildApiCard(item));
        });
      }

      async function openSceneDetail(sceneId) {
        if (!sceneDetailModal || !sceneDetailList) return;
        sceneDetailList.innerHTML = "<div class='hint'>加载中...</div>";
        if (sceneDetailMeta) {
          sceneDetailMeta.textContent = "";
        }
        try {
          const res = await fetch(`/parse/scenes/${sceneId}/endpoints`);
          if (!res.ok) {
            sceneDetailList.innerHTML = "<div class='hint'>加载失败</div>";
            return;
          }
          const list = await res.json();
          const items = Array.isArray(list) ? list : [];
          if (sceneDetailMeta) {
            sceneDetailMeta.textContent = `共 ${items.length} 个接口`;
          }
          if (!items.length) {
            sceneDetailList.innerHTML = "<div class='hint'>该场景暂无接口</div>";
          } else {
            sceneDetailList.innerHTML = items.map((item) => {
              const method = (item.method || "").toUpperCase() || "POST";
              const mockUrl = toAbsoluteUrl(item.mockUrl || "");
              return `
                <div class="doc-item">
                  <div>
                    <div class="name">${escapeHtml(item.title || "API")}</div>
                    <div class="meta">方法: ${escapeHtml(method)} | ${escapeHtml(mockUrl)}</div>
                  </div>
                  <div>
                    <button class="action-btn" data-copy="${escapeHtml(mockUrl)}">复制地址</button>
                  </div>
                </div>
              `;
            }).join("");
            sceneDetailList.querySelectorAll("[data-copy]").forEach((btn) => {
              btn.addEventListener("click", async () => {
                const value = btn.getAttribute("data-copy") || "";
                try {
                  await navigator.clipboard.writeText(value);
                  setStatus("已复制");
                  showToast("复制成功", "copy");
                } catch (err) {
                  setStatus("复制失败");
                  showToast("复制失败", "error");
                }
              });
            });
          }
        } catch (err) {
          sceneDetailList.innerHTML = "<div class='hint'>加载异常</div>";
        } finally {
          sceneDetailModal.classList.add("open");
        }
      }

      function openConfirmModal({ title = "请确认操作", message = "", okText = "确认", danger = false } = {}) {
        return new Promise((resolve) => {
          if (!confirmModal || !confirmOkBtn || !confirmCancelBtn || !confirmTitle || !confirmMessage) {
            // Fallback (shouldn't happen)
            resolve(false);
            return;
          }
          confirmTitle.textContent = title;
          confirmMessage.textContent = message;
          confirmOkBtn.textContent = okText;
          confirmOkBtn.classList.toggle("danger", !!danger);

          const cleanup = () => {
            confirmModal.classList.remove("open");
            confirmOkBtn.removeEventListener("click", onOk);
            confirmCancelBtn.removeEventListener("click", onCancel);
            if (closeConfirmModal) closeConfirmModal.removeEventListener("click", onCancel);
            confirmModal.removeEventListener("click", onBackdrop);
            document.removeEventListener("keydown", onKeydown);
          };
          const onOk = () => {
            cleanup();
            resolve(true);
          };
          const onCancel = () => {
            cleanup();
            resolve(false);
          };
          const onBackdrop = (e) => {
            if (e.target === confirmModal) onCancel();
          };
          const onKeydown = (e) => {
            if (e.key === "Escape") onCancel();
          };

          confirmOkBtn.addEventListener("click", onOk);
          confirmCancelBtn.addEventListener("click", onCancel);
          if (closeConfirmModal) closeConfirmModal.addEventListener("click", onCancel);
          confirmModal.addEventListener("click", onBackdrop);
          document.addEventListener("keydown", onKeydown);
          confirmModal.classList.add("open");
        });
      }

      async function handleSend() {
        const endpoint = endpointInput && endpointInput.value ? endpointInput.value.trim() : "";
        const file = (fileInput && fileInput.files && fileInput.files[0]) || selectedFile;
        const sceneId = sceneSelect ? sceneSelect.value.trim() : "";

        const finalEndpoint = endpoint || "/parse/endpoint";
        if (!sceneId) {
          setStatus("请选择场景");
          showToast("请先选择场景", "error");
          return;
        }
        if (!file) {
          setStatus("请选择文档");
          return;
        }

        sendBtn.disabled = true;
        copyBtn.disabled = true;
        setStatus("解析中...");
        if (uploadProgress) {
          uploadProgress.style.display = "block";
        }
        output.textContent = "{}";
        if (items) {
          items.innerHTML = "";
        }
        if (history) {
          history.innerHTML = "";
        }

        const formData = new FormData();
        formData.append("file", file);
        formData.append("sceneId", sceneId);

        try {
          const res = await fetch(finalEndpoint, {
            method: "POST",
            body: formData
          });
          const text = await res.text();
          const parsed = parseJsonLoose(text);
          
          if (!res.ok) {
            setStatus("请求失败：" + res.status);
            output.textContent = text;
            return;
          }
          
          // 异步模式：立即返回，显示上传成功
          if (parsed && parsed.fileId) {
            setStatus("文件上传成功，正在排队处理...");
            showToast("文件上传成功，正在处理中", "success");
            sendBtn.disabled = false;
            copyBtn.disabled = false;
            if (uploadProgress) {
              uploadProgress.style.display = "none";
            }
            // 刷新文件列表
            await loadUploadedFiles();
            // 切换到文档管理标签页
            switchTab("doc");
            return;
          }
          
          // 兼容旧模式（同步处理）
          if (parsed) {
            output.textContent = JSON.stringify(parsed, null, 2);
            if (parsed && Array.isArray(parsed.items)) {
              renderItems(parsed.items);
            }
          } else {
            output.textContent = prettyPrintFallback(text);
          }
          setStatus("完成");
          copyBtn.disabled = false;
          await loadHistory();
          switchTab("api");
        } catch (err) {
          const msg = String((err && err.message) || err || "").toLowerCase();
          // Upload parsing may take long; when network/proxy aborts you'll often see "Failed to fetch"
          if (
            msg.includes("failed to fetch") ||
            msg.includes("networkerror") ||
            msg.includes("network error") ||
            msg.includes("timeout") ||
            msg.includes("timed out") ||
            msg.includes("aborterror") ||
            msg.includes("aborted")
          ) {
            setStatus("AI模型正在处理请求，请稍后再试");
            output.textContent = "";
          } else {
            setStatus("请求异常：" + ((err && err.message) || err));
          }
          output.textContent = "";
        } finally {
          sendBtn.disabled = false;
          if (uploadProgress) {
            uploadProgress.style.display = "none";
          }
        }
      }

      sendBtn.addEventListener("click", handleSend);
      if (fileInput) {
        fileInput.addEventListener("change", () => {
          selectedFile = fileInput.files && fileInput.files.length ? fileInput.files[0] : null;
          const fileNameDisplay = document.getElementById("fileNameDisplay");
          if (selectedFile && fileNameDisplay) {
            fileNameDisplay.textContent = selectedFile.name;
            fileNameDisplay.classList.add("show");
          } else if (fileNameDisplay) {
            fileNameDisplay.classList.remove("show");
          }
        });
      }
      refreshBtn.addEventListener("click", loadHistory);
      
      // 页面加载时初始化配置
      (async function init() {
        await loadChatConfig();
        await loadScenes();
        await loadHistory();
      })();
      sendMockBtn.addEventListener("click", sendMockRequest);
      if (refreshLogsBtn) {
        refreshLogsBtn.addEventListener("click", () => loadLogs(true));
      }
      if (closeLogModal) {
        closeLogModal.addEventListener("click", () => logModal.classList.remove("open"));
      }
      if (closeJsonModal) {
        closeJsonModal.addEventListener("click", () => jsonModal.classList.remove("open"));
      }
      if (closeEditModal) {
        closeEditModal.addEventListener("click", () => editModal.classList.remove("open"));
      }
      if (closeManualModal) {
        closeManualModal.addEventListener("click", () => manualModal.classList.remove("open"));
      }
      if (closeSceneDetailModal) {
        closeSceneDetailModal.addEventListener("click", () => sceneDetailModal.classList.remove("open"));
      }
      if (sceneDetailModal) {
        sceneDetailModal.addEventListener("click", (e) => {
          if (e.target === sceneDetailModal) {
            sceneDetailModal.classList.remove("open");
          }
        });
      }

      if (uploadArea) {
        uploadArea.addEventListener("click", (e) => {
          if (e.target === uploadArea || e.target.closest('.upload-area') && !e.target.closest('.custom-file-upload')) {
            fileInput.click();
          }
        });
        uploadArea.addEventListener("dragover", (e) => {
          e.preventDefault();
          uploadArea.classList.add("dragover");
        });
        uploadArea.addEventListener("dragleave", () => {
          uploadArea.classList.remove("dragover");
        });
        uploadArea.addEventListener("drop", (e) => {
          e.preventDefault();
          uploadArea.classList.remove("dragover");
          const files = e.dataTransfer.files;
          if (files && files.length) {
            selectedFile = files[0];
            const fileNameDisplay = document.getElementById("fileNameDisplay");
            if (fileNameDisplay) {
              fileNameDisplay.textContent = selectedFile.name;
              fileNameDisplay.classList.add("show");
            }
            try {
              fileInput.files = files;
            } catch (err) {
              // ignore assignment errors in some browsers
            }
            handleSend();
          }
        });
      }
      copyBtn.addEventListener("click", async () => {
        try {
          await navigator.clipboard.writeText(output.textContent);
          setStatus("已复制结果");
          showToast("复制成功", "copy");
        } catch (err) {
          setStatus("复制失败");
          showToast("复制失败", "error");
        }
      });

      function renderItems(list) {
        if (!items) {
          return;
        }
        if (!list.length) {
          return;
        }
        list.forEach((item) => {
          items.appendChild(buildApiCard(item));
        });
      }

      async function loadHistory() {
        history.innerHTML = "";
        // Don't clear docs here: docs list is driven by /parse/uploaded-files
        // and clearing causes a blank delay on the doc page.
        try {
          const res = await fetch("/parse/endpoint/history");
          if (!res.ok) {
            console.error("Failed to load history:", res.status, res.statusText);
            return;
          }
          const json = await res.json();
          if (!json || !Array.isArray(json.items)) {
            console.error("Invalid history response:", json);
            return;
          }
          console.log("Loaded history items:", json.items.length);
          __historyCache = json.items;
          renderHistory(json.items);
          refreshMergedApiView();
        } catch (err) {
          console.error("Error loading history:", err);
        }
      }

      function renderHistory(list) {
        if (isMergedApiPage()) {
          // merged UI renders via scene accordion
          if (apiCount) apiCount.textContent = String((Array.isArray(list) ? list.length : 0));
          return;
        }
        history.innerHTML = "";
        if (!list.length) {
          history.innerHTML = "<div class='hint'>暂无历史数据</div>";
          if (apiCount) apiCount.textContent = "0";
          return;
        }
        const keyword = (apiSearch ? apiSearch.value.trim().toLowerCase() : "");
        const filtered = keyword
          ? list.filter((item) => {
              const text = `${item.title || ""} ${item.mockUrl || ""} ${item.sourceFileName || ""} ${item.sceneName || ""}`.toLowerCase();
              return text.includes(keyword);
            })
          : list;
        const sceneId = apiSceneFilter ? apiSceneFilter.value.trim() : "";
        const filteredByScene = sceneId
          ? filtered.filter((item) => String(item.sceneId || "") === sceneId)
          : filtered;
        if (apiCount) apiCount.textContent = String(filteredByScene.length);
        filteredByScene.forEach((item) => {
          history.appendChild(buildApiCard(item));
        });
      }

      // 加载上传文件列表（带状态）
      async function loadUploadedFiles() {
        if (!docs) return;
        // Show immediate feedback to avoid blank area while loading.
        if (!docs.innerHTML || !docs.innerHTML.trim()) {
          const cached = loadUploadedFilesCache();
          if (cached && cached.length) {
            renderDocs(cached);
          } else {
            docs.innerHTML = "<div class='hint'>加载中...</div>";
          }
        }
        try {
          const res = await fetch("/parse/uploaded-files");
          if (!res.ok) {
            console.error("Failed to load uploaded files:", res.status);
            return;
          }
          const files = await res.json();
          saveUploadedFilesCache(files);
          renderDocs(files);
        } catch (err) {
          console.error("Error loading uploaded files:", err);
        }
      }
      
      function renderDocs(filesList) {
        if (!docs) {
          return;
        }
        if (!filesList || !filesList.length) {
          docs.innerHTML = "<div class='hint'>暂无上传记录</div>";
          return;
        }
        docs.innerHTML = "";
        const list = document.createElement("div");
        list.className = "doc-list";
        filesList.forEach((file) => {
          const div = document.createElement("div");
          div.className = "doc-item";
          const downloadUrl = toAbsoluteUrl(file.fileUrl || "");
          const status = file.status || "PENDING";
          const statusText = {
            "PENDING": "未处理",
            "PROCESSING": "处理中",
            "COMPLETED": "处理完成",
            "FAILED": "处理失败"
          }[status] || status;
          const statusClass = {
            "PENDING": "status-pending",
            "PROCESSING": "status-processing",
            "COMPLETED": "status-completed",
            "FAILED": "status-failed"
          }[status] || "";
          const generatedCount = file.generatedCount || 0;
          const errorMessage = file.errorMessage || "";
          div.innerHTML = `
            <div>
              <div class="name">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" style="margin-right: 8px; flex-shrink: 0;">
                  <path d="M14 2H7a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8l-5-6Z" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/>
                  <path d="M14 2v6h6" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/>
                  <path d="M9 13h6M9 17h6" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                ${escapeHtml(file.fileName || "文件")}
              </div>
              <div class="meta">
                文件ID: ${escapeHtml(file.fileId || "-")}
                ${file.sceneName ? ` | 场景: ${escapeHtml(file.sceneName)}` : ""}
              </div>
              <div class="status-info">
                <span class="status-badge ${statusClass}">${statusText}</span>
                ${status === "COMPLETED" && generatedCount > 0 ? `<span class="generated-count">已生成 ${generatedCount} 个接口</span>` : ""}
                ${status === "FAILED" && errorMessage ? `<span class="error-msg">错误: ${escapeHtml(errorMessage)}</span>` : ""}
              </div>
            </div>
            <div>
              ${downloadUrl ? `<a class="action-btn" href="${downloadUrl}" target="_blank">下载原始文件</a>` : ""}
              <button class="action-btn danger" data-delete-file="${escapeHtml(file.fileId || "")}">删除</button>
            </div>
          `;
          list.appendChild(div);
        });
        docs.appendChild(list);
        docs.querySelectorAll("[data-delete-file]").forEach((btn) => {
          btn.addEventListener("click", async () => {
            const fileId = btn.getAttribute("data-delete-file");
            if (!fileId) return;
            const ok = await openConfirmModal({
              title: "确认删除",
              message: "确定删除该文档及其关联接口吗？此操作不可撤销。",
              okText: "确认删除",
              danger: true
            });
            if (!ok) return;
            try {
              btn.disabled = true;
              const res = await fetch(`/parse/endpoint/file/${fileId}`, { method: "DELETE" });
              if (res.ok) {
                setStatus("已删除文档");
                showToast("删除成功", "success");
                await loadUploadedFiles();
                await loadHistory();
              } else {
                setStatus("删除失败");
                showToast("删除失败", "error");
              }
            } catch (err) {
              setStatus("删除异常");
              showToast("删除异常", "error");
            } finally {
              btn.disabled = false;
            }
          });
        });
      }

      function buildApiCard(item) {
        const div = document.createElement("div");
        div.className = "api-card";
        const mockUrl = toAbsoluteUrl(item.mockUrl || "");
        const apiPath = item.apiPath || "";
        const required = Array.isArray(item.requiredFields) ? item.requiredFields : [];
        const downloadUrl = toAbsoluteUrl(item.sourceFileUrl || "");
        const title = item.title || "API";
        const method = (item.method || "").toUpperCase() || (/post/i.test(title) ? "POST" : "GET");
        const isPost = method === "POST";
        const scriptBadge = (item.responseMode === "script") ? '<span class="pill" style="margin-left:6px;font-size:11px;">脚本</span>' : "";
        div.innerHTML = `
          <div>
            <div class="row">
              <span class="method ${String(method || "").toLowerCase()}">${method}</span>
              <h3>${escapeHtml(title)}${scriptBadge}</h3>
            </div>
            <div class="api-meta api-meta-source">Source: <span class="api-meta-strong">${escapeHtml(item.sourceFileName || "-")}</span>
              ${downloadUrl ? `<a href="${downloadUrl}" target="_blank">下载</a>` : ""}
            </div>
            <div class="api-meta">Scene: <span class="api-meta-strong">${escapeHtml(item.sceneName || "-")}</span></div>
            ${apiPath ? `<div class="api-meta">Path: <span class="api-meta-strong">${escapeHtml(apiPath)}</span></div>` : ""}
            <div class="api-url">
              <span>${escapeHtml(mockUrl)}</span>
              <button class="action-btn" data-copy="${mockUrl}">复制</button>
            </div>
            <div class="api-meta api-meta-title">Required Fields</div>
            <div class="api-meta">${required.map((r) => `<span class="pill">${escapeHtml(formatRequiredFieldLabel(r))}</span>`).join("")}</div>
          </div>
          <div class="api-actions">
            <button class="play" data-use="success"><span style="font-size: 14px;">▶</span> 调试成功响应</button>
            <button class="action-btn warn" data-use="error"><span style="font-size: 14px;">✖</span> 调试失败响应</button>
            <button class="json" data-json="1"><span style="font-size: 14px;">≡</span> 查看 JSON</button>
            <button class="action-btn" data-edit="1"><span style="font-size: 14px;">✎</span> 编辑配置</button>
            <button class="action-btn danger" data-delete-api="${escapeHtml(item.id || "")}"><span style="font-size: 14px;">🗑</span> 删除接口</button>
          </div>
        `;
        div.querySelectorAll("button[data-copy]").forEach((btn) => {
          btn.addEventListener("click", async () => {
            const value = btn.getAttribute("data-copy") || "";
            try {
              await navigator.clipboard.writeText(value);
              setStatus("已复制");
              showToast("复制成功", "copy");
            } catch (err) {
              setStatus("复制失败");
              showToast("复制失败", "error");
            }
          });
        });
        div.querySelectorAll("button[data-use]").forEach((btn) => {
          const mode = btn.getAttribute("data-use");
          btn.addEventListener("click", () => useMockItem(item, mode === "error"));
        });
        const jsonBtn = div.querySelector("button[data-json]");
        if (jsonBtn) {
          jsonBtn.addEventListener("click", () => {
            if (!jsonModal) return;
            jsonReq.textContent = JSON.stringify(item.requestExample || {}, null, 2);
            jsonResp.textContent = JSON.stringify(item.responseExample || {}, null, 2);
            jsonErr.textContent = JSON.stringify(item.errorResponseExample || {}, null, 2);
            jsonModal.classList.add("open");
          });
        }
        const editBtn = div.querySelector("button[data-edit]");
        if (editBtn) {
          editBtn.addEventListener("click", () => {
            if (!editModal) return;
            editingMockId = item.id || null;
            editReq.value = JSON.stringify(item.requestExample || {}, null, 2);
            editResp.value = JSON.stringify(item.responseExample || {}, null, 2);
            editErr.value = JSON.stringify(item.errorResponseExample || {}, null, 2);
            if (editDelayMs) {
              editDelayMs.value = item.responseDelayMs != null ? String(item.responseDelayMs) : "";
            }
            if (editResponseMode) {
              editResponseMode.value = (item.responseMode === "script" ? "script" : "template");
            }
            if (editResponseScript) {
              editResponseScript.value = item.responseScript != null ? item.responseScript : "";
            }
            if (editResponseScriptWrap) {
              editResponseScriptWrap.style.display = (editResponseMode && editResponseMode.value === "script") ? "block" : "none";
            }
            editModal.classList.add("open");
          });
        }
        const delBtn = div.querySelector("button[data-delete-api]");
        if (delBtn) {
          delBtn.addEventListener("click", async () => {
            const id = delBtn.getAttribute("data-delete-api");
            if (!id) return;
            const ok = await openConfirmModal({
              title: "确认删除",
              message: "确定删除该 Mock 接口吗？此操作不可撤销。",
              okText: "确认删除",
              danger: true
            });
            if (!ok) return;
            try {
              delBtn.disabled = true;
              const res = await fetch(`/parse/endpoint/${id}`, { method: "DELETE" });
              if (res.ok) {
                setStatus("已删除接口");
                showToast("删除成功", "success");
                await loadHistory();
              } else {
                setStatus("删除失败");
                showToast("删除失败", "error");
              }
            } catch (err) {
              setStatus("删除异常");
              showToast("删除异常", "error");
            } finally {
              delBtn.disabled = false;
            }
          });
        }
        return div;
      }

      if (jsonModal) {
        jsonModal.addEventListener("click", (e) => {
          if (e.target === jsonModal) {
            jsonModal.classList.remove("open");
          }
        });
      }
      if (editModal) {
        editModal.addEventListener("click", (e) => {
          if (e.target === editModal) {
            editModal.classList.remove("open");
          }
        });
      }
      if (editResponseMode && editResponseScriptWrap) {
        editResponseMode.addEventListener("change", () => {
          editResponseScriptWrap.style.display = editResponseMode.value === "script" ? "block" : "none";
        });
      }
      if (manualResponseMode && manualResponseScriptWrap) {
        manualResponseMode.addEventListener("change", () => {
          manualResponseScriptWrap.style.display = manualResponseMode.value === "script" ? "block" : "none";
        });
      }

      if (saveEditBtn) {
        saveEditBtn.addEventListener("click", async () => {
          if (!editingMockId) return;
          let reqJson;
          let respJson;
          let errJson;
          let delayMsValue = null;
          try {
            reqJson = editReq.value.trim() ? JSON.parse(editReq.value) : {};
            respJson = editResp.value.trim() ? JSON.parse(editResp.value) : {};
            errJson = editErr.value.trim() ? JSON.parse(editErr.value) : {};
            if (editDelayMs && editDelayMs.value.trim()) {
              delayMsValue = Number(editDelayMs.value.trim());
            }
          } catch (err) {
            setStatus("JSON 格式错误");
            return;
          }
          const responseMode = editResponseMode ? editResponseMode.value : "template";
          const responseScript = editResponseScript ? editResponseScript.value : "";
          try {
            const res = await fetch(`/parse/endpoint/${editingMockId}`, {
              method: "PUT",
              headers: { "Content-Type": "application/json" },
              body: JSON.stringify({
                requestExample: reqJson,
                responseExample: respJson,
                errorResponseExample: errJson,
                responseDelayMs: delayMsValue,
                responseMode: responseMode,
                responseScript: responseScript
              })
            });
            if (res.ok) {
              setStatus("已保存");
              showToast("修改成功", "success");
              editModal.classList.remove("open");
              await loadHistory();
            } else {
              setStatus("保存失败");
              showToast("保存失败", "error");
            }
          } catch (err) {
            setStatus("保存异常");
            showToast("保存异常", "error");
          }
        });
      }

      if (sceneSaveBtn) {
        sceneSaveBtn.addEventListener("click", async () => {
          const name = sceneNameInput ? sceneNameInput.value.trim() : "";
          const description = sceneDescInput ? sceneDescInput.value.trim() : "";
          const keywords = sceneKeywordsInput ? sceneKeywordsInput.value.trim() : "";
          if (!name) {
            setStatus("请输入场景名称");
            showToast("场景名称不能为空", "error");
            return;
          }
          try {
            sceneSaveBtn.disabled = true;
            const payload = JSON.stringify({ name, description, keywords });
            const url = editingSceneId ? `/parse/scenes/${editingSceneId}` : "/parse/scenes";
            const method = editingSceneId ? "PUT" : "POST";
            const res = await fetch(url, {
              method,
              headers: { "Content-Type": "application/json" },
              body: payload
            });
            if (res.ok) {
              setStatus(editingSceneId ? "场景已更新" : "场景已创建");
              showToast(editingSceneId ? "更新成功" : "创建成功", "success");
              closeSceneForm();
              await loadScenes(true);
              await loadHistory();
            } else {
              setStatus("操作失败");
              showToast("操作失败", "error");
            }
          } catch (err) {
            setStatus("操作异常");
            showToast("操作异常", "error");
          } finally {
            sceneSaveBtn.disabled = false;
          }
        });
      }

      if (sceneCancelBtn) {
        sceneCancelBtn.addEventListener("click", () => closeSceneForm());
      }

      if (sceneCreateBtn) {
        sceneCreateBtn.addEventListener("click", () => openSceneForm(null));
      }

      if (closeSceneFormModal) {
        closeSceneFormModal.addEventListener("click", () => closeSceneForm());
      }
      if (sceneFormModal) {
        sceneFormModal.addEventListener("click", (e) => {
          if (e.target === sceneFormModal) {
            closeSceneForm();
          }
        });
      }

      if (manualCreateBtn) {
        manualCreateBtn.addEventListener("click", () => {
          if (!manualModal) return;
          setManualInputMode("chat"); // 默认打开聊天模式
          if (manualTitleInput) manualTitleInput.value = "";
          if (manualMethodSelect) manualMethodSelect.value = "POST";
          if (manualSceneSelect) manualSceneSelect.value = "";
          setManualMode("table");
          renderKeyValueTable(manualReqHeadersTable, {});
          renderKeyValueTable(manualReqQueryTable, {});
          renderKeyValueTable(manualReqBodyTable, {});
          renderKeyValueTable(manualRespHeadersTable, {});
          renderKeyValueTable(manualRespBodyTable, {});
          renderKeyValueTable(manualErrBodyTable, {});
          if (manualErrStatusInput) manualErrStatusInput.value = "";
          if (manualDelayMsInput) manualDelayMsInput.value = "";
          if (manualReqJson) manualReqJson.value = JSON.stringify({ headers: {}, query: {}, body: {} }, null, 2);
          if (manualRespJson) manualRespJson.value = JSON.stringify({ headers: {}, body: {} }, null, 2);
          if (manualErrJson) manualErrJson.value = "{}";
          if (manualRequired) manualRequired.value = "[]";
          if (typeof loadChatHistoryForUser === "function") {
            loadChatHistoryForUser(window.__currentUserId || "anonymous");
          } else {
            manualChatMessages = [];
            renderChatHistory();
          }
          manualModal.classList.add("open");
          // 避免打开弹窗时强制滚到底导致“往上跑/跳动”的观感；默认从顶部开始
          setTimeout(() => {
            const modalContent = document.querySelector("#manualModal .content");
            if (modalContent) {
              modalContent.scrollTop = 0;
            }
            if (manualTitleInput && typeof manualTitleInput.focus === "function") {
              try {
                // avoid scrolling the page when focusing inside modal
                manualTitleInput.focus({ preventScroll: true });
              } catch (e) {
                manualTitleInput.focus();
              }
            }
          }, 0);
        });
      }

      if (manualSaveBtn) {
        manualSaveBtn.addEventListener("click", async () => {
          const title = manualTitleInput ? manualTitleInput.value.trim() : "";
          const method = manualMethodSelect ? manualMethodSelect.value : "POST";
          const sceneId = manualSceneSelect ? manualSceneSelect.value.trim() : "";
          if (!title) {
            setStatus("请输入接口标题");
            showToast("接口标题不能为空", "error");
            return;
          }
          if (!sceneId) {
            setStatus("请选择场景");
            showToast("请选择场景", "error");
            return;
          }
          let reqObj;
          let respObj;
          let errObj;
          let requiredArr;
          try {
            if (manualJsonWrap && !manualJsonWrap.classList.contains("hidden")) {
              reqObj = manualReqJson && manualReqJson.value.trim() ? JSON.parse(manualReqJson.value) : {};
              respObj = manualRespJson && manualRespJson.value.trim() ? JSON.parse(manualRespJson.value) : {};
              errObj = manualErrJson && manualErrJson.value.trim() ? JSON.parse(manualErrJson.value) : {};
            } else {
              reqObj = {
                headers: collectTableData(manualReqHeadersTable),
                query: collectTableData(manualReqQueryTable),
                body: collectTableData(manualReqBodyTable)
              };
              respObj = {
                headers: collectTableData(manualRespHeadersTable),
                body: collectTableData(manualRespBodyTable)
              };
              errObj = collectTableData(manualErrBodyTable);
            }
            requiredArr = manualRequired && manualRequired.value.trim() ? JSON.parse(manualRequired.value) : [];
          } catch (err) {
            setStatus("JSON 格式错误");
            showToast("JSON 格式错误", "error");
            return;
          }
          const errorHttpStatus = manualErrStatusInput && manualErrStatusInput.value.trim()
            ? Number(manualErrStatusInput.value.trim())
            : null;
          const responseDelayMs = manualDelayMsInput && manualDelayMsInput.value.trim()
            ? Number(manualDelayMsInput.value.trim())
            : null;
          const responseMode = manualResponseMode ? manualResponseMode.value : "template";
          const responseScript = manualResponseScript ? manualResponseScript.value : "";
          try {
            manualSaveBtn.disabled = true;
            const res = await fetch("/parse/endpoint/manual", {
              method: "POST",
              headers: { "Content-Type": "application/json" },
              body: JSON.stringify({
                title,
                method,
                sceneId,
                requestExample: reqObj,
                responseExample: respObj,
                errorResponseExample: errObj,
                requiredFields: requiredArr,
                errorHttpStatus: errorHttpStatus,
                responseDelayMs: responseDelayMs,
                responseMode,
                responseScript
              })
            });
            if (res.ok) {
              const savedItem = await res.json();
              setStatus("已保存接口");
              showToast("创建成功", "success");
              manualModal.classList.remove("open");
              
              // 如果当前有场景过滤，且新接口的场景与当前过滤不一致，清除过滤以确保新接口显示
              if (apiSceneFilter && apiSceneFilter.value) {
                const savedSceneId = savedItem.sceneId || "";
                const currentFilterSceneId = apiSceneFilter.value.trim();
                if (savedSceneId !== currentFilterSceneId) {
                  // 清除场景过滤，显示所有接口
                  apiSceneFilter.value = "";
                  showToast("已清除场景过滤以显示新接口", "info");
                }
              }
              
              // 确保列表刷新
              await loadHistory();
            } else {
              const errorText = await res.text();
              setStatus("保存失败");
              showToast("保存失败: " + errorText, "error");
            }
          } catch (err) {
            setStatus("保存异常");
            showToast("保存异常", "error");
          } finally {
            manualSaveBtn.disabled = false;
          }
        });
      }

      function setManualMode(mode) {
        if (!manualTableWrap || !manualJsonWrap || !manualModeTable || !manualModeJson) return;
        const useJson = mode === "json";
        manualTableWrap.classList.toggle("hidden", useJson);
        manualJsonWrap.classList.toggle("hidden", !useJson);
        manualModeTable.classList.toggle("active", !useJson);
        manualModeJson.classList.toggle("active", useJson);
      }

      function setManualInputMode(mode) {
        const isChat = mode === "chat";
        if (manualChatWrap) manualChatWrap.classList.toggle("hidden", !isChat);
        if (manualModeManual) manualModeManual.classList.toggle("active", !isChat);
        if (manualModeChat) manualModeChat.classList.toggle("active", isChat);
        // 切换为聊天模式时，仅滚动聊天历史区域，不滚动整个弹窗（否则打开时会默认在最底部）
        if (isChat && manualChatHistory) {
          // Let DOM update first
          requestAnimationFrame(() => {
            try {
              manualChatHistory.scrollTop = manualChatHistory.scrollHeight;
            } catch (e) {
              // ignore
            }
          });
        }
      }

      function syncTableToJson() {
        if (!manualReqJson || !manualRespJson || !manualErrJson) return;
        const reqObj = {
          headers: collectTableData(manualReqHeadersTable),
          query: collectTableData(manualReqQueryTable),
          body: collectTableData(manualReqBodyTable)
        };
        const respObj = {
          headers: collectTableData(manualRespHeadersTable),
          body: collectTableData(manualRespBodyTable)
        };
        const errObj = collectTableData(manualErrBodyTable);
        manualReqJson.value = JSON.stringify(reqObj, null, 2);
        manualRespJson.value = JSON.stringify(respObj, null, 2);
        manualErrJson.value = JSON.stringify(errObj, null, 2);
      }

      function syncJsonToTable() {
        try {
          const reqObj = manualReqJson && manualReqJson.value.trim() ? JSON.parse(manualReqJson.value) : {};
          const respObj = manualRespJson && manualRespJson.value.trim() ? JSON.parse(manualRespJson.value) : {};
          const errObj = manualErrJson && manualErrJson.value.trim() ? JSON.parse(manualErrJson.value) : {};
          renderKeyValueTable(manualReqHeadersTable, reqObj.headers || {});
          renderKeyValueTable(manualReqQueryTable, reqObj.query || {});
          renderKeyValueTable(manualReqBodyTable, reqObj.body || {});
          renderKeyValueTable(manualRespHeadersTable, respObj.headers || {});
          renderKeyValueTable(manualRespBodyTable, respObj.body || {});
          renderKeyValueTable(manualErrBodyTable, errObj || {});
          return true;
        } catch (err) {
          setStatus("JSON 格式错误");
          showToast("JSON 格式错误", "error");
          return false;
        }
      }

      if (manualModeTable) {
        manualModeTable.addEventListener("click", () => {
          if (manualJsonWrap && !manualJsonWrap.classList.contains("hidden")) {
            const ok = syncJsonToTable();
            if (!ok) return;
          }
          setManualMode("table");
        });
      }
      if (manualModeJson) {
        manualModeJson.addEventListener("click", () => {
          syncTableToJson();
          setManualMode("json");
        });
      }

      if (manualModeManual) {
        manualModeManual.addEventListener("click", () => setManualInputMode("manual"));
      }
      if (manualModeChat) {
        manualModeChat.addEventListener("click", () => setManualInputMode("chat"));
      }

      const manualChatImageInput = document.getElementById("manualChatImageInput");
      const manualChatImages = document.getElementById("manualChatImages");
      let currentChatImages = [];

      // 处理图片文件（上传或粘贴）
      function handleImageFile(file) {
        if (!file || !file.type.startsWith("image/")) {
          showToast("请选择图片文件", "error");
          return false;
        }
        if (file.size > 10 * 1024 * 1024) {
          showToast("图片大小不能超过 10MB", "error");
          return false;
        }
        const reader = new FileReader();
        reader.onload = (event) => {
          const base64 = event.target.result;
          const imageId = "img_" + Date.now();
          currentChatImages.push({ id: imageId, base64: base64, file: file });
          renderChatImages();
          showToast("图片已添加", "success");
        };
        reader.readAsDataURL(file);
        return true;
      }

      // 上传图片
      if (manualChatImageInput) {
        manualChatImageInput.addEventListener("change", (e) => {
          const file = e.target.files[0];
          if (file) {
            handleImageFile(file);
            manualChatImageInput.value = "";
          }
        });
      }

      // 粘贴图片支持
      if (manualChatInput) {
        manualChatInput.addEventListener("paste", (e) => {
          const clipboardData = e.clipboardData || window.clipboardData;
          if (!clipboardData) return;
          
          const items = clipboardData.items;
          if (!items) return;
          
          for (let i = 0; i < items.length; i++) {
            const item = items[i];
            if (item.type.indexOf("image") !== -1) {
              e.preventDefault();
              const file = item.getAsFile();
              if (file) {
                handleImageFile(file);
              }
              break;
            }
          }
        });
      }

      function renderChatImages() {
        if (!manualChatImages) return;
        if (currentChatImages.length === 0) {
          manualChatImages.innerHTML = "";
          return;
        }
        manualChatImages.innerHTML = currentChatImages.map((img, idx) => `
          <div style="position: relative; border: 1px solid #ddd; border-radius: 4px; padding: 4px; background: #f9f9f9;">
            <img src="${img.base64}" style="max-width: 120px; max-height: 120px; display: block; border-radius: 4px;" />
            <button onclick="removeChatImage(${idx})" style="position: absolute; top: 2px; right: 2px; background: #ff4444; color: white; border: none; border-radius: 50%; width: 20px; height: 20px; cursor: pointer; font-size: 12px;">×</button>
          </div>
        `).join("");
      }

      window.removeChatImage = function(idx) {
        currentChatImages.splice(idx, 1);
        renderChatImages();
      };

      if (manualChatGenerateBtn) {
        manualChatGenerateBtn.addEventListener("click", async () => {
          const text = manualChatInput ? manualChatInput.value.trim() : "";
          if (!text && currentChatImages.length === 0) {
            setStatus("请输入描述或上传图片");
            showToast("请输入描述或上传图片", "error");
            return;
          }
          let originalText = "生成预览";
          try {
            // 显示加载状态
            originalText = manualChatGenerateBtn.textContent || "生成预览";
            manualChatGenerateBtn.disabled = true;
            manualChatGenerateBtn.innerHTML = '<span style="display:inline-block;width:14px;height:14px;border:2px solid #fff;border-top-color:transparent;border-radius:50%;animation:spin 0.8s linear infinite;margin-right:6px;"></span>生成中...';
            
            // 构建消息内容（文本+图片）
            const messageContent = [];
            if (text) {
              messageContent.push({ type: "text", text: text });
            }
            // 添加图片
            for (const img of currentChatImages) {
              messageContent.push({ 
                type: "image_url", 
                image_url: { url: img.base64 }
              });
            }
            // 添加用户消息
            manualChatMessages.push({ 
              role: "user", 
              content: messageContent.length === 1 && messageContent[0].type === "text" 
                ? messageContent[0].text 
                : messageContent
            });
            renderChatHistory();
            if (manualChatInput) manualChatInput.value = "";
            // 清空当前图片（已添加到消息历史）
            currentChatImages = [];
            renderChatImages();
            // 获取模型选择
            const providerSelect = document.getElementById("manualChatProvider");
            const provider = providerSelect ? providerSelect.value : "zhipu";
            
            // 构建消息数组发送给后端
            const messages = manualChatMessages.map(msg => ({
              role: msg.role,
              content: msg.content
            }));
            const res = await fetch("/parse/endpoint/llm-preview", {
              method: "POST",
              headers: { "Content-Type": "application/json" },
              body: JSON.stringify({ messages: messages, provider: provider, userId: window.__currentUserId || "" })
            });
            if (!res.ok) {
              const errorText = await res.text();
              setStatus("生成失败");
              showToast("生成失败: " + errorText, "error");
              return;
            }
            const resText = await res.clone().text();
            const preview = resText ? parseJsonLoose(resText) : null;
            console.log("manual preview response:", preview || resText);
            if (!preview) {
              const debugText = resText && resText.length > 800 ? resText.slice(0, 800) + "..." : resText;
              manualChatMessages.push({ role: "assistant", content: "调试：后端返回非JSON或解析失败。\n" + (debugText || "") });
              renderChatHistory();
              setStatus("解析失败");
              showToast("解析失败：后端响应不是标准JSON", "error");
              return;
            }
            const needMoreInfo = preview && (
              preview.needMoreInfo === true ||
              String(preview.needMoreInfo).toLowerCase() === "true" ||
              (Array.isArray(preview.missingFields) && preview.missingFields.length > 0) ||
              (preview.message && String(preview.message).includes("请补充")) ||
              (preview.draft && typeof preview.draft === "object") ||
              (!preview.title && !preview.method) ||
              (!preview.title && !preview.method && preview.message)
            );
            if (needMoreInfo) {
              const msg = (preview && preview.message) ? preview.message : "信息不完整，请补充后再生成。";
              manualChatMessages.push({ role: "assistant", content: msg });
              renderChatHistory();
              setStatus("需要补充信息");
              showToast(msg, "warning");
              if (preview && preview.title && manualTitleInput) manualTitleInput.value = preview.title;
              if (preview && preview.method && manualMethodSelect) manualMethodSelect.value = preview.method;
              if (!preview) {
                manualChatMessages.push({ role: "assistant", content: "调试：无法解析返回结果，请检查后端响应格式。" });
                renderChatHistory();
              }
              return;
            }
            // 添加 AI 响应到对话历史
            manualChatMessages.push({ 
              role: "assistant", 
              content: "已生成接口结构：" + (preview.title || "未命名接口")
            });
            renderChatHistory();
            // 填充表单
            if (manualTitleInput && preview.title) manualTitleInput.value = preview.title;
            if (manualMethodSelect && preview.method) manualMethodSelect.value = preview.method;
            if (manualErrStatusInput && preview.errorHttpStatus) manualErrStatusInput.value = String(preview.errorHttpStatus);
            if (manualRequired) manualRequired.value = JSON.stringify(preview.requiredFields || [], null, 2);
            const req = preview.requestExample || {};
            const resp = preview.responseExample || {};
            const err = preview.errorResponseExample || {};
            renderKeyValueTable(manualReqHeadersTable, req.headers || {});
            renderKeyValueTable(manualReqQueryTable, req.query || {});
            renderKeyValueTable(manualReqBodyTable, req.body || {});
            renderKeyValueTable(manualRespHeadersTable, resp.headers || {});
            renderKeyValueTable(manualRespBodyTable, resp.body || {});
            renderKeyValueTable(manualErrBodyTable, err || {});
            syncTableToJson();
            setManualMode("table");
            setStatus("已生成预览");
            showToast("生成成功", "success");
            // 确保保存按钮可见：滚动到底部
            setTimeout(() => {
              const modalContent = document.querySelector("#manualModal .content");
              if (modalContent) {
                modalContent.scrollTop = modalContent.scrollHeight;
              }
            }, 100);
          } catch (err) {
            setStatus("生成异常");
            showToast("生成异常: " + (err.message || String(err)), "error");
          } finally {
            manualChatGenerateBtn.disabled = false;
            manualChatGenerateBtn.textContent = originalText;
          }
        });
      }

      if (manualChatClearBtn) {
        manualChatClearBtn.addEventListener("click", () => {
          manualChatMessages = [];
          currentChatImages = [];
          renderChatHistory();
          renderChatImages();
        });
      }

      let manualChatMessages = [];
      let manualChatUserId = null;
      function getChatStorageKey(userId) {
        return "manualChatHistory:" + (userId || "anonymous");
      }
      function loadChatHistoryForUser(userId) {
        manualChatUserId = userId || "anonymous";
        try {
          const raw = sessionStorage.getItem(getChatStorageKey(manualChatUserId));
          manualChatMessages = raw ? JSON.parse(raw) : [];
        } catch (e) {
          manualChatMessages = [];
        }
        renderChatHistory();
      }
      function saveChatHistory() {
        if (!manualChatUserId) return;
        try {
          sessionStorage.setItem(getChatStorageKey(manualChatUserId), JSON.stringify(manualChatMessages || []));
        } catch (e) {}
      }
      function renderChatHistory() {
        if (!manualChatHistory) return;
        if (!manualChatMessages.length) {
          manualChatHistory.innerHTML = "<div class='hint'>可多轮追加描述，生成时会合并上下文。支持上传截图识别接口信息。</div>";
          saveChatHistory();
          return;
        }
        manualChatHistory.innerHTML = manualChatMessages
          .map((msg, idx) => {
            const isUser = msg.role === "user";
            const roleLabel = isUser ? "用户" : "AI";
            const roleClass = isUser ? "user-msg" : "ai-msg";
            let contentHtml = "";
            if (isUser && Array.isArray(msg.content)) {
              // 多模态消息（文本+图片）
              contentHtml = msg.content.map(item => {
                if (item.type === "text") {
                  return `<div style="margin-bottom:4px;">${escapeHtml(item.text)}</div>`;
                } else if (item.type === "image_url") {
                  return `<img src="${escapeHtml(item.image_url.url)}" style="max-width:200px; max-height:150px; border-radius:4px; margin:4px 0;" />`;
                }
                return "";
              }).join("");
            } else {
              contentHtml = escapeHtml(typeof msg.content === "string" ? msg.content : JSON.stringify(msg.content));
            }
            return `<div class="${roleClass}" style="margin-bottom:8px; padding:6px; border-radius:4px; background:${isUser ? '#e3f2fd' : '#f1f8e9'};">
              <div style="font-size:12px; color:#666; margin-bottom:4px;"><strong>${roleLabel}</strong> #${idx + 1}</div>
              <div style="font-size:13px; line-height:1.5;">${contentHtml}</div>
            </div>`;
          })
          .join("");
        // 滚动到底部
        manualChatHistory.scrollTop = manualChatHistory.scrollHeight;
        saveChatHistory();
      }

      if (manualAddReqHeader) {
        manualAddReqHeader.addEventListener("click", () => addKeyValueRow(manualReqHeadersTable, "", ""));
      }
      if (manualAddReqQuery) {
        manualAddReqQuery.addEventListener("click", () => addKeyValueRow(manualReqQueryTable, "", ""));
      }
      if (manualAddReqBody) {
        manualAddReqBody.addEventListener("click", () => addKeyValueRow(manualReqBodyTable, "", ""));
      }
      if (manualAddRespHeader) {
        manualAddRespHeader.addEventListener("click", () => addKeyValueRow(manualRespHeadersTable, "", ""));
      }
      if (manualAddRespBody) {
        manualAddRespBody.addEventListener("click", () => addKeyValueRow(manualRespBodyTable, "", ""));
      }
      if (manualAddErrBody) {
        manualAddErrBody.addEventListener("click", () => addKeyValueRow(manualErrBodyTable, "", ""));
      }

      function toAbsoluteUrl(path) {
        if (!path) return "";
        if (path.startsWith("http://") || path.startsWith("https://")) {
          return path;
        }
        const base = window.location.origin || "http://localhost:8080";
        return base.replace(/\/$/, "") + "/" + path.replace(/^\//, "");
      }

      function escapeHtml(text) {
        return String(text || "")
          .replace(/&/g, "&amp;")
          .replace(/</g, "&lt;")
          .replace(/>/g, "&gt;")
          .replace(/"/g, "&quot;")
          .replace(/'/g, "&#39;");
      }

      // Display-only formatter for required fields.
      // Rule: split by "_" / "-" / whitespace, capitalize each segment's first letter, join with spaces.
      // Examples: "user_id" -> "User Id", "user-name" -> "User Name", "userId" -> "UserId" (camelCase not split)
      function formatRequiredFieldLabel(field) {
        const raw = String(field == null ? "" : field).trim();
        if (!raw) return "";
        const parts = raw.split(/[_\-\s]+/).filter(Boolean);
        if (!parts.length) return raw;
        return parts
          .map((p) => {
            const s = String(p);
            if (!s) return s;
            return s.charAt(0).toUpperCase() + s.slice(1);
          })
          .join(" ");
      }

      function useMockItem(item, errorMode = false, navigate = true) {
        // In multi-page mode, persist the selection before navigation so debug.html can restore it.
        if (__isMultiPageMode__()) {
          saveDebugPreset(item, errorMode);
        }
        const mockUrl = toAbsoluteUrl(item.mockUrl || "");
        mockUrlInput.value = mockUrl;
        const req = item.requestExample || {};
        const title = (item.title || "").toLowerCase();
        currentMethod = (item.method || "").toUpperCase() || (title.includes("get ") || title.startsWith("get") ? "GET" : "POST");
        setMethodToggleLock(true, currentMethod);
        if (debugMethodSelect) {
          debugMethodSelect.value = currentMethod;
        }
        const required = Array.isArray(item.requiredFields) ? item.requiredFields : [];
        if (req.headers || req.query || req.body) {
          const parts = normalizeRequestParts(required, {
            headers: req.headers || {},
            query: req.query || req.queryParams || {},
            body: req.body || {}
          }, currentMethod, req);
          renderKeyValueTable(headersTable, parts.headers || {});
          renderKeyValueTable(queryTable, parts.query || {});
          mockBody.value = JSON.stringify(parts.body || {}, null, 2);
        } else {
          const extracted = splitRequestByPath(req);
          const parts = normalizeRequestParts(required, extracted, currentMethod, req);
          renderKeyValueTable(headersTable, parts.headers || {});
          renderKeyValueTable(queryTable, parts.query || {});
          mockBody.value = JSON.stringify(parts.body || {}, null, 2);
        }
        if (!mockBody.value || mockBody.value === "{}") {
          const fallback = extractBodyFromRequest(req);
          mockBody.value = JSON.stringify(fallback, null, 2);
        }
        if (!mockBody.value) {
          renderKeyValueTable(headersTable, {});
          renderKeyValueTable(queryTable, {});
          mockBody.value = JSON.stringify(req || {}, null, 2);
        }
        if (currentMethod === "GET") {
          mockBody.value = "{}";
        }
        if (currentMethod === "POST") {
          renderKeyValueTable(queryTable, {});
        }
        renderRequiredChips(required);
        if (errorMode) {
          if (currentMethod === "GET") {
            setTableValue(queryTable, "__mock_error", "1");
          } else {
            try {
              const parsed = mockBody.value.trim() ? JSON.parse(mockBody.value) : {};
              parsed.__mock_error = true;
              mockBody.value = JSON.stringify(parsed, null, 2);
            } catch (err) {
              // ignore
            }
          }
          setTableValue(headersTable, "__mock_error", "1");
        }
        currentDebugMockId = item.id || null;
        currentDebugScriptMode = (item.responseMode === "script");
        if (debugScriptWrap) {
          debugScriptWrap.classList.toggle("hidden", !currentDebugScriptMode);
        }
        if (debugScriptTextarea) {
          debugScriptTextarea.value = (item.responseScript != null ? item.responseScript : "");
        }
        mockResponse.textContent = "{}";
        if (navigate) {
          switchTab("debug");
        }
      }

      async function sendMockRequest() {
        const url = mockUrlInput.value.trim();
        if (!url) {
          setStatus("请输入Mock地址");
          return;
        }
        const setSendBtnUi = (state) => {
          if (!sendMockBtn) return;
          sendMockBtn.classList.remove("is-success", "is-error");
          if (state === "loading") {
            sendMockBtn.classList.add("btn-loading");
            sendMockBtn.innerHTML = `<span style="font-size: 16px;">⏳</span> 请求中`;
            return;
          }
          if (state === "success") {
            sendMockBtn.classList.add("is-success");
            sendMockBtn.innerHTML = `<span style="font-size: 16px;">✓</span> 请求完成`;
            return;
          }
          if (state === "error") {
            sendMockBtn.classList.add("is-error");
            sendMockBtn.innerHTML = `<span style="font-size: 16px;">!</span> 请求失败`;
            return;
          }
          sendMockBtn.classList.remove("btn-loading");
          sendMockBtn.innerHTML = `<span style="font-size: 16px;">▶</span> 发送请求`;
        };

        setSendBtnUi("loading");
        if (debugMethodSelect) {
          currentMethod = debugMethodSelect.value || currentMethod;
        }
        let body = {};
        let headers = {};
        let query = {};
        try {
          body = mockBody.value.trim() ? JSON.parse(mockBody.value) : {};
          headers = collectTableData(headersTable);
          query = collectTableData(queryTable);
        } catch (err) {
          setStatus("请求JSON格式错误");
          setSendBtnUi("error");
          setTimeout(() => setSendBtnUi("idle"), 900);
          return;
        }
        const hasHeader = (obj, name) => {
          const target = String(name || "").toLowerCase();
          return Object.keys(obj || {}).some((k) => k.toLowerCase() === target);
        };
        if (!hasHeader(headers, "Content-Type")) {
          headers["Content-Type"] = "application/json";
        }
        if (!hasHeader(headers, "Accept")) {
          headers["Accept"] = "application/json";
        }
        if (currentMethod === "GET") {
          body = {};
        }
        if (currentMethod === "POST") {
          query = {};
        }
        if ((!mockBody.value || mockBody.value.trim() === "" || mockBody.value.trim() === "{}") && Object.keys(body).length === 0) {
          const req = mockBody.value.trim() ? JSON.parse(mockBody.value) : {};
          body = extractBodyFromRequest(req);
        }
        setStatus("🚀 请求处理中...");
        mockResponse.textContent = "";
        if (debugStatus) debugStatus.textContent = "Status: -";
        if (debugTime) debugTime.textContent = "Time: -";
        if (debugSize) debugSize.textContent = "Size: ...";
        let requestUrl = buildUrlWithQuery(url, query);
        let requestMethod = currentMethod;
        let requestBody = currentMethod === "POST" ? JSON.stringify(body) : undefined;
        if (currentDebugScriptMode && debugScriptTextarea && debugScriptTextarea.value.trim()) {
          const match = url.match(/\/parse\/mock\/([a-zA-Z0-9]+)/);
          if (match) {
            requestUrl = url.replace(/\?.*$/, "").replace(/\/?$/, "") + "/debug";
            requestMethod = "POST";
            requestBody = JSON.stringify({
              body: body,
              query: query,
              headers: headers,
              script: debugScriptTextarea.value.trim()
            });
          }
        }
        try {
          const start = performance.now();
          if (debugStatus) debugStatus.textContent = "Status: 请求中";
          const res = await fetch(requestUrl, {
            method: requestMethod,
            headers: headers,
            body: requestMethod === "POST" ? requestBody : undefined
          });
          const text = await res.text();
          const elapsed = Math.round(performance.now() - start);
          if (aiLoadingOverlay) {
            aiLoadingOverlay.classList.remove("show");
            if (aiLoadingOverlay.dataset.animInterval) {
              clearInterval(parseInt(aiLoadingOverlay.dataset.animInterval));
              delete aiLoadingOverlay.dataset.animInterval;
            }
          }
          try {
            const json = JSON.parse(text);
            mockResponse.textContent = JSON.stringify(json, null, 2);
          } catch (err) {
            mockResponse.textContent = text;
          }
          if (!res.ok) {
            setStatus("❌ 请求失败：" + res.status);
            setSendBtnUi("error");
          } else {
            const cacheHint = elapsed < 500 ? " (缓存命中)" : " (示例响应)";
            setStatus("✅ 完成" + cacheHint);
            setSendBtnUi("success");
          }
          if (debugStatus) debugStatus.textContent = `Status: ${res.status}`;
          if (debugTime) {
            const timeColor = elapsed < 500 ? "#22c55e" : (elapsed < 2000 ? "#f59e0b" : "#ef4444");
            debugTime.innerHTML = `Time: <span style="color: ${timeColor}; font-weight: 600;">${elapsed}ms</span>`;
          }
          if (debugSize) debugSize.textContent = `Size: ${text.length} bytes`;
          const curlBody = (requestMethod === "POST" && currentDebugScriptMode && debugScriptTextarea && debugScriptTextarea.value.trim())
            ? { body, query, headers, script: debugScriptTextarea.value.trim() }
            : body;
          if (debugCurl) debugCurl.textContent = buildCurl(requestUrl, headers, curlBody, requestMethod);
          if (responseHeadersTable) {
            renderKeyValueTableReadonly(responseHeadersTable, res.headers);
          }
        } catch (err) {
          if (aiLoadingOverlay) {
            aiLoadingOverlay.classList.remove("show");
            if (aiLoadingOverlay.dataset.animInterval) {
              clearInterval(parseInt(aiLoadingOverlay.dataset.animInterval));
              delete aiLoadingOverlay.dataset.animInterval;
            }
          }
          setStatus("❌ 请求异常：" + err.message);
          mockResponse.textContent = "// 请求失败\n" + err.message;
          if (debugStatus) debugStatus.textContent = "Status: 异常";
          setSendBtnUi("error");
        } finally {
          if (sendMockBtn) sendMockBtn.classList.remove("btn-loading");
          // Keep success/error color briefly, then return to idle.
          setTimeout(() => setSendBtnUi("idle"), 900);
        }
      }

      async function loadDebugMockInfoFromUrl() {
        const url = mockUrlInput && mockUrlInput.value ? mockUrlInput.value.trim() : "";
        const match = url.match(/\/parse\/mock\/([a-zA-Z0-9]+)/);
        if (!match || !debugScriptWrap || !debugScriptTextarea) return;
        try {
          const base = url.replace(/\/parse\/mock\/[a-zA-Z0-9]+.*$/, "");
          const res = await fetch(base + "/parse/mock/" + match[1] + "/info");
          if (!res.ok) return;
          const info = await res.json();
          currentDebugMockId = info.id || match[1];
          currentDebugScriptMode = (info.responseMode === "script");
          debugScriptWrap.classList.toggle("hidden", !currentDebugScriptMode);
          debugScriptTextarea.value = (info.responseScript != null ? info.responseScript : "");
        } catch (e) {
          // ignore
        }
      }

      if (mockUrlInput) {
        mockUrlInput.addEventListener("blur", () => { loadDebugMockInfoFromUrl(); });
      }

      if (debugMethodSelect) {
        debugMethodSelect.addEventListener("change", () => {
          currentMethod = debugMethodSelect.value;
          if (currentMethod === "GET") {
            mockBody.value = "{}";
          }
          if (currentMethod === "POST") {
            renderKeyValueTable(queryTable, {});
          }
        });
      }

      // Method segmented toggle (POST/GET): no dropdown, avoids overlay issues.
      if (methodToggle) {
        const pills = Array.from(methodToggle.querySelectorAll(".method-pill"));
        const setMethod = (method) => {
          if (!method) return;
          if (methodToggle.dataset.locked === "true") {
            return;
          }
          currentMethod = method;
          if (debugMethodSelect) {
            debugMethodSelect.value = method;
            // Reuse existing handler for side effects.
            debugMethodSelect.dispatchEvent(new Event("change"));
          } else {
            if (currentMethod === "GET") mockBody.value = "{}";
            if (currentMethod === "POST") renderKeyValueTable(queryTable, {});
          }
          pills.forEach((p) => p.classList.toggle("active", p.getAttribute("data-method") === method));
        };
        pills.forEach((p) => {
          p.addEventListener("click", () => setMethod(p.getAttribute("data-method")));
        });
        // Init from select value (or default POST)
        setMethod((debugMethodSelect && debugMethodSelect.value) || "POST");
      }

      function setMethodToggleLock(locked, allowedMethod) {
        if (!methodToggle) return;
        methodToggle.dataset.locked = locked ? "true" : "false";
        const pills = Array.from(methodToggle.querySelectorAll(".method-pill"));
        pills.forEach((p) => {
          const method = p.getAttribute("data-method");
          const disabled = locked && method !== allowedMethod;
          p.disabled = disabled;
          p.style.opacity = disabled ? "0.5" : "";
          p.style.cursor = disabled ? "not-allowed" : "";
        });
      }

      function buildUrlWithQuery(url, query) {
        if (!query || Object.keys(query).length === 0) {
          return url;
        }
        const params = new URLSearchParams();
        Object.keys(query).forEach((key) => {
          const value = query[key];
          if (value !== undefined && value !== null) {
            params.append(key, String(value));
          }
        });
        const joiner = url.includes("?") ? "&" : "?";
        return url + joiner + params.toString();
      }

      function buildCurl(url, headers, body, method) {
        const lines = [`curl -X ${method} "${url}"`];
        Object.keys(headers || {}).forEach((key) => {
          lines.push(`-H "${key}: ${headers[key]}"`);
        });
        if (method === "POST" && body && Object.keys(body).length) {
          lines.push(`-d '${JSON.stringify(body)}'`);
        }
        return lines.join(" \\\n  ");
      }

      function splitRequestByPath(requestExample) {
        const headers = {};
        const query = {};
        const body = {};
        if (!requestExample || typeof requestExample !== "object") {
          return { headers, query, body };
        }
        Object.keys(requestExample).forEach((key) => {
          if (key.startsWith("headers.")) {
            headers[key.replace("headers.", "")] = requestExample[key];
          } else if (key.startsWith("query.")) {
            query[key.replace("query.", "")] = requestExample[key];
          } else if (key.startsWith("body.")) {
            body[key.replace("body.", "")] = requestExample[key];
          }
        });
        return { headers, query, body };
      }

      function extractBodyFromRequest(requestExample) {
        if (!requestExample || typeof requestExample !== "object") {
          return {};
        }
        if (requestExample.body && typeof requestExample.body === "object") {
          return requestExample.body;
        }
        if (requestExample.request && typeof requestExample.request === "object") {
          return requestExample.request;
        }
        if (requestExample.data && typeof requestExample.data === "object") {
          return requestExample.data;
        }
        return requestExample;
      }

      document.querySelectorAll("[data-req-tab]").forEach((el) => {
        el.addEventListener("click", () => {
          document.querySelectorAll("[data-req-tab]").forEach((n) => n.classList.remove("active"));
          el.classList.add("active");
          const tab = el.getAttribute("data-req-tab");
          if (reqBodyPanel) reqBodyPanel.classList.toggle("active", tab === "body");
          if (reqHeadersPanel) reqHeadersPanel.classList.toggle("active", tab === "headers");
          if (reqQueryPanel) reqQueryPanel.classList.toggle("active", tab === "query");
        });
      });
      document.querySelectorAll("[data-resp-tab]").forEach((el) => {
        el.addEventListener("click", () => {
          document.querySelectorAll("[data-resp-tab]").forEach((n) => n.classList.remove("active"));
          el.classList.add("active");
          const tab = el.getAttribute("data-resp-tab");
          if (respBodyPanel) respBodyPanel.classList.toggle("active", tab === "body");
          if (respHeadersPanel) respHeadersPanel.classList.toggle("active", tab === "headers");
        });
      });

      function renderKeyValueTable(container, data) {
        if (!container) return;
        container.innerHTML = "";
        Object.keys(data || {}).forEach((key) => {
          addKeyValueRow(container, key, data[key]);
        });
        if (!container.children.length) {
          addKeyValueRow(container, "", "");
        }
      }

      function renderKeyValueTableReadonly(container, headers) {
        if (!container) return;
        container.innerHTML = "";
        if (!headers) return;
        if (typeof headers.forEach === "function") {
          headers.forEach((value, key) => {
            const tr = document.createElement("tr");
            tr.innerHTML = `<td>${escapeHtml(key)}</td><td>${escapeHtml(value)}</td>`;
            container.appendChild(tr);
          });
        }
      }

      function setTableValue(container, key, value) {
        if (!container) return;
        let found = false;
        Array.from(container.querySelectorAll("tr")).forEach((tr) => {
          const inputs = tr.querySelectorAll("input");
          if (inputs.length >= 2 && inputs[0].value.trim() === key) {
            inputs[1].value = value;
            found = true;
          }
        });
        if (!found) {
          addKeyValueRow(container, key, value);
        }
      }

      function renderRequiredChips(required) {
        if (!ruleHint) return;
        ruleHint.innerHTML = "";
        if (!required || !required.length) {
          const span = document.createElement("span");
          span.className = "chip";
          span.textContent = "无必填字段";
          ruleHint.appendChild(span);
          return;
        }
        required.forEach((field) => {
          const span = document.createElement("span");
          span.className = "chip";
          span.textContent = formatRequiredFieldLabel(field);
          ruleHint.appendChild(span);
        });
      }

      function parseJsonLoose(text) {
        if (!text) return null;
        const trimmed = text.trim();
        try {
          return JSON.parse(trimmed);
        } catch (err) {
          // continue
        }
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
          try {
            const unescaped = JSON.parse(trimmed);
            if (typeof unescaped === "string") {
              try {
                return JSON.parse(unescaped);
              } catch (err) {
                return null;
              }
            }
          } catch (err) {
            // ignore
          }
        }
        const firstObj = trimmed.indexOf("{");
        const firstArr = trimmed.indexOf("[");
        let start = -1;
        if (firstObj !== -1 && firstArr !== -1) {
          start = Math.min(firstObj, firstArr);
        } else {
          start = Math.max(firstObj, firstArr);
        }
        const lastObj = trimmed.lastIndexOf("}");
        const lastArr = trimmed.lastIndexOf("]");
        const end = Math.max(lastObj, lastArr);
        if (start !== -1 && end !== -1 && end > start) {
          const slice = trimmed.slice(start, end + 1);
          try {
            return JSON.parse(slice);
          } catch (err) {
            return null;
          }
        }
        return null;
      }

      function prettyPrintFallback(text) {
        if (!text) return "";
        const trimmed = text.trim();
        if (!trimmed) return "";
        let out = "";
        let indent = 0;
        let inString = false;
        let escape = false;
        const indentStr = (n) => "  ".repeat(n);
        for (let i = 0; i < trimmed.length; i++) {
          const ch = trimmed[i];
          if (escape) {
            out += ch;
            escape = false;
            continue;
          }
          if (ch === "\\") {
            out += ch;
            if (inString) escape = true;
            continue;
          }
          if (ch === "\"") {
            inString = !inString;
            out += ch;
            continue;
          }
          if (inString) {
            out += ch;
            continue;
          }
          if (ch === "{" || ch === "[") {
            out += ch + "\n" + indentStr(++indent);
            continue;
          }
          if (ch === "}" || ch === "]") {
            out += "\n" + indentStr(--indent) + ch;
            continue;
          }
          if (ch === ",") {
            out += ch + "\n" + indentStr(indent);
            continue;
          }
          if (ch === ":") {
            out += ": ";
            continue;
          }
          out += ch;
        }
        return out;
      }

      function normalizeRequestParts(requiredFields, parts, method, source) {
        const headers = { ...(parts.headers || {}) };
        const query = { ...(parts.query || {}) };
        const body = { ...(parts.body || {}) };
        const required = Array.isArray(requiredFields) ? requiredFields : [];
        required.forEach((path) => {
          if (!path) return;
          const lower = path.toLowerCase();
          const key = path.split(".").slice(1).join(".");
          const fallbackValue = findValueByKey(source || {}, key);
          if (lower.startsWith("headers.") || lower.startsWith("header.")) {
            if (headers[key] === undefined && body[key] !== undefined) {
              headers[key] = body[key];
            }
            if (headers[key] === undefined && fallbackValue !== undefined) {
              headers[key] = fallbackValue;
            }
          } else if (lower.startsWith("queryparams.") || lower.startsWith("query.")) {
            if (query[key] === undefined && body[key] !== undefined) {
              query[key] = body[key];
              delete body[key];
            }
            if (query[key] === undefined && fallbackValue !== undefined) {
              query[key] = fallbackValue;
            }
          } else if (lower.startsWith("body.")) {
            if (body[key] === undefined && query[key] !== undefined) {
              body[key] = query[key];
            }
            if (body[key] === undefined && fallbackValue !== undefined) {
              body[key] = fallbackValue;
            }
          }
        });
        if (method === "GET" && Object.keys(query).length === 0 && Object.keys(body).length > 0) {
          Object.keys(body).forEach((key) => {
            if (query[key] === undefined) {
              query[key] = body[key];
            }
          });
          Object.keys(body).forEach((key) => delete body[key]);
        }
        if (method === "POST" && Object.keys(body).length === 0 && Object.keys(query).length > 0) {
          Object.keys(query).forEach((key) => {
            if (body[key] === undefined) {
              body[key] = query[key];
            }
          });
          Object.keys(query).forEach((key) => delete query[key]);
        }
        if (method === "GET" && Object.keys(query).length === 0 && source && typeof source === "object") {
          Object.keys(source).forEach((key) => {
            if (query[key] === undefined && typeof source[key] !== "object") {
              query[key] = source[key];
            }
          });
        }
        if (method === "GET") {
          return { headers, query: query, body: {} };
        }
        if (method === "POST") {
          return { headers, query: {}, body: body };
        }
        return { headers, query, body };
      }

      function findValueByKey(obj, targetKey) {
        if (!obj || typeof obj !== "object" || !targetKey) return undefined;
        const lowerTarget = String(targetKey).toLowerCase();
        const stack = [obj];
        while (stack.length) {
          const current = stack.pop();
          if (!current || typeof current !== "object") continue;
          Object.keys(current).forEach((key) => {
            const value = current[key];
            if (String(key).toLowerCase() === lowerTarget && value !== undefined) {
              stack.length = 0;
              return value;
            }
            if (value && typeof value === "object") {
              stack.push(value);
            }
          });
        }
        return undefined;
      }

      function addKeyValueRow(container, key, value) {
        const tr = document.createElement("tr");
        let displayValue = "";
        if (value === null || value === undefined) {
          displayValue = "";
        } else if (typeof value === "object") {
          // 对象或数组，格式化为 JSON 字符串
          try {
            displayValue = JSON.stringify(value, null, 2);
          } catch (e) {
            displayValue = String(value);
          }
        } else {
          displayValue = String(value);
        }
        tr.innerHTML = `
          <td><input class="kv-input" value="${escapeHtml(key)}" /></td>
          <td><input class="kv-input" value="${escapeHtml(displayValue)}" /></td>
          <td><button class="action-btn">删除</button></td>
        `;
        tr.querySelector(".action-btn").addEventListener("click", () => tr.remove());
        container.appendChild(tr);
      }

      function collectTableData(container) {
        const result = {};
        if (!container) return result;
        Array.from(container.querySelectorAll("tr")).forEach((tr) => {
          const inputs = tr.querySelectorAll("input");
          if (inputs.length >= 2) {
            const key = inputs[0].value.trim();
            const value = inputs[1].value.trim();
            if (key) {
              result[key] = value;
            }
          }
        });
        return result;
      }

      if (addHeaderRow) {
        addHeaderRow.addEventListener("click", () => addKeyValueRow(headersTable, "", ""));
      }
      if (addQueryRow) {
        addQueryRow.addEventListener("click", () => addKeyValueRow(queryTable, "", ""));
      }
      if (copyCurlBtn) {
        copyCurlBtn.addEventListener("click", async () => {
          try {
            await navigator.clipboard.writeText(debugCurl ? debugCurl.textContent : "");
            setStatus("已复制 cURL");
            showToast("复制成功", "copy");
          } catch (err) {
            setStatus("复制失败");
            showToast("复制失败", "error");
          }
        });
      }

      const __PAGE_BY_TAB__ = {
        // Use explicit file to avoid relying on server root mapping
        home: "/index.html",
        doc: "/doc.html",
        api: "/api.html",
        // scene page merged into api
        scene: "/api.html",
        debug: "/debug.html",
        statistics: "/statistics.html"
      };

      function __isMultiPageMode__() {
        return !!window.__multiPageMode;
      }

      function __renderTab__(tab) {
        // scene has been merged into api
        if (tab === "scene") tab = "api";
        document.querySelectorAll(".nav span").forEach((el) => {
          el.classList.toggle("active", el.getAttribute("data-tab") === tab);
        });
        // Highlight corresponding quick start card
        document.querySelectorAll(".quick").forEach((el) => {
          el.classList.toggle("active", el.getAttribute("data-jump") === tab);
        });
        const homeEl = document.getElementById("home");
        const docEl = document.getElementById("doc");
        const apiEl = document.getElementById("api");
        const sceneEl = document.getElementById("scene");
        const debugEl = document.getElementById("debug");
        const statEl = document.getElementById("statistics");
        if (homeEl) homeEl.classList.toggle("hidden", tab !== "home");
        if (docEl) docEl.classList.toggle("hidden", tab !== "doc");
        if (apiEl) apiEl.classList.toggle("hidden", tab !== "api");
        if (sceneEl) sceneEl.classList.toggle("hidden", tab !== "scene");
        if (debugEl) debugEl.classList.toggle("hidden", tab !== "debug");
        if (statEl) statEl.classList.toggle("hidden", tab !== "statistics");
        if (tab === "statistics") {
          loadLogs();
        }
      }

      function switchTab(tab) {
        // scene has been merged into api
        if (tab === "scene") tab = "api";
        if (__isMultiPageMode__()) {
          const target = __PAGE_BY_TAB__[tab] || "/";
          const targetPath = new URL(target, window.location.origin).pathname;
          if (window.location.pathname === targetPath) {
            // Already on this page; just ensure correct visible section.
            __renderTab__(tab);
            return;
          }
          window.location.href = target;
          return;
        }
        __renderTab__(tab);
      }

      document.querySelectorAll(".nav span").forEach((el) => {
        el.addEventListener("click", () => switchTab(el.getAttribute("data-tab")));
      });

      document.querySelectorAll("[data-jump]").forEach((el) => {
        // If it's a real link, let the browser handle navigation
        // so users can right-click / open in new tab.
        if (el.tagName === "A" && el.getAttribute("href")) {
          return;
        }
        el.addEventListener("click", () => switchTab(el.getAttribute("data-jump")));
      });

      // Home entry buttons (event delegation; works regardless of DOM structure/overlays)
      document.addEventListener("click", (e) => {
        const target = e && e.target && e.target.closest ? e.target.closest("[data-action]") : null;
        if (!target) return;
        const action = target.getAttribute("data-action");
        if (action === "manual-entry") {
          // Prefer immediate open on current page (index.html contains the modal markup)
          if (manualCreateBtn && manualModal) {
            try {
              manualCreateBtn.click();
              return;
            } catch (err) {
              // fall through
            }
          }
          // Multi-page: navigate to api.html and open there
          saveManualOpenIntent();
          switchTab("api");
          return;
        }
        if (action === "inspiration") {
          showToast("敬请期待", "copy");
          setStatus("敬请期待");
        }
      });

      if (apiSearch) {
        apiSearch.addEventListener("input", () => loadHistory());
      }
      if (apiSceneFilter) {
        apiSceneFilter.addEventListener("change", () => loadHistory());
      }
      if (apiSearch) {
        apiSearch.addEventListener("input", () => refreshMergedApiView());
      }

      async function loadLogs(fromUserAction = false) {
        if (!logTableBody) {
          return;
        }
        const setRefreshBtnUi = (state) => {
          if (!refreshLogsBtn) return;
          if (state === "loading") {
            refreshLogsBtn.classList.add("btn-loading");
            refreshLogsBtn.textContent = "⏳ 刷新中";
            return;
          }
          refreshLogsBtn.classList.remove("btn-loading");
          refreshLogsBtn.textContent = "刷新日志";
        };
        if (fromUserAction) {
          setRefreshBtnUi("loading");
          setStatus("加载日志中...");
        }
        try {
          const [summaryRes, listRes, sceneRes, topRes] = await Promise.all([
            fetch("/parse/stats/summary"),
            fetch("/parse/logs"),
            fetch("/parse/stats/scene-endpoints"),
            fetch("/parse/stats/endpoint-top")
          ]);
          if (!summaryRes.ok && !listRes.ok && !sceneRes.ok && !topRes.ok) {
            if (fromUserAction) {
              setStatus(`加载失败：${listRes.status || summaryRes.status}`);
            }
          }
          if (summaryRes.ok) {
            const stats = await summaryRes.json();
            if (totalEndpointCount) totalEndpointCount.textContent = stats.totalEndpoints || 0;
            if (todayNewEndpointCount) todayNewEndpointCount.textContent = stats.todayNewEndpoints || 0;
          }
          if (listRes.ok) {
            const list = await listRes.json();
            renderLogTable(list);
          } else if (fromUserAction) {
            logTableBody.innerHTML = `<tr><td colspan='6' class='hint'>日志加载失败（${listRes.status}）</td></tr>`;
          }
          if (sceneRes.ok && sceneEndpointStats) {
            const list = await sceneRes.json();
            renderSceneBars(sceneEndpointStats, list);
          }
          if (topRes.ok && endpointCallTop10) {
            const list = await topRes.json();
            renderEndpointBars(endpointCallTop10, list);
          }
          if (fromUserAction) {
            setStatus("日志已刷新");
          }
        } catch (err) {
          if (fromUserAction) {
            setStatus("日志刷新异常：" + (err && err.message ? err.message : String(err)));
            logTableBody.innerHTML = `<tr><td colspan='6' class='hint'>日志刷新异常</td></tr>`;
          }
        } finally {
          if (fromUserAction) {
            setRefreshBtnUi("idle");
          }
        }
      }

      function renderEndpointBars(container, list) {
        if (!container) return;
        if (!Array.isArray(list) || !list.length) {
          container.innerHTML = "<div class='hint'>暂无数据</div>";
          return;
        }
        const sorted = list.slice().sort((a, b) => (b.count || 0) - (a.count || 0));
        const max = Math.max(1, ...sorted.map((x) => x.count || 0));
        container.innerHTML = sorted.map((row) => {
          const width = Math.round(((row.count || 0) / max) * 100);
          const label = row.apiPath
            ? `${row.method || ""} ${row.apiPath}`.trim()
            : (row.title || row.mockId || "-");
          return `
            <div class="bar">
              <div>${escapeHtml(label)}</div>
              <div class="track"><div class="fill" style="width:${width}%"></div></div>
              <div>${row.count || 0}</div>
            </div>
          `;
        }).join("");
      }

      function renderSceneBars(container, list) {
        if (!container) return;
        if (!Array.isArray(list) || !list.length) {
          container.innerHTML = "<div class='hint'>暂无数据</div>";
          return;
        }
        const sorted = list.slice().sort((a, b) => (b.count || 0) - (a.count || 0));
        const max = Math.max(1, ...sorted.map((x) => x.count || 0));
        container.innerHTML = sorted.map((row) => {
          const width = Math.round(((row.count || 0) / max) * 100);
          return `
            <div class="bar">
              <div>${escapeHtml(row.sceneName || row.sceneId || "-")}</div>
              <div class="track"><div class="fill" style="width:${width}%"></div></div>
              <div>${row.count || 0}</div>
            </div>
          `;
        }).join("");
      }

      function renderTrend(container, values) {
        if (!container) return;
        const max = Math.max(1, ...values);
        container.innerHTML = values.map((v, idx) => {
          const h = Math.max(8, Math.round((v / max) * 100));
          const cls = idx === 2 ? "err" : idx === 1 ? "gen" : "hit";
          return `<div class="bar ${cls}" style="height:${h}%;"></div>`;
        }).join("");
      }

      function renderLogTable(list) {
        logTableBody.innerHTML = "";
        if (!Array.isArray(list) || !list.length) {
          logTableBody.innerHTML = "<tr><td colspan='6' class='hint'>暂无日志</td></tr>";
          return;
        }
        const now = new Date();
        logUpdatedAt.textContent = `最后更新 ${now.toLocaleTimeString()}`;
        list.forEach((row) => {
          const type = row.type || "-";
          const badge = badgeClass(type);
          const dot = dotClass(type);
          const tr = document.createElement("tr");
          tr.className = "log-row";
          tr.innerHTML = `
            <td><span class="status-dot ${dot}"></span></td>
            <td><span class="badge ${badge}">${escapeHtml(type)}</span></td>
            <td>${escapeHtml(row.sourceFileName || "-")}</td>
            <td>${escapeHtml(row.message || "-")}</td>
            <td class="muted">${escapeHtml(row.createdAt || "-")}</td>
            <td style="text-align:right;">
              <button class="action-btn">查看JSON</button>
            </td>
          `;
          tr.querySelector(".action-btn").addEventListener("click", () => {
            if (logModal) {
              logModalContent.textContent = JSON.stringify(row, null, 2);
              logModal.classList.add("open");
            }
          });
          logTableBody.appendChild(tr);
        });
      }

      function badgeClass(type) {
        if (type === "MOCK_GEN") return "gen";
        if (type === "MOCK_HIT") return "hit";
        if (type === "MOCK_ERROR") return "err";
        if (type === "MOCK_VALIDATION_FAIL") return "fail";
        if (type === "UPLOAD_MOCK") return "upload";
        return "upload";
      }

      function dotClass(type) {
        if (type === "MOCK_GEN") return "dot-green";
        if (type === "MOCK_HIT") return "dot-blue";
        if (type === "MOCK_ERROR") return "dot-red";
        if (type === "MOCK_VALIDATION_FAIL") return "dot-orange";
        return "dot-gray";
      }

      async function initAuthNav() {
        const adminLink = document.getElementById("navAdminLink");
        const loginLink = document.getElementById("navLoginLink");
        const userBadge = document.getElementById("navUserBadge");
        try {
          const res = await fetch("/auth/me");
          if (!res.ok) {
            if (location.pathname.endsWith(".html") && !location.pathname.endsWith("login.html")) {
              location.href = "/login.html";
              return;
            }
            if (adminLink) adminLink.classList.add("hidden");
            if (userBadge) userBadge.classList.add("hidden");
            if (loginLink) {
              loginLink.textContent = "登录";
              loginLink.onclick = () => (location.href = "/login.html");
            }
            return;
          }
          const me = await res.json();
          const isAdmin = String(me.role || "").toUpperCase() === "ADMIN";
          if (userBadge) {
            userBadge.textContent = me.username + " (" + (me.role || "USER") + ")";
            userBadge.classList.remove("hidden");
          }
          window.__currentUserId = me.id || "";
          if (manualChatHistory) {
            loadChatHistoryForUser(window.__currentUserId || "anonymous");
          }
          if (adminLink) {
            if (isAdmin) {
              adminLink.classList.remove("hidden");
              adminLink.onclick = () => (location.href = "/admin.html");
            } else {
              adminLink.classList.add("hidden");
              adminLink.onclick = null;
              adminLink.removeAttribute("onclick");
            }
          }
          if (loginLink) {
            loginLink.textContent = "退出";
            loginLink.setAttribute("role", "button");
            loginLink.style.cursor = "pointer";
            loginLink.onclick = () => {
              fetch("/auth/logout", { method: "POST" }).catch(() => {});
              location.href = "/login.html";
            };
          }
        } catch (e) {
          if (adminLink) adminLink.classList.add("hidden");
          if (userBadge) userBadge.classList.add("hidden");
        }
      }

      // Initial render (avoid navigation loop in multi-page mode)
      (function init() {
        initThemeToggle();
        initSettingsMenu();
        initAuthNav();
        const initTab = window.__initialTab || "home";
        __renderTab__(initTab);
        // Instant docs list: render cached uploaded-files immediately if available.
        if (docs) {
          const cachedFiles = loadUploadedFilesCache();
          if (cachedFiles && cachedFiles.length) {
            renderDocs(cachedFiles);
          }
        }
        // If we navigated from API list to debug.html, restore the selected mock preset.
        if (initTab === "debug") {
          const preset = consumeDebugPreset();
          if (preset && preset.item) {
            // apply without re-navigating
            useMockItem(preset.item, preset.errorMode, false);
          }
        }
        // If we navigated from home quick-start to api.html, auto open manual entry modal.
        if (initTab === "api") {
          const openManual = consumeManualOpenIntent();
          if (openManual) {
            setTimeout(() => {
              if (manualCreateBtn) manualCreateBtn.click();
            }, 0);
          }
        }
        // Keep the previous behavior: history is useful across pages.
        // For merged API accordion, we still load history; merged rendering is handled separately.
        loadHistory();
        if (initTab === "statistics") {
          loadLogs();
        }
      })();
      loadUploadedFiles();
      
      // 定时刷新上传文件列表（每5秒）
      setInterval(() => {
        loadUploadedFiles();
      }, 5000);
      loadScenes();
