# SkyWay: Авиабилеты

Лабораторная работа №2 по дисциплине «Разработка мобильных приложений». Вариант 3. Тема работы — проектирование интерфейса с Material Design 3 и Jetpack Compose.

Приложение содержит экран параметров поиска авиабилетов: маршрут, даты, тип поездки, количество пассажиров, класс обслуживания и фильтр прямых рейсов. Поля, переключатели и календарь работают локально. Поиск через API авиакомпаний, покупка билетов, избранное и список купленных билетов в эту лабораторную не входят: соответствующие действия показывают уведомление о нереализованном функционале. Отдельный отчёт не создавался.

## Открытие и запуск

1. В Android Studio выберите **File → Open** и откройте каталог `SkyWay`, содержащий [settings.gradle.kts](settings.gradle.kts).
2. В **Settings → Build, Execution, Deployment → Build Tools → Gradle** выберите встроенный в установленную Android Studio **Gradle JDK / JBR 25**. Не используйте автоматически найденный системный JDK 17 для данной версии Gradle.
3. В SDK Manager установите Android SDK Platform 37 и предложенные Android Studio Build Tools. Android Studio создаст или обновит `local.properties` с расположением вашего SDK; этот файл не нужно переносить между компьютерами.
4. Выполните **Sync Project with Gradle Files**. При первой сборке на другом компьютере нужен интернет для загрузки Gradle и зависимостей.
5. Выберите конфигурацию `app`, эмулятор или подключённый телефон с **Android 8.0 / API 26 и выше** и нажмите **Run**.

Версии проекта: Android Gradle Plugin **9.4.1**, Gradle **9.6.0**, Compose BOM **2026.02.01**, `compileSdk` и `targetSdk` **37**, `minSdk` **26**. Настройки находятся в [корневом build.gradle.kts](build.gradle.kts), [app/build.gradle.kts](app/build.gradle.kts) и [Gradle Wrapper](gradle/wrapper/gradle-wrapper.properties). Отдельная установка Gradle не требуется.

Команды из корня проекта в PowerShell при настроенном `JAVA_HOME` на JBR Android Studio:

```powershell
.\gradlew.bat assembleDebug lintDebug testDebugUnitTest
```

Собранный установочный файл: `app/build/outputs/apk/debug/app-debug.apk`. Эта команда собирает приложение и запускает Android Lint. Отдельных JVM-тестов нет: поведение формы проверяется инструментальными Compose-тестами, поэтому задача `testDebugUnitTest` может завершиться со статусом `NO-SOURCE`.

В [FlightSearchScreenTest.kt](app/src/androidTest/java/com/example/skyway/FlightSearchScreenTest.kt) находятся семь UI-тестов: двойной обмен маршрута, ошибка одинаковых городов, границы количества пассажиров, переключение типа поездки, восстановление формы после пересоздания Activity и Snackbar при поиске и открытии избранного. Для запуска нужен работающий эмулятор или телефон:

```powershell
.\gradlew.bat assembleDebugAndroidTest
.\gradlew.bat connectedDebugAndroidTest
```

Первая команда только компилирует тестовый APK; вторая действительно выполняет тесты на устройстве.

## Реализация каждого пункта задания

### 1. Анализ функциональных требований

Пользователь хочет указать, откуда и куда лететь, выбрать даты и условия поездки. На этой основе реализованы:

- ввод произвольного города и выбор из локализованного списка городов;
- обмен пунктов отправления и назначения;
- режимы «Туда-обратно» и «В одну сторону»;
- выбор даты вылета и, для обратного маршрута, даты возвращения;
- от 1 до 9 пассажиров, классы «Эконом», «Бизнес», «Первый»;
- фильтр «Только прямые рейсы» и итоговое резюме параметров;
- проверка обязательных полей, одинаковых городов и корректности дат.

Основная реализация — [FlightSearchScreen.kt](app/src/main/java/com/example/skyway/ui/FlightSearchScreen.kt), функции `FlightSearchScreen`, `RouteCard`, `PreferencesCard`, `SearchSummary`. Прошедшие даты недоступны; дата возвращения не может предшествовать вылету. Если вылет переносится позже текущего возвращения, дата возвращения корректируется.

### 2. Генерация темы и Material You

