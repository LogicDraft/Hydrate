import { useEffect, useRef, useState } from 'react';

interface RippleState {
  x: number;
  y: number;
  key: number;
}

export function useRipple() {
  const [ripples, setRipples] = useState<RippleState[]>([]);
  const counterRef = useRef(0);

  function createRipple(e: React.MouseEvent<HTMLElement>) {
    const rect = e.currentTarget.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;
    const key = counterRef.current++;
    setRipples((prev) => [...prev, { x, y, key }]);
    setTimeout(() => {
      setRipples((prev) => prev.filter((r) => r.key !== key));
    }, 700);
  }

  return { ripples, createRipple };
}

export function useLocalStorage<T>(key: string, initialValue: T) {
  const [value, setValue] = useState<T>(() => {
    try {
      const item = localStorage.getItem(key);
      return item ? JSON.parse(item) : initialValue;
    } catch {
      return initialValue;
    }
  });

  function set(v: T | ((prev: T) => T)) {
    const newValue = v instanceof Function ? v(value) : v;
    setValue(newValue);
    localStorage.setItem(key, JSON.stringify(newValue));
  }

  return [value, set] as const;
}

export function useAnimatedNumber(target: number, duration = 1000) {
  const [current, setCurrent] = useState(0);

  useEffect(() => {
    let start: number | null = null;
    const from = 0;

    const step = (timestamp: number) => {
      if (!start) start = timestamp;
      const progress = Math.min((timestamp - start) / duration, 1);
      const eased = 1 - Math.pow(1 - progress, 3);
      setCurrent(Math.round(from + eased * (target - from)));
      if (progress < 1) requestAnimationFrame(step);
    };

    requestAnimationFrame(step);
  }, [target, duration]);

  return current;
}

export function useNotifications() {
  const [permission, setPermission] = useState<NotificationPermission>(
    'Notification' in window ? Notification.permission : 'denied'
  );

  async function requestPermission(): Promise<boolean> {
    if (!('Notification' in window)) return false;
    const result = await Notification.requestPermission();
    setPermission(result);
    return result === 'granted';
  }

  function sendNotification(title: string, body: string) {
    if (permission !== 'granted') return;
    try {
      new Notification(title, {
        body,
        icon: '/icon-192.png',
        badge: '/icon-192.png',
        tag: 'hydrate-reminder',
      });
    } catch {
      console.warn('Notification failed');
    }
  }

  return { permission, requestPermission, sendNotification };
}
