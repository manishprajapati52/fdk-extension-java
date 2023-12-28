package com.fynd.extension.middleware;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fynd.extension.controllers.BaseApplicationController;
import com.fynd.extension.controllers.BasePlatformController;
import com.fynd.extension.model.*;
import com.fynd.extension.session.Session;

import com.sdk.v1_8_5.platform.PlatformClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ControllerInterceptor implements HandlerInterceptor {

    @Autowired
    Extension extension;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    SessionInterceptor sessionInterceptor;

    @Override
    public boolean preHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) {
        try {

            if (handler instanceof HandlerMethod) {
                HandlerMethod handlerMethod = (HandlerMethod) handler;
                Object controller = handlerMethod.getBean();

                if (controller instanceof BasePlatformController) {

                    boolean isSessionInterceptorPassed = sessionInterceptor.preHandle(request, response, handler);

                    log.info("[PLATFORM INTERCEPTOR]");
                    Session fdkSession = (Session) request.getAttribute("fdkSession");
                    PlatformClient platformClient = extension.getPlatformClient(fdkSession.getCompanyId(), fdkSession);

                    request.setAttribute("platformClient", platformClient);
                    request.setAttribute("extension", extension);

                    return isSessionInterceptorPassed;

                } else if (controller instanceof BaseApplicationController) {
                    log.info("[APPLICATION INTERCEPTOR]");
                    if (StringUtils.isNotEmpty(request.getHeader(Fields.X_USER_DATA))) {
                        User user = objectMapper.readValue(request.getHeader(Fields.X_USER_DATA), User.class);
                        request.setAttribute("user", user);
                        // TODO: add user_id in USER class
                    }

                    if (StringUtils.isNotEmpty(request.getHeader(Fields.X_APPLICATION_DATA))) {
                        Application application = objectMapper.readValue(request.getHeader(Fields.X_APPLICATION_DATA), Application.class);
                        request.setAttribute("application", application);

                        com.sdk.v1_8_5.application.ApplicationConfig applicationConfig185 = new com.sdk.v1_8_5.application.ApplicationConfig(
                                application.getID(),
                                application.getToken(),
                                extension.getExtensionProperties().getCluster()
                        );
                        com.sdk.v1_8_6.application.ApplicationConfig applicationConfig186 = new com.sdk.v1_8_6.application.ApplicationConfig(
                                application.getID(),
                                application.getToken(),
                                extension.getExtensionProperties().getCluster()
                        );
                        com.sdk.v1_8_7.application.ApplicationConfig applicationConfig = new com.sdk.v1_8_7.application.ApplicationConfig(
                                application.getID(),
                                application.getToken(),
                                extension.getExtensionProperties().getCluster()
                        );
                        request.setAttribute("applicationConfig185", applicationConfig185);
                        request.setAttribute("applicationConfig186", applicationConfig186);
                        request.setAttribute("applicationConfig", applicationConfig);

                        com.sdk.v1_8_5.application.ApplicationClient applicationClient185 = new com.sdk.v1_8_5.application.ApplicationClient(applicationConfig185);
                        com.sdk.v1_8_6.application.ApplicationClient applicationClient186 = new com.sdk.v1_8_6.application.ApplicationClient(applicationConfig186);
                        com.sdk.v1_8_7.application.ApplicationClient applicationClient = new com.sdk.v1_8_7.application.ApplicationClient(applicationConfig);
                        request.setAttribute("applicationClient185", applicationClient185);
                        request.setAttribute("applicationClient186", applicationClient186);
                        request.setAttribute("applicationClient", applicationClient);
                    }
                    return true;
                }
            }


            return true;
        } catch (Exception error) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, new Response(false, error.getMessage()).toString());
        }
    }


    public interface Fields {
        String X_USER_DATA = "x-user-data";
        String X_APPLICATION_DATA = "x-application-data";
        String COMPANY_ID = "company_id";
    }

}
