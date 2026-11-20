package me.chubbyduck.canvas.minestom;

import java.util.UUID;
import me.chubbyduck.canvas.core.dialog.InputBinding;
import me.chubbyduck.canvas.core.dialog.RenderedDialog;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.ByteBinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.NumberBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerCustomClickEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerResourcePackStatusEvent;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Routes dialog clicks, tracks resource pack status, and cleans up sessions in Minestom.
 */
@NullMarked
final class CanvasListener {

    private final CanvasRuntime runtime;
    private final EventNode<Event> eventNode;

    CanvasListener(final CanvasRuntime runtime, final EventNode<Event> parentNode) {
        this.runtime = runtime;
        this.eventNode = EventNode.all("canvas-listener");
        this.register(parentNode);
    }

    private void register(final EventNode<Event> parentNode) {
        this.eventNode.addListener(PlayerCustomClickEvent.class, this::onClick);
        this.eventNode.addListener(PlayerResourcePackStatusEvent.class, this::onPackStatus);
        this.eventNode.addListener(PlayerDisconnectEvent.class, this::onQuit);
        parentNode.addChild(this.eventNode);
    }

    public void unregister(final EventNode<Event> parentNode) {
        parentNode.removeChild(this.eventNode);
    }

    private void onClick(final PlayerCustomClickEvent event) {
        if (!event.getKey().equals(RenderedDialog.ACTION)) {
            return;
        }

        final BinaryTag rawPayload = event.getPayload();
        if (!(rawPayload instanceof final CompoundBinaryTag compound)) {
            return;
        }

        final Player player = event.getPlayer();
        final String session = compound.getString(RenderedDialog.FIELD_SESSION);
        final String action = compound.getString(RenderedDialog.FIELD_ACTION);
        final int nonce = compound.getInt(RenderedDialog.FIELD_NONCE);

        if (session == null || action == null) {
            return;
        }

        final InputBinding.InputReader inputs = new InputBinding.InputReader() {
            @Override
            public @Nullable String text(final String key) {
                final BinaryTag tag = compound.get(key);
                if (tag instanceof final StringBinaryTag str) {
                    return str.value();
                }
                return null;
            }

            @Override
            public @Nullable Boolean bool(final String key) {
                final BinaryTag tag = compound.get(key);
                if (tag instanceof final ByteBinaryTag b) {
                    return b.value() != 0;
                }
                return null;
            }

            @Override
            public @Nullable Float number(final String key) {
                final BinaryTag tag = compound.get(key);
                if (tag instanceof final NumberBinaryTag num) {
                    return num.floatValue();
                }
                return null;
            }
        };

        this.runtime.sessions().handleClick(player.getUuid(), session, nonce, action, inputs);
    }

    private void onPackStatus(final PlayerResourcePackStatusEvent event) {
        if (!this.runtime.platform().requiresPack() || event.getStatus() != net.kyori.adventure.resource.ResourcePackStatus.SUCCESSFULLY_LOADED) {
            return;
        }
        final Player player = event.getPlayer();
        this.runtime.platform().packLoaded(player.getUuid());
        this.runtime.rerender(player);
    }

    private void onQuit(final PlayerDisconnectEvent event) {
        final UUID id = event.getPlayer().getUuid();
        this.runtime.sessions().quit(id);
        this.runtime.platform().forget(id);
    }
}
