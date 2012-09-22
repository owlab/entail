package com.enxime.entail.server;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.enxime.entail.share.LogUtil;

public class TailWorker implements Runnable {
	//
    private static final Logger _logger = LogUtil.getLogger(TailWorker.class.getName());
	
	private final String filePath;
	private final File file;
	private final long delay;
	private final boolean end;
	private final TailListener listener;
	private volatile boolean run = true;
	private volatile boolean suspended = false;
	// to send multiple lines
	private final int lineBufferLength = 10;
	private int lineBufferCount = 0;
	private final String[] lineBuffer = new String[lineBufferLength];
	
	public TailWorker(String file, TailListener listener) {
		this(file, listener, 1000);
	}

	public TailWorker(String file, TailListener listener, long delay) {
		this(file, listener, delay, true);
	}

	public TailWorker(String filePath, TailListener listener, long delay, boolean end) {
		_logger.fine("called.");
		
		this.filePath = filePath;
		this.file = new File(filePath);
		this.listener = listener;
		this.delay = delay;
		this.end = end;

		listener.init(this);
	}

	public static TailWorker tail(String filePath, TailListener listener, long delay, boolean end) {
		
		TailWorker entailer = new TailWorker(filePath, listener, delay, end);
		Thread thread = new Thread(entailer);
		thread.setDaemon(true);
		thread.start();
		return entailer;
	}

	public static TailWorker tail(String file, TailListener listener, long delay) {
		return tail(file, listener, delay, true);
	}

	public static TailWorker tail(String file, TailListener listener) {
		return tail(file, listener, 1000, true);
	}

//	public File getFile() {
//		return file;
//	}
	public String getFilePath() {
		return this.filePath;
	}

	public long getDelay() {
		return delay;
	}

	public void run() {
		RandomAccessFile reader = null;
		try {
			long last = 0; // The last time the file was checked for changes
			long position = 0; // position within the file
			// Open the file
			while(run && (reader == null)) {
				try {
					reader = new RandomAccessFile(file, "r");
				} catch(FileNotFoundException e) {
					listener.fileNotFound();
				}

				if(reader == null) {
					try {
						Thread.sleep(delay);
					} catch(InterruptedException e) {
						// Ignore
					}
				} else {
					// The current position in the file
					if(end) {
						position = file.length();
						readLastLines(reader, listener, 10);
						
					} 
					//position = end ? file.length() : 0;
					last = System.currentTimeMillis();
					reader.seek(position);
				}
			}

			while(run) {
			    	if(suspended) {
			    	    synchronized(this) {
			    		wait();
			    	    }
			    	}
				// CHeck the file length to see if it was rotated
				long length = file.length();

				if(length < position) {
					// File was rotated
					listener.fileRotated();

					// Reopen the reader after rotation
					try {
						// Ensure that the old file is closed if we re-open it successfully
						RandomAccessFile save = reader;
						reader = new RandomAccessFile(file, "r");
						position = 0;
						//Close old file explicitly rather than relying on GC picking up previous RAF
						closeQuietly(save);
					} catch (FileNotFoundException e) {
						// in this case we continue to use the prepious reader and position values
						listener.fileNotFound();
					}

					continue;
				} else {
					// File was not rotated
					
					// See if the file needs to be read again
					if(length > position) {
						// The file has more content than it did last time
						last = System.currentTimeMillis();
						position = readLines(reader);
					} else if(isFileNewer(file, last)) {
						/* This can happen if the file is truncated or overwritten
						 * with the exact same length of information. In cases like 
						 * this, the file position needs to be reset
						 */
						position = 0;
						reader.seek(position); // cannot be null here

						// Now we can read new lines
						last = System.currentTimeMillis();
						position = readLines(reader);
					}
				}
				try {
					Thread.sleep(delay);
				} catch(InterruptedException e) {
					// Ignore
				}
			}
		} catch (Exception e) {
			listener.handleException(e);
		} finally {
			closeQuietly(reader);
		}
	}

	public void stop() {
		_logger.fine("called.");
		this.run = false;
		if(suspended) {
		    this.suspended = false;
		    synchronized(this) {
			notify();
		    }
		}
	}
	
	public void pause() {
		_logger.fine("called.");
		this.suspended = true;
	}
	
	public void restart() {
		_logger.fine("called.");
		this.suspended = false;
		synchronized(this) {
		    notify();
		}
	}
	
	private long readLines(RandomAccessFile reader) throws IOException {
		String line = null;
		while((line = reader.readLine()) != null) {
			lineBuffer[lineBufferCount++] = line;
			if(lineBufferCount == lineBufferLength) {
				listener.handleLines(lineBuffer);
				lineBufferCount = 0;
			}
		}
		
		if(lineBufferCount != 0) {
			listener.handleLines(lineBuffer, 0, lineBufferCount);
			lineBufferCount = 0;
		}
		
		return reader.getFilePointer();
	}

	private void readLastLines(RandomAccessFile raf, TailListener listener, int numLines) throws IOException {
		// TODO Implement to read last 10 lines
				
		int lineCount = 0;
		long position = raf.length();
		
		char prevC = '\0';
		char currC = '\0';
		
		List<String> lineList = new ArrayList<String>();
		
		StringBuilder sb = new StringBuilder(); // the first builder
		
		// to skip last \n \r \n\r
		position--;
		if(position < 0) {
			return;
		} else {
			if((char)raf.readByte() == '\n') {
				// 
			} else if((char)raf.readByte() == '\r') {
				position--;
				if(position < 0) {
					return;
				} else if((char)raf.readByte() == '\n') {
					//
				} else {
					position++;
				}
			}
		}
		while(position > 0 && lineCount <= numLines) {
			position--;
			raf.seek(position);
			currC = (char) raf.readByte();
			if(currC != '\r' && currC != '\n')
				sb.insert(0,  currC);
			// \n, \r, \n\r
			if(currC == '\r' && prevC != '\n') {
					lineCount++;
					lineList.add(0, sb.toString());
					sb = new StringBuilder();
					
			} else if(currC == '\n'&& prevC != '\r') {
					lineCount++;
					lineList.add(0, sb.toString());
					sb = new StringBuilder();
			}
			
			prevC = currC;
			//sb.insert(0, currC);
			//System.out.println("Position=" + position);
			if(position == 0 && sb.length() > 0) {
				lineList.add(0, sb.toString());
			}
		}
		
		// But the size of lines is always 11, not 10
		// and the last line is just '\r'
		// so, we remove the last line here
		int size = lineList.size();
		String[] sa = new String[size];
		for(int i = 0; i < size; i++)
			sa[i]  = lineList.get(i);
		//System.out.println("size = " + size);
		listener.handleLines(sa);
	}

	private static void closeQuietly(Closeable closeable) {
		try {
			if(closeable != null)
				closeable.close();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}

	private static boolean isFileNewer(File file, long timeMillis) {
		if(file == null) {
			throw new IllegalArgumentException("No file reference (null)");
		}
		if(!file.exists()) {
			throw new IllegalArgumentException("No specified file");
		}
		return file.lastModified() > timeMillis;
	}
}
