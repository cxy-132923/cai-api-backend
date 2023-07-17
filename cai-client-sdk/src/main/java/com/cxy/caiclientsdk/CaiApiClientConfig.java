package com.cxy.caiclientsdk;

import com.cxy.caiclientsdk.client.CaiApiClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Cai API 客户端配置类
 * @author cxy
 */
@Data
@Configuration
@ConfigurationProperties("cai.client")
@ComponentScan
public class CaiApiClientConfig {

    private String accessKey;

    private String secretKey;

    /**
     * 此处方法取名无所谓的，不影响任何地方
     *
     * @return
     */
    @Bean
    public CaiApiClient getApiClient() {
        return new CaiApiClient(accessKey, secretKey);
    }
}
