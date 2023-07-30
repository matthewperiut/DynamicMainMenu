package net.mine_diver.aethermainmenu;

import net.fabricmc.loader.api.FabricLoader;
import net.mine_diver.unsafeevents.listener.EventListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.SinglePlayerClientInteractionManager;
import net.minecraft.client.gui.screen.menu.MainMenu;
import net.minecraft.level.storage.LevelMetadata;
import net.minecraft.level.storage.LevelStorage;
import net.modificationstation.stationapi.api.event.mod.InitEvent;
import net.modificationstation.stationapi.api.mod.entrypoint.Entrypoint;
import net.modificationstation.stationapi.api.mod.entrypoint.EventBusPolicy;
import net.modificationstation.stationapi.api.registry.Identifier;
import net.modificationstation.stationapi.api.registry.ModID;
import net.modificationstation.stationapi.api.util.Null;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;

import static net.modificationstation.stationapi.api.registry.Identifier.of;

@Entrypoint(eventBus = @EventBusPolicy(registerInstance = false))
public class AetherMenu {

    @Entrypoint.ModID
    public static final ModID MODID = Null.get();

    @Entrypoint.Logger
    public static final Logger LOGGER = Null.get();

    // Identifiers
    public static Identifier modular;

    // Utility fields
    public static boolean modmenu;
    public static boolean captureSoundId;
    public static String musicId;
    public static long musicStartTimestamp;
    public static boolean needsPlayerCreation;
    public static boolean shouldWorldLoad = true;
    public static boolean replaceBgTile = true;
    public static boolean renderPlayer = true;

    public static boolean visibleWorldButton = false;

    public static String lastLevel;
    public static String toolTip = "HELLO";

    @EventListener
    private static void init(InitEvent event)
    {
        if (FabricLoader.getInstance().isModLoaded("modmenu"))
            modmenu = true;

        modular = of(MODID, "mainmenu.aether");

        CreateConfigFolder();
        AttemptLoadingSettings();
        AttemptLoadingLastLevel();
    }

    private static void CreateConfigFolder()
    {
        String filePath = FabricLoader.getInstance().getConfigDir().toString() + "/aether/";
        File folder = new File(filePath);

        if (!folder.exists()) {
            folder.mkdir();
        }
    }

    public static void SaveCurrentSettings()
    {
        String filePath = FabricLoader.getInstance().getConfigDir().toString() + "/aether/settings.txt";
        try {
            File file = new File(filePath);
            String fileContent  = "P " + renderPlayer + "\nW " + shouldWorldLoad + "\nT " + replaceBgTile;
            // Create directories recursively if needed
            boolean file_access = true;
            if (!file.exists()) {
                file_access = file.delete();
            }
            if (file_access)
            {
                file.getParentFile().mkdirs();
                file.createNewFile();
                FileWriter writer = new FileWriter(file);
                writer.write(fileContent);
                writer.close();
            }
        }
        catch (Exception e)
        {
            System.out.println("An error occurred: " + e.getMessage());
        }
    }

    private static void AttemptLoadingSettings()
    {
        String filePath = FabricLoader.getInstance().getConfigDir().toString() + "/aether/settings.txt";
        File file = new File(filePath);

        // Check if the file exists
        if (!file.exists()) {
            SaveCurrentSettings();
        }
        else
        {
            try {
                List<String> lines = Files.readAllLines(Path.of(filePath));
                for (String line : lines) {

                    if (line.length() < 2)
                        continue;

                    switch (line.charAt(0)) {
                        case 'P' -> renderPlayer = line.charAt(2) == 't';
                        case 'W' -> shouldWorldLoad = line.charAt(2) == 't';
                        case 'T' -> replaceBgTile = line.charAt(2) == 't';
                        default -> { LOGGER.log(org.apache.logging.log4j.Level.WARN, "Invalid line in \".minecraft/config/aether/settings.txt\":\n" + line); }
                    }
                }
            } catch (IOException e) {
                shouldWorldLoad = false;
            }
        }

    }

    public static void AttemptLoadingLastLevel()
    {
        Path configFolder = FabricLoader.getInstance().getConfigDir();
        Path lastsave = Path.of(configFolder.toString() + "/aether/lastsave.txt");
        try {
            List<String> lines = Files.readAllLines(lastsave);
            for (String line : lines) {
                lastLevel = line;
            }
        } catch (IOException e) {
            shouldWorldLoad = false;
        }
    }

    public static void LoadWorld(Minecraft minecraft) {
        List worlds;
        LevelStorage var1 = minecraft.getLevelStorage();
        worlds = var1.getMetadata();

        // Check if there are worlds to load
        if (worlds.size() > 0)
            visibleWorldButton = true;
        else
        {
            visibleWorldButton = false;
            return;
        }

        boolean found = false;
        // Preventing from generating new worlds
        if (shouldWorldLoad)
        {
            for (Object world : worlds)
            {
                if (((LevelMetadata)world).getLevelName().equals(lastLevel))
                {
                    found = true;
                    break;
                }
            }
        }

        // Load a world if previous world not found
        if (!found)
            lastLevel = ((LevelMetadata)worlds.get(0)).getFileName();

        if (minecraft.level == null && shouldWorldLoad)
        {
            minecraft.interactionManager = new SinglePlayerClientInteractionManager(minecraft);

            if(lastLevel != null)
            {
                minecraft.interactionManager = new SinglePlayerClientInteractionManager(minecraft);
                String name = lastLevel;//((LevelMetadata)worlds.get(0)).getFileName();
                minecraft.createOrLoadWorld(name, name, new Random().nextLong());
            }
        }
    }

    public static void quickLoad(Minecraft minecraft)
    {
        minecraft.openScreen(null);
        minecraft.interactionManager = new SinglePlayerClientInteractionManager(minecraft);
        minecraft.viewEntity = minecraft.player;
    }

    public static void startOrStopMenuWorld(Minecraft minecraft)
    {
        if (minecraft.level == null)
        {
            // loading
            needsPlayerCreation = true;
            shouldWorldLoad = true;
            LoadWorld(minecraft);
        }
        else
        {
            // quitting
            shouldWorldLoad = false;
            if (minecraft.hasLevel())
            {

                minecraft.level.disconnect();
            }

            minecraft.setLevel(null);
            minecraft.openScreen(new MainMenu());
        }
    }
}
