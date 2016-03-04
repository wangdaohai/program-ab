package org.alicebot.ab.utils;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

public final class LogUtil {

    private LogUtil() {}

    public static void activateDebug(boolean activate) {
        setLevel(Logger.ROOT_LOGGER_NAME, activate ? Level.DEBUG : Level.INFO);
    }

    public static void setLevel(String loggerName, Level level) {
        Logger logger = (Logger) LoggerFactory.getLogger(loggerName);
        logger.setLevel(level);
    }

}
