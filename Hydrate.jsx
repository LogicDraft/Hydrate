import { useState, useEffect, useRef, useCallback } from "react";

// ─── M3 Tokens ───────────────────────────────────────────────────────────────
const M3 = {
  bg: "#0a0a0a", surface: "#141414", surfaceVariant: "#1e1e1e",
  outline: "#2e2e2e", outlineVariant: "#1a1a1a",
  onBg: "#f5f5f5", onSurface: "#e0e0e0", onSurfaceVariant: "#888888",
  primary: "#ffffff", onPrimary: "#000000",
  error: "#ff5449", warn: "#f5a623", success: "#5adb7f",
};

const RATE_LIMIT_MIN = 20;   // minutes between logs
const SLOT_WINDOW_MIN = 15;  // ±minutes around a slot to allow logging
const CONFIRM_SEC = 3;       // countdown seconds

const css = () => `
  @import url('https://fonts.googleapis.com/css2?family=DM+Sans:opsz,wght@9..40,300;9..40,400;9..40,500;9..40,600&family=DM+Mono:wght@400;500&display=swap');
  *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
  body { background: #0a0a0a; font-family: 'DM Sans', sans-serif; -webkit-font-smoothing: antialiased; }
  input[type=number]::-webkit-inner-spin-button { -webkit-appearance: none; }
  input[type=time]::-webkit-calendar-picker-indicator { filter: invert(1); opacity: 0.4; cursor: pointer; }
  ::-webkit-scrollbar { width: 3px; }
  ::-webkit-scrollbar-track { background: #141414; }
  ::-webkit-scrollbar-thumb { background: #2e2e2e; border-radius: 2px; }
  .card { transition: border-color 0.2s; }
  .card:hover { border-color: #3a3a3a !important; }
  .press:active { transform: scale(0.96); }
  @keyframes fadeUp   { from { opacity:0; transform:translateY(18px); } to { opacity:1; transform:translateY(0); } }
  @keyframes scaleIn  { from { opacity:0; transform:scale(0.92); }      to { opacity:1; transform:scale(1); } }
  @keyframes slideUp  { from { opacity:0; transform:translateY(40px); } to { opacity:1; transform:translateY(0); } }
  @keyframes countdown-shrink { from { width:100%; } to { width:0%; } }
  @keyframes shake    { 0%,100%{transform:translateX(0)} 20%{transform:translateX(-6px)} 60%{transform:translateX(6px)} 80%{transform:translateX(-3px)} }
  @keyframes pop      { 0%{transform:scale(1)} 50%{transform:scale(1.06)} 100%{transform:scale(1)} }
`;

// ─── Helpers ─────────────────────────────────────────────────────────────────
const toMin = t => { const [h, m] = t.split(":").map(Number); return h * 60 + m; };
const nowMin = () => { const n = new Date(); return n.getHours() * 60 + n.getMinutes(); };
const today = () => new Date().toISOString().split("T")[0];
const nowTime = () => new Date().toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit", hour12: false });
const nowTs = () => Date.now();

function buildSchedule(wakeTime, sleepTime, totalMl, cupSize) {
  const wakeMin = toMin(wakeTime), sleepMin = toMin(sleepTime);
  const totalMin = sleepMin > wakeMin ? sleepMin - wakeMin : 1440 - wakeMin + sleepMin;
  const numCups = Math.max(1, Math.round(totalMl / cupSize));
  const interval = Math.floor(totalMin / numCups);
  return Array.from({ length: numCups }, (_, i) => {
    const min = (wakeMin + i * interval) % 1440;
    const h = Math.floor(min / 60), m = min % 60;
    const time24 = `${String(h).padStart(2,"0")}:${String(m).padStart(2,"0")}`;
    const ampm = h < 12 ? "AM" : "PM";
    const h12 = h % 12 === 0 ? 12 : h % 12;
    return { time24, label: `${h12}:${String(m).padStart(2,"0")} ${ampm}`, amount: cupSize, index: i };
  });
}

// ─── Anti-Fake Engine ─────────────────────────────────────────────────────────
function validateLog(amount, logs, schedule, settings) {
  const todayLogs = logs[today()] || [];
  const nm = nowMin();

  // 1. Daily cap: goal + 20%
  const consumed = todayLogs.reduce((s, l) => s + l.amount, 0);
  const cap = settings.totalMl * 1.2;
  if (consumed + amount > cap) {
    return { ok: false, reason: "daily_cap", msg: `Daily cap reached (${Math.round(cap / 1000 * 10) / 10}L). You've had enough! 🎉` };
  }

  // 2. Rate limit: last log within RATE_LIMIT_MIN
  if (todayLogs.length > 0) {
    const lastTs = todayLogs[todayLogs.length - 1].ts;
    const minsSince = (nowTs() - lastTs) / 60000;
    if (minsSince < RATE_LIMIT_MIN) {
      const wait = Math.ceil(RATE_LIMIT_MIN - minsSince);
      return { ok: false, reason: "rate_limit", msg: `You logged ${Math.floor(minsSince)} min ago. Wait ${wait} more min ⏳` };
    }
  }

  // 3. Schedule lock: must be within ±SLOT_WINDOW_MIN of a scheduled slot
  const nearSlot = schedule.find(s => Math.abs(toMin(s.time24) - nm) <= SLOT_WINDOW_MIN);
  if (!nearSlot) {
    const next = schedule.find(s => toMin(s.time24) > nm);
    const nextLabel = next ? next.label : "end of day";
    return { ok: false, reason: "slot_lock", msg: `Not a log window. Next slot at ${nextLabel} 🕐` };
  }

  return { ok: true, slot: nearSlot };
}

