package org.example.bookstore.application;

/** Serializes operations for one customer inside the caller's active transaction. */
public interface CustomerLock { /**
     * Acquires the customer lock inside the caller's transaction; the lock lasts until transaction completion.
     *
     * @param customerId customer ID resolved by a trusted caller
     */
    void acquire(long customerId); }
