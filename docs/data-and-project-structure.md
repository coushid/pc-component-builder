# Данные и структура проекта

## Назначение документа

Документ описывает локальный каталог комплектующих, структуру исходников и
поток данных в Android-приложении PCcomponent. Приложение работает полностью
офлайн и не использует сервер, базу данных или внешние API.

## Каталог комплектующих

Каталог хранится в `app/src/main/assets/catalog/components.json` и читается
через `AssetManager` в `ComponentRepository`. Файл содержит 309 демонстрационных
комплектующих в 8 категориях:

- `CPU` - процессоры, 44 позиции;
- `MOTHERBOARD` - материнские платы, 34 позиции;
- `GPU` - видеокарты, 44 позиции;
- `RAM` - оперативная память, 34 позиции;
- `STORAGE` - накопители, 35 позиций;
- `POWER_SUPPLY` - блоки питания, 37 позиций;
- `PC_CASE` - корпуса, 35 позиций;
- `COOLER` - охлаждение, 46 позиций.

### Корневая структура JSON

- `version` - версия формата каталога;
- `source` - описание источника данных;
- `components` - массив комплектующих.

### Поля комплектующего

- `id` - строковый идентификатор;
- `category` - одна из категорий `ComponentCategory`;
- `name` - название модели;
- `brand` - производитель;
- `price` - демонстрационная цена в рублях;
- `performanceLevel` - уровень производительности от 1 до 5;
- `powerConsumptionW` - потребляемая мощность в ваттах;
- `description` - краткое описание;
- `tags` - список тегов для поиска и фильтрации;
- `specifications` - объект с техническими характеристиками.

Объект `specifications` содержит только те поля, которые нужны правилам
совместимости и подбора: сокет, чипсет, форм-фактор, тип и объём памяти,
частоту, количество ядер и потоков, объём видеопамяти, длину и высоту,
интерфейс, объём накопителя, мощность блока питания, сертификат, поддерживаемые
сокеты, TDP, максимальную длину видеокарты, максимальную высоту кулера,
максимальный объём памяти, количество слотов M.2, поколение PCIe и разъёмы
питания видеокарты. Неиспользуемые поля допускается не указывать.

## Структура исходников

```text
app/src/main/
  AndroidManifest.xml
  assets/
    catalog/components.json
    licenses/RussoOne-OFL.txt
  java/com/example/pccomponent/
    MainActivity.kt
    data/
      model/ComponentModels.kt
      repository/ComponentRepository.kt
      storage/SavedConfigurationStore.kt
    domain/
      calculator/PowerCalculator.kt
      chatbot/ChatBotState.kt
      compatibility/CompatibilityChecker.kt
      feasibility/BuildFeasibilityEngine.kt
      questionnaire/QuestionnaireModels.kt
      recommendation/BuildRecommendationEngine.kt
      recommendation/BuildSolver.kt
    performance/PerformanceTrace.kt
    presentation/
      PcComponentViewModel.kt
      QuestionnaireViewModel.kt
    ui/
      PcComponentApp.kt
      HomeScreen.kt
      QuestionnaireScreen.kt
      CatalogScreen.kt
      BuildScreen.kt
      ResultScreen.kt
      SavedScreen.kt
      AboutScreen.kt
      UiSupport.kt
      components/ComponentCard.kt
      components/FilterFields.kt
      theme/Color.kt
      theme/SemanticText.kt
      theme/Type.kt
  keepRules/rules.keep
  res/
    drawable/
    font/
    mipmap-*/
    values/
    values-night/
    values-v31/
    xml/
```

Файлы экранов лежат в папке `ui`, но объявляют пакет
`com.example.pccomponent`. Это сделано для совместимости с существующей
навигацией и точкой входа `MainActivity`.

## Поток данных

1. `MainActivity` запускает Compose-приложение `PcComponentApp`.
2. `ComponentRepository` один раз читает JSON из assets и кэширует каталог,
   индексы по категории, сокету, цене и производительности.
3. `QuestionnaireViewModel` проверяет доступность вариантов ответа через
   `BuildFeasibilityEngine` и запускает подбор через `BuildRecommendationEngine`.
4. `CompatibilityChecker` проверяет совместимость выбранных комплектующих.
5. `PcComponentViewModel` хранит текущую сборку и сохранённые конфигурации.
6. `SavedConfigurationStore` сохраняет сборки в `SharedPreferences` в виде
   сериализованных записей.

## Правила совместимости

Проверяются сокет процессора и материнской платы, тип и максимальный объём
оперативной памяти, поддержка сокета кулером, TDP процессора и возможности
охлаждения, форм-фактор материнской платы и корпуса, длина видеокарты, высота
кулера, мощность блока питания с запасом и разъёмы питания видеокарты.

## Добавление комплектующих

1. Добавьте объект в массив `components`.
2. Укажите уникальный `id`.
3. Проверьте допустимость значения `category`.
4. Заполните обязательные поля и нужные поля `specifications`.
5. Убедитесь, что JSON остаётся валидным и имеет кодировку UTF-8.
6. Запустите модульные тесты, чтобы проверить совместимость и подбор.

## Хранение данных

История и сохранённые сборки не покидают устройство. Приложение не запрашивает
разрешение на интернет и не передаёт данные третьим лицам.
