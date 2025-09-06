package dev.stardust.gui.widgets.meteorites.input;

import java.util.Arrays;
import java.util.HashMap;
import dev.stardust.util.MsgUtil;
import static org.lwjgl.glfw.GLFW.*;
import dev.stardust.util.StardustUtil;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import dev.stardust.gui.widgets.meteorites.WMeteorites;
import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class InputTracker {
    private static final int CHEAT_CODE = 0x4CC2DECC;
    private final HashMap<Integer, Boolean> keyStates;
    private int[] lastPressed = new int[String.valueOf(CHEAT_CODE).length() + 1];

    public InputTracker() { this.keyStates = new HashMap<>(); }

    public void updateState(int key, boolean active, WMeteorites widget) {
        if ((key == 82 || widget.isPaused || widget.gameOver) && active && isNotHeld(key)) {
            int[] t = new int[lastPressed.length];
            for (int n = 0; n < lastPressed.length; n++) {
                if (n >= lastPressed.length - 1) {
                    t[n] = key;
                    continue;
                }
                t[n] = lastPressed[n + 1];
            }

            lastPressed = t;
            if (Arrays.hashCode(lastPressed) == CHEAT_CODE) {
                widget.highScore = null;
                widget.gameBegan = false;
                widget.module.clearSave();
                Arrays.fill(lastPressed, 0);
                widget.module.enteredCheatCode = true;
                widget.CHEAT_MODE = !widget.CHEAT_MODE;
                widget.module.debug.set(!widget.module.debug.get());

                if (widget.module.debug.get()) {
                    MsgUtil.sendModuleMsg(
                        StardustUtil.rCC() + "§oCheater cheater pumpkin eater..!", widget.module.name
                    );
                }

                widget.resetGame();
                widget.pauseGame();
            }
        }
        keyStates.put(key, active);
    }

    public boolean isNotHeld(int key) {
        return !keyStates.getOrDefault(key, false);
    }

    public static boolean isKeyDown(int key) {
        if (mc == null || mc.getWindow() == null) return false;
        long handle = mc.getWindow().getHandle();
        return glfwGetKey(handle, key) == GLFW_PRESS;
    }

    public static boolean isMouseDown(int button) {
        if (mc == null || mc.getWindow() == null) return false;
        long handle = mc.getWindow().getHandle();
        return glfwGetMouseButton(handle, button) == GLFW_PRESS;
    }
}
