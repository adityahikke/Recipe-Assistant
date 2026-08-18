package com.recipeassistant.controller;

import com.recipeassistant.model.*;
import com.recipeassistant.service.*;
import com.recipeassistant.util.ClientIpResolver;
import com.recipeassistant.util.ContentHashUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
public class RecipeController {

    private static final Logger log = LoggerFactory.getLogger(RecipeController.class);
    private static final String GENERIC_ERROR = "An error occurred. Please try again later.";
    private static final String TRIAL_DEVICE_NOTICE =
        "No signup required. Five free recipes per device (browser cookie). Your cookbook stays on this device — clearing cookies resets the trial.";

    @Autowired private GeminiService geminiService;
    @Autowired private SecurityAuditService securityAuditService;
    @Autowired private EncryptionService encryptionService;
    @Autowired private GeminiKeyService geminiKeyService;
    @Autowired private TrialClientService trialClientService;
    @Autowired private RecipeLibraryService recipeLibraryService;
    @Autowired private GenerationCancellationService cancellationService;
    @Autowired private IngredientValidator ingredientValidator;

    @Value("${app.asset-version:1.0.0}")
    private String assetVersion;

    @GetMapping("/")
    public String index(Model model, HttpSession session, HttpServletRequest request, HttpServletResponse response) {
        String clientId = trialClientService.ensureClientId(request, response);
        model.addAttribute("recipeRequest", new RecipeRequest());
        model.addAttribute("hasApiKey", geminiKeyService.hasUserKey(session));
        model.addAttribute("defaultKeyAvailable", geminiKeyService.isDefaultKeyConfigured());
        model.addAttribute("defaultTrialsRemaining", geminiKeyService.getDefaultTrialsRemaining(clientId));
        model.addAttribute("defaultRecipesMax", geminiKeyService.getMaxDefaultRecipesPerSession());
        model.addAttribute("defaultRecipesUsed", geminiKeyService.getDefaultRecipesUsed(clientId));
        model.addAttribute("trialDeviceNotice", TRIAL_DEVICE_NOTICE);
        model.addAttribute("assetVersion", assetVersion);
        return "index";
    }

