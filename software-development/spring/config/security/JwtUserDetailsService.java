import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class JwtUserDetailsService implements UserDetailsService {

	private static Logger log = LoggerFactory.getLogger(JwtUserDetailsService.class);

	/**
	 * This method is called by the JwtAuthenticationController when creating the Token, here wwe could probably call database to get permissions
	 * @param username
	 * @return
	 * @throws UsernameNotFoundException
	 */
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

		log.info("load user by name: {}", username);
//		List<GrantedAuthority> authorities = new ArrayList<>();
//		authorities.add(new SimpleGrantedAuthority("ROLE_DB_ADMIN"));
//		authorities.add(new SimpleGrantedAuthority("ROLE_EDIT_JOB"));
		return new User(username, "", new ArrayList<>());

	}

}
