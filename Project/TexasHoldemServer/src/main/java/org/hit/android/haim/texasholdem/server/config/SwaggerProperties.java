package org.hit.android.haim.texasholdem.server.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("swagger")//通过该属性可以获得application.yml中的swagger值
@Getter
@Setter
public class SwaggerProperties {
    /**
     * 是否开启swagger，生产环境一般关闭，所以这里定义一个变量
     */
    private Boolean enable;//对应application.yml中的swagger.enable

    /**
     * 项目应用名
     */
    private String applicationName;//对应application.yml中的swagger.application-name

    /**
     * 项目版本信息
     */
    private String applicationVersion;//对应application.yml中的swagger.application.version

    /**
     * 项目描述信息
     */
    private String applicationDescription;//对应application.yml中的swagger.application-description

    /**
     * 接口调试地址
     */
    private String tryHost;//对应application.yml中的swagger.try-host
}
