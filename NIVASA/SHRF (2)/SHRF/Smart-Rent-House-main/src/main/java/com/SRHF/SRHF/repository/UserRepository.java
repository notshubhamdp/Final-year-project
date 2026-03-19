package com.SRHF.SRHF.repository;

import com.SRHF.SRHF.entity.User;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;


public interface UserRepository extends CrudRepository<User, Long> {

    Optional<User> findByemail(String email);

    // Query to fetch pending student verifications
    List<User> findByTenantTypeAndStudentVerificationStatus(String tenantType, String status);

    // Query to fetch banned users
    List<User> findByBanned(boolean banned);

}
