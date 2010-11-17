package org.meandre.tools.client.utils;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.seasr.meandre.support.generic.logging.formatter.GenericLogFormatter;

/**
 * This Factory returns the same Logger every time. It is suitable for client
 * side applications that have minimal logging needs such as command line tools. 
 * 
 *  All output is send to the console.
 *  
 *  Default Log level is "FINE"
 *  
 *  No log files are written by default
 *  
 *  
 * @author pgroves
 *
 */
public class GenericLoggerFactory {
    
    /** A basic logger for client code */
    private static Logger _log = null;
    
    /** The basic handler for the logger */
    public static Handler _handler = null;
    
    public static final Level _defaultLevel = Level.FINE;
    
    /** the name of the ClientLogger used to get the logger from java's
     * global Logger store. Used to get this logger using Logger.getLogger()
     */
    public static final String GLOBAL_LOG_NAME = "MeandreClient";
    
    // Initializing the logger and its handlers
    static {
        
        _log = Logger.getLogger(GLOBAL_LOG_NAME);
        _log.setLevel(_defaultLevel);
        _handler = new ConsoleHandler();
        _handler.setLevel(_defaultLevel);
        _handler.setFormatter(new GenericLogFormatter());
        _log.addHandler(_handler);
        
    }
    
    /** Returns the default clientlogger.
     */
    public static Logger getLogger() {
        return _log;
    }
    
    /** Set the level to use on for the logger and handler.
     * 
     * @param level The requested level
     */
    public static void setLevel ( Level level ) {
        _log.setLevel(level);
        _handler.setLevel(level);
    }
}
