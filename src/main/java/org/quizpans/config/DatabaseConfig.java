package quizpans.config;

public class DatabaseConfig {
    private static final String URL = "jdbc:mysql://34.71.86.194:3306/mieszane";
    private static final String USER = "gierka_user";
    private static final String PASSWORD = "^$F4SD%3232F$F6RY%";

    public static String getUrl() {
        return URL;
    }

    public static String getUser() {
        return USER;
    }

    public static String getPassword() {
        return PASSWORD;
    }
}