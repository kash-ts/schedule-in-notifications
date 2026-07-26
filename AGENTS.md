# Инструкции для AI-агента: работа с Android-проектом

Ты работаешь с проектом на **Android Studio / Gradle**. Прежде чем вносить любые
изменения в код, конфигурацию сборки или зависимости, ты обязан выполнить
проверки ниже и не пропускать этот этап, даже если задача кажется простой.

## 1. Определи контекст проекта

- Убедись, что находишься в корне Android-проекта: должен быть файл
  `settings.gradle` или `settings.gradle.kts`, и папка `app` (или другой
  основной модуль) с `build.gradle`/`build.gradle.kts`.
- Прочитай `app/build.gradle.kts` (или `.gradle`) и определи:
  - `compileSdk`, `minSdk`, `targetSdk`
  - версию Android Gradle Plugin (AGP)
  - версию Kotlin
  - используется ли Jetpack Compose или классический View-based UI
- Прочитай `gradle/libs.versions.toml` (если используется Version Catalog) —
  это основной источник версий зависимостей в современных проектах.

## 2. Проверь связь с живым Android Studio (если доступно)

Если установлен Android CLI, перед началом работы выполни:

```
android studio check
```

- Если Android Studio запущена с этим проектом — используй её возможности
  вместо угадывания:
  - `android studio analyze-file <путь>` — проверка файла на ошибки/warning'и
    через реальный инспектор IDE, а не по памяти.
  - `android studio find-usages <symbol>` — перед переименованием или
    удалением чего-либо, ищи все реальные использования по всему проекту.
  - `android studio find-declaration <symbol>` — перед правкой чужого кода
    находи точное место объявления.
  - `android studio version-lookup <lib>` — перед добавлением зависимости
    проверяй актуальную стабильную версию, а не пиши версию по памяти.
- Если `android studio check` не находит запущенных инстансов — сообщи об
  этом пользователю явно, а не продолжай работу вслепую по одним файлам.

## 3. Проверь окружение сборки

Перед тем как предлагать команды сборки/запуска, убедись, что:

- Android SDK установлен и путь к нему корректен:
  ```
  android info
  ```
- Нужные платформы и build-tools стоят:
  ```
  android sdk list --all
  ```
  Если `compileSdk` из шага 1 не совпадает с установленными платформами —
  предупреди пользователя и предложи либо установить нужную платформу
  (`android sdk install platforms/android-<N>`), либо синхронизировать
  `compileSdk` под то, что реально установлено.
- JAVA_HOME указывает на JDK 17+ (стандарт для современных AGP):
  ```
  echo $env:JAVA_HOME
  ```

## 4. Проверь состояние Gradle-синхронизации

- НЕ вноси изменения в зависимости или build-скрипты, не проверив сперва,
  нет ли уже несовместимости версий (например, между `androidx.core` и
  `compileSdk` — частая причина ошибок AAR metadata).
- После любых правок в `build.gradle*` файлах явно проговори пользователю,
  что проект нужно синхронизировать (`Sync Now` в Android Studio), сам
  агент это не делает автоматически.

## 5. Проверь доступность устройства/эмулятора перед запуском приложения

- Список доступных виртуальных устройств:
  ```
  android emulator list
  ```
- Список подключённых физических/запущенных устройств:
  ```
  adb devices
  ```
- Если ни одного устройства нет — не пытайся выполнить `android run`,
  сначала предложи пользователю запустить эмулятор
  (`android emulator start <device-name>`) или подключить физическое
  устройство.
- На Windows команда `android emulator` для запуска эмулятора сейчас
  отключена (известное ограничение) — в этом случае используй напрямую
  `emulator.exe` из `%ANDROID_HOME%\emulator`.

## 6. Перед деплоем — собери актуальный APK, не полагайся на старый билд

- `android run` только устанавливает уже собранный APK, он не пересобирает
  проект. Перед деплоем убедись, что сборка выполнена заново, если были
  изменения в коде (через Gradle или через Android Studio).

## 7. Общие правила поведения

