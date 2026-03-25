package de.example.ollamaspike.infra.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public final class LocalHttpClient {

    public String get(String url) {
        HttpURLConnection connection = null;
        try {
            connection = openConnection(url, "GET");
            return readResponse(connection);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not execute GET request to " + url, exception);
        } finally {
            closeConnection(connection);
        }
    }

    public String postJson(String url, String requestBody) {
        HttpURLConnection connection = null;
        try {
            connection = openConnection(url, "POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            writeRequestBody(connection, requestBody);
            return readResponse(connection);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not execute POST request to " + url, exception);
        } finally {
            closeConnection(connection);
        }
    }

    private HttpURLConnection openConnection(String url, String method) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(2_000);
        connection.setReadTimeout(2_000);
        return connection;
    }

    private void writeRequestBody(HttpURLConnection connection, String requestBody) throws IOException {
        OutputStream outputStream = connection.getOutputStream();
        try {
            outputStream.write(requestBody.getBytes("UTF-8"));
            outputStream.flush();
        } finally {
            outputStream.close();
        }
    }

    private String readResponse(HttpURLConnection connection) throws IOException {
        int responseCode = connection.getResponseCode();
        InputStream responseStream = responseCode >= 400 ? connection.getErrorStream() : connection.getInputStream();
        if (responseStream == null) {
            return "";
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(responseStream, "UTF-8"));
        try {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            return builder.toString();
        } finally {
            reader.close();
        }
    }

    private void closeConnection(HttpURLConnection connection) {
        if (connection != null) {
            connection.disconnect();
        }
    }
}
