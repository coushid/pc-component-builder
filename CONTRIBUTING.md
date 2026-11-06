# Contributing

Спасибо за интерес к проекту. Ниже короткие правила, которые помогут
поддерживать репозиторий в рабочем состоянии.

## Как предложить изменение

1. Создайте fork репозитория и отдельную ветку.
2. Внесите изменение небольшими логическими коммитами.
3. Проверьте сборку и модульные тесты локально.
4. Откройте pull request с описанием изменения и шагов проверки.

## Локальная проверка

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

В Windows используйте `gradlew.bat` с теми же задачами.

## Стиль кода

- Kotlin code style: official.
- Не добавляйте в репозиторий `local.properties`, ключи, keystore и другие секреты.
- Новые правила совместимости и подбора должны сопровождаться модульными тестами.
- Сохраняйте офлайн-характер приложения: без сетевых вызовов и внешних API.

## Каталог комплектующих

Файл `app/src/main/assets/catalog/components.json` содержит демонстрационный
каталог. При изменении данных проверьте, что категории, обязательные поля и
идентификаторы остаются корректными, а тесты проходят.

## Лицензия

Отправляя изменение, вы соглашаетесь с тем, что оно распространяется по
лицензии MIT, указанной в файле `LICENSE`.

# Contributing in English

Thanks for your interest in the project.

1. Fork the repository and create a feature branch.
2. Keep changes focused and commits readable.
3. Run unit tests and a debug build before opening a pull request.
4. Describe the change and the verification steps in the pull request.

Useful commands:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Do not commit `local.properties`, keys, keystores, or any other secrets.
Compatibility and recommendation changes should include unit tests.
By submitting a change you agree that it is distributed under the MIT License.
