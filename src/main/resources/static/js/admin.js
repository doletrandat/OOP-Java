/* ============ Trang quản trị (HR) ============ */

const $ = (sel) => document.querySelector(sel);
const state = {
  token: localStorage.getItem("cvr_admin_token") || null,
  jobs: [],
  currentJob: null,
  currentApplicationId: null,
  aiStatus: null,
};

function esc(s) {
  return String(s ?? "")
    .replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;").replaceAll("'", "&#39;");
}

async function api(path, opts = {}) {
  const headers = Object.assign({}, opts.headers || {});
  if (state.token) headers["X-Auth-Token"] = state.token;
  if (opts.body && !(opts.body instanceof FormData)) headers["Content-Type"] = "application/json";
  const res = await fetch(path, Object.assign({}, opts, { headers }));
  const data = await res.json().catch(() => ({}));
  if (!res.ok) {
    if (res.status === 401 && path !== "/api/auth/login") {
      logout(false);
      throw new Error("Phiên làm việc đã hết hạn, vui lòng đăng nhập lại.");
    }
    throw new Error(data.error || `Lỗi ${res.status}`);
  }
  return data;
}

function toast(msg, isErr = false) {
  const el = document.createElement("div");
  el.className = "toast" + (isErr ? " err" : "");
  el.textContent = msg;
  $("#toast-zone").appendChild(el);
  setTimeout(() => el.remove(), 4200);
}

function fmtScore(v) { return v == null ? "—" : Math.round(v); }
function scoreClass(v) {
  if (v == null) return "s-na";
  if (v >= 75) return "s-high";
  if (v >= 50) return "s-mid";
  return "s-low";
}
function fmtTime(iso) {
  try { return new Date(iso).toLocaleString("vi-VN"); } catch { return iso || "—"; }
}
const AI_STATUS_LABEL = {
  OK: "Đã chấm AI",
  NOT_CONFIGURED: "Chưa cấu hình AI",
  ERROR: "Lỗi AI",
};
const STATUS_META = {
  MATCHED:  { icon: "✅", label: "Khớp",        cls: "green" },
  UNCERTAIN:{ icon: "⚠️", label: "Chưa rõ",     cls: "amber" },
  MISSING:  { icon: "❌", label: "Chưa thấy",   cls: "red" },
};
const SOURCE_LABEL = { KEYWORD: "Từ khoá", AI: "AI", BOTH: "Từ khoá + AI", NONE: "—" };
const DECISION_META = {
  NEW:         { label: "Chưa xử lý", cls: "gray" },
  SHORTLISTED: { label: "Mời phỏng vấn", cls: "green" },
  REJECTED:    { label: "Từ chối", cls: "red" },
};
const CRITERION_TYPES = [
  { value: "EXPERIENCE",    label: "Số năm kinh nghiệm", hint: "VD: 2 (số năm tối thiểu)" },
  { value: "EDUCATION",     label: "Trình độ đào tạo",   hint: "VD: Đại học" },
  { value: "LANGUAGE",      label: "Chứng chỉ ngoại ngữ", hint: "VD: TOEIC 600 / IELTS 6.5" },
  { value: "CERTIFICATION", label: "Chứng chỉ nghề nghiệp", hint: "VD: AWS Certified, Oracle OCP" },
  { value: "OTHER",         label: "Tiêu chí khác",      hint: "VD: Kinh nghiệm làm việc nhóm" },
];
const CRITERION_STATUS_META = {
  MET:     { icon: "✅", label: "Đạt",      cls: "green" },
  PARTIAL: { icon: "🟡", label: "Một phần", cls: "amber" },
  NOT_MET: { icon: "❌", label: "Chưa đạt", cls: "red" },
  UNKNOWN: { icon: "❔", label: "Chưa rõ",   cls: "gray" },
};

/* ================== Xác thực ================== */

function showLogin() {
  $("#login-view").classList.remove("hidden");
  $("#app-view").classList.add("hidden");
  $("#nav-right").innerHTML = "";
}

function showApp() {
  $("#login-view").classList.add("hidden");
  $("#app-view").classList.remove("hidden");
  renderNav();
}

function logout(callServer = true) {
  if (callServer && state.token) {
    api("/api/auth/logout", { method: "POST" }).catch(() => {});
  }
  state.token = null;
  localStorage.removeItem("cvr_admin_token");
  showLogin();
}

function renderNav() {
  const ai = state.aiStatus;
  $("#nav-right").innerHTML = `
    <span class="ai-chip" title="Tiêu chí chấm điểm bằng AI">
      <span class="dot ${ai && ai.configured ? "on" : "off"}"></span>
      ${ai && ai.configured ? `${esc(ai.displayName || ai.model)} · trọng số ${Math.round(ai.aiWeight * 100)}%` : "Chưa cấu hình AI"}
    </span>
    <button class="btn btn-outline btn-sm" id="btn-logout">Đăng xuất</button>`;
  $("#btn-logout").addEventListener("click", () => logout());
}

$("#login-form").addEventListener("submit", async (e) => {
  e.preventDefault();
  const btn = $("#login-btn");
  btn.disabled = true;
  btn.textContent = "Đang đăng nhập…";
  $("#login-error").classList.add("hidden");
  try {
    const res = await api("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({ password: $("#login-password").value }),
    });
    state.token = res.token;
    localStorage.setItem("cvr_admin_token", res.token);
    await boot();
  } catch (err) {
    $("#login-error").textContent = err.message;
    $("#login-error").classList.remove("hidden");
  } finally {
    btn.disabled = false;
    btn.textContent = "Đăng nhập";
  }
});

