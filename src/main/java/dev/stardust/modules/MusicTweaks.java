package dev.stardust.modules;

import java.util.List;
import java.util.ArrayList;
import dev.stardust.Stardust;
import net.minecraft.text.Text;
import javax.annotation.Nullable;
import dev.stardust.util.StardustUtil;
import net.minecraft.sound.MusicSound;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.MinecraftClient;
import meteordevelopment.orbit.EventHandler;
import dev.stardust.mixin.MusicTrackerAccessor;
import io.netty.util.internal.ThreadLocalRandom;
import net.minecraft.client.sound.SoundInstance;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.mixininterface.IChatHud;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;


/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class MusicTweaks extends Module {
    public MusicTweaks() {
        super(Stardust.CATEGORY, "MusicTweaks", "Allows you to fuck with the background music.");
        runInMainMenu = true;
        sgOverworldSoundtrack.sectionExpanded = false;
        sgCreativeSoundtrack.sectionExpanded = false;
        sgUnderwaterSoundtrack.sectionExpanded = false;
        sgNetherSoundtrack.sectionExpanded = false;
        sgEndSoundtrack.sectionExpanded = false;
        sgRecordsSoundtrack.sectionExpanded = false;
        sgMenuSoundtrack.sectionExpanded = false;
    }

    public enum DisplayType {
        Chat, Record
    }

    private final SettingGroup sgPitch = settings.createGroup("Pitch");
    private final SettingGroup sgVolume = settings.createGroup("Volume");
    private final SettingGroup sgCooldown = settings.createGroup("Cooldown");
    private final SettingGroup sgNowPlaying = settings.createGroup("Now Playing");
    private final SettingGroup sgOverworldSoundtrack = settings.createGroup("Overworld Soundtrack");
    private final SettingGroup sgCreativeSoundtrack = settings.createGroup("Creative Soundtrack");
    private final SettingGroup sgUnderwaterSoundtrack = settings.createGroup("Underwater Soundtrack");
    private final SettingGroup sgNetherSoundtrack = settings.createGroup("Nether Soundtrack");
    private final SettingGroup sgEndSoundtrack = settings.createGroup("End Soundtrack");
    private final SettingGroup sgRecordsSoundtrack = settings.createGroup("Music Discs");
    private final SettingGroup sgMenuSoundtrack = settings.createGroup("Menu Soundtrack");


    private final Setting<Boolean> startOnEnable = sgNowPlaying.add(
        new BoolSetting.Builder()
            .name("Start on Enable")
            .description("Start playing music when enabling the module. Won't overwrite a currently-playing song.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> stopOnDisable = sgNowPlaying.add(
        new BoolSetting.Builder()
            .name("Stop on Disable")
            .description("Stop the currently playing music when disabling the module.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> displayNowPlaying = sgNowPlaying.add(
        new BoolSetting.Builder()
            .name("Display Now Playing")
            .description("Displays the name of the currently playing song.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> fadeOut = sgNowPlaying.add(
        new BoolSetting.Builder()
            .name("Fade Out Display")
            .description("Fade out the display instead of keeping it active for the duration of the song.")
            .visible(displayNowPlaying::get)
            .defaultValue(false)
            .build()
    );

    private final Setting<DisplayType> displayTypeSetting = sgNowPlaying.add(
        new EnumSetting.Builder<DisplayType>()
            .name("Display Mode")
            .defaultValue(DisplayType.Chat)
            .visible(displayNowPlaying::get)
            .build()
    ) ;

    private final Setting<Boolean> overrideDelayMode = sgCooldown.add(
        new BoolSetting.Builder()
            .name("Use Exact Delay")
            .description("Use one specific cooldown between songs instead of a random range.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> timeUntilNextSong = sgCooldown.add(
        new IntSetting.Builder()
            .name("Song Delay Seconds")
            .description("Desired cooldown between songs. Will apply after next song if not currently playing (or module toggle.)")
            .range(0, 10000)
            .sliderRange(0, 2400)
            .defaultValue(300)
            .visible(overrideDelayMode::get)
            .build()
    );

    private final Setting<Integer> minTimeUntilNextSong = sgCooldown.add(
        new IntSetting.Builder()
            .name("Minimum Delay Seconds")
            .description("Minimum desired cooldown between songs (in seconds.)")
            .range(0, 10000)
            .sliderRange(0, 1200)
            .defaultValue(600)
            .visible(() -> !overrideDelayMode.get())
            .build()
    );

    private final Setting<Integer> maxTimeUntilNextSong = sgCooldown.add(
        new IntSetting.Builder()
            .name("Maximum Delay Seconds")
            .description("Maximum desired cooldown between songs (in seconds.)")
            .range(0, 10000)
            .sliderRange(0, 2400)
            .defaultValue(1200)
            .visible(() -> !overrideDelayMode.get())
            .build()
    );

    private final Setting<Boolean> randomPitch = sgPitch.add(
        new BoolSetting.Builder()
            .name("Random Pitch")
            .description("Use a random pitch within a range instead of the same adjusted pitch each time.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> trippyPitchSetting = sgPitch.add(
        new BoolSetting.Builder()
            .name("Trippy Pitch")
            .description(":3")
            .defaultValue(false)
            .visible(randomPitch::get)
            .build()
    );

    private final Setting<Integer> pitchAdjustment = sgPitch.add(
        new IntSetting.Builder()
            .name("Song Pitch Adjustment")
            .description("Desired pitch adjustment.")
            .range(-500, 500)
            .sliderRange(-250, 250)
            .defaultValue(0)
            .visible(() -> !randomPitch.get())
            .build()
    );

    private final Setting<Integer> pitchRange = sgPitch.add(
        new IntSetting.Builder()
            .name("Random Pitch Adjustment Range")
            .description("Will apply on the next song.")
            .range(1, 500)
            .sliderRange(1, 500)
            .defaultValue(37)
            .visible(() -> randomPitch.get() && !trippyPitchSetting.get())
            .build()
    );

    private final Setting<Integer> pitchIntensity = sgPitch.add(
        new IntSetting.Builder()
            .name("Intensity")
            .range(0, 1000)
            .sliderRange(0, 500)
            .defaultValue(77)
            .visible(() -> randomPitch.get() && trippyPitchSetting.get())
            .build()
    );

    private final Setting<Integer> weightedChanceSetting = sgPitch.add(
        new IntSetting.Builder()
            .name("Weighted Chance %")
            .sliderRange(0, 100)
            .defaultValue(95)
            .visible(() -> randomPitch.get() && trippyPitchSetting.get())
            .build()
    );

    private final Setting<Integer> volume = sgVolume.add(
        new IntSetting.Builder()
            .name("Volume % Boost")
            .sliderRange(-100, 250)
            .range(-100, 400)
            .defaultValue(0)
            .build()
    );


    // Soundtracks
    private final Setting<Boolean> minecraft = sgOverworldSoundtrack.add(
        new BoolSetting.Builder()
            .name("C418 / Minecraft")
            .description("calm1.ogg")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> clark = sgOverworldSoundtrack.add(
        new BoolSetting.Builder()
            .name("C418 / Clark")
            .description("calm2.ogg")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> sweden = sgOverworldSoundtrack.add(
        new BoolSetting.Builder()
            .name("C418 / Sweden")
            .description("calm3.ogg")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> subwooferLullaby = sgOverworldSoundtrack.add(
        new BoolSetting.Builder()
            .name("C418 / Subwoofer Lullaby")
            .description("hal1.ogg")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> livingMice = sgOverworldSoundtrack.add(
        new BoolSetting.Builder()
            .name("C418 / Living Mice")
            .description("hal2.ogg")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> haggstrom = sgOverworldSoundtrack.add(
        new BoolSetting.Builder()
            .name("C418 / Haggstrom")
            .description("hal3.ogg")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> danny = sgOverworldSoundtrack.add(
        new BoolSetting.Builder()
            .name("C418 / Danny")
            .description("hal4.ogg")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> key = sgOverworldSoundtrack.add(
        new BoolSetting.Builder()
            .name("C418 / Key")
            .description("nuance1.ogg")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> oxygene = sgOverworldSoundtrack.add(
        new BoolSetting.Builder()
            .name("C418 / Oxygene")
            .description("nuance2.ogg")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> dryHands = sgOverworldSoundtrack.add(
        new BoolSetting.Builder()
            .name("C418 / Dry Hands")
            .description("piano1.ogg")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> wetHands = sgOverworldSoundtrack.add(
        new BoolSetting.Builder()
            .name("C418 / Wet Hands")
            .description("piano2.ogg")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> miceOnVenus = sgOverworldSoundtrack.add(
        new BoolSetting.Builder()
            .name("C418 / Mice on Venus")
            .description("piano3.ogg")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> aerie = sgOverworldSoundtrack.add(
        new BoolSetting.Builder()
            .name("Lena Raine / Aerie")
            .description("aerie.ogg")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> ancestry = sgOverworldSoundtrack.add(
        new BoolSetting.Builder()
            .name("Lena Raine / Ancestry")
            .description("ancestry.ogg")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> aFamiliarRoom = sgOverworldSoundtrack.add(
        new BoolSetting.Builder()
            .name("Aaron Cherof / A Familiar Room")
            .description("a_familiar_room.ogg")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> anOrdinaryDay = sgOverworldSoundtrack.add(
        new BoolSetting.Builder()
            .name("Kumi Tanioka / An Ordinary Day")
            .description("an_ordinary_day.ogg")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> bromeliad = sgOverworldSoundtrack.add(
        new BoolSetting.Builder()
            .name("Aaron Cherof / Bromeliad")
            .description("bromeliad.ogg")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> comfortingMemories = sgOverworldSoundtrack.add(
        new BoolSetting.Builder()
            .name("Kumi Tanioka / Comforting Memories")
            .description("comforting_memories.ogg")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> crescentDunes = sgOverworldSoundtrack.add(
        new BoolSetting.Builder()
            .name("Aaron Cherof / Crescent Dunes")
            .description("crescent_dunes.ogg")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> echoInTheWind = sgOverworldSoundtrack.add(
        new BoolSetting.Builder()
            .name("Aaron Cherof / Echo in the Wind")
            .description("echo_in_the_wind.ogg")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> firebugs = sgOverworldSoundtrack.add(
        new BoolSetting.Builder()
            .name("Lena Raine / Firebugs")
            .description("firebugs.ogg")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> floatingDream = sgOverworldSoundtrack.add(
        new BoolSetting.Builder()
            .name("Kumi Tanioka / Floating Dream")
            .description("floating_dream.ogg")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> infiniteAmethyst = sgOverworldSoundtrack.add(
        new BoolSetting.Builder()
            .name("Lena Raine / Infinite Amethyst")
            .description("infinite_amethyst.ogg")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> labyrinthine = sgOverworldSoundtrack.add(
        new BoolSetting.Builder()
            .name("Lena Raine / Labyrinthine")
            .description("labyrinthine.ogg")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> leftToBloom = sgOverworldSoundtrack.add(
        new BoolSetting.Builder()
            .name("Lena Raine / Left to Bloom")
            .description("left_to_bloom.ogg")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> oneMoreDay = sgOverworldSoundtrack.add(
        new BoolSetting.Builder()
            .name("Lena Raine / One More Day")
            .description("one_more_day.ogg")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> standTall = sgOverworldSoundtrack.add(
        new BoolSetting.Builder()
            .name("Lena Raine / Stand Tall")
            .description("stand_tall.ogg")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> wending = sgOverworldSoundtrack.add(
        new BoolSetting.Builder()
            .name("Lena Raine / Wending")
            .description("wending.ogg")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> biomeFest = sgCreativeSoundtrack.add(
        new BoolSetting.Builder()
            .name("C418 / Biome Fest")
            .description("creative1.ogg")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> blindSpots = sgCreativeSoundtrack.add(
        new BoolSetting.Builder()
            .name("C418 / Blind Spots")
            .description("creative2.ogg")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> hauntMuskie = sgCreativeSoundtrack.add(
        new BoolSetting.Builder()
            .name("C418 / Haunt Muskie")
            .description("creative3.ogg")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> ariaMath = sgCreativeSoundtrack.add(
        new BoolSetting.Builder()
            .name("C418 / Aria Math")
            .description("creative4.ogg")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> dreiton = sgCreativeSoundtrack.add(
        new BoolSetting.Builder()
            .name("C418 / Dreiton")
            .description("creative5.ogg")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> tasWell = sgCreativeSoundtrack.add(
        new BoolSetting.Builder()
            .name("C418 / Taswell")
            .description("creative6.ogg")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> axolotl = sgUnderwaterSoundtrack.add(
        new BoolSetting.Builder()
            .name("C418 / Axolotl")
            .description("axolotl.ogg")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> dragonFish = sgUnderwaterSoundtrack.add(
        new BoolSetting.Builder()
            .name("C418 / Dragon Fish")
            .description("dragon_fish.ogg")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> shuniji = sgUnderwaterSoundtrack.add(
        new BoolSetting.Builder()
            .name("C418 / Shuniji")
            .description("shuniji.ogg")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> concreteHalls = sgNetherSoundtrack.add(
        new BoolSetting.Builder()
            .name("C418 / Concrete Halls")
            .description("nether1.ogg")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> deadVoxel = sgNetherSoundtrack.add(
        new BoolSetting.Builder()
            .name("C418 / Dead Voxel")
            .description("nether2.ogg")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> warmth = sgNetherSoundtrack.add(
        new BoolSetting.Builder()
            .name("C418 / Warmth")
            .description("nether3.ogg")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> balladOfTheCats = sgNetherSoundtrack.add(
        new BoolSetting.Builder()
            .name("C418 / Ballad of the Cats")
            .description("nether4.ogg")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> chrysopoeia = sgNetherSoundtrack.add(
        new BoolSetting.Builder()
            .name("Lena Raine / Chrysopoeia")
            .description("chrysopoeia.ogg")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> rubedo = sgNetherSoundtrack.add(
        new BoolSetting.Builder()
            .name("Lena Raine / Rubedo")
            .description("rubedo.ogg")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> soBelow = sgNetherSoundtrack.add(
        new BoolSetting.Builder()
            .name("Lena Raine / So Below")
            .description("so_below.ogg")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> theEnd = sgEndSoundtrack.add(
        new BoolSetting.Builder()
            .name("C418 / The End")
            .description("end.ogg")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> boss = sgEndSoundtrack.add(
        new BoolSetting.Builder()
            .name("C418 / Boss")
            .description("boss.ogg")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> alpha = sgEndSoundtrack.add(
        new BoolSetting.Builder()
            .name("C418 / Alpha")
            .description("credits.ogg")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> record5 = sgRecordsSoundtrack.add(
        new BoolSetting.Builder()
            .name("Samuel Aberg / 5")
            .description("Music Disc: 5")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> record11 = sgRecordsSoundtrack.add(
        new BoolSetting.Builder()
            .name("C418 / 11")
            .description("Music Disc: 11")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> record13 = sgRecordsSoundtrack.add(
        new BoolSetting.Builder()
            .name("C418 / 13")
            .description("Music Disc: 13")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> recordCat = sgRecordsSoundtrack.add(
        new BoolSetting.Builder()
            .name("C418 / Cat")
            .description("Music Disc: Cat")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> recordBlocks = sgRecordsSoundtrack.add(
        new BoolSetting.Builder()
            .name("C418 / Blocks")
            .description("Music Disc: Blocks")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> recordChirp = sgRecordsSoundtrack.add(
        new BoolSetting.Builder()
            .name("C418 / Chirp")
            .description("Music Disc: Chirp")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> recordFar = sgRecordsSoundtrack.add(
        new BoolSetting.Builder()
            .name("C418 / Far")
            .description("Music Disc: Far")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> recordMall = sgRecordsSoundtrack.add(
        new BoolSetting.Builder()
            .name("C418 / Mall")
            .description("Music Disc: Mall")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> recordMellohi = sgRecordsSoundtrack.add(
        new BoolSetting.Builder()
            .name("C418 / Mellohi")
            .description("Music Disc: Mellohi")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> recordStal = sgRecordsSoundtrack.add(
        new BoolSetting.Builder()
            .name("C418 / Stal")
            .description("Music Disc: Stal")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> recordStrad = sgRecordsSoundtrack.add(
        new BoolSetting.Builder()
            .name("C418 / Strad")
            .description("Music Disc: Strad")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> recordWard = sgRecordsSoundtrack.add(
        new BoolSetting.Builder()
            .name("C418 / Ward")
            .description("Music Disc: Ward")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> recordWait = sgRecordsSoundtrack.add(
        new BoolSetting.Builder()
            .name("C418 / Wait")
            .description("Music Disc: Wait")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> recordOtherside = sgRecordsSoundtrack.add(
        new BoolSetting.Builder()
            .name("Lena Raine / Otherside")
            .description("Music Disc: Otherside")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> recordPigstep = sgRecordsSoundtrack.add(
        new BoolSetting.Builder()
            .name("Lena Raine / Pigstep")
            .description("Music Disc: Pigstep")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> recordRelic = sgRecordsSoundtrack.add(
        new BoolSetting.Builder()
            .name("Aaron Cherof / Relic")
            .description("Music Disc: Relic")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> mutation = sgMenuSoundtrack.add(
        new BoolSetting.Builder()
            .name("C418 / Mutation")
            .description("menu1.ogg")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> moogCity2 = sgMenuSoundtrack.add(
        new BoolSetting.Builder()
            .name("C418 / Moog City 2")
            .description("menu2.ogg")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> beginning2 = sgMenuSoundtrack.add(
        new BoolSetting.Builder()
            .name("C418 / Beginning 2")
            .description("menu3.ogg")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> floatingTrees = sgMenuSoundtrack.add(
        new BoolSetting.Builder()
            .name("C418 / Floating Trees")
            .description("menu4.ogg")
            .defaultValue(false)
            .build()
    );


    // See MinecraftClientMixin.java
    public MusicSound getType() {
        if (currentType != null) return currentType;

        int min;
        int max;
        if (mc.player == null) {
            min = 69;
            max = 420;
        } else if (overrideDelayMode.get()) {
            min = getTimeUntilNextSong();
            max = getTimeUntilNextSong();
        } else {
            min = minTimeUntilNextSong.get() * 20;
            max = maxTimeUntilNextSong.get() * 20;
        }
        if (max <= min) max = min + 1;

        // It doesn't matter which SoundEvents.MUSIC_??? we return since the WeightedSoundSet is overwritten directly now.
        // actually I lied tho don't use the music disc events, or it won't work (see WeightedSoundSetMixin.java)
        MusicSound type = new MusicSound(SoundEvents.MUSIC_GAME, min, ThreadLocalRandom.current().nextInt(min, max), false);

        currentType = type;
        return type;
    }

    // See SoundSystemMixin.java
    public String getSongName(String songID) {
        String songName;
        switch (songID) {
            case "minecraft.ogg" -> songName = "C418 - Minecraft";
            case "clark.ogg" -> songName = "C418 - Clark";
            case "sweden.ogg" -> songName = "C418 - Sweden";
            case "biome_fest.ogg" -> songName = "C418 - Biome Fest";
            case "blind_spots.ogg" -> songName = "C418 - Blind Spots";
            case "haunt_muskie.ogg" -> songName = "C418 - Haunt Muskie";
            case "aria_math.ogg" -> songName = "C418 - Aria Math";
            case "dreiton.ogg" -> songName = "C418 - Dreiton";
            case "taswell.ogg" -> songName = "C418 - Taswell";
            case "subwoofer_lullaby.ogg" -> songName = "C418 - Subwoofer Lullaby";
            case "living_mice.ogg" -> songName = "C418 - Living Mice";
            case "haggstrom.ogg" -> songName = "C418 - Haggstrom";
            case "danny.ogg" -> songName = "C418 - Danny";
            case "key.ogg" -> songName = "C418 - Key";
            case "oxygene.ogg" -> songName = "C418 - Oxygène";
            case "dry_hands.ogg" -> songName = "C418 - Dry Hands";
            case "wet_hands.ogg" -> songName = "C418 - Wet Hands";
            case "mice_on_venus.ogg" -> songName = "C418 - Mice on Venus";
            case "aerie.ogg" -> songName = "Lena Raine - Aerie";
            case "ancestry.ogg" -> songName = "Lena Raine - Ancestry";
            case "a_familiar_room.ogg" -> songName = "Aaron Cherof - A Familiar Room";
            case "an_ordinary_day.ogg" -> songName = "Kumi Tanioka - An Ordinary Day";
            case "bromeliad.ogg" -> songName = "Aaron Cherof - Bromeliad";
            case "comforting_memories.ogg" -> songName = "Kumi Tanioka - Comforting Memories";
            case "crescent_dunes.ogg" -> songName = "Aaron Cherof - Crescent Dunes";
            case "echo_in_the_wind.ogg" -> songName = "Aaron Cherof - Echo in the Wind";
            case "firebugs.ogg" -> songName = "Lena Raine - Firebugs";
            case "floating_dream.ogg" -> songName = "Kumi Tanioka - Floating Dream";
            case "infinite_amethyst.ogg" -> songName = "Lena Raine - Infinite Amethyst";
            case "labyrinthine.ogg" -> songName = "Lena Raine - Labyrinthine";
            case "left_to_bloom.ogg" -> songName = "Lena Raine - Left to Bloom";
            case "one_more_day.ogg" -> songName = "Lena Raine - One More Day";
            case "stand_tall.ogg" -> songName = "Lena Raine - Stand Tall";
            case "wending.ogg" -> songName = "Lena Raine - Wending";
            case "axolotl.ogg" -> songName = "C418 - Axolotl";
            case "dragon_fish.ogg" -> songName = "C418 - Dragon Fish";
            case "shuniji.ogg" -> songName = "C418 - Shuniji";
            case "concrete_halls.ogg" -> songName = "C418 - Concrete Halls";
            case "dead_voxel.ogg" -> songName = "C418 - Dead Voxel";
            case "warmth.ogg" -> songName = "C418 - Warmth";
            case "ballad_of_the_cats.ogg" -> songName = "C418 - Ballad of the Cats";
            case "chrysopoeia.ogg" -> songName = "Lena Raine - Chrysopoeia";
            case "rubedo.ogg" -> songName = "Lena Raine - Rubedo";
            case "so_below.ogg" -> songName = "Lena Raine - So Below";
            case "boss.ogg" -> songName = "C418 - Boss";
            case "the_end.ogg" -> songName = "C418 - The End";
            case "mutation.ogg" -> songName = "C418 - Mutation";
            case "moog_city_2.ogg" -> songName = "C418 - Moog City 2";
            case "beginning_2.ogg" -> songName = "C418 - Beginning 2";
            case "floating_trees.ogg" -> songName = "C418 - Floating Trees";
            case "alpha.ogg" -> songName = "C418 - Alpha";
            case "5.ogg" -> songName = "Samuel Aberg - 5";
            case "11.ogg" -> songName = "C418 - 11";
            case "13.ogg" -> songName = "C418 - 13";
            case "cat.ogg" -> songName = "C418 - Cat";
            case "blocks.ogg" -> songName = "C418 - Blocks";
            case "chirp.ogg" -> songName = "C418 - Chirp";
            case "far.ogg" -> songName = "C418 - Far";
            case "mall.ogg" -> songName = "C418 - Mall";
            case "mellohi.ogg" -> songName = "C418 - Mellohi";
            case "stal.ogg" -> songName = "C418 - Stal";
            case "strad.ogg" -> songName = "C418 - Strad";
            case "ward.ogg" -> songName = "C418 - Ward";
            case "wait.ogg" -> songName = "C418 - Wait";
            case "otherside.ogg" -> songName = "Lena Raine - Otherside";
            case "pigstep.ogg" -> songName = "Lena Raine - Pigstep";
            case "relic.ogg" -> songName = "Aaron Cherof - Relic";
            default -> songName = "Unknown Track";
        }

        return songName;
    }

    // See WeightedSoundSetMixin.java
    public List<String> getSoundSet() {
        List<String> ids = new ArrayList<>();
        if (minecraft.get()) ids.add("minecraft:music/game/minecraft");
        if (clark.get()) ids.add("minecraft:music/game/clark");
        if (sweden.get()) ids.add("minecraft:music/game/sweden");
        if (biomeFest.get()) ids.add("minecraft:music/game/creative/biome_fest");
        if (blindSpots.get()) ids.add("minecraft:music/game/creative/blind_spots");
        if (hauntMuskie.get()) ids.add("minecraft:music/game/creative/haunt_muskie");
        if (ariaMath.get()) ids.add("minecraft:music/game/creative/aria_math");
        if (dreiton.get()) ids.add("minecraft:music/game/creative/dreiton");
        if (tasWell.get()) ids.add("minecraft:music/game/creative/taswell");
        if (subwooferLullaby.get()) ids.add("minecraft:music/game/subwoofer_lullaby");
        if (livingMice.get()) ids.add("minecraft:music/game/living_mice");
        if (haggstrom.get()) ids.add("minecraft:music/game/haggstrom");
        if (danny.get()) ids.add("minecraft:music/game/danny");
        if (key.get()) ids.add("minecraft:music/game/key");
        if (oxygene.get()) ids.add("minecraft:music/game/oxygene");
        if (dryHands.get()) ids.add("minecraft:music/game/dry_hands");
        if (wetHands.get()) ids.add("minecraft:music/game/wet_hands");
        if (miceOnVenus.get()) ids.add("minecraft:music/game/mice_on_venus");
        if (aerie.get()) ids.add("minecraft:music/game/swamp/aerie");
        if (bromeliad.get()) ids.add("minecraft:music/game/bromeliad");
        if (firebugs.get()) ids.add("minecraft:music/game/swamp/firebugs");
        if (leftToBloom.get()) ids.add("minecraft:music/game/left_to_bloom");
        if (axolotl.get()) ids.add("minecraft:music/game/water/axolotl");
        if (dragonFish.get()) ids.add("minecraft:music/game/water/dragon_fish");
        if (shuniji.get()) ids.add("minecraft:music/game/water/shuniji");
        if (labyrinthine.get()) ids.add("minecraft:music/game/swamp/labyrinthine");
        if (echoInTheWind.get()) ids.add("minecraft:music/game/echo_in_the_wind");
        if (standTall.get()) ids.add("minecraft:music/game/stand_tall");
        if (ancestry.get()) ids.add("minecraft:music/game/ancestry");
        if (aFamiliarRoom.get()) ids.add("minecraft:music/game/a_familiar_room");
        if (oneMoreDay.get()) ids.add("minecraft:music/game/one_more_day");
        if (wending.get()) ids.add("minecraft:music/game/wending");
        if (infiniteAmethyst.get()) ids.add("minecraft:music/game/infinite_amethyst");
        if (anOrdinaryDay.get()) ids.add("minecraft:music/game/an_ordinary_day");
        if (crescentDunes.get()) ids.add("minecraft:music/game/crescent_dunes");
        if (floatingDream.get()) ids.add("minecraft:music/game/floating_dream");
        if (comfortingMemories.get()) ids.add("minecraft:music/game/comforting_memories");
        if (mutation.get()) ids.add("minecraft:music/menu/mutation");
        if (moogCity2.get()) ids.add("minecraft:music/menu/moog_city_2");
        if (beginning2.get()) ids.add("minecraft:music/menu/beginning_2");
        if (floatingTrees.get()) ids.add("minecraft:music/menu/floating_trees");
        if (alpha.get()) ids.add("minecraft:music/game/end/alpha");
        if (theEnd.get()) ids.add("minecraft:music/game/end/the_end");
        if (boss.get()) ids.add("minecraft:music/game/end/boss");
        if (soBelow.get()) ids.add("minecraft:music/game/nether/soulsand_valley/so_below");
        if (rubedo.get()) ids.add("minecraft:music/game/nether/nether_wastes/rubedo");
        if (chrysopoeia.get()) ids.add("minecraft:music/game/nether/crimson_forest/chrysopoeia");
        if (concreteHalls.get()) ids.add("minecraft:music/game/nether/concrete_halls");
        if (deadVoxel.get()) ids.add("minecraft:music/game/nether/dead_voxel");
        if (warmth.get()) ids.add("minecraft:music/game/nether/warmth");
        if (balladOfTheCats.get()) ids.add("minecraft:music/game/nether/ballad_of_the_cats");
        if (record5.get()) ids.add("minecraft:records/5");
        if (record11.get()) ids.add("minecraft:records/11");
        if (record13.get()) ids.add("minecraft:records/13");
        if (recordCat.get()) ids.add("minecraft:records/cat");
        if (recordBlocks.get()) ids.add("minecraft:records/blocks");
        if (recordChirp.get()) ids.add("minecraft:records/chirp");
        if (recordFar.get()) ids.add("minecraft:records/far");
        if (recordMall.get()) ids.add("minecraft:records/mall");
        if (recordMellohi.get()) ids.add("minecraft:records/mellohi");
        if (recordStal.get()) ids.add("minecraft:records/stal");
        if (recordStrad.get()) ids.add("minecraft:records/strad");
        if (recordWard.get()) ids.add("minecraft:records/ward");
        if (recordWait.get()) ids.add("minecraft:records/wait");
        if (recordOtherside.get()) ids.add("minecraft:records/otherside");
        if (recordPigstep.get()) ids.add("minecraft:records/pigstep");
        if (recordRelic.get()) ids.add("minecraft:records/relic");

        // Prevent duplicates
        if (currentSong != null && ids.size() > 1) {
            for (int n = 0; n < ids.size(); n++) {
                if (currentSong.equals("Sound["+ids.get(n)+"]")) {
                    ids.remove(n);
                    currentSong = null;
                    break;
                }
            }
        }

        return ids;
    }

    // See SoundSystemMixin.java
    public float getNextPitchStep(float currentPitch) {
        if (lastDirection == null) {
            lastDirection = PitchDirection.Descending;
            float intensity = -(pitchIntensity.get() / 10000f);
            return MathHelper.clamp(currentPitch + (currentPitch * intensity), -5f, 5f);
        }

        switch (lastDirection) { // Lmao
            case Ascending -> {
                float weightedChance = ThreadLocalRandom.current().nextFloat(0, 1);

                float intensity;
                if (weightedChance <= (weightedChanceSetting.get() / 100f)) {
                    intensity = pitchIntensity.get() / 10000f;
                } else {
                    intensity = -(pitchIntensity.get() / 10000f);
                    lastDirection = PitchDirection.Descending;
                }
                return MathHelper.clamp(currentPitch + (currentPitch * intensity), -5f, 5f);
            }
            case Descending -> {
                float weightedChance = ThreadLocalRandom.current().nextFloat(0, 1);

                float intensity;
                if (weightedChance <= (weightedChanceSetting.get() / 100f)) {
                    intensity = -(pitchIntensity.get() / 10000f);
                } else {
                    intensity = pitchIntensity.get() / 10000f;
                    lastDirection = PitchDirection.Ascending;
                }
                return MathHelper.clamp(currentPitch + (currentPitch * intensity), -5f, 5f);
            }
        }
        return currentPitch;
    }

    public void sendNowPlayingMessage(String songName) {
        if (mc.player == null) return;
        String[] pieces = songName.split(" - ");
        ((IChatHud) mc.inGameHud.getChatHud()).meteor$add(
            Text.of("§8<"+rcc+"§o✨§r§8> §2§oNow Playing§r§8: §7§o"+pieces[0]+" §8- "+rcc+"§o"+pieces[1]+"§r§8."),
            songName.hashCode()
        );
    }

    // See MusicTrackerMixin.java
    public void nullifyCurrentType() {
        currentType = null;
        rcc = StardustUtil.rCC();
    }

    // See SoundSystemMixin.java
    public MinecraftClient getClient() { return mc; }
    public boolean shouldFadeOut() { return fadeOut.get(); }
    public boolean randomPitch() { return randomPitch.get(); }
    public boolean trippyPitch() { return trippyPitchSetting.get(); }
    public float getVolumeAdjustment() { return volume.get() / 100f; }
    public boolean overrideDelay() { return overrideDelayMode.get(); }
    public DisplayType getDisplayMode() { return displayTypeSetting.get(); }
    public void setCurrentSong(@Nullable String id) { currentSong = id; }
    public int getTimeUntilNextSong() { return timeUntilNextSong.get() * 20; }
    public float getPitchAdjustment() { return pitchAdjustment.get() / 1000f; }
    public boolean shouldDisplayNowPlaying() { return displayNowPlaying.get(); }
    public float getRandomPitch() { return ThreadLocalRandom.current().nextFloat(-pitchRange.get() / 1000f, pitchRange.get() / 1000f); }

    private enum PitchDirection {
        Ascending, Descending
    }

    @Nullable
    private String lastDim = null;
    @Nullable
    private String currentSong = null;
    @Nullable
    private MusicSound currentType = null;
    @Nullable
    private PitchDirection lastDirection = null;

    private String rcc = StardustUtil.rCC();

    @Override
    public void onActivate() {
        if (!startOnEnable.get()) return;

        MusicSound type = getType();
        if (((MusicTrackerAccessor) mc.getMusicTracker()).getCurrent() == null) mc.getMusicTracker().play(type);
    }

    @Override
    public void onDeactivate() {
        if (stopOnDisable.get()) mc.getMusicTracker().stop();
        nullifyCurrentType();
    }

    @EventHandler
    private void onGameJoin(GameJoinedEvent event) {
        SoundInstance instance = ((MusicTrackerAccessor) mc.getMusicTracker()).getCurrent();
        if (instance != null) {
            MusicSound type = getType();
            if (type != mc.getMusicType()) {
                mc.getMusicTracker().stop();
                mc.getMusicTracker().play(type);
            }
        }

        if (mc.world != null) {
            lastDim = mc.world.getDimensionKey().toString();
        }
    }

    @EventHandler
    private void onDimensionChange(PacketEvent.Receive event) {
        if (mc.world == null) return;
        if (!(event.packet instanceof PlayerRespawnS2CPacket)) return;

        String dimensionType = mc.world.getDimensionKey().toString();
        if (lastDim != null) {
            if (!dimensionType.equals(lastDim)) {
                MusicSound type = getType();

                mc.getMusicTracker().stop();
                mc.getMusicTracker().play(type);
                lastDim = dimensionType;
            }
        }
    }
}