// ─── Icons ───────────────────────────────────────────────────────────────────
const DropIcon = ({ size = 24, filled = false, color = "currentColor" }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none">
    <path d="M12 2C12 2 5 10.5 5 14.5C5 18.09 8.13 21 12 21C15.87 21 19 18.09 19 14.5C19 10.5 12 2 12 2Z"
      fill={filled ? color : "none"} stroke={color} strokeWidth="1.5" strokeLinejoin="round" />
  </svg>
);
const CheckIcon = ({ size = 16, color = "#000" }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth="2.5" strokeLinecap="round">
    <polyline points="20 6 9 17 4 12" />
  </svg>
);
const XIcon = ({ size = 16, color = "currentColor" }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth="2" strokeLinecap="round">
    <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
  </svg>
);
const LockIcon = ({ size = 16, color = "currentColor" }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth="1.5" strokeLinecap="round">
    <rect x="3" y="11" width="18" height="11" rx="2" /><path d="M7 11V7a5 5 0 0 1 10 0v4" />
  </svg>
);
const GearIcon = ({ size = 20, color = "currentColor" }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth="1.5" strokeLinecap="round">
    <circle cx="12" cy="12" r="3" />
    <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06-.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z" />
  </svg>
);
const CalIcon = ({ size = 20, color = "currentColor" }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth="1.5" strokeLinecap="round">
    <rect x="3" y="4" width="18" height="18" rx="3" />
    <line x1="16" y1="2" x2="16" y2="6" /><line x1="8" y1="2" x2="8" y2="6" /><line x1="3" y1="10" x2="21" y2="10" />
  </svg>
);

// ─── M3 Components ────────────────────────────────────────────────────────────
const Card = ({ children, style = {} }) => (
  <div className="card" style={{ background: M3.surface, border: `1px solid ${M3.outline}`, borderRadius: 24, padding: "20px 22px", ...style }}>
    {children}
  </div>
);
const Label = ({ children }) => (
  <p style={{ fontSize: 10, fontWeight: 600, color: M3.onSurfaceVariant, letterSpacing: 1.4, textTransform: "uppercase", marginBottom: 14 }}>{children}</p>
);
const Chip = ({ label, selected, onClick }) => (
  <button onClick={onClick} className="press" style={{
    background: selected ? M3.primary : "transparent", color: selected ? M3.onPrimary : M3.onSurfaceVariant,
    border: `1px solid ${selected ? M3.primary : M3.outline}`, borderRadius: 99,
    padding: "7px 16px", fontSize: 12, fontWeight: 500, fontFamily: "'DM Sans',sans-serif",
    cursor: "pointer", transition: "all 0.18s",
  }}>{label}</button>
);
const FilledBtn = ({ children, onClick, style = {}, disabled = false }) => (
  <button onClick={onClick} disabled={disabled} className="press" style={{
    background: disabled ? M3.outline : M3.primary, color: disabled ? M3.onSurfaceVariant : M3.onPrimary,
    border: "none", borderRadius: 99, padding: "15px 28px", fontSize: 14, fontWeight: 600,
    fontFamily: "'DM Sans',sans-serif", cursor: disabled ? "not-allowed" : "pointer", transition: "all 0.18s",
    ...style,
  }}>{children}</button>
);
const OutlinedBtn = ({ children, onClick, style = {} }) => (
  <button onClick={onClick} className="press" style={{
    background: "transparent", color: M3.onSurface, border: `1px solid ${M3.outline}`,
    borderRadius: 99, padding: "15px 28px", fontSize: 14, fontWeight: 500,
    fontFamily: "'DM Sans',sans-serif", cursor: "pointer", transition: "all 0.18s", ...style,
  }}>{children}</button>
);
const Field = ({ label, value, onChange, type = "text", suffix = "", min, max }) => (
  <div>
    <p style={{ fontSize: 10, fontWeight: 600, color: M3.onSurfaceVariant, letterSpacing: 1.4, textTransform: "uppercase", marginBottom: 8 }}>{label}</p>
    <div style={{ position: "relative" }}>
      <input type={type} value={value} onChange={onChange} min={min} max={max} style={{
        width: "100%", background: M3.surfaceVariant, border: `1px solid ${M3.outline}`, borderRadius: 14,
        padding: suffix ? "14px 48px 14px 16px" : "14px 16px",
        fontSize: 15, fontFamily: "'DM Mono',monospace", color: M3.onBg, outline: "none", transition: "border-color 0.2s",
      }}
        onFocus={e => e.target.style.borderColor = "#555"}
        onBlur={e => e.target.style.borderColor = M3.outline}
      />
      {suffix && <span style={{ position: "absolute", right: 14, top: "50%", transform: "translateY(-50%)", fontSize: 12, color: M3.onSurfaceVariant, fontFamily: "'DM Mono',monospace" }}>{suffix}</span>}
    </div>
  </div>
);

