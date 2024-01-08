package com.seng.comfy.backend.controllers;


import com.seng.comfy.backend.admin.ApiKeyService;
import com.seng.comfy.backend.entity.*;
import com.seng.comfy.backend.helper.GenerateRequest;
import com.seng.comfy.backend.jwt.JwtUtil;
import com.seng.comfy.backend.repository.*;
import com.seng.comfy.backend.service.ComfyUiService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

// Plus any additional imports for your comfyUiService and other components

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.*;

@RestController
public class RestConroller {


    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserAuthRepository userAuthRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;
    @Autowired
    private CommentRepository commentRepository;
    private ComfyUiService comfyUiService;

    @Autowired
    public void RestController(ComfyUiService comfyUiService) {
        this.comfyUiService = comfyUiService;
    }

    @Autowired
    private ApiKeyService apiKeyService;

    public RestConroller(ComfyUiService comfyUiService) {
        this.comfyUiService = comfyUiService;
    }


    @Autowired
    private ImageRepository imageRepository;
    String imagesDirPath = "src/main/resources/static";

    @Autowired
    private LikeRepository likeRepository;

    @Transactional
    @PostMapping("/generate")
    public ResponseEntity<String> queuePrompt(@RequestBody GenerateRequest request,
                                              @RequestHeader(name = "Authorization") String authorizationHeader) {

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String jwtToken = authorizationHeader.substring(7);
            String username = jwtUtil.extractUsername(jwtToken);

            Optional<UserAuth> userAuth = userAuthRepository.findByUsername(username);

            if (userAuth.isPresent()) {

                long userId = userAuth.get().getUserId();
                Optional<UserProfile> userProfile = userProfileRepository.findById(userId);

                if (userProfile.isPresent()) {


                    int credits = userProfile.get().getCredits();
                    if (credits >= 10) {


                        try {
                            // Process the request object
                            String filename = comfyUiService.queuePrompt(
                                    request
                            );
                            System.out.println("Filename is : " + filename);
                           /* String downloadUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                                    .path("/images/")
                                    .path(filename)
                                    .toUriString();*/
                            String imageId = filename.substring(0, filename.lastIndexOf('.'));

                            userProfile.get().setCredits(credits - 10);
                            userProfileRepository.save(userProfile.get());
                            //userAuthRepository.save(userAuth.get());


                            Image image = new Image(filename.substring(0, filename.lastIndexOf('.')), "images", "lq_images",
                                    "image description", request.getModel(), request.getPrompt(),
                                    userProfile.get(), 0, new Timestamp(System.currentTimeMillis()), false);

                            imageRepository.save(image);

                            return ResponseEntity.ok(imageId);
                        } catch (Exception e) {
                            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error queuing prompt: " + e.getMessage());
                        }


                    } else {
                        return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body("Error queuing prompt: Not enough credits");

                    }


                } else {
                    return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body("Error queuing prompt: User profile not found");

                }

            } else {

                return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body("Error queuing prompt: User auth not found");
            }

        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid JWT token type.");
        }
    }


    @GetMapping("/images/{imageId}")
    public ResponseEntity<?> getImage(@PathVariable String imageId,
                                      @RequestHeader(name = "Authorization") String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid JWT token type.");
        }

        String jwtToken = authorizationHeader.substring(7);
        String username = jwtUtil.extractUsername(jwtToken);
        Optional<UserAuth> userAuth = userAuthRepository.findByUsername(username);

        if (!userAuth.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User auth not found");
        }

        long userId = userAuth.get().getUserId();
        Optional<Image> imageOpt = imageRepository.findByImageId(imageId);

        if (!imageOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        Image image = imageOpt.get();
        boolean isOwner = image.getUser().getUserId() == userId;

        // Check if the image is visible or the user is the owner
        if (!image.isVisible() && !isOwner) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized to access this image");
        }

        // Construct the path to the file
        String filename = image.getImageId() + ".jpg";
        Path filePath = Paths.get(imagesDirPath).resolve(image.getImagePath()).resolve(filename).normalize();

        // Check if the file exists and is readable
        Resource resource;
        try {
            resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().body("Malformed URL for image path");
        }

        // Prepare the content for the JSON response
        Map<String, Object> imageData = new HashMap<>();
        imageData.put("imageId", image.getImageId());
        imageData.put("description", image.getDescription());
        imageData.put("model", image.getModel());
        imageData.put("prompt", image.getPrompt());
        imageData.put("likes", image.getLikes());
        imageData.put("time", image.getTime());
        imageData.put("visible", image.isVisible());

        try {
            List<Comment> comments = commentRepository.findByImage_imageId(image.getImageId());
            List<Map<String, Object>> formattedComments = new ArrayList<>();

            for (Comment comment : comments) {
                Map<String, Object> commentData = new HashMap<>();
                commentData.put("commentId", comment.getCommentId());
                commentData.put("comment", comment.getComment());
                commentData.put("userId", comment.getUser().getUserId()); // Assuming UserProfile has getUserId
                commentData.put("time", comment.getTime());
                String commenterUsername;
                commenterUsername = comment.getUser().getUser().getUsername();
                commentData.put("username", commenterUsername);

                Optional<UserProfile> userProfileOpt;
                userProfileOpt = userProfileRepository.findByUser_username(commenterUsername);
                UserProfile userProfile;
                userProfile = userProfileOpt.get();
                Path ppPath;
                if (userProfileOpt.get().getPpPath() != null) {
                    ppPath = Paths.get(imagesDirPath).resolve(userProfile.getPpPath()).normalize();
                } else {
                    ppPath = Paths.get(imagesDirPath).resolve("pp_images/pp_default.png").normalize();

                }

                try {
                    byte[] imageBytes = Files.readAllBytes(ppPath);
                    String base64Image = Base64.getEncoder().encodeToString(imageBytes);
                    commentData.put("pp", base64Image);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                formattedComments.add(commentData);
            }

            imageData.put("comments", formattedComments);
        } catch (Exception e) {
            System.out.println("Error retrieving comments: " + e.getMessage());
            imageData.put("comments", Collections.emptyList());
        }
        // Add any other image fields you want to include in the response

        try {
            // Read the image file into a byte array
            byte[] imageBytes = StreamUtils.copyToByteArray(resource.getInputStream());
            String encodedImage = Base64.getEncoder().encodeToString(imageBytes);
            imageData.put("imageFile", encodedImage);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(imageData);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Unable to read the image file");
        }
    }


    @Transactional
    @PostMapping("/share")
    public ResponseEntity<String> shareImage(
            @RequestHeader(name = "Authorization") String authorizationHeader,
            @RequestParam String imageId,
            @RequestParam String description) {

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid JWT token type.");
        }

        String jwtToken = authorizationHeader.substring(7);
        String username = jwtUtil.extractUsername(jwtToken);
        Optional<UserAuth> userAuth = userAuthRepository.findByUsername(username);

        if (!userAuth.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User auth not found");
        }

        long userId = userAuth.get().getUserId();
        Optional<Image> imageOptional = imageRepository.findByImageId(imageId);

        if (!imageOptional.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        Image image = imageOptional.get();
        if (image.getUser().getUserId() != userId) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized to share this image");
        }

        image.setDescription(description);
        image.setVisible(true);
        imageRepository.save(image);

        return ResponseEntity.ok("Image shared successfully");
    }


    @Transactional
    @PostMapping("/like")
    public ResponseEntity<String> likeImage(
            @RequestHeader(name = "Authorization") String authorizationHeader,
            @RequestParam String imageId) {


        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid JWT token type.");
        }

        String jwtToken = authorizationHeader.substring(7);
        String username = jwtUtil.extractUsername(jwtToken);
        Optional<UserAuth> userAuth = userAuthRepository.findByUsername(username);

        if (!userAuth.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User auth not found");
        }

        long userId = userAuth.get().getUserId();


        if (userProfileRepository.findByUserId(userId).isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User profile not found");
        }
        UserProfile userProfile = userProfileRepository.findByUserId(userId).get();

        Optional<Image> imageOpt = imageRepository.findByImageId(imageId);

        if (imageOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Image image = imageOpt.get();
        Optional<Like> likeOpt = likeRepository.findByUserAndImage(userProfile, image);


        if (likeOpt.isPresent()) {
            // Unlike the image
            likeRepository.delete(likeOpt.get());
            image.setLikes(image.getLikes() - 1);
            imageRepository.save(image);
            return ResponseEntity.ok("Image unliked successfully");
        } else {
            // Like the image
            Like like = new Like();
            like.setUser(userProfile);
            like.setImage(image);
            likeRepository.save(like);
            image.setLikes(image.getLikes() + 1);
            imageRepository.save(image);
            return ResponseEntity.ok("Image liked successfully");
        }
    }


    @GetMapping("/images")
    public ResponseEntity<?> getImages(@RequestParam int amount,
                                       @RequestParam(required = false) String order,
                                       @RequestParam(required = false) String sort,
                                       @RequestParam(required = false) Long userId,
                                       @RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
        // Check if the order and sort parameters are valid

        if (order == null) {
            order = "time";
        }
        if (sort == null) {
            sort = "descending";
        }
        if (!Arrays.asList("like", "time").contains(order.toLowerCase())) {
            return ResponseEntity.badRequest().body("Invalid order parameter");
        }
        if (!Arrays.asList("ascending", "descending").contains(sort.toLowerCase())) {
            return ResponseEntity.badRequest().body("Invalid sort parameter");
        }

        // Setup sorting direction
        Sort.Direction sortDirection = sort.equalsIgnoreCase("ascending") ? Sort.Direction.ASC : Sort.Direction.DESC;

        // Create Pageable instance
        Pageable pageable = PageRequest.of(0, amount, Sort.by(sortDirection, order));

        // Retrieve visible images from the database
        List<Image> images;
        if (userId != null) {
            if (userId == -1) {
                Optional<UserProfile> userProfile = userProfileRepository.findByUser_username(jwtUtil.extractUsername(authorizationHeader.substring(7)));
                if (userProfile.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User profile not found");
                }
                Long userId1 = userProfile.get().getUserId();
                images = imageRepository.findByUser_userId(userId1);
            } else {
                images = imageRepository.findAllByVisibleAndUser_userId(true, userId, pageable);
            }
        } else {
            images = imageRepository.findAllByVisible(true, pageable);
        }
        // Prepare the JSON response
        List<Map<String, Object>> imagesData = new ArrayList<>();

        for (Image image : images) {
            Map<String, Object> imageData = new HashMap<>();
            imageData.put("imageId", image.getImageId());
            imageData.put("description", image.getDescription());
            imageData.put("model", image.getModel());
            imageData.put("prompt", image.getPrompt());
            imageData.put("likes", image.getLikes());
            imageData.put("time", image.getTime());
            imageData.put("visible", image.isVisible());

            // Construct the path to the low-quality file
            String filename = "lq_" + image.getImageId() + ".jpg";
            Path filePath = Paths.get(imagesDirPath).resolve(image.getLqImagePath()).resolve(filename).normalize();
            System.out.println("File path is : " + filePath.toString());
            try {
                Resource resource = new UrlResource(filePath.toUri());
                if (resource.exists() && resource.isReadable()) {
                    byte[] imageBytes = StreamUtils.copyToByteArray(resource.getInputStream());
                    String encodedImage = Base64.getEncoder().encodeToString(imageBytes);
                    imageData.put("imageFile", encodedImage);
                } else {
                    imageData.put("imageFile", "image file dont exist");
                }
            } catch (IOException e) {
                System.out.println("couldnot read the image file");
                // If the image cannot be read, you can choose to skip it or add some placeholder data
                imageData.put("imageFile", "image couldnot read");
                continue;
            }

            imagesData.add(imageData);
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(imagesData);
    }


    @PostMapping("/admin/credit")
    public ResponseEntity<?> addCredits(
            @RequestHeader("X-API-KEY") String apiKey,
            @RequestParam Long user_id,
            @RequestParam int amount) {

        if (!apiKeyService.isValidKey(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid API Key");
        }

        Optional<UserProfile> userOptional = userProfileRepository.findByUserId(user_id);
        if (!userOptional.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        UserProfile userProfile = userOptional.get();
        userProfile.setCredits(userProfile.getCredits() + amount);
        userProfileRepository.save(userProfile);

        return ResponseEntity.ok().body("Credits added successfully");
    }


    @Transactional
    @PostMapping("/profile-update")
    public ResponseEntity<String> updateProfile(
            @RequestHeader(name = "Authorization") String authorizationHeader,
            @RequestParam(required = false) MultipartFile pp,
            @RequestParam(required = false) String bio) {

        Set<String> allowedExtensions = new HashSet<>(Arrays.asList("jpg", "jpeg", "png"));
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid JWT token type.");
        }

        String jwtToken = authorizationHeader.substring(7);
        String curr_username = jwtUtil.extractUsername(jwtToken);
        Optional<UserProfile> userProfileOpt = userProfileRepository.findByUser_username(curr_username);

        if (userProfileOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User profile not found");
        }

        UserProfile userProfile = userProfileOpt.get();

        try {
            if (pp != null && !pp.isEmpty()) {
                String originalFilename = pp.getOriginalFilename();
                String fileExtension = "";
                if (originalFilename != null && originalFilename.lastIndexOf(".") != -1) {
                    fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
                }
                if (!allowedExtensions.contains(fileExtension)) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Unsupported image format");
                }
                String newFilename = userProfile.getUserId() + fileExtension;
                Path imagePath = Paths.get(imagesDirPath, "pp_images", newFilename);
                Files.createDirectories(imagePath.getParent());
                Files.copy(pp.getInputStream(), imagePath, StandardCopyOption.REPLACE_EXISTING);
                userProfile.setPpPath("pp_images/" + newFilename);
            }
            if (bio != null && !bio.isEmpty()) {
                userProfile.setBio(bio);
            }


            userProfileRepository.save(userProfile);
            return ResponseEntity.ok("Profile updated successfully");

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error while updating profile");
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile(@RequestParam(required = false) Long userId,
                                            @RequestHeader(name = "Authorization", required = false) String authorizationHeader) {


        Optional<UserProfile> userProfileOpt;
        if (userId != null) {
            userProfileOpt = userProfileRepository.findByUserId(userId);
        } else {
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid JWT token type.");
            }
            String jwtToken = authorizationHeader.substring(7);
            String username = jwtUtil.extractUsername(jwtToken);
            userProfileOpt = userProfileRepository.findByUser_username(username);
        }


        if (userProfileOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User profile not found");
        }
        UserProfile userProfile = userProfileOpt.get();
        JSONObject userProfileJson = new JSONObject();
        userProfileJson.put("username", userProfile.getUser().getUsername());
        userProfileJson.put("email", userProfile.getUser().getEmail());
        userProfileJson.put("bio", userProfile.getBio());
        userProfileJson.put("credits", userProfile.getCredits());
        userProfileJson.put("id", userProfile.getUserId());
        Path ppPath;
        if (userProfileOpt.get().getPpPath() != null) {
            ppPath = Paths.get(imagesDirPath).resolve(userProfile.getPpPath()).normalize();
        } else {
            ppPath = Paths.get(imagesDirPath).resolve("pp_images/pp_default.png").normalize();

        }

        try {
            byte[] imageBytes = Files.readAllBytes(ppPath);
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            userProfileJson.put("pp", base64Image);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return ResponseEntity.ok(userProfileJson.toString());
    }

    @PostMapping("/comment")
    public ResponseEntity<?> postComment(@RequestHeader(name = "Authorization") String authorizationHeader,
                                         @RequestParam String imageId,
                                         @RequestParam String commentText) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid JWT token.");
        }

        String jwtToken = authorizationHeader.substring(7);
        String username = jwtUtil.extractUsername(jwtToken);
        Optional<UserAuth> userAuthOpt = userAuthRepository.findByUsername(username);

        if (userAuthOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        }

        Optional<Image> imageOpt = imageRepository.findByImageId(imageId);
        if (imageOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Image not found.");
        }

        Comment comment = new Comment();
        comment.setUser(userAuthOpt.get().getUserProfile()); // Assuming UserAuth has a reference to UserProfile
        comment.setImage(imageOpt.get());
        comment.setComment(commentText);
        comment.setTime(new Timestamp(System.currentTimeMillis()));

        commentRepository.save(comment);

        return ResponseEntity.ok("Comment added successfully.");
    }


    @GetMapping("/images-liked")
    public ResponseEntity<?> getImagesLiked(@RequestParam int amount,
                                            @RequestParam(required = false) String order,
                                            @RequestParam(required = false) String sort,
                                            @RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
        // Check if the order and sort parameters are valid

        if (order == null) {
            order = "time";
        }
        if (sort == null) {
            sort = "descending";
        }
        if (!Arrays.asList("like", "time").contains(order.toLowerCase())) {
            return ResponseEntity.badRequest().body("Invalid order parameter");
        }
        if (!Arrays.asList("ascending", "descending").contains(sort.toLowerCase())) {
            return ResponseEntity.badRequest().body("Invalid sort parameter");
        }

        // Setup sorting direction
        Sort.Direction sortDirection = sort.equalsIgnoreCase("ascending") ? Sort.Direction.ASC : Sort.Direction.DESC;

        // Create Pageable instance
        Pageable pageable = PageRequest.of(0, amount, Sort.by(sortDirection, order));

        // Retrieve visible images from the database
        List<Image> images;

        images = imageRepository.findAllByVisible(true, pageable);
        // Prepare the JSON response
        List<Map<String, Object>> imagesData = new ArrayList<>();

        for (Image image : images) {
            Map<String, Object> imageData = new HashMap<>();
            imageData.put("imageId", image.getImageId());
            imageData.put("description", image.getDescription());
            imageData.put("model", image.getModel());
            imageData.put("prompt", image.getPrompt());
            imageData.put("likes", image.getLikes());
            imageData.put("time", image.getTime());
            imageData.put("visible", image.isVisible());

            // Construct the path to the low-quality file
            String filename = "lq_" + image.getImageId() + ".jpg";
            Path filePath = Paths.get(imagesDirPath).resolve(image.getLqImagePath()).resolve(filename).normalize();
            System.out.println("File path is : " + filePath.toString());
            try {
                Resource resource = new UrlResource(filePath.toUri());
                if (resource.exists() && resource.isReadable()) {
                    byte[] imageBytes = StreamUtils.copyToByteArray(resource.getInputStream());
                    String encodedImage = Base64.getEncoder().encodeToString(imageBytes);
                    imageData.put("imageFile", encodedImage);
                } else {
                    imageData.put("imageFile", "image file dont exist");
                }
            } catch (IOException e) {
                System.out.println("couldnot read the image file");
                // If the image cannot be read, you can choose to skip it or add some placeholder data
                imageData.put("imageFile", "image couldnot read");
                continue;
            }

            imagesData.add(imageData);
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(imagesData);
    }



    @GetMapping("/liked-images")
    public ResponseEntity<?> getLikedImages(@RequestHeader(name = "Authorization") String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized: No JWT token provided.");
        }

        try {
            String jwtToken = authorizationHeader.substring(7);
            String username = jwtUtil.extractUsername(jwtToken);
            Optional<UserAuth> userAuthOpt = userAuthRepository.findByUsername(username);
            if (!userAuthOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
            }

            Long userId = userAuthOpt.get().getUserId();
            List<Image> likedImages = imageRepository.findLikedImagesByUserId(userId);

            // Normally, you would convert the entities to DTOs. For simplicity, we're returning the entities directly.
            // In a real-world scenario, you should convert these to a format that is decoupled from your database entities.
            return ResponseEntity.ok(likedImages);

        } catch (Exception e) {
            // Generic exception handling, replace this with more specific handling if necessary
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }

    @GetMapping("/hello")
    public String home() {
        return ("Welcome to Comfy API");
    }
}