Фирменная палитра подготовлена в [официальном Material Theme Builder](https://material-foundation.github.io/material-theme-builder/) с исходным цветом **#1764C0**, системным шрифтом и стандартной контрастностью. Параметры генерации и значения палитры сохранены в [design/skyway-theme.json](design/skyway-theme.json). Роли фиксированной светлой и тёмной палитры описаны в [Color.kt](app/src/main/java/com/example/skyway/ui/theme/Color.kt), выбор схемы — в [Theme.kt](app/src/main/java/com/example/skyway/ui/theme/Theme.kt).

На Android 12 / API 31 и выше `SkyWayTheme` использует динамические `dynamicLightColorScheme` / `dynamicDarkColorScheme`: цвета зависят от оформления устройства. На API 26–30 автоматически используется фиксированная фирменная схема. Светлый или тёмный вариант соответствует системной теме. Для воспроизводимых Compose Preview динамическая палитра отключена параметром `dynamicColor = false`.

### 3. Архитектура темы

[Theme.kt](app/src/main/java/com/example/skyway/ui/theme/Theme.kt) объединяет цветовую схему из [Color.kt](app/src/main/java/com/example/skyway/ui/theme/Color.kt) и стили текста из [Typography.kt](app/src/main/java/com/example/skyway/ui/theme/Typography.kt) и передаёт их в `MaterialTheme`. Тема устанавливается над всем деревом интерфейса в [MainActivity.kt](app/src/main/java/com/example/skyway/MainActivity.kt).

Дочерние компоненты обращаются к смысловым ролям: `MaterialTheme.colorScheme.primary` — основные действия, `primaryContainer` / `onPrimaryContainer` — вступительный блок, `surfaceContainerLowest` — карточки, `error` — сообщения об ошибках. Текст использует роли `headlineLarge`, `titleLarge`, `titleMedium`, `bodyMedium`, `labelMedium` и другие стили `MaterialTheme.typography`. Это централизует оформление и сохраняет согласованность светлой и тёмной тем.

Дополнительные токены отступов вынесены в [Spacing.kt](app/src/main/java/com/example/skyway/ui/theme/Spacing.kt): `xs = 4.dp`, `sm = 8.dp`, `md = 16.dp`, `lg = 24.dp`, `xl = 32.dp`. Они доступны через `MaterialTheme.spacing` и собственный `CompositionLocal`.

### 4. Ресурсы и два языка

Все пользовательские подписи, подсказки, ошибки, уведомления и описания кнопок вынесены в ресурсы:

- [res/values/strings.xml](app/src/main/res/values/strings.xml) — английский язык и ресурсы по умолчанию;
- [res/values-ru/strings.xml](app/src/main/res/values-ru/strings.xml) — русский язык;
- [res/xml/locales_config.xml](app/src/main/res/xml/locales_config.xml) — список поддерживаемых языков для настроек Android.

Compose получает текст через `stringResource` и `pluralStringResource`. Для количества пассажиров используются формы множественного числа; даты форматируются с учётом текущей локали. Предустановленные города сохраняются как индексы ресурсов, поэтому переводятся при смене языка; произвольный ввод пользователя сохраняется как введён. Технические теги тестов и имена векторных значков не являются текстом интерфейса.

Язык выбирается системой. На Android 13+ его также можно менять в системных настройках языка приложения SkyWay; на старых версиях — изменением языка устройства.

### 5. Проектирование одного экрана

Реализован предложенный в задании для SkyWay **экран параметров и фильтров поиска авиабилетов**. Его структура:

| Компонент | Назначение |
| --- | --- |
| `BrandBar` | Название и значок приложения |
| `JourneyHero` | Заголовок и пояснение задачи экрана |
| `RouteCard` | Тип поездки, пункты маршрута, обмен направления, даты |
| `PreferencesCard` | Пассажиры, класс обслуживания, пересадки |
| `SearchSummary` | Сводка выбранных параметров и кнопка поиска |
| `NavigationBar` | Поиск и действия «Избранное», «Билеты» с уведомлением |
| `TravelDateDialog` | Выбор допустимой даты в календаре |

На ширине от **840 dp** карточки маршрута и параметров располагаются рядом, на узком экране — последовательно. `BoxWithConstraints` выбирает компоновку, `verticalScroll` сохраняет доступность содержимого на небольшом экране. Изменение компоновки не создаёт отдельный экран и не сбрасывает введённые данные.

### 6. Компоненты Material Design 3

В [FlightSearchScreen.kt](app/src/main/java/com/example/skyway/ui/FlightSearchScreen.kt) используются готовые M3-компоненты: `Card`, `Surface`, `PrimaryTabRow`, `Tab`, `OutlinedTextField`, `ExposedDropdownMenuBox`, `DropdownMenuItem`, `FilledTonalIconButton`, `DatePickerDialog`, `DatePicker`, `FilterChip`, `Switch`, `Button`, `NavigationBar`, `NavigationBarItem`. В корне приложения находятся `Scaffold` и `SnackbarHost`.

Взаимоисключающий выбор класса выражен выбранным чипсом, бинарный фильтр пересадок — переключателем, главное действие — заполненной кнопкой. Декоративные значки не дублируют подписи для средств доступности; самостоятельные кнопки со значками имеют локализованные `contentDescription`. Раскрытие ошибки и изменение размеров оформлены через `AnimatedVisibility` и `animateContentSize`.

### 7. Конвейер внедрения компонентов

| Бизнес-требование | Спецификация M3 | Выбор и валидация | Реализация в Compose | Стилизация |
| --- | --- | --- | --- | --- |
| Ввести или выбрать город | [Text fields](https://m3.material.io/components/text-fields/overview), [Menus](https://m3.material.io/components/menus/overview) | Редактируемое поле с вариантами; ошибка для пустого/одинакового маршрута | `OutlinedTextField` и `ExposedDropdownMenuBox` в `CityField` | Форма `medium`, подпись, подсказка, `isError` |
| Выбрать тип поездки | [Tabs](https://m3.material.io/components/tabs/overview) | Два взаимоисключающих режима; возврат скрывается в режиме одной стороны | `PrimaryTabRow`, `Tab`, `RouteCard` | Цвета и текстовые роли темы |
| Поменять направление | [Icon buttons](https://m3.material.io/components/icon-buttons/overview) | Компактное действие над парой полей; обменивает и выбранные, и введённые города | `FilledTonalIconButton` | Тональная кнопка и локализованное описание |
| Указать даты | [Date pickers](https://m3.material.io/components/date-pickers/overview) | Календарь исключает прошлое и возврат до вылета | `DatePickerDialog`, `DatePicker`, `SelectableDates` | Схема и типографика темы |
| Выбрать класс | [Chips](https://m3.material.io/components/chips/overview) | Выбран ровно один из трёх классов | `FilterChip` в `PreferencesCard` | Состояние `selected`, значок выбора, перенос в `FlowRow` |
| Оставить прямые рейсы | [Switch](https://m3.material.io/components/switch/overview) | Бинарный фильтр, видимое состояние | `Switch` | Автоматические состояния M3 и значок выбора |
| Запустить поиск | [Buttons](https://m3.material.io/components/buttons/overview) | Одно главное действие; сначала проверяется форма | `Button` в `SearchSummary` | Заливка `primary`, высота не менее 56 dp |
| Объяснить отсутствие функции | [Snackbar](https://m3.material.io/components/snackbar/overview) | Короткое уведомление с возможностью закрытия | `SnackbarHost` и `PlaceholderNotifier` | Стандартный контраст M3 |

### 8. Сохранение состояния

`FlightSearchScreen` использует `rememberSaveable` для индексов выбранных городов и произвольного ввода, типа поездки, дат, числа пассажиров, класса, фильтра прямых рейсов, открытого календаря и признака запрошенной валидации. Раскрытие списка города также хранится через `rememberSaveable`. Календарь использует `rememberDatePickerState`.

Сохраняются простые типы (`Int`, `Long`, `Boolean`, `String`), которые Android может восстановить из сохранённого состояния Activity. При повороте экрана или пересоздании Activity после смены системной темы форма восстанавливается. Это механизм состояния интерфейса, а не база данных для постоянного хранения поисков.

### 9. Отступы и разделение блоков

Для внешних и внутренних отступов используются `Modifier.padding` и общие токены `MaterialTheme.spacing`; для расстояний — `Arrangement.spacedBy` и `Spacer`. `HorizontalDivider` разделяет маршрут, даты и настройки; `VerticalDivider` визуально отделяет даты вылета и возвращения. Размеры и ограничения задают `fillMaxWidth`, `widthIn`, `heightIn` и `weight`.

### 10. Предупреждение о нереализованном функционале

В [PlaceholderNotifier.kt](app/src/main/java/com/example/skyway/ui/theme/PlaceholderNotifier.kt) воспроизведён механизм из примера задания:

1. `PlaceholderNotifier` задаёт действие `notifyPlaceholder()`.
2. `LocalPlaceholderNotifier = staticCompositionLocalOf` объявляет общий объект уведомлений. Обращение без провайдера выдаёт техническую ошибку разработчика.
3. `SkyWayApp` в [MainActivity.kt](app/src/main/java/com/example/skyway/MainActivity.kt) создаёт `SnackbarHostState`, `rememberCoroutineScope` и локализованный текст, затем запоминает реализацию уведомления.
4. `CompositionLocalProvider` предоставляет эту реализацию вложенному интерфейсу.
5. Расширение `MaterialTheme.snackbar` возвращает `LocalPlaceholderNotifier.current`; вложенный экран получает объект без цепочки параметров через промежуточные функции.
6. `showNotImplemented` запускает coroutine, закрывает предыдущее сообщение и вызывает приостанавливаемый `showSnackbar` с `SnackbarDuration.Short` и кнопкой закрытия.

Кнопки «Избранное» и «Билеты», а также поиск с корректно заполненной формой вызывают этот механизм. Некорректная форма сначала показывает ошибки соответствующих полей. Реального запроса к сервису авиабилетов нет; интерфейс прямо обозначен как учебный.

## Preview и ручная проверка

Откройте [FlightSearchScreen.kt](app/src/main/java/com/example/skyway/ui/FlightSearchScreen.kt) в режиме **Split** или **Design**. Функция `FlightSearchPreview` имеет четыре конфигурации:

- **Russian · light** — русский язык, светлая тема, 412 × 915 dp;
- **English · light** — английский язык, светлая тема, 412 × 915 dp;
- **Russian · dark** — русский язык, тёмная тема, 412 × 915 dp;
- **Tablet · English** — английский язык, широкий экран 1100 × 900 dp.

Preview использует фиксированную тему и пустую реализацию уведомления, поскольку нужен для просмотра оформления. Работу Snackbar проверяйте в запущенном приложении.

| Проверка | Ожидаемый результат |
| --- | --- |
| Изменить города и дважды поменять направление | После второго обмена исходный маршрут восстановлен |
| Очистить город или задать одинаковые города, нажать поиск | Локализованная ошибка, поиск не запускается |
| Открыть вылет и возвращение | Прошедшие даты и возврат до вылета недоступны |
| Выбрать вылет позже текущего возвращения | Возвращение автоматически перенесено на допустимую дату |
| Переключить «В одну сторону» | Возвращение скрыто и не участвует в проверке |
| Нажимать «−» и «+» у пассажиров | Диапазон ограничен 1–9, кнопка на границе отключена |
| Изменить маршрут, даты, класс, пассажиров и пересадки; повернуть устройство | Значения формы сохранены |
| Сменить системную светлую/тёмную тему | Оформление изменено, значения сохранены |
| Сменить язык приложения или устройства | Подписи, ошибки, календарь и предустановленные города локализованы |
| Нажать поиск с корректной формой, «Избранное» или «Билеты» | Показано уведомление о нереализованной функции |
| Проверить API 31+ с разными обоями | Используется динамическая системная палитра |
| Проверить API 26–30 | Используется фиксированная фирменная палитра |
| Открыть landscape, широкий экран и увеличить системный шрифт | Содержимое доступно прокруткой, чипсы переносятся, на ширине от 840 dp карточки стоят рядом |

## Фактически выполненные проверки

31 октября 2026 года:

- `assembleDebug` — APK приложения собран; его пакет, версии SDK и отладочная подпись дополнительно проверены инструментами Android SDK (`aapt`, `apksigner`).
- `assembleDebugAndroidTest` — APK с семью инструментальными Compose-тестами собран.
- `lintDebug` — выполнен успешно: 0 ошибок, 4 предупреждения (настройка резервного копирования и три неиспользуемых строковых ресурса).
- `testDebugUnitTest` — `NO-SOURCE`: отдельных JVM-тестов в проекте нет.
- UI-тесты на устройстве **не выполнялись**: эмулятор не удалось нормально загрузить в изолированной среде. Для их выполнения используйте `connectedDebugAndroidTest` на запущенном эмуляторе или подключённом телефоне.

Из-за особенностей проверки прав Java в изолированной среде сборка проверялась во временной копии проекта. Исходный проект сохранён целиком; временный ключ подписи и локальные настройки SDK в исходный архив и Git не включены.

## Основные файлы

```text
SkyWay/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/example/skyway/
│   │   ├── MainActivity.kt
│   │   └── ui/
│   │       ├── FlightSearchScreen.kt
│   │       └── theme/
│   │           ├── Color.kt
│   │           ├── Theme.kt
│   │           ├── Typography.kt
│   │           ├── Spacing.kt
│   │           └── PlaceholderNotifier.kt
│   └── res/
│       ├── values/strings.xml
│       ├── values-ru/strings.xml
│       └── xml/locales_config.xml
├── gradle/wrapper/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```
