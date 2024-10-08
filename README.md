# ✨ Stardust

Stardust is an addon for [Meteor Client](https://meteorclient.com) designed for use on the
anarchy Minecraft server [2b2t](https://2b2t.org).<br>
It contains original modules to enhance your experience on old server, with a focus on configurability and polish.

### Feature Set
#### Commands

- **Stats2b2t, LastSeen2b2t, FirstSeen2b2t, Playtime2b2t** - *Self explanatory commands. Use your meteor prefix.* Credit to [rfresh](https://github.com/rfresh2) for the current best [2b2t stats api](https://api.2b2t.vc).
- **Panorama** - *Takes a panorama screenshot and automatically assembles it into a resource pack for the main menu screen.* Usage: `.panorama [name]`
<br><br>

#### Modules
- **ChatSigns** - *Read nearby signs in your chat.* Can also highlight and ESP potentially old signs placed before version 1.8 (January 2015 on 2b2t.)*
- **SignatureSign** - *An AutoSign module that's actually good.* Fully customizable template mode, & story mode for long input files over multiple signs.
- **SignHistorian** - *Record and restore broken or modified signs that you previously visited.* Since 1.19, creepers have been a real problem for sign boards everywhere. Now even if they blow that shit up, all is not lost.
- **RocketMan** - *Makes flying with fireworks much easier (and faster!) \[bring lots of rockets!\]* RocketMan is a versatile and highly-configurable firework efly module with many useful capabilities.
- **StashBrander** - *Automatically rename desired items in bulk when using anvils.*
- **AutoDoors** - *Automatically interacts with doors.* Includes an insufferable door spammer mode as well (complete with client-side mute option).
- **AutoSmith** - *Automatically upgrades gear or trims armor sets when interacting with smithing tables.*
- **AntiToS** - *Censors player-generated text on render, like signs, books, chat, and more, according to a customizable content blacklist.*
- **LoreLocator** - *Slot highlighter for rare, unique, and anomalous items.* Capable of highlighting renamed items, items with illegal enchants, zero durability items, and more.
- **AxolotlTools** - *Variant ESP, auto-collector & auto-breeder for axolotls.* Can also catch buckets of tropical fish, with or without a farm setup. I used this to get blue axolotls on 2b2t because interacting with them normally pisses me off.
- **AutoDrawDistance** - *Automatically adjusts your render distance to maintain an FPS target.* Some biomes/areas can drop my fps by half, so I occasionally find this useful.
- **AutoDyeShulkers** - *Automatically dye shulker boxes a desired color in crafting grids.*
- **BannerData** - *Right-click banners to display their pattern and color data.* Looks cool, gives you the base color without any fuss. Can also copy the raw nbt to your clipboard.
- **BookTools** - *Enhancements for working with books.* Adds buttons for inserting color & formatting codes into writable books, and adds a deobfuscation button to written books containing obfuscated/magic text.
- **PagePirate** - *Pirates books held by other players* (displays their contents in your chat, and optionally makes a local copy using a book & quill).
- **TreasureESP** - *An ESP module for buried treasure chests.* Finding buried treasure is the only way of obtaining Hearts of the Sea, and Conduits by extension.
- **MusicTweaks** - *Lets you tweak various things relating to the background music.* Change the pitch, volume, or cooldown between songs, or even choose and view which soundtracks play during your session.
- **Honker** - *Automatically uses goat horns when a player enters your render distance.* You can select your preferred horn type, or choose random for a surprise pick from your inventory each time.
- **Updraft** - *Automatically enhances your jumps with wind charges.* Wind charges seem to be nerfed on Paper compared to Vanilla, but maybe someone would still find this fun or useful.
- **WaxAura** - *Automatically waxes signs within your reach.* On 2b2t, sign editing has been disabled "for now™". This module makes it easy to wax as many signs as possible before it gets enabled.
<br><br>

#### Miscellaneous
- **Illegal Disconnect** - *Adds "Illegal Disconnect" functionality to Meteor's built-in AutoLog module.* Enable this in the AutoLog module's settings. Also adds a config setting to put a dedicated Illegal Disconnect button on the pause menu.
- **Click-to-Come** - *When using Meteor's Freecam module, click on a block or entity to Baritone path there.* Enable this in the Freecam module's settings.
- **Title Screen Tweaks** - *2b2t-themed changes to the title screen.* Adds config settings to make the splash texts green, to cycle them randomly, and to add a 2b2t direct-connect button to the title screen.
- **Themed Category Icons** - *Enable module category icons in your config settings to see a random Stardust/2b2t-themed icon for the Stardust modules category.* Randomly picks a new icon each time you open your game!

>*OldSigns: <br>
Ever since 2b2t updated to 1.19, the NBT tag artifact that separates pre-1.8 from post-1.8 signs is missing on newly-placed signs.<br>
This means that false positives are now *unavoidable*, but should be limited to *oak signs placed after 1.19 in old (pre-1.8) chunks that do not contain dates after 2015*.<br>
Use your best judgement to further identify false positives or fakes.<br>
OldSigns functionality can be accessed in the ChatSigns module settings.
<br>

### Installation

1. Ensure that [Fabric](https://fabricmc.net) for Minecraft version 1.21.1 is installed.
2. Download the [latest release](https://github.com/0xTas/stardust/releases/latest), or build the addon from source with `./gradlew build`.
3. Drop the .jar file in your `.minecraft/mods` folder, along with Meteor Client (v0.5.8 for 1.21.1)
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
