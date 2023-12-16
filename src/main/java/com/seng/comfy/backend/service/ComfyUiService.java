package com.seng.comfy.backend.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
public class ComfyUiService {

    private final RestTemplate restTemplate;
    private final WebSocketService webSocketService;

    @Autowired
    public ComfyUiService(RestTemplate restTemplate, WebSocketService webSocketService) {
        this.restTemplate = restTemplate;
        this.webSocketService = webSocketService;
    }

    public String queuePrompt(String prompt) throws IOException {
        String filePath = "src/main/resources/static/comfy_templates/default_workflow.json";
        String temp = "";
        String serverAddress = "127.0.0.1:8188"; // The server address
        String clientId = UUID.randomUUID().toString(); // Generates a unique client ID
        String wsUri = "ws://" + serverAddress + "/ws?clientId=" + clientId;

        try {
            webSocketService.connect(wsUri);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            // Handle connection error
        }


        try {
            String content = new String(Files.readAllBytes(Paths.get(filePath)));
            JSONObject copyJson = new JSONObject(content);
            copyJson.getJSONObject("6").getJSONObject("inputs").put("text", prompt);
            copyJson.getJSONObject("3").getJSONObject("inputs").put("seed", System.nanoTime());
            temp = copyJson.toString(); // for pretty printing
        } catch (Exception e) {
            e.printStackTrace();
        }

        String jsonInput ="{\"prompt\":"+  temp+", \"client_id\": \"a-unique-uusid\"} ";
        System.out.println("JSON INPUT: " + jsonInput);
        URL url = new URL("http://127.0.0.1:8188/prompt");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json; utf-8");
        con.setRequestProperty("Accept", "application/json");
        con.setDoOutput(true);

        try (OutputStream os = con.getOutputStream()) {
            byte[] input = jsonInput.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(con.getInputStream(), "utf-8"))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }
        JSONObject jsonObject = new JSONObject(response.toString());
        String promptId = jsonObject.getString("prompt_id");
        String imgName = retrieveImage(promptId);
        System.out.println("Server Response: " + response.toString());
        return imgName;
    }

    public static byte[] getImage(String filename, String subfolder, String type) throws IOException {
        String urlString = "http://127.0.0.1:8188/view?filename=" + filename + "&subfolder=" + subfolder + "&type=" + type;
        URL url = new URL(urlString);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream is = con.getInputStream();
        byte[] byteChunk = new byte[4096];
        int n;

        while ((n = is.read(byteChunk)) > 0) {
            baos.write(byteChunk, 0, n);
        }

        return baos.toByteArray();
    }

    public static String processResponseAndRetrieveImages(JSONObject history, String promptId) {
        // Check if the promptId key exists
        if (!history.has(promptId)) {
            System.err.println("Error: Prompt ID '" + promptId + "' not found in the response.");
            return "error";
        }

        JSONObject promptData = history.getJSONObject(promptId);
        if (!promptData.has("outputs")) {
            System.err.println("Error: 'outputs' key not found for prompt ID '" + promptId + "'.");
            return "error";
        }

        JSONObject outputs = promptData.getJSONObject("outputs");
        String imagesDirPath = "src/main/resources/static/images/";
        File imagesDir = new File(imagesDirPath);
        if (!imagesDir.exists()) {
            imagesDir.mkdirs();
        }

        for (String key : outputs.keySet()) {
            JSONObject output = outputs.getJSONObject(key);
            if (output.has("images")) {
                JSONArray images = output.getJSONArray("images");
                for (int j = 0; j < images.length(); j++) {
                    JSONObject image = images.getJSONObject(j);
                    String filename = image.getString("filename"); // Filename to be returned

                    String subfolder = image.optString("subfolder", ""); // Using optString to handle missing subfolder
                    String type = image.getString("type");

                    File outputFile = new File(imagesDirPath + filename);
                    try {
                        byte[] imageData = getImage(filename, subfolder, type);
                        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                            fos.write(imageData);
                        }
                        return filename; // Return the filename of the first successfully processed image
                    } catch (FileNotFoundException e) {
                        System.err.println("File not found for writing image data: " + outputFile.getAbsolutePath());
                        e.printStackTrace();
                    } catch (IOException e) {
                        System.err.println("Error occurred while writing image data to file: " + outputFile.getAbsolutePath());
                        e.printStackTrace();
                    }
                }
            }
        }

        return "error"; // Return "error" if no images were processed successfully
    }

    public static String retrieveImage(String promptId) {
        final int maxAttempts = 10; // Maximum number of attempts
        final long delayMillis = 2000; // Delay between attempts in milliseconds
        String imgName ="";
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                JSONObject history = getHistory(promptId);
                if (!history.isEmpty()) {
                    imgName =  processResponseAndRetrieveImages(history, promptId);
                    System.out.println("HISTORY RETRIEVED MOVING TO RETRIEVAL");
                    return imgName; // Exit the loop if data is found
                } else {
                    System.out.println("Waiting for image generation to complete. Attempt " + (attempt + 1) + " of " + maxAttempts);
                    Thread.sleep(delayMillis); // Wait for a while before retrying
                }
            } catch (IOException e) {
                System.err.println("Error while retrieving image history: " + e.getMessage());
                e.printStackTrace();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore the interrupted status
                System.err.println("Thread was interrupted while waiting for image history.");
                break;
            }
        }
        System.err.println("Failed to retrieve image history after " + maxAttempts + " attempts.");
        return "error";
    }

    public static JSONObject getHistory(String promptId) throws IOException {
        String urlString = "http://127.0.0.1:8188/history/" + promptId;
        URL url = new URL(urlString);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();

        System.out.println("History : : " + content.toString());
        return new JSONObject(content.toString());
    }
}
