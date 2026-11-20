package me.chubbyduck.canvas.minestom;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import me.chubbyduck.canvas.core.dialog.GlyphCompiler;
import me.chubbyduck.canvas.core.dialog.PlainCompiler;
import me.chubbyduck.canvas.core.render.DebugBox;
import me.chubbyduck.canvas.core.render.Renderer;
import me.chubbyduck.canvas.core.session.SessionManager;
import me.chubbyduck.canvas.elements.Themes;
import me.chubbyduck.canvas.minestom.platform.MinestomPlatform;
import me.chubbyduck.canvas.node.Node;
import me.chubbyduck.canvas.pack.CanvasPack;
import me.chubbyduck.canvas.pack.CanvasPackBuilder;
import me.chubbyduck.canvas.session.Session;
import me.chubbyduck.canvas.spi.CanvasPlatform;
import me.chubbyduck.canvas.asset.Theme;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Runs Canvas inside a Minestom server: builds the resource pack and installs
 * the runtime behind {@code new Canvas(...).open(player)}.
 */
@NullMarked
public class CanvasRuntime {

    private static final String PACK_FILE = "pack.zip";

    private static volatile @Nullable CanvasRuntime current;

    private final Path dataDirectory;
    private final EventNode<Event> eventNode;
    private final Logger logger;
    private final List<Theme> themes;
    private final Set<Class<? extends Node>> classes;
    private final MinestomPlatform platform;
    private final CanvasListener listener;
    private volatile CanvasPack pack;
    private volatile @Nullable SessionManager sessions;
    private boolean rebuildScheduled;

    private CanvasRuntime(
        final Path dataDirectory,
        final EventNode<Event> eventNode,
        final Logger logger,
        final List<Theme> themes,
        final Set<Class<? extends Node>> classes,
        final boolean requirePack
    ) {
        this.dataDirectory = dataDirectory;
        this.eventNode = eventNode;
        this.logger = logger;
        this.themes = themes;
        this.classes = classes;
        this.platform = new MinestomPlatform(requirePack);
        this.pack = this.rebuild();
        this.listener = new CanvasListener(this, eventNode);
    }

    public static Builder bootstrap(final Path dataDirectory) {
        return new Builder(dataDirectory);
    }

    public static Builder bootstrap() {
        return new Builder(Path.of("canvas"));
    }

    public static CanvasRuntime get() {
        final CanvasRuntime runtime = current;
        if (runtime == null) {
            throw new IllegalStateException("Canvas is not started");
        }
        return runtime;
    }

    @SafeVarargs
    public final void register(final Class<? extends Node>... classes) {
        final List<Class<? extends Node>> list = new ArrayList<>(classes.length);
        list.addAll(Arrays.asList(classes));
        this.register(list);
    }

    public void register(final List<Class<? extends Node>> classes) {
        if (!this.classes.addAll(classes)) {
            return;
        }
        this.scheduleRebuild();
    }

    public void registerTheme(final Theme... themes) {
        for (final Theme theme : themes) {
            if (this.themes.stream().anyMatch(registered -> registered.id().equals(theme.id()))) {
                continue;
            }
            this.themes.add(theme);
            this.scheduleRebuild();
        }
    }

    public void reload() {
        this.pack = this.rebuild();
    }

    private void scheduleRebuild() {
        if (this.rebuildScheduled) {
            return;
        }
        this.rebuildScheduled = true;
        MinecraftServer.getSchedulerManager().scheduleNextTick(() -> {
            this.rebuildScheduled = false;
            this.reload();
        });
    }

