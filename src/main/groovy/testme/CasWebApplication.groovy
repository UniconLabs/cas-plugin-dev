package testme

import org.jasig.cas.web.support.CasBanner
import org.springframework.boot.actuate.autoconfigure.MetricsDropwizardAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.groovy.template.GroovyTemplateAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.jersey.JerseyAutoConfiguration
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
import org.springframework.boot.autoconfigure.velocity.VelocityAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.cloud.config.server.EnableConfigServer
import org.springframework.context.annotation.ImportResource

@SpringBootApplication(
        scanBasePackages = ['org.jasig.cas'],
        exclude = [
                HibernateJpaAutoConfiguration,
                JerseyAutoConfiguration,
                GroovyTemplateAutoConfiguration,
                DataSourceAutoConfiguration,
                MetricsDropwizardAutoConfiguration,
                VelocityAutoConfiguration]
)
@ImportResource(locations = [
        "classpath:/spring-configuration/*.xml",
        "classpath:/spring-configuration/*.groovy",
        "classpath:/deployerConfigContext.xml",
        'classpath*:/META-INF/spring/*.xml'
])
@EnableConfigServer
class CasWebApplication {
    public static void main(String... args) {
        new SpringApplicationBuilder(CasWebApplication).banner(new CasBanner()).run(args)
    }
}