    @PostMapping("/set-api-key")
    @ResponseBody
    public ResponseEntity<RecipeResponse> setApiKey(@Valid @RequestBody ApiKeyRequest request,
                                                   BindingResult bindingResult,
                                                   HttpSession session,
                                                   HttpServletRequest httpRequest) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest()
                .body(new RecipeResponse(false, null, validationMessage(bindingResult)));
        }
        try {
            if (geminiService.validateApiKey(request.getApiKey())) {
                session.setAttribute(GeminiKeyService.SESSION_USER_KEY, encryptionService.encrypt(request.getApiKey()));
                securityAuditService.logApiKeySet(ClientIpResolver.resolve(httpRequest));
                return ResponseEntity.ok(new RecipeResponse(true, "API key encrypted and stored successfully!", null));
            }
            return ResponseEntity.badRequest()
                .body(new RecipeResponse(false, null, "Invalid Gemini API key. Please check and try again."));
        } catch (Exception e) {
            log.warn("API key validation failed for {}", ClientIpResolver.resolve(httpRequest));
            securityAuditService.logApiKeyValidationFailed(ClientIpResolver.resolve(httpRequest), "validation error");
            return ResponseEntity.internalServerError().body(new RecipeResponse(false, null, GENERIC_ERROR));
        }
    }

    @PostMapping("/clear-api-key")
    @ResponseBody
    public RecipeResponse clearApiKey(HttpSession session) {
        session.removeAttribute(GeminiKeyService.SESSION_USER_KEY);
        return new RecipeResponse(true, "API key cleared successfully!", null);
    }

    @GetMapping("/api-key-status")
    @ResponseBody
    public RecipeResponse getApiKeyStatus(HttpSession session, HttpServletRequest request, HttpServletResponse response) {
        String clientId = trialClientService.ensureClientId(request, response);
        RecipeResponse apiResponse = new RecipeResponse();
        boolean hasUserKey = geminiKeyService.hasUserKey(session);
        populateTrialFields(apiResponse, clientId);
        apiResponse.setHasUserKey(hasUserKey);
        apiResponse.setTrialDeviceNotice(TRIAL_DEVICE_NOTICE);

        if (hasUserKey) {
            apiResponse.setSuccess(true);
            apiResponse.setContent("Your API key is set. Unlimited recipe generation.");
            apiResponse.setKeySource("user");
            return apiResponse;
        }

        if (geminiKeyService.isDefaultKeyConfigured()) {
            int remaining = geminiKeyService.getDefaultTrialsRemaining(clientId);
            apiResponse.setKeySource("default");
            apiResponse.setSuccess(remaining > 0);
            apiResponse.setContent(remaining > 0
                ? "Free trial: " + remaining + " recipe(s) remaining on this device."
                : null);
            if (remaining == 0) {
                apiResponse.setError("Free trial used on this device. Add your Gemini API key to continue.");
            }
            return apiResponse;
        }

        apiResponse.setSuccess(false);
        apiResponse.setError("No API key configured. Add your Gemini API key to generate recipes.");
        return apiResponse;
    }

    @PostMapping("/cancel-generation")
    @ResponseBody
    public ResponseEntity<RecipeResponse> cancelGeneration(HttpServletRequest httpRequest,
                                                           HttpServletResponse httpResponse) {
        String clientId = trialClientService.ensureClientId(httpRequest, httpResponse);
        cancellationService.requestCancel(clientId);
        return ResponseEntity.ok(new RecipeResponse(true, "Cancellation requested.", null));
    }

    @PostMapping("/generate-recipe")
    @ResponseBody
    public ResponseEntity<RecipeResponse> generateRecipe(@Valid @RequestBody RecipeRequest request,
                                                        BindingResult bindingResult,
                                                        HttpSession session,
                                                        HttpServletRequest httpRequest,
                                                        HttpServletResponse httpResponse) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(new RecipeResponse(false, null, "Invalid recipe request."));
        }

        String clientId = trialClientService.ensureClientId(httpRequest, httpResponse);
        GeminiKeyService.ResolvedKey resolved = geminiKeyService.resolveKey(session, clientId, encryptionService, true);
        if (!resolved.isValid()) {
            return trialDenied(resolved, clientId, session);
        }

        try {
            cancellationService.register(clientId);
            Runnable checkCancelled = () -> cancellationService.throwIfCancelled(clientId);

            ResponseEntity<RecipeResponse> ingredientRejection = rejectInvalidIngredients(
                resolved.getApiKey(), request.getIngredients(), checkCancelled);
            if (ingredientRejection != null) {
                return ingredientRejection;
            }

            StructuredRecipe recipe = geminiService.generateRecipe(
                resolved.getApiKey(), request.getIngredients(), request.getCuisine(),
                request.getDietaryRestrictions(), request.isOnlyListedIngredients(), checkCancelled);
            cancellationService.throwIfCancelled(clientId);
            if (resolved.isIncrementTrialOnSuccess()) {
                geminiKeyService.incrementDefaultRecipeCount(clientId, httpRequest);
            }
            securityAuditService.logRecipeGeneration(ClientIpResolver.resolve(httpRequest), request.getIngredients());
            String recipeJson = geminiService.recipeToJson(recipe);
            String contentHash = ContentHashUtil.sha256(recipeJson);
            Recipe saved = recipeLibraryService.persistGeneratedRecipe(
                clientId, recipe, recipeJson, contentHash,
                request.getIngredients(), request.getCuisine(), request.getDietaryRestrictions()
            );
            RecipeResponse body = buildRecipeSuccess(recipe, resolved, clientId, session);
            body.setRecipeId(saved.getId().toString());
            body.setContentHash(contentHash);
            body.setFavorited(recipeLibraryService.isInSavedCollection(clientId, saved));
            return ResponseEntity.ok(body);
        } catch (GenerationCancelledException e) {
            return ResponseEntity.status(499)
                .body(new RecipeResponse(false, null, "Generation cancelled."));
        } catch (GeminiClientException e) {
            log.error("Recipe generation Gemini error for {}", ClientIpResolver.resolve(httpRequest), e);
            if (e.isUnauthorizedOrForbidden()) {
                return ResponseEntity.badRequest()
                    .body(new RecipeResponse(false, null, "Invalid Gemini API key. Check your key in .env or the API Key field."));
            }
            if (e.isRateLimitedOrQuotaExceeded()) {
                return ResponseEntity.status(429)
                    .body(new RecipeResponse(false, null, "Gemini rate limit or quota exceeded (HTTP 429). Please check your Google AI Studio quota, or enter a valid API key in the API Key box."));
            }
            if (e.isServiceUnavailable()) {
                return ResponseEntity.status(503)
                    .body(new RecipeResponse(false, null, "Gemini service is temporarily experiencing high demand. Please try again in a few moments."));
            }
            if (e.isBadRequest()) {
                return ResponseEntity.internalServerError()
                    .body(new RecipeResponse(false, null, "Recipe format error. Please try again."));
            }
            return ResponseEntity.badRequest()
                .body(new RecipeResponse(false, null, "Gemini API rejected the request. Please try again."));
        } catch (Exception e) {
            log.error("Recipe generation failed for {}", ClientIpResolver.resolve(httpRequest), e);
            return ResponseEntity.internalServerError().body(new RecipeResponse(false, null, GENERIC_ERROR));
        } finally {
            cancellationService.clear(clientId);
        }
    }

    @PostMapping("/get-cooking-tips")
    @ResponseBody
    public ResponseEntity<RecipeResponse> getCookingTips(@Valid @RequestBody RecipeRequest request,
                                                        BindingResult bindingResult,
                                                        HttpSession session,
                                                        HttpServletRequest httpRequest,
                                                        HttpServletResponse httpResponse) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(new RecipeResponse(false, null, "Invalid request."));
        }

        String clientId = trialClientService.ensureClientId(httpRequest, httpResponse);
        GeminiKeyService.ResolvedKey resolved = geminiKeyService.resolveKey(session, clientId, encryptionService, true);
        if (!resolved.isValid()) {
            return trialDenied(resolved, clientId, session);
        }

        try {
            cancellationService.register(clientId);
            Runnable checkCancelled = () -> cancellationService.throwIfCancelled(clientId);

            ResponseEntity<RecipeResponse> ingredientRejection = rejectInvalidIngredients(
                resolved.getApiKey(), request.getIngredients(), checkCancelled);
            if (ingredientRejection != null) {
                return ingredientRejection;
            }

            CookingTipsResult tips = geminiService.getCookingTips(
                resolved.getApiKey(), request.getIngredients(), checkCancelled);
            cancellationService.throwIfCancelled(clientId);
            if (resolved.isIncrementTrialOnSuccess()) {
                geminiKeyService.incrementDefaultRecipeCount(clientId, httpRequest);
            }
            RecipeResponse body = new RecipeResponse(true, null, null);
            body.setCookingTips(tips);
            body.setKeySource(resolved.getSource().name().toLowerCase());
            populateTrialFields(body, clientId);
            return ResponseEntity.ok(body);
        } catch (GenerationCancelledException e) {
            return ResponseEntity.status(499)
                .body(new RecipeResponse(false, null, "Generation cancelled."));
        } catch (GeminiClientException e) {
            log.error("Cooking tips Gemini error for {}", ClientIpResolver.resolve(httpRequest), e);
            if (e.isUnauthorizedOrForbidden()) {
                return ResponseEntity.badRequest()
                    .body(new RecipeResponse(false, null, "Invalid Gemini API key. Check your key in .env or the API Key field."));
            }
            if (e.isRateLimitedOrQuotaExceeded()) {
                return ResponseEntity.status(429)
                    .body(new RecipeResponse(false, null, "Gemini rate limit or quota exceeded (HTTP 429). Please check your Google AI Studio quota, or enter a valid API key in the API Key box."));
            }
            if (e.isServiceUnavailable()) {
                return ResponseEntity.status(503)
                    .body(new RecipeResponse(false, null, "Gemini service is temporarily experiencing high demand. Please try again in a few moments."));
            }
            return ResponseEntity.badRequest()
                .body(new RecipeResponse(false, null, "Gemini API rejected the request. Please try again."));
        } catch (Exception e) {
            log.error("Cooking tips failed for {}", ClientIpResolver.resolve(httpRequest), e);
            return ResponseEntity.internalServerError().body(new RecipeResponse(false, null, GENERIC_ERROR));
        } finally {
            cancellationService.clear(clientId);
        }
    }

    @PostMapping("/detect-ingredients-image")
    @ResponseBody
    public ResponseEntity<RecipeResponse> detectIngredientsFromImage(
            @RequestParam("image") org.springframework.web.multipart.MultipartFile image,
            HttpSession session,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        if (image == null || image.isEmpty()) {
            return ResponseEntity.badRequest().body(new RecipeResponse(false, null, "Please select or capture a photo to scan."));
        }

        String clientId = trialClientService.ensureClientId(httpRequest, httpResponse);
        GeminiKeyService.ResolvedKey resolved = geminiKeyService.resolveKey(session, clientId, encryptionService, true);
        if (!resolved.isValid()) {
            return trialDenied(resolved, clientId, session);
        }

        try {
            cancellationService.register(clientId);
            Runnable checkCancelled = () -> cancellationService.throwIfCancelled(clientId);

            byte[] bytes = image.getBytes();
            String contentType = image.getContentType();
            if (contentType == null || contentType.isBlank() || !contentType.startsWith("image/")) {
                String originalName = image.getOriginalFilename();
                if (originalName != null && originalName.toLowerCase().endsWith(".png")) {
                    contentType = "image/png";
                } else if (originalName != null && originalName.toLowerCase().endsWith(".webp")) {
                    contentType = "image/webp";
                } else {
                    contentType = "image/jpeg";
                }
            }

            DetectedIngredientsResult detected = geminiService.detectIngredientsFromImage(
                resolved.getApiKey(), bytes, contentType, checkCancelled);

            if (resolved.isIncrementTrialOnSuccess()) {
                geminiKeyService.incrementDefaultRecipeCount(clientId, httpRequest);
            }

            RecipeResponse body = new RecipeResponse(true, "Ingredients detected successfully!", null);
            body.setDetectedIngredients(detected.ingredients());
            body.setDetectedSummary(detected.summary());
            body.setKeySource(resolved.getSource().name().toLowerCase());
            populateTrialFields(body, clientId);
            return ResponseEntity.ok(body);
        } catch (GenerationCancelledException e) {
            return ResponseEntity.status(499)
                .body(new RecipeResponse(false, null, "Image scanning cancelled."));
        } catch (GeminiClientException e) {
            log.error("Gemini Vision error for {}", ClientIpResolver.resolve(httpRequest), e);
            if (e.isUnauthorizedOrForbidden()) {
                return ResponseEntity.badRequest()
                    .body(new RecipeResponse(false, null, "Invalid Gemini API key. Check your key in .env or the API Key field."));
            }
            if (e.isRateLimitedOrQuotaExceeded()) {
                return ResponseEntity.status(429)
                    .body(new RecipeResponse(false, null, "Gemini rate limit or quota exceeded (HTTP 429)."));
            }
            String detail = e.getMessage() != null && !e.getMessage().isBlank() ? e.getMessage() : "Could not detect ingredients from image. Please try a clearer food photo.";
            return ResponseEntity.badRequest().body(new RecipeResponse(false, null, detail));
        } catch (Exception e) {
            log.error("Image ingredient detection failed for {}", ClientIpResolver.resolve(httpRequest), e);
            return ResponseEntity.internalServerError().body(new RecipeResponse(false, null, "Could not process image: " + e.getMessage()));
        } finally {
            cancellationService.clear(clientId);
        }
    }

    private RecipeResponse buildRecipeSuccess(StructuredRecipe recipe, GeminiKeyService.ResolvedKey resolved,
                                              String clientId, HttpSession session) {
        RecipeResponse body = new RecipeResponse(true, geminiService.recipeToPlainText(recipe), null);
        body.setRecipe(recipe);
        body.setContentHash(ContentHashUtil.sha256(geminiService.recipeToJson(recipe)));
        body.setKeySource(resolved.getSource().name().toLowerCase());
        body.setHasUserKey(geminiKeyService.hasUserKey(session));
        populateTrialFields(body, clientId);
        body.setTrialDeviceNotice(TRIAL_DEVICE_NOTICE);
        return body;
    }

    private ResponseEntity<RecipeResponse> trialDenied(GeminiKeyService.ResolvedKey resolved, String clientId,
                                                       HttpSession session) {
        int status = geminiKeyService.isDefaultKeyConfigured() && !geminiKeyService.hasUserKey(session)
            && geminiKeyService.getDefaultTrialsRemaining(clientId) == 0 ? 429 : 401;
        RecipeResponse body = new RecipeResponse(false, null, resolved.getErrorMessage());
        populateTrialFields(body, clientId);
        body.setTrialDeviceNotice(TRIAL_DEVICE_NOTICE);
        return ResponseEntity.status(status).body(body);
    }

    private void populateTrialFields(RecipeResponse response, String clientId) {
        response.setDefaultKeyAvailable(geminiKeyService.isDefaultKeyConfigured());
        response.setDefaultTrialsRemaining(geminiKeyService.getDefaultTrialsRemaining(clientId));
        response.setDefaultRecipesMax(geminiKeyService.getMaxDefaultRecipesPerSession());
        response.setDefaultRecipesUsed(geminiKeyService.getDefaultRecipesUsed(clientId));
    }

    private String validationMessage(BindingResult bindingResult) {
        return bindingResult.getFieldErrors().stream()
            .map(error -> error.getDefaultMessage())
            .findFirst()
            .orElse("Invalid request");
    }

    private ResponseEntity<RecipeResponse> rejectInvalidIngredients(String apiKey, String ingredients,
                                                                    Runnable cancellationCheck) {
        try {
            IngredientValidationResult validation = ingredientValidator.validate(apiKey, ingredients, cancellationCheck);
            if (!validation.valid()) {
                return ResponseEntity.badRequest()
                    .body(new RecipeResponse(false, null, validation.message()));
            }
            return null;
        } catch (GeminiClientException e) {
            log.warn("Ingredient validation Gemini error: status={}", e.getStatusCode());
            if (e.isUnauthorizedOrForbidden()) {
                return ResponseEntity.badRequest()
                    .body(new RecipeResponse(false, null, "Invalid Gemini API key. Check your key in .env or the API Key field."));
            }
            if (e.isRateLimitedOrQuotaExceeded()) {
                return ResponseEntity.status(429)
                    .body(new RecipeResponse(false, null, "Gemini rate limit or quota exceeded (HTTP 429). Please check your Google AI Studio quota, or enter a valid API key in the API Key box."));
            }
            if (e.isServiceUnavailable()) {
                return ResponseEntity.status(503)
                    .body(new RecipeResponse(false, null, "Gemini service is temporarily experiencing high demand. Please try again in a few moments."));
            }
            return ResponseEntity.badRequest()
                .body(new RecipeResponse(false, null, "Could not validate ingredients. Please try again."));
        }
    }
}
