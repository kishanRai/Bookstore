package org.example.bookstore.services.authentication;

import lombok.RequiredArgsConstructor;
import org.example.bookstore.application.CustomerLock;
import org.example.bookstore.repositories.authentication.AppUserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** A database row lock also coordinates an empty cart. It is released on commit or rollback. */
@Component
@RequiredArgsConstructor
public class JpaCustomerLock implements CustomerLock {
    private final AppUserRepository users;
    /**
     * Locks the customer row; requires an existing transaction and rejects an unknown customer.
     *
     * @param customerId customer ID resolved by a trusted caller
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void acquire(long customerId) {
        users.findByIdForUpdate(customerId).orElseThrow(() -> new BadCredentialsException("Authentication required"));
    }
}
