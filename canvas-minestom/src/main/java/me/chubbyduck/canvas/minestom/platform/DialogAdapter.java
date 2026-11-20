package me.chubbyduck.canvas.minestom.platform;

import java.io.IOException;
import java.util.List;
import me.chubbyduck.canvas.core.dialog.RenderedDialog;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.TagStringIO;
import net.minestom.server.dialog.Dialog;
import net.minestom.server.dialog.DialogAction;
import net.minestom.server.dialog.DialogActionButton;
import net.minestom.server.dialog.DialogAfterAction;
import net.minestom.server.dialog.DialogBody;
import net.minestom.server.dialog.DialogInput;
import net.minestom.server.dialog.DialogMetadata;
import org.jspecify.annotations.NullMarked;

/**
 * Converts the platform-neutral {@link RenderedDialog} into a Minestom {@link Dialog}.
 */
@NullMarked
public final class DialogAdapter {

    private DialogAdapter() {
    }

    public static Dialog toMinestom(final RenderedDialog dialog) {
        final List<DialogBody> body = dialog.body().stream()
            .map(element -> (DialogBody) new DialogBody.PlainMessage(element.text(), element.width()))
            .toList();

        final List<DialogInput> inputs = dialog.inputs().stream()
            .map(DialogAdapter::input)
            .toList();

        final DialogMetadata metadata = new DialogMetadata(
            dialog.title(),
            null, // externalTitle
            dialog.closeOnEscape(),
            false, // pause
            DialogAfterAction.NONE,
            body,
            inputs
        );

        final DialogActionButton exit = button(dialog.exit());

        if (dialog.buttons().isEmpty()) {
            return new Dialog.Notice(metadata, exit);
        }

        final List<DialogActionButton> buttons = dialog.buttons().stream()
            .map(DialogAdapter::button)
            .toList();

        return new Dialog.MultiAction(metadata, buttons, exit, dialog.columns());
    }

    private static DialogActionButton button(final RenderedDialog.Button data) {
        CompoundBinaryTag payloadTag;
        try {
            payloadTag = TagStringIO.tagStringIO().asCompound(data.payload());
        } catch (final IOException exception) {
            payloadTag = CompoundBinaryTag.empty();
        }

        // DynamicCustom action automatically injects dialog input values from the client
        final DialogAction action = new DialogAction.DynamicCustom(RenderedDialog.ACTION, payloadTag);

        return new DialogActionButton(
            data.label(),
            data.tooltip(),
            data.width(),
            action
        );
    }

    private static DialogInput input(final RenderedDialog.Input rendered) {
        final String key = rendered.key();
        return switch (rendered.input()) {
            case final me.chubbyduck.canvas.paint.DialogInput.Text text -> new DialogInput.Text(
                key,
                text.width(),
                text.label(),
                text.labelVisible(),
                text.initial(),
                text.maxLength(),
                null // single-line text input
            );
            case final me.chubbyduck.canvas.paint.DialogInput.Bool bool -> new DialogInput.Boolean(
                key,
                bool.label(),
                bool.initial(),
                "true",
                "false"
            );
            case final me.chubbyduck.canvas.paint.DialogInput.Range range -> new DialogInput.NumberRange(
                key,
                range.width(),
                range.label(),
                range.labelFormat(),
                range.start(),
                range.end(),
                range.initial(),
                range.step()
            );
            case final me.chubbyduck.canvas.paint.DialogInput.Options options -> new DialogInput.SingleOption(
                key,
                options.width(),
                options.options().stream()
                    .map(option -> new DialogInput.SingleOption.Option(
                        option.id(),
                        option.display(),
                        option.id().equals(options.initialId())
                    ))
                    .toList(),
                options.label(),
                options.labelVisible()
            );
        };
    }
}
