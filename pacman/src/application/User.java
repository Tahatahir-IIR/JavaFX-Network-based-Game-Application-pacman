package application;

public class User {
    public final int id;
    public final String username;
    public final boolean isAdmin;

    public User(int id, String username) {
        this.id = id;
        this.username = username;
        this.isAdmin = false;
    }
    
    public User(int id, String username, boolean isAdmin) {
        this.id = id;
        this.username = username;
        this.isAdmin = isAdmin;
    }
}
