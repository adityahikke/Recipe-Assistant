/* ============================================================
   CulinaryAI Smart Chef Studio — App Logic & Studio Controller
   ============================================================ */

// Splash screen dismiss (2.0s Culinary Entrance Animation)
(function() {
    const splash = document.getElementById('splashScreen');
    if (!splash) return;
    const statusText = document.getElementById('splashStatusText');
    setTimeout(() => {
        if (statusText) statusText.textContent = 'Curating gourmet recipes & kitchen studio…';
    }, 850);
    setTimeout(() => {
        if (statusText) statusText.textContent = 'Welcome to CulinaryAI';
    }, 1550);
    // Dismiss at exactly 2.0s
    setTimeout(() => {
        splash.classList.add('splash-hidden');
        document.body.classList.add('app-loaded');
        setTimeout(() => { splash.remove(); }, 650);
    }, 2000);
})();

document.addEventListener('DOMContentLoaded', function () {
    const R = window.RecipeRenderer;
    const STORAGE_LAST_RECIPE = 'ra_last_recipe';
    const STORAGE_FAVORITES = 'ra_favorites_v1';
    const STORAGE_MIGRATED = 'ra_library_migrated_v1';
    const COLLECTION_ICONS = ['fa-utensils', 'fa-heart', 'fa-leaf', 'fa-fire', 'fa-star', 'fa-book'];
    const CHECKING_MESSAGE = 'Inspecting fresh ingredients…';
    
    const LOADING_MESSAGES = [
        'Inspecting fresh ingredients…',
        'Crafting flavor balance & spices…',
        'Drafting step-by-step instructions…',
        'Calculating macros & plating…'
    ];
    
    const TIPS_LOADING_MESSAGES = [
        'Gathering culinary wisdom…',
        'Drafting flavor pairings…',
        'Finishing chef hacks…'
    ];

    const SURPRISE_COMBOS = [
        'chicken breast, garlic, olive oil, lemon, rosemary, potatoes',
        'salmon fillet, asparagus, soy sauce, ginger, honey, sesame seeds',
        'fettuccine pasta, heavy cream, garlic, parmesan cheese, mushrooms',
        'firm tofu, broccoli, bell pepper, garlic, soy sauce, chili flakes',
        'eggs, spinach, feta cheese, cherry tomatoes, olive oil, black pepper',
        'ground beef, taco seasoning, black beans, cheddar cheese, avocado, onion',
        'shrimp, garlic, butter, white wine, parsley, linguine pasta',
        'chickpeas, cucumber, cherry tomatoes, red onion, kalamata olives, olive oil'
    ];

    const els = {
        apiKeyInput: document.getElementById('apiKey'),
        setApiKeyBtn: document.getElementById('setApiKeyBtn'),
        clearApiKeyBtn: document.getElementById('clearApiKeyBtn'),
        apiKeyStatus: document.getElementById('apiKeyStatus'),
        apiKeyError: document.getElementById('apiKeyError'),
        apiKeyErrorMessage: document.getElementById('apiKeyErrorMessage'),
        apiKeyPanel: document.getElementById('apiKeyPanel'),
        apiKeyPanelTitle: document.getElementById('apiKeyPanelTitle'),
        apiKeyPanelSubtitle: document.getElementById('apiKeyPanelSubtitle'),
        apiKeySummaryStatus: document.getElementById('apiKeySummaryStatus'),
        trialExhaustedInline: document.getElementById('trialExhaustedInline'),
        apiKeyNoTrialInfo: document.getElementById('apiKeyNoTrialInfo'),
        headerTrialBadge: document.getElementById('headerTrialBadge'),
        headerTrialText: document.getElementById('headerTrialText'),
        headerTrialProgress: document.getElementById('headerTrialProgress'),
        trialHowBtn: document.getElementById('trialHowBtn'),
        trialInfoModal: document.getElementById('trialInfoModal'),
        closeTrialInfoBtn: document.getElementById('closeTrialInfoBtn'),
        preferencesPanel: document.getElementById('preferencesPanel'),
        generationBlockedNotice: document.getElementById('generationBlockedNotice'),
        generationBlockedText: document.getElementById('generationBlockedText'),
        openApiKeyFromNotice: document.getElementById('openApiKeyFromNotice'),
        form: document.getElementById('recipeForm'),
        generateBtn: document.getElementById('generateBtn'),
        tipsBtn: document.getElementById('tipsBtn'),
        cancelBtn: document.getElementById('cancelGenerateBtn'),
        results: document.getElementById('results'),
        resultsLoading: document.getElementById('resultsLoading'),
        resultsLoadingText: document.getElementById('resultsLoadingText'),
        resultsError: document.getElementById('resultsError'),
        resultsErrorMessage: document.getElementById('resultsErrorMessage'),
        retryBtn: document.getElementById('retryBtn'),
        resultsPanel: document.getElementById('resultsPanel'),
        emptyShowcase: document.getElementById('emptyShowcase'),
        formError: document.getElementById('formError'),
        formErrorMessage: document.getElementById('formErrorMessage'),
        libraryModal: document.getElementById('libraryModal'),
        confirmModal: document.getElementById('confirmModal'),
        confirmModalMessage: document.getElementById('confirmModalMessage'),
        confirmModalOk: document.getElementById('confirmModalOk'),
        confirmModalCancel: document.getElementById('confirmModalCancel'),
        toggleApiKeyVisibility: document.getElementById('toggleApiKeyVisibility'),
        ingredientChips: document.getElementById('ingredientChips'),
        surprisePantryBtn: document.getElementById('surprisePantryBtn')
    };

    let apiKeyStatusData = null;
    let currentRecipe = null;
    let currentRecipeId = null;
    let currentContentHash = null;
    let currentInSaved = false;
    let libraryCollections = [];
    let libraryChapterCards = [];
    let activeCollectionId = null;
    let activeCollectionTitle = '';
    let activeCollectionSlug = '';
    let confirmResolve = null;
    let lastFocusedElement = null;
    let activeAbort = null;
    let lastFailedAction = null;
    let loadingMessageTimer = null;
    let recipeMoreMenuListenerBound = false;

    // Initialization
    checkApiKeyStatus();
    refreshLibraryCount();
    migrateLocalLibraryIfNeeded();
    restoreLastRecipe();
    initStudioInteractions();

    function getCsrfToken() {
        const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]*)/);
        return match ? decodeURIComponent(match[1]) : '';
    }

    function readLocalFavoritesMap() {
        try {
            return JSON.parse(localStorage.getItem(STORAGE_FAVORITES) || '{}');
        } catch {
            return {};
        }
    }

    async function apiRequest(method, endpoint, data, options = {}) {
        const headers = { 'Content-Type': 'application/json' };
        const csrf = getCsrfToken();
        if (csrf) headers['X-XSRF-TOKEN'] = csrf;
        const fetchOptions = { method, headers, credentials: 'same-origin', signal: options.signal };
        if (data !== undefined) fetchOptions.body = JSON.stringify(data);
        const response = await fetch(endpoint, fetchOptions);
        let result = {};
        try {
            result = await response.json();
        } catch {
            if (!response.ok) {
                throw new Error(response.status === 429
                    ? 'Too many requests. Please wait a minute.'
                    : 'Something went wrong (' + response.status + ').');
            }
        }
        if (!response.ok || result.success === false) {
            throw new Error(result.error || result.message || 'Request failed');
        }
        return result;
    }

    async function refreshLibraryCount() {
        try {
            const result = await apiRequest('GET', '/library/collections');
            libraryCollections = result.collections || [];
            document.getElementById('libraryCount').textContent = result.totalRecipes ?? 0;
        } catch {
            document.getElementById('libraryCount').textContent = '0';
        }
    }

    async function migrateLocalLibraryIfNeeded() {
        if (localStorage.getItem(STORAGE_MIGRATED)) return;
        const map = readLocalFavoritesMap();
        const favorites = Object.values(map).filter(f => f.structuredRecipe);
        if (!favorites.length) {
            localStorage.setItem(STORAGE_MIGRATED, '1');
            return;
        }
        try {
            const result = await apiRequest('POST', '/library/migrate-local', { favorites });
            localStorage.setItem(STORAGE_MIGRATED, '1');
            localStorage.removeItem(STORAGE_FAVORITES);
            if (result.content) showNotification(result.content, 'success');
            await refreshLibraryCount();
        } catch {
            /* retry next visit */
        }
    }

    function syncPreferencesPanel() {
        const hasPrefs = document.getElementById('cuisine').value
            || document.getElementById('dietaryRestrictions').value.trim();
        if (hasPrefs && els.preferencesPanel) els.preferencesPanel.open = true;
    }

    function showResultsPanel() {
        if (els.emptyShowcase) els.emptyShowcase.classList.add('hidden');
        els.resultsPanel.classList.remove('hidden');
    }

    function hideResultsPanel() {
        els.resultsPanel.classList.add('hidden');
        if (els.emptyShowcase) els.emptyShowcase.classList.remove('hidden');
        hideResultsError();
        els.cancelBtn.classList.add('hidden');
        els.resultsLoading.classList.add('hidden');
        els.resultsPanel.setAttribute('aria-busy', 'false');
    }

    function closeRecipeMoreMenu() {
        const menu = document.getElementById('recipeMoreMenu');
        const btn = document.getElementById('recipeMoreBtn');
        if (!menu || !btn) return;
        menu.classList.add('hidden');
        btn.setAttribute('aria-expanded', 'false');
    }

    function toggleRecipeMoreMenu() {
        const menu = document.getElementById('recipeMoreMenu');
        const btn = document.getElementById('recipeMoreBtn');
        if (!menu || !btn) return;
        const isHidden = menu.classList.toggle('hidden');
        btn.setAttribute('aria-expanded', isHidden ? 'false' : 'true');
    }

    function bindRecipeMoreMenuDismiss() {
        if (recipeMoreMenuListenerBound) return;
        recipeMoreMenuListenerBound = true;
        document.addEventListener('click', e => {
            if (!e.target.closest('.recipe-actions-more')) closeRecipeMoreMenu();
        });
        document.addEventListener('keydown', e => {
            if (e.key === 'Escape') closeRecipeMoreMenu();
        });
    }

    function persistLastRecipe() {
        if (!currentRecipe) return;
        try {
            sessionStorage.setItem(STORAGE_LAST_RECIPE, JSON.stringify({
                recipe: currentRecipe,
                ingredients: document.getElementById('ingredients').value,
                cuisine: document.getElementById('cuisine').value,
                dietaryRestrictions: document.getElementById('dietaryRestrictions').value,
                contentHash: currentContentHash,
                recipeId: currentRecipeId,
                inSaved: currentInSaved
            }));
        } catch { /* storage full */ }
    }

    function restoreLastRecipe() {
        try {
            const raw = sessionStorage.getItem(STORAGE_LAST_RECIPE);
            if (!raw) return;
            const saved = JSON.parse(raw);
            if (!saved.recipe) return;
            document.getElementById('ingredients').value = saved.ingredients || '';
            document.getElementById('cuisine').value = saved.cuisine || '';
            document.getElementById('dietaryRestrictions').value = saved.dietaryRestrictions || '';
            currentRecipe = saved.recipe;
            currentContentHash = saved.contentHash || null;
            currentRecipeId = saved.recipeId || null;
            currentInSaved = !!saved.inSaved;
            syncPreferencesPanel();
            els.results.replaceChildren();
            els.results.appendChild(R.renderStructuredRecipe(saved.recipe));
            appendActionButtons();
            showFavoriteButton(currentInSaved);
            showResultsPanel();
        } catch { /* ignore */ }
    }

    function openApiKeyPanel() {
        els.apiKeyPanel.classList.remove('hidden');
        els.apiKeyPanel.open = true;
        els.apiKeyInput.focus();
    }

    function checkApiKeyStatus() {
        fetch('/api-key-status', { credentials: 'same-origin' })
            .then(r => r.json())
            .then(updateApiKeyUi)
            .catch(() => showApiKeyInput());
    }

    function updateTrialDisplay(data) {
        const max = data.defaultRecipesMax ?? 5;
        const remaining = data.defaultTrialsRemaining ?? 0;
        const onServerTrial = data.defaultKeyAvailable && !data.hasUserKey;
        const trialActive = onServerTrial && remaining > 0;

        els.headerTrialBadge.classList.toggle('hidden', !onServerTrial || data.hasUserKey);

        if (onServerTrial && !data.hasUserKey) {
            els.headerTrialText.textContent = remaining + ' of ' + max + ' free left';
            els.headerTrialProgress.style.width = (max > 0 ? Math.min(100, (remaining / max) * 100) : 0) + '%';
        }

        els.apiKeyPanel.classList.toggle('hidden', trialActive);

        if (data.hasUserKey) {
            els.apiKeyPanel.classList.remove('hidden');
            els.apiKeyPanelTitle.textContent = 'Custom Gemini Engine';
            els.apiKeyPanelSubtitle.textContent = ' (Unlimited)';
            els.apiKeySummaryStatus.textContent = 'Active with your custom API key.';
            els.trialExhaustedInline.classList.add('hidden');
            return;
        }

        if (data.defaultKeyAvailable) {
            if (remaining === 0) {
                els.apiKeyPanel.classList.remove('hidden');
                els.apiKeyPanel.open = true;
                els.apiKeyPanelTitle.textContent = 'API Engine';
                els.apiKeyPanelSubtitle.textContent = ' (Trial Completed)';
                els.apiKeySummaryStatus.textContent = 'Add your key to continue crafting recipes.';
                els.trialExhaustedInline.classList.remove('hidden');
            }
            els.apiKeyNoTrialInfo.classList.add('hidden');
        } else {
            els.apiKeyPanel.classList.remove('hidden');
            els.apiKeyPanel.open = true;
            els.apiKeyPanelTitle.textContent = 'API Engine';
            els.apiKeyPanelSubtitle.textContent = ' (Required)';
            els.apiKeySummaryStatus.textContent = 'No server key configured — enter yours below.';
            els.apiKeyNoTrialInfo.classList.remove('hidden');
            els.trialExhaustedInline.classList.add('hidden');
        }
    }

    function canGenerate(data) {
        if (!data) return false;
        if (data.hasUserKey) return true;
        return data.defaultKeyAvailable && (data.defaultTrialsRemaining ?? 0) > 0;
    }

    function updateGenerationControls(data) {
        apiKeyStatusData = data;
        if (activeAbort) return;
        const allowed = canGenerate(data);
        els.generateBtn.disabled = !allowed;
        els.tipsBtn.disabled = !allowed;
        if (allowed) {
            els.generationBlockedNotice.classList.add('hidden');
            return;
        }
        els.generationBlockedNotice.classList.remove('hidden');
        els.generationBlockedText.textContent = !data.defaultKeyAvailable && !data.hasUserKey
            ? 'Add a Gemini API key to craft recipes.'
            : 'Free trial completed on this device.';
    }

    function updateApiKeyUi(data) {
        els.apiKeyError.classList.add('hidden');
        updateTrialDisplay(data);
        updateGenerationControls(data);
        if (data.hasUserKey) showApiKeyStatus();
        else showApiKeyInput();
    }

    function showApiKeyStatus() {
        els.apiKeyInput.classList.add('hidden');
        els.apiKeyStatus.classList.remove('hidden');
        els.apiKeyNoTrialInfo.classList.add('hidden');
    }

    function showApiKeyInput() {
        els.apiKeyInput.classList.remove('hidden');
        els.apiKeyStatus.classList.add('hidden');
    }

    function showApiKeyError(message) {
        els.apiKeyErrorMessage.textContent = message;
        els.apiKeyError.classList.remove('hidden');
    }

    function startLoadingMessages(initialMessage, followUpMessages) {
        stopLoadingMessages();
        const messages = followUpMessages || LOADING_MESSAGES;
        let index = 0;
        let phase = 'checking';
        els.resultsLoadingText.textContent = CHECKING_MESSAGE;
        loadingMessageTimer = setInterval(() => {
            if (phase === 'checking') {
                phase = 'generating';
                els.resultsLoadingText.textContent = initialMessage;
                return;
            }
            index = (index + 1) % messages.length;
            els.resultsLoadingText.textContent = messages[index];
        }, 3200);
    }

    function stopLoadingMessages() {
        if (loadingMessageTimer) {
            clearInterval(loadingMessageTimer);
            loadingMessageTimer = null;
        }
    }

    function showLoading(message, followUpMessages) {
        showResultsPanel();
        hideResultsError();
        hideFormError();
        startLoadingMessages(message, followUpMessages);
        els.resultsLoading.classList.remove('hidden');
        els.results.classList.add('hidden');
        els.cancelBtn.classList.remove('hidden');
        els.resultsPanel.setAttribute('aria-busy', 'true');
        els.generateBtn.disabled = true;
        els.tipsBtn.disabled = true;
    }

    function hideLoading() {
        stopLoadingMessages();
        els.resultsLoading.classList.add('hidden');
        els.results.classList.remove('hidden');
        els.cancelBtn.classList.add('hidden');
        els.resultsPanel.setAttribute('aria-busy', 'false');
        activeAbort = null;
        if (apiKeyStatusData) updateGenerationControls(apiKeyStatusData);
        const hasContent = els.results.childElementCount > 0;
        const hasError = !els.resultsError.classList.contains('hidden');
        if (!hasContent && !hasError) hideResultsPanel();
    }

    function showFormValidationError(message) {
        els.formErrorMessage.textContent = message;
        els.formError.classList.remove('hidden');
    }

    function hideFormError() {
        els.formError.classList.add('hidden');
    }

    function isIngredientValidationError(message) {
        if (!message) return false;
        const lower = message.toLowerCase();
        return lower.includes('food ingredient') || lower.includes('pantry') || lower.includes("doesn't look like");
    }

    function showResultsError(message, showRetry) {
        showResultsPanel();
        els.resultsErrorMessage.textContent = message;
        els.resultsError.classList.remove('hidden');
        els.retryBtn.classList.toggle('hidden', !showRetry);
    }

    function hideResultsError() {
        els.resultsError.classList.add('hidden');
        els.resultsErrorMessage.textContent = '';
        els.retryBtn.classList.add('hidden');
        lastFailedAction = null;
    }

    function showRecipeResult(recipe, result) {
        currentRecipe = recipe;
        currentContentHash = result?.contentHash || currentContentHash;
        currentRecipeId = result?.recipeId || currentRecipeId;
        currentInSaved = !!result?.favorited;
        hideResultsError();
        els.results.replaceChildren();
        els.results.appendChild(R.renderStructuredRecipe(recipe));
        appendActionButtons();
        showFavoriteButton(currentInSaved);
        showResultsPanel();
        persistLastRecipe();
        if (result) {
            checkApiKeyStatus();
            refreshLibraryCount();
        }
    }

    function showTipsResult(tipsResult) {
        currentRecipe = null;
        currentRecipeId = null;
        currentContentHash = null;
        currentInSaved = false;
        hideResultsError();
        closeRecipeMoreMenu();
        els.results.replaceChildren();
        els.results.appendChild(R.renderTipsBanner());
        els.results.appendChild(R.renderCookingTips(tipsResult));
        sessionStorage.removeItem(STORAGE_LAST_RECIPE);
        showResultsPanel();
        checkApiKeyStatus();
    }

    function appendActionButtons() {
        bindRecipeMoreMenuDismiss();
        const shareAvailable = typeof navigator.share === 'function';
        const actions = document.createElement('div');
        actions.className = 'recipe-actions-bar';
        actions.innerHTML = `
            <button type="button" id="favoriteBtn" class="recipe-action-primary">
                <i class="fas fa-heart" aria-hidden="true"></i><span>Save to Cookbook</span>
            </button>
            <button type="button" id="copyBtn" class="recipe-action-secondary" title="Copy clean recipe">
                <i class="fas fa-copy" aria-hidden="true"></i><span>Copy</span>
            </button>
            ${shareAvailable ? `<button type="button" id="shareBtn" class="recipe-action-secondary" title="Share recipe">
                <i class="fas fa-share-nodes" aria-hidden="true"></i><span>Share</span>
            </button>` : ''}
            <button type="button" id="regenerateBtn" class="recipe-action-secondary" title="Craft a variation with the same ingredients">
                <i class="fas fa-rotate-right" aria-hidden="true"></i><span>Try Another Twist</span>
            </button>
            <div class="recipe-actions-more">
                <button type="button" id="recipeMoreBtn" class="recipe-action-menu-btn" aria-expanded="false" aria-haspopup="true">
                    <i class="fas fa-ellipsis-vertical" aria-hidden="true"></i><span>More</span>
                </button>
                <div id="recipeMoreMenu" class="recipe-actions-menu hidden" role="menu">
                    <button type="button" id="printBtn" role="menuitem">
                        <i class="fas fa-print" aria-hidden="true"></i><span>Print Recipe</span>
                    </button>
                    <button type="button" id="addToCollectionBtn" role="menuitem">
                        <i class="fas fa-folder-plus" aria-hidden="true"></i><span>Add to Collection</span>
                    </button>
                </div>
            </div>`;
        els.results.appendChild(actions);

        document.getElementById('favoriteBtn').addEventListener('click', toggleSaved);
        document.getElementById('copyBtn').addEventListener('click', copyToClipboard);
        if (shareAvailable) {
            document.getElementById('shareBtn').addEventListener('click', shareRecipe);
        }
        document.getElementById('regenerateBtn').addEventListener('click', () => els.form.requestSubmit());
        document.getElementById('recipeMoreBtn').addEventListener('click', e => {
            e.stopPropagation();
            toggleRecipeMoreMenu();
        });
        document.getElementById('printBtn').addEventListener('click', () => { closeRecipeMoreMenu(); printRecipe(); });
        document.getElementById('addToCollectionBtn').addEventListener('click', () => { closeRecipeMoreMenu(); openAddToCollectionModal(); });

        // Also wire a direct print button if it exists in the actions bar
        const directPrintBtn = document.getElementById('directPrintBtn');
        if (directPrintBtn) directPrintBtn.addEventListener('click', printRecipe);
    }

    function printRecipe() {
        if (!currentRecipe) return showNotification('Generate a recipe first.', 'error');
        const w = window.open('', '_blank');
        if (!w) return showNotification('Allow popups to print.', 'error');
        w.document.write(R.renderPrintDocument(currentRecipe));
        w.document.close();
    }

    function copyToClipboard() {
        const text = currentRecipe ? R.recipeToPlainText(currentRecipe) : '';
        if (!text) return showNotification('Nothing to copy.', 'error');
        navigator.clipboard.writeText(text)
            .then(() => showNotification('Recipe copied to clipboard!', 'success'))
            .catch(() => showNotification('Could not copy to clipboard.', 'error'));
    }

    async function shareRecipe() {
        if (!currentRecipe || typeof navigator.share !== 'function') {
            return copyToClipboard();
        }
        const text = R.recipeToPlainText(currentRecipe);
        try {
            await navigator.share({
                title: currentRecipe.name || 'AI Recipe',
                text
            });
        } catch (e) {
            if (e.name !== 'AbortError') showNotification('Could not share.', 'error');
        }
    }

    async function makeRequest(endpoint, data, options = {}) {
        return apiRequest('POST', endpoint, data, options);
    }

    async function runGeneration(endpoint, data, loadingMessage, failedAction, followUpMessages) {
        const controller = new AbortController();
        activeAbort = () => controller.abort();
        lastFailedAction = failedAction;
        showLoading(loadingMessage, followUpMessages);
        try {
            return await makeRequest(endpoint, data, { signal: controller.signal });
        } finally {
            hideLoading();
        }
    }

    function formPayload() {
        return {
            ingredients: document.getElementById('ingredients').value.trim(),
            cuisine: document.getElementById('cuisine').value,
            dietaryRestrictions: document.getElementById('dietaryRestrictions').value,
            onlyListedIngredients: document.getElementById('onlyListedIngredients').checked
        };
    }

    function addIngredientChip(name) {
        const field = document.getElementById('ingredients');
        const parts = field.value.split(',').map(s => s.trim()).filter(Boolean);
        if (!parts.some(p => p.toLowerCase() === name.toLowerCase())) {
            parts.push(name);
            field.value = parts.join(', ');
        }
        field.focus();
    }

    function initStudioInteractions() {
        // Quick add chips
        els.ingredientChips.addEventListener('click', e => {
            const chip = e.target.closest('.ingredient-chip');
            if (chip) addIngredientChip(chip.dataset.ingredient);
        });

        // Surprise Me button
        if (els.surprisePantryBtn) {
            els.surprisePantryBtn.addEventListener('click', () => {
                const randomCombo = SURPRISE_COMBOS[Math.floor(Math.random() * SURPRISE_COMBOS.length)];
                document.getElementById('ingredients').value = randomCombo;
                document.getElementById('ingredients').focus();
                showNotification('✨ Loaded chef-curated ingredient combo!', 'info');
            });
        }

        // Quick Starter Cards in Empty State
        document.querySelectorAll('.starter-card').forEach(card => {
            card.addEventListener('click', () => {
                const ings = card.dataset.ingredients;
                const cuisine = card.dataset.cuisine;
                if (ings) document.getElementById('ingredients').value = ings;
                if (cuisine) document.getElementById('cuisine').value = cuisine;
                syncPreferencesPanel();
                els.form.requestSubmit();
            });
        });

        // Dietary Quick Pills
        document.querySelectorAll('.diet-pill').forEach(pill => {
            pill.addEventListener('click', () => {
                const dietInput = document.getElementById('dietaryRestrictions');
                const dietName = pill.dataset.diet;
                const existing = dietInput.value.split(',').map(s => s.trim()).filter(Boolean);
                const idx = existing.indexOf(dietName);
                if (idx > -1) {
                    existing.splice(idx, 1);
                    pill.classList.remove('active');
                } else {
                    existing.push(dietName);
                    pill.classList.add('active');
                }
                dietInput.value = existing.join(', ');
            });
        });
    }

    // Trial Modal Trigger
    els.trialHowBtn.addEventListener('click', () => {
        lastFocusedElement = document.activeElement;
        els.trialInfoModal.classList.remove('hidden');
        els.closeTrialInfoBtn.focus();
    });

    function closeTrialInfoModal() {
        els.trialInfoModal.classList.add('hidden');
        if (lastFocusedElement) lastFocusedElement.focus();
    }

    els.closeTrialInfoBtn.addEventListener('click', closeTrialInfoModal);
    els.trialInfoModal.addEventListener('click', e => {
        if (e.target === els.trialInfoModal) closeTrialInfoModal();
    });

    // API Key Save / Clear
    els.setApiKeyBtn.addEventListener('click', async () => {
        const apiKey = els.apiKeyInput.value.trim();
        if (!apiKey) return showApiKeyError('Please enter your Gemini API key.');
        els.setApiKeyBtn.disabled = true;
        try {
            await makeRequest('/set-api-key', { apiKey });
            checkApiKeyStatus();
            els.apiKeyInput.value = '';
            showNotification('Gemini API key saved & encrypted.', 'success');
        } catch (e) {
            showApiKeyError(e.message);
        } finally {
            els.setApiKeyBtn.disabled = false;
        }
    });

    els.clearApiKeyBtn.addEventListener('click', async () => {
        try {
            await makeRequest('/clear-api-key', {});
            checkApiKeyStatus();
            showNotification('Custom API key removed.', 'info');
        } catch (e) {
            showApiKeyError(e.message);
        }
    });

    els.openApiKeyFromNotice.addEventListener('click', openApiKeyPanel);

    els.toggleApiKeyVisibility.addEventListener('click', () => {
        const show = els.apiKeyInput.type === 'password';
        els.apiKeyInput.type = show ? 'text' : 'password';
        els.toggleApiKeyVisibility.querySelector('i').className = show ? 'fas fa-eye-slash' : 'fas fa-eye';
    });

    els.cancelBtn.addEventListener('click', async () => {
        try {
            await apiRequest('POST', '/cancel-generation', {});
        } catch (_) { /* ignore */ }
        if (activeAbort) activeAbort();
        hideLoading();
        showNotification('Recipe generation cancelled.', 'info');
    });

    els.retryBtn.addEventListener('click', () => {
        if (lastFailedAction === 'tips') els.tipsBtn.click();
        else if (lastFailedAction === 'generate') els.form.requestSubmit();
    });

    els.form.addEventListener('submit', async e => {
        e.preventDefault();
        const data = formPayload();
        if (!data.ingredients) return showFormValidationError('Please enter at least one ingredient to start.');
        hideFormError();
        try {
            const result = await runGeneration('/generate-recipe', data, 'Crafting your gourmet recipe…', 'generate', LOADING_MESSAGES);
            showRecipeResult(result.recipe, result);
        } catch (err) {
            if (err.name === 'AbortError') return;
            if (isIngredientValidationError(err.message)) {
                showFormValidationError(err.message);
                return;
            }
            showResultsError(err.message, true);
        }
    });

    els.tipsBtn.addEventListener('click', async () => {
        const data = formPayload();
        if (!data.ingredients) return showFormValidationError('Enter ingredients to get cooking tips.');
        hideFormError();
        try {
            const result = await runGeneration('/get-cooking-tips', data, 'Gathering culinary tips…', 'tips', TIPS_LOADING_MESSAGES);
            showTipsResult(result.cookingTips);
        } catch (err) {
            if (err.name === 'AbortError') return;
            if (isIngredientValidationError(err.message)) {
                showFormValidationError(err.message);
                return;
            }
            showResultsError(err.message, true);
        }
    });

    document.getElementById('clearRecipeBtn').addEventListener('click', async () => {
        if (!(await showConfirm('Clear your pantry and reset the studio canvas?'))) return;
        els.results.replaceChildren();
        document.getElementById('ingredients').value = '';
        document.getElementById('cuisine').value = '';
        document.getElementById('dietaryRestrictions').value = '';
        document.getElementById('onlyListedIngredients').checked = false;
        document.querySelectorAll('.diet-pill').forEach(p => p.classList.remove('active'));
        if (els.preferencesPanel) els.preferencesPanel.open = false;
        hideFormError();
        hideResultsPanel();
        sessionStorage.removeItem(STORAGE_LAST_RECIPE);
        currentRecipe = null;
        currentRecipeId = null;
        currentContentHash = null;
        currentInSaved = false;
        showNotification('Studio canvas reset.', 'info');
    });

    async function toggleSaved() {
        if (!currentRecipeId) return showNotification('Generate a recipe first.', 'error');
        try {
            const result = await makeRequest('/library/saved/toggle', { recipeId: currentRecipeId });
            currentInSaved = !!result.favorited;
            showFavoriteButton(currentInSaved);
            showNotification(result.content, currentInSaved ? 'success' : 'info');
            await refreshLibraryCount();
        } catch (e) {
            showNotification(e.message, 'error');
        }
    }

    function showFavoriteButton(isSaved) {
        const btn = document.getElementById('favoriteBtn');
        if (!btn) return;
        btn.classList.toggle('is-saved', isSaved);
        btn.innerHTML = isSaved
            ? '<i class="fas fa-heart-crack" aria-hidden="true"></i><span>Remove from Saved</span>'
            : '<i class="fas fa-heart" aria-hidden="true"></i><span>Save to Cookbook</span>';
    }

    async function openAddToCollectionModal() {
        if (!currentRecipeId) return showNotification('Generate a recipe first.', 'error');
        try {
            const result = await apiRequest('GET', '/library/collections');
            const list = document.getElementById('addToCollectionList');
            list.replaceChildren();
            (result.collections || []).filter(c => c.slug !== 'saved').forEach(col => {
                const btn = document.createElement('button');
                btn.type = 'button';
                btn.className = 'collection-select-btn';
                btn.innerHTML = `
                    <span><i class="fas fa-folder" style="color:var(--c-primary);margin-right:0.5rem;"></i>${R.escapeHtml(col.title)}</span>
                    <span style="font-size:0.75rem;color:var(--c-text-dim);">${col.recipeCount}</span>
                `;
                btn.addEventListener('click', async () => {
                    try {
                        await apiRequest('POST', '/library/collections/' + col.id + '/recipes', { recipeId: currentRecipeId });
                        showNotification('Added to ' + col.title, 'success');
                        document.getElementById('addToCollectionModal').classList.add('hidden');
                        await refreshLibraryCount();
                    } catch (e) {
                        showNotification(e.message, 'error');
                    }
                });
                list.appendChild(btn);
            });
            document.getElementById('addToCollectionModal').classList.remove('hidden');
        } catch (e) {
            showNotification(e.message, 'error');
        }
    }

    document.getElementById('closeAddToCollectionBtn').addEventListener('click', () => {
        document.getElementById('addToCollectionModal').classList.add('hidden');
    });

    function collectionIcon(slug, index) {
        if (slug === 'saved') return 'fa-heart';
        if (slug === 'my-recipes') return 'fa-utensils';
        return COLLECTION_ICONS[index % COLLECTION_ICONS.length];
    }

    function renderLibraryShelf() {
        const grid = document.getElementById('libraryShelfGrid');
        const empty = document.getElementById('emptyLibraryShelf');
        grid.replaceChildren();
        if (!libraryCollections.length) {
            empty.classList.remove('hidden');
            return;
        }
        empty.classList.add('hidden');
        libraryCollections.forEach((col, index) => {
            const wrap = document.createElement('div');
            wrap.className = 'relative';
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'library-collection-card';
            btn.innerHTML = `
                <div class="card-icon"><i class="fas ${collectionIcon(col.slug, index)}" aria-hidden="true"></i></div>
                <div class="card-title">${R.escapeHtml(col.title)}</div>
                <div class="card-count">${collectionSubtitle(col)} &middot; ${col.recipeCount} recipe${col.recipeCount === 1 ? '' : 's'}</div>
            `;
            btn.addEventListener('click', () => openLibraryChapter(col));
            wrap.appendChild(btn);

            if (!col.systemDefault) {
                const del = document.createElement('button');
                del.type = 'button';
                del.className = 'modal-close-btn';
                del.style.position = 'absolute';
                del.style.top = '0.5rem';
                del.style.right = '0.5rem';
                del.style.width = '1.75rem';
                del.style.height = '1.75rem';
                del.title = 'Delete collection';
                del.innerHTML = '<i class="fas fa-trash" style="font-size:0.75rem;color:var(--c-rose-light);" aria-hidden="true"></i>';
                del.addEventListener('click', async ev => {
                    ev.stopPropagation();
                    if (!(await showConfirm('Delete collection “' + col.title + '”? Recipes remain in My Recipes.'))) return;
                    try {
                        await apiRequest('DELETE', '/library/collections/' + col.id);
                        await refreshLibraryCount();
                        renderLibraryShelf();
                        showNotification('Collection deleted.', 'info');
                    } catch (e) {
                        showNotification(e.message, 'error');
                    }
                });
                wrap.appendChild(del);
            }
            grid.appendChild(wrap);
        });
    }

    function collectionSubtitle(col) {
        if (col.slug === 'my-recipes') return 'Everything generated';
        if (col.slug === 'saved') return 'Starred picks';
        return col.description || 'Custom collection';
    }

    async function openLibraryChapter(col) {
        try {
            const result = await apiRequest('GET', '/library/collections/' + col.id);
            activeCollectionId = col.id;
            activeCollectionTitle = col.title;
            activeCollectionSlug = col.slug;
            libraryChapterCards = result.recipeCards || [];
            showLibraryChapterView();
        } catch (e) {
            showNotification(e.message, 'error');
        }
    }

    function showLibraryShelfView() {
        document.getElementById('libraryShelfView').classList.remove('hidden');
        document.getElementById('libraryChapterView').classList.add('hidden');
        document.getElementById('libraryBackBtn').classList.add('hidden');
        document.getElementById('libraryTitleText').textContent = 'My Cookbook';
        activeCollectionId = null;
        activeCollectionSlug = '';
    }

    function showLibraryChapterView() {
        document.getElementById('libraryShelfView').classList.add('hidden');
        document.getElementById('libraryChapterView').classList.remove('hidden');
        document.getElementById('libraryBackBtn').classList.remove('hidden');
        document.getElementById('libraryTitleText').textContent = activeCollectionTitle;
        const list = document.getElementById('libraryChapterList');
        const empty = document.getElementById('emptyLibraryChapter');
        list.replaceChildren();

        if (!libraryChapterCards.length) {
            const title = document.getElementById('emptyLibraryChapterTitle');
            const hint = document.getElementById('emptyLibraryChapterHint');
            if (activeCollectionSlug === 'saved') {
                title.textContent = 'No saved recipes yet';
                hint.textContent = 'Star any recipe on the canvas to save it here.';
            } else if (activeCollectionSlug === 'my-recipes') {
                title.textContent = 'No recipes yet';
                hint.textContent = 'Generate your first recipe to automatically save it here.';
            } else {
                title.textContent = 'No recipes in this collection';
                hint.textContent = 'Use "Add to collection" on a recipe to add it.';
            }
            empty.classList.remove('hidden');
            return;
        }

        empty.classList.add('hidden');
        const removeLabel = activeCollectionSlug === 'my-recipes' ? 'Delete' : 'Remove';
        
        libraryChapterCards.forEach(card => {
            const row = document.createElement('div');
            row.className = 'library-recipe-row';
            const savedTag = card.inSaved ? ' <i class="fas fa-heart" style="color:var(--c-rose-light);font-size:0.75rem;" title="In Saved"></i>' : '';
            row.innerHTML = `
                <div class="flex justify-between items-center gap-2 mb-1">
                    <h3>${R.escapeHtml(card.title || 'Untitled')}${savedTag}</h3>
                    <div class="flex gap-2 shrink-0">
                        <button type="button" class="view-lib btn btn-secondary btn-sm" style="padding:0.25rem 0.625rem;font-size:0.75rem;">
                            <i class="fas fa-eye"></i> Open
                        </button>
                        <button type="button" class="remove-lib btn btn-danger btn-sm" style="padding:0.25rem 0.625rem;font-size:0.75rem;">
                            <i class="fas fa-trash"></i> ${R.escapeHtml(removeLabel)}
                        </button>
                    </div>
                </div>
                <p class="truncate">${R.escapeHtml(card.ingredients || '')}</p>
            `;
            row.querySelector('.view-lib').addEventListener('click', () => loadLibraryRecipe(card.id));
            row.querySelector('.remove-lib').addEventListener('click', () => removeFromChapter(card));
            list.appendChild(row);
        });
    }

    async function loadLibraryRecipe(recipeId) {
        try {
            const result = await apiRequest('GET', '/library/recipes/' + recipeId);
            if (!result.recipe) return showNotification('Could not load recipe.', 'error');
            currentRecipe = result.recipe;
            currentRecipeId = result.recipeId;
            currentContentHash = result.contentHash;
            currentInSaved = !!result.favorited;
            document.getElementById('ingredients').value = result.savedIngredients || '';
            document.getElementById('cuisine').value = result.savedCuisine || '';
            document.getElementById('dietaryRestrictions').value = result.savedDietaryRestrictions || '';
            showRecipeResult(result.recipe, result);
            closeLibraryModal();
        } catch (e) {
            showNotification(e.message, 'error');
        }
    }

    async function removeFromChapter(card) {
        const isMyRecipes = activeCollectionSlug === 'my-recipes';
        const msg = isMyRecipes
            ? 'Delete this recipe permanently from your cookbook?'
            : 'Remove this recipe from ' + activeCollectionTitle + '?';
        if (!(await showConfirm(msg))) return;
        try {
            if (isMyRecipes) {
                await apiRequest('DELETE', '/library/recipes/' + card.id);
                if (currentRecipeId === card.id) {
                    currentRecipeId = null;
                    currentInSaved = false;
                    showFavoriteButton(false);
                }
            } else {
                await apiRequest('DELETE', '/library/collections/' + activeCollectionId + '/recipes/' + card.id);
            }
            await openLibraryChapter({ id: activeCollectionId, title: activeCollectionTitle });
            await refreshLibraryCount();
            showNotification(isMyRecipes ? 'Recipe deleted.' : 'Removed from collection.', 'info');
        } catch (e) {
            showNotification(e.message, 'error');
        }
    }

    function trapFocus(modal, onClose) {
        const focusable = modal.querySelectorAll('button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])');
        const first = focusable[0];
        const last = focusable[focusable.length - 1];
        function onKey(e) {
            if (e.key === 'Escape') {
                onClose();
                return;
            }
            if (e.key !== 'Tab' || focusable.length === 0) return;
            if (e.shiftKey && document.activeElement === first) {
                e.preventDefault();
                last.focus();
            } else if (!e.shiftKey && document.activeElement === last) {
                e.preventDefault();
                first.focus();
            }
        }
        modal.addEventListener('keydown', onKey);
        return () => modal.removeEventListener('keydown', onKey);
    }

    let releaseLibraryTrap = null;

    document.getElementById('libraryBtn').addEventListener('click', async () => {
        await refreshLibraryCount();
        showLibraryShelfView();
        renderLibraryShelf();
        lastFocusedElement = document.activeElement;
        els.libraryModal.classList.remove('hidden');
        releaseLibraryTrap = trapFocus(els.libraryModal, closeLibraryModal);
        document.getElementById('closeLibraryBtn').focus();
    });

    function closeLibraryModal() {
        els.libraryModal.classList.add('hidden');
        showLibraryShelfView();
        if (releaseLibraryTrap) releaseLibraryTrap();
        if (lastFocusedElement) lastFocusedElement.focus();
    }

    document.getElementById('closeLibraryBtn').addEventListener('click', closeLibraryModal);
    document.getElementById('libraryBackBtn').addEventListener('click', () => {
        showLibraryShelfView();
        renderLibraryShelf();
    });
    els.libraryModal.addEventListener('click', e => {
        if (e.target === els.libraryModal) closeLibraryModal();
    });

    document.getElementById('newCollectionForm').addEventListener('submit', async e => {
        e.preventDefault();
        const title = document.getElementById('newCollectionTitle').value.trim();
        if (!title) return;
        try {
            await apiRequest('POST', '/library/collections', { title });
            document.getElementById('newCollectionTitle').value = '';
            await refreshLibraryCount();
            renderLibraryShelf();
            showNotification('Collection created successfully.', 'success');
        } catch (err) {
            showNotification(err.message, 'error');
        }
    });

    function showNotification(message, type) {
        const toast = document.createElement('div');
        const iconClass = type === 'error' ? 'fa-circle-exclamation' : (type === 'info' ? 'fa-circle-info' : 'fa-circle-check');
        const typeClass = type === 'error' ? 'toast-error' : (type === 'info' ? 'toast-info' : 'toast-success');
        
        toast.className = `toast-pill ${typeClass}`;
        toast.innerHTML = `<i class="fas ${iconClass}"></i><span>${R.escapeHtml(message)}</span>`;
        
        document.getElementById('toastRegion').appendChild(toast);
        setTimeout(() => {
            toast.style.opacity = '0';
            toast.style.transform = 'translateY(10px)';
            toast.style.transition = 'all 0.25s ease';
            setTimeout(() => toast.remove(), 250);
        }, 3200);
    }

    function showConfirm(message) {
        return new Promise(resolve => {
            confirmResolve = resolve;
            els.confirmModalMessage.textContent = message;
            els.confirmModal.classList.remove('hidden');
            els.confirmModalOk.focus();
        });
    }

    function closeConfirm(result) {
        els.confirmModal.classList.add('hidden');
        if (confirmResolve) confirmResolve(result);
        confirmResolve = null;
    }

    // ============================================================
    // AI VISION SCANNER SUBSYSTEM
    // ============================================================
    const vEls = {
        openVisionBtn: document.getElementById('openVisionBtn'),
        visionModal: document.getElementById('visionModal'),
        closeVisionBtn: document.getElementById('closeVisionBtn'),
        visionCancelBtn: document.getElementById('visionCancelBtn'),
        visionDropzone: document.getElementById('visionDropzone'),
        visionFileInput: document.getElementById('visionFileInput'),
        visionCameraInput: document.getElementById('visionCameraInput'),
        visionBrowseBtn: document.getElementById('visionBrowseBtn'),
        visionCameraBtn: document.getElementById('visionCameraBtn'),
        visionCameraBox: document.getElementById('visionCameraBox'),
        visionCameraVideo: document.getElementById('visionCameraVideo'),
        visionSnapBtn: document.getElementById('visionSnapBtn'),
        visionCloseCameraBtn: document.getElementById('visionCloseCameraBtn'),
        visionPreviewBox: document.getElementById('visionPreviewBox'),
        visionImagePreview: document.getElementById('visionImagePreview'),
        visionRemoveImageBtn: document.getElementById('visionRemoveImageBtn'),
        scanImageBtn: document.getElementById('scanImageBtn'),
        visionLoading: document.getElementById('visionLoading'),
        visionResultsBox: document.getElementById('visionResultsBox'),
        visionSummaryText: document.getElementById('visionSummaryText'),
        visionDetectedChips: document.getElementById('visionDetectedChips'),
        visionSelectAllBtn: document.getElementById('visionSelectAllBtn'),
        importDetectedBtn: document.getElementById('importDetectedBtn'),
        visionError: document.getElementById('visionError'),
        visionErrorMessage: document.getElementById('visionErrorMessage')
    };

    let selectedImageFile = null;
    let cameraStream = null;
    let detectedIngredientsList = [];

    if (vEls.openVisionBtn && vEls.visionModal) {
        vEls.openVisionBtn.addEventListener('click', openVisionScannerModal);
        vEls.closeVisionBtn.addEventListener('click', closeVisionScannerModal);
        vEls.visionCancelBtn.addEventListener('click', closeVisionScannerModal);
        vEls.visionModal.addEventListener('click', (e) => {
            if (e.target === vEls.visionModal) closeVisionScannerModal();
        });

        // Browse trigger
        if (vEls.visionBrowseBtn) {
            vEls.visionBrowseBtn.addEventListener('click', () => vEls.visionFileInput.click());
        }
        if (vEls.visionDropzone) {
            vEls.visionDropzone.addEventListener('click', (e) => {
                if (e.target !== vEls.visionBrowseBtn && e.target !== vEls.visionCameraBtn && !e.target.closest('button')) {
                    vEls.visionFileInput.click();
                }
            });
        }

        // File Selection (Browse)
        if (vEls.visionFileInput) {
            vEls.visionFileInput.addEventListener('change', (e) => {
                if (e.target.files && e.target.files[0]) {
                    handleVisionImageSelected(e.target.files[0]);
                }
            });
        }

        // Direct Camera Input Selection (Mobile / Native Camera Fallback)
        if (vEls.visionCameraInput) {
            vEls.visionCameraInput.addEventListener('change', (e) => {
                if (e.target.files && e.target.files[0]) {
                    handleVisionImageSelected(e.target.files[0]);
                }
            });
        }

        // Drag and Drop on dropzone
        if (vEls.visionDropzone) {
            ['dragenter', 'dragover'].forEach(name => {
                vEls.visionDropzone.addEventListener(name, (e) => {
                    e.preventDefault();
                    vEls.visionDropzone.classList.add('is-dragover');
                });
            });
            ['dragleave', 'drop'].forEach(name => {
                vEls.visionDropzone.addEventListener(name, (e) => {
                    e.preventDefault();
                    vEls.visionDropzone.classList.remove('is-dragover');
                });
            });
            vEls.visionDropzone.addEventListener('drop', (e) => {
                if (e.dataTransfer.files && e.dataTransfer.files[0]) {
                    handleVisionImageSelected(e.dataTransfer.files[0]);
                }
            });
        }

        // Paste screenshot anywhere
        window.addEventListener('paste', (e) => {
            if (e.clipboardData && e.clipboardData.items) {
                for (const item of e.clipboardData.items) {
                    if (item.type && item.type.startsWith('image/')) {
                        const file = item.getAsFile();
                        if (file) {
                            openVisionScannerModal();
                            handleVisionImageSelected(file);
                            e.preventDefault();
                            break;
                        }
                    }
                }
            }
        });

        // Drag & drop directly onto sidebar pantry
        const pantryArea = document.querySelector('.studio-sidebar');
        if (pantryArea) {
            pantryArea.addEventListener('dragover', (e) => e.preventDefault());
            pantryArea.addEventListener('drop', (e) => {
                if (e.dataTransfer && e.dataTransfer.files && e.dataTransfer.files[0]) {
                    const f = e.dataTransfer.files[0];
                    if (isImageFile(f)) {
                        e.preventDefault();
                        openVisionScannerModal();
                        handleVisionImageSelected(f);
                    }
                }
            });
        }

        // Camera capture
        if (vEls.visionCameraBtn) {
            vEls.visionCameraBtn.addEventListener('click', handleCameraTrigger);
        }
        if (vEls.visionCloseCameraBtn) {
            vEls.visionCloseCameraBtn.addEventListener('click', stopCameraStream);
        }
        if (vEls.visionSnapBtn) {
            vEls.visionSnapBtn.addEventListener('click', snapCameraPhoto);
        }
        if (vEls.visionRemoveImageBtn) {
            vEls.visionRemoveImageBtn.addEventListener('click', resetVisionUpload);
        }

        // Scan trigger
        if (vEls.scanImageBtn) {
            vEls.scanImageBtn.addEventListener('click', performVisionScan);
        }

        // Select all / toggle chips
        if (vEls.visionSelectAllBtn) {
            vEls.visionSelectAllBtn.addEventListener('click', () => {
                const chips = vEls.visionDetectedChips.querySelectorAll('.vision-detected-chip');
                const allSelected = Array.from(chips).every(c => !c.classList.contains('is-unselected'));
                chips.forEach(c => c.classList.toggle('is-unselected', allSelected));
            });
        }

        // Import to pantry
        if (vEls.importDetectedBtn) {
            vEls.importDetectedBtn.addEventListener('click', importCheckedIngredients);
        }
    }

    function isImageFile(file) {
        if (!file) return false;
        if (file.type && file.type.startsWith('image/')) return true;
        const name = (file.name || '').toLowerCase();
        return /\.(jpe?g|png|webp|gif|bmp|jfif|heic|heif|avif|svg)$/i.test(name);
    }

    function openVisionScannerModal() {
        lastFocusedElement = document.activeElement;
        resetVisionUpload();
        vEls.visionModal.classList.remove('hidden');
    }

    function closeVisionScannerModal() {
        stopCameraStream();
        vEls.visionModal.classList.add('hidden');
        if (lastFocusedElement) lastFocusedElement.focus();
    }

    function resetVisionUpload() {
        selectedImageFile = null;
        detectedIngredientsList = [];
        stopCameraStream();
        if (vEls.visionFileInput) vEls.visionFileInput.value = '';
        if (vEls.visionCameraInput) vEls.visionCameraInput.value = '';
        if (vEls.visionDropzone) vEls.visionDropzone.classList.remove('hidden');
        if (vEls.visionPreviewBox) vEls.visionPreviewBox.classList.add('hidden');
        if (vEls.visionImagePreview) vEls.visionImagePreview.src = '';
        if (vEls.scanImageBtn) vEls.scanImageBtn.classList.add('hidden');
        if (vEls.visionLoading) vEls.visionLoading.classList.add('hidden');
        if (vEls.visionResultsBox) vEls.visionResultsBox.classList.add('hidden');
        if (vEls.importDetectedBtn) vEls.importDetectedBtn.classList.add('hidden');
        if (vEls.visionError) vEls.visionError.classList.add('hidden');
    }

    async function handleVisionImageSelected(file) {
        if (!isImageFile(file)) {
            showVisionError('Please select a valid image file (JPG, PNG, WEBP, or HEIC).');
            return;
        }

        stopCameraStream();
        if (vEls.visionDropzone) vEls.visionDropzone.classList.add('hidden');
        if (vEls.visionError) vEls.visionError.classList.add('hidden');

        try {
            const optimizedFile = await optimizeImageForUpload(file);
            selectedImageFile = optimizedFile;

            const reader = new FileReader();
            reader.onload = (e) => {
                vEls.visionImagePreview.src = e.target.result;
                vEls.visionPreviewBox.classList.remove('hidden');
                vEls.scanImageBtn.classList.remove('hidden');
                vEls.scanImageBtn.focus();
            };
            reader.readAsDataURL(optimizedFile);
        } catch (err) {
            selectedImageFile = file;
            const reader = new FileReader();
            reader.onload = (e) => {
                vEls.visionImagePreview.src = e.target.result;
                vEls.visionPreviewBox.classList.remove('hidden');
                vEls.scanImageBtn.classList.remove('hidden');
                vEls.scanImageBtn.focus();
            };
            reader.readAsDataURL(file);
        }
    }

    function optimizeImageForUpload(file) {
        return new Promise((resolve) => {
            const img = new Image();
            const url = URL.createObjectURL(file);
            img.onload = () => {
                URL.revokeObjectURL(url);
                const MAX_DIM = 1800;
                let { width, height } = img;
                if (width > MAX_DIM || height > MAX_DIM || file.size > 2 * 1024 * 1024) {
                    if (width > height) {
                        if (width > MAX_DIM) {
                            height = Math.round((height * MAX_DIM) / width);
                            width = MAX_DIM;
                        }
                    } else {
                        if (height > MAX_DIM) {
                            width = Math.round((width * MAX_DIM) / height);
                            height = MAX_DIM;
                        }
                    }
                    const canvas = document.createElement('canvas');
                    canvas.width = width;
                    canvas.height = height;
                    const ctx = canvas.getContext('2d');
                    ctx.drawImage(img, 0, 0, width, height);
                    canvas.toBlob((blob) => {
                        if (blob) {
                            resolve(new File([blob], file.name ? file.name.replace(/\.[^.]+$/, '.jpg') : 'pantry-scan.jpg', { type: 'image/jpeg' }));
                        } else {
                            resolve(file);
                        }
                    }, 'image/jpeg', 0.88);
                } else {
                    resolve(file);
                }
            };
            img.onerror = () => {
                URL.revokeObjectURL(url);
                resolve(file);
            };
            img.src = url;
        });
    }

    async function handleCameraTrigger() {
        const isMobile = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);
        
        // On mobile or when browser does not support getUserMedia (e.g. non-HTTPS), trigger native camera
        if (isMobile || !navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
            if (vEls.visionCameraInput) {
                vEls.visionCameraInput.click();
                return;
            }
        }

        // On desktop, try direct live stream with native fallback
        await startCameraStream();
    }

    async function startCameraStream() {
        if (vEls.visionError) vEls.visionError.classList.add('hidden');
        try {
            if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
                // Fallback to native camera input
                if (vEls.visionCameraInput) {
                    vEls.visionCameraInput.click();
                    return;
                }
                throw new Error('Camera access not supported');
            }

            let stream;
            try {
                stream = await navigator.mediaDevices.getUserMedia({
                    video: { facingMode: 'environment', width: { ideal: 1280 }, height: { ideal: 720 } },
                    audio: false
                });
            } catch (err) {
                // Retry with relaxed basic video constraint
                stream = await navigator.mediaDevices.getUserMedia({
                    video: true,
                    audio: false
                });
            }

            cameraStream = stream;
            vEls.visionCameraVideo.srcObject = cameraStream;
            vEls.visionCameraVideo.muted = true;
            await vEls.visionCameraVideo.play().catch(() => {});

            vEls.visionDropzone.classList.add('hidden');
            vEls.visionCameraBox.classList.remove('hidden');
        } catch (err) {
            // If live stream fails, open native file camera picker
            if (vEls.visionCameraInput) {
                vEls.visionCameraInput.click();
            } else {
                showVisionError('Could not access camera. Please allow camera permissions or browse a photo.');
            }
        }
    }

    function stopCameraStream() {
        if (cameraStream) {
            cameraStream.getTracks().forEach(t => t.stop());
            cameraStream = null;
        }
        if (vEls.visionCameraVideo) {
            vEls.visionCameraVideo.srcObject = null;
        }
        if (vEls.visionCameraBox) {
            vEls.visionCameraBox.classList.add('hidden');
        }
        if (!selectedImageFile && vEls.visionDropzone) {
            vEls.visionDropzone.classList.remove('hidden');
        }
    }

    function snapCameraPhoto() {
        if (!cameraStream) return;
        const video = vEls.visionCameraVideo;
        const width = video.videoWidth || 640;
        const height = video.videoHeight || 480;
        const canvas = document.createElement('canvas');
        canvas.width = width;
        canvas.height = height;
        const ctx = canvas.getContext('2d');
        ctx.drawImage(video, 0, 0, width, height);
        
        canvas.toBlob((blob) => {
            if (blob) {
                const file = new File([blob], 'camera-capture.jpg', { type: 'image/jpeg' });
                handleVisionImageSelected(file);
            }
        }, 'image/jpeg', 0.9);
    }

    async function performVisionScan() {
        if (!selectedImageFile) return showVisionError('Please select or capture a photo first.');
        vEls.scanImageBtn.disabled = true;
        vEls.scanImageBtn.classList.add('hidden');
        vEls.visionLoading.classList.remove('hidden');
        vEls.visionResultsBox.classList.add('hidden');
        vEls.visionError.classList.add('hidden');

        try {
            const formData = new FormData();
            formData.append('image', selectedImageFile);

            const headers = {};
            const csrf = getCsrfToken();
            if (csrf) headers['X-XSRF-TOKEN'] = csrf;

            const response = await fetch('/detect-ingredients-image', {
                method: 'POST',
                headers,
                body: formData,
                credentials: 'same-origin'
            });

            const result = await response.json();
            if (!response.ok || result.success === false) {
                throw new Error(result.error || result.message || 'Vision analysis failed');
            }

            detectedIngredientsList = result.detectedIngredients || [];
            if (!detectedIngredientsList.length) {
                throw new Error('No food ingredients could be identified in this image. Try a clearer shot with good lighting.');
            }

            renderDetectedResults(result);
            checkApiKeyStatus();
            showNotification('✨ Identified ' + detectedIngredientsList.length + ' ingredients!', 'success');
        } catch (err) {
            showVisionError(err.message);
            vEls.scanImageBtn.classList.remove('hidden');
            vEls.scanImageBtn.disabled = false;
        } finally {
            vEls.visionLoading.classList.add('hidden');
        }
    }

    function renderDetectedResults(result) {
        vEls.visionSummaryText.textContent = result.detectedSummary || 'Identified ingredients from your photo:';
        vEls.visionDetectedChips.replaceChildren();

        detectedIngredientsList.forEach(item => {
            const chip = document.createElement('div');
            chip.className = 'vision-detected-chip';
            chip.dataset.name = item;
            chip.innerHTML = `<i class="fas fa-circle-check"></i><span>${R.escapeHtml(item)}</span>`;
            chip.addEventListener('click', () => {
                chip.classList.toggle('is-unselected');
                const isUnsel = chip.classList.contains('is-unselected');
                chip.querySelector('i').className = isUnsel ? 'far fa-circle' : 'fas fa-circle-check';
            });
            vEls.visionDetectedChips.appendChild(chip);
        });

        vEls.visionResultsBox.classList.remove('hidden');
        vEls.importDetectedBtn.classList.remove('hidden');
        vEls.importDetectedBtn.focus();
    }

    function importCheckedIngredients() {
        const activeChips = Array.from(vEls.visionDetectedChips.querySelectorAll('.vision-detected-chip:not(.is-unselected)'));
        if (!activeChips.length) {
            return showVisionError('Please select at least one ingredient to import.');
        }

        const toAdd = activeChips.map(c => c.dataset.name.trim()).filter(Boolean);
        const textarea = document.getElementById('ingredients');
        const existing = textarea.value.split(',').map(s => s.trim()).filter(Boolean);

        toAdd.forEach(item => {
            if (!existing.some(e => e.toLowerCase() === item.toLowerCase())) {
                existing.push(item);
            }
        });

        textarea.value = existing.join(', ');
        textarea.focus();
        closeVisionScannerModal();
        showNotification('🛒 Imported ' + toAdd.length + ' items to your pantry!', 'success');
    }

    function showVisionError(msg) {
        vEls.visionErrorMessage.textContent = msg;
        vEls.visionError.classList.remove('hidden');
    }

    els.confirmModalOk.addEventListener('click', () => closeConfirm(true));
    els.confirmModalCancel.addEventListener('click', () => closeConfirm(false));
    els.confirmModal.addEventListener('click', (e) => {
        if (e.target === els.confirmModal) closeConfirm(false);
    });
});

