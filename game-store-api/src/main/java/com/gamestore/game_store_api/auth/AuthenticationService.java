package com.gamestore.game_store_api.auth;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gamestore.game_store_api.user.Role;
import com.gamestore.game_store_api.user.UserAccount;
import com.gamestore.game_store_api.user.UserAccountRepository;

@Service
public class AuthenticationService {

	private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);
	private static final int BCRYPT_MAX_PASSWORD_BYTES = 72;

	private final UserAccountRepository userAccountRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenService jwtTokenService;
	private final String dummyPasswordHash;

	public AuthenticationService(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder,
			JwtTokenService jwtTokenService) {
		this.userAccountRepository = userAccountRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtTokenService = jwtTokenService;
		this.dummyPasswordHash = passwordEncoder.encode("timing-check-only-password");
	}

	@Transactional
	public UserAccount registerBuyer(RegisterRequest request) {
		validateBcryptLength(request.password());
		if (userAccountRepository.existsByEmailIgnoreCase(request.email())) {
			throw new EmailAlreadyRegisteredException();
		}

		var account = new UserAccount(request.email(), passwordEncoder.encode(request.password()), Role.BUYER);
		try {
			var savedAccount = userAccountRepository.saveAndFlush(account);
			log.info("Registered buyer account with id {}", savedAccount.getId());
			return savedAccount;
		}
		catch (DataIntegrityViolationException exception) {
			throw new EmailAlreadyRegisteredException();
		}
	}

	@Transactional(readOnly = true)
	public IssuedToken login(LoginRequest request) {
		var account = userAccountRepository.findByEmailIgnoreCase(request.email());
		var passwordHash = account.map(UserAccount::getPasswordHash).orElse(dummyPasswordHash);
		var passwordMatches = passwordEncoder.matches(request.password(), passwordHash);

		if (account.isEmpty() || !passwordMatches || !account.orElseThrow().isEnabled()) {
			throw new BadCredentialsException("Invalid email or password");
		}

		var authenticatedAccount = account.orElseThrow();
		log.debug("Issued access token for user id {}", authenticatedAccount.getId());
		return jwtTokenService.issueFor(authenticatedAccount);
	}

	private static void validateBcryptLength(String password) {
		if (password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_PASSWORD_BYTES) {
			throw new InvalidPasswordException("Password must not exceed 72 UTF-8 bytes");
		}
	}
}
