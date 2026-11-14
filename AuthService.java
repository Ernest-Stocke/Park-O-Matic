import java.util.List;
import java.util.regex.Pattern;

/** Login and account management. */
public class AuthService {
    private static final Pattern USERNAME = Pattern.compile("^[a-z0-9_.]{3,30}$");
    public static final int MIN_PASSWORD_LENGTH = 6;
    private final Repository repo;

    /** On first run (no accounts yet) creates admin/admin123 and employee/employee123. Change them! */
    public AuthService(Repository repo) {
        this.repo = repo;
        if (repo.listUsers().isEmpty()) {
            repo.addUser(new User("admin", PasswordHasher.hash("admin123"), Role.ADMIN));
            repo.addUser(new User("employee", PasswordHasher.hash("employee123"), Role.EMPLOYEE));
        }
    }

    public User login(String username, String password) {
        User user = repo.findUser(normalize(username));
        // Same message for unknown user and wrong password, so it does not reveal which usernames exist
        if (user == null || password == null || !PasswordHasher.verify(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username or password.");
        }
        return user;
    }

    public List<User> listUsers() {
        return repo.listUsers();
    }

    public void addUser(Role actor, String username, String password, Role role) {
        requireAdmin(actor);
        String name = normalize(username);
        if (!USERNAME.matcher(name).matches()) {
            throw new IllegalArgumentException("Username must be 3-30 characters: letters, digits, '_' or '.'.");
        }
        checkPassword(password);
        if (repo.findUser(name) != null) {
            throw new IllegalStateException("Username '" + name + "' already exists.");
        }
        repo.addUser(new User(name, PasswordHasher.hash(password), role));
    }

    public void changePassword(String username, String newPassword) {
        checkPassword(newPassword);
        if (repo.findUser(normalize(username)) == null) {
            throw new IllegalArgumentException("No such user.");
        }
        repo.updateUserPassword(normalize(username), PasswordHasher.hash(newPassword));
    }

    public void resetPassword(Role actor, String username, String newPassword) {
        requireAdmin(actor);
        changePassword(username, newPassword);
    }

    private static void checkPassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters.");
        }
    }

    private static void requireAdmin(Role actor) {
        if (actor != Role.ADMIN) {
            throw new IllegalStateException("Only an admin can do this.");
        }
    }

    private static String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }
}