/* ================== Khởi động ================== */

async function boot() {
  try {
    state.aiStatus = await api("/api/admin/ai-status");
    showApp();
    await loadJobs();
    showJobsView();
  } catch (e) {
    if (!state.token) showLogin();
    else toast(e.message, true);
  }
}

/* ================== Quản lý vị trí ================== */

async function loadJobs() {
  state.jobs = await api("/api/admin/jobs");
  const tbody = $("#jobs-tbody");
  tbody.innerHTML = state.jobs.map(j => `
    <tr>
      <td><b>${esc(j.title)}</b></td>
      <td class="muted">${esc(j.department || "—")}</td>
      <td>${(j.skills || []).length}</td>
      <td>${j.applicationCount}</td>
      <td>${j.status === "OPEN" ? '<span class="badge green">Đang mở</span>' : '<span class="badge gray">Đã đóng</span>'}</td>
      <td style="text-align:right;white-space:nowrap;">
        <button class="btn btn-primary btn-sm" data-view="${esc(j.id)}">Ứng viên</button>
        <button class="btn btn-outline btn-sm" data-edit="${esc(j.id)}">Sửa</button>
        <button class="btn btn-outline btn-sm" data-toggle="${esc(j.id)}">${j.status === "OPEN" ? "Đóng" : "Mở lại"}</button>
        <button class="btn btn-danger btn-sm" data-del="${esc(j.id)}">Xoá</button>
      </td>
    </tr>`).join("") || `<tr><td colspan="6" class="muted" style="text-align:center;padding:28px;">Chưa có vị trí nào. Nhấn “Tạo vị trí” để bắt đầu.</td></tr>`;

  tbody.querySelectorAll("[data-view]").forEach(b => b.addEventListener("click", () => openJobDetail(b.dataset.view)));
  tbody.querySelectorAll("[data-edit]").forEach(b => b.addEventListener("click", () => openJobModal(b.dataset.edit)));
  tbody.querySelectorAll("[data-toggle]").forEach(b => b.addEventListener("click", () => toggleJob(b.dataset.toggle)));
  tbody.querySelectorAll("[data-del]").forEach(b => b.addEventListener("click", () => deleteJob(b.dataset.del)));
}

function showJobsView() {
  $("#jobs-view").classList.remove("hidden");
  $("#job-detail-view").classList.add("hidden");
  state.currentJob = null;
}

/* ---------- Modal tạo / sửa vị trí ---------- */

function skillRowHtml(name = "", weight = "REQUIRED") {
  return `
    <div class="row mb" style="flex-wrap:nowrap;" data-skill-row>
      <input type="text" placeholder="VD: Java, Database, REST API..." value="${esc(name)}" style="flex:1;" data-skill-name>
      <select data-skill-weight style="width:150px;">
        <option value="REQUIRED" ${weight === "REQUIRED" ? "selected" : ""}>Bắt buộc</option>
        <option value="PREFERRED" ${weight === "PREFERRED" ? "selected" : ""}>Lợi thế</option>
      </select>
      <button type="button" class="icon-btn" data-remove-skill title="Xoá kỹ năng">🗑</button>
    </div>`;
}

function bindRemoveSkill() {
  $("#jf-skills").querySelectorAll("[data-remove-skill]").forEach(b =>
    b.onclick = () => b.closest("[data-skill-row]").remove());
}

function criterionRowHtml(name = "", type = "EXPERIENCE", requirement = "", weight = 2) {
  const meta = CRITERION_TYPES.find(t => t.value === type) || CRITERION_TYPES[0];
  return `
    <div class="row mb" style="flex-wrap:wrap;gap:8px;" data-criterion-row>
      <input type="text" placeholder="Tên tiêu chí (VD: Kinh nghiệm)" value="${esc(name)}"
             data-c-name style="flex:1;min-width:150px;">
      <select data-c-type style="width:190px;">
        ${CRITERION_TYPES.map(t =>
          `<option value="${t.value}" ${t.value === meta.value ? "selected" : ""}>${t.label}</option>`).join("")}
      </select>
      <input type="text" placeholder="${esc(meta.hint)}" value="${esc(requirement)}"
             data-c-req style="flex:1;min-width:170px;">
      <select data-c-weight style="width:150px;" title="Trọng số của tiêu chí">
        <option value="1" ${Number(weight) === 1 ? "selected" : ""}>Nhẹ</option>
        <option value="2" ${Number(weight) === 2 ? "selected" : ""}>Quan trọng</option>
        <option value="3" ${Number(weight) === 3 ? "selected" : ""}>Rất quan trọng</option>
      </select>
      <button type="button" class="icon-btn" data-remove-criterion title="Xoá tiêu chí">🗑</button>
    </div>`;
}

function bindCriterionRows() {
  const wrap = $("#jf-criteria");
  wrap.querySelectorAll("[data-remove-criterion]").forEach(b =>
    b.onclick = () => b.closest("[data-criterion-row]").remove());
  wrap.querySelectorAll("[data-c-type]").forEach(sel => {
    sel.onchange = () => {
      const meta = CRITERION_TYPES.find(t => t.value === sel.value);
      const req = sel.closest("[data-criterion-row]").querySelector("[data-c-req]");
      if (meta) req.placeholder = meta.hint;
    };
  });
}

