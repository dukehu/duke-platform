package ${package};

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
#if($useFeign == "y")
import org.springframework.cloud.openfeign.EnableFeignClients;
#end
#if($useDatabase == "y")
import org.mybatis.spring.annotation.MapperScan;
#end

@SpringBootApplication
@EnableDiscoveryClient
#if($useFeign == "y")
@EnableFeignClients
#end
#if($useDatabase == "y")
@MapperScan("${package}.mapper")
#end
@ConfigurationPropertiesScan("${package}.config.properties")
public class ${classPrefix}Application {

    public static void main(String[] args) {
        SpringApplication.run(${classPrefix}Application.class, args);
    }
}



