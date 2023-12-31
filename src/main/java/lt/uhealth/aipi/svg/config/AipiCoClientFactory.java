package lt.uhealth.aipi.svg.config;

import lt.uhealth.aipi.svg.client.AipiCoClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class AipiCoClientFactory {

    private final String aipiCoUrl;

    @Autowired
    public AipiCoClientFactory(@Value("${aipico.url}") String aipiCoUrl){
        this.aipiCoUrl = aipiCoUrl;
    }

    @Bean
    AipiCoClient aipiCoClient(){
        WebClient webClient = WebClient.builder()
                .baseUrl(aipiCoUrl)
                .build();

        HttpServiceProxyFactory httpServiceProxyFactory = HttpServiceProxyFactory
                .builderFor(WebClientAdapter.create(webClient))
                .build();

        return httpServiceProxyFactory.createClient(AipiCoClient.class);
    }
}
