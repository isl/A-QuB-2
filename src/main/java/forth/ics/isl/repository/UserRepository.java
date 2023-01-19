package forth.ics.isl.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import forth.ics.isl.data.model.User;


@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    public User findByUsername(String username);    
    public List<User> findAll();

    
}