- Никогда не меняй `minSdk`, `targetSdk`, `compileSdk` или версии
  AGP/Kotlin без явного запроса пользователя — только сообщай о
  несовместимостях и предлагай варианты.
- Перед добавлением новой библиотеки — проверь актуальную версию через
  `android studio version-lookup`, а не полагайся на версию из памяти.
- Если что-то в окружении не соответствует ожиданиям (нет SDK-платформы,
  не запущен эмулятор, нет связи с Android Studio) — сообщи об этом прямо
  и предложи конкретную команду для исправления, прежде чем продолжать
  основную задачу.

---

## 8. Соглашения проекта «Schedule in notifications»

> Этот раздел описывает реальные соглашения конкретного проекта.
> Всё нижеперечисленное подтверждено чтением кода — ничего не придумано.

### 8.1 Параметры сборки (актуально на 2026-07)

| Параметр        | Значение                              |
|-----------------|---------------------------------------|
| AGP             | `9.3.1`                               |
| Kotlin          | `2.4.10`                              |
| KSP             | `2.3.10`                              |
| `compileSdk`    | `36` (API 36.1, через новый DSL `release {}`) |
| `minSdk`        | `36`                                  |
| `targetSdk`     | `36`                                  |
| JVM target      | `11` (sourceCompatibility + jvmTarget) |
| JDK для демона  | JDK 17+ (см. `gradle/gradle-daemon-jvm.properties`) |

> **Важно:** `minSdk = 36` означает, что проект не запустится ни на одном
> эмуляторе/устройстве с API < 36. Перед запуском убедись через
> `android emulator list`, что эмулятор создан с API 36+.

### 8.2 Модульная структура

Проект **одномодульный**: единственный модуль `:app` (`include(":app")` в
`settings.gradle.kts`). Корневой `build.gradle.kts` содержит только плагины
с `apply false`. Любые новые возможности добавляй **в тот же модуль `:app`**,
пока пользователь явно не попросил создать новый модуль.

### 8.3 Архитектура и пакетная структура

Пакет `com.example.scheduleinnotifications` разделён на три слоя:

```
data/
  dao/         — интерфейсы Room DAO (LessonDao, ScheduleDao)
  db/          — AppDatabase (синглтон через companion object)
  model/       — Entity-классы (Schedule, Lesson) — они же Room-сущности
  repository/  — ScheduleRepository (принимает Context, создаёт DB)
service/       — ScheduleNotificationService (Foreground Service)
ui/
  adapter/     — RecyclerView адаптеры (LessonAdapter, ScheduleAdapter)
  viewmodel/   — ScheduleViewModel (единственный, AndroidViewModel)
  *.kt         — фрагменты и MainActivity
```

**Паттерн:** MVVM без Clean Architecture. Нет domain-слоя и use case'ов.
`ScheduleRepository` напрямую создаётся в `ScheduleViewModel` через
`ScheduleRepository(application)` — DI **не используется**.

- Не добавляй Hilt/Koin без явного запроса — сейчас DI отсутствует намеренно
  (или не было приоритетом).
- `ScheduleViewModel` — **единственный** ViewModel, он используется через
  `activityViewModels()` во всех фрагментах. Состояние UI не вынесено в
  отдельный `*State` data class — LiveData возвращает напрямую списки моделей.

### 8.4 База данных: Room

- Библиотека: `androidx.room` v`2.8.4`, кодогенерация через **KSP** (не KAPT).
- Схема: 2 таблицы — `schedules` и `lessons` (FK: `lessons.scheduleId → schedules.id`,
  `CASCADE DELETE`).
- `exportSchema = false` — миграции не настроены, схема не сохраняется.
  При изменении схемы нужно либо добавить миграцию, либо дать пользователю
  удалить данные.
- `AppDatabase` — синглтон с `@Volatile` + `synchronized` блоком. Инстанс
  создаётся в `AppDatabase.getInstance(context)`.
- DAOs возвращают `LiveData<List<*>>` для реактивного наблюдения и
  `suspend fun` для разовых запросов в корутинах.
- Имя БД в device: `schedule_db`.

### 8.5 Асинхронность

