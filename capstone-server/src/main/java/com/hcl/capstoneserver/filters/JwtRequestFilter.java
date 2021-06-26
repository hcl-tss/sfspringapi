package com.hcl.capstoneserver.filters;

import com.hcl.capstoneserver.user.UserService;
import com.hcl.capstoneserver.util.JWTUtil;
import io.jsonwebtoken.JwtException;
import org.hibernate.annotations.Filter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {
    private final UserService userService;
    private final JWTUtil jwtUtil;

    public JwtRequestFilter(UserService userService, JWTUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // extract the authorization header
        final String authorizationHeader = request.getHeader("Authorization");

        String jwt = null;
        String userName = null;
        System.out.println("auth header:"+authorizationHeader);
        //check if authorization header has bearer on it
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            // extract content after bearer as jwt token
            // Bearer jwt_token -> jwt_token
            jwt = authorizationHeader.substring(7);

            try {
                //extract the username from jwt
                userName = jwtUtil.extractUsername(jwt);
            } catch (JwtException ignored) {
            	
            }
        }

        //if username exists and request is not already authorized
        if (userName != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            //fetch the user by username
            String userType = jwtUtil.extractUserType(jwt);

            //check if the jwt is valid or not
            if (jwtUtil.validateToken(jwt)) {
                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(
                                userName, null, Collections.singleton(new SimpleGrantedAuthority(userType))
                        );

                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