// ─── Ring ─────────────────────────────────────────────────────────────────────
const Ring = ({ percent, size = 210, stroke = 11, children }) => {
  const r = (size - stroke) / 2;
  const circ = 2 * Math.PI * r;
  const offset = circ - (Math.min(100, percent) / 100) * circ;
  const color = percent >= 100 ? M3.success : M3.primary;
  return (
    <div style={{ position: "relative", width: size, height: size }}>
      <svg width={size} height={size} style={{ transform: "rotate(-90deg)", display: "block" }}>
        <circle cx={size/2} cy={size/2} r={r} fill="none" stroke={M3.outlineVariant} strokeWidth={stroke} />
        <circle cx={size/2} cy={size/2} r={r} fill="none" stroke={color} strokeWidth={stroke}
          strokeLinecap="round" strokeDasharray={circ} strokeDashoffset={offset}
          style={{ transition: "stroke-dashoffset 0.9s cubic-bezier(0.4,0,0.2,1), stroke 0.4s" }} />
      </svg>
      <div style={{ position: "absolute", inset: 0, display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center" }}>
        {children}
      </div>
    </div>
  );
};

// ─── Confirmation Modal ───────────────────────────────────────────────────────
function ConfirmModal({ amount, onConfirm, onCancel }) {
  const [count, setCount] = useState(CONFIRM_SEC);
  const [confirmed, setConfirmed] = useState(false);
  const timerRef = useRef(null);

  useEffect(() => {
    timerRef.current = setInterval(() => {
      setCount(c => {
        if (c <= 1) { clearInterval(timerRef.current); return 0; }
        return c - 1;
      });
    }, 1000);
    return () => clearInterval(timerRef.current);
  }, []);

  const handleCancel = () => {
    clearInterval(timerRef.current);
    onCancel();
  };

  const handleConfirm = () => {
    clearInterval(timerRef.current);
    setConfirmed(true);
    setTimeout(onConfirm, 300);
  };

  const progressW = ((CONFIRM_SEC - count) / CONFIRM_SEC) * 100;

  return (
    <div style={{
      position: "fixed", inset: 0, background: "rgba(0,0,0,0.7)",
      display: "flex", alignItems: "flex-end", justifyContent: "center",
      zIndex: 200, backdropFilter: "blur(8px)",
    }}>
      <div style={{
        background: M3.surface, border: `1px solid ${M3.outline}`,
        borderRadius: "28px 28px 0 0", padding: "28px 24px 40px",
        width: "100%", maxWidth: 480,
        animation: "slideUp 0.3s cubic-bezier(0.34,1.2,0.64,1) both",
      }}>
        {/* Handle */}
        <div style={{ width: 40, height: 4, background: M3.outline, borderRadius: 2, margin: "0 auto 24px" }} />

        {/* Icon */}
        <div style={{ display: "flex", justifyContent: "center", marginBottom: 16 }}>
          <div style={{
            width: 64, height: 64, borderRadius: "50%",
            background: confirmed ? "rgba(90,219,127,0.12)" : M3.surfaceVariant,
            border: `1px solid ${confirmed ? M3.success : M3.outline}`,
            display: "flex", alignItems: "center", justifyContent: "center",
            transition: "all 0.3s",
          }}>
            {confirmed
              ? <CheckIcon size={28} color={M3.success} />
              : <DropIcon size={28} filled color={M3.primary} />
            }
          </div>
        </div>

        <h3 style={{ textAlign: "center", fontSize: 20, fontWeight: 600, color: M3.onBg, marginBottom: 8 }}>
          {confirmed ? "Logged! 💧" : `Log +${amount}ml?`}
        </h3>
        <p style={{ textAlign: "center", fontSize: 13, color: M3.onSurfaceVariant, marginBottom: 28, lineHeight: 1.6 }}>
          {confirmed
            ? "Great job staying hydrated!"
            : "Are you actually drinking right now? This will be saved to your log."}
        </p>

        {/* Countdown bar */}
        {!confirmed && (
          <div style={{ marginBottom: 24 }}>
            <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 8 }}>
              <span style={{ fontSize: 11, color: M3.onSurfaceVariant, letterSpacing: 0.5 }}>Auto-cancels in</span>
              <span style={{ fontSize: 11, fontWeight: 600, fontFamily: "'DM Mono',monospace", color: M3.onBg }}>{count}s</span>
            </div>
            <div style={{ height: 4, background: M3.outline, borderRadius: 2, overflow: "hidden" }}>
              <div style={{
                height: "100%", background: M3.primary, borderRadius: 2,
                width: `${((CONFIRM_SEC - count) / CONFIRM_SEC) * 100}%`,
                transition: "width 1s linear",
              }} />
            </div>
            <p style={{ fontSize: 10, color: M3.onSurfaceVariant, marginTop: 6, textAlign: "center", letterSpacing: 0.3 }}>
              Tap cancel if you didn't actually drink
            </p>
          </div>
        )}

        {!confirmed && (
          <div style={{ display: "flex", gap: 12 }}>
            <OutlinedBtn onClick={handleCancel} style={{ flex: 1 }}>
              Cancel
            </OutlinedBtn>
            <FilledBtn onClick={handleConfirm} style={{ flex: 1 }}>
              Yes, I drank it ✓
            </FilledBtn>
          </div>
        )}
      </div>
    </div>
  );
}

// ─── Toast ────────────────────────────────────────────────────────────────────
function Toast({ msg, type = "error", onDone }) {
  useEffect(() => {
    const t = setTimeout(onDone, 3200);
    return () => clearTimeout(t);
  }, []);

  const colors = {
    error: { bg: "rgba(255,84,73,0.12)", border: "rgba(255,84,73,0.3)", text: M3.error },
    warn:  { bg: "rgba(245,166,35,0.12)", border: "rgba(245,166,35,0.3)", text: M3.warn },
    info:  { bg: "rgba(255,255,255,0.06)", border: M3.outline, text: M3.onSurface },
  };
  const c = colors[type] || colors.info;

  return (
    <div style={{
      position: "fixed", top: 20, left: "50%", transform: "translateX(-50%)",
      zIndex: 300, animation: "fadeUp 0.3s ease both",
      maxWidth: 400, width: "calc(100% - 40px)",
    }}>
      <div style={{
        background: c.bg, border: `1px solid ${c.border}`, borderRadius: 16,
        padding: "14px 18px", display: "flex", alignItems: "flex-start", gap: 12,
      }}>
        <div style={{ marginTop: 1 }}>
          {type === "error" ? <XIcon size={16} color={c.text} /> : <LockIcon size={16} color={c.text} />}
        </div>
        <p style={{ fontSize: 13, color: c.text, lineHeight: 1.5, flex: 1 }}>{msg}</p>
      </div>
    </div>
  );
}