function openJobModal(jobId = null) {
  const modal = $("#job-modal");
  $("#jf-skills").innerHTML = "";
  $("#jf-criteria").innerHTML = "";
  if (jobId) {
    const job = state.jobs.find(j => j.id === jobId);
    if (!job) return;
    $("#job-modal-title").textContent = "Sửa vị trí: " + job.title;
    $("#jf-title").value = job.title;
    $("#jf-dept").value = job.department || "";
    $("#jf-desc").value = job.description || "";
    (job.skills || []).forEach(s => {
      $("#jf-skills").insertAdjacentHTML("beforeend", skillRowHtml(s.name, s.weight || "PREFERRED"));
    });
    (job.criteria || []).forEach(c => {
      $("#jf-criteria").insertAdjacentHTML("beforeend",
        criterionRowHtml(c.name, c.type, c.requirement, c.weight));
    });
    $("#jf-save").dataset.jobId = jobId;
  } else {
    $("#job-modal-title").textContent = "Tạo vị trí tuyển dụng";
    $("#jf-title").value = "";
    $("#jf-dept").value = "";
    $("#jf-desc").value = "";
    $("#jf-skills").innerHTML = skillRowHtml();
    $("#jf-skills").insertAdjacentHTML("beforeend", skillRowHtml());
    delete $("#jf-save").dataset.jobId;
  }
  bindRemoveSkill();
  bindCriterionRows();
  modal.classList.add("open");
}

$("#btn-new-job").addEventListener("click", () => openJobModal());
$("#jf-add-skill").addEventListener("click", () => {
  $("#jf-skills").insertAdjacentHTML("beforeend", skillRowHtml());
  bindRemoveSkill();
});
$("#jf-add-criterion").addEventListener("click", () => {
  $("#jf-criteria").insertAdjacentHTML("beforeend", criterionRowHtml());
  bindCriterionRows();
});

$("#jf-save").addEventListener("click", async () => {
  const skills = [];
  $("#jf-skills").querySelectorAll("[data-skill-row]").forEach(row => {
    const name = row.querySelector("[data-skill-name]").value.trim();
    const weight = row.querySelector("[data-skill-weight]").value;
    if (name) skills.push({ name, weight });
  });
  const criteria = [];
  $("#jf-criteria").querySelectorAll("[data-criterion-row]").forEach(row => {
    const name = row.querySelector("[data-c-name]").value.trim();
    const type = row.querySelector("[data-c-type]").value;
    const requirement = row.querySelector("[data-c-req]").value.trim();
    const weight = Number(row.querySelector("[data-c-weight]").value) || 2;
    if (name && requirement) criteria.push({ name, type, requirement, weight });
    else if (name || requirement) {
      criteria.push(null); // đánh dấu dòng nhập thiếu
    }
  });
  if (criteria.includes(null)) return toast("Mỗi tiêu chí cần có cả tên và yêu cầu.", true);

  const payload = {
    title: $("#jf-title").value.trim(),
    department: $("#jf-dept").value.trim(),
    description: $("#jf-desc").value,
    skills,
    criteria: criteria.filter(Boolean),
  };
  if (!payload.title) return toast("Vui lòng nhập tên vị trí.", true);
  if (!skills.length) return toast("Vui lòng thêm ít nhất một kỹ năng.", true);

  const editing = $("#jf-save").dataset.jobId;
  try {
    if (editing) {
      await api(`/api/admin/jobs/${editing}`, { method: "PUT", body: JSON.stringify(payload) });
      toast("Đã cập nhật vị trí.");
    } else {
      await api("/api/admin/jobs", { method: "POST", body: JSON.stringify(payload) });
      toast("Đã tạo vị trí mới.");
    }
    $("#job-modal").classList.remove("open");
    await loadJobs();
    if (state.currentJob) await openJobDetail(state.currentJob.id);
  } catch (e) {
    toast(e.message, true);
  }
});

async function toggleJob(id) {
  const job = state.jobs.find(j => j.id === id);
  if (!job) return;
  try {
    await api(`/api/admin/jobs/${id}`, {
      method: "PUT",
      body: JSON.stringify({ status: job.status === "OPEN" ? "CLOSED" : "OPEN" }),
    });
    await loadJobs();
    toast(job.status === "OPEN" ? "Đã đóng vị trí." : "Đã mở lại vị trí.");
  } catch (e) {
    toast(e.message, true);
  }
}

async function deleteJob(id) {
  const job = state.jobs.find(j => j.id === id);
  if (!confirm(`Xoá vị trí "${job?.title}" và toàn bộ hồ sơ ứng tuyển của vị trí này?`)) return;
  try {
    await api(`/api/admin/jobs/${id}`, { method: "DELETE" });
    toast("Đã xoá vị trí.");
    await loadJobs();
  } catch (e) {
    toast(e.message, true);
  }
}

/* ================== Chi tiết vị trí + ứng viên ================== */

