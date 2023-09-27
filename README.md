# Stardust

Stardust is an addon for [Meteor Client](https://meteorclient.com) designed for use on the
anarchy Minecraft server [2b2t](https://2b2t.org).<br>
It provides a handful of modules for enhancing your experience on old server.<br><br>

### Feature List

- **LastSeen2b2t, FirstSeen2b2t, Playtime2b2t** - *Self explanatory commands. Use your meteor prefix.* Credit to [rfresh](https://github.com/rfresh2) for the current best [2b2t stats api](https://api.2b2t.vc).
- **SignatureSign** - *An AutoSign module that's actually good.* Fully customizable template mode, & story mode for long input files over multiple signs.
- **AutoDoors** - *Automatically interacts with doors.* Includes an insufferable door spammer mode as well (complete with client-side mute option).
- **AutoDrawDistance** - *Automatically adjusts your render distance to maintain an FPS target.* Some biomes/areas can drop my fps by half, so I found this somewhat useful at times.
- **BannerData** - *Right-click banners to display their pattern and color data.* Looks cool, gives you the base color without any fuss. Can also copy the raw nbt to your clipboard.
- **BookTools** - *Enhancements for working with books.* Adds buttons for inserting color & formatting codes into writable books, and adds a deobfuscation button to written books containing obfuscated/magic text.
- **RocketMan** - *Makes flying with fireworks much easier (bring lots of rockets!)* This doesn't feature any fancy grim control bypasses or anything like that. This is just a good clean quasi-control firework efly that won't be patched as long as you have access to rockets (which are currently afkable.)
- **ChatSigns** - *Read nearby signs in your chat.* Can also point-out possible old signs*.

 **\*1.19+ OldSigns**<br>
*Now that 2b2t has updated to 1.19, old sign (pre 1.8) metadata is now identical to metadata for new oak signs placed in old chunks after the update. Metadata for signs placed in versions 1.8-1.12 is still recognizable as such.*<br>

This means that false positives are now **unavoidable**, but the new module takes into account the likelihood of a sign being old based on whether it is in old chunks, is an oak sign, and doesn't contain dates >= 2023.<br>

It isn't completely useless, but you will have to use your best judgement to determine what is legitimately likely to be old.

**Because it isn't as reliable anymore, I have not made an ESP module for OldSigns.** *You'll need to use the functionality built-in to ChatSigns instead.*

---
### Installation

1. Ensure that [Fabric](https://fabricmc.net) for Minecraft 1.20.1 is installed.
2. Download the latest release, or build the addon from source with `./gradlew build`.
3. Drop the .jar file in your `.minecraft/mods` folder, along with [Meteor Client *v0.5.4*](https://meteorclient.com) or later.
4. Run your Fabric installation.

---
### Contributing
Issues and pull requests are welcome if you would like to submit them.<br>
You can also get in touch with me if needed in the [Meteor Client Discord](https://discord.com/invite/bBGQZvd).

---
### Credits
- [**Meteor Development**](https://github.com/MeteorDevelopment) *for [Meteor Client](https://meteorclient.com).*
- [**rfresh**](https://github.com/rfresh2) *for the [2b2t statistics api](https://api.2b2t.vc).*
