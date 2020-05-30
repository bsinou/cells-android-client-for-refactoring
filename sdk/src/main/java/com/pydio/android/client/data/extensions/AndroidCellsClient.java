package com.pydio.android.client.data.extensions;

import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.pydio.sdk.core.PydioCells;
import com.pydio.sdk.core.common.callback.TransferProgressListener;
import com.pydio.sdk.core.common.errors.SDKException;
import com.pydio.sdk.core.common.http.ContentBody;
import com.pydio.sdk.core.common.http.HttpClient;
import com.pydio.sdk.core.common.http.HttpRequest;
import com.pydio.sdk.core.common.http.Method;
import com.pydio.sdk.core.model.Message;
import com.pydio.sdk.core.model.ServerNode;
import com.pydio.sdk.core.utils.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class AndroidCellsClient extends PydioCells {

    public AndroidCellsClient(ServerNode node) {
        super(node);
    }

    private String cleanPathForPreSignedURL(String path) throws SDKException {
        String[] parts = path.substring(1).split("/");
        String cleanPath = "";

        for (String part : parts) {
            if (part.length() == 0) {
                continue;
            }
            try {
                String encodedPart = URLEncoder.encode(part, "utf-8");
                cleanPath = cleanPath.concat("/").concat(encodedPart);
            } catch (UnsupportedEncodingException e) {
                throw SDKException.encoding(e);
            }
        }
        return cleanPath;
    }

    private String getUploadPreSignedURL(String ws, String folder, String name) throws SDKException {
        getJWT();

        AWSCredentials credentials = new BasicAWSCredentials("gateway", "gatewaysecret");
        final AmazonS3 s3 = new AmazonS3Client(credentials);

        //String cleanPath = cleanPathForPreSignedURL(String.format("%s/%s", node, name));
        String cleanPath = String.format("%s/%s", folder, name);
        String filename = String.format("%s%s", ws, cleanPath).replace("//", "/");

        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest("io", filename);
        request.setMethod(HttpMethod.PUT);
        request.setContentType("application/octet-stream");
        request.addRequestParameter("pydio_jwt", this.bearerValue);

        String u = this.URL;
        if (u.endsWith("/")) {
            u = u.substring(0, u.length() - 1);
        }

        s3.setEndpoint(u);
        java.net.URL url = s3.generatePresignedUrl(request);
        return url.toString().replace(" ", "%20");
    }

    private String getDownloadPreSignedURL(String ws, String file) throws SDKException {

        getJWT();
        String u = this.URL;
        if (u.endsWith("/")) {
            u = u.substring(0, u.length() - 1);
        }

        AWSCredentials credentials = new BasicAWSCredentials("gateway", "gatewaysecret");
        final AmazonS3 s3 = new AmazonS3Client(credentials);
        s3.setEndpoint(u);


        String filename = ws + file;
        filename = filename.replace("//", "/");

        /*String encodedFilename;
        try {
            encodedFilename = URLEncoder.encode(filename, "utf-8");
        } catch (UnsupportedEncodingException e) {
            throw SDKException.encoding("utf-8", filename, e);
        }*/

        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest("io", filename);
        request.setMethod(HttpMethod.GET);
        request.addRequestParameter("pydio_jwt", this.bearerValue);

        java.net.URL url = s3.generatePresignedUrl(request);
        return url.toString().replace(" ", "%20");
    }

    private void httpPUT(String url, InputStream source, long length, TransferProgressListener listener) throws SDKException {
        ContentBody body = new ContentBody(source, length);
        body.setListener(new ContentBody.ProgressListener() {
            @Override
            public void transferred(long progress) throws IOException {
                if (listener.onProgress(progress)) {
                    throw new IOException("stopped");
                }
            }

            @Override
            public void partTransferred(int part, int total) throws IOException {
                long progress = (long) ((float) part / (float) total);
                if (listener.onProgress(progress)) {
                    throw new IOException("stopped");
                }
            }
        });
        try {
            HttpRequest request = new HttpRequest();
            request.setMethod(Method.PUT);
            request.setContentBody(body);
            request.setEndpoint(url);
            HttpClient.request(request);
        } catch (IOException e) {
            throw SDKException.conWriteFailed(e);
        }
    }

    private InputStream httpGET(String url) throws SDKException {
        try {
            return new java.net.URL(url).openStream();
        } catch (IOException e) {
            throw SDKException.conFailed(e);
        }
    }


    //**********************************************************
    //          OVERRIDES of CELLS CLIENT
    //**********************************************************
    @Override
    public long download(String ws, String file, File target, TransferProgressListener progressListener) throws SDKException {
        OutputStream out;
        try {
            out = new FileOutputStream(target);
        } catch (FileNotFoundException e) {
            throw SDKException.notFound(e);
        }
        long totalRead;
        try {
            totalRead = this.download(ws, file, out, progressListener);
            io.close(out);
        } catch (SDKException e) {
            target.delete();
            io.close(out);
            throw e;
        }
        return totalRead;
    }

    @Override
    public long download(String ws, String file, OutputStream target, TransferProgressListener progressListener) throws SDKException {
        String url = getDownloadPreSignedURL(ws, file);
        InputStream in = httpGET(url);
        try {
            return io.pipeReadWithProgress(in, target, progressListener::onProgress);
        } catch (IOException e) {
            e.printStackTrace();
            throw SDKException.conReadFailed(e);
        }
    }

    @Override
    public String downloadURL(String ws, String file) throws SDKException {
        return getDownloadPreSignedURL(ws, file);
    }

    @Override
    public Message upload(File source, String ws, String path, String name, boolean autoRename, TransferProgressListener progressListener) throws SDKException {
        return upload(source, ws, path, name, progressListener);
    }

    private Message upload(File f, String ws, String path, String name, TransferProgressListener tpl) throws SDKException {

        InputStream in;
        try {
            in = new FileInputStream(f);
        } catch (FileNotFoundException e) {
            throw SDKException.notFound(e);
        }

        String url = getUploadPreSignedURL(ws, path, name);
        httpPUT(url, in, f.length(), tpl);
        return null;

        /*getJWT();

        AWSCredentials credentials = new BasicAWSCredentials("gateway", "gatewaysecret");
        final AmazonS3 s3 = new AmazonS3Client(credentials);
        String u = this.URL;
        if (u.endsWith("/")) {
            u = u.substring(0, u.length() - 1);
        }
        s3.setEndpoint(u);

        String cleanPath = String.format("%s/%s", path, name);
        String filename = String.format("%s%s", ws, cleanPath).replace("//", "/");

        PutObjectRequest request = new PutObjectRequest("io", filename, f);
        ObjectMetadata meta = new ObjectMetadata();
        meta.setHeader("X-Pydio-Bearer", this.JWT);
        request.setMetadata(meta);

        final long[] totalTransferred = {0};
        request = request.withGeneralProgressListener(progressEvent -> {
            if(tpl != null){
                totalTransferred[0] += progressEvent.getBytesTransferred();
                tpl.onProgress((totalTransferred[0]*100) / f.length());
            }
        });

        try {
            s3.putObject(request);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;*/
    }

    @Override
    public Message upload(InputStream source, long length, String ws, String path, String name, boolean autoRename, TransferProgressListener progressListener) throws SDKException {

        getJWT();

        AWSCredentials credentials = new BasicAWSCredentials("gateway", "gatewaysecret");
        final AmazonS3 s3 = new AmazonS3Client(credentials);
        String u = this.URL;
        if (u.endsWith("/")) {
            u = u.substring(0, u.length() - 1);
        }
        s3.setEndpoint(u);

        //String cleanPath = cleanPathForPreSignedURL(String.format("%s/%s", node, name));
        String cleanPath = String.format("%s/%s", path, name);
        String filename = String.format("%s%s", ws, cleanPath).replace("//", "/");

        //GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest("io", filename);
        ObjectMetadata meta = new ObjectMetadata();
        //meta.setContentLength(length);
        meta.setContentType("application/octet-stream");
        meta.setHeader("X-Pydio-Bearer", this.bearerValue);
        PutObjectRequest request = new PutObjectRequest("io", filename, source, meta);
        request.withGeneralProgressListener(progressEvent -> {
            long progress = progressEvent.getBytesTransferred();
            progressListener.onProgress(progress);
        });
        try {
            PutObjectResult res = s3.putObject(request);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String uploadURL(String ws, String folder, String name, boolean autoRename) throws SDKException {
        return getUploadPreSignedURL(ws, folder, name);
    }

    @Override
    public InputStream previewData(String ws, String file, int dim) throws SDKException {
        this.getJWT();
        AWSCredentials credentials = new BasicAWSCredentials(this.bearerValue, "gatewaysecret");
        final AmazonS3 s3 = new AmazonS3Client(credentials);
        s3.setEndpoint(this.URL);
        GetObjectRequest request = new GetObjectRequest("io", "pydio-thumbstore" + file);
        try {
            S3Object s3Object = s3.getObject(request);
            return s3Object.getObjectContent();
        } catch (AmazonS3Exception ignored) {
            return null;
        }
    }

    @Override
    public String streamingVideoURL(String ws, String file) {
        AWSCredentials credentials = new BasicAWSCredentials("gateway", "gatewaysecret");
        final AmazonS3 s3 = new AmazonS3Client(credentials);
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest("io", ws + file);
        request.addRequestParameter("response-content-type", "video/mp4");
        request.addRequestParameter("pydio_jwt", this.bearerValue);
        String u = this.URL;
        if (u.endsWith("/")) {
            u = u.substring(0, u.length() - 1);
        }
        s3.setEndpoint(u);
        java.net.URL url = s3.generatePresignedUrl(request);
        return url.toString();
    }

    @Override
    public String streamingAudioURL(String ws, String file) {
        AWSCredentials credentials = new BasicAWSCredentials("gateway", "gatewaysecret");
        final AmazonS3 s3 = new AmazonS3Client(credentials);
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest("io", ws + file);
        request.addRequestParameter("response-content-type", "audio/mp3");
        request.addRequestParameter("pydio_jwt", this.bearerValue);
        String u = this.URL;
        if (u.endsWith("/")) {
            u = u.substring(0, u.length() - 1);
        }
        s3.setEndpoint(u);
        java.net.URL url = s3.generatePresignedUrl(request);
        return url.toString();
    }
}
