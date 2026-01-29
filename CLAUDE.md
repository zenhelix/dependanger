# CLAUDE.md

Проект документации для Dependanger — инструмента управления зависимостями.

## Структура

```
docs/
├── 00-overview/       # Обзор проекта
├── 01-concepts/       # Концепции (ЧТО такое)
├── 02-features/       # Функциональные требования (ЧТО делает)
├── 03-interfaces/     # Интерфейсы (КАК пользоваться)
├── 04-appendix/       # Приложения (NFR, справочные материалы)
├── 05-decisions/      # ADR
├── 06-reference/      # Справочник (КАК выглядит — DSL syntax, Model schema)
└── _templates/        # Шаблоны
```

## Команды документации

| Команда                        | Описание                                            |
|--------------------------------|-----------------------------------------------------|
| `/doc-create [тип] [название]` | Создать документ (feature, adr, concept, interface) |
| `/doc-check [путь]`            | Проверить полноту → STOP / ITERATE / WAIT           |
| `/doc-iterate [путь]`          | Перейти на следующий уровень (L0→L1→L2→L3)          |
| `/doc-polish [путь]`           | Финальная редактура                                 |
| `/doc-status`                  | Статус всей документации                            |

**Пакетный режим:** команды `doc-check`, `doc-iterate`, `doc-polish` принимают путь к папке — обрабатывают все `.adoc` файлы по порядку.

```bash
/doc-check docs/02-features/     # проверить все features
/doc-iterate docs/               # итерировать весь docs
/doc-polish docs/01-concepts/    # редактура всех concepts
```

## Ключевые правила

- **Формат:** AsciiDoc (`.adoc`)
- **Диаграммы:** Mermaid
- **DRY:** Не повторять — использовать `xref:` и `<<anchor>>`
- **AC:** В формате Given/When/Then
- **После изменений:** Проверить связанные документы (каскадная проверка)
- **Якоря:** Использовать явные ASCII якоря (`[[anchor-name]]`) вместо авто-генерируемых из кириллических заголовков. Пример:
  `[[system-context]]` вместо `#_системный_контекст`
- **Блоки кода:** AsciiDoc-синтаксис `[source,lang]` + `----`, НЕ markdown ```
- **Плейсхолдеры:** Экранировать `\{name\}` в тексте/таблицах (внутри `[source]` не нужно)
- **Подробные правила форматирования:** см. `.claude/skills/documentation-agent.md`

## Уровни зрелости

| Уровень | Название          | Когда достигнут            |
|---------|-------------------|----------------------------|
| L0      | Draft             | Есть идея                  |
| L1      | Minimal Viable    | Цель + user stories        |
| L2      | Development Ready | AC + edge cases            |
| L3      | Complete          | Всё testable, вопросов нет |

## Подробная документация

- Правила полноты: `docs/_templates/documentation-completeness.adoc`
- Workflow: `docs/_templates/documentation-workflow.adoc`
- Шаблоны: `docs/_templates/`
