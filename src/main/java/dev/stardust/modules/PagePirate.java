package dev.stardust.modules;

import java.util.*;
import java.time.LocalDate;
import java.time.LocalTime;
import net.minecraft.text.*;
import dev.stardust.Stardust;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.util.Hand;
import net.minecraft.item.Items;
import net.minecraft.item.ToolItem;
import java.util.stream.Collectors;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import dev.stardust.util.StardustUtil;
import net.minecraft.entity.ItemEntity;
import org.jetbrains.annotations.Nullable;
import net.minecraft.item.WritableBookItem;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.util.collection.ArrayListDeque;
import net.minecraft.client.network.ClientPlayerEntity;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.component.type.WritableBookContentComponent;
import net.minecraft.network.packet.c2s.play.BookUpdateC2SPacket;
import meteordevelopment.meteorclient.utils.player.FindItemResult;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class PagePirate extends Module {
    public PagePirate() { super(Stardust.CATEGORY, "PagePirate", "Pirates books that are held by other players."); }

    private final SettingGroup sgChat = settings.createGroup("Chat Display");
    private final SettingGroup sgCopy = settings.createGroup("Physical Copy");

    private final Setting<Boolean> chatDisplay = sgChat.add(
        new BoolSetting.Builder()
            .name("Chat Display")
            .description("Write nearby books to your chat for inspection.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> deobfuscatePages = sgChat.add(
        new BoolSetting.Builder()
            .name("Deobfuscate Contents")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> localCopy = sgCopy.add(
        new BoolSetting.Builder()
            .name("Physical Copy")
            .description("Write nearby books into a Book & Quill from your inventory for inspection.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> finalizeCopy = sgCopy.add(
        new BoolSetting.Builder()
            .name("Sign Local Copy")
            .description("Sign the local pirated copy with the name of the original book.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> copyBooksOnGround = sgCopy.add(
        new BoolSetting.Builder()
            .name("Copy Books on Ground")
            .description("Copy books that are laying on the ground as an item.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> overwrite = sgCopy.add(
        new BoolSetting.Builder()
            .name("Overwrite Book & Quill")
            .description("Overwrite Book & Quills that already contain page content.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> tickDelay = sgCopy.add(
        new IntSetting.Builder()
            .name("Tick Delay")
            .description("Required to avoid being kicked when copying multiple books at once.")
            .range(0, 500).sliderRange(40, 200)
            .defaultValue(40)
            .build()
    );

    private int timer = 0;
    private final HashSet<String> seenPages = new HashSet<>();
    private final ArrayListDeque<PirateTask> jobQueue = new ArrayListDeque<>();
    private final HashMap<String, ArrayList<String>> seenBooks = new HashMap<>();

    private String formatPageText(String page) {
        String formattedPage = page.replaceAll("\\\\n", " ");
        formattedPage = formattedPage
            .replace("{\"text\":\"", "")
            .replaceAll("(?m)\"}$", "");

        return formattedPage.trim();
    }

    private String decodeUnicodeChars(String page) {
        Pattern pattern = Pattern.compile("\\\\u[0-9a-fA-F]{4}");
        Matcher matcher = pattern.matcher(page);

        StringBuilder decodedPage = new StringBuilder();

        while (matcher.find()) {
            String unicode = matcher.group();
            char uChar = (char) Integer.parseInt(unicode.substring(2), 16);
            matcher.appendReplacement(decodedPage, Character.toString(uChar));
        }
        matcher.appendTail(decodedPage);

        return decodedPage.toString();
    }

    private String dayOfMonthSuffix(int dom) {
        String day = String.valueOf(dom);

        if (!day.endsWith("11") && day.endsWith("1")) {
            return day+"st";
        } else if (!day.endsWith("12") && day.endsWith("2")) {
            return day+"nd";
        }else if (!day.endsWith("13") && day.endsWith("3")) {
            return day+"rd";
        } else {
            return day+"th";
        }
    }

    private boolean equipBookAndQuill() {
        FindItemResult result = InvUtils.find(stack -> {
            if (stack.getItem() instanceof WritableBookItem) {
                WritableBookContentComponent data = stack.get(DataComponentTypes.WRITABLE_BOOK_CONTENT);
                List<String> pageList = data.pages().stream().map(RawFilteredPair::raw).toList();
                return overwrite.get()
                    || pageList.stream()
                        .map(this::formatPageText)
                        .map(this::decodeUnicodeChars)
                        .allMatch(page -> page.trim().isEmpty());
            }
            return false;
        });

        if (result.found()) {
            if (result.slot() < 9) {
                InvUtils.swap(result.slot(), true);
            } else {
                FindItemResult emptySlot = InvUtils.findEmpty();
                if (emptySlot.found() && emptySlot.slot() < 9) {
                    InvUtils.move().from(result.slot()).to(emptySlot.slot());
                    InvUtils.swap(emptySlot.slot(), true);
                } else {
                    FindItemResult nonCriticalSlot = InvUtils.find(stack -> !(stack.getItem() instanceof ToolItem) && !stack.contains(DataComponentTypes.FOOD));
                    if (nonCriticalSlot.found() && nonCriticalSlot.slot() < 9) {
                        InvUtils.move().from(result.slot()).to(nonCriticalSlot.slot());
                        InvUtils.swap(nonCriticalSlot.slot(), true);
                    } else {
                        InvUtils.move().from(result.slot()).to(mc.player.getInventory().selectedSlot);
                    }
                }
            }
            return true;
        }
        return false;
    }

    private void handleWrittenBook(ItemStack book, String piratedFrom) {
        if (book.contains(DataComponentTypes.WRITTEN_BOOK_CONTENT)) {
            WrittenBookContentComponent metadata = book.get(DataComponentTypes.WRITTEN_BOOK_CONTENT);

            String author = metadata.author();
            String title = metadata.title().raw();
            List<String> pages = metadata.getPages(false).stream().map(Text::getString).toList();

            String pageText = pages.stream()
                .map(this::formatPageText)
                .map(this::decodeUnicodeChars)
                .collect(Collectors.joining("\n"));

            if (seenPages.contains(pageText) && seenBooks.containsKey(author) && seenBooks.get(author).contains(title)) return;

            seenPages.add(pageText);
            if (seenBooks.containsKey(author)) {
                ArrayList<String> booksFromAuthor = seenBooks.get(author);
                booksFromAuthor.add(title);
                seenBooks.put(author, booksFromAuthor);
            } else {
                ArrayList<String> books = new ArrayList<>();
                books.add(title);
                seenBooks.put(author, books);
            }
            if (chatDisplay.get()) {
                if (deobfuscatePages.get()) {
                    pageText = pageText.replace("§k", "");
                }
                mc.player.sendMessage(
                    Text.literal(
                        "§8[§a§oPagePirate§8] §7Author: "
                            +StardustUtil.rCC()+"§o"+author+" §7Title: "
                            +StardustUtil.rCC()+"§5§o"+title+" §7Pages: \n§o"+pageText
                    )
                );
            }
            if (localCopy.get()) {
                if (copyBooksOnGround.get() || !piratedFrom.equals("on ground")) {
                    jobQueue.addLast(new PirateTask(metadata, piratedFrom, pages));
                }
            }
        }
    }

    private void handleBookAndQuill(ItemStack book, String piratedFrom) {
        if (!book.contains(DataComponentTypes.WRITABLE_BOOK_CONTENT)) return;
        WritableBookContentComponent metadata = book.get(DataComponentTypes.WRITABLE_BOOK_CONTENT);
        List<String> pages = metadata.pages().stream().map(p -> p.get(false)).toList();

        String pageText = pages.stream()
            .map(this::formatPageText)
            .map(this::decodeUnicodeChars)
            .collect(Collectors.joining("\n"));

        if (seenPages.contains(pageText)) return;

        seenPages.add(pageText);
        if (chatDisplay.get() && !pageText.isBlank()) {
            if (deobfuscatePages.get()) {
                pageText = pageText.replace("§k", "");
            }
            mc.player.sendMessage(
                Text.literal(
                    "§8[§a§oPagePirate§8] §7Unsigned Contents from §a§o"+ piratedFrom+"§7: \n§7§o"+pageText
                )
            );
        }
        if (localCopy.get()) {
            if (copyBooksOnGround.get() || !piratedFrom.equals("on ground")) {
                jobQueue.addLast(new PirateTask(null, piratedFrom, pages));
            }
        }
    }

    private void makeLocalCopy(@Nullable WrittenBookContentComponent metadata, List<String> pages, String piratedFrom) {
        ArrayList<String> filtered = new ArrayList<>(pages.stream()
            .map(this::formatPageText)
            .map(this::decodeUnicodeChars)
            .toList());
        if (filtered.isEmpty()) return;

        if (!equipBookAndQuill()) {
            mc.player.sendMessage(Text.literal("§8[§4§oPagePirate§8] §7Failed to copy nearby book because you have no empty Book & Quills§4..!"));
            return;
        }

        List<String> piratedPages = new ArrayList<>();
        if (metadata != null) {
            String rcc = StardustUtil.rCC();
            LocalDate currentDate = LocalDate.now();
            LocalTime currentTime = LocalTime.now();

            String paddedMinute, paddedHour;
            if (currentTime.getMinute() < 10) {
                paddedMinute = "0"+currentTime.getMinute();
            } else {
                paddedMinute = String.valueOf(currentTime.getMinute());
            }
            if (currentTime.getHour() < 10) {
                paddedHour = "0"+currentTime.getHour();
            } else {
                paddedHour = String.valueOf(currentTime.getHour());
            }
            String coverPage = "   "+rcc+"§o✨ PagePirate ✨ \n\n§0§lTitle: "+rcc+"§o" +
                metadata.title().raw() + "\n§0§lAuthor: "+rcc+"§o" + metadata.author() +
                "\n\n" + "§0§lPirated From: "+rcc+"§o" + piratedFrom + "\n§0§oat "+rcc+"§o" +
                paddedHour + "§0§o:"+rcc+"§o" + paddedMinute + " §0§oon the "+rcc +
                "§o" + dayOfMonthSuffix(currentDate.getDayOfMonth()) +
                " §0§oof "+rcc+"§o" + currentDate.getMonth().toString().charAt(0) +
                currentDate.getMonth().toString().substring(1).toLowerCase() + "§0§o, "+rcc+"§o" + currentDate.getYear() + "§0§o.";
            piratedPages.add(coverPage);
        }
        piratedPages.addAll(filtered);
        int slot = mc.player.getInventory().selectedSlot;
        boolean shouldSign = finalizeCopy.get() && metadata != null;
        mc.player.sendMessage(Text.literal("§8[§a§oPagePirate§8] §7Successfully copied nearby book!"));
        mc.getNetworkHandler().sendPacket(new BookUpdateC2SPacket(slot, piratedPages, shouldSign ? Optional.of(metadata.title().raw()) : Optional.empty()));
    }

    @Override
    public void onDeactivate() {
        timer = 0;
        jobQueue.clear();
        seenBooks.clear();
        seenPages.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!Utils.canUpdate()) return;
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof PlayerEntity player && !(entity instanceof ClientPlayerEntity)) {
                String name = player.getGameProfile().getName();
                ItemStack mainHand = player.getStackInHand(Hand.MAIN_HAND);
                if (mainHand.getItem() == Items.WRITTEN_BOOK) handleWrittenBook(mainHand, name);
                else if (mainHand.getItem() == Items.WRITABLE_BOOK) handleBookAndQuill(mainHand, name);

                ItemStack offHand = player.getStackInHand(Hand.OFF_HAND);
                if (offHand.getItem() == Items.WRITTEN_BOOK) handleWrittenBook(offHand, name);
                else if (mainHand.getItem() == Items.WRITABLE_BOOK) handleBookAndQuill(offHand, name);
            } else if (entity instanceof ItemEntity item) {
                String piratedFrom = "on ground";
                if (item.getStack().getItem() == Items.WRITTEN_BOOK) handleWrittenBook(item.getStack(), piratedFrom);
                else if (item.getStack().getItem() == Items.WRITABLE_BOOK) handleBookAndQuill(item.getStack(), piratedFrom);
            }
        }

        ++timer;
        if (timer >= tickDelay.get() && !jobQueue.isEmpty()) {
            timer = 0;
            PirateTask task = jobQueue.removeFirst();
            makeLocalCopy(task.getData(), task.getPages(), task.getPiratedFrom());
        }
    }

    private record PirateTask(@Nullable WrittenBookContentComponent metadata, String piratedFrom, List<String> pages) {
        public List<String> getPages() { return this.pages; }
        public String getPiratedFrom() { return this.piratedFrom; }
        public WrittenBookContentComponent getData() { return this.metadata; }
    }
}