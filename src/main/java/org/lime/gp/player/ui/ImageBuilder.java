package org.lime.gp.player.ui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.TextColor;
import org.lime.gp.chat.ChatHelper;

import java.util.ArrayList;
import java.util.List;

public class ImageBuilder {
    private final Component image;
    private final int size;
    private final int offset;
    private final TextColor color;
    private final boolean absolute;
    private ImageBuilder(Component image, int size, int offset, TextColor color, boolean absolute) {
        this.image = image;
        this.size = size;
        this.offset = offset;
        this.color = color;
        this.absolute = absolute;
    }

    public static ImageBuilder of(String text) {
        return of(text, ChatHelper.getTextSize(text));
    }

    public static ImageBuilder of(String image, int size) {
        return of(Component.text(image), size);
    }
    public static ImageBuilder of(int image, int size) {
        return of((char)image, size);
    }
    public static ImageBuilder of(char image, int size) {
        return of(Component.text(image), size);
    }
    public static ImageBuilder of(Component image, int size) {
        return new ImageBuilder(image, size, 0, null, false);
    }

    public ImageBuilder withSize(int size) {
        return new ImageBuilder(image, size, offset, color, absolute);
    }
    public ImageBuilder withOffset(int offset) {
        return new ImageBuilder(image, size, offset, color, absolute);
    }
    public ImageBuilder addOffset(int offset) {
        return withOffset(this.offset + offset);
    }
    public ImageBuilder withColor(TextColor color) {
        return new ImageBuilder(image, size, offset, color, absolute);
    }
    public ImageBuilder withAbsolute() {
        return withAbsolute(true);
    }
    public ImageBuilder withoutAbsolute() {
        return withAbsolute(false);
    }
    public ImageBuilder withAbsolute(boolean absolute) {
        return new ImageBuilder(image, size, offset, color, absolute);
    }

    public static final ImageBuilder empty = ImageBuilder.of((Component)null, 0);
    public static ImageBuilder getEmpty() {
        return empty;
    }

    private static Component space(int size) {
        return Component.text(ChatHelper.getSpaceSize(size));
    }

    public static ImageBuilder combine(List<ImageBuilder> images, int offset) {
        return ImageBuilder.of(join(images, offset), 0);
    }
    public static Component join(List<ImageBuilder> images, int offset) {
        List<Component> components = new ArrayList<>();
        for (ImageBuilder image : images)
        {
            if (image != null && image.image != null)
                components.add(image.withAbsolute(true).build());
        }
        components.add(0, space(offset));
        components.add(space(-offset));
        return Component.join(JoinConfiguration.separator(Component.empty()), components);
    }
    public static Component join(List<ImageBuilder> images) {
        return join(images, 0);
    }

    public Component build() {
        if (this.image == null) return Component.empty();
        int size = -(this.size / 2);
        int oSize = this.size % 2;
        int offset = this.offset;

        return space(size + offset - (absolute ? 1 : 0))
                .append(color == null ? image : image.color(color))
                .append(space(size - oSize - offset));
    }
}
