async function openJobDetail(jobId) {
  try {
    const [job, apps] = await Promise.all([
      api(`/api/admin/jobs/${jobId}`),
      api(`/api/admin/jobs/${jobId}/applications`),
    ]);
    state.currentJob = { id: jobId, title: job.title };
    resetCvForm();

    $("#jobs-view").classList.add("hidden");
    $("#job-detail-view").classList.remove("hidden");

    $("#job-detail-info").innerHTML = `
      <div class="card">
        <div class="row spread">
          <div>
            <h2 style="margin:0 0 2px;">${esc(job.title)}</h2>
            <div class="muted">🏢 ${esc(job.department || "—")} · ${job.status === "OPEN" ? '<span class="badge green">Đang mở</span>' : '<span class="badge gray">Đã đóng</span>'}</div>
          </div>
          <button class="btn btn-outline btn-sm" data-rescore-all="${esc(job.id)}">🔄 Chấm lại tất cả</button>
          <button class="btn btn-outline btn-sm" data-edit-detail="${esc(job.id)}">✏️ Sửa JD</button>
        </div>
        <div class="muted mt" style="font-size:.85rem;">Sau khi sửa JD hoặc kỹ năng, hãy bấm “Chấm lại tất cả” để cập nhật điểm và xếp hạng.</div>
        <div class="chips mt">
          ${(job.skills || []).map(s =>
            `<span class="chip ${s.weight === "REQUIRED" ? "req" : "pref"}">${esc(s.name)} · ${s.weight === "REQUIRED" ? "bắt buộc" : "lợi thế"}</span>`).join("")}
        </div>
        ${(job.criteria || []).length ? `
        <div class="muted mt" style="font-size:.85rem;margin-bottom:4px;">Tiêu chí bổ sung</div>
        <div class="chips">
          ${job.criteria.map(c => {
            const meta = CRITERION_TYPES.find(t => t.value === c.type) || CRITERION_TYPES[4];
            return `<span class="chip">${esc(c.name)}: <b>${esc(c.requirement)}</b> · ${meta.label} · trọng số ${esc(c.weight)}</span>`;
          }).join("")}
        </div>` : ""}
      </div>`;
    $("#job-detail-info").querySelector("[data-edit-detail]")
      .addEventListener("click", () => openJobModal(jobId));
    $("#job-detail-info").querySelector("[data-rescore-all]")
      .addEventListener("click", (e) => rescoreAll(jobId, e.currentTarget));

    const tbody = $("#apps-tbody");
    $("#apps-empty").classList.toggle("hidden", apps.length > 0);
    tbody.innerHTML = apps.map((a, i) => {
      const r = a.finalScore == null;
      return `
        <tr class="clickable" data-app="${esc(a.id)}">
          <td><b>${i + 1}</b></td>
          <td>
            <b>${esc(a.candidateName)}</b><br>
            <span class="muted" style="font-size:.85rem;">${esc(a.email)}${a.phone ? " · " + esc(a.phone) : ""}</span>
          </td>
          <td><span class="score-pill ${scoreClass(a.finalScore)}">${r ? "…" : fmtScore(a.finalScore)}</span></td>
          <td><span class="score-pill ${scoreClass(a.keywordScore)}">${fmtScore(a.keywordScore)}</span></td>
          <td>${a.aiStatus === "OK"
              ? `<span class="score-pill ${scoreClass(a.aiScore)}">${fmtScore(a.aiScore)}</span>`
              : `<span class="badge ${a.aiStatus === "ERROR" ? "red" : "gray"}">${AI_STATUS_LABEL[a.aiStatus] || "—"}</span>`}</td>
          <td>${a.matchedCount != null ? `${a.matchedCount}/${a.totalSkills}` : "—"}</td>
          <td>${a.criteriaTotal ? `${a.criteriaMet}/${a.criteriaTotal}` : `<span class="muted">—</span>`}</td>
          <td><span class="badge ${(DECISION_META[a.hrDecision] || DECISION_META.NEW).cls}">${(DECISION_META[a.hrDecision] || DECISION_META.NEW).label}</span></td>
          <td class="muted" style="font-size:.87rem;">${fmtTime(a.submittedAt)}</td>
          <td style="text-align:right;"><button class="btn btn-outline btn-sm" data-pdf="${esc(a.id)}" data-name="${esc(a.candidateName)}">PDF</button></td>
        </tr>`;
    }).join("") || `<tr><td colspan="10" class="muted" style="text-align:center;padding:28px;">Chưa có CV nào được tải lên cho vị trí này.</td></tr>`;

    tbody.querySelectorAll("[data-app]").forEach(tr =>
      tr.addEventListener("click", (e) => {
        if (e.target.closest("button, a")) return;
        openApplication(tr.dataset.app);
      }));
    tbody.querySelectorAll("[data-pdf]").forEach(btn =>
      btn.addEventListener("click", (e) => {
        e.stopPropagation();
        openCvPdf(btn.dataset.pdf, btn, btn.dataset.name || "");
      }));
    window.scrollTo({ top: 0 });
  } catch (e) {
    toast(e.message, true);
  }
}

/* ================== HR tải CV lên ================== */

const cvInput = $("#cv-file");
const cvDropZone = $("#cv-drop");

cvDropZone.addEventListener("click", () => cvInput.click());
cvDropZone.addEventListener("dragover", (e) => { e.preventDefault(); cvDropZone.classList.add("drag"); });
cvDropZone.addEventListener("dragleave", () => cvDropZone.classList.remove("drag"));
cvDropZone.addEventListener("drop", (e) => {
  e.preventDefault();
  cvDropZone.classList.remove("drag");
  if (e.dataTransfer.files.length) {
    cvInput.files = e.dataTransfer.files;
    updateCvFileLabel();
  }
});
cvInput.addEventListener("change", updateCvFileLabel);

function updateCvFileLabel() {
  const f = cvInput.files[0];
  if (f) {
    cvDropZone.classList.add("has-file");
    $("#cv-drop-text").innerHTML = `📄 <b>${esc(f.name)}</b> (${(f.size / 1024 / 1024).toFixed(2)} MB)`;
  } else {
    resetCvFileLabel();
  }
}

