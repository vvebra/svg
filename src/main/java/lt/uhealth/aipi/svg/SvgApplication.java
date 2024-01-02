package lt.uhealth.aipi.svg;

import lt.uhealth.aipi.svg.model.*;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@RegisterReflectionForBinding({Answer.class, Barrier.class, Expected.class, Issue.class, MagicItem.class,
		Params.class, Payload.class, RestError.class})
public class SvgApplication {

	public static void main(String[] args) {
		SpringApplication.run(SvgApplication.class, args);
	}

}
