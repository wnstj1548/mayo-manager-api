package com.example.mayoweb.commons.interceptor;

import com.example.mayoweb.commons.annotation.Authenticated;
import com.example.mayoweb.commons.annotation.CreateUser;
import com.example.mayoweb.commons.exception.ApplicationException;
import com.example.mayoweb.commons.exception.payload.ErrorStatus;
import com.example.mayoweb.commons.utils.JwtTokenUtils;
import com.example.mayoweb.user.service.UserService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;

@Component
public class CreateUserInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        if(handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            CreateUser createUser = handlerMethod.getMethodAnnotation(CreateUser.class);

            if(createUser != null) {
                String token = JwtTokenUtils.getBearerToken(request);
                if(token == null) {
                    throw new ApplicationException(
                            ErrorStatus.toErrorStatus("허가 되지 않은 사용자입니다.", 401, LocalDateTime.now())
                    );
                }

                FirebaseToken decodedToken = null;
                try {
                    decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
                } catch (FirebaseAuthException e) {
                    throw new ApplicationException(
                            ErrorStatus.toErrorStatus("허가 되지 않은 사용자입니다.", 401, LocalDateTime.now())
                    );
                }

                request.setAttribute("uid", decodedToken.getUid());
            }
        }
        return true;
    }
}
