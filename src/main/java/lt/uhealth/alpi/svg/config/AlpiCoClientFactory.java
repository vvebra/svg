package lt.uhealth.alpi.svg.config;

import lt.uhealth.alpi.svg.client.AlpiCoClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class AlpiCoClientFactory {

    private final String alpiCoUrl;

    @Autowired
    public AlpiCoClientFactory(@Value("${alpico.url}") String alpiCoUrl){
        this.alpiCoUrl = alpiCoUrl;
    }

    @Bean
    AlpiCoClient alpiCoClient(){
        WebClient webClient = WebClient.builder()
                .baseUrl(alpiCoUrl)
                .build();

        HttpServiceProxyFactory httpServiceProxyFactory = HttpServiceProxyFactory
                .builderFor(WebClientAdapter.create(webClient))
                .build();

        return httpServiceProxyFactory.createClient(AlpiCoClient.class);
    }
}
