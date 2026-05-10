import { useEffect, useRef, useState } from 'react';
import {
  getSettings,
  getTodayTotal,
  addWaterLog,
  getNextReminderTime,
  formatMl,
  getMotivation,
} from '../store';
import { useAnimatedNumber, useRipple, useNotifications } from '../hooks';

// ─── Progress Ring ─────────────────────────────────────────────────────────────
interface RingProps {
  percent: number;
  size?: number;
  stroke?: number;
}

function ProgressRing({ percent, size = 240, stroke = 14 }: RingProps) {
  const r = (size - stroke) / 2;
  const circ = 2 * Math.PI * r;
  const offset = circ - (Math.min(percent, 100) / 100) * circ;
  const [animated, setAnimated] = useState(false);

  useEffect(() => {
    const t = setTimeout(() => setAnimated(true), 100);
    return () => clearTimeout(t);
  }, []);

  return (
    <div className="relative" style={{ width: size, height: size }}>
      <svg width={size} height={size} style={{ transform: 'rotate(-90deg)' }}>
        {/* Background track */}
        <circle
          cx={size / 2}
          cy={size / 2}
          r={r}
          fill="none"
          stroke="rgba(255,255,255,0.05)"
          strokeWidth={stroke}
        />
        {/* Glow filter */}
        <defs>
          <filter id="glow">
            <feGaussianBlur stdDeviation="3" result="coloredBlur" />
            <feMerge>
              <feMergeNode in="coloredBlur" />
              <feMergeNode in="SourceGraphic" />
            </feMerge>
          </filter>
          <linearGradient id="ringGrad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="#00B4D8" />
            <stop offset="100%" stopColor="#90E0EF" />
          </linearGradient>
        </defs>
        {/* Progress arc */}
        <circle
          cx={size / 2}
          cy={size / 2}
          r={r}
          fill="none"
          stroke="url(#ringGrad)"
          strokeWidth={stroke}
          strokeLinecap="round"
          strokeDasharray={circ}
          strokeDashoffset={animated ? offset : circ}
          style={{ transition: 'stroke-dashoffset 1.5s cubic-bezier(0.4,0,0.2,1)' }}
          filter="url(#glow)"
        />
        {/* Dot at tip */}
        {percent > 2 && (
          <circle
            cx={size / 2 + r * Math.cos(((Math.min(percent, 100) / 100) * 360 - 90) * (Math.PI / 180))}
            cy={size / 2 + r * Math.sin(((Math.min(percent, 100) / 100) * 360 - 90) * (Math.PI / 180))}
            r={stroke / 2 - 1}
            fill="#00B4D8"
            filter="url(#glow)"
            style={{ opacity: animated ? 1 : 0, transition: 'opacity 0.5s 1.2s' }}
          />
        )}
      </svg>
    </div>
  );
}

// ─── Quick Add Button ──────────────────────────────────────────────────────────
interface QuickAddProps {
  amount: number;
  label: string;
  emoji: string;
  onAdd: (amount: number) => void;
}

function QuickAddBtn({ amount, label, emoji, onAdd }: QuickAddProps) {
  const { ripples, createRipple } = useRipple();

  return (
    <button
      className="btn-water relative overflow-hidden rounded-2xl flex-1"
      onClick={(e) => { createRipple(e); onAdd(amount); }}
    >
      {ripples.map((r) => (
        <span
          key={r.key}
          className="ripple"
          style={{ left: r.x - 20, top: r.y - 20, width: 40, height: 40 }}
        />
      ))}
      <span className="text-2xl">{emoji}</span>
      <span className="text-cyan-300 font-bold text-sm">{label}</span>
      <span className="text-white/40 text-xs">{amount}ml</span>
    </button>
  );
}

