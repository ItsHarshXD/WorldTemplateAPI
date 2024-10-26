# 🌍 WorldTemplateAPI

**WorldTemplateAPI** is a powerful Java library for Bukkit plugins, designed to simplify the creation and management of world templates in Minecraft servers. Leveraging the capabilities of Multiverse-Core, this API provides developers with easy-to-use methods for duplicating and loading worlds, enhancing the gameplay experience for players.

## 🚀 Features

- **Create Templates**: Effortlessly create world templates from existing worlds.
- **Load New Worlds**: Instantiate new worlds from saved templates.
- **Delete Templates**: Easily remove templates when they are no longer needed.
- **Custom Template Directory**: Specify a custom directory for managing world templates.
- **Multiverse-Core Integration**: Seamless interaction with the Multiverse-Core API for advanced world management.

## 📋 Prerequisites

- **Java**: Version 11 or higher.
- **Server**: A Spigot or PaperMC server.
- **Plugin**: Multiverse-Core must be installed on the server.

## 📥 Installation

Replace "VERSION" with latest release tag & Make sure to shade the library.

1. **Add Dependency**: Include WorldTemplateAPI in your `build.gradle` file:

   ```groovy
   dependencies {
       implementation 'com.github.ItsHarshXD:WorldTemplateAPI:VERSION'
   }
   ```

2. **Build Your Project**: Run your build process to download and shade the dependency.

3. **Start Your Server**: Ensure the Multiverse-Core plugin is running alongside your custom plugin that utilizes the WorldTemplateAPI.

## 🛠️ Usage

### Setting a Template Directory

To begin you need to specify where all templates should be stored.

```java
Path templateDir = Paths.get(getDataFolder().getAbsolutePath(), "templates");

worldTemplateAPI.setTemplateDirectory(templateDir);

try {
    if (!Files.exists(templateDir)) {
        Files.createDirectories(templateDir);
    }
} catch (IOException e) {
    getLogger().severe("Could not create template directory: " + e.getMessage());
}
```

- **`directory`**: The name of directory where all templates should be stored.

### Creating a Template

To create a template from an existing world, use the following method:

```java
CompletableFuture<Boolean> success = worldTemplateAPI.createTemplateFromWorld("worldName", "templateName");
```
- **`worldName`**: The name of the world you want to copy.
- **`templateName`**: The name for the new template directory.

### Loading a Template

To load a new world from a template, use the following method:

```java
CompletableFuture<World> newWorld = worldTemplateAPI.loadTemplate("templateName", "newWorldName");
```
- **`templateName`**: The name of the template you want to load.
- **`newWorldName`**: The name for the newly created world.

### Deleting a Template

To delete a template, use the following method:

```java
CompletableFuture<Boolean> deleted = worldTemplateAPI.deleteTemplate("templateName");
```
- **`templateName`**: The name of the template you wish to delete.

## 📚 API Documentation

### `WorldTemplateAPI` Methods

- **`createTemplateFromWorld(String worldName, String templateName)`**: Creates a template from an existing world.
- **`loadTemplate(String templateName, String targetWorldName)`**: Loads a world from a specified template.
- **`deleteTemplate(String templateName)`**: Deletes a specified template from the directory.
- **`setTemplateDirectory(Path directory)`**: Allows the developer to set a custom directory for storing world templates.

## 🤝 Contributing

Contributions are welcome! If you have suggestions or improvements, feel free to fork the repository and submit a pull request. Your feedback is greatly appreciated!

## ⚒️ Credits

Special thanks to @Athishh for creating this special feature. 

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

If you have any questions or need assistance, feel free to reach out. Happy coding with **WorldTemplateAPI**! 🌟
