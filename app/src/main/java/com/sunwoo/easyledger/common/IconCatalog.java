package com.sunwoo.easyledger.common;

import android.widget.ImageView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;

import com.sunwoo.easyledger.R;

public final class IconCatalog {
    public static final String[] KEYS = {
            "food", "dining", "cafe", "snack", "shopping", "clothes", "shoes", "delivery",
            "transport", "bus", "train", "fuel", "taxi", "parking", "flight", "hotel",
            "home", "furniture", "living", "laundry", "cleaning", "repair", "plant", "phone",
            "health", "medicine", "beauty", "hair", "culture", "movie", "music", "game",
            "sports", "hobby", "travel", "camera", "education", "book", "baby", "pet",
            "subscription", "event", "party", "gift", "heart", "work", "finance", "invest",
            "wallet", "saving", "bill", "electric", "water", "insurance", "salary", "bonus",
            "side", "allowance", "interest", "refund", "income", "transfer", "more", "generic"
    };

    private IconCatalog() {
    }

    public static void apply(ImageView view, String key) {
        if (view == null) {
            return;
        }
        view.setImageResource(resId(key));
        // Multi-color vectors — do not force a single tint.
        view.setImageTintList(null);
        view.clearColorFilter();
    }

    @DrawableRes
    public static int resId(String key) {
        if (key == null) {
            return R.drawable.ic_cat_generic;
        }
        switch (key) {
            case "food":
                return R.drawable.ic_cat_food;
            case "dining":
                return R.drawable.ic_cat_dining;
            case "cafe":
                return R.drawable.ic_cat_cafe;
            case "snack":
                return R.drawable.ic_cat_snack;
            case "shopping":
                return R.drawable.ic_cat_shopping;
            case "clothes":
                return R.drawable.ic_cat_clothes;
            case "shoes":
                return R.drawable.ic_cat_shoes;
            case "delivery":
                return R.drawable.ic_cat_delivery;
            case "transport":
                return R.drawable.ic_cat_transport;
            case "bus":
                return R.drawable.ic_cat_bus;
            case "train":
                return R.drawable.ic_cat_train;
            case "fuel":
                return R.drawable.ic_cat_fuel;
            case "taxi":
                return R.drawable.ic_cat_taxi;
            case "parking":
                return R.drawable.ic_cat_parking;
            case "flight":
                return R.drawable.ic_cat_flight;
            case "hotel":
                return R.drawable.ic_cat_hotel;
            case "home":
                return R.drawable.ic_cat_home;
            case "furniture":
                return R.drawable.ic_cat_furniture;
            case "living":
                return R.drawable.ic_cat_living;
            case "laundry":
                return R.drawable.ic_cat_laundry;
            case "cleaning":
                return R.drawable.ic_cat_cleaning;
            case "repair":
                return R.drawable.ic_cat_repair;
            case "plant":
                return R.drawable.ic_cat_plant;
            case "phone":
                return R.drawable.ic_cat_phone;
            case "health":
                return R.drawable.ic_cat_health;
            case "medicine":
                return R.drawable.ic_cat_medicine;
            case "beauty":
                return R.drawable.ic_cat_beauty;
            case "hair":
                return R.drawable.ic_cat_hair;
            case "culture":
                return R.drawable.ic_cat_culture;
            case "movie":
                return R.drawable.ic_cat_movie;
            case "music":
                return R.drawable.ic_cat_music;
            case "game":
                return R.drawable.ic_cat_game;
            case "sports":
                return R.drawable.ic_cat_sports;
            case "hobby":
                return R.drawable.ic_cat_hobby;
            case "travel":
                return R.drawable.ic_cat_travel;
            case "camera":
                return R.drawable.ic_cat_camera;
            case "education":
                return R.drawable.ic_cat_education;
            case "book":
                return R.drawable.ic_cat_book;
            case "baby":
                return R.drawable.ic_cat_baby;
            case "pet":
                return R.drawable.ic_cat_pet;
            case "subscription":
                return R.drawable.ic_cat_subscription;
            case "event":
                return R.drawable.ic_cat_event;
            case "party":
                return R.drawable.ic_cat_party;
            case "gift":
                return R.drawable.ic_cat_gift;
            case "heart":
                return R.drawable.ic_cat_heart;
            case "work":
                return R.drawable.ic_cat_work;
            case "finance":
                return R.drawable.ic_cat_finance;
            case "invest":
                return R.drawable.ic_cat_invest;
            case "wallet":
                return R.drawable.ic_cat_wallet;
            case "saving":
                return R.drawable.ic_cat_saving;
            case "bill":
                return R.drawable.ic_cat_bill;
            case "electric":
                return R.drawable.ic_cat_electric;
            case "water":
                return R.drawable.ic_cat_water;
            case "insurance":
                return R.drawable.ic_cat_insurance;
            case "salary":
                return R.drawable.ic_cat_salary;
            case "bonus":
                return R.drawable.ic_cat_bonus;
            case "side":
                return R.drawable.ic_cat_side;
            case "allowance":
                return R.drawable.ic_cat_allowance;
            case "interest":
                return R.drawable.ic_cat_interest;
            case "refund":
                return R.drawable.ic_cat_refund;
            case "income":
                return R.drawable.ic_cat_income;
            case "transfer":
                return R.drawable.ic_cat_transfer;
            case "more":
                return R.drawable.ic_cat_more;
            default:
                return R.drawable.ic_cat_generic;
        }
    }

