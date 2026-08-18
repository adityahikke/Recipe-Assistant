package com.recipeassistant.model;

import java.util.ArrayList;
import java.util.List;

public class CookingTipsResult {
    private List<String> tips = new ArrayList<>();

    public List<String> getTips() { return tips; }
    public void setTips(List<String> tips) { this.tips = tips; }
}
