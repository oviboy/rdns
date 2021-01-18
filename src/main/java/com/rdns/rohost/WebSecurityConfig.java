package com.rdns.rohost;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
	@Autowired UserRepository ur;
	@Override
    protected void configure(HttpSecurity http) throws Exception {
        http
        	.csrf().disable()
            .authorizeRequests()
            .anyRequest().authenticated()
            .and()
            .httpBasic();
    }
	
	@Override
	@Bean
	public UserDetailsService userDetailsService() {
        List<UserDetails> users= new ArrayList<UserDetails>();
        try {
        	for(User u: ur.findAll()) {
        		users.add(org.springframework.security.core.userdetails.User
        				.withUsername(u.getZonename())
        				.password("{noop}"+u.getPassword())
        				.roles("USER")
        				.build()
        		);
        	}
        } catch(Exception ex) {
	        ex.printStackTrace();
        	return new InMemoryUserDetailsManager(users);
        }
        return new InMemoryUserDetailsManager(users);
	}
}
