import java.time.Year;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates Indian vehicle registration numbers.
 *
 * Supported formats (spaces and hyphens are ignored, case-insensitive):
 *  1. Standard:  SS DD XXX NNNN   e.g. MH12AB1234, DL1CAB1234
 *       SS  = state / UT code (must be a real code, e.g. MH, DL, KA)
 *       DD  = RTO district number, 1-2 digits, not zero
 *       XXX = series, 1-3 letters
 *       NNNN = 4-digit number, 0001-9999
 *  2. Bharat (BH) series:  YY BH NNNN XX   e.g. 22BH1234AA
 *       YY = year of first registration (21 onwards), XX = two letters (I and O are not used)
 *
 * This checks the FORMAT only. It cannot tell whether the plate is actually issued to a vehicle.
 * Temporary, diplomatic and military plates use other formats and are not accepted.
 */
public final class PlateValidator {
    private static final Pattern STANDARD = Pattern.compile("^([A-Z]{2})(\\d{1,2})([A-Z]{1,3})(\\d{4})$");
    private static final Pattern BH_SERIES = Pattern.compile("^(\\d{2})BH(\\d{4})([A-HJ-NP-Z]{2})$");
    private static final Pattern ALLOWED_CHARS = Pattern.compile("^[A-Z0-9]+$");

    private static final Set<String> STATE_CODES = new HashSet<>(Arrays.asList(
            "AN", "AP", "AR", "AS", "BR", "CH", "CG", "DD", "DN", "DL", "GA", "GJ", "HP", "HR",
            "JH", "JK", "KA", "KL", "LA", "LD", "MH", "ML", "MN", "MP", "MZ", "NL", "OD", "OR",
            "PB", "PY", "RJ", "SK", "TN", "TR", "TS", "TG", "UK", "UA", "UP", "WB"));

    private PlateValidator() { }

    /** Removes spaces and hyphens and upper-cases, e.g. " mh-12 ab 1234 " -> "MH12AB1234". */
    public static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toUpperCase().replaceAll("[\\s-]", "");
    }

    /** Returns null if the plate is valid, otherwise a human-readable reason. */
    public static String validate(String raw) {
        String plate = normalize(raw);
        if (plate.isEmpty()) {
            return "Registration number cannot be empty.";
        }
        if (!ALLOWED_CHARS.matcher(plate).matches()) {
            return "Use only letters and digits (spaces and hyphens are ignored).";
        }

        Matcher bh = BH_SERIES.matcher(plate);
        if (bh.matches()) {
            int year = Integer.parseInt(bh.group(1));
            int currentYear = Year.now().getValue() % 100;
            if (year < 21 || year > currentYear) {
                return "BH-series year '" + bh.group(1) + "' is not valid (BH series started in 2021).";
            }
            if (Integer.parseInt(bh.group(2)) == 0) {
                return "Plate number part cannot be 0000.";
            }
            return null;
        }

        Matcher std = STANDARD.matcher(plate);
        if (std.matches()) {
            if (!STATE_CODES.contains(std.group(1))) {
                return "Unknown state/UT code '" + std.group(1) + "'.";
            }
            if (Integer.parseInt(std.group(2)) == 0) {
                return "RTO district number cannot be 00.";
            }
            if (Integer.parseInt(std.group(4)) == 0) {
                return "Plate number part cannot be 0000.";
            }
            return null;
        }
        return "Not a valid Indian plate. Expected e.g. MH12AB1234 (state, district, series, number) "
                + "or 22BH1234AA (BH series).";
    }

    public static boolean isValid(String raw) {
        return validate(raw) == null;
    }

    /** Returns the normalised plate or throws IllegalArgumentException with the reason. */
    public static String normalizeAndValidate(String raw) {
        String error = validate(raw);
        if (error != null) {
            throw new IllegalArgumentException(error);
        }
        return normalize(raw);
    }
}
