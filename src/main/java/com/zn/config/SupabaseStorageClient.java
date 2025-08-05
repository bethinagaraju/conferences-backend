package com.zn.config;

import java.io.InputStream;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component
public class SupabaseStorageClient {

     @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.api.key}")
    private String serviceKey;


 

    @Value("${supabase.bucket}")
    private String bucket;

    public String uploadFile(String path, InputStream fileStream, long length) throws Exception {
        String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucket + "/" + path;

        HttpPut request = new HttpPut(uploadUrl);
        request.addHeader("Authorization", "Bearer " + serviceKey);
        request.addHeader("Content-Type", "application/octet-stream");
       request.setEntity(new InputStreamEntity(fileStream, length, org.apache.hc.core5.http.ContentType.APPLICATION_OCTET_STREAM));

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            return client.execute(request, response -> {
                int status = response.getCode();
                if (status >= 200 && status < 300) {
                    return supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + path;
                } else {
                    throw new RuntimeException("Upload failed with status: " + status);
                }
            });
        }
    }

    public InputStream downloadFile(String path) throws Exception {
        String downloadUrl = supabaseUrl + "/storage/v1/object/" + bucket + "/" + path;

        HttpGet request = new HttpGet(downloadUrl);
        request.addHeader("Authorization", "Bearer " + serviceKey);

        CloseableHttpClient client = HttpClients.createDefault();
        return client.execute(request).getEntity().getContent();
    }
}
