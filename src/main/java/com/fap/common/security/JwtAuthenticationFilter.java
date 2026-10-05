package com.fap.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;
	private final FapUserDetailsService userDetailsService;

	public JwtAuthenticationFilter(JwtService jwtService, FapUserDetailsService userDetailsService) {
		this.jwtService = jwtService;
		this.userDetailsService = userDetailsService;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String header = request.getHeader("Authorization");
		if (header == null || !header.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}

		String token = header.substring(7);
		try {
			// extractSubject verifies the signature and expiry, so the token is parsed only once.
			String subject = jwtService.extractSubject(token);
			if (subject != null && SecurityContextHolder.getContext().getAuthentication() == null) {
				FapUserPrincipal userDetails = userDetailsService.loadPrincipalForToken(subject);
				// A deactivated user's unexpired token must stop working immediately.
				if (userDetails.isEnabled() && subject.equals(userDetails.getUsername())) {
					UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
							userDetails,
							null,
							userDetails.getAuthorities());
					authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
					SecurityContextHolder.getContext().setAuthentication(authentication);
				}
			}
		} catch (RuntimeException ignored) {
			SecurityContextHolder.clearContext();
		}
		filterChain.doFilter(request, response);
	}
}
