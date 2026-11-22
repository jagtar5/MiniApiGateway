// HTTP-layer test for the Admin API. Needs spring-boot-starter-test on the test classpath.
// I could not compile or run this one (no Spring jars available where I built the project), so
// treat it as a starting point: rename to AdminApiHttpTest.java, run `mvn test`, fix any import issue.
// It covers: 401/403 on missing/wrong credential, 400 with field+reason, 400 on malformed JSON.
package com.mag.gateway.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mag.gateway.config.web.AdminAuthFilter;
import com.mag.gateway.config.web.AdminController;
import com.mag.gateway.config.web.AdminExceptionHandler;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminApiHttpTest {

    private final MockMvc mvc;

    AdminApiHttpTest() {
        ConfigManager manager = new ConfigManager(new ConfigValidator(() -> Set.of("token-bucket")));
        AdminService service = new AdminService(manager, new InMemoryConfigStore());
        mvc = MockMvcBuilders.standaloneSetup(new AdminController(service))
                .setControllerAdvice(new AdminExceptionHandler())
                .addFilters(new AdminAuthFilter(new AdminCredentialChecker("tok")))
                .build();
    }

    @Test
    void noCredentialGives401() throws Exception {
        mvc.perform(get("/admin/routes")).andExpect(status().isUnauthorized());
    }

    @Test
    void wrongCredentialGives403() throws Exception {
        mvc.perform(get("/admin/routes").header("X-Admin-Token", "nope")).andExpect(status().isForbidden());
    }

    @Test
    void validCredentialGives200() throws Exception {
        mvc.perform(get("/admin/routes").header("X-Admin-Token", "tok")).andExpect(status().isOk());
    }

    @Test
    void badPayloadGives400NamingFieldAndReason() throws Exception {
        String body = "{\"id\":\"r\",\"pathPattern\":\"/r\",\"upstreamUrl\":\"http://h/\",\"cacheTtlSeconds\":-1,\"timeoutMs\":1000}";
        mvc.perform(post("/admin/routes").header("X-Admin-Token", "tok")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("routes[0].cacheTtlSeconds"));
    }

    @Test
    void malformedJsonGives400() throws Exception {
        mvc.perform(post("/admin/routes").header("X-Admin-Token", "tok")
                        .contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("body"));
    }
}
