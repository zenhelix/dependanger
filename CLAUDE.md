# CLAUDE.md

Это проект документации для Dependanger — инструмента управления зависимостями.

## Структура проекта

```
docs/
├── 00-overview/       # Обзор проекта
├── 01-concepts/       # Концептуальные документы
├── 02-features/       # Функциональные требования (F###)
├── 03-interfaces/     # Интерфейсы (CLI, API)
├── 04-appendix/       # Приложения
├── 05-decisions/      # ADR (Architecture Decision Records)
└── _templates/        # Шаблоны и руководства
```

## Работа с документацией

При работе с файлами в `docs/` используй Documentation Agent.

### Доступные команды

| Команда                        | Описание                                                             |
|--------------------------------|----------------------------------------------------------------------|
| `/doc-check [путь]`            | Проверить полноту документа, получить рекомендацию STOP/ITERATE/WAIT |
| `/doc-create [тип] [название]` | Создать новый документ (feature, adr, concept, interface)            |
| `/doc-iterate [путь]`          | Помочь с переходом на следующий уровень зрелости                     |
| `/doc-status`                  | Показать статус всей документации проекта                            |

### Примеры использования

```
/doc-check docs/02-features/F001-registry-dsl.adoc
/doc-create feature metadata-caching
/doc-iterate docs/02-features/F007-update-check.adoc
/doc-status
```

### Уровни зрелости документов

- **L0 Draft** — идея есть, структура неполная
- **L1 Minimal Viable** — цель ясна, основные user stories есть
- **L2 Development Ready** — AC есть, edge cases описаны
- **L3 Complete** — всё тестируемо, все вопросы отвечены

### Когда использовать агента

- При создании новых документов — `/doc-create`
- При вопросе "достаточно ли описано?" — `/doc-check`
- При доработке документа — `/doc-iterate`
- Для обзора состояния документации — `/doc-status`

## Правила документации

- Шаблоны находятся в `docs/_templates/`
- Правила полноты: `docs/_templates/documentation-completeness.adoc`
- Workflow: `docs/_templates/documentation-workflow.adoc`
- Acceptance Criteria должны быть в формате Given/When/Then
- Используй `xref:` для ссылок между документами (DRY)

## Формат документов

Все документы в формате AsciiDoc (`.adoc`). При создании новых документов всегда использовать соответствующий шаблон из `_templates/`.
