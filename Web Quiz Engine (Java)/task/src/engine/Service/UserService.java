package engine.Service;

import engine.Entity.User;

public interface UserService {
    User register(User newUser);
    boolean containsEmail(String email);
}