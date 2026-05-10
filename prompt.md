Build a premium Android application called **Hydrate** using **Kotlin**, **Jetpack Compose**, and **Material 3**.

## 📱 App Concept

Hydrate is a minimalist black-and-white hydration tracking app that intelligently generates a personalized water-drinking schedule based on the user’s wake time, sleep time, daily water goal, and cup size.

The app should feel like a premium wellness application rather than a basic utility app.

---

# 🛠️ Tech Stack

* Kotlin
* Jetpack Compose
* Material 3
* Room Database
* DataStore Preferences
* WorkManager
* AlarmManager
* NotificationManager
* MPAndroidChart (or Compose Charts)
* Hilt for Dependency Injection
* Navigation Compose

Minimum SDK: 26
Target SDK: Latest Android SDK

---

# 🎨 Design System

## Theme

Monochrome Material 3 design.

### Colors

* Background: `#0A0A0A`
* Surface: `#111111`
* Surface Variant: `#1B1B1B`
* Outline: `#2D2D2D`
* Primary: `#FFFFFF`
* Secondary Text: `#A1A1A1`

### Typography

* Font Family: DM Sans
* Monospaced Font: DM Mono (for times and numbers)

### Style

* Rounded corners: 28dp
* Glass-like elevated cards
* Smooth animations
* Minimal black-and-white aesthetic
* Large whitespace and premium spacing

---

# 📲 Screens

## 1. Onboarding Screen

Shown only on first launch.

Fields:

* Wake Time Picker
* Sleep Time Picker
* Daily Goal (ml) — default 2500
* Cup Size Selector:

  * 150 ml
  * 200 ml
  * 250 ml
  * Custom

Actions:

* Calculate number of reminders
* Show live preview
* Save settings
* Navigate to Home

---

## 2. Today Screen

Main dashboard.

Features:

* Animated circular progress ring
* Current intake vs goal
* Percentage completed
* Motivational message
* Quick-add buttons:

  * +1 Cup
  * +250 ml
  * +500 ml
  * Custom amount
* Next reminder countdown
* Today’s log list
* Streak badge

Motivational messages:

* 0–25%: “Great start.”
* 26–50%: “Keep going.”
* 51–75%: “You’re doing great.”
* 76–99%: “Almost there.”
* 100%+: “Goal complete.”

---

## 3. Schedule Screen

Generated reminder timeline.

Features:

* Chronological list of reminder times
* Completed reminders with checkmarks
* Current slot highlighted
* Upcoming slots dimmed
* Cumulative water totals

Example:

* 07:00 — 250 ml ✓
* 08:36 — 500 ml ✓
* 10:12 — Current
* 11:48 — Upcoming

---

## 4. History Screen

Hydration analytics.

Features:

* Last 7 days bar chart
* Current streak
* Longest streak
* Best day
* Average daily intake

---

## 5. Settings Screen

Manage app preferences.

Options:

* Wake Time
* Sleep Time
* Daily Goal
* Cup Size
* Notifications On/Off
* Snooze duration
* Reset today
* Erase all data

---

# 🧠 Core Logic

## Schedule Generation

1. Calculate cups required:
   `dailyGoal / cupSize`
2. Calculate active minutes between wake and sleep.
3. Divide active minutes evenly.
4. Generate reminder timestamps.

Example:

* Goal: 2500 ml
* Cup: 250 ml
* Cups: 10
* Active Hours: 16
* Interval: 96 minutes

Generated reminders:
07:00, 08:36, 10:12, 11:48, 13:24, 15:00, 16:36, 18:12, 19:48, 21:24

---

# 🔔 Notifications

Use WorkManager and AlarmManager.

Features:

* Exact notifications at generated reminder times
* Skip notification if water was logged within the last 20 minutes
* Snooze for 1 hour
* Daily rescheduling at midnight
* Deep link opens Today Screen

Notification text:

* Title: “Time to Hydrate”
* Body: “Drink 250 ml of water.”

Actions:

* “I Drank”
* “Snooze 1 Hour”

---

# 💾 Data Storage

## DataStore

Store:

* Wake time
* Sleep time
* Daily goal
* Cup size
* Notification settings

## Room Database

### WaterLog Entity

* id
* timestamp
* amountMl

### DailyStats Entity (optional)

* date
* totalMl
* goalCompleted

---

# 🗂️ Project Structure

```text
app/
└── src/main/java/com/hydrate/
    ├── data/
    │   ├── local/
    │   ├── repository/
    │   └── model/
    ├── domain/
    │   ├── usecase/
    │   └── scheduler/
    ├── ui/
    │   ├── onboarding/
    │   ├── today/
    │   ├── schedule/
    │   ├── history/
    │   ├── settings/
    │   ├── components/
    │   └── theme/
    ├── workers/
    ├── receivers/
    ├── navigation/
    ├── di/
    └── MainActivity.kt
```

---

# 🧩 Reusable Components

* ProgressRing
* StatCard
* QuickAddButton
* TimelineItem
* StreakBadge
* ReminderCountdown
* SettingsItem
* ConfirmDialog

---

# ✨ Animations

* Progress ring fills smoothly
* Cards fade and slide in
* Timeline items stagger
* Button press scale effect
* Count-up number animations

---

# 📊 Analytics

* Current streak
* Longest streak
* Best hydration day
* Weekly completion percentage
* Average daily intake

---

# 🔒 Permissions

* POST_NOTIFICATIONS (Android 13+)
* SCHEDULE_EXACT_ALARM (if needed)

---

# 🏷️ App Name and Branding

App Name: Hydrate
Package Name: `com.gowtham.hydrate`

Launcher Icon:

* Black background
* White water droplet
* Minimal Material 3 style

---

# 🚀 Deliverables

Generate a complete Android Studio project with:

* Kotlin source code
* Jetpack Compose UI
* Material 3 theme
* Room database
* DataStore
* WorkManager notifications
* Hilt dependency injection
* Fully functional reminder scheduling
* Clean architecture
* Well-commented production-ready code

The project should compile and run immediately in Android Studio without additional setup.
