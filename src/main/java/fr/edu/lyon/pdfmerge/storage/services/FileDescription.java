package fr.edu.lyon.pdfmerge.storage.services;

public class FileDescription implements Comparable<FileDescription> {
	public String content;
	public String filename;
	public String key;

	@Override
	public int compareTo(FileDescription fd) {
		if (getKey() == null || fd.getKey() == null) {
			return 0;
		}
		return getKey().compareTo(fd.getKey());
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

}
