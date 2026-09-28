# FinniApp — инструкции для агентов

- Перед любой правкой кода, контента или документации загрузи скилл `finni-android-rules`
  (`.claude/skills/finni-android-rules/SKILL.md`): слои, конвенции ViewModel/Compose, ограничения ТЗ, тесты.
- ТЗ: `docs/reference/tz-2026.md`. Текущий план исправлений: `docs/audit/2026-09-28-full-code-check.md`.
- Проверка: `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`.
