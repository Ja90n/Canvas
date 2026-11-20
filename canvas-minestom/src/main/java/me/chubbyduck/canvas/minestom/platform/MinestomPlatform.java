package me.chubbyduck.canvas.minestom.platform;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import me.chubbyduck.canvas.core.dialog.RenderedDialog;
import me.chubbyduck.canvas.core.session.SessionPlatform;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.jspecify.annotations.NullMarked;

/**
 * Minestom implementation of the session platform: shows dialogs, schedules tasks,
 * and decides between the glyph canvas and the plain fallback.
 */
@NullMarked
public final class MinestomPlatform implements SessionPlatform {

    private final Set<UUID> packLoaded = ConcurrentHashMap.newKeySet();
    private final Set<UUID> forcedPlain = ConcurrentHashMap.newKeySet();

    private final boolean requirePack;

    public MinestomPlatform(final boolean requirePack) {
        this.requirePack = requirePack;
    }

    @Override
    public void show(final Audience viewer, final RenderedDialog dialog) {
        if (viewer instanceof final Player player) {
            player.showDialog(DialogAdapter.toMinestom(dialog));
        }
    }

    @Override
    public void close(final Audience viewer) {
        if (viewer instanceof final Player player) {
            player.closeDialog();
        }
    }

    @Override
    public void schedule(final Audience viewer, final Runnable task) {
        MinecraftServer.getSchedulerManager().scheduleNextTick(task);
    }

    @Override
    public boolean supportsGlyphs(final Audience viewer) {
        final UUID id = viewer.get(Identity.UUID).orElse(null);
        if (id == null || this.forcedPlain.contains(id) || BedrockDetector.isBedrock(id)) {
            return false;
        }
        return !this.requirePack || this.packLoaded.contains(id);
    }

    public void packLoaded(final UUID player) {
        this.packLoaded.add(player);
    }

    public void forget(final UUID player) {
        this.packLoaded.remove(player);
        this.forcedPlain.remove(player);
    }

    public void forcePlain(final UUID player, final boolean plain) {
        if (plain) {
            this.forcedPlain.add(player);
            return;
        }
        this.forcedPlain.remove(player);
    }

    public boolean requiresPack() {
        return this.requirePack;
    }
}
