package com.gamestore.game_store_api.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gamestore.game_store_api.config.ManagerBootstrapProperties;
import com.gamestore.game_store_api.user.Role;
import com.gamestore.game_store_api.user.UserAccount;
import com.gamestore.game_store_api.user.UserAccountRepository;

@Service
public class ManagerBootstrapService {

	private static final Logger log = LoggerFactory.getLogger(ManagerBootstrapService.class);

	private final UserAccountRepository userAccountRepository;
	private final PasswordEncoder passwordEncoder;
	private final ManagerBootstrapProperties properties;

	public ManagerBootstrapService(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder,
			ManagerBootstrapProperties properties) {
		this.userAccountRepository = userAccountRepository;
		this.passwordEncoder = passwordEncoder;
		this.properties = properties;
	}

	@Transactional
	public void bootstrap() {
		var existingAccount = userAccountRepository.findByEmailIgnoreCase(properties.managerEmail());
		if (existingAccount.isPresent()) {
			if (existingAccount.orElseThrow().getRole() != Role.MANAGER) {
				throw new IllegalStateException("The configured manager email belongs to a non-manager account");
			}
			log.debug("Manager bootstrap skipped because the account already exists");
			return;
		}

		if (properties.managerPassword().length() < 12 || properties.managerPassword().length() > 72) {
			throw new IllegalStateException("The configured manager password must contain 12 to 72 characters");
		}
		var manager = new UserAccount(properties.managerEmail(),
				passwordEncoder.encode(properties.managerPassword()), "Store Manager", Role.MANAGER);
		var savedManager = userAccountRepository.saveAndFlush(manager);
		log.info("Bootstrapped manager account with id {}", savedManager.getId());
	}
}
