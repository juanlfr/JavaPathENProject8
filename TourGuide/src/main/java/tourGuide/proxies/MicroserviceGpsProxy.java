package tourGuide.proxies;

import gpsUtil.location.VisitedLocation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import tourGuide.user.User;

import java.util.UUID;

@FeignClient(name = "microservice-gps", url = "localhost:9001")
public interface MicroserviceGpsProxy {
    @GetMapping("/getLocation/{id}")
    VisitedLocation getLocation(@PathVariable("userId")  UUID userId);
}
