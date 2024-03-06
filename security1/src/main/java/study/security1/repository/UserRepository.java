package study.security1.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import study.security1.model.User;

public interface UserRepository extends JpaRepository<User, Long>{

    public User findByUsername(String username);
}
