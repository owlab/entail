package com.enxime.entail.client.ui;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.enxime.entail.share.LogUtil;

public class TailURL {
    private static final Logger _logger = LogUtil.getLogger(TailURL.class.getName());
    
	public static final int PATH_UNIX = 1;
	public static final int PATH_NT = 2;
	
	private static String preFix = "tail://";
	private static Pattern IP_PORT = Pattern.compile("(\\d{1,3}\\.){3}\\d{1,3}:\\d{1,5}");
	private static Pattern AD_PORT = Pattern.compile("[\\w+\\.]+\\w+:\\d{1,5}");
	private String hostName;
	private int port;
	private String filePath;
	private int pathType; //
	
	public TailURL(String aURL) throws InvalidTailURLException {
		if(aURL == null)
			throw new InvalidTailURLException("Null string");
		if(!aURL.toLowerCase().startsWith(TailURL.preFix)) //TODO make it case insensitive
			throw new InvalidTailURLException("Prefix does not start with \"tail://\"");
		String[] parts = aURL.split("/");
		if(parts.length < 4)
			throw new InvalidTailURLException("The url not valid: " + aURL);
		Matcher matcher_ip = IP_PORT.matcher(parts[2]);
		Matcher matcher_ad = AD_PORT.matcher(parts[2]);
		if(matcher_ip.find() || matcher_ad.find()) {
			this.hostName = parts[2].split(":")[0];
			String portString = parts[2].split(":")[1];
			this.port = Integer.parseInt(portString);
			if(this.port > 65535)
				throw new InvalidTailURLException("The port is out of range: " + this.port);
			
			int thirdIndex = aURL.indexOf("/", 7);
			String pathPart = aURL.substring(thirdIndex);
			
			if(pathPart.contains(":")) {
				this.pathType = TailURL.PATH_NT;
				this.filePath = pathPart.substring(1);
			} else {
				this.pathType = TailURL.PATH_UNIX;
				this.filePath = pathPart;
			}
			
			if(aURL.endsWith("/")) {
				throw new InvalidTailURLException("Path ends with /.");
			}
		} else {
			throw new InvalidTailURLException("Host name (or IP) and port format not valid.");
		}
//		this.hostName = aURL.split("/")[2].split(":")[0];
//		this.port = Integer.parseInt(aURL.split("/")[2].split(":")[1]);
//		
//		int thirdIndex = aURL.indexOf("/", 7);
//		String pathPart = aURL.substring(thirdIndex);
//		
//		if(pathPart.contains(":")) {
//			this.pathType = TailURL.PATH_NT;
//			this.filePath = pathPart.substring(1);
//		} else {
//			this.pathType = TailURL.PATH_UNIX;
//			this.filePath = pathPart;
//		}
//		
//		if(aURL.endsWith("/")) {
//			throw new InvalidTailURLException("Path ends with /.");
//		}
	}
	
	public String getHost() {
		return this.hostName;
	}
	
	public int getPort() {
		return this.port;
	}
	
	public String getFilePath() {
		return this.filePath;
	}
	
	public String getFileName() {
		
		int lastIndex = -1;
		if(this.pathType == TailURL.PATH_UNIX)
			lastIndex = this.filePath.lastIndexOf("/");
		else
			lastIndex = this.filePath.lastIndexOf("\\");
		
		if(lastIndex != -1 && lastIndex != this.filePath.length()) {
				return this.filePath.substring(lastIndex+1);
			} else {
				return "[error]";
			}
	}
	
	public String getHostAndPort() {
		return this.hostName + ":" + this.port;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(TailURL.preFix).append(this.getHostAndPort());
		if(this.pathType == TailURL.PATH_NT)
			sb.append("/").append(this.filePath);
		else if(this.pathType == TailURL.PATH_UNIX)
			sb.append(this.filePath);
		
		return sb.toString();
	}

	@Override
	public int hashCode() {
	    final int prime = 31;
	    int result = 1;
	    result = prime * result
		    + ((filePath == null) ? 0 : filePath.hashCode());
	    result = prime * result
		    + ((hostName == null) ? 0 : hostName.hashCode());
	    result = prime * result + port;
	    return result;
	}

	@Override
	public boolean equals(Object object) {
	    if (this == object)
		return true;
	    if (object == null)
		return false;
	    if (getClass() != object.getClass())
		return false;
	    
	    TailURL other = (TailURL) object;
	    if (filePath == null) {
		if (other.filePath != null)
		    return false;
	    } else if (pathType == PATH_UNIX && !filePath.equals(other.filePath)) {
		return false;
	    } else if(pathType == PATH_NT && !filePath.equalsIgnoreCase(other.filePath)) {
		return false;
	    }
	    
	    if (hostName == null) {
		if (other.hostName != null)
		    return false;
	    } else if (!hostName.equalsIgnoreCase(other.hostName))
		return false;
	    if (port != other.port)
		return false;
	    return true;
	}
}
