# Soul-Link Mod for Minecraft Forge 1.20.1

A multiplayer mod that links the health, hunger, and knockback of all players together. True cooperation required!

## Features

### 🔗 Health Linking
- When one player takes damage, **all players** take that damage
- When one player heals (from eating, potions, regeneration, etc.), **all players** heal
- Configurable damage/healing multipliers

### 💥 Knockback Linking  
- When one player receives knockback, **all players** receive the same knockback
- Experience the chaos together!

### 🍖 Hunger Linking
- When one player loses hunger, **all players** lose hunger
- When one player eats and gains hunger/saturation, **all players** benefit
- Encourages team resource management

### ☠️ Optional Shared Death
- If enabled, when one player dies, **all players** die
- For the ultimate hardcore experience!

## Configuration

The mod creates a config file at `config/soullink-common.toml` with the following options:

### General Settings
- `minPlayersForLink` - Minimum players required for linking (default: 2)
- `showLinkMessages` - Show action bar messages for link events (default: true)
- `preventPvPLoop` - Prevent infinite damage loops in PvP (default: true)

### Damage Settings
- `linkDamage` - Enable damage linking (default: true)
- `damageMultiplier` - Multiplier for linked damage (default: 1.0)
- `shareDeath` - If one dies, all die (default: false)

### Healing Settings
- `linkHealing` - Enable healing linking (default: true)
- `healingMultiplier` - Multiplier for linked healing (default: 1.0)

### Knockback Settings
- `linkKnockback` - Enable knockback linking (default: true)
- `knockbackMultiplier` - Multiplier for linked knockback (default: 1.0)

### Hunger Settings
- `linkHunger` - Enable hunger (food level) linking (default: true)
- `linkSaturation` - Enable saturation linking (default: true)
- `hungerMultiplier` - Multiplier for linked hunger changes (default: 1.0)

## Commands

All commands require operator permissions (level 2+):

- `/soullink status` - Show current Soul-Link settings and player count
- `/soullink sync` - Synchronize all player health/hunger to averages
- `/soullink damage <true|false>` - Toggle damage linking
- `/soullink healing <true|false>` - Toggle healing linking
- `/soullink knockback <true|false>` - Toggle knockback linking
- `/soullink hunger <true|false>` - Toggle hunger linking
- `/soullink help` - Show command help

## Installation

1. Install Minecraft Forge 1.20.1 (version 47.2.0 or later)
2. Download the Soul-Link mod JAR file
3. Place the JAR in your `mods` folder
4. Launch Minecraft with the Forge profile

## Building from Source

1. Clone this repository
2. Open a terminal in the project folder
3. Run: `./gradlew build`
4. Find the JAR in `build/libs/`

### Development

To set up your development environment:

```bash
# Generate IDE files for IntelliJ IDEA
./gradlew genIntellijRuns

# Generate IDE files for Eclipse
./gradlew genEclipseRuns

# Run the client
./gradlew runClient

# Run the server
./gradlew runServer
```

## Technical Details

### How It Works

1. **Damage Events**: Uses `LivingDamageEvent` to detect when a player takes damage, then applies the same damage to all other online players.

2. **Healing Events**: Uses `LivingHealEvent` to detect healing and propagate it to all players.

3. **Knockback Events**: Uses `LivingKnockBackEvent` to capture knockback and sends network packets to apply the same knockback to other players.

4. **Hunger Tracking**: Monitors player food levels every tick and synchronizes changes across all players.

### Loop Prevention

The mod includes multiple safeguards to prevent infinite loops:
- Processing flags to track which players are currently being updated
- Cooldown timers for damage events
- PvP loop prevention option
- Thread-safe concurrent data structures

### Network Packets

The mod uses custom network packets to sync:
- `KnockbackPacket` - Syncs knockback motion to clients
- `SyncHealthPacket` - Ensures client health display is correct
- `SyncHungerPacket` - Syncs food level and saturation to clients

## License

MIT License - Feel free to use, modify, and distribute!

## Credits

Developed by JellyCreative

---

**Warning**: This mod can make Minecraft significantly more challenging in multiplayer! Coordinate with your team and be prepared for shared suffering (and shared victory)!