function resetCvFileLabel() {
  cvDropZone.classList.remove("has-file");
  $("#cv-drop-text").innerHTML = '📥 Kéo thả file PDF vào đây hoặc <b>nhấn để chọn file</b>';
}

function resetCvForm() {
  $("#cv-upload-form").reset();
  $("#cv-upload-error").classList.add("hidden");
  resetCvFileLabel();
}

function showUploadError(msg) {
  const err = $("#cv-upload-error");
  err.textContent = msg;
  err.classList.remove("hidden");
}

$("#cv-upload-form").addEventListener("submit", async (e) => {
  e.preventDefault();
  if (!state.currentJob) return;

  const file = cvInput.files[0];
  $("#cv-upload-error").classList.add("hidden");
  if (!$("#cv-name").value.trim()) return showUploadError("Vui lòng nhập họ tên ứng viên.");
  if (!file) return showUploadError("Vui lòng chọn file CV (PDF).");
  if (!/\.pdf$/i.test(file.name)) return showUploadError("Chỉ nhận file PDF. Hãy xuất CV ra PDF rồi tải lên.");

  const btn = $("#cv-upload-btn");
  btn.disabled = true;
  btn.innerHTML = '<span class="spinner"></span> Đang phân tích CV…';
  const jobId = state.currentJob.id;
  try {
    const fd = new FormData();
    fd.append("name", $("#cv-name").value.trim());
    fd.append("email", $("#cv-email").value.trim());
    fd.append("phone", $("#cv-phone").value.trim());
    fd.append("file", file);

    const app = await api(`/api/admin/jobs/${jobId}/cv`, { method: "POST", body: fd });
    const score = app.report ? Math.round(app.report.finalScore) : null;
    toast(score == null ? "Đã tải CV lên." : `Đã tải CV và chấm điểm: ${score} điểm.`);
    resetCvForm();
    await loadJobs();
    await openJobDetail(jobId);
  } catch (err) {
    showUploadError(err.message);
  } finally {
    btn.disabled = false;
    btn.textContent = "Tải lên & chấm điểm";
  }
});

/* ================== Chi tiết hồ sơ ================== */

let cvPageUrls = [];
let cvPdfUrl = null;
let cvFileName = "CV.pdf";
let cvApplicationId = null;
let cvBusy = false;

/**
 * Xem CV ngay trong modal.
 *
 * Vì sao không dùng <a href="/api/.../cv"> hay window.open trực tiếp:
 *  - Điều hướng tab mới không gửi header X-Auth-Token → server trả 401.
 *  - Nhiều trình duyệt/ứng dụng chặn xem PDF nhúng hoặc chặn blob URL mở
 *    ở tab khác → khung xem trắng.
 * Nên: tải thông tin CV kèm token, rồi tải từng trang đã kết xuất thành ảnh
 * PNG (server render bằng PDFBox) và hiển thị trong modal. Bản PDF gốc vẫn
 * tải được qua nút "Tải PDF gốc".
 */
async function openCvPdf(id, btn = null, label = "") {
  const modal = $("#cv-modal");
  const loading = $("#cv-loading");
  const pagesBox = $("#cv-pages");

  cvApplicationId = id;
  releaseCvUrls();
  pagesBox.innerHTML = "";
  $("#cv-modal-title").textContent = label ? `CV: ${label}` : "CV ứng viên";
  $("#cv-meta").textContent = "Đang tải…";
  loading.textContent = "Đang tải CV…";
  loading.classList.remove("hidden");
  modal.classList.add("open");
  pagesBox.scrollTop = 0;
  if (btn) btn.disabled = true;
  cvBusy = true;

  try {
    const info = await cvFetch(`/api/admin/applications/${id}/cv/info`);
    const data = await info.json();
    const total = Number(data.pages) || 0;
    cvFileName = data.fileName || `CV_${id}.pdf`;
    if (!total) throw new Error("File CV không có trang nào đọc được.");
    $("#cv-meta").textContent =
      `${cvFileName} · ${total} trang · ${(Number(data.sizeBytes || 0) / 1024).toFixed(0)} KB`;

    for (let p = 1; p <= total; p++) {
      loading.textContent = total > 1 ? `Đang tải trang ${p}/${total}…` : "Đang tải CV…";
      const res = await cvFetch(`/api/admin/applications/${id}/cv/pages/${p}`);
      const blob = await res.blob();
      if (!blob.size) throw new Error(`Trang ${p} không đọc được.`);
      const url = URL.createObjectURL(blob);
      cvPageUrls.push(url);

      const img = document.createElement("img");
      img.src = url;
      img.alt = `Trang ${p} của CV ${cvFileName}`;
      img.loading = "lazy";
      pagesBox.appendChild(img);
    }
    loading.classList.add("hidden");
  } catch (e) {
    loading.textContent = "Không mở được CV: " + e.message;
    loading.classList.remove("hidden");
    $("#cv-meta").textContent = "";
    toast("Không mở được CV: " + e.message, true);
  } finally {
    cvBusy = false;
    if (btn) btn.disabled = false;
  }
}

/** fetch kèm token + chuẩn hoá thông báo lỗi cho các endpoint CV. */
async function cvFetch(url) {
  const res = await fetch(url, {
    headers: state.token ? { "X-Auth-Token": state.token } : {},
  });
  if (!res.ok) {
    let msg = `Lỗi ${res.status}`;
    try {
      const data = await res.json();
      if (data && data.error) msg = data.error;
    } catch { /* không phải JSON */ }
    if (res.status === 401) msg = "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.";
    throw new Error(msg);
  }
  return res;
}