- Только **Kotlin Coroutines + LiveData**. RxJava в проекте нет.
- `ScheduleViewModel` использует `viewModelScope.launch { }` для всех
  suspend-операций с репозиторием.
- `ScheduleNotificationService` создаёт свой `CoroutineScope(Dispatchers.IO + SupervisorJob())`
  и отменяет его в `onDestroy()`.

### 8.6 UI: классический XML View-based (не Compose)

- UI построен на **XML-layouts** + **ViewBinding** (не DataBinding, не Compose).
- Компоненты: Material3 (тема `Theme.Material3.DayNight.NoActionBar`),
  ConstraintLayout, RecyclerView, MaterialAlertDialogBuilder, MaterialTimePicker,
  ChipGroup, FloatingActionButton, Snackbar.
- **Не предлагай** Jetpack Compose без явного запроса.
- Навигация: Navigation Component v`2.9.0`, граф `nav_graph.xml`.
  Аргументы между фрагментами передаются через `Bundle` вручную (не SafeArgs).
- Edge-to-edge включён (`enableEdgeToEdge()` в `MainActivity.onCreate()`),
  insets обрабатываются вручную через `ViewCompat.setOnApplyWindowInsetsListener`
  в каждом фрагменте отдельно (паттерн `setupInsets()`).

### 8.7 Сеть и изображения

В проекте **нет сетевых запросов** и **нет загрузки изображений**.
Не добавляй Retrofit, OkHttp, Glide, Coil — они здесь не нужны.

### 8.8 Foreground Service

- `ScheduleNotificationService` — `foregroundServiceType="specialUse"`.
- Обновляет уведомления **раз в минуту** (ждёт до начала следующей минуты).
- Канал уведомлений: `schedule_channel`, `IMPORTANCE_DEFAULT`, без звука и
  вибрации.
- Системное уведомление сервиса: `FOREGROUND_NOTIF_ID = 1`.
- Уведомления расписаний: `id = NOTIF_BASE_ID + index` (начиная со `100`).
  При добавлении > 1 включённого расписания ID назначаются по индексу —
  это нужно учитывать при удалении расписаний (старые уведомления могут
  остаться, пока сервис не обновится).
- Сервис запускается из `ScheduleListFragment.syncService()` при каждом
  переключении тумблера расписания или его удалении.

### 8.9 Добавление зависимостей

Все зависимости объявляются **только** в `gradle/libs.versions.toml`
(Version Catalog). Прямые строковые версии в `build.gradle.kts` недопустимы.

Шаблон для добавления новой зависимости:
1. В `[versions]` добавь алиас версии.
2. В `[libraries]` добавь запись с `version.ref`.
3. В `app/build.gradle.kts` используй `implementation(libs.xxx)`.
4. Сообщи пользователю о необходимости **Gradle Sync**.

Уже выбранные библиотеки для типовых задач (не заменяй их):

| Задача                | Библиотека                                  |
|-----------------------|---------------------------------------------|
| БД                    | Room 2.8.4 (KSP)                            |
| ViewModel/LiveData    | Lifecycle 2.9.1                             |
| Навигация             | Navigation Fragment KTX 2.9.0              |
| UI-компоненты         | Material3 1.12.0                            |
| Верстка               | ConstraintLayout 2.2.1                      |
| Корутины              | Kotlin Coroutines (встроены в lifecycle/room) |

### 8.10 Тесты (реальное состояние)

- Тесты **почти отсутствуют**: есть только scaffold-файлы `ExampleUnitTest.kt`
  (проверяет `2+2=4`) и `ExampleInstrumentedTest.kt` (проверяет package name).
- Реального покрытия логики нет. Это нормально для проекта на текущем этапе,
  но не следует заявлять, что "тесты есть".
- Запуск unit-тестов: `.\gradlew.bat testDebugUnitTest`
- Запуск instrumented-тестов: `.\gradlew.bat connectedDebugAndroidTest`
  (требует запущенного устройства/эмулятора API 36+).

### 8.11 Статический анализ и линт

