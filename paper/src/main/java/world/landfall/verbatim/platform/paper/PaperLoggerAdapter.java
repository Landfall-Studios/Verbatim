package world.landfall.verbatim.platform.paper;

import org.slf4j.Logger;
import org.slf4j.Marker;

/**
 * Adapter that wraps Paper's java.util.logging.Logger to implement SLF4J's Logger interface.
 * Paper provides SLF4J via Log4j2, but the plugin's getLogger() returns a JUL logger.
 * This bridges JUL to the SLF4J interface expected by core.
 */
public class PaperLoggerAdapter implements Logger {

    private final java.util.logging.Logger julLogger;
    private final String name;

    public PaperLoggerAdapter(java.util.logging.Logger julLogger) {
        this.julLogger = julLogger;
        this.name = "Verbatim";
    }

    private String format(String message, Object... args) {
        if (args == null || args.length == 0) {
            return message;
        }
        StringBuilder sb = new StringBuilder();
        int argIndex = 0;
        int i = 0;
        while (i < message.length()) {
            if (i < message.length() - 1 && message.charAt(i) == '{' && message.charAt(i + 1) == '}') {
                if (argIndex < args.length) {
                    sb.append(args[argIndex++]);
                } else {
                    sb.append("{}");
                }
                i += 2;
            } else {
                sb.append(message.charAt(i));
                i++;
            }
        }
        return sb.toString();
    }

    @Override
    public String getName() { return name; }

    // TRACE
    @Override public boolean isTraceEnabled() { return julLogger.isLoggable(java.util.logging.Level.FINEST); }
    @Override public void trace(String msg) { julLogger.finest(msg); }
    @Override public void trace(String format, Object arg) { julLogger.finest(format(format, arg)); }
    @Override public void trace(String format, Object arg1, Object arg2) { julLogger.finest(format(format, arg1, arg2)); }
    @Override public void trace(String format, Object... arguments) { julLogger.finest(format(format, arguments)); }
    @Override public void trace(String msg, Throwable t) { julLogger.log(java.util.logging.Level.FINEST, msg, t); }
    @Override public boolean isTraceEnabled(Marker marker) { return isTraceEnabled(); }
    @Override public void trace(Marker marker, String msg) { trace(msg); }
    @Override public void trace(Marker marker, String format, Object arg) { trace(format, arg); }
    @Override public void trace(Marker marker, String format, Object arg1, Object arg2) { trace(format, arg1, arg2); }
    @Override public void trace(Marker marker, String format, Object... argArray) { trace(format, argArray); }
    @Override public void trace(Marker marker, String msg, Throwable t) { trace(msg, t); }

    // DEBUG
    @Override public boolean isDebugEnabled() { return julLogger.isLoggable(java.util.logging.Level.FINE); }
    @Override public void debug(String msg) { julLogger.fine(msg); }
    @Override public void debug(String format, Object arg) { julLogger.fine(format(format, arg)); }
    @Override public void debug(String format, Object arg1, Object arg2) { julLogger.fine(format(format, arg1, arg2)); }
    @Override public void debug(String format, Object... arguments) { julLogger.fine(format(format, arguments)); }
    @Override public void debug(String msg, Throwable t) { julLogger.log(java.util.logging.Level.FINE, msg, t); }
    @Override public boolean isDebugEnabled(Marker marker) { return isDebugEnabled(); }
    @Override public void debug(Marker marker, String msg) { debug(msg); }
    @Override public void debug(Marker marker, String format, Object arg) { debug(format, arg); }
    @Override public void debug(Marker marker, String format, Object arg1, Object arg2) { debug(format, arg1, arg2); }
    @Override public void debug(Marker marker, String format, Object... arguments) { debug(format, arguments); }
    @Override public void debug(Marker marker, String msg, Throwable t) { debug(msg, t); }

    // INFO
    @Override public boolean isInfoEnabled() { return julLogger.isLoggable(java.util.logging.Level.INFO); }
    @Override public void info(String msg) { julLogger.info(msg); }
    @Override public void info(String format, Object arg) { julLogger.info(format(format, arg)); }
    @Override public void info(String format, Object arg1, Object arg2) { julLogger.info(format(format, arg1, arg2)); }
    @Override public void info(String format, Object... arguments) { julLogger.info(format(format, arguments)); }
    @Override public void info(String msg, Throwable t) { julLogger.log(java.util.logging.Level.INFO, msg, t); }
    @Override public boolean isInfoEnabled(Marker marker) { return isInfoEnabled(); }
    @Override public void info(Marker marker, String msg) { info(msg); }
    @Override public void info(Marker marker, String format, Object arg) { info(format, arg); }
    @Override public void info(Marker marker, String format, Object arg1, Object arg2) { info(format, arg1, arg2); }
    @Override public void info(Marker marker, String format, Object... arguments) { info(format, arguments); }
    @Override public void info(Marker marker, String msg, Throwable t) { info(msg, t); }

    // WARN
    @Override public boolean isWarnEnabled() { return julLogger.isLoggable(java.util.logging.Level.WARNING); }
    @Override public void warn(String msg) { julLogger.warning(msg); }
    @Override public void warn(String format, Object arg) { julLogger.warning(format(format, arg)); }
    @Override public void warn(String format, Object... arguments) { julLogger.warning(format(format, arguments)); }
    @Override public void warn(String format, Object arg1, Object arg2) { julLogger.warning(format(format, arg1, arg2)); }
    @Override public void warn(String msg, Throwable t) { julLogger.log(java.util.logging.Level.WARNING, msg, t); }
    @Override public boolean isWarnEnabled(Marker marker) { return isWarnEnabled(); }
    @Override public void warn(Marker marker, String msg) { warn(msg); }
    @Override public void warn(Marker marker, String format, Object arg) { warn(format, arg); }
    @Override public void warn(Marker marker, String format, Object arg1, Object arg2) { warn(format, arg1, arg2); }
    @Override public void warn(Marker marker, String format, Object... arguments) { warn(format, arguments); }
    @Override public void warn(Marker marker, String msg, Throwable t) { warn(msg, t); }

    // ERROR
    @Override public boolean isErrorEnabled() { return julLogger.isLoggable(java.util.logging.Level.SEVERE); }
    @Override public void error(String msg) { julLogger.severe(msg); }
    @Override public void error(String format, Object arg) { julLogger.severe(format(format, arg)); }
    @Override public void error(String format, Object arg1, Object arg2) { julLogger.severe(format(format, arg1, arg2)); }
    @Override public void error(String format, Object... arguments) { julLogger.severe(format(format, arguments)); }
    @Override public void error(String msg, Throwable t) { julLogger.log(java.util.logging.Level.SEVERE, msg, t); }
    @Override public boolean isErrorEnabled(Marker marker) { return isErrorEnabled(); }
    @Override public void error(Marker marker, String msg) { error(msg); }
    @Override public void error(Marker marker, String format, Object arg) { error(format, arg); }
    @Override public void error(Marker marker, String format, Object arg1, Object arg2) { error(format, arg1, arg2); }
    @Override public void error(Marker marker, String format, Object... arguments) { error(format, arguments); }
    @Override public void error(Marker marker, String msg, Throwable t) { error(msg, t); }
}
