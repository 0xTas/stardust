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
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;


/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class MusicTweaks extends Module {
    public MusicTweaks() {
        super(Stardust.CATEGORY, "MusicTweaks", "Allows you to fuck with the background music.");
        this.runInMainMenu = true;
    }

    public enum DisplayType {
        Chat, Record
    }

    private final SettingGroup sgPitch = settings.createGroup("Pitch");
    private final SettingGroup sgVolume = settings.createGroup("Volume");
    private final SettingGroup sgCooldown = settings.createGroup("Cooldown");
    private final SettingGroup sgTypes = settings.createGroup("Soundtracks");
    private final SettingGroup sgNowPlaying = settings.createGroup("Now Playing");

    private final Setting<Boolean> gameMusic = sgTypes.add(
        new BoolSetting.Builder()
            .name("Survival")
            .description("Plays songs from the overworld survival gamemode soundtrack.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> creativeMusic = sgTypes.add(
        new BoolSetting.Builder()
            .name("Creative")
            .description("Plays songs from the creative gamemode soundtrack.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> underwaterMusic = sgTypes.add(
        new BoolSetting.Builder()
            .name("Underwater")
            .description("Plays songs from the underwater soundtrack.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> netherMusic = sgTypes.add(
        new BoolSetting.Builder()
            .name("Nether")
            .description("Plays songs from the Nether dimension soundtrack.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> endMusic = sgTypes.add(
        new BoolSetting.Builder()
            .name("End")
            .description("Plays the End Dimension soundtrack.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> bossMusic = sgTypes.add(
        new BoolSetting.Builder()
            .name("Dragon")
            .description("Plays the End Dragon boss soundtrack.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> creditsMusic = sgTypes.add(
        new BoolSetting.Builder()
            .name("Credits")
            .description("Plays the end credits soundtrack.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> menuMusic = sgTypes.add(
        new BoolSetting.Builder()
            .name("Menu")
            .description("Plays songs from the main menu soundtrack.")
            .defaultValue(false)
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
            .sliderRange(0, 600)
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
            .defaultValue(420)
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
            .name("Song Pitch % Adjustment")
            .description("Desired pitch adjustment %.")
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


    // See MusicTrackerMixin.java && SoundSystemMixin.java && PositionedSoundInstanceMixin.java
    //     && MinecraftClientMixin.java
    @Nullable
    public MusicSound getTypes() {
        if (this.currentType != null) return this.currentType;

        int min;
        int max;
        if (mc.player == null) {
            min = 69;
            max = 420;
        } else if (overrideDelayMode.get()) {
            min = this.getTimeUntilNextSong();
            max = this.getTimeUntilNextSong();
        } else {
            min = minTimeUntilNextSong.get() * 20;
            max = maxTimeUntilNextSong.get() * 20;
        }

        if (max <= min) max = min + 1;
        List<MusicSound> types = new ArrayList<>();
        if (gameMusic.get()) {
            types.add(new MusicSound(SoundEvents.MUSIC_GAME, min, ThreadLocalRandom.current().nextInt(min, max), false));
            types.add(new MusicSound(SoundEvents.MUSIC_OVERWORLD_FOREST, min, ThreadLocalRandom.current().nextInt(min, max), false));
            types.add(new MusicSound(SoundEvents.MUSIC_OVERWORLD_DRIPSTONE_CAVES, min, ThreadLocalRandom.current().nextInt(min, max), false));
            types.add(new MusicSound(SoundEvents.MUSIC_OVERWORLD_DEEP_DARK, min, ThreadLocalRandom.current().nextInt(min, max), false));
            types.add(new MusicSound(SoundEvents.MUSIC_OVERWORLD_CHERRY_GROVE, min, ThreadLocalRandom.current().nextInt(min, max), false));
            types.add(new MusicSound(SoundEvents.MUSIC_OVERWORLD_OLD_GROWTH_TAIGA, min, ThreadLocalRandom.current().nextInt(min, max), false));
            types.add(new MusicSound(SoundEvents.MUSIC_OVERWORLD_JAGGED_PEAKS, min, ThreadLocalRandom.current().nextInt(min, max), false));
        }
        if (netherMusic.get()) {
            types.add(new MusicSound(SoundEvents.MUSIC_NETHER_BASALT_DELTAS, min, ThreadLocalRandom.current().nextInt(min, max), false));
            types.add(new MusicSound(SoundEvents.MUSIC_NETHER_CRIMSON_FOREST, min, ThreadLocalRandom.current().nextInt(min, max), false));
            types.add(new MusicSound(SoundEvents.MUSIC_NETHER_NETHER_WASTES, min, ThreadLocalRandom.current().nextInt(min, max), false));
            types.add(new MusicSound(SoundEvents.MUSIC_NETHER_WARPED_FOREST, min, ThreadLocalRandom.current().nextInt(min, max), false));
            types.add(new MusicSound(SoundEvents.MUSIC_NETHER_SOUL_SAND_VALLEY, min, ThreadLocalRandom.current().nextInt(min, max), false));
        }
        if (creativeMusic.get()) types.add(new MusicSound(SoundEvents.MUSIC_CREATIVE, min, ThreadLocalRandom.current().nextInt(min, max), false));
        if (underwaterMusic.get()) types.add(new MusicSound(SoundEvents.MUSIC_UNDER_WATER, min, ThreadLocalRandom.current().nextInt(min, max), false));
        if (endMusic.get()) types.add(new MusicSound(SoundEvents.MUSIC_END, min, ThreadLocalRandom.current().nextInt(min, max), false));
        if (menuMusic.get()) types.add(new MusicSound(SoundEvents.MUSIC_MENU, min, ThreadLocalRandom.current().nextInt(min, max), false));
        if (creditsMusic.get()) types.add(new MusicSound(SoundEvents.MUSIC_CREDITS, min, ThreadLocalRandom.current().nextInt(min, max), false));
        if (bossMusic.get()) types.add(new MusicSound(SoundEvents.MUSIC_DRAGON, min, ThreadLocalRandom.current().nextInt(min, max), false));

        MusicSound type;
        if (!types.isEmpty()) {
            if (types.size() > 1) {
                type = types.get(ThreadLocalRandom.current().nextInt(types.size()));
            } else {
                type = types.get(0);
            }
        } else {
            return null;
        }

        this.currentType = type;
        return type;
    }

    public String getSongName(String songID) {
        String songName;
        switch (songID) {
            case "calm1.ogg" -> songName = "C418 - Minecraft";
            case "calm2.ogg" -> songName = "C418 - Clark";
            case "calm3.ogg" -> songName = "C418 - Sweden";
            case "creative1.ogg" -> songName = "C418 - Biome Fest";
            case "creative2.ogg" -> songName = "C418 - Blind Spots";
            case "creative3.ogg" -> songName = "C418 - Haunt Muskie";
            case "creative4.ogg" -> songName = "C418 - Aria Math";
            case "creative5.ogg" -> songName = "C418 - Dreiton";
            case "creative6.ogg" -> songName = "C418 - Taswell";
            case "hal1.ogg" -> songName = "C418 - Subwoofer Lullaby";
            case "hal2.ogg" -> songName = "C418 - Living Mice";
            case "hal3.ogg" -> songName = "C418 - Haggstrom";
            case "hal4.ogg" -> songName = "C418 - Danny";
            case "nuance1.ogg" -> songName = "C418 - Key";
            case "nuance2.ogg" -> songName = "C418 - Oxygène";
            case "piano1.ogg" -> songName = "C418 - Dry Hands";
            case "piano2.ogg" -> songName = "C418 - Wet Hands";
            case "piano3.ogg" -> songName = "C418 - Mice on Venus";
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
            case "nether1.ogg" -> songName = "C418 - Concrete Halls";
            case "nether2.ogg" -> songName = "C418 - Dead Voxel";
            case "nether3.ogg" -> songName = "C418 - Warmth";
            case "nether4.ogg" -> songName = "C418 - Ballad of the Cats";
            case "chrysopoeia.ogg" -> songName = "Lena Raine - Chrysopoeia";
            case "rubedo.ogg" -> songName = "Lena Raine - Rubedo";
            case "so_below.ogg" -> songName = "Lena Raine - So Below";
            case "boss.ogg" -> songName = "C418 - Boss";
            case "end.ogg" -> songName = "C418 - The End";
            case "menu1.ogg" -> songName = "C418 - Mutation";
            case "menu2.ogg" -> songName = "C418 - Moog City 2";
            case "menu3.ogg" -> songName = "C418 - Beginning 2";
            case "menu4.ogg" -> songName = "C418 - Floating Trees";
            case "credits.ogg" -> songName = "C418 - Alpha";
            default -> songName = "Unknown Track";
        }

        return songName;
    }

    public float getNextPitchStep(float currentPitch) {
        if (this.lastDirection == null) {
            this.lastDirection = PitchDirection.Descending;
            float intensity = -(pitchIntensity.get() / 10000f);
            return MathHelper.clamp(currentPitch + (currentPitch * intensity), -5f, 5f);
        }

        switch (this.lastDirection) { // Lmao
            case Ascending -> {
                float weightedChance = ThreadLocalRandom.current().nextFloat(0, 1);

                float intensity;
                if (weightedChance <= (weightedChanceSetting.get() / 100f)) {
                    intensity = pitchIntensity.get() / 10000f;
                } else {
                    intensity = -(pitchIntensity.get() / 10000f);
                    this.lastDirection = PitchDirection.Descending;
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
                    this.lastDirection = PitchDirection.Ascending;
                }
                return MathHelper.clamp(currentPitch + (currentPitch * intensity), -5f, 5f);
            }
        }
        return currentPitch;
    }

    public void sendNowPlayingMessage(String songName) {
        if (mc.player == null) return;
        ((IChatHud) mc.inGameHud.getChatHud()).meteor$add(Text.of("§8<"+ StardustUtil.rCC()+"§o✨§r§8> §2§oNow Playing§r§7: §5§o"+songName+"§r§7."), songName.hashCode());
    }

    public MinecraftClient getClient() { return this.mc; }
    public boolean randomPitch() { return randomPitch.get(); }
    public void nullifyCurrentType() { this.currentType = null; }
    public boolean trippyPitch() { return trippyPitchSetting.get(); }
    public float getVolumeAdjustment() { return volume.get() / 100f; }
    public boolean overrideDelay() { return overrideDelayMode.get(); }
    public DisplayType getDisplayMode() { return displayTypeSetting.get(); }
    public int getTimeUntilNextSong() { return timeUntilNextSong.get() * 20; }
    public float getPitchAdjustment() { return pitchAdjustment.get() / 1000f; }
    public boolean shouldDisplayNowPlaying() { return displayNowPlaying.get(); }
    public float getRandomPitch() { return ThreadLocalRandom.current().nextFloat(-pitchRange.get() / 1000f, pitchRange.get() / 1000f); }

    private enum PitchDirection {
        Ascending, Descending
    }

    @Nullable
    private MusicSound currentType = null;
    @Nullable
    private PitchDirection lastDirection = null;

    @Override
    public void onActivate() {
        MusicSound type = this.getTypes();

        if (type == null) return;
        if (((MusicTrackerAccessor) mc.getMusicTracker()).getCurrent() == null) mc.getMusicTracker().play(type);
    }

    @Override
    public void onDeactivate() {
        if (stopOnDisable.get()) mc.getMusicTracker().stop();
        this.currentType = null;
    }

    @EventHandler
    private void onGameJoin(GameJoinedEvent event) {
        MusicSound type = this.getTypes();

        if (type == null) return;
        SoundInstance instance = ((MusicTrackerAccessor) mc.getMusicTracker()).getCurrent();
        if (instance != null) {
            if (type != mc.getMusicType()) {
                mc.getMusicTracker().stop();
                mc.getMusicTracker().play(type);
            }
        }
    }
}