    @ColorInt
    public static int color(String key) {
        if (key == null) {
            return 0xFF5D6D7E;
        }
        switch (key) {
            case "food":
                return 0xFFF39C12;
            case "dining":
                return 0xFFE74C3C;
            case "cafe":
                return 0xFFA0522D;
            case "snack":
                return 0xFFFF7043;
            case "shopping":
                return 0xFF9B59B6;
            case "clothes":
                return 0xFF8E44AD;
            case "shoes":
                return 0xFF795548;
            case "delivery":
                return 0xFFE67E22;
            case "transport":
                return 0xFF3498DB;
            case "bus":
                return 0xFF2980B9;
            case "train":
                return 0xFF1ABC9C;
            case "fuel":
                return 0xFF16A085;
            case "taxi":
                return 0xFFF1C40F;
            case "parking":
                return 0xFF546E7A;
            case "flight":
                return 0xFF0984E3;
            case "hotel":
                return 0xFFE67E22;
            case "home":
                return 0xFF27AE60;
            case "furniture":
                return 0xFFA0522D;
            case "living":
                return 0xFF1ABC9C;
            case "laundry":
                return 0xFF4FC3F7;
            case "cleaning":
                return 0xFF26A69A;
            case "repair":
                return 0xFF7F8C8D;
            case "plant":
                return 0xFF2ECC71;
            case "phone":
                return 0xFF8E44AD;
            case "health":
                return 0xFFE91E63;
            case "medicine":
                return 0xFFE74C3C;
            case "beauty":
                return 0xFFFF6B9D;
            case "hair":
                return 0xFFD81B60;
            case "culture":
                return 0xFF6C5CE7;
            case "movie":
                return 0xFF8E44AD;
            case "music":
                return 0xFF9B59B6;
            case "game":
                return 0xFF6C5CE7;
            case "sports":
                return 0xFF00B894;
            case "hobby":
                return 0xFFE67E22;
            case "travel":
                return 0xFF0984E3;
            case "camera":
                return 0xFF34495E;
            case "education":
                return 0xFFFD79A8;
            case "book":
                return 0xFF2980B9;
            case "baby":
                return 0xFF74B9FF;
            case "pet":
                return 0xFFE17055;
            case "subscription":
                return 0xFF636E72;
            case "event":
                return 0xFFC0392B;
            case "party":
                return 0xFFF39C12;
            case "gift":
                return 0xFFE74C3C;
            case "heart":
                return 0xFFE84393;
            case "work":
                return 0xFF2C3E50;
            case "finance":
                return 0xFF2D3436;
            case "invest":
                return 0xFF27AE60;
            case "wallet":
                return 0xFF16A085;
            case "saving":
                return 0xFF00CEC9;
            case "bill":
                return 0xFFD35400;
            case "electric":
                return 0xFFF1C40F;
            case "water":
                return 0xFF3498DB;
            case "insurance":
                return 0xFF3F51B5;
            case "salary":
                return 0xFF2ECC71;
            case "bonus":
                return 0xFFF39C12;
            case "side":
                return 0xFF9B59B6;
            case "allowance":
                return 0xFF3498DB;
            case "interest":
                return 0xFF1ABC9C;
            case "refund":
                return 0xFF27AE60;
            case "income":
                return 0xFF2ECC71;
            case "transfer":
                return 0xFF2980B9;
            case "more":
                return 0xFF7F8C8D;
            default:
                return 0xFF5D6D7E;
        }
    }
}
