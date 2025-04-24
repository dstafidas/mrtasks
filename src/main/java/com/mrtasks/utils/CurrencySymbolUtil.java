package com.mrtasks.utils;

public final class CurrencySymbolUtil {

    private CurrencySymbolUtil() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static String getSymbol(String currencyCode) {
        return switch (currencyCode) {
            case "USD" -> "$";
            case "EUR" -> "€";
            case "GBP" -> "£";
            case "JPY", "CNY" -> "¥";
            case "AUD" -> "A$";
            case "CAD" -> "C$";
            case "CHF" -> "CHF";
            default -> "$";
        };
    }
}