package org.lime.gp.chat;

import net.kyori.adventure.text.*;
import net.kyori.adventure.text.format.Style;
import org.lime.system;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class TextSplitRenderer {
    private record Frame(Component component, Style style, String context) {
        public static Frame of(Component component, Style parent) {
            component = component.children(Collections.emptyList());
            if (component instanceof KeybindComponent element) return new Frame(element, parent.merge(element.style()), element.keybind());
            else if (component instanceof ScoreComponent element) return new Frame(element, parent.merge(element.style()), element.value());
            else if (component instanceof SelectorComponent element) return new Frame(element, parent.merge(element.style()), element.pattern());
            else if (component instanceof TextComponent element) return new Frame(element, parent.merge(element.style()), element.content());
            else if (component instanceof TranslatableComponent element) return new Frame(element, parent.merge(element.style()), element.key());
            throw new IllegalArgumentException("Component '" + component + "' with type '"+(component == null ? "NULLABLE" : component.getClass())+"' not supported");
        }
    }
    private static Stream<Frame> components(Component component, Style parent) {
        Frame base = Frame.of(component, parent);
        return component.children().isEmpty()
                ? Stream.of(base)
                : Stream.concat(Stream.of(base), component.children().stream().flatMap(v -> components(v, base.style)));
    }
    private interface ISplit {
        List<Component> split(Pattern pattern);
        String content();

        static ISplit single(Frame frame) {
            return new ISplit() {
                @Override public List<Component> split(Pattern pattern) { return Collections.singletonList(frame.component); }
                @Override public String content() { return frame.context; }
            };
        }
    }
    private record TextSplit(List<TextComponent> components) implements ISplit {
        private record TextComponentGroup(List<TextComponent> components) implements CharSequence {
            @Override public int length() {
                return components.stream().map(TextComponent::content).mapToInt(String::length).sum();
            }
            @Override public char charAt(int index) {
                for (TextComponent component : components) {
                    String content = component.content();
                    int length = content.length();
                    if (index >= length) index -= length;
                    else return content.charAt(index);
                }
                throw new IndexOutOfBoundsException(index);
            }
            static void checkBoundsBeginEnd(int begin, int end, int length) {
                if (begin < 0 || begin > end || end > length) throw new StringIndexOutOfBoundsException(
                        "begin " + begin + ", end " + end + ", length " + length);
            }
            @Override public TextComponentGroup subSequence(int beginIndex, int endIndex) {
                int length = length();
                checkBoundsBeginEnd(beginIndex, endIndex, length);
                if (beginIndex == 0 && endIndex == length) return this;

                List<TextComponent> components = new ArrayList<>();

                int subLen = endIndex - beginIndex;

                for (TextComponent component : this.components) {
                    String content = component.content();
                    int content_length = content.length();
                    if (beginIndex >= content_length) beginIndex -= content_length;
                    else if (subLen >= content_length && beginIndex == 0) components.add(component);
                    else {
                        components.add(Component.text(content.substring(beginIndex, Math.min(content_length, subLen))).style(component.style()));
                        subLen -= content_length;
                        beginIndex = 0;
                        if (subLen <= 0) break;
                    }
                }
                return new TextComponentGroup(components);
            }
            public Component group() { return Component.empty().children(components); }
            @Override public String toString() { return components.stream().map(TextComponent::content).collect(Collectors.joining()); }
        }
        private static List<TextComponentGroup> split(Pattern pattern, TextComponentGroup input, int limit) {
            int index = 0;
            boolean matchLimited = limit > 0;
            ArrayList<TextComponentGroup> matchList = new ArrayList<>();
            Matcher m = pattern.matcher(input);

            // Add segments before each match found
            while(m.find()) {
                if (!matchLimited || matchList.size() < limit - 1) {
                    if (index == 0 && index == m.start() && m.start() == m.end()) {
                        // no empty leading substring included for zero-width match
                        // at the beginning of the input char sequence.
                        continue;
                    }
                    TextComponentGroup match = input.subSequence(index, m.start());
                    matchList.add(match);
                    index = m.end();
                } else if (matchList.size() == limit - 1) { // last one
                    TextComponentGroup match = input.subSequence(index,
                            input.length());
                    matchList.add(match);
                    index = m.end();
                }
            }

            // If no match was found, return this
            if (index == 0) return Collections.singletonList(input);

            // Add remaining segment
            if (!matchLimited || matchList.size() < limit) matchList.add(input.subSequence(index, input.length()));

            // Construct result
            int resultSize = matchList.size();
            if (limit == 0)
                while (resultSize > 0 && matchList.get(resultSize-1).isEmpty())
                    resultSize--;
            return matchList.subList(0, resultSize);
        }
        @Override public List<Component> split(Pattern pattern) {
            return split(pattern, new TextComponentGroup(components), 0)
                    .stream()
                    .map(TextComponentGroup::group)
                    .collect(Collectors.toList());
        }

        @Override public String content() {
            return components.stream().map(TextComponent::content).collect(Collectors.joining("&"));
        }
        public TextSplit join(TextSplit other) {
            return new TextSplit(system.list.<TextComponent>of().add(this.components).add(other.components).build());
        }
    }
    public static List<Component> split(Component component, String spliter) {
        return split(component, Pattern.compile(Pattern.quote(spliter)));
    }
    public static List<Component> split(Component component, Pattern pattern) {
        List<ISplit> frames = new ArrayList<>();
        components(component, Style.empty())
                .forEach(frame -> {
                    int length = frames.size();
                    int index = length - 1;
                    if (!(frame.component instanceof TextComponent textComponent)) {
                        frames.add(ISplit.single(frame));
                        return;
                    }
                    TextSplit split = new TextSplit(Collections.singletonList(textComponent));
                    Optional<ISplit> last = index >= 0 ? Optional.of(frames.get(index)) : Optional.empty();
                    last.map(v -> v instanceof TextSplit text ? text : null)
                            .map(v -> v.join(split))
                            .ifPresentOrElse(v -> frames.set(index, v), () -> frames.add(split));
                });
        List<Component> components = new ArrayList<>();
        frames.forEach(split -> {
            int size = components.size();
            List<Component> list = split.split(pattern);
            int list_size = list.size();
            if (list_size == 0) return;

            if (size == 0) components.add(Component.empty().append(list.get(0)));
            else components.set(size - 1, components.get(size - 1).append(list.get(0)));

            for (int i = 1; i < list_size; i++) components.add(list.get(i));
        });
        return components;
    }
}































