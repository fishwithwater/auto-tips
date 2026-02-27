# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew buildPlugin          # Build plugin zip (build/distributions/auto-tips-1.0.0.zip)
./gradlew runIde               # Launch test IDE with plugin loaded
./gradlew test                 # Run all tests
./gradlew test --tests "cn.myjdemo.autotips.service.ConfigurationServiceImplTest"  # Run single test class
./gradlew test --tests "*PropertyTest"  # Run property-based tests only
```

## Architecture

Auto-Tips is an IntelliJ plugin that displays non-intrusive popups when a developer calls a method annotated with `@tips` in its Javadoc/KDoc.

**Core event flow:**
1. User types `)` → `TipsTypedActionHandlerImpl` intercepts via `TypedActionHandler`
2. `CallDetectionService` analyzes PSI at cursor to identify the method call
3. `AnnotationParser` extracts `@tips` content from the method's Javadoc/KDoc
4. `CacheService` (LRU, 1000 entries) caches parsed results by method signature
5. `TipDisplayService` shows a non-modal popup (BALLOON/TOOLTIP/NOTIFICATION style)
6. Popup auto-hides after configured duration (default 5000ms)

Auto-completion is handled separately via `AutoCompletionDocumentListener` which hooks into document change events.

**Service scopes:**
- `ConfigurationServiceImpl` — application-scoped (global settings)
- `AnnotationParserImpl`, `CallDetectionServiceImpl`, `TipDisplayServiceImpl`, `CacheServiceImpl`, `JavadocExtractorImpl` — all project-scoped

**Language parsing:** `AnnotationParserImpl` delegates to `JavaLanguageParser` or `KotlinLanguageParser` based on file type. Kotlin support is optional and declared in `kotlin-support.xml`.

**Configuration model:** `TipsConfiguration` (in `TipsContent.kt`) holds all settings: enabled flag, display duration, `TipStyle` enum, custom annotation patterns, and javadoc mode toggle.

## Key Files

- `src/main/resources/META-INF/plugin.xml` — registers all services, listeners, handlers, and the custom `@tips` Javadoc tag
- `src/main/kotlin/cn/myjdemo/autotips/model/TipsContent.kt` — all data classes and enums used across the plugin
- `src/main/kotlin/cn/myjdemo/autotips/service/` — service interfaces
- `src/main/kotlin/cn/myjdemo/autotips/service/impl/` — service implementations

## Testing

Tests use JUnit 5 + Kotest for property-based tests, and Mockito for mocking. There are no IntelliJ platform light fixture tests — all tests mock the platform APIs directly.

Test categories: handler property tests (`handler/`), service unit tests (`service/`), javadoc extraction tests (`javadoc/`), integration tests (`integration/`).
