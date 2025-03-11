package mg.itu.prom16.validation;

public class NumericValidator {
    public static boolean isValidNumeric(String value, double min, double max) {
        try {
            double numericValue = Double.parseDouble(value);
            return numericValue >= min && numericValue <= max;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}