- ktlint и detekt **не настроены**.
- Стиль Kotlin: `kotlin.code.style=official` (в `gradle.properties`).
- `app/build.gradle.kts` не содержит кастомных правил lint.
- Перед коммитом достаточно убедиться, что сборка проходит:
  ```
  .\gradlew.bat assembleDebug
  .\gradlew.bat testDebugUnitTest
  ```

### 8.12 CI/CD

CI **не настроен** (нет папки `.github/workflows` или аналога).
Любые проверки выполняются вручную локально.

### 8.13 Конвенции именования (по реальному коду)

- **Layouts:** `activity_*.xml`, `fragment_*.xml`, `item_*.xml`, `dialog_*.xml`
- **Фрагменты:** `*Fragment.kt` в пакете `ui/`
- **Adapters:** `*Adapter.kt` в пакете `ui/adapter/`
- **ViewModel:** `*ViewModel.kt` в пакете `ui/viewmodel/`
- **DAO:** `*Dao.kt` в пакете `data/dao/`
- **Entities:** `*.kt` в пакете `data/model/` (без суффикса Entity)
- **Strings:** snake_case в `strings.xml`; строки на **русском языке**
- **Drawables:** `ic_*` для иконок

### 8.14 Известные проблемы и ловушки

1. ~~**Дублирование логики `calendarDayToLocal`**~~ — **исправлено**.
   Логика вынесена в `util/DateUtils.kt` (`DateUtils.calendarDayToLocal()` и
   `DateUtils.todayLocal()`). Оба потребителя (`ScheduleNotificationService`,
   `ScheduleDetailFragment`) теперь используют общий объект.

2. ~~**Фильтрация уроков по дню на стороне UI**~~ — **исправлено**.
   `ScheduleViewModel` теперь хранит `_selectedDay: MutableLiveData<Int>` и
   предоставляет `lessonsForCurrentDay: LiveData<List<Lesson>>` через
   `MediatorLiveData`. Фрагмент только наблюдает результат.
   `viewModel.selectDay(day)` — единственная точка изменения выбранного дня.

3. ~~**Аргументы передаются через Bundle вручную**~~ — **исправлено**.
   Подключён плагин `androidx.navigation.safeargs.kotlin` v`2.9.8`
   (не `2.9.0` — несовместима с AGP 9+; отдельный алиас `navigationSafeArgs`
   в `libs.versions.toml`). Навигация теперь:
   ```kotlin
   // отправитель:
   ScheduleListFragmentDirections
       .actionScheduleListFragmentToScheduleDetailFragment(id, name)
   // получатель:
   val args = ScheduleDetailFragmentArgs.fromBundle(requireArguments())
   args.scheduleId   // Long
   args.scheduleName // String
   ```
   > **Ловушка SafeArgs + AGP 9:** плагин SafeArgs объявляется в корневом
   > `build.gradle.kts` с `apply false`, затем применяется в `app/build.gradle.kts`.
   > Использовать версию ниже `2.9.8` нельзя — выдаёт
   > `safeargs plugin must be used with android plugin`.

4. ~~**`AppDatabase` без миграций**~~ — **частично исправлено**.
   Добавлено `.fallbackToDestructiveMigration()` — при изменении схемы
   данные сбрасываются вместо краша. `exportSchema = false` оставлен.
   > **Перед продакшеном:** переключи `exportSchema = true` и добавь
   > явные объекты `Migration` вместо `fallbackToDestructiveMigration`.

5. ~~**Dark theme не доработана**~~ — **исправлено**.
   `values/colors.xml` содержит полный набор токенов `md_theme_dark_*`.
   `values-night/themes.xml` применяет их через `@color/` ссылки.
   Все hex-значения в `values/themes.xml` заменены ссылками на `@color/`.

6. **`minSdk = 36`** — очень высокий минимум (Android 16). Проект требует
   эмулятора/устройства с API 36+. Это намеренное ограничение, не трогай
   без явного запроса.

7. **Util-пакет** — создан `util/DateUtils.kt`. Новые утилиты общего назначения
   (конвертации, форматирование) добавляй туда, а не дублируй в сервисах/фрагментах.