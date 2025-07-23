package dev.stardust.util;

import dev.stardust.Stardust;

public class LogUtil {
    public static void info(String msg) {
        Stardust.LOG.info("{} {}", MsgUtil.getRawPrefix(), msg);
    }
    public static void info(String msg, String module) {
        Stardust.LOG.info("{}{} {}", MsgUtil.getRawPrefix(), MsgUtil.getRawPrefix(module), msg);
    }
    public static void warn(String msg) {
        Stardust.LOG.warn("{} {}", MsgUtil.getRawPrefix(), msg);
    }
    public static void warn(String msg, String module) {
        Stardust.LOG.warn("{}{} {}", MsgUtil.getRawPrefix(), MsgUtil.getRawPrefix(module), msg);
    }
    public static void error(String msg) {
        Stardust.LOG.error("{} {}", MsgUtil.getRawPrefix(), msg);
    }
    public static void error(String msg, String module) {
        Stardust.LOG.error("{}{} {}", MsgUtil.getRawPrefix(), MsgUtil.getRawPrefix(module), msg);
    }
    public static void debug(String msg) {
        Stardust.LOG.debug("{} {}", MsgUtil.getRawPrefix(), msg);
    }
    public static void debug(String msg, String module) {
        Stardust.LOG.debug("{}{} {}", MsgUtil.getRawPrefix(), MsgUtil.getRawPrefix(module), msg);
    }
}
