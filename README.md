# Ring of Time

Ring of Time is a RuneLite Plugin Hub plugin that draws shrinking annular
countdowns for temporary skill buffs, skill debuffs, poison, venom, antipoison,
stamina, and antifire. Every circle is a separate RuneLite overlay, so it can be
dragged and saved independently instead of belonging to one large grid component.
Untouched active circles receive a simple vertical starting layout.

## Skill-level rings

For temporary skill changes, the thick outer ring represents the whole level
difference when Ring of Time first sees it. It shrinks smoothly through every
ordinary one-level decay or recovery cycle and disappears when the skill returns
to its real level.

For a change larger than one level, the optional thin configurable inner ring counts
down to the next one-level change and then restarts. It shows the next buff drop
for a multi-level buff and the next recovery for a multi-level debuff. Buff and
debuff inner rings can be switched on or off separately. At `+1` or `-1`, the
outer ring already represents the next and final change, so a duplicate inner
ring is not drawn.

For example, a Strength change from 99 to 111 starts with a full outer ring and
a full inner ring. After one level decays, the inner ring restarts while the
outer ring continues with roughly eleven-twelfths of its original total effect
remaining. This continues until Strength returns to 99.

RuneScape does not expose the current phase of the shared stat-change clock when
a plugin first starts or first notices an effect. Ring of Time immediately shows
its best standard-cycle projection without an approximation prefix. After the first
natural one-level decay or recovery, the ring synchronizes to the observed game
tick. Preserve's extended buff cycle is included: active buff countdown text turns
cyan while Preserve is enabled and returns to white when Preserve is disabled.

## Poison, venom, antipoison, stamina, and antifire rings

- **Poison** uses a green outer ring for total time until natural cure. Its badge
  shows the next poison damage. An optional inner ring shows the next damage
  cycle and restarts after each poison hit.
- **Venom** uses a dark-green next-damage ring over a complete poison-green
  track. Venom does not naturally expire or turn into poison, so displaying a
  false total-expiry countdown would be misleading. If the game's poison state
  actually converts venom to poison, the same independently positioned circle
  changes from dark green to the green poison natural-cure timer.
- **Antipoison protection** uses one independent circle for both ordinary
  antipoison and anti-venom immunity. It reads the negative protection values
  from the shared poison variable, switches between editable light/dark green
  colors and matching potion icons, and counts down the full protection period.
- **Stamina** uses a brown ring and the game's stamina active/duration variables.
  The ring therefore supports ordinary, extended, and activity-specific stamina
  effects that publish those standard variables.
- **Antifire** uses light-purple rings. Regular/extended antifire and
  super/extended-super antifire have separate draggable circles because the
  game publishes separate timer variables for the two protection strengths.

Antipoison, stamina, and antifire game variables update in coarse blocks.
Ring of Time anchors each block to the event where the value changes and smoothly
animates the time between changes. An effect discovered during login begins with
the best available projection and does not show an approximation prefix.

## Exact supported coverage

Ring of Time supports ordinary one-level buff decay and debuff recovery for
these 22 skills:

- Combat: Attack, Strength, Defence, Ranged, and Magic.
- Non-combat: Mining, Agility, Smithing, Herblore, Fishing, Thieving, Cooking,
  Crafting, Firemaking, Fletching, Woodcutting, Runecraft, Slayer, Farming,
  Construction, Hunter, and Sailing.

For every skill above, it works with any source that changes the player's
visible boosted skill level and then uses the ordinary shared one-level
restoration clock. Examples include ordinary combat and skilling potions, spicy
stew boosts/drains, Dragon battleaxe stat changes, Saradomin brew combat-stat
changes, and ordinary stat-draining attacks.

The supported non-skill effects are:

- Active poison: next damage, next-damage cycle, and projected natural cure.
- Active venom: next damage and repeating next-damage cycle, plus real
  venom-to-poison state transitions.
- Ordinary antipoison protection duration.
- Anti-venom protection duration.
- Stamina effect duration.
- Regular and extended antifire duration.
- Super and extended-super antifire duration.

The following remain outside the current release:

- Hitpoints and Prayer restoration.
- Fixed-duration or repeatedly refreshed stat boosts such as divine potions,
  overload-style effects, and smelling salts.
- Run energy itself, teleblock, freezes, vengeance, thralls, spell cooldowns,
  and boss or raid mechanics.
- NPC and opponent stats.

## Configuration

The settings are ordered into focused sections:

- **Duplicate timers** contains static dash-prefixed reminders for overlapping
  RuneLite displays. It never changes another plugin's settings.
- **Labels & icons** contains timer position, icon position, separate timer and
  +/- font sizes, and the adjustable +/- background.
- **Ring style** contains ring size, thickness, ring outlines, ring-outline
  thickness, and **Ring background color**.
- **Buffs** contains **Shown buffs**, the next-change ring, +/- display options,
  shared-color controls, and expiry flashing.
- **Individual buff colors** contains the 22 editable skill colors and is
  collapsed by default.
- **Debuffs** contains **Shown debuffs**, skill-drain settings, and poison/venom
  settings including the next poison-hit ring.
- **Protection & effect timers** contains antipoison, anti-venom, stamina, and
  antifire settings.

**Timer position** supports top, middle, bottom, or off. **Icon position**
supports all four corners, the center of the circle, above the ring, or under
the ring. **Timer font size** and **+/- font size** are separate and both
default to 10 px.

