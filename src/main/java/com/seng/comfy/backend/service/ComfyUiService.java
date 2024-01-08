package com.seng.comfy.backend.service;

import com.seng.comfy.backend.helper.GenerateRequest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
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


    //directory to save the full size images
    static String imagesDirPath = "src/main/resources/static/images/";

    //directory to save the low quality images
    static String lq_imagesDirPath = "src/main/resources/static/lq_images/";

    @Autowired
    public ComfyUiService(RestTemplate restTemplate, WebSocketService webSocketService) {
        this.restTemplate = restTemplate;
        this.webSocketService = webSocketService;
    }

    public String queuePrompt(GenerateRequest generateRequest) throws IOException {
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
            copyJson.getJSONObject("6").getJSONObject("inputs").put("text", generateRequest.getPrompt());
            copyJson.getJSONObject("7").getJSONObject("inputs").put("text", generateRequest.getNegativePrompt());
            Integer[] resolution = getResolution(generateRequest.getResolution(), generateRequest.getRatio());
            copyJson.getJSONObject("5").getJSONObject("inputs").put("width", resolution[0]);
            copyJson.getJSONObject("5").getJSONObject("inputs").put("height", resolution[1]);
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


    public Integer[] getResolution(Integer resolution, String ratioString) {
        String[] ratioParts = ratioString.split(":");
        if (ratioParts.length != 2) {
            return new Integer[]{resolution, resolution}; // Default to square if ratio is invalid
        }

        try {
            int x = Integer.parseInt(ratioParts[0]);
            int y = Integer.parseInt(ratioParts[1]);

            if (x <= 0 || y <= 0) {
                return new Integer[]{resolution, resolution}; // Default to square if ratio parts are non-positive
            }

            int smallerDimension = Math.min(x, y);
            int largerDimension = Math.max(x, y);

            // Calculate the other dimension based on the ratio
            if (smallerDimension == x) {
                return new Integer[]{resolution, (int) Math.round(resolution * ((double) y / x))};
            } else {
                return new Integer[]{(int) Math.round(resolution * ((double) x / y)), resolution};
            }
        } catch (NumberFormatException e) {
            return new Integer[]{resolution, resolution}; // Default to square if ratio parts are not integers
        }
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
        File imagesDir = new File(imagesDirPath);
        if (!imagesDir.exists()) {
            imagesDir.mkdirs();
        }

        File lqImagesDir = new File(lq_imagesDirPath);
        if (!lqImagesDir.exists()) {
            lqImagesDir.mkdirs();
        }

        for (String key : outputs.keySet()) {
            JSONObject output = outputs.getJSONObject(key);
            if (output.has("images")) {
                JSONArray images = output.getJSONArray("images");
                for (int j = 0; j < images.length(); j++) {
                    JSONObject image = images.getJSONObject(j);
                    String filename = image.getString("filename");
                    String uidName = UUID.randomUUID().toString() + ".jpg"; // Change the file extension to .jpg
                    String lqUidName = "lq_" + uidName;
                    String subfolder = image.optString("subfolder", "");
                    String type = image.getString("type");

                    File outputFile = new File(imagesDirPath + uidName);
                    File lqOutputFile = new File(lq_imagesDirPath + lqUidName); // File for low-quality image

                    try {
                        byte[] imageData = getImage(filename, subfolder, type);
                        BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageData));
                        ImageIO.write(bufferedImage, "jpg", outputFile); // Save original image as JPG

                        // Resize and save low-quality image
                        BufferedImage lqImage = resizeImage(bufferedImage, bufferedImage.getWidth() / 4, bufferedImage.getHeight() / 4);
                        ImageIO.write(lqImage, "jpg", lqOutputFile);

                        return uidName; // Return the filename of the first successfully processed image
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
    private static BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = resizedImage.createGraphics();
        graphics2D.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        graphics2D.dispose();
        return resizedImage;
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
