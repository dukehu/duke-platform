package ${package}.config;

import com.duke.framework.common.Result;
import com.duke.framework.common.ResultCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Spring Security 閰嶇疆 - 鏃犵姸鎬?JWT 妯″紡
 * 褰?useSecurity=n 鏃跺彲鍒犻櫎姝ゆ枃浠? */
#if($useSecurity != "y")
// 娉ㄦ剰锛歶seSecurity=n锛屾鏂囦欢浠呬綔鍗犱綅锛屽彲瀹夊叏鍒犻櫎
#end
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ObjectMapper objectMapper;

    /** 鐧藉悕鍗曪細鏃犻渶璁よ瘉鐨勮矾寰勶紙鐩稿浜?context-path 涔嬪悗鐨勮矾寰勶級 */
    private static final String[] WHITE_LIST = {
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/internal/**"
            // TODO
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    List<RequestMatcher> matchers = new ArrayList<>();
                    for (String pattern : WHITE_LIST) {
                        matchers.add(new AntPathRequestMatcher(pattern));
                    }
                    auth.requestMatchers(matchers.toArray(new RequestMatcher[0])).permitAll();
                    auth.anyRequest().authenticated();
                })
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) -> {
                            response.setContentType("application/json;charset=UTF-8");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.getOutputStream().write(
                                    objectMapper.writeValueAsString(Result.fail(ResultCode.UNAUTHORIZED))
                                            .getBytes(StandardCharsets.UTF_8));
                        })
                        .accessDeniedHandler((request, response, e) -> {
                            response.setContentType("application/json;charset=UTF-8");
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.getOutputStream().write(
                                    objectMapper.writeValueAsString(Result.fail(ResultCode.FORBIDDEN))
                                            .getBytes(StandardCharsets.UTF_8));
                        })
                );
        // TODO
        return http.build();
    }
}



