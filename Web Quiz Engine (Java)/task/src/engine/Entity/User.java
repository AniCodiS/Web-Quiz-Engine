package engine.Entity;

import javax.persistence.*;

@Entity
public class User {

    @Id
    @Column(nullable = false, unique = true)
    private String email;
    @Column(nullable = false)
    private String password;

    public User() {

    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isValid() {
        if (email != null) {
            if (!email.contains("@") || !email.contains(".")) {
                return false;
            }
        }
        if (password != null) {
            return password.length() >= 5;
        }
        return true;
    }
}