/** Tải file PDF gốc (dùng cho nút tải về / mở tab mới). */
async function loadCvFile() {
  if (cvPdfUrl) return cvPdfUrl;
  if (!cvApplicationId) throw new Error("Chưa chọn hồ sơ nào.");
  const res = await cvFetch(`/api/admin/applications/${cvApplicationId}/cv`);
  const blob = await res.blob();
  if (!blob.size) throw new Error("File CV rỗng hoặc không đọc được.");
  cvPdfUrl = URL.createObjectURL(blob);
  return cvPdfUrl;
}

function releaseCvUrls() {
  cvPageUrls.forEach(u => URL.revokeObjectURL(u));
  cvPageUrls = [];
  if (cvPdfUrl) {
    URL.revokeObjectURL(cvPdfUrl);
    cvPdfUrl = null;
  }
}

function closeCvViewer() {
  releaseCvUrls();
  $("#cv-pages").innerHTML = "";
  cvApplicationId = null;
  $("#cv-modal").classList.remove("open");
}

async function downloadCvFile(btn) {
  if (cvBusy) return;
  if (btn) btn.disabled = true;
  try {
    const url = await loadCvFile();
    const a = document.createElement("a");
    a.href = url;
    a.download = cvFileName;
    document.body.appendChild(a);
    a.click();
    a.remove();
  } catch (e) {
    toast("Không tải được CV: " + e.message, true);
  } finally {
    if (btn) btn.disabled = false;
  }
}

/**
 * Mở bản PDF gốc ở tab mới bằng link trực tiếp — server nhận diện phiên qua
 * cookie HttpOnly (đã đồng bộ khi đăng nhập), nên tab mới không cần header.
 * Nếu bị chặn popup thì tải file về.
 */
async function openCvInNewTab(btn) {
  if (!cvApplicationId) return;
  const win = window.open(`/api/admin/applications/${cvApplicationId}/cv`, "_blank");
  if (win) return;
  await downloadCvFile(btn);
  toast("Trình duyệt chặn tab mới — CV đã được tải về thay vì mở.");
}

$("#btn-cv-download").addEventListener("click", (e) => downloadCvFile(e.currentTarget));
$("#btn-cv-newtab").addEventListener("click", (e) => openCvInNewTab(e.currentTarget));

