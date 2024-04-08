# Stardust

Stardust is an addon for [Meteor Client](https://meteorclient.com) designed for use on the
anarchy Minecraft server [2b2t](https://2b2t.org).<br>
It provides a handful of modules for enhancing your experience on old server.<br><br>

### Feature Set
#### Commands

- **LastSeen2b2t, FirstSeen2b2t, Playtime2b2t** - *Self explanatory commands. Use your meteor prefix.* Credit to [rfresh](https://github.com/rfresh2) for the current best [2b2t stats api](https://api.2b2t.vc).
- **Panorama** - *Takes a panorama screenshot and automatically assembles it into a resource pack for the main menu screen.* Usage: `.panorama [name]`
#### Modules
- **SignatureSign** - *An AutoSign module that's actually good.* Fully customizable template mode, & story mode for long input files over multiple signs.
- **SignHistorian** - *Record and restore broken or modified signs that you previously visited.* Since 1.19, creepers have been a real problem for sign boards everywhere. Now even if they blow that shit up, all is not lost.
- **AxolotlTools** - *Variant ESP, auto-collector & auto-breeder for axolotls.* Can also catch buckets of tropical fish, with or without a farm setup. I used this to get blue axolotls on 2b2t because interacting with them normally pisses me off.
- **AutoDoors** - *Automatically interacts with doors.* Includes an insufferable door spammer mode as well (complete with client-side mute option).
- **AutoDrawDistance** - *Automatically adjusts your render distance to maintain an FPS target.* Some biomes/areas can drop my fps by half, so I found this somewhat useful at times.
- **AutoDyeShulkers** - *Automatically dye shulker boxes a desired color in crafting grids.*
- **LoreLocator** - *Slot highlighter for rare, unique, and anomalous items.* Capable of highlighting renamed items, items with illegal enchants, negative durability values, and more.
- **BannerData** - *Right-click banners to display their pattern and color data.* Looks cool, gives you the base color without any fuss. Can also copy the raw nbt to your clipboard.
- **BookTools** - *Enhancements for working with books.* Adds buttons for inserting color & formatting codes into writable books, and adds a deobfuscation button to written books containing obfuscated/magic text.
- **StashBrander** - *Automatically rename desired items in bulk when using anvils.*
- **TreasureESP** - *An ESP module for buried treasure chests.* Finding buried treasure is the only way of obtaining Hearts of the Sea, and Conduits by extension.
- **MusicTweaks** - *Lets you tweak various things relating to the background music.* Change the pitch, volume, or cooldown between songs, or even choose and view which soundtracks play during your session.
- **RocketMan** - *Makes flying with fireworks much easier (bring lots of rockets!)* This doesn't feature any fancy grim control bypasses or anything like that. This is just a good clean quasi-control firework efly that won't be patched as long as you have access to rockets (which are currently afkable.)
- **Honker** - *Automatically uses goat horns when a player enters your render distance.* You can select your preferred horn type, or choose random for a surprise pick from your inventory each time.
- **WaxAura** - *Automatically waxes signs within your reach.* On 2b2t, sign editing has been disabled "for now™". This module makes it easy to wax as many signs as possible before it gets enabled.
- **ChatSigns** - *Read nearby signs in your chat.* Can also highlight potentially old pre-1.8 signs*.

**\*1.19+ OldSigns**<br>
*Now that 2b2t has updated to 1.19, old sign (pre 1.8) metadata is now identical to metadata for new oak signs placed in old chunks after the update. Metadata for signs placed in versions 1.8-1.12 is still recognizable as such.*<br>

This means that false positives are now **unavoidable**, but the new module takes into account the likelihood of a sign being old based on whether it is in old chunks, is an oak sign, and doesn't contain dates >= 2015.<br>

It isn't useless, but you will have to use your best judgement to determine what is legitimately likely to be old.

**I have not made a separate ESP module for OldSigns.** *You'll need to configure the OldSign ESP functionality built-in to ChatSigns instead.*

---
### Installation

1. Ensure that [Fabric](https://fabricmc.net) for Minecraft versions 1.20.1 or 1.20.4 are installed.
2. Download the [latest release](https://github.com/0xTas/stardust/releases/latest) which corresponds to your selected version,<br>or build the desired branch of the addon from source with `./gradlew build`.
3. Drop the .jar file in your `.minecraft/mods` folder, along with Meteor Client (v0.5.4 for 1.20.1, v0.5.6 for 1.20.4)
4. Run your Fabric installation.

---
### Contributing
Issues and pull requests are welcome if you would like to submit them.<br>
You can also get in touch with me if needed in the [Meteor Client Discord](https://discord.com/invite/bBGQZvd).

---
### Credits
- [**Meteor Development**](https://github.com/MeteorDevelopment) *for [Meteor Client](https://meteorclient.com).*
- [**rfresh**](https://github.com/rfresh2) *for the [2b2t statistics api](https://api.2b2t.vc).*
