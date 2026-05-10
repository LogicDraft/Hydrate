// ─── Types ────────────────────────────────────────────────────────────────────
export interface UserSettings {
  wakeTime: string;        // "06:00"
  sleepTime: string;       // "22:00"
  dailyGoal: number;       // ml
  cupSize: number;         // ml (default cup)
  notificationsEnabled: boolean;
  setupComplete: boolean;
}

export interface WaterLog {
  id: string;
  amount: number;          // ml
  timestamp: number;       // Date.now()
  date: string;            // "YYYY-MM-DD"
}

export interface DayData {
  date: string;
  total: number;
  entries: WaterLog[];
}

// ─── Keys ─────────────────────────────────────────────────────────────────────
const SETTINGS_KEY = 'hydrate_settings';
const LOGS_KEY = 'hydrate_logs';

// ─── Defaults ─────────────────────────────────────────────────────────────────
export const DEFAULT_SETTINGS: UserSettings = {
  wakeTime: '07:00',
  sleepTime: '22:00',
  dailyGoal: 2500,
  cupSize: 250,
  notificationsEnabled: false,
  setupComplete: false,
};

// ─── Settings helpers ─────────────────────────────────────────────────────────
export function getSettings(): UserSettings {
  try {
    const raw = localStorage.getItem(SETTINGS_KEY);
    return raw ? { ...DEFAULT_SETTINGS, ...JSON.parse(raw) } : DEFAULT_SETTINGS;
  } catch {
    return DEFAULT_SETTINGS;
  }
}

export function saveSettings(s: Partial<UserSettings>): UserSettings {
  const current = getSettings();
  const updated = { ...current, ...s };
  localStorage.setItem(SETTINGS_KEY, JSON.stringify(updated));
  return updated;
}

// ─── Log helpers ──────────────────────────────────────────────────────────────
export function getLogs(): WaterLog[] {
  try {
    const raw = localStorage.getItem(LOGS_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
}

function saveLogs(logs: WaterLog[]): void {
  localStorage.setItem(LOGS_KEY, JSON.stringify(logs));
}

export function addWaterLog(amount: number): WaterLog {
  const logs = getLogs();
  const now = Date.now();
  const entry: WaterLog = {
    id: `${now}-${Math.random().toString(36).slice(2)}`,
    amount,
    timestamp: now,
    date: toDateStr(new Date()),
  };
  saveLogs([...logs, entry]);
  return entry;
}

export function resetToday(): void {
  const logs = getLogs();
  const today = toDateStr(new Date());
  saveLogs(logs.filter((l) => l.date !== today));
}

// ─── Aggregation ──────────────────────────────────────────────────────────────
export function getTodayTotal(): number {
  const today = toDateStr(new Date());
  return getLogs()
    .filter((l) => l.date === today)
    .reduce((sum, l) => sum + l.amount, 0);
}

export function getLastNDays(n: number): DayData[] {
  const logs = getLogs();
  const result: DayData[] = [];

  for (let i = n - 1; i >= 0; i--) {
    const d = new Date();
    d.setDate(d.getDate() - i);
    const dateStr = toDateStr(d);
    const entries = logs.filter((l) => l.date === dateStr);
    result.push({ date: dateStr, total: entries.reduce((s, e) => s + e.amount, 0), entries });
  }
  return result;
}

export function getLastLogTimestamp(): number | null {
  const today = toDateStr(new Date());
  const todayLogs = getLogs().filter((l) => l.date === today);
  if (todayLogs.length === 0) return null;
  return Math.max(...todayLogs.map((l) => l.timestamp));
}

// ─── Streak ────────────────────────────────────────────────────────────────────
export function calculateStreak(goal: number): number {
  const logs = getLogs();
  const byDate: Record<string, number> = {};
  logs.forEach((l) => {
    byDate[l.date] = (byDate[l.date] || 0) + l.amount;
  });

  let streak = 0;
  const today = new Date();

  for (let i = 0; i < 365; i++) {
    const d = new Date(today);
    d.setDate(d.getDate() - i);
    const ds = toDateStr(d);
    if ((byDate[ds] || 0) >= goal) {
      streak++;
    } else {
      break;
    }
  }
  return streak;
}

export function getBestDay(days: DayData[]): DayData | null {
  if (days.length === 0) return null;
  return days.reduce((best, d) => (d.total > best.total ? d : best), days[0]);
}

// ─── Reminder logic ───────────────────────────────────────────────────────────
export function shouldSendReminder(settings: UserSettings): boolean {
  if (!settings.notificationsEnabled) return false;

  const now = new Date();
  const [wakeH, wakeM] = settings.wakeTime.split(':').map(Number);
  const [sleepH, sleepM] = settings.sleepTime.split(':').map(Number);

  const wakeMinutes = wakeH * 60 + wakeM;
  const sleepMinutes = sleepH * 60 + sleepM;
  const nowMinutes = now.getHours() * 60 + now.getMinutes();

  if (nowMinutes < wakeMinutes || nowMinutes >= sleepMinutes) return false;

  // Skip if logged in last 20 minutes
  const lastLog = getLastLogTimestamp();
  if (lastLog && Date.now() - lastLog < 20 * 60 * 1000) return false;

  return true;
}

export function getNextReminderTime(settings: UserSettings): string {
  const lastLog = getLastLogTimestamp();
  if (lastLog) {
    const next = new Date(lastLog + 60 * 60 * 1000);
    const now = new Date();
    if (next > now) {
      return next.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    }
  }

  // Next full hour
  const next = new Date();
  next.setMinutes(0, 0, 0);
  next.setHours(next.getHours() + 1);
  return next.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

// ─── Utils ────────────────────────────────────────────────────────────────────
export function toDateStr(d: Date): string {
  return d.toISOString().split('T')[0];
}

export function formatMl(ml: number): string {
  return ml >= 1000 ? `${(ml / 1000).toFixed(1)}L` : `${ml}ml`;
}

export function formatDate(dateStr: string): string {
  const d = new Date(dateStr + 'T00:00:00');
  return d.toLocaleDateString([], { weekday: 'short', month: 'short', day: 'numeric' });
}

export function getMotivation(percent: number): { message: string; emoji: string } {
  if (percent === 0) return { message: "Start your hydration journey", emoji: "💧" };
  if (percent < 25) return { message: "Great start! Keep sipping", emoji: "🌱" };
  if (percent < 50) return { message: "You're building momentum!", emoji: "⚡" };
  if (percent < 75) return { message: "Halfway there — stay strong!", emoji: "🔥" };
  if (percent < 100) return { message: "Almost at your goal!", emoji: "🏆" };
  return { message: "Goal smashed! Outstanding!", emoji: "🎉" };
}
