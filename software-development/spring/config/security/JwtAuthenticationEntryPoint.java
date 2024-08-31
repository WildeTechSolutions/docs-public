import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint, Serializable {
	private static Logger log = LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);
	private static final long serialVersionUID = -7858869558953243875L;

	/**
	 * Method will be triggered anytime unauthenticated requests
	 * @param request
	 * @param response
	 * @param authException
	 * @throws IOException
	 */
	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
						 AuthenticationException authException) throws IOException {

		Iterator<String> headers = request.getHeaderNames().asIterator();
		while(headers.hasNext()){
			log.info("Header: " + headers.next());
		}

		response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");

	}
}
