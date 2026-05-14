# VRPhys

VRPhys — полноценный мод для NeoForge `1.21.1`, который связывает Vivecraft VR-ввод с физическими конструкциями Sable.

Цель v1: дать VR-игроку возможность брать рукой Sable sub-level, тянуть его физической spring-силой и бросать при отпускании. Сервер остаётся авторитетным: клиент отправляет только позу руки и состояние удержания, а цель захвата, дистанции, VR-статус и физический импульс проверяются на сервере.

## Что подключено

- Sable NeoForge `1.21.1-1.2.2` — обязательный runtime backend.
- Sable Companion `1.6.0` — встроен через JarJar для безопасных helper’ов координат и поиска sub-level.
- Vivecraft — optional: без него мод запускается, но VR-захват не активируется.
- NeoForge networking — пакеты `GrabStartC2S`, `GrabHoldC2S`, `GrabReleaseC2S`, `GrabAckS2C`.
- Серверная физика — `GrabSession` на `player UUID + hand`, две руки на одной конструкции работают как две отдельные точки тяги.

## Механика v1

- Радиус ладони для поиска конструкции: `0.35` блока.
- При удержании сервер применяет spring-damper impulse в локальной точке Sable sub-level.
- При отпускании скорость руки берётся из истории pose-пакетов, масштабируется и ограничивается clamp’ом.
- Некорректные состояния очищаются: выход игрока, смерть, смена измерения, остановка сервера, unloaded/removed sub-level.

## Настройки

- `grabRadius = 0.35`
- `springStiffness = 1800`
- `springDamping = 90`
- `maxForcePerHand = 4500`
- `throwVelocityScale = 1.0`
- `maxThrowVelocity = 18.0`
- `debugGrabLogging = false`

## Сборка

Локальная сборка не требуется. Основной путь — пуш в GitHub:

```bash
git push
```

После пуша GitHub Actions выполнит:

```bash
./gradlew --no-daemon build
```

Готовый jar будет доступен в artifacts workflow `Build`.
