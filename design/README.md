# Палитра SkyWay

В официальном [Material Theme Builder](https://material-foundation.github.io/material-theme-builder/) выбран Source color `#1764C0`, стандартная контрастность и системные шрифты. Конструктор рассчитывает светлую и тёмную палитру; поэтому итоговый `primary` не обязан совпадать с исходным цветом.

Для воспроизводимого переноса семантических ролей в проект использована официальная библиотека Google [Material Color Utilities](https://github.com/material-foundation/material-color-utilities), версия 0.3.0, схема `SchemeTonalSpot`, `contrastLevel = 0`. Карта ролей сохранена в [skyway-theme.json](skyway-theme.json). Это сериализация палитры библиотекой, а не исходный ZIP-экспорт веб-конструктора.

Фиксированная светлая схема: `primary = #415F91`, тёмная: `primary = #AAC7FF`. Значения находятся в `ui/theme/Color.kt`, а соответствие ролям Compose — в `ui/theme/Theme.kt`. Размеры и веса системного шрифта настроены в `Typography.kt` под иерархию экрана.

На Android 12+ приложение по умолчанию выбирает палитру обоев устройства. Чтобы увидеть именно фирменную схему, используйте Compose Preview: там передаётся `dynamicColor = false`. Устройства API 26–30 автоматически используют эту фиксированную схему.
