package net.matrixcreations.worldTemplateAPI;

import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.api.MVWorldManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * The WorldTemplateAPI class provides methods for creating, loading, and deleting world templates
 * in a Minecraft server using the Bukkit API and Multiverse-Core.
 */
public class WorldTemplateAPI {

    private final JavaPlugin plugin;
    private final MultiverseCore core;
    private final MVWorldManager worldManager;
    private final List<World> clonedWorlds;
    private Path templateDirectory;

    /**
     * Constructs a WorldTemplateAPI instance.
     *
     * @param plugin the JavaPlugin instance associated with this API
     * @throws IllegalStateException if Multiverse-Core is not found
     */
    public WorldTemplateAPI(JavaPlugin plugin) {
        this.plugin = plugin;

        core = (MultiverseCore) Bukkit.getServer().getPluginManager().getPlugin("Multiverse-Core");
        if (core == null) {
            throw new IllegalStateException("Multiverse-Core not found! Please install Multiverse-Core.");
        }
        worldManager = core.getMVWorldManager();
        clonedWorlds = new ArrayList<>();
    }

    /**
     * Allows the developer to specify a custom directory for storing world templates.
     *
     * @param directory the Path to the custom template directory
     */
    public void setTemplateDirectory(Path directory) {
        this.templateDirectory = directory;
    }

    /**
     * Creates a template from an existing world by copying its files to the template directory.
     *
     * @param worldName   the name of the world to copy from
     * @param templateName the name for the new template directory
     * @return a CompletableFuture indicating success or failure
     */
    public CompletableFuture<Boolean> createTemplateFromWorld(String worldName, String templateName) {
        World sourceWorld = Bukkit.getWorld(worldName);
        if (sourceWorld == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Source world '" + worldName + "' not found!"));
        }

        Path sourcePath = Paths.get(Bukkit.getWorldContainer().getPath(), worldName);
        Path targetPath = templateDirectory.resolve(templateName);

        return duplicateDirectory(sourcePath, targetPath);
    }

    /**
     * Loads a world from a template stored in the custom template directory.
     *
     * @param templateName   the name of the template to load
     * @param targetWorldName the name for the newly created world
     * @return a CompletableFuture containing the loaded World instance, or an exception if failed
     */
    public CompletableFuture<World> loadTemplate(String templateName, String targetWorldName) {
        CompletableFuture<World> future = new CompletableFuture<>();
        Path templatePath = templateDirectory.resolve(templateName);
        if (!Files.exists(templatePath)) {
            future.completeExceptionally(new IllegalArgumentException("Template '" + templateName + "' not found!"));
            return future;
        }

        Path targetPath = Paths.get(Bukkit.getWorldContainer().getPath(), targetWorldName);
        duplicateDirectory(templatePath, targetPath)
                .thenCompose(successful -> {
                    if (!successful) {
                        throw new RuntimeException("Failed to duplicate template directory");
                    }
                    return initializeWorld(targetWorldName);
                })
                .thenAccept(world -> {
                    if (world != null) {
                        clonedWorlds.add(world);
                        future.complete(world);
                    } else {
                        future.completeExceptionally(new RuntimeException("Failed to initialize world"));
                    }
                })
                .exceptionally(throwable -> {
                    Bukkit.getLogger().log(Level.SEVERE, "Error loading template", throwable);
                    future.completeExceptionally(throwable);
                    return null;
                });

        return future;
    }

    /**
     * Duplicates a directory from a source path to a target path.
     *
     * @param sourcePath the source directory path to copy from
     * @param targetPath the target directory path to copy to
     * @return a CompletableFuture indicating success or failure
     */
    private CompletableFuture<Boolean> duplicateDirectory(Path sourcePath, Path targetPath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!Files.exists(sourcePath)) {
                    Bukkit.getLogger().warning("Source directory not found: " + sourcePath);
                    return false;
                }

                Files.walkFileTree(sourcePath, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        Path targetDir = targetPath.resolve(sourcePath.relativize(dir));
                        Files.createDirectories(targetDir);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        String fileName = file.getFileName().toString();
                        if (fileName.equals("session.lock") || fileName.equals("uid.dat")) {
                            return FileVisitResult.CONTINUE;
                        }
                        Files.copy(file, targetPath.resolve(sourcePath.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                        return FileVisitResult.CONTINUE;
                    }
                });
                return true;
            } catch (IOException e) {
                Bukkit.getLogger().log(Level.SEVERE, "Error duplicating directory", e);
                return false;
            }
        });
    }

    /**
     * Initializes a new world with the given name.
     *
     * @param newWorldName the name of the world to initialize
     * @return a CompletableFuture containing the initialized World instance, or an exception if failed
     */
    private CompletableFuture<World> initializeWorld(String newWorldName) {
        CompletableFuture<World> future = new CompletableFuture<>();

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    WorldCreator worldCreator = new WorldCreator(newWorldName);
                    World newWorld = Bukkit.createWorld(worldCreator);

                    if (newWorld != null) {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                registerWithMultiverse(newWorld);
                                future.complete(newWorld);
                            }
                        }.runTaskLater(plugin, 20L);
                    } else {
                        future.complete(null);
                    }
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            }
        }.runTask(plugin);

        return future;
    }

    /**
     * Deletes a template world from the template directory.
     *
     * @param templateName the name of the template to delete
     * @return a CompletableFuture indicating success or failure
     */
    public CompletableFuture<Boolean> deleteTemplate(String templateName) {
        Path templatePath = templateDirectory.resolve(templateName);

        return CompletableFuture.supplyAsync(() -> {
            try {
                Files.walkFileTree(templatePath, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
                return true;
            } catch (IOException e) {
                Bukkit.getLogger().log(Level.SEVERE, "Error deleting template directory", e);
                return false;
            }
        });
    }

    /**
     * Registers the newly created world with Multiverse-Core.
     *
     * @param world the World instance to register
     */
    private void registerWithMultiverse(World world) {
        worldManager.addWorld(
                world.getName(),
                world.getEnvironment(),
                null,
                null,
                false,
                null
        );
    }
}