# Build verification — Kotora Cat 1.1.1

Проверено локально:
- структура ForgeGradle-проекта;
- JSON-файлы;
- PNG-текстура 64x32;
- TOML `mods.toml`;
- баланс скобок во всех Java-файлах;
- renderer зарегистрирован как `MobRenderer<KotoraCatEntity, CatModel<KotoraCatEntity>>`;
- сохранение состояния ярости в NBT;
- GitHub Actions использует JDK 17 и Gradle 8.1.1.

Полную Gradle-компиляцию в среде подготовки нельзя выполнить без загрузки ForgeGradle/Minecraft-зависимостей из Maven. Поэтому этот архив не следует считать бинарно подтверждённым JAR, пока GitHub Actions или локальный `gradle build` не завершится успешно.
