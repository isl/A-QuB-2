//package forth.ics.isl.configuration;
//
//import javax.ws.rs.HttpMethod;
//import javax.sql.DataSource;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.builders.WebSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
//
//@Configuration
//@EnableWebSecurity
//public class AppSecurityConfig extends WebSecurityConfigurerAdapter {
//
//    @Autowired
//    private BCryptPasswordEncoder bCryptPasswordEncoder;
//
//    @Autowired
//    private DataSource dataSource;
//
//    @Value("${spring.queries.users-query}")
//    private String usersQuery;
//
//    @Value("${spring.queries.roles-query}")
//    private String rolesQuery;
//
//    @Override
//    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
//        auth.jdbcAuthentication().usersByUsernameQuery(usersQuery).authoritiesByUsernameQuery(rolesQuery)
//                .dataSource((javax.sql.DataSource) dataSource).passwordEncoder(bCryptPasswordEncoder);
//        // auth.inMemoryAuthentication().withUser("user").password("user").roles("USER");
//    }
//
//    @Override
//    protected void configure(HttpSecurity http) throws Exception {
//        System.out.println("CALLED");
//
//        http.authorizeRequests()
//                .antMatchers(HttpMethod.GET, "/login", "/webjars/**", "/", "/views/**", "/index.html", "/js/**", "/static/**", "/css/**", "/images/**",
//                        "/registration").permitAll()
//                .antMatchers("/registerUser").permitAll()
//                .antMatchers("/loginSucess").permitAll()
//                .antMatchers("/retrieve_portal_state").permitAll()
//                .anyRequest().authenticated()// enable security
//                .and()
//                .sessionManagement()
//                .invalidSessionUrl("/logout")
//                .and()
//                .csrf().disable()
//                .formLogin()
//                .loginPage("/login")
//                .failureUrl("/login")
//                .defaultSuccessUrl("/loginSucess")
//                .loginProcessingUrl("/login")
//                .usernameParameter("username").passwordParameter("password")
//                .and()
//                .logout()
//                .logoutRequestMatcher(new AntPathRequestMatcher("/logout")).logoutSuccessUrl("/login").deleteCookies("auth_code", "JSESSIONID").invalidateHttpSession(true)
//                .and()
//                .exceptionHandling().accessDeniedPage("/access-denied");
//
//    }
//
//    @Override
//    public void configure(WebSecurity web) throws Exception {
//        // web.ignoring().antMatchers("/resources/**", "/static/**", "/css/**",
//        // "/js/**", "/images/**", "/console/**",
//        web.ignoring().antMatchers("/console/**", HttpMethod.OPTIONS, "/js/**", "/css/**", "/images/**", "/webjars/**", "/static/**", "/css/**", "/images/**");// ,
//
//        // "/**"
//        // web.ignoring().antMatchers(HttpMethod.OPTIONS, "/index*");//, "/**"
//    }
//
//}
