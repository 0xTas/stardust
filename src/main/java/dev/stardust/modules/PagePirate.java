package dev.stardust.modules;

import java.util.*;
import java.time.LocalDate;
import java.time.LocalTime;
import net.minecraft.item.*;
import net.minecraft.text.*;
import dev.stardust.Stardust;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.util.Hand;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.Box;
import java.util.stream.Collectors;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtCompound;
import dev.stardust.util.StardustUtil;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.MathHelper;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import net.minecraft.network.packet.c2s.play.BookUpdateC2SPacket;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.systems.modules.render.blockesp.ESPBlockData;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class PagePirate extends Module {
    public PagePirate() { super(Stardust.CATEGORY, "PagePirate", "Pirates books that are held by other players."); }

    private final SettingGroup sgChat = settings.createGroup("Chat Display");
    private final SettingGroup sgCopy = settings.createGroup("Physical Copy");
    private final SettingGroup sgESP = settings.createGroup("ESP Settings");

    private final Setting<Boolean> chatDisplay = sgChat.add(
        new BoolSetting.Builder()
            .name("chat-display")
            .description("Write nearby books to your chat for inspection.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> displayBooksOnGround = sgChat.add(
        new BoolSetting.Builder()
            .name("display-books-on-ground")
            .description("Display the contents of books laying on the ground in your chat.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> displayBooksInItemFrames = sgChat.add(
        new BoolSetting.Builder()
            .name("display-books-in-item-frames")
            .description("Display the contents of books in item frames.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> deobfuscatePages = sgChat.add(
        new BoolSetting.Builder()
            .name("deobfuscate-contents-in-chat")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> localCopy = sgCopy.add(
        new BoolSetting.Builder()
            .name("physical-copy")
            .description("Write nearby books into a Book & Quill from your inventory for inspection.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> finalizeCopy = sgCopy.add(
        new BoolSetting.Builder()
            .name("sign-local-copy")
            .description("Sign the local pirated copy with the name of the original book.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> writeCoverPage = sgCopy.add(
        new BoolSetting.Builder()
            .name("write-cover-page")
            .description("Writes a cover page with metadata about the pirated book.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> copyBooksOnGround = sgCopy.add(
        new BoolSetting.Builder()
            .name("copy-books-on-ground")
            .description("Copy books that are laying on the ground as an item.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> copyBooksInItemFrames = sgCopy.add(
        new BoolSetting.Builder()
            .name("copy-books-in-item-frames")
            .description("Copy the contents of books in item frames.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> overwrite = sgCopy.add(
        new BoolSetting.Builder()
            .name("overwrite-book-&-quill")
            .description("Overwrite Book & Quills that already contain page content.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> tickDelay = sgCopy.add(
        new IntSetting.Builder()
            .name("tick-delay")
            .description("Required to avoid being kicked when copying multiple books at once.")
            .range(0, 500).sliderRange(40, 200)
            .defaultValue(40)
            .build()
    );

    private final Setting<Boolean> espItemFrames = sgESP.add(
        new BoolSetting.Builder()
            .name("ESP-item-frames")
            .description("Renders item frames containing written books or book & quills.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> espBooksOnGround = sgESP.add(
        new BoolSetting.Builder()
            .name("ESP-books-on-ground")
            .description("Render books that are laying on the ground as an item.")
            .defaultValue(true)
            .build()
    );

    private final Setting<ESPBlockData> bookESP = sgESP.add(
        new GenericSetting.Builder<ESPBlockData>()
            .name("book-entity-ESP")
            .defaultValue(
                new ESPBlockData(
                    ShapeMode.Both,
                    new SettingColor(69, 42, 242, 255),
                    new SettingColor(69, 42, 242, 44),
                    true,
                    new SettingColor(69, 42, 242, 137)
                )
            )
            .build()
    );

    private int timer = 0;
    private final HashSet<String> seenPages = new HashSet<>();
    private final HashSet<ItemEntity> booksOnGround = new HashSet<>();
    private final ArrayDeque<PirateTask> jobQueue = new ArrayDeque<>();
    private final HashSet<ItemFrameEntity> booksInItemFrames = new HashSet<>();
    private final HashMap<String, ArrayList<String>> seenBooks = new HashMap<>();

    private String formatPageText(String page) {
        String formattedPage = page.replaceAll("\\\\n", "~pgprte~newline~");
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

    private boolean bookAndQuillHasContent(ItemStack book) {
        if (book.getItem() != Items.WRITABLE_BOOK) return false;

        NbtCompound metadata = book.getNbt();
        if (metadata == null || !metadata.contains("pages", NbtElement.LIST_TYPE)) return false;

        NbtList content = metadata.getList("pages", NbtElement.STRING_TYPE);

        final List<String> pages = new ArrayList<>();
        for (int n = 0; n < content.size(); n++) {
            pages.add(content.getString(n));
        }

        return pages.stream().anyMatch(page -> !page.trim().isBlank());
    }

    private boolean equipBookAndQuill() {
        FindItemResult result = InvUtils.find(stack -> {
            if (stack.getItem() instanceof WritableBookItem) {
                NbtCompound metadata = stack.getNbt();
                if (metadata == null || !metadata.contains("pages", NbtElement.LIST_TYPE)) return false;

                NbtList content = metadata.getList("pages", NbtElement.STRING_TYPE);

                final List<String> pageList = new ArrayList<>();
                for (int n = 0; n < content.size(); n++) {
                    pageList.add(content.getString(n));
                }

                return overwrite.get()
                    || pageList.stream()
                    .map(this::formatPageText)
                    .map(this::decodeUnicodeChars)
                    .allMatch(page -> page.replace("~pgprte~newline~", " ").trim().isEmpty());
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
                    FindItemResult nonCriticalSlot = InvUtils.find(stack -> !(stack.getItem() instanceof ToolItem) && !stack.isFood());
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

    private boolean itemFrameHasBook(ItemFrameEntity itemFrame) {
        ItemStack stack = itemFrame.getHeldItemStack();
        return stack.getItem() == Items.WRITTEN_BOOK || bookAndQuillHasContent(stack);
    }

    private void handleWrittenBook(ItemStack book, String piratedFrom) {
        NbtCompound metadata = book.getNbt();
        if (metadata == null || !metadata.contains("pages", NbtElement.LIST_TYPE)) return;
        if (!metadata.contains("title", NbtElement.STRING_TYPE) || !metadata.contains("author", NbtElement.STRING_TYPE)) return;

        String title = metadata.getString("title");
        String author = metadata.getString("author");
        NbtList content = metadata.getList("pages", NbtElement.STRING_TYPE);

        final List<String> pages = new ArrayList<>();
        for (int n = 0; n < content.size(); n++) {
            pages.add(content.getString(n));
        }

        String pageText = pages.stream()
            .map(this::formatPageText)
            .map(this::decodeUnicodeChars)
            .collect(Collectors.joining("\n"));

        if (seenPages.contains(pageText.replace("~pgprte~newline~", "\n")) && seenBooks.containsKey(author) && seenBooks.get(author).contains(title)) return;

        seenPages.add(pageText.replace("~pgprte~newline~", "\n"));
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

            switch (piratedFrom) {
                case "on ground" -> {
                    if (displayBooksOnGround.get()) {
                        mc.player.sendMessage(
                            Text.literal(
                                "§8[§a§oPagePirate§8] §7Author: "
                                    +StardustUtil.rCC()+"§o"+author+" §7Title: "
                                    +StardustUtil.rCC()+"§5§o"+title+" §7Pages: \n§o"+pageText.replace("~pgprte~newline~", "\n")
                            )
                        );
                    }
                }
                case "item frame" -> {
                    if (displayBooksInItemFrames.get()) {
                        mc.player.sendMessage(
                            Text.literal(
                                "§8[§a§oPagePirate§8] §7Author: "
                                    +StardustUtil.rCC()+"§o"+author+" §7Title: "
                                    +StardustUtil.rCC()+"§5§o"+title+" §7Pages: \n§o"+pageText.replace("~pgprte~newline~", "\n")
                            )
                        );
                    }
                }
                default -> mc.player.sendMessage(
                    Text.literal(
                        "§8[§a§oPagePirate§8] §7Author: "
                            +StardustUtil.rCC()+"§o"+author+" §7Title: "
                            +StardustUtil.rCC()+"§5§o"+title+" §7Pages: \n§o"+pageText.replace("~pgprte~newline~", "\n")
                    )
                );
            }
        }
        if (localCopy.get()) {
            switch (piratedFrom) {
                case "on ground" -> {
                    if (copyBooksOnGround.get()) {
                        jobQueue.addLast(new PirateTask(metadata, piratedFrom, pages));
                    }
                }
                case "item frame" -> {
                    if (copyBooksInItemFrames.get()) {
                        jobQueue.addLast(new PirateTask(metadata, piratedFrom, pages));
                    }
                }
                default -> jobQueue.addLast(new PirateTask(metadata, piratedFrom, pages));
            }
        }
    }

    private void handleBookAndQuill(ItemStack book, String piratedFrom) {
        NbtCompound metadata = book.getNbt();
        if (metadata == null || !metadata.contains("pages", NbtElement.LIST_TYPE)) return;

        NbtList content = metadata.getList("pages", NbtElement.STRING_TYPE);

        final List<String> pages = new ArrayList<>();
        for (int n = 0; n < content.size(); n++) {
            pages.add(content.getString(n));
        }

        String pageText = pages.stream()
            .map(this::formatPageText)
            .map(this::decodeUnicodeChars)
            .collect(Collectors.joining("\n"));

        if (seenPages.contains(pageText.replace("~pgprte~newline~", "\n"))) return;

        seenPages.add(pageText.replace("~pgprte~newline~", "\n"));
        if (chatDisplay.get() && !pageText.replace("~pgprte~newline~", " ").isBlank()) {
            if (deobfuscatePages.get()) {
                pageText = pageText.replace("§k", "");
            }

            switch (piratedFrom) {
                case "on ground" -> {
                    if (displayBooksOnGround.get()) {
                        mc.player.sendMessage(
                            Text.literal("§8[§a§oPagePirate§8] §7Unsigned Contents from §a§o"+ piratedFrom+"§7: \n§7§o"+pageText.replace("~pgprte~newline~", "\n"))
                        );
                    }
                }
                case "item frame" -> {
                    if (displayBooksInItemFrames.get()) {
                        mc.player.sendMessage(
                            Text.literal("§8[§a§oPagePirate§8] §7Unsigned Contents from §a§o"+ piratedFrom+"§7: \n§7§o"+pageText.replace("~pgprte~newline~", "\n"))
                        );
                    }
                }
                default -> mc.player.sendMessage(Text.literal("§8[§a§oPagePirate§8] §7Unsigned Contents from §a§o"+ piratedFrom+"§7: \n§7§o"+pageText.replace("~pgprte~newline~", "\n")));
            }
        }
        if (localCopy.get()) {
            switch (piratedFrom) {
                case "on ground" -> {
                    if (copyBooksOnGround.get()) {
                        jobQueue.addLast(new PirateTask(null, piratedFrom, pages));
                    }
                }
                case "item frame" -> {
                    if (copyBooksInItemFrames.get()) {
                        jobQueue.addLast(new PirateTask(null, piratedFrom, pages));
                    }
                }
                default -> jobQueue.addLast(new PirateTask(null, piratedFrom, pages));
            }

        }
    }

    private void makeLocalCopy(NbtCompound metadata, List<String> pages, String piratedFrom) {
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
        if (writeCoverPage.get() && metadata != null && metadata.contains("title", NbtElement.STRING_TYPE) && metadata.contains("author", NbtElement.STRING_TYPE)) {
            String rcc = StardustUtil.rCC();
            LocalDate currentDate = LocalDate.now();
            LocalTime currentTime = LocalTime.now();
            String title = metadata.getString("title");
            String author = metadata.getString("author");

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
                title + "\n§0§lAuthor: "+rcc+"§o" + author +
                "\n\n" + "§0§lPirated From: "+rcc+"§o" + piratedFrom + "\n§0§oat "+rcc+"§o" +
                paddedHour + "§0§o:"+rcc+"§o" + paddedMinute + " §0§oon the "+rcc +
                "§o" + dayOfMonthSuffix(currentDate.getDayOfMonth()) +
                " §0§oof "+rcc+"§o" + currentDate.getMonth().toString().charAt(0) +
                currentDate.getMonth().toString().substring(1).toLowerCase() + "§0§o, "+rcc+"§o" + currentDate.getYear() + "§0§o.";

            piratedPages.add(coverPage);
        }
        int slot = mc.player.getInventory().selectedSlot;
        mc.player.sendMessage(Text.literal("§8[§a§oPagePirate§8] §7Successfully copied nearby book!"));
        boolean shouldSign = finalizeCopy.get() && metadata != null && metadata.contains("title", NbtElement.STRING_TYPE);
        piratedPages.addAll(filtered.stream().map(page -> page.replace("~pgprte~newline~", "\n")).toList());
        mc.getNetworkHandler().sendPacket(new BookUpdateC2SPacket(slot, piratedPages, shouldSign ? Optional.of(metadata.getString("title")) : Optional.empty()));
    }

    @Override
    public void onDeactivate() {
        timer = 0;
        jobQueue.clear();
        seenBooks.clear();
        seenPages.clear();
        booksOnGround.clear();
        booksInItemFrames.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!Utils.canUpdate()) return;
        booksInItemFrames.removeIf(frame -> !itemFrameHasBook(frame));
        booksOnGround.removeIf(book -> book.isRemoved() || book.isRegionUnloaded());
        booksInItemFrames.removeIf(frame -> frame.isRemoved() || frame.isRegionUnloaded());

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
                if (booksOnGround.contains(item)) continue;
                String piratedFrom = "on ground";
                if (item.getStack().getItem() == Items.WRITTEN_BOOK) {
                    booksOnGround.add(item);
                    handleWrittenBook(item.getStack(), piratedFrom);
                }
                else if (bookAndQuillHasContent(item.getStack())) {
                    booksOnGround.add(item);
                    handleBookAndQuill(item.getStack(), piratedFrom);
                }
            } else if (entity instanceof ItemFrameEntity itemFrame) {
                if (booksInItemFrames.contains(itemFrame)) continue;
                ItemStack stack = itemFrame.getHeldItemStack();
                if (stack.getItem() == Items.WRITTEN_BOOK) {
                    booksInItemFrames.add(itemFrame);
                    handleWrittenBook(stack, "item frame");
                }
                else if (stack.getItem() == Items.WRITABLE_BOOK) {
                    booksInItemFrames.add(itemFrame);
                    handleBookAndQuill(stack, "item frame");
                }
            }
        }

        ++timer;
        if (timer >= tickDelay.get() && !jobQueue.isEmpty()) {
            timer = 0;
            PirateTask task = jobQueue.removeFirst();
            makeLocalCopy(task.getData(), task.getPages(), task.getPiratedFrom());
        }
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (!Utils.canUpdate()) return;
        ESPBlockData esp = bookESP.get();
        if (espItemFrames.get()) {
            for (ItemFrameEntity frame : booksInItemFrames) {
                Box box = frame.getBoundingBox();
                double x = MathHelper.lerp(event.tickDelta, frame.lastRenderX, frame.getX()) - frame.getX();
                double y = MathHelper.lerp(event.tickDelta, frame.lastRenderY, frame.getY()) - frame.getY();
                double z = MathHelper.lerp(event.tickDelta, frame.lastRenderZ, frame.getZ()) - frame.getZ();

                double x1 = x + box.minX;
                double y1 = y + box.minY;
                double z1 = z + box.minZ;
                double x2 = x + box.maxX;
                double y2 = y + box.maxY;
                double z2 = z + box.maxZ;
                if (esp.sideColor.a > 0 || esp.lineColor.a > 0) {
                    event.renderer.box(
                        x1, y1, z1, x2, y2, z2,
                        esp.sideColor, esp.lineColor, esp.shapeMode, 0
                    );
                }
                if (esp.tracer) {
                    event.renderer.line(
                        RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z,
                        (x1 + x2) / 2, (y1 + y2) / 2, (z1 + z2) / 2, esp.tracerColor
                    );
                }
            }
        }

        if (espBooksOnGround.get()) {
            for (ItemEntity book : booksOnGround) {
                Box box = book.getBoundingBox();
                double x = MathHelper.lerp(event.tickDelta, book.lastRenderX, book.getX()) - book.getX();
                double y = MathHelper.lerp(event.tickDelta, book.lastRenderY, book.getY()) - book.getY();
                double z = MathHelper.lerp(event.tickDelta, book.lastRenderZ, book.getZ()) - book.getZ();

                double x1 = x + box.minX;
                double y1 = y + box.minY;
                double z1 = z + box.minZ;
                double x2 = x + box.maxX;
                double y2 = y + box.maxY;
                double z2 = z + box.maxZ;
                if (esp.sideColor.a > 0 || esp.lineColor.a > 0) {
                    event.renderer.box(
                        x + box.minX, y + box.minY, z + box.minZ,
                        x + box.maxX, y + box.maxY, z + box.maxZ,
                        esp.sideColor, esp.lineColor, esp.shapeMode, 0
                    );
                }
                if (esp.tracer) {
                    event.renderer.line(
                        RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z,
                        (x1 + x2) / 2, (y1 + y2) / 2, (z1 + z2) / 2, esp.tracerColor
                    );
                }
            }
        }
    }

    @EventHandler
    private void onRespawnOrDimensionChange(PacketEvent.Receive event) {
        if (!(event.packet instanceof PlayerRespawnS2CPacket)) return;
        booksOnGround.clear();
        booksInItemFrames.clear();
    }

    private record PirateTask(NbtCompound metadata, String piratedFrom, List<String> pages) {
        public List<String> getPages() { return this.pages; }
        public String getPiratedFrom() { return this.piratedFrom; }
        public NbtCompound getData() { return this.metadata; }
    }
}