// ─── Log Button ───────────────────────────────────────────────────────────────
function LogBtn({ ml, selected, onClick, disabled }) {
  return (
    <button onClick={() => !disabled && onClick(ml)} className="press" style={{
      background: disabled ? M3.outlineVariant : selected ? M3.primary : M3.surfaceVariant,
      color: disabled ? "#444" : selected ? M3.onPrimary : M3.onSurface,
      border: `1px solid ${disabled ? M3.outlineVariant : selected ? M3.primary : M3.outline}`,
      borderRadius: 12, padding: "10px 14px",
      fontSize: 12, fontWeight: 600, fontFamily: "'DM Mono',monospace",
      cursor: disabled ? "not-allowed" : "pointer", transition: "all 0.15s",
      position: "relative",
    }}>
      +{ml}ml
      {disabled && (
        <span style={{ position: "absolute", top: -6, right: -6, background: M3.surface, borderRadius: "50%", padding: 1 }}>
          <LockIcon size={10} color="#555" />
        </span>
      )}
    </button>
  );
}

// ─── Setup ────────────────────────────────────────────────────────────────────
function Setup({ onSave }) {
  const [wake, setWake] = useState("07:00");
  const [sleep, setSleep] = useState("23:00");
  const [goal, setGoal] = useState(2500);
  const [cup, setCup] = useState(250);
  const schedule = buildSchedule(wake, sleep, goal, cup);

  return (
    <div style={{ minHeight: "100vh", background: M3.bg, display: "flex", alignItems: "center", justifyContent: "center", padding: 24 }}>
      <div style={{ width: "100%", maxWidth: 420, animation: "scaleIn 0.4s ease both" }}>
        <div style={{ textAlign: "center", marginBottom: 36 }}>
          <div style={{
            display: "inline-flex", alignItems: "center", justifyContent: "center",
            width: 64, height: 64, background: M3.surface, border: `1px solid ${M3.outline}`,
            borderRadius: 20, marginBottom: 18,
          }}>
            <DropIcon size={32} filled color={M3.primary} />
          </div>
          <h1 style={{ fontSize: 26, fontWeight: 600, color: M3.onBg, letterSpacing: -0.5 }}>Hydrate</h1>
          <p style={{ fontSize: 13, color: M3.onSurfaceVariant, marginTop: 6 }}>Set your goal. We'll distribute it honestly.</p>
        </div>

        <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
          <Card>
            <Label>Daily Schedule</Label>
            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 }}>
              <Field label="Wake Up" type="time" value={wake} onChange={e => setWake(e.target.value)} />
              <Field label="Sleep" type="time" value={sleep} onChange={e => setSleep(e.target.value)} />
            </div>
          </Card>

          <Card>
            <Field label="Daily Water Goal" type="number" value={goal} onChange={e => setGoal(Number(e.target.value))} suffix="ml" min={500} max={6000} />
            <div style={{ display: "flex", gap: 8, marginTop: 12, flexWrap: "wrap" }}>
              {[1500, 2000, 2500, 3000, 3500].map(v => (
                <Chip key={v} label={`${v/1000}L`} selected={goal === v} onClick={() => setGoal(v)} />
              ))}
            </div>
          </Card>

          <Card>
            <Label>Cup Size</Label>
            <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
              {[150, 200, 250, 300, 350].map(c => (
                <Chip key={c} label={`${c}ml`} selected={cup === c} onClick={() => setCup(c)} />
              ))}
            </div>
          </Card>

          {/* Accountability notice */}
          <div style={{
            background: "rgba(255,255,255,0.03)", border: `1px solid ${M3.outline}`,
            borderRadius: 16, padding: "14px 18px",
          }}>
            <p style={{ fontSize: 11, color: M3.onSurfaceVariant, lineHeight: 1.6 }}>
              🛡️ <strong style={{ color: M3.onSurface }}>Accountability mode on.</strong> Logging is only allowed near scheduled slots, with a {RATE_LIMIT_MIN}-min cooldown and a {CONFIRM_SEC}s confirmation — so your data stays real.
            </p>
          </div>

          <div style={{ background: M3.surfaceVariant, border: `1px solid ${M3.outline}`, borderRadius: 16, padding: "14px 18px", display: "flex", justifyContent: "space-between" }}>
            <span style={{ fontSize: 12, color: M3.onSurfaceVariant }}>Reminders scheduled</span>
            <span style={{ fontSize: 20, fontWeight: 600, fontFamily: "'DM Mono',monospace", color: M3.onBg }}>{schedule.length}</span>
          </div>

          <FilledBtn onClick={() => onSave({ wakeTime: wake, sleepTime: sleep, totalMl: goal, cupSize: cup })} style={{ width: "100%", padding: 16, marginTop: 4 }}>
            Start Tracking
          </FilledBtn>
        </div>
      </div>
    </div>
  );
}

