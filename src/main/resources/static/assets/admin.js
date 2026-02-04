function $(id) {
  return document.getElementById(id);
}

function setMessage(text, isError) {
  const el = $("adminMessage");
  if (!el) return;
  el.textContent = text || "";
  el.style.color = isError ? "#ef4444" : "#16a34a";
}

function getByPath(obj, path) {
  if (!obj || !path) return undefined;
  return path.split(".").reduce((acc, key) => (acc ? acc[key] : undefined), obj);
}

function setByPath(obj, path, value) {
  const parts = path.split(".");
  let cur = obj;
  for (let i = 0; i < parts.length - 1; i++) {
    if (!cur[parts[i]] || typeof cur[parts[i]] !== "object") {
      cur[parts[i]] = {};
    }
    cur = cur[parts[i]];
  }
  cur[parts[parts.length - 1]] = value;
}

const configFields = [
  { section: "LLM", path: "llm.provider", label: "llm.provider" },
  { section: "LLM", path: "llm.chat-provider", label: "llm.chat-provider" },

  { section: "Zhipu", path: "zhipu.api-key", label: "zhipu.api-key" },
  { section: "Zhipu", path: "zhipu.api-keys", label: "zhipu.api-keys" },
  { section: "Zhipu", path: "zhipu.model", label: "zhipu.model" },
  { section: "Zhipu", path: "zhipu.file-model", label: "zhipu.file-model" },
  { section: "Zhipu", path: "zhipu.temperature", label: "zhipu.temperature", type: "number" },
  { section: "Zhipu", path: "zhipu.max-tokens", label: "zhipu.max-tokens", type: "number" },

  { section: "OpenAI", path: "openai.api-key", label: "openai.api-key" },
  { section: "OpenAI", path: "openai.model", label: "openai.model" },
  { section: "OpenAI", path: "openai.temperature", label: "openai.temperature", type: "number" },

  { section: "Doubao", path: "doubao.api-key", label: "doubao.api-key" },
  { section: "Doubao", path: "doubao.phase-a-model", label: "doubao.phase-a-model" },
  { section: "Doubao", path: "doubao.phase-b-model", label: "doubao.phase-b-model" },
  { section: "Doubao", path: "doubao.vision-model", label: "doubao.vision-model" },
  { section: "Doubao", path: "doubao.temperature", label: "doubao.temperature", type: "number" },

  { section: "Mock", path: "mock.upload-dir", label: "mock.upload-dir" },
  { section: "Mock", path: "mock.processing.concurrent-limit", label: "mock.processing.concurrent-limit", type: "number" },

  { section: "Qiniu", path: "qiniu.enabled", label: "qiniu.enabled", type: "boolean" },
  { section: "Qiniu", path: "qiniu.access-key", label: "qiniu.access-key" },
  { section: "Qiniu", path: "qiniu.secret-key", label: "qiniu.secret-key" },
  { section: "Qiniu", path: "qiniu.bucket", label: "qiniu.bucket" },
  { section: "Qiniu", path: "qiniu.domain", label: "qiniu.domain" },

  { section: "Filter", path: "filter.keywords", label: "filter.keywords" },
  { section: "Filter", path: "filter.url-regex", label: "filter.url-regex" },

  { section: "Datasource", path: "spring.datasource.url", label: "spring.datasource.url" },
  { section: "Datasource", path: "spring.datasource.driver-class-name", label: "spring.datasource.driver-class-name" },
  { section: "Datasource", path: "spring.datasource.username", label: "spring.datasource.username" },
  { section: "Datasource", path: "spring.datasource.password", label: "spring.datasource.password" },

  { section: "JPA", path: "spring.jpa.hibernate.ddl-auto", label: "spring.jpa.hibernate.ddl-auto" },

  { section: "Upload", path: "spring.servlet.multipart.max-file-size", label: "spring.servlet.multipart.max-file-size" },
  { section: "Upload", path: "spring.servlet.multipart.max-request-size", label: "spring.servlet.multipart.max-request-size" },

  { section: "Logging", path: "logging.level.com.example.mock.parser.service.DocumentParserService", label: "logging.level.com.example.mock.parser.service.DocumentParserService" },
  { section: "Logging", path: "logging.level.com.example.mock.parser.service.MockEndpointService", label: "logging.level.com.example.mock.parser.service.MockEndpointService" },
];

function renderConfigForm(config) {
  const wrap = $("configForm");
  if (!wrap) return;
  wrap.innerHTML = "";
  let currentSection = null;
  configFields.forEach((field) => {
    if (field.section !== currentSection) {
      currentSection = field.section;
      const header = document.createElement("div");
      header.className = "label";
      header.style.marginTop = "12px";
      header.textContent = currentSection;
      wrap.appendChild(header);
    }
    const row = document.createElement("div");
    row.style.marginTop = "8px";
    const label = document.createElement("div");
    label.className = "label";
    label.textContent = field.label;
    const input = document.createElement("input");
    input.id = "cfg-" + field.path;
    if (field.type === "boolean") {
      input.type = "checkbox";
      input.checked = !!getByPath(config, field.path);
    } else {
      input.type = "text";
      const value = getByPath(config, field.path);
      input.value = value === undefined || value === null ? "" : String(value);
    }
    row.appendChild(label);
    row.appendChild(input);
    wrap.appendChild(row);
  });
}

