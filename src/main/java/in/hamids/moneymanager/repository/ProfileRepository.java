package in.hamids.moneymanager.repository;

import in.hamids.moneymanager.entity.ProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProfileRepository extends JpaRepository<ProfileEntity, Long> {

    // select * from tbl_profiles where email = ?
    Optional<ProfileEntity> findByEmail(String email);
}