Rings default to 50 px with a 5 px stroke. **Ring outlines** default to off and
their stored **Ring outline thickness** defaults to 1 px. The **+/- background**
defaults to `#32000000` (ARGB: translucent black). All exposed colors and alpha
values remain adjustable.

Timer text has no filled background. Its configured font size is a maximum:
long countdowns automatically shrink until they fit inside the clear area of
the outer ring or visible next-change ring. The +/- badge keeps its selected
anchor when the icon position changes. Icons and labels move outward only when
they would collide. Center-of-circle is available for both the icon and +/-;
if several items request the same space, the stacking logic keeps the +/-
anchor and moves the icon.

**Shown buffs** selects combat, non-combat, or all supported skills and defaults
to **All**. **Shown debuffs** independently selects skill drains, poison/venom,
both groups, or off. The +/- value mode switches between the signed change
(`+5` or `-5`) and total effective level. Buff +/- text defaults to green;
debuff and toxin +/- text defaults to red.

Buffs use themed per-skill colors by default. **Use one buff color** replaces
them with **Shared buff color**. Beneficial circles can flash in their active
color near expiry. **Flash threshold (seconds)** defaults to 6 and supports
values from 1 to 30 seconds.

The default buff palette is Attack red, Strength crimson, Defence steel blue,
Ranged forest green, Magic royal blue, Mining slate, Agility teal, Smithing
orange, Herblore herb green, Fishing cyan, Thieving violet, Cooking warm red,
Crafting tan, Firemaking flame orange, Fletching lime, Woodcutting moss green,
Runecraft purple, Slayer dark violet, Farming bright green, Construction gold,
Hunter khaki, and Sailing ocean blue.

Buff and debuff ring-outline colors default to translucent black and remain
independently adjustable. Poison and venom share the debuff outline color.
Buff next-change rings default to yellow; skill-debuff next-change rings default
to red. The next poison-hit color remains adjustable. Protection, stamina, and
antifire circles share an adjustable yellow effect outline.

Each supported skill or effect has a uniquely named RuneLite overlay. Active
circles receive a gap-free vertical starting layout. Hold Alt and drag any
circle to remove that entity from automatic stacking; RuneLite remembers its
independent position across sessions.

## Develop and test

Requirements:

- Java 11
- IntelliJ IDEA Community Edition (recommended) or another Gradle-capable IDE

Commands on Windows:

```powershell
.\gradlew.bat test
.\gradlew.bat run
```

Commands on macOS/Linux:

```bash
./gradlew test
./gradlew run
```

For a Jagex account, follow RuneLite's
[Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts)
development-client instructions.

## Code map

| File | Responsibility |
| --- | --- |
| `RingOfTimePlugin.java` | RuneLite lifecycle, event subscriptions, game-variable sampling, and creation/removal of every independent overlay. |
| `StatChangeTracker.java` | Pure skill timing model for buffs, drains, shared restoration cycles, and Preserve. |
| `EffectTimerTracker.java` | Pure poison/venom, antipoison/anti-venom protection, stamina, regular-antifire, and super-antifire state/timing model. |
| `SkillTimerOverlay.java` | One independently draggable skill circle with total/next-change rings, crisp configurable text, positioned +/- badge, editable themed color, and expiry flash. |
| `EffectTimerOverlay.java` | One independently draggable effect circle with editable colors, crisp configurable text, expiry flash, relevant icon/badge, and venom's green under-track. |
| `TimerCircleOverlay.java` | Shared automatic-stack and independent-drag behavior for every circle entity. |
| `TimerCircleLayout.java` | Pure vertical starting-position calculation for untouched circles. |
| `TimerLabelLayout.java` | Fits top/middle/bottom countdowns inside the rings, preserves +/- anchors, positions icons, and prevents collisions. |
| `SkillColorPalette.java` | Distinct themed default buff colors for all 22 supported skills. |
| `RingOfTimeConfig.java` | Duplicate-timer guidance plus focused Labels & icons, Ring style, Buffs, Debuffs, and Protection & effect timers sections. |
| `StatChangeTrackerTest.java` | Unit coverage for skill total/next-change progress, synchronization, removal, and Preserve. |
| `EffectTimerTrackerTest.java` | Unit coverage for poison cure, venom transition, both antipoison protection types, stamina smoothing, and both antifire scales. |
| `SkillColorAndLayoutTest.java` | Unit coverage for all 22 colors, vertical placement, timer fitting, stable +/- anchors, icon layout, and debuff-category selection. |
| `RingOfTimePluginTest.java` | Developer-mode RuneLite launcher; it is not shipped as plugin behavior. |
| `build.gradle` | Java 11 build, dependencies, tests, and developer launcher. |
| `runelite-plugin.properties` | Plugin Hub metadata and main-class declaration. |

All non-obvious calculations and lifecycle decisions are documented at the
class, method, and decision points in the Java source so future contributors can
safely change the timing or rendering logic.

## Plugin Hub submission

1. Put this project in the public `laurenszs/Ring-of-Time` GitHub repository
   and keep the BSD-2-Clause license.
2. Run `.\gradlew.bat test` and test every ring in the development client.
3. Push a reviewed commit.
4. Add a two-line marker file containing the repository URL and full commit hash
   to a fork of `runelite/plugin-hub`, then open a pull request.

The plugin performs no input automation, networking, disk access, reflection,
or external process execution.