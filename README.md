<p align="center">
  <img src="https://raw.githubusercontent.com/DevMagu/MagusElites/main/branding/logo-512.png" alt="Magu's Elites" width="200">
</p>

<h1 align="center">Magu's Elites</h1>

<p align="center">
  <b>Monsters that grow stronger every few days, and elites with combat powers and their own loot.</b><br>
  NeoForge 1.21.1 · <b>Server-side only</b>: players join with a vanilla client.
</p>

---

## What it does

- **Every monster levels up** as Minecraft days go by: more health and damage the longer your world lives.
- **Some monsters spawn as elites**, in three tiers: **Strong**, **Elite** and **Legendary**.
- **Elites have combat powers**: poison, fire, freezing, life steal, teleporting, summoning help...
  Their name tells you what you're facing: `[Legendary] Venomous Blinking Zombie [Lv. 7]`.
- **Elites drop their own loot**: resources and a chance of enchanted gear, better for higher tiers.
- **Colored particles** around elites, and Legendaries **glow** when you get close (and don't burn in daylight).
- **Safe zone** around the world spawn, so new players don't meet a Legendary on their first night.
- **Fully configurable**, and works with **monsters from other mods**.

## Tiers

About **6 %** of naturally spawning monsters become elites. Of those:

| Tier | How often | Health | Damage | Speed | Powers | Gear drop chance |
|---|---|---|---|---|---|---|
| **Strong** | 70 % | +50 % | +25 % | +10 % | 0 | 20 % (iron) |
| **Elite** | 25 % | +100 % | +50 % | +20 % | 1 | 50 % (diamond) |
| **Legendary** | 5 % | +200 % | +100 % | +30 % | 2 – 3 | 95 % (diamond / netherite) |

Legendaries also glow for players within 20 blocks and don't burn in daylight.

## Combat powers

| Power | What it does |
|---|---|
| **Venomous** | Its hits poison you for 5 s (Poison II from Legendaries). |
| **Blazing** | Its hits set you on fire for 4 s. |
| **Freezing** | Its hits slow you down (Slowness II + Mining Fatigue) for 4 s. |
| **Hurling** | Its hits launch you through the air. |
| **Vampiric** | Heals half of the damage it deals. |
| **Thorned** | Hitting it hurts you back: it reflects 30 % of the damage. |
| **Blinking** | If you run more than 10 blocks away, it teleports next to you. |
| **Summoning** | When a player hits it, it may call 1–2 monsters of its kind for help. |

Attack powers also work with arrows. Every power can be turned off in the config.

## Levels

Every monster (elite or not) gets a level when it spawns: **level 1 + one level every 3 days**, up to **level 20**.
Each level adds **+10 % health** and **+5 % damage**; the elite bonus multiplies on top of that.
Normal monsters show their level when you look at them; elites always show their name.

> [!WARNING]
> Levels use the **world clock**: sleeping skips to the next day and counts.
> **`/time set` moves the clock back** (`/time set day` = day 0 = level 1 again). To change the time without
> resetting the levels, use **`/time add`** instead.

## Loot

Elites drop extra loot **only when killed by a player** (no elite farms): resources such as diamonds, emeralds,
golden apples and netherite scrap, plus a chance of **enchanted gear** with bonus health or damage.
Higher levels give a little extra loot.

The loot tables are normal datapack files (`magus_elites:elites/strong`, `.../elite`, `.../legendary`), so you can
replace them with your own.

## Safe zone

No elites spawn naturally within **128 blocks** of the world spawn (normal monsters still get their level).
In the Nether every block counts as 8, like portals; in the End it's measured from the center island.

## Commands

| Command | Who | What it does |
|---|---|---|
| `/elite level` | everyone | Current day, monster level and days until the next level (and whether you're in the safe zone). |
| `/elite info` | everyone | Tier, level, health and powers of the monster you're looking at. |
| `/elite spawn <tier> <mob> [powers...]` | operators | Spawns an elite next to you, e.g. `/elite spawn legendary zombie venomous blazing`. Without powers, they're random. |

`/summon` also creates a random elite, so you can test it. Note: `/summon` **with NBT data** (`{...}`) skips the
spawn process in vanilla and gives a normal monster.

## Configuration

Everything is in `config/magus_elites-common.toml` (created on first launch, with comments):

- `[elites]` – elite chance, safe zone radius, and for each tier: weight, health, damage, speed, min/max powers.
- `[levels]` – days per level, max level, health and damage per level.
- `[legendary]` – glow distance (0 = no glow), whether Legendaries ignore daylight.
- `[effects]` – particles on/off.
- `[powers]` – turn each combat power on or off.

## Datapacks and other mods

Every hostile monster is affected by default, including monsters from other mods.
Two entity type tags let you change that with a datapack:

- `magus_elites:blacklist` – never touched by the mod (no level, no elite). By default: bosses (`#c:bosses`),
  the Elder Guardian and the Warden.
- `magus_elites:whitelist` – affected even if they're not hostile.

Example: `data/magus_elites/tags/entity_type/blacklist.json`
```json
{
  "replace": false,
  "values": ["minecraft:creeper"]
}
```

## Installation

- **Server**: put the jar in the server's `mods` folder. Players **don't** need it: vanilla clients can join.
- **Singleplayer**: put the jar in your `mods` folder like any other mod.

Requires **Minecraft 1.21.1** and **NeoForge 21.1.265** or newer.

Only naturally spawning monsters (and `/summon`) can become elites: spawners and spawn eggs never create them.

## Bugs and ideas

Found a bug or have an idea? Open an issue on [GitHub](https://github.com/DevMagu/MagusElites/issues).

## License

[MIT](LICENSE): you can use it in your modpacks and servers.
