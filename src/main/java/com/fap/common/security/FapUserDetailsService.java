package com.fap.common.security;

import com.fap.user.entity.User;
import com.fap.user.enums.UserStatus;
import com.fap.user.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FapUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	public FapUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user = userRepository.findByEmailIgnoreCase(username)
				.orElseThrow(() -> new UsernameNotFoundException("User not found"));
		Set<String> roles = user.getRoles().stream()
				.map(role -> role.getName())
				.collect(Collectors.toSet());
		List<SimpleGrantedAuthority> authorities = new ArrayList<>();
		user.getRoles().forEach(role -> {
			authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
			authorities.add(new SimpleGrantedAuthority("ROLE_ID_" + role.getId()));
		});
		return new FapUserPrincipal(
				user.getId(),
				user.getEmail(),
				user.getPasswordHash(),
				roles,
				user.getStatus() == UserStatus.Active,
				authorities);
	}
}
