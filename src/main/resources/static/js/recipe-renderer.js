/* ============================================================
   Structured Recipe Renderer – Interactive Culinary Studio
   ============================================================ */
window.RecipeRenderer = (function () {
    function escapeHtml(text) {
        if (text == null) return '';
        const div = document.createElement('div');
        div.textContent = String(text);
        return div.innerHTML;
    }

    function formatNutritionLabel(key) {
        const labels = {
            calories: 'Calories',
            protein: 'Protein',
            carbs: 'Carbs',
            fat: 'Fat',
            fiber: 'Fiber',
            sugar: 'Sugar',
            sodium: 'Sodium'
        };
        if (labels[key]) return labels[key];
        return key.replace(/([A-Z])/g, ' $1').replace(/^./, s => s.toUpperCase());
    }

    function renderStructuredRecipe(recipe) {
        const root = document.createElement('div');
        root.className = 'recipe-plate';

        // 1. Hero Recipe Header
        if (recipe.name) {
            const header = document.createElement('div');
            header.className = 'recipe-header-hero';
            
            let metaHtml = '';
            const metaItems = [];
            if (recipe.preparationTime) metaItems.push(['Prep', recipe.preparationTime]);
            if (recipe.cookingTime) metaItems.push(['Cook', recipe.cookingTime]);
            if (recipe.servings) metaItems.push(['Servings', recipe.servings]);
            
            if (metaItems.length) {
                metaHtml = `<div class="recipe-meta-grid">` + 
                    metaItems.map(([label, value]) => `
                        <div class="recipe-meta-badge">
                            <div class="meta-badge-label">${escapeHtml(label)}</div>
                            <div class="meta-badge-value">${escapeHtml(value)}</div>
                        </div>
                    `).join('') + `</div>`;
            }

            header.innerHTML = `
                <h1 class="recipe-title">${escapeHtml(recipe.name)}</h1>
                ${metaHtml}
            `;
            root.appendChild(header);
        }

        // 2. Interactive Ingredients Checklist
        if (recipe.ingredients && recipe.ingredients.length) {
            const ingSection = document.createElement('div');
            ingSection.className = 'recipe-section';
            ingSection.innerHTML = `
                <h2 class="recipe-section-title">
                    <i class="fas fa-list-check" aria-hidden="true"></i>
                    Ingredients <span style="font-size:0.8125rem;color:var(--c-text-dim);font-weight:400;margin-left:auto;">Check off as you prep</span>
                </h2>
                <ul class="interactive-ingredients-list"></ul>
            `;
            const ul = ingSection.querySelector('ul');
            recipe.ingredients.forEach(item => {
                const li = document.createElement('li');
                li.className = 'ingredient-item-row';
                li.innerHTML = `
                    <input type="checkbox" class="ingredient-item-checkbox" aria-label="Mark ingredient ready">
                    <span class="ingredient-item-text">${escapeHtml(item)}</span>
                `;
                li.addEventListener('click', (e) => {
                    const cb = li.querySelector('.ingredient-item-checkbox');
                    if (e.target !== cb) {
                        cb.checked = !cb.checked;
                    }
                    li.classList.toggle('is-checked', cb.checked);
                });
                ul.appendChild(li);
            });
            root.appendChild(ingSection);
        }

        // 3. Interactive Step-by-Step Instructions
        if (recipe.instructions && recipe.instructions.length) {
            const stepSection = document.createElement('div');
            stepSection.className = 'recipe-section';
            stepSection.innerHTML = `
                <h2 class="recipe-section-title">
                    <i class="fas fa-kitchen-set" aria-hidden="true"></i>
                    Step-by-Step Instructions <span style="font-size:0.8125rem;color:var(--c-text-dim);font-weight:400;margin-left:auto;">Click step to highlight</span>
                </h2>
                <ol class="interactive-steps-list"></ol>
            `;
            const ol = stepSection.querySelector('ol');
            recipe.instructions.forEach((step, idx) => {
                const li = document.createElement('li');
                li.className = 'step-item-card';
                li.textContent = step;
                li.addEventListener('click', () => {
                    ol.querySelectorAll('.step-item-card').forEach(s => s.classList.remove('is-active-step'));
                    li.classList.add('is-active-step');
                });
                ol.appendChild(li);
            });
            root.appendChild(stepSection);
        }

        // 4. Chef Pro Tips Box
        if (recipe.tips && recipe.tips.length) {
            const tipSection = document.createElement('div');
            tipSection.className = 'recipe-section';
            tipSection.innerHTML = `
                <div class="pro-tips-card">
                    <h2 class="recipe-section-title" style="color:var(--c-amber-light);margin-bottom:0.75rem;">
                        <i class="fas fa-lightbulb" style="color:var(--c-amber);" aria-hidden="true"></i>
                        Chef's Secret Tips
                    </h2>
                    <ul></ul>
                </div>
            `;
            const ul = tipSection.querySelector('ul');
            recipe.tips.forEach(tip => {
                const li = document.createElement('li');
                li.textContent = tip;
                ul.appendChild(li);
            });
            root.appendChild(tipSection);
        }

        // 5. Nutritional Macro Dashboard
        if (recipe.nutrition && Object.keys(recipe.nutrition).length) {
            const nutSection = document.createElement('div');
            nutSection.className = 'recipe-section';
            nutSection.innerHTML = `
                <div class="nutrition-card">
                    <h2 class="recipe-section-title" style="margin-bottom:0.875rem;">
                        <i class="fas fa-chart-pie" style="color:var(--c-emerald);" aria-hidden="true"></i>
                        Nutritional Overview
                    </h2>
                    <div class="nutrition-grid"></div>
                </div>
            `;
            const grid = nutSection.querySelector('.nutrition-grid');
            Object.entries(recipe.nutrition).forEach(([label, value]) => {
                const item = document.createElement('div');
                item.className = 'nutrition-macro-item';
                item.innerHTML = `
                    <div class="nutrition-macro-val">${escapeHtml(value)}</div>
                    <div class="nutrition-macro-label">${escapeHtml(formatNutritionLabel(label))}</div>
                `;
                grid.appendChild(item);
            });
            root.appendChild(nutSection);
        }

        return root;
    }

    function renderTipsBanner() {
        const banner = document.createElement('div');
        banner.className = 'alert-box alert-box-blue';
        banner.style.marginBottom = '1.25rem';
        banner.innerHTML = `
            <i class="fas fa-lightbulb" style="color:var(--c-amber);font-size:1.125rem;" aria-hidden="true"></i>
            <div>
                <strong>Culinary Tips & Hacks Mode:</strong> Showing instant cooking wisdom for your ingredients. Click <strong>Generate Recipe</strong> for full proportions & steps.
            </div>
        `;
        return banner;
    }

    function renderCookingTips(tipsResult) {
        const root = document.createElement('div');
        root.className = 'recipe-plate';

        const section = document.createElement('div');
        section.className = 'recipe-section';
        section.innerHTML = `
            <div class="pro-tips-card">
                <h2 class="recipe-section-title" style="color:var(--c-amber-light);margin-bottom:0.875rem;">
                    <i class="fas fa-wand-magic-sparkles" style="color:var(--c-amber);" aria-hidden="true"></i>
                    AI Culinary Tips & Combinations
                </h2>
                <ul></ul>
            </div>
        `;
        const ul = section.querySelector('ul');
        (tipsResult.tips || []).forEach(tip => {
            const li = document.createElement('li');
            li.textContent = tip;
            ul.appendChild(li);
        });
        root.appendChild(section);
        return root;
    }

    function recipeToPlainText(recipe) {
        if (!recipe) return '';
        const lines = [];
        if (recipe.name) lines.push(recipe.name.toUpperCase(), '='.repeat(recipe.name.length), '');
        if (recipe.preparationTime) lines.push('⏱️ Prep Time: ' + recipe.preparationTime);
        if (recipe.cookingTime) lines.push('🔥 Cook Time: ' + recipe.cookingTime);
        if (recipe.servings) lines.push('🍽️ Servings: ' + recipe.servings);
        
        if (recipe.ingredients && recipe.ingredients.length) {
            lines.push('', '--- INGREDIENTS ---');
            recipe.ingredients.forEach(i => lines.push('• ' + i));
        }
        if (recipe.instructions && recipe.instructions.length) {
            lines.push('', '--- INSTRUCTIONS ---');
            recipe.instructions.forEach((step, idx) => lines.push((idx + 1) + '. ' + step));
        }
        if (recipe.tips && recipe.tips.length) {
            lines.push('', '--- CHEF TIPS ---');
            recipe.tips.forEach(t => lines.push('💡 ' + t));
        }
        if (recipe.nutrition && Object.keys(recipe.nutrition).length) {
            lines.push('', '--- NUTRITION ---');
            Object.entries(recipe.nutrition).forEach(([k, v]) => lines.push(formatNutritionLabel(k) + ': ' + v));
        }
        lines.push('', 'Crafted by CulinaryAI Chef Studio — culinaryai.app');
        return lines.join('\n');
    }

    function renderPrintDocument(recipe) {
        const inner = renderStructuredRecipe(recipe).outerHTML;
        return `<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>${escapeHtml(recipe.name || 'Recipe')} — CulinaryAI Chef Studio</title>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;600;700;800&display=swap" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" rel="stylesheet">
    <link href="/css/recipe.css" rel="stylesheet">
    <style>
        body { background: #fff !important; color: #0f172a !important; font-family: 'Plus Jakarta Sans', sans-serif; padding: 2.5rem; max-width: 800px; margin: 0 auto; }
        .recipe-title { color: #ea580c !important; -webkit-text-fill-color: #ea580c !important; font-size: 2rem !important; }
        .recipe-meta-badge { background: #f8fafc !important; border-color: #cbd5e1 !important; }
        .meta-badge-value { color: #0f172a !important; }
        .ingredient-item-row, .step-item-card, .pro-tips-card, .nutrition-card { background: #f8fafc !important; border-color: #e2e8f0 !important; color: #1e293b !important; }
        .ingredient-item-checkbox { display: none !important; }
        .recipe-section-title { color: #0f172a !important; }
        .pro-tips-card li { color: #334155 !important; }
        .nutrition-macro-item { background: #fff !important; border-color: #cbd5e1 !important; }
        .nutrition-macro-val { color: #ea580c !important; }
    </style>
</head>
<body>
    <div class="print-container">
        ${inner}
    </div>
    <script>window.onload = () => { window.print(); }</script>
</body>
</html>`;
    }

    return {
        escapeHtml,
        formatNutritionLabel,
        renderStructuredRecipe,
        renderCookingTips,
        renderTipsBanner,
        recipeToPlainText,
        renderPrintDocument
    };
})();