    private CanvasPack rebuild() {
        final CanvasPackBuilder builder = new CanvasPackBuilder().registerTheme(this.themes.toArray(Theme[]::new));
        for (final Class<? extends Node> type : this.classes) {
            builder.register(type);
        }
        builder.register(DebugBox.class);
        final CanvasPack built = builder.build();
        final Path file = this.dataDirectory.resolve(PACK_FILE);
        try {
            Files.createDirectories(this.dataDirectory);
            built.writeTo(file);
        } catch (final IOException exception) {
            throw new UncheckedIOException("Could not write the Canvas pack to " + file, exception);
        }

        final SessionManager previous = this.sessions;
        if (previous != null) {
            // Canvases rendered with the old atlas may use glyphs that no longer exist.
            for (final Session session : previous.sessions()) {
                session.close();
            }
        }
        final SessionManager manager = new SessionManager(
            new Renderer(built.atlas(), built.registered()::contains),
            new GlyphCompiler(built.atlas()),
            new PlainCompiler(),
            this.platform,
            this::resolveTheme,
            this.logger
        );
        this.sessions = manager;
        CanvasPlatform.install(manager);
        this.logger.info(String.format(Locale.ROOT, "Canvas pack written to %s (%d KiB, sha1 %s)",
            file, built.zip().length / 1024, built.sha1()));
        return built;
    }

    private Theme resolveTheme(final @Nullable Theme theme) {
        if (theme == null) {
            return this.themes.getFirst();
        }
        for (final Theme registered : this.themes) {
            if (registered.id().equals(theme.id())) {
                return registered;
            }
        }
        throw new IllegalStateException("Theme " + theme.id() + " is not registered");
    }

    public void stop() {
        if (current != this) {
            return;
        }
        this.listener.unregister(this.eventNode);
        for (final Session session : this.sessions().sessions()) {
            session.close();
        }
        CanvasPlatform.install(null);
        current = null;
    }

    public CanvasPack pack() {
        return this.pack;
    }

    public SessionManager sessions() {
        return Objects.requireNonNull(this.sessions, "Canvas sessions are not initialized");
    }

    public void forcePlain(final Player player, final boolean plain) {
        this.platform.forcePlain(player.getUuid(), plain);
        this.rerender(player);
    }

    public void debug(final Player player, final boolean enabled) {
        this.sessions().debug(player.getUuid(), enabled);
    }

    public MinestomPlatform platform() {
        return this.platform;
    }

    void rerender(final Player player) {
        this.sessions().getCurrentSession(player).ifPresent(Session::updateCanvas);
    }

    public static class Builder {

        private final Path dataDirectory;
        private @Nullable EventNode<Event> eventNode;
        private @Nullable Logger logger;
        private final List<Theme> themes = new ArrayList<>();
        private final Set<Class<? extends Node>> classes = new LinkedHashSet<>();
        private boolean requirePack;

        private Builder(final Path dataDirectory) {
            this.dataDirectory = dataDirectory;
        }

        public Builder eventNode(final EventNode<Event> eventNode) {
            this.eventNode = eventNode;
            return this;
        }

        public Builder logger(final Logger logger) {
            this.logger = logger;
            return this;
        }

        public Builder registerTheme(final Theme... themes) {
            this.themes.addAll(Arrays.asList(themes));
            return this;
        }

        @SafeVarargs
        public final Builder register(final Class<? extends Node>... classes) {
            this.classes.addAll(Arrays.asList(classes));
            return this;
        }

        public Builder register(final List<Class<? extends Node>> classes) {
            this.classes.addAll(classes);
            return this;
        }

        /**
         * Whether players only get glyph dialogs after a resource pack has loaded; off by default.
         */
        public Builder requirePack(final boolean requirePack) {
            this.requirePack = requirePack;
            return this;
        }

        public CanvasRuntime start() {
            if (current != null) {
                throw new IllegalStateException("Canvas is already started");
            }
            if (this.themes.isEmpty()) {
                this.themes.add(Themes.ORE);
            }
            final EventNode<Event> node = this.eventNode != null ? this.eventNode : MinecraftServer.getGlobalEventHandler();
            final Logger log = this.logger != null ? this.logger : Logger.getLogger("Canvas");
            final CanvasRuntime runtime = new CanvasRuntime(this.dataDirectory, node, log, this.themes, this.classes, this.requirePack);
            current = runtime;
            return runtime;
        }
    }
}
