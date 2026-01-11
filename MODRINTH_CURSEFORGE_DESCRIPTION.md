# 🔗 Soul-Link

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.20.1-green?style=for-the-badge&logo=minecraft" alt="Minecraft 1.20.1">
  <img src="https://img.shields.io/badge/Mod%20Loader-Forge-orange?style=for-the-badge" alt="Forge">
  <img src="https://img.shields.io/badge/Version-1.1.0-blue?style=for-the-badge" alt="Version 1.1.0">
</p>

<p align="center">
  <b>Your souls are bound together. Feel every hit. Share every meal. Survive as one.</b>
</p>

---

# 💀 What is Soul-Link?

**Soul-Link** is a multiplayer mod that creates an unbreakable bond between all players on your server. When one player suffers, everyone suffers. When one player heals, everyone heals. 

This mod transforms Minecraft into the ultimate co-op survival experience where **teamwork isn't just encouraged—it's mandatory.**

---

# ✨ Features

## ❤️ Shared Health
> *"I felt that..."*

When **Player A** takes damage from a skeleton's arrow, **Player B** mining in a cave feels that same damage. Every heart matters when it's everyone's heart.

- ✅ All damage types synced (mobs, fall damage, fire, etc.)
- ✅ Configurable damage multiplier
- ✅ Optional shared death (if one dies, all die!)

&nbsp;

## 💚 Shared Healing
> *"Thanks for eating that steak!"*

Regeneration works both ways! When one player heals from any source, all linked players receive the same healing.

- ✅ Natural regeneration synced
- ✅ Potion effects synced
- ✅ Golden apple healing shared
- ✅ Configurable healing multiplier

&nbsp;

## 💨 Shared Knockback
> *"Why am I flying?!"*

Get hit by a zombie? Your friends across the world will stumble too. The chaos is real.

- ✅ Knockback direction and force synced
- ✅ Shield blocking prevents sync (smart detection!)
- ✅ Configurable knockback multiplier

&nbsp;

## 🍖 Shared Hunger
> *"Stop sprinting, you're making us all hungry!"*

Your hunger bar is everyone's hunger bar. Coordinate your eating, or starve together.

- ✅ Hunger loss synced
- ✅ Hunger restoration synced
- ✅ Saturation synced
- ✅ Configurable hunger multiplier

&nbsp;

## 🎒 Shared Inventory
> *"Who took my diamonds?!"*

All players share the same inventory. Every item picked up, every tool crafted—it all goes into the shared pool.

- ✅ Full inventory sync (hotbar, main inventory, armor, offhand)
- ✅ Saved with world data
- ✅ First player's inventory becomes the shared inventory
- ✅ Keep inventory on death (configurable)

---

# ⚙️ Configuration

All features can be toggled and customized in `config/soullink-common.toml`:

| Setting | Default | Description |
|---------|---------|-------------|
| `linkDamage` | `true` | Sync damage between players |
| `linkHealing` | `true` | Sync healing between players |
| `linkKnockback` | `true` | Sync knockback between players |
| `linkHunger` | `true` | Sync hunger between players |
| `linkInventory` | `true` | Share inventory between players |
| `damageMultiplier` | `1.0` | Multiplier for synced damage |
| `healingMultiplier` | `1.0` | Multiplier for synced healing |
| `knockbackMultiplier` | `1.0` | Multiplier for synced knockback |
| `hungerMultiplier` | `1.0` | Multiplier for synced hunger |
| `shareDeath` | `false` | If one player dies, all players die |
| `minPlayersForLink` | `2` | Minimum players for link to activate |
| `keepInventoryOnDeath` | `true` | Preserve shared inventory on death |

---

# 🎮 Commands

All commands require operator permissions (OP level 2).

| Command | Description |
|---------|-------------|
| `/soullink status` | View current settings and player count |
| `/soullink sync` | Force sync all player vitals |
| `/soullink inventory sync` | Force sync shared inventory |
| `/soullink inventory reset` | Clear the shared inventory |
| `/soullink inventory copyfrom <player>` | Copy a player's inventory to shared |
| `/soullink help` | Show all commands |

---

# 🎯 Perfect For

- 🏆 **Hardcore Co-op Challenges** - True teamwork or total failure
- 🎬 **Content Creators** - Hilarious moments guaranteed  
- 👨‍👩‍👧‍👦 **Friend Groups** - Bond like never before (or blame each other!)
- 🏃 **Speedrun Challenges** - New category: Soul-Linked Any%
- 🎲 **Custom Modpacks** - Adds unique multiplayer dynamics

---

# 📥 Installation

1. Install **Minecraft Forge 1.20.1** (47.2.0 or later)
2. Download **Soul-Link** from this page
3. Place the `.jar` file in your `mods` folder
4. Launch Minecraft and link your souls!

---

# 🔧 Compatibility

- ✅ Works with most other mods
- ✅ Server-side only (clients need it too for full functionality)
- ✅ Supports dedicated servers and LAN worlds
- ⚠️ May conflict with mods that heavily modify player health/inventory systems

---

# 📜 License

This mod is released under the **MIT License**. Feel free to include it in modpacks!

---

# 💜 Credits

Developed with ❤️ by **JellyCreative**

---

<p align="center">
  <i>"In Soul-Link, you don't just play together—you live and die together."</i>
</p>
