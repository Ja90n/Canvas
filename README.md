# Canvas (Minestom & Paper)

Canvas is a reactive, declarative UI framework for Minecraft's new **Dialog API** (introduced in modern Minecraft versions). It allows developers to build rich, native dialog interfaces using a modern component model (similar to React/Flutter) with state hooks, flexbox/absolute layouts, custom themes, and native inputs.

---

## 🚀 Modules

- **`canvas-api`**: Core declarative interfaces (`Canvas`, `Node`, `State`, `Theme`, `Component`).
- **`canvas-core`**: Layout engine, rendering pipelines, glyph compiler, and session management.
- **`canvas-pack`**: Resource pack generation for custom UI sprites and glyph atlases.
- **`canvas-elements`**: Built-in UI elements (Button, Text, Panel, Segmented, Tabs, Toggle, Slider, Dropdown, etc.).
- **`canvas-minestom`**: Native **Minestom** platform adapter (`MinestomPlatform`, `DialogAdapter`, `CanvasListener`, `CanvasRuntime`).
- **`canvas-paper`**: **Paper** platform adapter for Bukkit/Paper/Folia servers.

---

## 📦 Using in Minestom

### 1. Dependency

Add `canvas-minestom` to your `pom.xml`:

```xml
<dependency>
    <groupId>me.chubbyduck</groupId>
    <artifactId>canvas-minestom</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Or in Gradle:

```kotlin
dependencies {
    implementation("me.chubbyduck:canvas-minestom:0.1.0-SNAPSHOT")
}
```

### 2. Bootstrap Runtime

In your Minestom server startup (e.g. `main` or server initialization):

```java
import me.chubbyduck.canvas.minestom.CanvasRuntime;
import me.chubbyduck.canvas.elements.Themes;

// Initializes Canvas runtime and binds to Minestom's GlobalEventHandler
CanvasRuntime runtime = CanvasRuntime.bootstrap(Path.of("canvas"))
    .registerTheme(Themes.ORE)
    .start();
```

### 3. Open a Canvas Dialog

```java
import me.chubbyduck.canvas.Canvas;
import me.chubbyduck.canvas.elements.Button;
import me.chubbyduck.canvas.elements.Text;
import me.chubbyduck.canvas.elements.Panel;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;

// Construct and open a reactive canvas:
new Canvas(
    new Panel(
        new Text("Welkom bij NLPVP!"),
        new Button(Component.text("Sluiten"), session -> session.close())
    )
).open(player);
```

---

## 🛠 Building & Publishing

To build all modules and publish to your local Maven repository (`~/.m2/repository`):

```bash
./gradlew publishToMavenLocal
```
