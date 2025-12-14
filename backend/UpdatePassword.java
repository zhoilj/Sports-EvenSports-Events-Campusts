import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class UpdatePassword {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "123456";
        String hash = encoder.encode(password);
        System.out.println("UPDATE sys_user SET password = '" + hash + "' WHERE username = 'admin';");
    }
}
