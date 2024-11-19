package mg.itu.prom16.validation;

import java.text.ParseException;
import java.text.SimpleDateFormat;

public class DateValidator {
    public static boolean isValidDate(String dateStr, String format) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        dateFormat.setLenient(false);
        try {
            dateFormat.parse(dateStr);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }
}
