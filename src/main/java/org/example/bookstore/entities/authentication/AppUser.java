package org.example.bookstore.entities.authentication;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Persisted customer account holding normalized email and an encoded password, never an HTTP response model.
 */
@Entity
@Table(name = "app_users")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class AppUser {

	@Id
	@GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 254)
	private String email;

	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash;


	/**
	 * Creates an account from the normalized email and already encoded password supplied by registration.
	 *
	 * @param email normalized customer email
	 * @param passwordHash already encoded password; never a raw password
	 */
	public AppUser(String email, String passwordHash) {
		this.email = email;
		this.passwordHash = passwordHash;
	}


}
