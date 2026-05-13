package its.repository;

import its.model.AccountStatus;
import its.model.Role;
import its.model.User;
import java.util.List;

/**
 * Repository interface for user
 * Defines operations for saving and finding users.
 *
 * @author hanung
 */

public interface UserRepository {

    
    void save(User user);

    void update(User user)

    void deleteByUserId(long userId);

    User findByUserId(long userId);

    User findByLoginId(String loginId);

    User findByAccountStatus(AccountStatus accountStatus);

    User findByRole(Role role);

    List<User> findAll();

    List<User> findPendingUsers();

    long generateUserId();
}