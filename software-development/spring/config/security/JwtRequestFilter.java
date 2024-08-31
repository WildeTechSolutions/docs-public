import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Profile("api")
public class JwtRequestFilter extends OncePerRequestFilter {

	private static Logger log = LoggerFactory.getLogger(JwtRequestFilter.class);

	@Autowired
	private JwtUserDetailsService jwtUserDetailsService;

	@Autowired
	private JwtTokenUtil jwtTokenUtil;

	@Autowired
	@Qualifier("handlerExceptionResolver")
	private HandlerExceptionResolver exceptionResolver;



	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {

		// Check if the request path matches any of the permitted endpoints
		log.debug("Request uri: {}, method: {}", request.getRequestURI(), request.getMethod());
		if (request.getRequestURI().endsWith("/authenticate")
				|| request.getRequestURI().endsWith("/memory")
//				|| request.getRequestURI().endsWith("/download")
		) {
			log.debug("Permitted path, no auth needed");
			chain.doFilter(request, response); // Skip authentication for permitted paths
			return;
		}
		log.debug("Testing authentication");

		String jwt = jwtTokenUtil.getJwtFromCookies(request);
		final String requestTokenHeader = request.getHeader("Authorization");

		Iterator<String> headers = request.getHeaderNames().asIterator();

		while(headers.hasNext()){
			String headerName = headers.next();
			String value = request.getHeader(headerName);
			log.info("Header: {}, value: {}", headerName, value);
		}

		String username = null;
		String jwtToken = null;

		if(jwt != null && !jwt.isBlank()){
			log.debug("Getting jwt from cookie: {}", jwt);
			jwtToken = jwt;
		}else{
			jwtToken = requestTokenHeader;

		}

		// JWT Token is in the form "Bearer token". Remove Bearer word and get only the Token
		if (jwtToken != null && jwtToken.startsWith("Bearer ")) {
			jwtToken = jwtToken.substring(7);
		}

		if(Strings.isNotBlank(jwtToken)){
			try {
				username = jwtTokenUtil.getUsernameFromToken(jwtToken);

				log.info("Got username from token: {}", username);
				for(Map.Entry<String, Object> claim : jwtTokenUtil.getAllClaimsFromToken(jwtToken).entrySet()){
					log.info("Claim name: {}, value: {}", claim.getKey(), claim.getValue());
				}



			} catch (IllegalArgumentException e) {
				log.warn("Unable to get JWT Token");
			} catch (ExpiredJwtException e) {
				log.warn("JWT Token has expired");
				this.exceptionResolver.resolveException(request, response, null, e);
				return;
			}
		}else{
			log.warn("No token was passed");
			this.exceptionResolver.resolveException(request, response, null, new UnsupportedJwtException("Not Authenticated"));
			return;
		}



		//Once we get the token validate it.
		if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

//			UserDetails userDetails = this.jwtUserDetailsService.loadUserByUsername(username);

			// if token is valid configure Spring Security to manually set authentication
			// if token is valid configure Spring Security to manually set authentication
			log.info("parsing authorities from userDetails");
			// Get the authorities from the token itself
			username = jwtTokenUtil.getUsernameFromToken(jwtToken);

			Map<String, Object> claims = jwtTokenUtil.getAllClaimsFromToken(jwtToken);

			Long id = null;
			if(claims.containsKey("id")){
				id = ((Number) claims.get("id")).longValue();
			}

			List<String> commaSepAuth = (List<String>) claims.get("authorities");
			List<GrantedAuthority> authorities = new ArrayList<>();

			if(commaSepAuth != null){
//				String[] auths = commaSepAuth.split(",");

				for(String auth : commaSepAuth){
					authorities.add(new SimpleGrantedAuthority(auth));
				}
			}


			UserDetails userDetails = new UserDetails(username, id, authorities);

			UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
					userDetails, null, authorities);
			usernamePasswordAuthenticationToken
					.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
			// After setting the Authentication in the context, we specify
			// that the current user is authenticated. So it passes the Spring Security Configurations successfully.
			SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);

		}
		chain.doFilter(request, response);
	}

}