// ─── Dashboard Page ────────────────────────────────────────────────────────────
export default function DashboardPage() {
  const settings = getSettings();
  const [total, setTotal] = useState(getTodayTotal);
  const [showCustom, setShowCustom] = useState(false);
  const [customVal, setCustomVal] = useState('');
  const [lastAdded, setLastAdded] = useState<number | null>(null);
  const [showToast, setShowToast] = useState(false);
  const customRef = useRef<HTMLInputElement>(null);
  const { sendNotification } = useNotifications();

  const percent = Math.round((total / settings.dailyGoal) * 100);
  const animatedTotal = useAnimatedNumber(total, 800);
  const animatedPercent = useAnimatedNumber(percent, 1000);
  const motivation = getMotivation(percent);
  const nextReminder = getNextReminderTime(settings);
  const remaining = Math.max(0, settings.dailyGoal - total);

  // Hourly reminder check
  useEffect(() => {
    if (!settings.notificationsEnabled) return;
    const check = () => {
      const { shouldSendReminder } = require('../store');
      if (shouldSendReminder(settings)) {
        sendNotification('💧 Hydrate Reminder', `You've had ${formatMl(getTodayTotal())} today. Time for a sip!`);
      }
    };
    check();
    const interval = setInterval(check, 60 * 60 * 1000);
    return () => clearInterval(interval);
  }, [settings.notificationsEnabled]);

  function handleAdd(amount: number) {
    addWaterLog(amount);
    const newTotal = getTodayTotal();
    setTotal(newTotal);
    setLastAdded(amount);
    setShowToast(true);
    setTimeout(() => setShowToast(false), 2500);
  }

  function handleCustomAdd() {
    const v = parseInt(customVal, 10);
    if (!isNaN(v) && v > 0 && v <= 5000) {
      handleAdd(v);
      setCustomVal('');
      setShowCustom(false);
    }
  }

  return (
    <div className="flex-1 flex flex-col px-4 py-6 overflow-y-auto no-scrollbar animate-fade-in">
      {/* Background orbs */}
      <div className="orb w-80 h-80 bg-cyan-500/8 top-0 right-[-20%]" />

      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <p className="text-white/40 text-sm font-medium">
            {new Date().toLocaleDateString([], { weekday: 'long', month: 'long', day: 'numeric' })}
          </p>
          <h1 className="text-2xl font-black text-white mt-0.5">Today's Progress</h1>
        </div>
        <div className="glass-card px-3 py-2 flex items-center gap-2">
          <span className="w-2 h-2 rounded-full bg-cyan-400 notif-pulse" />
          <span className="text-white/60 text-xs font-medium">{nextReminder}</span>
        </div>
      </div>

      {/* Main ring card */}
      <div className="glass-card p-6 flex flex-col items-center mb-4 relative overflow-hidden">
        <div className="shimmer-bg absolute inset-0 rounded-2xl opacity-30" />

        <div className="relative">
          <ProgressRing percent={percent} size={220} stroke={14} />
          {/* Center content */}
          <div className="absolute inset-0 flex flex-col items-center justify-center">
            <span className="text-4xl font-black text-white">
              {formatMl(animatedTotal)}
            </span>
            <span className="text-white/40 text-sm mt-1">
              of {formatMl(settings.dailyGoal)}
            </span>
            <div className="mt-2 glass-card px-3 py-1 rounded-full">
              <span className="text-cyan-300 font-bold text-lg">{animatedPercent}%</span>
            </div>
          </div>
        </div>

        {/* Motivation */}
        <div className="mt-4 text-center animate-slide-up">
          <p className="text-2xl">{motivation.emoji}</p>
          <p className="text-white/80 font-medium mt-1">{motivation.message}</p>
          {remaining > 0 && (
            <p className="text-white/40 text-sm mt-1">
              {formatMl(remaining)} remaining
            </p>
          )}
        </div>
      </div>

      {/* Stats row */}
      <div className="grid grid-cols-3 gap-3 mb-4">
        {[
          { label: 'Consumed', value: formatMl(total), icon: '💧' },
          { label: 'Remaining', value: formatMl(remaining), icon: '🎯' },
          { label: 'Next Sip', value: nextReminder, icon: '⏰' },
        ].map((s) => (
          <div key={s.label} className="glass-card p-3 flex flex-col items-center text-center">
            <span className="text-lg mb-1">{s.icon}</span>
            <span className="text-white font-bold text-sm">{s.value}</span>
            <span className="text-white/40 text-xs">{s.label}</span>
          </div>
        ))}
      </div>

      {/* Quick Add */}
      <div className="glass-card p-4 mb-4">
        <p className="text-xs font-semibold text-white/40 uppercase tracking-wider mb-3">
          Quick Add
        </p>
        <div className="flex gap-2 mb-3">
          <QuickAddBtn amount={150} label="Small" emoji="☕" onAdd={handleAdd} />
          <QuickAddBtn amount={settings.cupSize} label="Cup" emoji="🥛" onAdd={handleAdd} />
          <QuickAddBtn amount={500} label="Bottle" emoji="🍶" onAdd={handleAdd} />
        </div>
        {!showCustom ? (
          <button
            onClick={() => { setShowCustom(true); setTimeout(() => customRef.current?.focus(), 100); }}
            className="w-full py-2.5 rounded-xl border border-dashed border-white/20 text-white/40 text-sm hover:border-cyan-500/40 hover:text-cyan-400 transition-all duration-200"
          >
            + Custom amount
          </button>
        ) : (
          <div className="flex gap-2 animate-slide-up">
            <input
              ref={customRef}
              type="number"
              value={customVal}
              onChange={(e) => setCustomVal(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleCustomAdd()}
              placeholder="Enter ml..."
              className="glass-input flex-1 text-sm"
              min={1}
              max={5000}
            />
            <button onClick={handleCustomAdd} className="btn-primary px-4 py-2 text-sm">
              Add
            </button>
            <button
              onClick={() => { setShowCustom(false); setCustomVal(''); }}
              className="px-3 py-2 rounded-xl bg-white/5 text-white/40 text-sm hover:bg-white/10 transition-all"
            >
              ✕
            </button>
          </div>
        )}
      </div>

      {/* Toast */}
      {showToast && lastAdded && (
        <div className="fixed bottom-24 left-1/2 -translate-x-1/2 z-50 animate-bounce-in">
          <div className="glass-card px-5 py-3 flex items-center gap-3 border-cyan-500/30">
            <span className="text-cyan-400 text-xl">💧</span>
            <span className="text-white font-semibold">+{lastAdded}ml added!</span>
          </div>
        </div>
      )}
    </div>
  );
}
