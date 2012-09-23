package com.enxime.entail.share;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class LogUtil {
	private static Level level = Level.INFO;
	
	public static Logger getLogger(Class klass) {
		return getLogger(klass.getName(), level); 
	}
	
	public static Logger getLogger(String name) {
		return getLogger(name, level); 
	}
	
	public static void setLevel(Level level) {
		LogUtil.level = level;
	}
	
	public static Logger getLogger(String name, Level level) {
		Logger logger = Logger.getLogger(name);
		// Not allow duplicated log output INFO and above..
		logger.setUseParentHandlers(false);
		
		logger.setLevel(level);
		ConsoleHandler console = new ConsoleHandler();
		//System.out.println(console.getFormatter());
		
		console.setFormatter(new SingleLineFormatter());
		// Level should be the same of above
		console.setLevel(level);
//		Handler[] handlers = logger.getHandlers();
//		for(Handler handler: handlers)
//			System.out.println("Log Handler: " + handler.toString());
		logger.addHandler(console);
		
		return logger;		
	}
	
	// Custom Line Formatter
	static class SingleLineFormatter extends SimpleFormatter {
		private static final String LINE_SEPARATOR = System.getProperty("line.separator");
		private DateFormat dateFormat;

	    @Override
	    public String format(LogRecord record) {
	        StringBuffer buf = new StringBuffer(180);
	        
	        if (dateFormat == null)
	          dateFormat = DateFormat.getDateTimeInstance();
	    
	        buf.append(dateFormat.format(new Date(record.getMillis())));
	        buf.append(' ');
	        buf.append(record.getSourceClassName());
	        buf.append(' ');
	        buf.append(record.getSourceMethodName());
	        buf.append(' ');
	    
	        buf.append(record.getLevel());
	        buf.append(": ");
	        buf.append(formatMessage(record));
	    
	        buf.append(LINE_SEPARATOR);
	    
	        Throwable throwable = record.getThrown();
	        if (throwable != null)
	          {
	            StringWriter sink = new StringWriter();
	            throwable.printStackTrace(new PrintWriter(sink, true));
	            buf.append(sink.toString());
	          }
	    
	        return buf.toString();


	    }

	}
}
