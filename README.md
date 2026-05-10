# React + TypeScript + Vite

This template provides a minimal setup to get React working in Vite with HMR and some ESLint rules.

Currently, two official plugins are available:

- [@vitejs/plugin-react](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react) uses [Oxc](https://oxc.rs)
- [@vitejs/plugin-react-swc](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react-swc) uses [SWC](https://swc.rs/)

## React Compiler

The React Compiler is not enabled on this template because of its impact on dev & build performances. To add it, see [this documentation](https://react.dev/learn/react-compiler/installation).

## Expanding the ESLint configuration

If you are developing a production application, we recommend updating the configuration to enable type-aware lint rules:

```js
export default defineConfig([
  # Hydrate

  Hydrate is a premium monochrome hydration tracker for Android built with Kotlin, Jetpack Compose, Material 3, Room, DataStore, Hilt, WorkManager, and AlarmManager.

  ## Highlights

  - Personalized reminder schedule from wake time, sleep time, daily goal, and cup size.
  - Dark, minimal black-and-white UI with glass-like cards and rounded surfaces.
  - Today, Schedule, History, and Settings screens with reusable Compose components.
  - Room-backed water logs and daily stats.
  - DataStore-backed preferences for onboarding and reminder settings.
  - Exact reminder alarms with notification actions for logging and snoozing.

  ## Project Structure

  - `app/src/main/java/com/gowtham/hydrate/data` for persistence and models.
  - `app/src/main/java/com/gowtham/hydrate/domain` for schedule and analytics use cases.
  - `app/src/main/java/com/gowtham/hydrate/ui` for the Compose UI and theme.
  - `app/src/main/java/com/gowtham/hydrate/receivers` and `app/src/main/java/com/gowtham/hydrate/workers` for reminder delivery.

  ## Notes

  - The app uses built-in fonts in this scaffold so it compiles without extra asset setup. You can drop DM Sans and DM Mono into `app/src/main/res/font` later if you want pixel-perfect typography.
  - The launcher icon is generated from local vector resources and matches the requested monochrome direction.

  ## Build

  - Open the folder in Android Studio and sync the Gradle project.
  - If you want to build from the command line, add the standard Gradle wrapper jar under `gradle/wrapper/` first.
// eslint.config.js
