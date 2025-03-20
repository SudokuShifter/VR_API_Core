package com.inlinegroup.vrcalculationbackend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.inlinegroup.vrcalculationbackend.api.fmm.HttpValidationErrorDto;
import com.inlinegroup.vrcalculationbackend.api.fmm.TaskSolutionDto;
import com.inlinegroup.vrcalculationbackend.api.zif.common.ProblemDetailsDto;
import com.inlinegroup.vrcalculationbackend.exceptions.VRClientException;
import com.inlinegroup.vrcalculationbackend.exceptions.VRNotFoundClientException;
import com.inlinegroup.vrcalculationbackend.service.enums.AppType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

@Component
@Slf4j
public class WebClientExecutionService {

    private static final String MSG_SUCCESS_GET_REQUEST = "Successful GET request: ";
    private static final String MSG_SUCCESS_POST_REQUEST = "Successful POST request: ";
    private static final String EX_MSG_NOT_FOUND = "Object not found";
    private static final String EX_MSG_GONE = "The object was deleted";
    private static final String EX_MSG_UNPROCESSABLE_ENTITY = "The calculation module returned an error";
    private static final String EX_MSG_UNAUTHORIZED = "WebClient authorization error";
    private static final String EX_MSG_BAD_REQUEST = "WebClient bad request";
    private static final String EX_MSG_5XX_ERROR = "WebClient internal server error";
    private final WebClient webClient;

    public WebClientExecutionService(WebClient webClient) {
        this.webClient = webClient;
    }

    public <T> Mono<T> executeMonoGetRequest(URI uri, AppType appType,
                                             ParameterizedTypeReference<T> parameterizedTypeReference, String logMsg) {
        return webClient
                .get()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> handleClientError(response, appType, logMsg))
                .onStatus(HttpStatusCode::is5xxServerError, response -> handleServerError(response, appType, logMsg))
                .bodyToMono(parameterizedTypeReference)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(trouble -> trouble instanceof VRClientException ex &&
                                ex.getStatus().equals(HttpStatus.BAD_GATEWAY)))
                .doOnSuccess(res -> log.info(getTextLogMsgForRequest(logMsg) + MSG_SUCCESS_GET_REQUEST + uri));
    }

    public <T> Mono<T> executeMonoPostRequest(URI uri, AppType appType,
                                              ParameterizedTypeReference<T> parameterizedTypeReference, String logMsg) {
        return webClient
                .post()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> handleClientError(response, appType, logMsg))
                .onStatus(HttpStatusCode::is5xxServerError, response -> handleServerError(response, appType, logMsg))
                .bodyToMono(parameterizedTypeReference)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(trouble -> trouble instanceof VRClientException ex &&
                                ex.getStatus().equals(HttpStatus.BAD_GATEWAY)))
                .doOnSuccess(res -> log.info(getTextLogMsgForRequest(logMsg) + MSG_SUCCESS_POST_REQUEST + uri));
    }

    public <T> Mono<T> executeMonoPostRequest(URI uri, Mono<?> body, AppType appType,
                                              ParameterizedTypeReference<T> parameterizedTypeReference, String logMsg) {
        return body.flatMap(bodyInner -> webClient
                .post()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(bodyInner)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> handleClientError(response, appType, logMsg))
                .onStatus(HttpStatusCode::is5xxServerError, response -> handleServerError(response, appType, logMsg))
                .bodyToMono(parameterizedTypeReference)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(trouble -> trouble instanceof VRClientException ex &&
                                ex.getStatus().equals(HttpStatus.BAD_GATEWAY)))
                .doOnSuccess(res -> log.info(getTextLogMsgForRequest(logMsg) + MSG_SUCCESS_POST_REQUEST + uri)));
    }

    private Mono<? extends Throwable> handleClientError(ClientResponse clientResponse, AppType appType, String logMsg) {
        Class<?> errorClass;
        if (Objects.requireNonNull(appType) == AppType.VR) {
            errorClass = HttpValidationErrorDto.class;
        } else {
            errorClass = ProblemDetailsDto.class;
        }
        if (clientResponse.statusCode() == HttpStatus.UNPROCESSABLE_ENTITY) {
            errorClass = TaskSolutionDto.class;
        }
        HttpStatusCode code = clientResponse.statusCode();

        return clientResponse
                .bodyToMono(errorClass).flatMap(body -> {
                    logErrorMsg(clientResponse, body, logMsg);
                    if (HttpStatus.NOT_FOUND.equals(code)) {
                        return Mono.error(new VRNotFoundClientException(EX_MSG_NOT_FOUND, body));
                    } else if (HttpStatus.GONE.equals(code)) {
                        return Mono.error(new VRNotFoundClientException(EX_MSG_GONE, body));
                    } else if (HttpStatus.UNPROCESSABLE_ENTITY.equals(code)) {
                        return Mono.error(new VRClientException(EX_MSG_UNPROCESSABLE_ENTITY, body,
                                clientResponse.statusCode()));
                    } else if (HttpStatus.UNAUTHORIZED.equals(code)) {
                        return Mono.error(new VRClientException(EX_MSG_UNAUTHORIZED, body,
                                clientResponse.statusCode()));
                    } else if (HttpStatus.BAD_REQUEST.equals(code)) {
                        return Mono.error(new VRClientException(EX_MSG_BAD_REQUEST, body,
                                clientResponse.statusCode()));
                    }
                    return Mono.error(new VRClientException(EX_MSG_5XX_ERROR, clientResponse.statusCode()));
                });
    }

    private Mono<? extends Throwable> handleServerError(ClientResponse clientResponse, AppType appType, String logMsg) {
        Class<?> errorClass;
        if (Objects.requireNonNull(appType) == AppType.VR) {
            errorClass = HttpValidationErrorDto.class;
        } else {
            logErrorMsg(clientResponse, logMsg);
            return Mono.error(new VRClientException(EX_MSG_5XX_ERROR, clientResponse.statusCode()));
        }
        return clientResponse
                .bodyToMono(errorClass).flatMap(body -> {
                    logErrorMsg(clientResponse, body, logMsg);
                    return Mono.error(new VRClientException(EX_MSG_5XX_ERROR, body, clientResponse.statusCode()));
                });
    }

    private void logErrorMsg(ClientResponse clientResponse, String logMsg) {
        logErrorMsg(clientResponse, null, logMsg);
    }

    private void logErrorMsg(ClientResponse clientResponse, Object body, String logMsg) {
        log.error("The request was not executed. Status code: " + clientResponse.statusCode() + "; URI: " +
                clientResponse.request().getURI() + " " + getTextRequestBodyMsg(body) + " " +
                getTextLogMsgForRequest(logMsg));
    }

    private static String getTextLogMsgForRequest(String logMsg) {
        return logMsg == null || logMsg.isEmpty() ? "" : logMsg + ". ";
    }

    protected static String getTextRequestBodyMsg(Object body) {
        String jsonError;
        try {
            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            jsonError = ow.writeValueAsString(body);
        } catch (JsonProcessingException ex) {
            jsonError = "";
        }
        return jsonError;
    }
}
