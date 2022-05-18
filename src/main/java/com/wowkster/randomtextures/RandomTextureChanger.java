package com.wowkster.randomtextures;


import com.google.common.collect.Lists;
import com.wowkster.randomtextures.gui.GuiModOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.ResourcePackListEntry;
import net.minecraft.client.resources.ResourcePackListEntryFound;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Mod(modid = RandomTextureChanger.MODID, name = RandomTextureChanger.MOD_NAME, version = RandomTextureChanger.VERSION, acceptedMinecraftVersions = "[1.8.9]", clientSideOnly = true)
public class RandomTextureChanger {
    public static final String MODID = "randomtextures";
    public static final String MOD_NAME = "RandomTextureChanger";
    public static final String VERSION = "0.0.1";
    public static List<KeyBinding> keyBindings;
    public static boolean showDebugHud = false;
    public static boolean showDebugMessages = true;
    public static RandomTextureChanger instance;
    public static int timeInterval = 60; // Seconds
    private ResourcePackRepository.Entry currentPack;
    private boolean enabled = false;
    private boolean paused = false;
    private boolean showedMessage = false;
    private int numPacks = 0;

    private final Timer timer;

    public RandomTextureChanger() {
        instance = this;
        timer = new Timer();
        timer.reset();

        numPacks = getNumPacks();
    }

    private static ResourcePackListEntry chooseNewRandomPack(List<ResourcePackListEntry> availablePacks, List<ResourcePackListEntry> currentSelectedPacks) {
        int index = (int) (Math.random() * availablePacks.size());
        ResourcePackListEntry newPack = availablePacks.size() > 0 ? availablePacks.get(index) : null;

        if (currentSelectedPacks.stream().anyMatch(p -> p == newPack) && availablePacks.size() > 1)
            return chooseNewRandomPack(availablePacks, currentSelectedPacks);

        return newPack;
    }

    private static int getNumPacks() {
        ResourcePackRepository repository = Minecraft.getMinecraft().getResourcePackRepository();
        repository.updateRepositoryEntriesAll();
        List<ResourcePackRepository.Entry> allEntries = Lists.newArrayList(repository.getRepositoryEntriesAll());
        allEntries.removeAll(repository.getRepositoryEntries());

        return allEntries.size();
    }

    public static RandomTextureChanger getInstance() {
        return RandomTextureChanger.instance;
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance().bus().register(this);
        MinecraftForge.EVENT_BUS.register(this);

        keyBindings = new ArrayList<>();

        keyBindings.add(new KeyBinding("key.menu.desc", Keyboard.KEY_RSHIFT, "key.randomtextures.category"));

        for (KeyBinding key : keyBindings)
            ClientRegistry.registerKeyBinding(key);


    }

    public boolean getEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        timer.reset();

        if (enabled) {
            timer.start();
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent(priority = EventPriority.NORMAL, receiveCanceled = true)
    public void onTick(TickEvent.ClientTickEvent event) {
        if (enabled && !paused && timer.getSeconds() > timeInterval) {
            randomizeResourcePack();
            timer.reset();
            timer.start();
        } else if (enabled && !paused && timer.getSeconds() > timeInterval - 1 && !showedMessage) {
            showRandomizingMessage();
            showedMessage = true;
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent(priority = EventPriority.NORMAL, receiveCanceled = true)
    public void onTick(GuiOpenEvent event) {
        if (event.gui == null && Minecraft.getMinecraft().currentScreen != null) {
            if (paused) {
                paused = false;
                timer.start();
                return;
            }
        }

        if (event.gui != null) {
            if (!paused) {
                paused = true;
                timer.stop();
            }
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent(priority = EventPriority.NORMAL, receiveCanceled = true)
    public void onTick(RenderGameOverlayEvent event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) return;

        FontRenderer font = Minecraft.getMinecraft().fontRendererObj;

        if (Minecraft.getMinecraft().gameSettings.showDebugInfo) return;

        if (!showDebugHud) return;
        font.drawStringWithShadow("§bRandom Texture Changer", 10, 10, 0xffffff);
        font.drawStringWithShadow("§e# Packs Available: §7" + numPacks, 10, 20, 0xffffff);
        font.drawStringWithShadow("§eSelected Pack: §7" + (this.currentPack == null ? "None" : this.currentPack.getResourcePackName()), 10, 30, 0xffffff);
        font.drawStringWithShadow("§eTime Left: §7" + String.format("%.2fs", timeInterval - timer.getSeconds()), 10, 40, 0xffffff);
        font.drawStringWithShadow("§eCurrent Time: §7" + String.format("%.2fs", timer.getSeconds()), 10, 50, 0xffffff);
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent(priority = EventPriority.NORMAL, receiveCanceled = true)
    public void onEvent(InputEvent.KeyInputEvent event) {
        if (keyBindings.get(0).isPressed())
            Minecraft.getMinecraft().displayGuiScreen(new GuiModOptions());
    }

    public void setTimeInterval(int timeInterval) {
        RandomTextureChanger.timeInterval = timeInterval;
        timer.reset();
    }

    public void showRandomizingMessage() {
        if (!showDebugMessages) return;
        Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new ChatComponentText("§cR§6a§en§ad§bo§dm§ci§6z§ei§an§bg §fTexture Pack!"));
    }

    public void randomizeResourcePack() {
        long time = System.currentTimeMillis();
        Minecraft mc = Minecraft.getMinecraft();

        List<ResourcePackListEntry> availableResourcePacks = Lists.newArrayList();
        List<ResourcePackListEntry> selectedResourcePacks = Lists.newArrayList();

        // Find all packs

        ResourcePackRepository repository = mc.getResourcePackRepository();
        repository.updateRepositoryEntriesAll();
        List<ResourcePackRepository.Entry> allEntries = Lists.newArrayList(repository.getRepositoryEntriesAll());
        allEntries.removeAll(repository.getRepositoryEntries());

        for (ResourcePackRepository.Entry entry : allEntries) {
            availableResourcePacks.add(new ResourcePackListEntryFound(null, entry));
        }

        for (ResourcePackRepository.Entry entry : Lists.reverse(repository.getRepositoryEntries())) {
            selectedResourcePacks.add(new ResourcePackListEntryFound(null, entry));
        }

        // Pick Random Pack

        ResourcePackListEntry newPack = chooseNewRandomPack(availableResourcePacks, selectedResourcePacks);
        this.currentPack = ((ResourcePackListEntryFound) newPack).func_148318_i();

        // Change Pack

        List<ResourcePackRepository.Entry> newEntries = Lists.newArrayList();
        newEntries.add(((ResourcePackListEntryFound) newPack).func_148318_i());

        Collections.reverse(newEntries);
        mc.getResourcePackRepository().setRepositories(newEntries);
        mc.gameSettings.resourcePacks.clear();
        mc.gameSettings.incompatibleResourcePacks.clear();

        for (ResourcePackRepository.Entry entry : newEntries) {
            mc.gameSettings.resourcePacks.add(entry.getResourcePackName());
            if (entry.func_183027_f() != 1) {
                mc.gameSettings.incompatibleResourcePacks.add(entry.getResourcePackName());
            }
        }

        mc.gameSettings.saveOptions();
        mc.refreshResources();

        long elapsed = System.currentTimeMillis() - time;
        showedMessage = false;
        if (!showDebugMessages) return;
        Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(String.format("§eReloaded Textures in: §7%.2fs", ((double) elapsed) / 1000)));
        Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new ChatComponentText("§eNew Pack is: §7" + currentPack.getResourcePackName()));
    }
}