function collectConfig() {
  const app = {};
  configFields.forEach((field) => {
    const input = $("cfg-" + field.path);
    if (!input) return;
    let value;
    if (field.type === "boolean") {
      value = !!input.checked;
    } else if (field.type === "number") {
      value = input.value.trim() === "" ? 0 : Number(input.value.trim());
    } else {
      value = input.value.trim();
    }
    setByPath(app, field.path, value);
  });
  return app;
}

function renderUsers(users) {
  const wrap = $("adminUserTableWrap");
  if (!wrap) return;
  if (!users || users.length === 0) {
    wrap.innerHTML = "<div class='muted'>暂无用户</div>";
    return;
  }
  const table = document.createElement("table");
  table.className = "kv-table";
  table.innerHTML = `
    <thead>
      <tr>
        <th>ID</th>
        <th>用户名</th>
        <th>角色</th>
        <th>状态</th>
        <th>操作</th>
      </tr>
    </thead>
    <tbody></tbody>
  `;
  const body = table.querySelector("tbody");
  users.forEach((user) => {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${user.id}</td>
      <td>${user.username}</td>
      <td>${user.role}</td>
      <td>${user.status}</td>
      <td>
        <button class="btn-outline" data-action="approve" data-id="${user.id}">批准</button>
        <button class="btn-outline" data-action="reject" data-id="${user.id}">拒绝</button>
        <button class="btn-outline" data-action="toggle-role" data-id="${user.id}" data-role="${user.role}">切换角色</button>
      </td>
    `;
    body.appendChild(tr);
  });
  wrap.innerHTML = "";
  wrap.appendChild(table);

  wrap.querySelectorAll("button[data-action]").forEach((btn) => {
    btn.addEventListener("click", async () => {
      const id = btn.getAttribute("data-id");
      const action = btn.getAttribute("data-action");
      if (action === "approve") {
        await fetch(`/auth/users/${id}/approve`, { method: "POST" });
      } else if (action === "reject") {
        await fetch(`/auth/users/${id}/reject`, { method: "POST" });
      } else if (action === "toggle-role") {
        const role = btn.getAttribute("data-role") === "ADMIN" ? "USER" : "ADMIN";
        await fetch(`/auth/users/${id}/role`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ role }),
        });
      }
      await loadUsers();
    });
  });
}

async function loadUsers() {
  const res = await fetch("/auth/users");
  if (!res.ok) return;
  const data = await res.json();
  renderUsers(data);
}

async function loadConfig() {
  const res = await fetch("/admin/config");
  if (!res.ok) return;
  const data = await res.json();
  renderConfigForm(data.application || {});
  if ($("openaiPrompts")) $("openaiPrompts").value = (data.prompts && data.prompts["openai-prompts.yml"]) || "";
  if ($("zhipuPrompts")) $("zhipuPrompts").value = (data.prompts && data.prompts["zhipu-prompts.yml"]) || "";
}

async function saveConfig() {
  const application = collectConfig();
  const res = await fetch("/admin/config", {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ application }),
  });
  const data = await res.json();
  if (data && data.success) {
    setMessage("配置已保存");
  } else {
    setMessage("保存失败", true);
  }
}

async function savePrompts() {
  const prompts = {
    "openai-prompts.yml": $("openaiPrompts").value || "",
    "zhipu-prompts.yml": $("zhipuPrompts").value || "",
  };
  const res = await fetch("/admin/config", {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ prompts }),
  });
  const data = await res.json();
  if (data && data.success) {
    setMessage("提示词已保存");
  } else {
    setMessage("保存失败", true);
  }
}

async function initAdminPage() {
  const meRes = await fetch("/auth/me");
  if (!meRes.ok) {
    location.href = "/login.html";
    return;
  }
  const me = await meRes.json();
  if (!me || me.role !== "ADMIN") {
    setMessage("仅管理员可访问此页面", true);
    return;
  }
  const logoutBtn = $("adminLogoutBtn");
  if (logoutBtn) {
    logoutBtn.addEventListener("click", async () => {
      await fetch("/auth/logout", { method: "POST" });
      location.href = "/login.html";
    });
  }
  const saveConfigBtn = $("saveConfigBtn");
  const savePromptsBtn = $("savePromptsBtn");
  if (saveConfigBtn) saveConfigBtn.addEventListener("click", saveConfig);
  if (savePromptsBtn) savePromptsBtn.addEventListener("click", savePrompts);

  await loadUsers();
  await loadConfig();
}

document.addEventListener("DOMContentLoaded", initAdminPage);
