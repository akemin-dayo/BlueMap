package de.bluecolored.bluemap.common.config;

import de.bluecolored.bluemap.common.BlueMapConfigProvider;
import de.bluecolored.bluemap.common.config.storage.StorageConfig;
import de.bluecolored.bluemap.common.serverinterface.ServerInterface;
import de.bluecolored.bluemap.core.BlueMap;
import de.bluecolored.bluemap.api.debug.DebugDump;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.util.Tristate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

@DebugDump
public class BlueMapConfigs implements BlueMapConfigProvider {

    private final ServerInterface serverInterface;
    private final ConfigManager configManager;

    private final CoreConfig coreConfig;
    private final WebserverConfig webserverConfig;
    private final WebappConfig webappConfig;
    private final PluginConfig pluginConfig;
    private final Map<String, MapConfig> mapConfigs;
    private final Map<String, StorageConfig> storageConfigs;

    public BlueMapConfigs(ServerInterface serverInterface) throws ConfigurationException {
        this(serverInterface, Path.of("bluemap"), Path.of("bluemap", "web"), true);
    }

    public BlueMapConfigs(ServerInterface serverInterface, Path defaultDataFolder, Path defaultWebroot, boolean usePluginConf) throws ConfigurationException {
        this.serverInterface = serverInterface;
        this.configManager = new ConfigManager(serverInterface.getConfigFolder());

        this.coreConfig = loadCoreConfig(defaultDataFolder);
        this.webappConfig = loadWebappConfig(defaultWebroot);
        this.webserverConfig = loadWebserverConfig(webappConfig.getWebroot());
        this.pluginConfig = usePluginConf ? loadPluginConfig() : new PluginConfig();
        this.storageConfigs = Collections.unmodifiableMap(loadStorageConfigs(webappConfig.getWebroot()));
        this.mapConfigs = Collections.unmodifiableMap(loadMapConfigs());
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    @Override
    public CoreConfig getCoreConfig() {
        return coreConfig;
    }

    @Override
    public WebappConfig getWebappConfig() {
        return webappConfig;
    }

    @Override
    public WebserverConfig getWebserverConfig() {
        return webserverConfig;
    }

    @Override
    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    @Override
    public Map<String, MapConfig> getMapConfigs() {
        return mapConfigs;
    }

    @Override
    public Map<String, StorageConfig> getStorageConfigs() {
        return storageConfigs;
    }

    private synchronized CoreConfig loadCoreConfig(Path defaultDataFolder) throws ConfigurationException {
        Path configFileRaw = Path.of("core");
        Path configFile = configManager.findConfigPath(configFileRaw);
        Path configFolder = configFile.getParent();

        if (!Files.exists(configFile)) {

            // determine render-thread preset (very pessimistic, rather let people increase it themselves)
            Runtime runtime = Runtime.getRuntime();
            int availableCores = runtime.availableProcessors();
            long availableMemoryMiB = runtime.maxMemory() / 1024L / 1024L;
            int presetRenderThreadCount = 1;
            if (availableCores >= 6 && availableMemoryMiB >= 4096)
                presetRenderThreadCount = 2;
            if (availableCores >= 10 && availableMemoryMiB >= 8192)
                presetRenderThreadCount = 3;

            try {
                Files.createDirectories(configFolder);
                Files.writeString(
                        configFolder.resolve("core.conf"),
                        configManager.loadConfigTemplate("/de/bluecolored/bluemap/config/core.conf")
                                .setConditional("metrics", serverInterface.isMetricsEnabled() == Tristate.UNDEFINED)
                                .setVariable("timestamp", LocalDateTime.now().withNano(0).toString())
                                .setVariable("version", BlueMap.VERSION)
                                .setVariable("data", formatPath(defaultDataFolder))
                                .setVariable("implementation", "bukkit")
                                .setVariable("render-thread-count", Integer.toString(presetRenderThreadCount))
                                .build(),
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
                );
            } catch (IOException | NullPointerException ex) {
                Logger.global.logWarning("Failed to create default core-configuration-file: " + ex);
            }
        }

        return configManager.loadConfig(configFileRaw, CoreConfig.class);
    }

    private synchronized WebserverConfig loadWebserverConfig(Path defaultWebroot) throws ConfigurationException {
        Path configFileRaw = Path.of("webserver");
        Path configFile = configManager.findConfigPath(configFileRaw);
        Path configFolder = configFile.getParent();

        if (!Files.exists(configFile)) {
            try {
                Files.createDirectories(configFolder);
                Files.writeString(
                        configFolder.resolve("webserver.conf"),
                        configManager.loadConfigTemplate("/de/bluecolored/bluemap/config/webserver.conf")
                                .setVariable("webroot", formatPath(defaultWebroot))
                                .build(),
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
                );
            } catch (IOException | NullPointerException ex) {
                Logger.global.logWarning("Failed to create default webserver-configuration-file: " + ex);
            }
        }

        return configManager.loadConfig(configFileRaw, WebserverConfig.class);
    }

    private synchronized WebappConfig loadWebappConfig(Path defaultWebroot) throws ConfigurationException {
        Path configFileRaw = Path.of("webapp");
        Path configFile = configManager.findConfigPath(configFileRaw);
        Path configFolder = configFile.getParent();

        if (!Files.exists(configFile)) {
            try {
                Files.createDirectories(configFolder);
                Files.writeString(
                        configFolder.resolve("webapp.conf"),
                        configManager.loadConfigTemplate("/de/bluecolored/bluemap/config/webapp.conf")
                                .setVariable("webroot", formatPath(defaultWebroot))
                                .build(),
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
                );
            } catch (IOException | NullPointerException ex) {
                Logger.global.logWarning("Failed to create default webapp-configuration-file: " + ex);
            }
        }

        return configManager.loadConfig(configFileRaw, WebappConfig.class);
    }

    private synchronized PluginConfig loadPluginConfig() throws ConfigurationException {
        Path configFileRaw = Path.of("plugin");
        Path configFile = configManager.findConfigPath(configFileRaw);
        Path configFolder = configFile.getParent();

        if (!Files.exists(configFile)) {
            try {
                Files.createDirectories(configFolder);
                Files.writeString(
                        configFolder.resolve("plugin.conf"),
                        configManager.loadConfigTemplate("/de/bluecolored/bluemap/config/plugin.conf")
                                .build(),
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
                );
            } catch (IOException | NullPointerException ex) {
                Logger.global.logWarning("Failed to create default webapp-configuration-file: " + ex);
            }
        }

        return configManager.loadConfig(configFileRaw, PluginConfig.class);
    }

    private synchronized Map<String, MapConfig> loadMapConfigs() throws ConfigurationException {
        Map<String, MapConfig> mapConfigs = new HashMap<>();

        Path mapFolder = Paths.get("maps");
        Path mapConfigFolder = configManager.getConfigRoot().resolve(mapFolder);

        if (!Files.exists(mapConfigFolder)){
            try {
                Files.createDirectories(mapConfigFolder);
                var worlds = serverInterface.getLoadedWorlds();
                if (worlds.isEmpty()) {
                    Files.writeString(
                            mapConfigFolder.resolve("overworld.conf"),
                            createOverworldMapTemplate("Overworld", Path.of("world")).build(),
                            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
                    );
                    Files.writeString(
                            mapConfigFolder.resolve("nether.conf"),
                            createNetherMapTemplate("Nether", Path.of("world", "DIM-1")).build(),
                            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
                    );
                    Files.writeString(
                            mapConfigFolder.resolve("end.conf"),
                            createEndMapTemplate("End", Path.of("world", "DIM1")).build(),
                            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
                    );
                } else {
                    for (var world : worlds) {
                        String name = world.getName().orElse(world.getDimension().getName());
                        Path worldFolder = world.getSaveFolder();
                        ConfigTemplate template;
                        switch (world.getDimension()) {
                            case NETHER: template = createNetherMapTemplate(name, worldFolder); break;
                            case END: template = createEndMapTemplate(name, worldFolder); break;
                            default: template = createOverworldMapTemplate(name, worldFolder); break;
                        }

                        Path configFile = mapConfigFolder.resolve(sanitiseMapId(name.toLowerCase(Locale.ROOT)) + ".conf");
                        int i = 1;
                        while (Files.exists(configFile)) {
                            configFile = mapConfigFolder.resolve(sanitiseMapId(name.toLowerCase(Locale.ROOT)) + '_' + (i++) + ".conf");
                        }

                        Files.writeString(
                                configFile,
                                template.build(),
                                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
                        );
                    }
                }
            } catch (IOException | NullPointerException ex) {
                throw new ConfigurationException("BlueMap failed to create default map-configuration-files in\n" +
                        mapConfigFolder.toAbsolutePath().normalize() + "\n" +
                        "Check if BlueMap has the permission to create and read from this folder.",
                        ex);
            }
        }

        try (Stream<Path> configFiles = Files.list(mapConfigFolder)) {
            for (var configFile : configFiles.toArray(Path[]::new)) {
                if (!configManager.isConfigFile(configFile)) continue;
                Path rawConfig = configManager.getRaw(configFile);
                String id = sanitiseMapId(rawConfig.getFileName().toString());

                if (mapConfigs.containsKey(id)) {
                    throw new ConfigurationException("At least two of your map-config file-names result in ambiguous map-id's!\n" +
                            configFile.toAbsolutePath().normalize() + "\n" +
                            "To resolve this issue, rename this file to something else.");
                }

                MapConfig mapConfig = configManager.loadConfig(rawConfig, MapConfig.class);
                mapConfigs.put(id, mapConfig);
            }
        } catch (IOException ex) {
            throw new ConfigurationException("BlueMap failed to read your map configuration from\n" +
                            mapConfigFolder.toAbsolutePath().normalize() + "\n" +
                            "Check if BlueMap has the permission to create and read from this folder.",
                    ex);
        }

        return mapConfigs;
    }

    private synchronized Map<String, StorageConfig> loadStorageConfigs(Path defaultWebroot) throws ConfigurationException {
        Map<String, StorageConfig> storageConfigs = new HashMap<>();

        Path storageFolder = Paths.get("storages");
        Path storageConfigFolder = configManager.getConfigRoot().resolve(storageFolder);

        if (!Files.exists(storageConfigFolder)){
            try {
                Files.createDirectories(storageConfigFolder);
                Files.writeString(
                        storageConfigFolder.resolve("file.conf"),
                        configManager.loadConfigTemplate("/de/bluecolored/bluemap/config/storages/file.conf")
                                .setVariable("root", formatPath(defaultWebroot.resolve("maps")))
                                .build(),
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
                );
                Files.writeString(
                        storageConfigFolder.resolve("sql.conf"),
                        configManager.loadConfigTemplate("/de/bluecolored/bluemap/config/storages/sql.conf").build(),
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
                );
            } catch (IOException | NullPointerException ex) {
                throw new ConfigurationException("BlueMap failed to create default storage-configuration-files in\n" +
                                storageConfigFolder.toAbsolutePath().normalize() + "\n" +
                                "Check if BlueMap has the permission to create and read from this folder.",
                        ex);
            }
        }


        try (Stream<Path> configFiles = Files.list(storageConfigFolder)) {
            for (var configFile : configFiles.toArray(Path[]::new)) {
                if (!configManager.isConfigFile(configFile)) continue;
                Path rawConfig = configManager.getRaw(configFile);
                String id = rawConfig.getFileName().toString();

                StorageConfig storageConfig = configManager.loadConfig(rawConfig, StorageConfig.class); // load superclass
                storageConfig = configManager.loadConfig(rawConfig, storageConfig.getStorageType().getConfigType()); // load actual config type

                storageConfigs.put(id, storageConfig);
            }
        } catch (IOException ex) {
            throw new ConfigurationException("BlueMap failed to read your map configuration from\n" +
                            storageConfigFolder.toAbsolutePath().normalize() + "\n" +
                            "Check if BlueMap has the permission to create and read from this folder.",
                    ex);
        }

        return storageConfigs;
    }

    private String sanitiseMapId(String id) {
        return id.replaceAll("\\W", "_");
    }

    private ConfigTemplate createOverworldMapTemplate(String name, Path worldFolder) throws IOException {
        return configManager.loadConfigTemplate("/de/bluecolored/bluemap/config/maps/map.conf")
                .setVariable("name", name)
                .setVariable("sorting", "0")
                .setVariable("world", formatPath(worldFolder))
                .setVariable("sky-color", "#7dabff")
                .setVariable("ambient-light", "0.1")
                .setVariable("world-sky-light", "15")
                .setVariable("remove-caves-below-y", "55")
                .setConditional("max-y-comment", true)
                .setVariable("max-y", "100");
    }

    private ConfigTemplate createNetherMapTemplate(String name, Path worldFolder) throws IOException {
        return configManager.loadConfigTemplate("/de/bluecolored/bluemap/config/maps/map.conf")
                .setVariable("name", name)
                .setVariable("sorting", "100")
                .setVariable("world", formatPath(worldFolder))
                .setVariable("sky-color", "#290000")
                .setVariable("ambient-light", "0.6")
                .setVariable("world-sky-light", "0")
                .setVariable("remove-caves-below-y", "-10000")
                .setConditional("max-y-comment", false)
                .setVariable("max-y", "90");
    }

    private ConfigTemplate createEndMapTemplate(String name, Path worldFolder) throws IOException {
        return configManager.loadConfigTemplate("/de/bluecolored/bluemap/config/maps/map.conf")
                .setVariable("name", name)
                .setVariable("sorting", "200")
                .setVariable("world", formatPath(worldFolder))
                .setVariable("sky-color", "#080010")
                .setVariable("ambient-light", "0.6")
                .setVariable("world-sky-light", "0")
                .setVariable("remove-caves-below-y", "-10000")
                .setConditional("max-y-comment", true)
                .setVariable("max-y", "100");
    }

    private String formatPath(Path path) {
        return Path.of("")
                .toAbsolutePath()
                .relativize(path.toAbsolutePath())
                .normalize()
                .toString()
                .replace("\\", "\\\\");
    }

}
