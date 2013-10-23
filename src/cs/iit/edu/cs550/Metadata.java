package cs.iit.edu.cs550;

import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(namespace = "Metadata")
public class Metadata {
private String IPAddress;
private int port;
private Map<String, Long> fileProp;
private String type;
private String fileName;
private List<String> peerList;

@XmlElement(name = "IPAddress")
public String getIPAddress() {
	return IPAddress;
}
public void setIPAddress(String iPAddress) {
	IPAddress = iPAddress;
}
public int getPort() {
	return port;
}
public void setPort(int port) {
	this.port = port;
}

public String getType() {
	return type;
}
public void setType(String type) {
	this.type = type;
}
public String getFileName() {
	return fileName;
}
public void setFileName(String fileName) {
	this.fileName = fileName;
}
public List<String> getPeerList() {
	return peerList;
}
public void setPeerList(List<String> peerList) {
	this.peerList = peerList;
}

public Map<String, Long> getFileProp() {
	return fileProp;
}
public void setFileProp(Map<String, Long> fileProp) {
	this.fileProp = fileProp;
}

}
