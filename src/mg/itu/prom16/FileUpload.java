package mg.itu.prom16;

import java.io.InputStream;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;

public class FileUpload {
    private String fileName;
    private String contentType; 
    private long size;
    private InputStream content;
    
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public InputStream getContent() {
        return content;
    }

    public void setContent(InputStream content) {
        this.content = content;
    }

    public FileUpload(String fileName, String contentType, long size, InputStream content) {
        this.setFileName(fileName);
        this.setContentType(contentType);
        this.setSize(size);
        this.setContent(content);
    }
    

    public static FileUpload handleFileUpload(HttpServletRequest request) throws Exception {
    Part filePart = request.getPart("file");
    String fileName = filePart.getSubmittedFileName();
    String contentType = filePart.getContentType();
    long fileSize = filePart.getSize();
    InputStream fileContent = filePart.getInputStream();
    
    return new FileUpload(fileName, contentType, fileSize, fileContent);
}

}