// ─── Dashboard ────────────────────────────────────────────────────────────────
function Dashboard({ settings, logs, onLog, onTabChange }) {
  const schedule = buildSchedule(settings.wakeTime, settings.sleepTime, settings.totalMl, settings.cupSize);
  const todayLogs = logs[today()] || [];
  const consumed = todayLogs.reduce((s, l) => s + l.amount, 0);
  const percent = Math.round((consumed / settings.totalMl) * 100);

  // Next slot
  const nm = nowMin();
  const nextSlot = schedule.find(s => toMin(s.time24) > nm);

  // Is within any slot window?
  const nearSlot = schedule.find(s => Math.abs(toMin(s.time24) - nm) <= SLOT_WINDOW_MIN);
  const isLocked = !nearSlot;

  // Rate limit check
  const lastLog = todayLogs.length > 0 ? todayLogs[todayLogs.length - 1] : null;
  const minsSinceLast = lastLog ? (nowTs() - lastLog.ts) / 60000 : Infinity;
  const isRateLimited = minsSinceLast < RATE_LIMIT_MIN;

  const logBlocked = isLocked || isRateLimited;

  const [confirm, setConfirm] = useState(null); // { amount }
  const [toast, setToast] = useState(null);     // { msg, type }
  const [shake, setShake] = useState(false);
  const [bounce, setBounce] = useState(false);

  const showToast = (msg, type = "error") => {
    setToast({ msg, type });
    setShake(true);
    setTimeout(() => setShake(false), 500);
  };

  const handleLogAttempt = (ml) => {
    const result = validateLog(ml, logs, schedule, settings);
    if (!result.ok) {
      showToast(result.msg, result.reason === "daily_cap" ? "info" : "warn");
      return;
    }
    setConfirm({ amount: ml });
  };

  const handleConfirmed = () => {
    onLog(confirm.amount);
    setConfirm(null);
    setBounce(true);
    setTimeout(() => setBounce(false), 500);
  };

  const statusText = () => {
    if (percent === 0) return "Start your hydration journey 💧";
    if (percent < 25) return "Early stages — keep going!";
    if (percent < 50) return "Making progress";
    if (percent < 75) return "Great progress 🔥";
    if (percent < 100) return "Almost at your goal!";
    return "Goal crushed! 🎉";
  };

  // Rate limit countdown
  const waitMin = isRateLimited ? Math.ceil(RATE_LIMIT_MIN - minsSinceLast) : 0;

  return (
    <div style={{ minHeight: "100vh", background: M3.bg, padding: "28px 20px 110px", maxWidth: 480, margin: "0 auto" }}>
      {confirm && (
        <ConfirmModal
          amount={confirm.amount}
          onConfirm={handleConfirmed}
          onCancel={() => setConfirm(null)}
        />
      )}
      {toast && <Toast msg={toast.msg} type={toast.type} onDone={() => setToast(null)} />}

      {/* Header */}
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 32 }}>
        <div>
          <p style={{ fontSize: 11, color: M3.onSurfaceVariant, letterSpacing: 0.5, marginBottom: 3 }}>
            {new Date().toLocaleDateString("en-IN", { weekday: "long", day: "numeric", month: "long" })}
          </p>
          <h1 style={{ fontSize: 24, fontWeight: 600, color: M3.onBg, letterSpacing: -0.5 }}>Hydrate</h1>
        </div>
        <button onClick={() => onTabChange("settings")} style={{
          width: 42, height: 42, background: M3.surface, border: `1px solid ${M3.outline}`,
          borderRadius: 14, display: "flex", alignItems: "center", justifyContent: "center", cursor: "pointer",
        }}>
          <GearIcon color={M3.onSurfaceVariant} />
        </button>
      </div>

      {/* Ring */}
      <div style={{ display: "flex", justifyContent: "center", marginBottom: 12 }}>
        <Ring percent={percent} size={210} stroke={11}>
          <div style={{
            textAlign: "center",
            animation: bounce ? "pop 0.4s cubic-bezier(0.34,1.56,0.64,1)" : shake ? "shake 0.4s ease" : "none",
          }}>
            <div style={{ fontSize: 44, fontWeight: 600, fontFamily: "'DM Mono',monospace", color: M3.onBg, lineHeight: 1 }}>
              {(consumed / 1000).toFixed(1)}
            </div>
            <div style={{ fontSize: 12, color: M3.onSurfaceVariant, marginTop: 5, fontFamily: "'DM Mono',monospace" }}>
              / {(settings.totalMl / 1000).toFixed(1)} L
            </div>
            <div style={{
              marginTop: 10, display: "inline-block",
              background: percent >= 100 ? "rgba(90,219,127,0.12)" : M3.surfaceVariant,
              border: `1px solid ${percent >= 100 ? "rgba(90,219,127,0.3)" : M3.outline}`,
              borderRadius: 99, padding: "3px 12px",
              fontSize: 11, fontWeight: 600,
              color: percent >= 100 ? M3.success : M3.onSurfaceVariant,
            }}>
              {percent}%
            </div>
          </div>
        </Ring>
      </div>

      <p style={{ textAlign: "center", fontSize: 13, color: M3.onSurfaceVariant, marginBottom: 24 }}>{statusText()}</p>

      {/* Status Banner */}
      {logBlocked && (
        <div style={{
          background: isRateLimited ? "rgba(245,166,35,0.08)" : "rgba(255,255,255,0.04)",
          border: `1px solid ${isRateLimited ? "rgba(245,166,35,0.25)" : M3.outline}`,
          borderRadius: 16, padding: "14px 18px",
          display: "flex", alignItems: "center", gap: 12, marginBottom: 14,
          animation: "fadeUp 0.3s ease both",
        }}>
          <LockIcon size={16} color={isRateLimited ? M3.warn : M3.onSurfaceVariant} />
          <div>
            <p style={{ fontSize: 12, fontWeight: 600, color: isRateLimited ? M3.warn : M3.onSurface }}>
              {isRateLimited ? `Cooldown — ${waitMin} min left` : "Outside log window"}
            </p>
            <p style={{ fontSize: 11, color: M3.onSurfaceVariant, marginTop: 2 }}>
              {isRateLimited
                ? `You logged ${Math.floor(minsSinceLast)} min ago. Cooldown is ${RATE_LIMIT_MIN} min.`
                : nextSlot ? `Next log window opens at ${nextSlot.label}` : "No more slots today"
              }
            </p>
          </div>
        </div>
      )}

      {/* Quick Log */}
      <Card style={{ marginBottom: 14 }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 14 }}>
          <Label>Quick Log</Label>
          {nearSlot && !isRateLimited && (
            <span style={{ fontSize: 10, color: M3.success, fontWeight: 600, letterSpacing: 0.5, background: "rgba(90,219,127,0.1)", padding: "3px 10px", borderRadius: 99 }}>
              WINDOW OPEN
            </span>
          )}
        </div>
        <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
          {[150, 200, 250, 300, 500].map(ml => (
            <LogBtn key={ml} ml={ml} selected={ml === settings.cupSize} disabled={logBlocked} onClick={handleLogAttempt} />
          ))}
        </div>
        {nearSlot && !isRateLimited && (
          <p style={{ fontSize: 11, color: M3.onSurfaceVariant, marginTop: 12, lineHeight: 1.5 }}>
            Near <strong style={{ color: M3.onSurface }}>{nearSlot.label}</strong> slot · {CONFIRM_SEC}s confirm required · {RATE_LIMIT_MIN}min cooldown after
          </p>
        )}
      </Card>

      {/* Next Slot */}
      {nextSlot && (
        <Card style={{ marginBottom: 14, display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          <div>
            <Label>Next Reminder</Label>
            <p style={{ fontSize: 22, fontWeight: 600, fontFamily: "'DM Mono',monospace", color: M3.onBg, lineHeight: 1 }}>{nextSlot.label}</p>
            <p style={{ fontSize: 11, color: M3.onSurfaceVariant, marginTop: 6 }}>Log window: ±{SLOT_WINDOW_MIN} min</p>
          </div>
          <div style={{ textAlign: "center" }}>
            <DropIcon size={28} filled color={M3.onSurfaceVariant} />
            <p style={{ fontSize: 11, color: M3.onSurfaceVariant, marginTop: 6, fontFamily: "'DM Mono',monospace" }}>{nextSlot.amount}ml</p>
          </div>
        </Card>
      )}

      {/* Today's Log */}
      <Card>
        <Label>Today's Log</Label>
        {todayLogs.length === 0 ? (
          <div style={{ textAlign: "center", padding: "20px 0" }}>
            <DropIcon size={32} color={M3.outline} />
            <p style={{ fontSize: 13, color: M3.onSurfaceVariant, marginTop: 10 }}>No logs yet — drink up!</p>
          </div>
        ) : (
          <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
            {[...todayLogs].reverse().slice(0, 6).map((l, i) => (
              <div key={i} style={{
                display: "flex", justifyContent: "space-between", alignItems: "center",
                background: M3.surfaceVariant, borderRadius: 12, padding: "10px 14px",
                animation: `fadeUp 0.25s ease ${i * 0.04}s both`,
              }}>
                <div>
                  <span style={{ fontSize: 13, color: M3.onSurfaceVariant, fontFamily: "'DM Mono',monospace" }}>{l.time}</span>
                  {l.slot && <span style={{ fontSize: 10, color: M3.onSurfaceVariant, marginLeft: 8 }}>@ {l.slot}</span>}
                </div>
                <span style={{ fontSize: 13, fontWeight: 600, color: M3.onBg, fontFamily: "'DM Mono',monospace" }}>+{l.amount} ml</span>
              </div>
            ))}
            {todayLogs.length > 6 && (
              <p style={{ fontSize: 11, color: M3.onSurfaceVariant, textAlign: "center", paddingTop: 4 }}>+{todayLogs.length - 6} more</p>
            )}
          </div>
        )}
      </Card>
    </div>
  );
}

