package com.example.dinesplit.domain.exception

/**
 * Thrown when attempting to register a username that is already taken.
 */
class UsernameAlreadyExistsException : IllegalArgumentException("Username already exists")
