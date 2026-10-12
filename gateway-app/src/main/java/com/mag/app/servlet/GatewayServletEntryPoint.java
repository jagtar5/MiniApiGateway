package com.mag.app.servlet;

import com.mag.core.filter.FilterChain;
import com.mag.core.filter.GatewayContext;
import com.mag.core.model.GatewayRequest;
import com.mag.core.model.GatewayResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;

/**
 * Servlet entry point — the single Spring @Controller that receives all requests.
 *
 * Responsibilities (SRP: boundary translation only):
 *   1. Convert {@link HttpServletRequest} → {@link GatewayRequest}.
 *   2. Build (or look up) the per-route {@link FilterChain}.
 *   3. Execute the chain.
 *   4. Write the {@link GatewayResponse} back to {@link HttpServletResponse}.
 *
 * No business logic here — all logic lives in gateway-core filters.
 */
@Controller
@RequestMapping("/**")
public class GatewayServletEntryPoint {

    // TODO M2: inject PipelineBuilder (or a pre-built chain-per-route map) and ConfigProvider

    @RequestMapping
    public void handle(HttpServletRequest httpRequest, HttpServletResponse httpResponse)
            throws IOException {
        // TODO M2:
        //   1. Convert httpRequest → GatewayRequest via RequestConverter.
        //   2. Look up the matching route from config (MAG-FR-01: highest-priority match).
        //   3. Build/retrieve the FilterChain for that route.
        //   4. Create GatewayContext and run chain.proceed(ctx).
        //   5. Write ctx.getResponse() back to httpResponse.
        //   6. Catch GatewayException → GatewayExceptionMapper.toResponse() → write to httpResponse.
        httpResponse.setStatus(501);
        httpResponse.getWriter().write("Gateway not yet implemented — M2");
    }
}
