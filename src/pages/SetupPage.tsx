import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { saveSettings, DEFAULT_SETTINGS } from '../store';

const CUP_SIZES = [150, 200, 250, 350, 500];

export default function SetupPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    wakeTime: DEFAULT_SETTINGS.wakeTime,
    sleepTime: DEFAULT_SETTINGS.sleepTime,
    dailyGoal: String(DEFAULT_SETTINGS.dailyGoal),
    cupSize: DEFAULT_SETTINGS.cupSize,
  });

  function handleSave() {
    const goal = parseInt(form.dailyGoal, 10);
    if (isNaN(goal) || goal < 500 || goal > 10000) return;
    saveSettings({
      wakeTime: form.wakeTime,
      sleepTime: form.sleepTime,
      dailyGoal: goal,
      cupSize: form.cupSize,
      notificationsEnabled: false,
      setupComplete: true,
    });
    navigate('/dashboard');
  }

  return (
    <div className="min-h-screen bg-deep flex flex-col items-center justify-center p-6 animate-fade-in">
      {/* Background orbs */}
      <div className="orb w-96 h-96 bg-cyan-500/10 top-[-10%] right-[-15%]" />
      <div className="orb w-72 h-72 bg-blue-600/10 bottom-[-5%] left-[-10%]" />

      <div className="relative z-10 w-full max-w-md">
        {/* Logo */}
        <div className="text-center mb-10">
          <div className="inline-flex items-center justify-center w-20 h-20 rounded-3xl bg-gradient-to-br from-cyan-500/30 to-blue-600/30 border border-cyan-500/30 mb-4 animate-float">
            <span className="text-4xl">💧</span>
          </div>
          <h1 className="text-4xl font-black text-gradient mb-2">Hydrate</h1>
          <p className="text-white/50 text-sm font-medium">Your premium water companion</p>
        </div>

        {/* Form card */}
        <div className="glass-card p-6 space-y-6">
          <div>
            <h2 className="text-lg font-bold text-white mb-1">Let's get you set up</h2>
            <p className="text-white/40 text-sm">Takes just 30 seconds</p>
          </div>

          {/* Wake / Sleep */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-white/50 uppercase tracking-wider mb-2">
                Wake Time
              </label>
              <input
                type="time"
                value={form.wakeTime}
                onChange={(e) => setForm({ ...form, wakeTime: e.target.value })}
                className="glass-input text-center"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-white/50 uppercase tracking-wider mb-2">
                Sleep Time
              </label>
              <input
                type="time"
                value={form.sleepTime}
                onChange={(e) => setForm({ ...form, sleepTime: e.target.value })}
                className="glass-input text-center"
              />
            </div>
          </div>

          {/* Daily Goal */}
          <div>
            <label className="block text-xs font-semibold text-white/50 uppercase tracking-wider mb-2">
              Daily Goal
            </label>
            <div className="relative">
              <input
                type="number"
                value={form.dailyGoal}
                onChange={(e) => setForm({ ...form, dailyGoal: e.target.value })}
                min={500}
                max={10000}
                step={100}
                className="glass-input pr-12"
                placeholder="2500"
              />
              <span className="absolute right-4 top-1/2 -translate-y-1/2 text-white/30 text-sm font-medium">
                ml
              </span>
            </div>
            {/* Quick presets */}
            <div className="flex gap-2 mt-2">
              {[1500, 2000, 2500, 3000].map((g) => (
                <button
                  key={g}
                  onClick={() => setForm({ ...form, dailyGoal: String(g) })}
                  className={`flex-1 text-xs py-1.5 rounded-lg font-medium transition-all duration-150 ${
                    form.dailyGoal === String(g)
                      ? 'bg-cyan-500/30 text-cyan-300 border border-cyan-500/50'
                      : 'bg-white/5 text-white/40 border border-white/10 hover:bg-white/10'
                  }`}
                >
                  {g / 1000}L
                </button>
              ))}
            </div>
          </div>

          {/* Cup Size */}
          <div>
            <label className="block text-xs font-semibold text-white/50 uppercase tracking-wider mb-2">
              Default Cup Size
            </label>
            <div className="grid grid-cols-5 gap-2">
              {CUP_SIZES.map((size) => (
                <button
                  key={size}
                  onClick={() => setForm({ ...form, cupSize: size })}
                  className={`flex flex-col items-center py-3 rounded-xl border text-xs font-semibold transition-all duration-150 ${
                    form.cupSize === size
                      ? 'bg-cyan-500/20 border-cyan-400/60 text-cyan-300'
                      : 'bg-white/5 border-white/10 text-white/40 hover:bg-white/10'
                  }`}
                >
                  <span className="text-base mb-0.5">
                    {size <= 200 ? '☕' : size <= 350 ? '🥛' : '🍶'}
                  </span>
                  {size}
                </button>
              ))}
            </div>
          </div>

          {/* CTA */}
          <button
            onClick={handleSave}
            className="btn-primary w-full text-center"
          >
            Start Tracking →
          </button>
        </div>

        <p className="text-center text-white/20 text-xs mt-4">
          All data stays on your device
        </p>
      </div>
    </div>
  );
}