async function openApplication(id) {
  try {
    const app = await api(`/api/admin/applications/${id}`);
    state.currentApplicationId = id;
    const r = app.report;
    // Lấy JD hiện tại để HR đối chiếu; nếu job đã bị xoá thì bỏ qua
    const job = await api(`/api/admin/jobs/${app.jobId}`).catch(() => null);

    $("#app-modal-title").textContent = `Hồ sơ: ${app.candidateName}`;
    $("#btn-view-cv").onclick = () => openCvPdf(id, $("#btn-view-cv"), app.candidateName);

    const skillMatches = (r && r.skillMatches) || [];
    const criteriaResults = (r && r.criteriaResults) || [];

    // Cột trái = yêu cầu HR đưa ra, cột phải = kết quả quét CV tương ứng
    const skillCompare = skillMatches.map(sm => {
      const meta = STATUS_META[sm.status] || STATUS_META.MISSING;
      return `
        <div class="compare-row">
          <div class="compare-cell jd">
            <div class="c-title">${esc(sm.skillName)}</div>
            <div class="c-sub">
              <span class="chip ${sm.weight === "REQUIRED" ? "req" : "pref"}">${sm.weight === "REQUIRED" ? "Bắt buộc" : "Lợi thế"}</span>
            </div>
          </div>
          <div class="compare-cell cv">
            <div class="c-head">
              <span class="badge ${meta.cls}">${meta.icon} ${meta.label}</span>
              <span class="badge ${sm.source === "BOTH" ? "green" : sm.source === "NONE" ? "gray" : "blue"}">${SOURCE_LABEL[sm.source] || "—"}</span>
              ${sm.occurrences > 0 ? `<span class="muted" style="font-size:.82rem;">×${sm.occurrences} lần trong CV</span>` : ""}
            </div>
            ${sm.evidence ? `<div class="quote">“${esc(sm.evidence)}”</div>` : `<div class="muted" style="font-size:.85rem;">Không tìm thấy bằng chứng trong CV.</div>`}
            ${sm.aiNote ? `<div class="ai-note">🤖 ${esc(sm.aiNote)}</div>` : ""}
          </div>
        </div>`;
    }).join("");

    const criteriaCompare = criteriaResults.map(c => {
      const meta = CRITERION_STATUS_META[c.status] || CRITERION_STATUS_META.UNKNOWN;
      const typeMeta = CRITERION_TYPES.find(t => t.value === c.type) || CRITERION_TYPES[4];
      return `
        <div class="compare-row">
          <div class="compare-cell jd">
            <div class="c-title">${esc(c.name)}</div>
            <div class="c-sub">
              <span class="chip">${typeMeta.label}</span>
              <span class="badge gray">trọng số ${esc(c.weight)}</span>
            </div>
            <div class="c-req">Yêu cầu: <b>${esc(c.requirement)}</b></div>
          </div>
          <div class="compare-cell cv">
            <div class="c-head">
              <span class="badge ${meta.cls}">${meta.icon} ${meta.label}</span>
              ${c.source === "AI"
                ? `<span class="badge ${c.inferred ? "amber" : "blue"}">${c.inferred ? "AI suy luận" : "AI bổ sung"}</span>`
                : c.source === "BOTH" ? `<span class="badge blue">Quy tắc + AI</span>` : ""}
            </div>
            <div class="c-req">${c.actual ? `Thực tế: <b>${esc(c.actual)}</b>` : `<span class="muted">CV không nêu thông tin.</span>`}</div>
            ${c.note ? `<div class="muted" style="font-size:.85rem;">${esc(c.note)}</div>` : ""}
            ${c.evidence ? `<div class="quote">“${esc(c.evidence)}”</div>` : ""}
          </div>
        </div>`;
    }).join("");

    const hasCompare = skillMatches.length > 0 || criteriaResults.length > 0;
    const skillMatched = skillMatches.filter(s => s.status === "MATCHED").length;
    const criteriaMet = criteriaResults.filter(c => c.status === "MET").length;
    const compareSummary = [
      skillMatches.length ? `${skillMatched}/${skillMatches.length} kỹ năng khớp` : "",
      criteriaResults.length ? `${criteriaMet}/${criteriaResults.length} tiêu chí đạt` : "",
    ].filter(Boolean).join(" · ");
    const compareBlock = hasCompare ? `
      <div class="row spread mt" style="margin-bottom:4px;">
        <h4 style="margin:0;">Đối chiếu JD ↔ CV</h4>
        <span class="muted" style="font-size:.82rem;">${compareSummary}</span>
      </div>
      <div class="compare">
        <div class="compare-head">
          <div>📋 Yêu cầu trong JD</div>
          <div>🔍 Kết quả scan CV</div>
        </div>
        ${skillMatches.length ? `
          <div class="compare-group">Kỹ năng yêu cầu</div>
          ${skillCompare}` : ""}
        ${criteriaResults.length ? `
          <div class="compare-group">Tiêu chí bổ sung</div>
          ${criteriaCompare}` : ""}
      </div>` : "";

    const aiBlock = r && r.aiStatus === "OK" ? `
      <div class="card mt">
        <h4 style="margin:0 0 6px;">🤖 Nhận xét của AI</h4>
        <p class="desc-pre" style="margin-top:4px;">${esc(r.aiSummary)}</p>
        ${(r.aiDimensions || []).length ? `
        <div class="dim-list mt">
          ${r.aiDimensions.map(d => `
            <div class="dim-row">
              <div class="dim-head"><span>${esc(d.name)}</span><b>${fmtScore(d.score)}</b></div>
              <div class="dim-bar"><span style="width:${Math.max(0, Math.min(100, Number(d.score) || 0))}%"></span></div>
              ${d.comment ? `<div class="muted" style="font-size:.83rem;">${esc(d.comment)}</div>` : ""}
            </div>`).join("")}
        </div>` : ""}
        ${(r.aiHighlights || []).length ? `<b style="font-size:.9rem;">Điểm mạnh</b><ul class="hl-list">${r.aiHighlights.map(h => `<li>${esc(h)}</li>`).join("")}</ul>` : ""}
        ${(r.aiConcerns || []).length ? `<b style="font-size:.9rem;">Điểm cần làm rõ</b><ul class="hl-list concerns">${r.aiConcerns.map(c => `<li>${esc(c)}</li>`).join("")}</ul>` : ""}
      </div>` : r ? `
      <div class="alert warn mt">⚠️ Tiêu chí AI chưa chấm: ${esc(AI_STATUS_LABEL[r.aiStatus] || r.aiStatus)}
        ${r.aiError ? `— ${esc(r.aiError)}` : ""}.
        ${r.aiStatus === "NOT_CONFIGURED" ? "Hãy đặt <b>AI_API_KEY</b> (và <b>AI_PROVIDER</b>, <b>AI_MODEL</b> nếu dùng DeepSeek/OpenAI) trong <code>config.properties</code> rồi khởi động lại server." : ""}
      </div>` : "";

    $("#app-modal-body").innerHTML = `
      <div class="row" style="justify-content:space-between;align-items:flex-start;">
        <div>
          <b style="font-size:1.05rem;">${esc(app.candidateName)}</b>
          <div class="muted" style="font-size:.9rem;">${esc(app.email)}${app.phone ? " · " + esc(app.phone) : ""}</div>
          <div class="muted" style="font-size:.9rem;">📎 ${esc(app.cvFileName)} · Nộp lúc ${fmtTime(app.submittedAt)}</div>
        </div>
      </div>

      <div class="notice mt">
        ⚖️ Điểm số chỉ mang tính <b>tham khảo</b> cho HR khi sàng lọc. AI không tự đưa ra quyết định tuyển dụng.
      </div>

      <div class="card mt">
        <div class="row spread">
          <b>Quyết định của HR</b>
          <span class="badge ${(DECISION_META[app.hrDecision] || DECISION_META.NEW).cls}">${(DECISION_META[app.hrDecision] || DECISION_META.NEW).label}</span>
        </div>
        <div class="row mt" style="align-items:flex-end;">
          <label class="field" style="flex:1;min-width:160px;">
            <span>Trạng thái</span>
            <select id="hr-decision">
              <option value="NEW" ${app.hrDecision === "NEW" ? "selected" : ""}>Chưa xử lý</option>
              <option value="SHORTLISTED" ${app.hrDecision === "SHORTLISTED" ? "selected" : ""}>Mời phỏng vấn</option>
              <option value="REJECTED" ${app.hrDecision === "REJECTED" ? "selected" : ""}>Từ chối</option>
            </select>
          </label>
          <label class="field" style="flex:2;min-width:200px;">
            <span>Ghi chú nội bộ</span>
            <input type="text" id="hr-note" maxlength="500" placeholder="VD: Cần hỏi thêm về kinh nghiệm Docker" value="${esc(app.hrNote)}">
          </label>
          <button class="btn btn-primary" id="hr-save-decision">Lưu quyết định</button>
        </div>
      </div>

      ${r ? `
      <div class="grid grid-stats mt">
        <div class="card score-card"><div class="num" style="color:${r.finalScore >= 75 ? "var(--green)" : r.finalScore >= 50 ? "var(--amber)" : "var(--red)"}">${fmtScore(r.finalScore)}</div><div class="lbl">Điểm tổng</div></div>
        <div class="card score-card"><div class="num" style="color:${r.keywordScore >= 75 ? "var(--green)" : r.keywordScore >= 50 ? "var(--amber)" : "var(--red)"}">${fmtScore(r.keywordScore)}</div><div class="lbl">① Khớp từ khoá</div></div>
        <div class="card score-card"><div class="num" style="color:var(--primary);">${r.aiStatus === "OK" ? fmtScore(r.aiScore) : "—"}</div><div class="lbl">② AI</div></div>
        <div class="card score-card"><div class="num" style="color:${(r.criteriaScore ?? -1) >= 75 ? "var(--green)" : (r.criteriaScore ?? -1) >= 50 ? "var(--amber)" : "var(--red)"}">${r.criteriaScore >= 0 ? fmtScore(r.criteriaScore) : "—"}</div><div class="lbl">③ Tiêu chí bổ sung</div></div>
        <div class="card score-card"><div class="num">${(r.skillMatches || []).filter(s => s.status === "MATCHED").length}/${(r.skillMatches || []).length}</div><div class="lbl">Kỹ năng khớp</div></div>
        <div class="card score-card"><div class="num">${criteriaResults.length ? `${criteriaResults.filter(c => c.status === "MET").length}/${criteriaResults.length}` : "—"}</div><div class="lbl">Tiêu chí đạt</div></div>
      </div>` : `<div class="alert warn mt">Hồ sơ chưa được chấm điểm.</div>`}

      ${job ? `<details class="jd-box mt">
        <summary>📄 Nội dung JD đã đưa ra: <b>${esc(job.title)}</b>${job.department ? ` · ${esc(job.department)}` : ""}</summary>
        <div class="desc-pre mt">${esc(job.description || "(không có mô tả)")}</div>
      </details>` : ""}

      ${compareBlock}

      ${aiBlock}
      <p class="muted mt" style="font-size:.82rem;">Chấm lúc: ${r?.scoredAt ? fmtTime(r.scoredAt) : "—"}</p>
    `;

    $("#app-modal").classList.add("open");
    $("#hr-save-decision").addEventListener("click", () => saveDecision(id));
  } catch (e) {
    toast(e.message, true);
  }
}

