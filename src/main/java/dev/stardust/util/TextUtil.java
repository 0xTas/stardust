package dev.stardust.util;

import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import net.minecraft.text.*;
import java.util.function.UnaryOperator;

public class TextUtil {
    // See ChatHudMixin.java && EntityRendererMixin.java
    public static Text modifyWithStyle(Text original, UnaryOperator<String> modifier) {
        List<StyledChar> chars = new ArrayList<>();
        collectStyledChars(original, original.getStyle(), chars);

        String modifiedContent = modifier.apply(original.getString());

        List<StyledChar> modifiedChars = new ArrayList<>();
        for (int n = 0; n < chars.size(); n++) {
            modifiedChars.add(new StyledChar(modifiedContent.charAt(n), chars.get(n).style));
        }

        return rebuildFromStyledChars(modifiedChars);
    }

    private static void collectStyledChars(Text original, Style inherited, List<StyledChar> out) {
        Style style = original.getStyle().withParent(inherited);
        String content = original.getContent().visit(Optional::of).orElse("");

        for (char c : content.toCharArray()) {
            out.add(new StyledChar(c, style));
        }

        for (Text sibling : original.getSiblings()) {
            collectStyledChars(sibling, style, out);
        }
    }

    private static Text rebuildFromStyledChars(List<StyledChar> chars) {
        if (chars.isEmpty()) return Text.literal("");

        MutableText root = null;
        StringBuilder sb = new StringBuilder();
        Style currentStyle = chars.getFirst().style();

        for (StyledChar sc : chars) {
            if (!sc.style.equals(currentStyle)) {
                if (!sb.isEmpty()) {
                    MutableText chunk = Text.literal(sb.toString()).setStyle(currentStyle);
                    if (root == null) root = chunk;
                    else root.append(chunk);
                    sb.setLength(0);
                }
                currentStyle = sc.style();
            }
            sb.append(sc.c());
        }

        if (!sb.isEmpty()) {
            MutableText chunk = Text.literal(sb.toString()).setStyle(currentStyle);
            if (root == null) root = chunk;
            else root.append(chunk);
        }

        return root;
    }

    record StyledChar(char c, Style style) {}
}