// ─── Schedule Tab ─────────────────────────────────────────────────────────────
function ScheduleTab({ settings, logs }) {
  const schedule = buildSchedule(settings.wakeTime, settings.sleepTime, settings.totalMl, settings.cupSize);
  const todayLogs = logs[today()] || [];
  const consumed = todayLogs.reduce((s, l) => s + l.amount, 0);
  const nm = nowMin();
  const curIdx = (() => {
    let idx = -1;
    schedule.forEach((s, i) => { if (nm >= toMin(s.time24)) idx = i; });
    return idx;
  })();
  const intervalMin = schedule.length > 1
    ? Math.round((toMin(settings.sleepTime) - toMin(settings.wakeTime)) / schedule.length)
    : 60;

  return (
    <div style={{ minHeight: "100vh", background: M3.bg, padding: "28px 20px 110px", maxWidth: 480, margin: "0 auto" }}>
      <h2 style={{ fontSize: 22, fontWeight: 600, color: M3.onBg, marginBottom: 6 }}>Schedule</h2>
      <p style={{ fontSize: 12, color: M3.onSurfaceVariant, marginBottom: 24 }}>
        {schedule.length} reminders · every ~{intervalMin} min · ±{SLOT_WINDOW_MIN}min log window each
      </p>

      {/* Summary */}
      <Card style={{ marginBottom: 22, padding: 0, overflow: "hidden" }}>
        {[
          { label: "Goal", val: `${(settings.totalMl / 1000).toFixed(1)}L` },
          { label: "Cups", val: schedule.length },
          { label: "Interval", val: `~${intervalMin}m` },
          { label: "Cooldown", val: `${RATE_LIMIT_MIN}m` },
        ].map((s, i) => (
          <div key={i} style={{
            display: "inline-block", width: "25%", textAlign: "center",
            padding: "16px 4px",
            borderRight: i < 3 ? `1px solid ${M3.outline}` : "none",
          }}>
            <p style={{ fontSize: 15, fontWeight: 600, fontFamily: "'DM Mono',monospace", color: M3.onBg }}>{s.val}</p>
            <p style={{ fontSize: 10, color: M3.onSurfaceVariant, marginTop: 3, letterSpacing: 0.4 }}>{s.label}</p>
          </div>
        ))}
      </Card>

      {/* Timeline */}
      <div style={{ position: "relative" }}>
        <div style={{ position: "absolute", left: 19, top: 20, bottom: 20, width: 2, background: M3.outlineVariant, borderRadius: 1 }} />
        {schedule.map((slot, i) => {
          const cumulative = (i + 1) * slot.amount;
          const done = consumed >= cumulative;
          const curr = i === curIdx;
          const inWindow = Math.abs(toMin(slot.time24) - nm) <= SLOT_WINDOW_MIN;

          return (
            <div key={i} style={{ display: "flex", alignItems: "flex-start", gap: 16, marginBottom: 10, animation: `fadeUp 0.3s ease ${Math.min(i * 0.04, 0.4)}s both` }}>
              <div style={{
                width: 40, height: 40, flexShrink: 0, borderRadius: "50%",
                background: done ? M3.primary : curr ? M3.surface : M3.surfaceVariant,
                border: `2px solid ${done ? M3.primary : curr ? "#555" : M3.outline}`,
                display: "flex", alignItems: "center", justifyContent: "center",
                position: "relative", zIndex: 1,
                boxShadow: curr ? "0 0 0 4px rgba(255,255,255,0.06)" : "none",
                transition: "all 0.3s",
              }}>
                {done ? <CheckIcon color={M3.onPrimary} /> : <DropIcon size={16} color={curr ? M3.onBg : M3.onSurfaceVariant} />}
              </div>

              <div style={{
                flex: 1,
                background: curr ? M3.surface : "transparent",
                border: curr ? `1px solid ${M3.outline}` : "1px solid transparent",
                borderRadius: 16, padding: curr ? "14px 16px" : "10px 4px",
                transition: "all 0.3s",
              }}>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                  <span style={{
                    fontSize: 15, fontWeight: curr ? 600 : 400,
                    fontFamily: "'DM Mono',monospace",
                    color: done ? M3.onSurfaceVariant : M3.onBg,
                    textDecoration: done ? "line-through" : "none",
                  }}>{slot.label}</span>
                  <div style={{ display: "flex", gap: 6, alignItems: "center" }}>
                    {inWindow && !done && (
                      <span style={{ fontSize: 9, color: M3.success, background: "rgba(90,219,127,0.1)", padding: "2px 8px", borderRadius: 99, fontWeight: 600 }}>OPEN</span>
                    )}
                    <span style={{ fontSize: 11, fontFamily: "'DM Mono',monospace", color: M3.onSurfaceVariant, background: M3.surfaceVariant, padding: "3px 10px", borderRadius: 99 }}>
                      {slot.amount}ml
                    </span>
                  </div>
                </div>
                {curr && (
                  <p style={{ fontSize: 11, color: M3.onSurfaceVariant, marginTop: 5 }}>
                    Drink now · <span style={{ color: M3.onBg, fontWeight: 500 }}>{(cumulative / 1000).toFixed(2)}L</span> cumulative
                  </p>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

// ─── Settings Tab ─────────────────────────────────────────────────────────────
function SettingsTab({ settings, onUpdate, onReset, onErase }) {
  const [local, setLocal] = useState({ ...settings });
  const preview = buildSchedule(local.wakeTime, local.sleepTime, local.totalMl, local.cupSize);

  return (
    <div style={{ minHeight: "100vh", background: M3.bg, padding: "28px 20px 110px", maxWidth: 480, margin: "0 auto" }}>
      <h2 style={{ fontSize: 22, fontWeight: 600, color: M3.onBg, marginBottom: 24 }}>Settings</h2>
      <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
        <Card>
          <Label>Schedule</Label>
          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 }}>
            <Field label="Wake Up" type="time" value={local.wakeTime} onChange={e => setLocal({ ...local, wakeTime: e.target.value })} />
            <Field label="Sleep" type="time" value={local.sleepTime} onChange={e => setLocal({ ...local, sleepTime: e.target.value })} />
          </div>
        </Card>
        <Card>
          <Field label="Daily Goal" type="number" value={local.totalMl} onChange={e => setLocal({ ...local, totalMl: Number(e.target.value) })} suffix="ml" min={500} max={6000} />
          <div style={{ display: "flex", gap: 8, marginTop: 12, flexWrap: "wrap" }}>
            {[1500, 2000, 2500, 3000, 3500].map(v => (
              <Chip key={v} label={`${v/1000}L`} selected={local.totalMl === v} onClick={() => setLocal({ ...local, totalMl: v })} />
            ))}
          </div>
        </Card>
        <Card>
          <Label>Cup Size</Label>
          <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
            {[150, 200, 250, 300, 350].map(c => (
              <Chip key={c} label={`${c}ml`} selected={local.cupSize === c} onClick={() => setLocal({ ...local, cupSize: c })} />
            ))}
          </div>
        </Card>

        {/* Accountability rules */}
        <Card style={{ background: "rgba(255,255,255,0.02)" }}>
          <Label>Accountability Rules</Label>
          {[
            { icon: "🔒", label: "Schedule Lock", desc: `Log only within ±${SLOT_WINDOW_MIN} min of a slot` },
            { icon: "⏳", label: "Rate Limit", desc: `${RATE_LIMIT_MIN}-min cooldown between logs` },
            { icon: "✅", label: "Confirm Delay", desc: `${CONFIRM_SEC}-sec countdown before saving` },
            { icon: "🚫", label: "Daily Cap", desc: "Hard cap at goal + 20%" },
          ].map((r, i) => (
            <div key={i} style={{ display: "flex", gap: 12, alignItems: "flex-start", marginBottom: i < 3 ? 14 : 0 }}>
              <span style={{ fontSize: 16 }}>{r.icon}</span>
              <div>
                <p style={{ fontSize: 12, fontWeight: 600, color: M3.onSurface }}>{r.label}</p>
                <p style={{ fontSize: 11, color: M3.onSurfaceVariant, marginTop: 2 }}>{r.desc}</p>
              </div>
            </div>
          ))}
        </Card>

        <div style={{ background: M3.surfaceVariant, border: `1px solid ${M3.outline}`, borderRadius: 16, padding: "14px 18px", display: "flex", justifyContent: "space-between" }}>
          <span style={{ fontSize: 12, color: M3.onSurfaceVariant }}>New schedule</span>
          <span style={{ fontSize: 18, fontWeight: 600, fontFamily: "'DM Mono',monospace", color: M3.onBg }}>{preview.length} reminders</span>
        </div>

        <FilledBtn onClick={() => onUpdate(local)} style={{ width: "100%" }}>Save Changes</FilledBtn>
        <OutlinedBtn onClick={onReset} style={{ width: "100%" }}>Reset Today's Log</OutlinedBtn>
        <OutlinedBtn onClick={onErase} style={{ width: "100%", color: M3.error, borderColor: M3.error }}>Erase All Data</OutlinedBtn>
      </div>
    </div>
  );
}

// ─── Bottom Nav ───────────────────────────────────────────────────────────────
function Nav({ active, onChange }) {
  const tabs = [
    { id: "dashboard", label: "Today", icon: (a) => <DropIcon size={22} filled={a} color={a ? M3.onBg : M3.onSurfaceVariant} /> },
    { id: "schedule",  label: "Schedule", icon: (a) => <CalIcon size={22} color={a ? M3.onBg : M3.onSurfaceVariant} /> },
    { id: "settings",  label: "Settings", icon: (a) => <GearIcon size={22} color={a ? M3.onBg : M3.onSurfaceVariant} /> },
  ];
  return (
    <div style={{
      position: "fixed", bottom: 0, left: 0, right: 0,
      background: M3.surface, borderTop: `1px solid ${M3.outline}`,
      display: "flex", padding: "10px 0 18px", zIndex: 100,
    }}>
      {tabs.map(tab => {
        const isActive = active === tab.id;
        return (
          <button key={tab.id} onClick={() => onChange(tab.id)} style={{
            flex: 1, display: "flex", flexDirection: "column", alignItems: "center", gap: 5,
            background: "none", border: "none", cursor: "pointer", padding: "8px 0", position: "relative",
          }}>
            {isActive && (
              <div style={{ position: "absolute", top: 6, width: 56, height: 34, background: M3.surfaceVariant, borderRadius: 99 }} />
            )}
            <div style={{ position: "relative", zIndex: 1 }}>{tab.icon(isActive)}</div>
            <span style={{
              fontSize: 10, fontWeight: isActive ? 600 : 400, letterSpacing: 0.5,
              color: isActive ? M3.onBg : M3.onSurfaceVariant,
              position: "relative", zIndex: 1, textTransform: "uppercase",
            }}>{tab.label}</span>
          </button>
        );
      })}
    </div>
  );
}

// ─── Root ─────────────────────────────────────────────────────────────────────
export default function App() {
  const [settings, setSettings] = useState(() => {
    try { return JSON.parse(localStorage.getItem("h_settings")); } catch { return null; }
  });
  const [logs, setLogs] = useState(() => {
    try { return JSON.parse(localStorage.getItem("h_logs")) || {}; } catch { return {}; }
  });
  const [tab, setTab] = useState("dashboard");

  useEffect(() => {
    const style = document.createElement("style");
    style.textContent = css();
    document.head.appendChild(style);
    return () => document.head.removeChild(style);
  }, []);

  const saveSettings = (s) => {
    localStorage.setItem("h_settings", JSON.stringify(s));
    setSettings(s);
  };

  const logWater = (amount) => {
    const d = today();
    const schedule = buildSchedule(settings.wakeTime, settings.sleepTime, settings.totalMl, settings.cupSize);
    const nm = nowMin();
    const nearSlot = schedule.find(s => Math.abs(toMin(s.time24) - nm) <= SLOT_WINDOW_MIN);
    const entry = { time: nowTime(), amount, ts: nowTs(), slot: nearSlot?.label };
    const updated = { ...logs, [d]: [...(logs[d] || []), entry] };
    setLogs(updated);
    localStorage.setItem("h_logs", JSON.stringify(updated));
  };

  const resetToday = () => {
    const d = today();
    const updated = { ...logs, [d]: [] };
    setLogs(updated);
    localStorage.setItem("h_logs", JSON.stringify(updated));
  };

  const eraseAll = () => {
    setSettings(null); setLogs({});
    localStorage.removeItem("h_settings");
    localStorage.removeItem("h_logs");
  };

  if (!settings) return <Setup onSave={saveSettings} />;

  return (
    <div style={{ background: M3.bg, minHeight: "100vh" }}>
      {tab === "dashboard" && <Dashboard settings={settings} logs={logs} onLog={logWater} onTabChange={setTab} />}
      {tab === "schedule"  && <ScheduleTab settings={settings} logs={logs} />}
      {tab === "settings"  && <SettingsTab settings={settings} onUpdate={s => { saveSettings(s); setTab("dashboard"); }} onReset={resetToday} onErase={eraseAll} />}
      <Nav active={tab} onChange={setTab} />
    </div>
  );
}
