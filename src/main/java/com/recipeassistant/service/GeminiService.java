package com.recipeassistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recipeassistant.model.CookingTipsResult;
import com.recipeassistant.model.DetectedIngredientsResult;
import com.recipeassistant.model.IngredientValidationLLMResponse;
import com.recipeassistant.model.IngredientValidationResult;
import com.recipeassistant.model.StructuredRecipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    private static final String RECIPE_JSON_SCHEMA = """
        {
          "type": "object",
          "required": ["name","preparationTime","cookingTime","servings","ingredients","instructions","tips","nutrition"],
          "properties": {
            "name": { "type": "string" },
            "preparationTime": { "type": "string" },
            "cookingTime": { "type": "string" },
            "servings": { "type": "string" },
            "ingredients": { "type": "array", "items": { "type": "string" } },
            "instructions": { "type": "array", "items": { "type": "string" } },
            "tips": { "type": "array", "items": { "type": "string" } },
            "nutrition": {
              "type": "object",
              "required": ["calories", "protein", "carbs", "fat"],
              "properties": {
                "calories": { "type": "string" },
                "protein": { "type": "string" },
                "carbs": { "type": "string" },
                "fat": { "type": "string" }
              }
            }
          }
        }
        """;

    private static final String TIPS_JSON_SCHEMA = """
        {
          "type": "object",
          "required": ["tips"],
          "properties": {
            "tips": { "type": "array", "items": { "type": "string" } }
          }
        }
        """;

    private static final String VALIDATION_JSON_SCHEMA = """
        {
          "type": "object",
          "required": ["valid", "invalidItems", "message"],
          "properties": {
            "valid": { "type": "boolean" },
            "invalidItems": { "type": "array", "items": { "type": "string" } },
            "message": { "type": "string" }
          }
        }
        """;

    private static final String VISION_JSON_SCHEMA = """
        {
          "type": "object",
          "required": ["ingredients", "summary"],
          "properties": {
            "ingredients": { "type": "array", "items": { "type": "string" } },
            "summary": { "type": "string" }
          }
        }
        """;

    @Value("${gemini.api.base-url:https://generativelanguage.googleapis.com/v1beta}")
    private String baseUrl;

    @Value("${gemini.api.model.recipe:gemini-3.6-flash}")
    private String recipeModel;

    @Value("${gemini.api.model.tips:gemini-3.6-flash}")
    private String tipsModel;

    @Value("${gemini.api.model.validation:gemini-3.6-flash}")
    private String validationModel;

    @Value("${gemini.api.model.vision:gemini-3.6-flash}")
    private String visionModel;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeminiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean validateApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return false;        }
        try {
            String url = baseUrl + "/models?key=" + apiKey.trim();
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-goog-api-key", apiKey.trim());
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (HttpStatusCodeException e) {
            log.warn("Gemini key validation failed: status={}", e.getStatusCode());
            return false;
        } catch (Exception e) {
            log.warn("Gemini key validation failed: {}", e.getMessage());
            return false;
        }
    }

    public StructuredRecipe generateRecipe(String apiKey, String ingredients, String cuisine,
                                           String dietaryRestrictions, boolean onlyListedIngredients,
                                           Runnable cancellationCheck) {
        String prompt = buildRecipePrompt(ingredients, cuisine, dietaryRestrictions, onlyListedIngredients);
        String json = callGemini(apiKey, recipeModel, prompt, RECIPE_JSON_SCHEMA, cancellationCheck, 2048, 0.7);
        return parseJson(json, StructuredRecipe.class);
    }

    public CookingTipsResult getCookingTips(String apiKey, String ingredients, Runnable cancellationCheck) {
        String prompt = buildCookingTipsPrompt(ingredients);
        String json = callGemini(apiKey, tipsModel, prompt, TIPS_JSON_SCHEMA, cancellationCheck, 2048, 0.7);
        return parseJson(json, CookingTipsResult.class);
    }

    public IngredientValidationResult validateIngredients(String apiKey, String ingredients,
                                                          Runnable cancellationCheck) {
        String prompt = buildIngredientValidationPrompt(ingredients);
        String json = callGemini(apiKey, validationModel, prompt, VALIDATION_JSON_SCHEMA,
            cancellationCheck, 256, 0.1);
        IngredientValidationLLMResponse parsed = parseJson(json, IngredientValidationLLMResponse.class);
        if (parsed.valid()) {
            return IngredientValidationResult.ok();
        }
        List<String> invalid = parsed.invalidItems() != null ? parsed.invalidItems() : List.of();
        return IngredientValidationResult.fail(invalid, parsed.message());
    }

    public DetectedIngredientsResult detectIngredientsFromImage(String apiKey, byte[] imageBytes, String mimeType,
                                                                Runnable cancellationCheck) {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("Image data is required.");
        }
        String safeMime = (mimeType != null && !mimeType.isBlank()) ? mimeType : "image/jpeg";
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        String prompt = "You are a professional AI culinary chef. Identify all raw food ingredients, fresh produce, vegetables, meats, poultry, seafood, dairy, herbs, seasonings, spices, and packaged food items visible in this image. "
            + "Return a JSON object with: 1) 'ingredients': a list of clean, distinct ingredient names (e.g. ['chicken breast', 'roma tomatoes', 'fresh basil', 'olive oil']), 2) 'summary': a concise 1-sentence description of the scanned items.";

        String json = callGeminiMultimodal(apiKey, visionModel, prompt, safeMime, base64Image, VISION_JSON_SCHEMA, cancellationCheck, 1024, 0.2);
        return parseJson(json, DetectedIngredientsResult.class);
    }

    public String recipeToPlainText(StructuredRecipe recipe) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(recipe);
        } catch (Exception e) {
            return recipe.getName();
        }
    }

    public String recipeToJson(StructuredRecipe recipe) {
        try {
            return objectMapper.writeValueAsString(recipe);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize recipe", e);
        }
    }

    private String buildRecipePrompt(String ingredients, String cuisine, String dietaryRestrictions,
                                     boolean onlyListedIngredients) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Create a detailed home-cooking recipe using: ").append(ingredients).append(". ");
        if (onlyListedIngredients) {
            prompt.append("STRICT: Use ONLY the listed ingredients plus basic pantry salt, pepper, and water. ");
            prompt.append("Do not add any other ingredient. ");
        }
        if (cuisine != null && !cuisine.isBlank()) {
            prompt.append("Cuisine style: ").append(cuisine).append(". ");
        }
        if (dietaryRestrictions != null && !dietaryRestrictions.isBlank()) {
            prompt.append("Dietary requirements: ").append(dietaryRestrictions).append(". ");
        }
        prompt.append("Return JSON only matching the schema. Use realistic times and clear step-by-step instructions.");
        return prompt.toString();
    }

    private String buildCookingTipsPrompt(String ingredients) {
        return "Provide 5-8 practical cooking tips for these ingredients: " + ingredients
            + ". Return JSON only matching the schema.";
    }

    private String buildIngredientValidationPrompt(String ingredients) {
        return """
            You validate recipe ingredient lists. Input is a comma-separated list the user wants to cook with.

            Rules:
            - Accept real food: produce, meat, seafood, dairy, grains, legumes, spices, herbs, oils, sauces, regional staples, and obvious typos that clearly mean food (e.g. chiken -> chicken).
            - Reject non-food objects (car, temple, phone, laptop), places, abstract words, URLs, numbers-only tokens, and nonsense.
            - Do NOT creatively reinterpret invalid words as food (never map "car" to carrot or "temple" to a dish theme).
            - If ANY item is invalid, set valid=false and list every invalid token exactly as the user typed it in invalidItems.
            - message: one short, friendly sentence for the user explaining the problem.

            Ingredients to validate: %s
            """.formatted(ingredients);
    }

    private <T> T parseJson(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Gemini JSON response: " + json, e);
        }
    }

    private String callGemini(String apiKey, String model, String prompt, String jsonSchema,
                              Runnable cancellationCheck, int maxTokens, double temperature) {
        return doCallGemini(
                apiKey,
                model,
                prompt,
                jsonSchema,
                cancellationCheck,
                maxTokens,
                temperature
        );
    }

    private String doCallGemini(String apiKey, String model, String prompt, String jsonSchema,
                                Runnable cancellationCheck, int maxTokens, double temperature) {
        if (cancellationCheck != null) {
            cancellationCheck.run();
        }
        try {
            String url = baseUrl + "/models/" + model + ":generateContent?key=" + apiKey.trim();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-goog-api-key", apiKey.trim());

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", List.of(
                Map.of("role", "user", "parts", List.of(Map.of("text", prompt)))
            ));

            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", temperature);
            generationConfig.put("maxOutputTokens", maxTokens);
            generationConfig.put("responseMimeType", "application/json");
            if (jsonSchema != null) {
                generationConfig.put("responseSchema", objectMapper.readTree(jsonSchema));
            }
            requestBody.put("generationConfig", generationConfig);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            if (cancellationCheck != null) {
                cancellationCheck.run();
            }
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new GeminiClientException(response.getStatusCode());
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode candidates = root.path("candidates");
            if (candidates.isEmpty()) {
                throw new GeminiClientException("Gemini returned no candidates (blocked by safety or invalid request)");
            }
            return candidates.get(0).path("content").path("parts").get(0).path("text").asText();
        } catch (GeminiClientException e) {
            throw e;
        } catch (HttpStatusCodeException e) {
            log.error("Gemini API HTTP error: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new GeminiClientException(e.getStatusCode());
        } catch (Exception e) {
            log.error("Gemini API call failed: {}", e.getMessage(), e);
            throw new GeminiClientException("Gemini request failed: " + e.getMessage());
        }
    }

    private String callGeminiMultimodal(String apiKey, String model, String prompt,
                                        String mimeType, String base64Image,
                                        String jsonSchema, Runnable cancellationCheck,
                                        int maxTokens, double temperature) {
        return doCallGeminiMultimodal(
                apiKey,
                model,
                prompt,
                mimeType,
                base64Image,
                jsonSchema,
                cancellationCheck,
                maxTokens,
                temperature
        );
    }

    private String doCallGeminiMultimodal(String apiKey, String model, String prompt, String mimeType, String base64Image,
                                          String jsonSchema, Runnable cancellationCheck, int maxTokens, double temperature) {
        if (cancellationCheck != null) {
            cancellationCheck.run();
        }
        try {
            String url = baseUrl + "/models/" + model + ":generateContent?key=" + apiKey.trim();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-goog-api-key", apiKey.trim());

            Map<String, Object> textPart = Map.of("text", prompt);
            Map<String, Object> inlineDataPart = Map.of("inlineData", Map.of(
                "mimeType", mimeType,
                "data", base64Image
            ));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", List.of(
                Map.of("role", "user", "parts", List.of(textPart, inlineDataPart))
            ));

            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", temperature);
            generationConfig.put("maxOutputTokens", maxTokens);
            generationConfig.put("responseMimeType", "application/json");
            if (jsonSchema != null) {
                generationConfig.put("responseSchema", objectMapper.readTree(jsonSchema));
            }
            requestBody.put("generationConfig", generationConfig);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            if (cancellationCheck != null) {
                cancellationCheck.run();
            }
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new GeminiClientException(response.getStatusCode());
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode candidates = root.path("candidates");
            if (candidates.isEmpty()) {
                throw new GeminiClientException("Gemini returned no candidates for image");
            }
            return candidates.get(0).path("content").path("parts").get(0).path("text").asText();
        } catch (GeminiClientException e) {
            throw e;
        } catch (HttpStatusCodeException e) {
            log.error("Gemini Vision HTTP error: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new GeminiClientException(e.getStatusCode());
        } catch (Exception e) {
            log.error("Gemini Vision call failed: {}", e.getMessage(), e);
            throw new GeminiClientException("Gemini Vision request failed: " + e.getMessage());
        }
    }
}
