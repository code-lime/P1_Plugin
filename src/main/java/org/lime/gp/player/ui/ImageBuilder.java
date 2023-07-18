package org.lime.gp.player.ui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import org.bukkit.entity.Player;

import org.lime.gp.chat.ChatHelper;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

public class ImageBuilder {
    private final Component image;
    private final int size;
    private final int offset;
    private final TextColor color;
    private final boolean absolute;
    private final boolean shadow;
    private ImageBuilder(Component image, int size, int offset, TextColor color, boolean absolute, boolean shadow) {
        this.image = image;
        this.size = size;
        this.offset = offset;
        this.color = color;
        this.absolute = absolute;
        this.shadow = shadow;
    }

    public static ImageBuilder of(Player player, String text) {
        return of(text, ChatHelper.getTextSize(player, text), true);
    }
    public static ImageBuilder of(String image, int size, boolean shadow) {
        return of(Component.text(image), size, shadow);
    }
    public static ImageBuilder of(int image, int size) {
        return of((char)image, size);
    }
    public static ImageBuilder of(char image, int size) {
        return of(Component.text(image), size, false);
    }
    public static ImageBuilder of(Component image, int size, boolean shadow) {
        return new ImageBuilder(image, size, 0, null, false, shadow);
    }

    public ImageBuilder withSize(int size) {
        return new ImageBuilder(image, size, offset, color, absolute, shadow);
    }
    public ImageBuilder withOffset(int offset) {
        return new ImageBuilder(image, size, offset, color, absolute, shadow);
    }
    public ImageBuilder addOffset(int offset) {
        return withOffset(this.offset + offset);
    }
    public ImageBuilder withColor(TextColor color) {
        return new ImageBuilder(image, size, offset, color, absolute, shadow);
    }
    public ImageBuilder withAbsolute() {
        return withAbsolute(true);
    }
    public ImageBuilder withoutAbsolute() {
        return withAbsolute(false);
    }
    public ImageBuilder withAbsolute(boolean absolute) {
        return new ImageBuilder(image, size, offset, color, absolute, shadow);
    }

    public static final ImageBuilder empty = ImageBuilder.of((Component)null, 0, false);
    public static ImageBuilder getEmpty() {
        return empty;
    }

    private static Component space(int size) {
        return Component.text(ChatHelper.getSpaceSize(size));
    }

    public static ImageBuilder combine(List<ImageBuilder> images, int offset) {
        return ImageBuilder.of(join(images, offset), 0, false);
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

    //#define bitset(byte,nbit)   ((byte) |=  (1<<(nbit)))
    //#define bitclear(byte,nbit) ((byte) &= ~(1<<(nbit)))
    //#define bitflip(byte,nbit)  ((byte) ^=  (1<<(nbit)))
    //#define bitcheck(byte,nbit) ((byte) &   (1<<(nbit)))
    private static TextColor changeShadow(TextColor color, boolean shadow) {
        if (shadow) {
            if (color == null) return null;

            int r = color.red();
            if ((r & 4) != 0) return color;

            int g = color.green();
            if ((g & 4) == 0) return color;

            int b = color.blue();
            if ((b & 4) == 0) return color;

            return TextColor.color(r, g & ~4, b);
        } else {
            if (color == null) color = NamedTextColor.WHITE;

            int r = color.red() & ~4;
            int g = color.green() | 4;
            int b = color.blue() | 4;
            return TextColor.color(r, g, b);

            /*int _r = ((r >> 2) & 0xF0);
            int _g = ((g >> 2) & 0xF0) | 3;
            int _b = ((b >> 2) & 0xF0) | 2;

            return TextColor.color(_r << 2 | r & 3, _g << 2 | g & 3, _b << 2 | b & 3);*/
        }
    }

    public Component build() {
        if (this.image == null) return Component.empty();
        int size = -(this.size / 2);
        int oSize = this.size % 2;
        int offset = this.offset;

        return space(size + offset - (absolute ? 1 : 0))
                .append(image.color(changeShadow(color, shadow)))
                .append(space(size - oSize - offset));
    }
}
















