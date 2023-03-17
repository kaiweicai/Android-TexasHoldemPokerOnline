package org.hit.android.haim.texasholdem.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.security.util.FieldUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.oas.annotations.EnableOpenApi;
import springfox.documentation.service.*;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;


/**
 * Swagger配置组件
 */
@Configuration
@EnableOpenApi
public class SwaggerConfig implements WebMvcConfigurer {

    @Resource
    private SwaggerProperties swaggerProperties;
    //是否开启 swagger，正式环境一般是需要关闭的，可根据springboot的多环境配置进行设置

    @Bean
    @Order(1)
    public Docket createRestApis() {
        return new Docket(DocumentationType.OAS_30)
                .apiInfo(apiInfo())
                // 是否开启
                .enable(swaggerProperties.getEnable())
                .select()
                // 扫描的路径包
                .apis(RequestHandlerSelectors.basePackage("org.hit.android.haim.texasholdem.server.controller"))
                // 指定路径处理PathSelectors.any()代表所有的路径
//				.paths(frontPathsAnt()) //只监听
                .build()
                .securitySchemes(security())
                .securityContexts(securityContexts())
                .pathMapping("/");
    }

//	private Predicate<String> frontPathsAnt() {
//		return PathSelectors.ant("/api/front/**");
//	}

    private List<SecurityScheme> security() {
        List<SecurityScheme> result = new ArrayList<>();
        result.add(new ApiKey(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.AUTHORIZATION_HEADER, "header"));
        return result;
    }

    /**
     * 授权信息全局应用
     */
    private List<SecurityContext> securityContexts() {
        return Collections.singletonList(
                SecurityContext.builder()
                        .securityReferences(Collections.singletonList(new SecurityReference(JwtAuthenticationFilter.AUTHORIZATION_HEADER, new AuthorizationScope[]{new AuthorizationScope("global", "")})))
                        .build()
        );
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title(swaggerProperties.getApplicationName())
                .description(swaggerProperties.getApplicationDescription())
                .termsOfServiceUrl("http://host:port")
                .version(swaggerProperties.getApplicationVersion()).build();
    }

    private List<SecurityReference> defaultAuth() {
        List<SecurityReference> res = new ArrayList<>();
        AuthorizationScope authorizationScope = new AuthorizationScope("global", JwtAuthenticationFilter.AUTHORIZATION_HEADER);
        AuthorizationScope[] authorizationScopes = new AuthorizationScope[1];
        authorizationScopes[0] = authorizationScope;
        res.add(new SecurityReference(JwtAuthenticationFilter.AUTHORIZATION_HEADER, authorizationScopes));
        return res;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        try {
            Field registrationsField = FieldUtils.getField(InterceptorRegistry.class, "registrations");
            List<InterceptorRegistration> registrations = (List<InterceptorRegistration>) ReflectionUtils.getField(registrationsField, registry);
            if (registrations != null) {
                for (InterceptorRegistration interceptorRegistration : registrations) {
                    interceptorRegistration
                            .excludePathPatterns("/swagger**/**")
                            .excludePathPatterns("/webjars/**")
                            .excludePathPatterns("/v3/**")
                            .excludePathPatterns("/doc.html");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
