# ✨ Stardust

Stardust is an addon for [Meteor Client](https://meteorclient.com) designed for use on the
anarchy Minecraft server [2b2t](https://2b2t.org).<br>
It contains original modules to enhance your experience on Old Server, with a focus on configurability and polish.

### Feature Set
#### Commands

| Command                                                  | Description                                                                                                                                                                                                    |
|----------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Stats2b2t, LastSeen2b2t, FirstSeen2b2t, Playtime2b2t** | *Self explanatory commands. Use your Meteor command prefix.* Credit to [rfresh](https://github.com/rfresh2) for the current best [2b2t stats api](https://api.2b2t.vc).                                        |
| **Panorama**                                             | *Takes a panorama screenshot and automatically assembles it into a resource pack for the main menu screen.* Usage: `.panorama [name]`                                                                          |
| **Loadout**                                              | *Save & load inventory configurations.* The current state of your inventory is saved when using `.loadout save name`, and your inventory will be sorted to match if you use `.loadout load name` at any point. |

<br>

#### Modules
| Module               | Description                                                                                                                                                                                                                                           |
|----------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **ChatSigns**        | *Read nearby signs in your chat.* Can also highlight and ESP potentially old signs placed before version 1.8 (January 2015 on 2b2t.)*                                                                                                                 |
| **SignatureSign**    | *An AutoSign module that's actually good.* Fully customizable template mode, & story mode for long input files over multiple signs.                                                                                                                   |
| **SignHistorian**    | *Record and restore broken or modified signs that you previously visited.* Since 1.19, creepers have been a real problem for sign boards everywhere. Now even if they blow that shit up, all is not lost.                                             |
| **RocketMan**        | *Makes flying with fireworks much easier \[bring lots of rockets!\]* RocketMan is a versatile and highly-configurable firework efly module with many useful capabilities.                                                                             |
| **RocketJump**       | *Rocket-boosted jumps (requires an elytra).* Simple but fun mobility module used for gracefully accessing out-of-reach areas, or as an easy way to prime a density mace...                                                                            |
| **RapidFire**        | *Automatically fires every crossbow in your hotbar or inventory spaced by a configurable delay.* Also capable of automatically reloading and auto-firing crossbows.                                                                                   |
| **StashBrander**     | *Automatically rename desired items in bulk when using anvils.*                                                                                                                                                                                       |
| **AutoDoors**        | *Automatically interacts with doors.* Includes an insufferable door spammer mode as well (complete with client-side mute option).                                                                                                                     |
| **AutoSmith**        | *Automatically upgrades gear or trims armor sets when interacting with smithing tables.*                                                                                                                                                              |
| **AntiToS**          | *Censors player-generated text on render, like signs, books, chat, and more, according to a customizable content blacklist.*                                                                                                                          |
| **Archaeology**      | *ESP / Notifications for archaeological dig sites, & automatic brushing of targeted suspicious blocks.* Also prevents accidental breakage of suspicious blocks.                                                                                       |
| **Loadouts**         | *Save & load inventory configurations.* Adds quicksave & load buttons to your inventory screen, and handles sorting your inventory when a config is loaded with the Loadout command.                                                                  |
| **LoreLocator**      | *Slot highlighter for rare, unique, and anomalous items.* Capable of highlighting renamed items, items with illegal enchants, zero durability items, and more.                                                                                        |
| **AxolotlTools**     | *Variant ESP, auto-collector & auto-breeder for axolotls.* Can also catch buckets of tropical fish, with or without a farm setup. I used this to get blue axolotls on 2b2t because interacting with them normally pisses me off.                      |
| **AutoDrawDistance** | *Automatically adjusts your render distance to maintain an FPS target.* Some biomes/areas can drop my fps by half, so I occasionally find this useful.                                                                                                |
| **AutoDyeShulkers**  | *Automatically dye shulker boxes a desired color in crafting grids.*                                                                                                                                                                                  |
| **BannerData**       | *Right-click banners to display their pattern and color data.* Looks cool, gives you the base color without any fuss. Can also copy the raw nbt to your clipboard.                                                                                    |
| **BookTools**        | *Enhancements for working with books.* Adds buttons for inserting color & formatting codes into writable books, and adds a deobfuscation button to written books containing obfuscated/magic text.                                                    |
| **PagePirate**       | *Pirates books held by other players* (displays their contents in your chat, and optionally makes a local copy using a book & quill). Also allows you to highlight and view the contents of books in item frames, and dropped on the ground as items. |
| **TreasureESP**      | *An ESP module for buried treasure chests.* Finding buried treasure is the only way of obtaining Hearts of the Sea, and Conduits by extension.                                                                                                        |
| **MusicTweaks**      | *Lets you tweak various things relating to the background music.* Change the pitch, volume, or cooldown between songs, or even choose and view which soundtracks play during your session.                                                            |
| **Honker**           | *Automatically uses goat horns when a player enters your render distance.* You can select your preferred horn type, or choose random for a surprise pick from your inventory each time.                                                               |
| **Updraft**          | *Automatically enhances your jumps with wind charges.* Wind charges seem to be nerfed on Paper compared to Vanilla, but maybe someone would still find this fun or useful.                                                                            |
| **WaxAura**          | *Automatically waxes signs within your reach.* On 2b2t, sign editing has been disabled "for now™". This module makes it easy to wax as many signs as possible before it gets enabled.                                                                 |

>*OldSigns: <br>
Ever since 2b2t updated to 1.19, the NBT tag artifact that separates pre-1.8 from post-1.8 signs is missing on newly-placed signs.<br>
This means that false positives are now *unavoidable*, but should be limited to *oak signs placed after 1.19 in old (pre-1.8) chunks, which don't contain dates after 2015*.<br>
Use your best judgement to further identify false positives or fakes.<br>
OldSigns functionality can be accessed in the ChatSigns module settings. 

<br>

#### Miscellaneous

| Feature                       | Description                                                                                                                                                                                                                                                                                                                         |
|-------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Illegal Disconnect**        | *Adds "Illegal Disconnect" functionality to Meteor's built-in AutoLog module.* Enable this in the AutoLog module's settings. Also adds a config setting to put a dedicated Illegal Disconnect button on the pause menu.                                                                                                             |
| **Click-to-Come**             | *When using Meteor's Freecam module, click on a block or entity to Baritone path there.* Enable this in the Freecam module's settings.                                                                                                                                                                                              |
| **Greeter**                   | *Adds Greeter-style join/leave notifications to Meteor's built-in Notifier module.*                                                                                                                                                                                                                                                 |
| **Elytra Mending**            | *Tweaks Meteor's built-in AutoMend module to allow automatically equipping damaged elytras for mending.* This can be used in conjunction with offhand mending, or exclusively by enabling the "ignore offhand" setting.                                                                                                             |
| **Title Screen Tweaks**       | *2b2t-themed changes to the title screen.* Adds config settings to make the splash texts green, to cycle them randomly, and to add a 2b2t direct-connect button to the title screen.                                                                                                                                                |
| **Themed Category Icons**     | *Enable module category icons in your Meteor GUI settings to see a random Stardust/2b2t-themed icon for the Stardust modules category.* Randomly picks a new icon each time you launch your game!                                                                                                                                   |
| **Disable Meteor Telemetry**  | *Config setting to disable Meteor's periodic telemetry pings for their online player api.*                                                                                                                                                                                                                                          |
| **Packet-spam Anti-kick**     | *Config setting to prevent you from getting kicked for sending too many invalid inventory interactions to the server.* As of June 2025, 2b2t kicks you for sending just a small handful of invalid QUICK_MOVE packets too quickly. This feature inspects these packets and cancels them if they are invalid, preventing some kicks. |
| **Ignore 2b2t.org Overlay**   | *Config setting to disable the 2b2t.org overlay messages that show up periodically when playing on 2b2t.*                                                                                                                                                                                                                           |
| **ExpThrower Level Cap**      | *Adds settings to Meteor's built-in ExpThrower module to automatically stop throwing when a chosen experience level is reached.*                                                                                                                                                                                                    |
| **BetterTooltips Durability** | *Adds raw damage & true durability settings to Meteor's BetterTooltips module.*                                                                                                                                                                                                                                                     |
| **Custom GUI Theme**          | *Adds a custom Stardust theme option to your Meteor GUI config.* Credit to [crosby](https://github.com/RacoonDog) for the original implementation of recolored themes. I may add more themes in the future if I feel like it.                                                                                                       |

<br>

### Installation

1. Ensure that [Fabric](https://fabricmc.net) for Minecraft version 1.21.4 is installed.
2. Download the [latest release](https://github.com/0xTas/stardust/releases/tag/latest-1.21.4), or build the addon from source with `./gradlew build`.
3. Drop the .jar file in your `.minecraft/mods` folder, along with [Meteor Client for 1.21.4](https://meteorclient.com/archive).
4. Run your Fabric installation.
<br>

### Building & Contributing

1. Building the addon requires that Java 21 is installed, and in your PATH environment.
2. After cloning and inspecting the source code, the addon can be built with the `./gradlew build` command.
3. To contribute, IntelliJ Idea is recommended. First, run the `./gradlew genSources` task to generate a mapped version of Minecraft's code locally.
4. Then you can apply those mappings by ctrl+clicking on any Minecraft class and selecting `Choose Sources` from the red banner across the top.
5. Select the appropriate sources Jar from the list (usually named something like `net.minecraft:minecraft-merged-...-sources.jar`) and hit `Ok`.
6. Now you can contribute or modify code with those mappings as a reference (ctrl+click to view the source for any class.)

>Issues and pull requests are welcome if you would like to submit them.
<br>

### Credits
- [**Meteor Development**](https://github.com/MeteorDevelopment) *for [Meteor Client](https://meteorclient.com).*
- [**rfresh**](https://github.com/rfresh2) *for the [2b2t statistics api](https://api.2b2t.vc).*
- [**crosby**](https://github.com/RacoonDog) *for the [custom GUI themes impl](https://github.com/RacoonDog/Tokyo-Client/blob/main/src/main/java/io/github/racoondog/tokyo/utils/RecolorGuiTheme.java).*
