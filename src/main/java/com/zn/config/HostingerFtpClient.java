package com.zn.config;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
public class HostingerFtpClient {

    @Value("${hostinger.ftp.host}")
    private String ftpHost;

    @Value("${hostinger.ftp.port}")
    private int ftpPort;

    @Value("${hostinger.ftp.username}")
    private String ftpUsername;

    @Value("${hostinger.ftp.password}")
    private String ftpPassword;

    @Value("${hostinger.ftp.upload.path}")
    private String uploadPath;

    public void uploadFile(String filename, InputStream inputStream) throws IOException {
        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect(ftpHost, ftpPort);
            ftpClient.setConnectTimeout(10000); // 10 seconds timeout
            ftpClient.setDefaultTimeout(10000);

            boolean login = ftpClient.login(ftpUsername, ftpPassword);
            if (!login) {
                int replyCode = ftpClient.getReplyCode();
                String replyString = ftpClient.getReplyString();
                throw new IOException("FTP login failed. Reply code: " + replyCode + ", Reply: " + replyString);
            }

            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            // Change to upload directory
            boolean changed = ftpClient.changeWorkingDirectory(uploadPath);
            if (!changed) {
                int replyCode = ftpClient.getReplyCode();
                String replyString = ftpClient.getReplyString();
                throw new IOException("Could not change working directory to " + uploadPath + ". Reply code: " + replyCode + ", Reply: " + replyString);
            }

            // Create subdirectories if filename contains '/'
            if (filename.contains("/")) {
                String[] parts = filename.split("/");
                String currentPath = "";
                for (int i = 0; i < parts.length - 1; i++) {
                    currentPath += "/" + parts[i];
                    boolean dirExists = ftpClient.changeWorkingDirectory(currentPath);
                    if (!dirExists) {
                        boolean created = ftpClient.makeDirectory(currentPath);
                        if (!created) {
                            throw new IOException("Could not create directory: " + currentPath);
                        }
                        ftpClient.changeWorkingDirectory(currentPath);
                    }
                }
                // Go back to upload directory
                ftpClient.changeWorkingDirectory(uploadPath);
            }

            boolean stored = ftpClient.storeFile(filename, inputStream);
            if (!stored) {
                int replyCode = ftpClient.getReplyCode();
                String replyString = ftpClient.getReplyString();
                throw new IOException("Failed to upload file: " + filename + ". Reply code: " + replyCode + ", Reply: " + replyString);
            }
        } finally {
            if (ftpClient.isConnected()) {
                try {
                    ftpClient.logout();
                    ftpClient.disconnect();
                } catch (IOException ex) {
                    // ignore
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ex) {
                    // ignore
                }
            }
        }
    }
}