async function saveDecision(id) {
  const btn = $("#hr-save-decision");
  btn.disabled = true;
  try {
    await api(`/api/admin/applications/${id}/decision`, {
      method: "POST",
      body: JSON.stringify({
        decision: $("#hr-decision").value,
        note: $("#hr-note").value,
      }),
    });
    toast("Đã lưu quyết định của HR.");
    await openApplication(id);
    if (state.currentJob) await openJobDetail(state.currentJob.id);
  } catch (e) {
    toast(e.message, true);
  } finally {
    btn.disabled = false;
  }
}

async function rescoreAll(jobId, btn) {
  if (!confirm("Chấm lại toàn bộ hồ sơ của vị trí này?\nNếu đã bật AI, thao tác có thể mất vài phút.")) return;
  const original = btn.textContent;
  btn.disabled = true;
  btn.textContent = "Đang chấm lại…";
  try {
    const res = await api(`/api/admin/jobs/${jobId}/rescore-all`, { method: "POST" });
    toast(`Đã chấm lại ${res.rescored} hồ sơ.`);
    await loadJobs();
    await openJobDetail(jobId);
  } catch (e) {
    toast(e.message, true);
    btn.disabled = false;
    btn.textContent = original;
  }
}

$("#btn-rescore").addEventListener("click", async () => {
  if (!state.currentApplicationId) return;
  const btn = $("#btn-rescore");
  btn.disabled = true;
  btn.innerHTML = '<span class="spinner dark"></span> Đang chấm lại…';
  try {
    await api(`/api/admin/applications/${state.currentApplicationId}/rescore`, { method: "POST" });
    toast("Đã chấm lại hồ sơ.");
    await openApplication(state.currentApplicationId);
    if (state.currentJob) await openJobDetail(state.currentJob.id);
  } catch (e) {
    toast(e.message, true);
  } finally {
    btn.disabled = false;
    btn.textContent = "🔄 Chấm lại với AI";
  }
});

/* ================== Đóng modal ================== */

document.querySelectorAll(".modal-overlay").forEach(ov => {
  ov.addEventListener("click", (e) => {
    if (e.target === ov) ov.classList.remove("open");
  });
  ov.querySelectorAll("[data-close]").forEach(b =>
    b.addEventListener("click", () => ov.classList.remove("open")));
});

// Đóng khung xem CV thì giải phóng blob URL (tránh giữ file PDF trong bộ nhớ)
document.querySelectorAll("#cv-modal [data-close]").forEach(b =>
  b.addEventListener("click", closeCvViewer));
$("#cv-modal").addEventListener("click", (e) => {
  if (e.target.id === "cv-modal") closeCvViewer();
});

/* ================== Start ================== */

if (state.token) boot(); else showLogin();
