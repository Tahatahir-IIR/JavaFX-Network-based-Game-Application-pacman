package application;

public class UserSession {
    private static User currentUser = null;

    public static void setCurrentUser(User u) { currentUser = u; }
    public static User getCurrentUser() { return currentUser; }
}
