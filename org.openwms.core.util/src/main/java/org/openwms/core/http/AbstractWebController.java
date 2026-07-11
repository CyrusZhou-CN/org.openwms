/*
 * Copyright 2005-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openwms.core.http;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import org.ameba.LoggingCategories;
import org.ameba.exception.BehaviorAwareException;
import org.ameba.exception.BusinessRuntimeException;
import org.ameba.exception.TechnicalRuntimeException;
import org.ameba.http.Response;
import org.openwms.core.exception.ExceptionCodes;
import org.openwms.core.listener.RemovalNotAllowedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.util.UriTemplate;

import java.io.Serializable;
import java.net.URI;
import java.util.Locale;

/**
 * An AbstractWebController is the common base class for all REST controllers in OpenWMS.org services. It maps the well-known exception
 * types onto appropriate HTTP responses with translated messages, and offers factory methods to build {@code ResponseEntity} instances
 * and {@code Location} URIs for created resources - also behind proxies, by respecting the {@code x-forwarded-*} headers.
 *
 * @author Heiko Scherrer
 */
public abstract class AbstractWebController {

    private static final Logger EXC_LOGGER = LoggerFactory.getLogger(LoggingCategories.PRESENTATION_LAYER_EXCEPTION);
    private static final String P_PRESENTATION_LAYER_EXCEPTION = "[P] Presentation Layer Exception: {}";
    private final MessageSource messageSource;

    /**
     * Constructor.
     *
     * @param messageSource The Spring MessageSource used to translate message keys into message texts
     */
    protected AbstractWebController(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * Map a {@link BehaviorAwareException} onto a response with the HTTP status the exception carries.
     *
     * @param bae The exception to map
     * @return A ResponseEntity with the exception's status, message, message key and data
     */
    @ExceptionHandler(BehaviorAwareException.class)
    protected ResponseEntity<Response<?>> handleBehaviorAwareException(BehaviorAwareException bae) {
        EXC_LOGGER.error(P_PRESENTATION_LAYER_EXCEPTION, bae.getLocalizedMessage(), bae);
        return new ResponseEntity<>(Response.newBuilder()
                .withMessage(bae.getMessage())
                .withMessageKey(bae.getMessageKey())
                .withHttpStatus(String.valueOf(bae.getStatus().value()))
                .withObj(bae.getData())
                .build(),
                bae.getStatus()
        );
    }

    /**
     * Map a {@link RemovalNotAllowedException} onto a response with HTTP status {@code 403 FORBIDDEN}.
     *
     * @param rnae The exception to map
     * @return A ResponseEntity with status {@code 403 FORBIDDEN}
     */
    @ExceptionHandler(RemovalNotAllowedException.class)
    protected ResponseEntity<Response<?>> handleRemovalNotAllowedException(RemovalNotAllowedException rnae) {
        EXC_LOGGER.error(P_PRESENTATION_LAYER_EXCEPTION, rnae.getLocalizedMessage(), rnae);
        return new ResponseEntity<>(Response.newBuilder()
                .withMessage(rnae.getMessage())
                .withMessageKey(rnae.getMessageKey())
                .withObj(rnae.getData())
                .build(),
                HttpStatus.FORBIDDEN
        );
    }

    /**
     * Map a {@link BusinessRuntimeException} onto a response with the HTTP status taken from the exception's {@code ResponseStatus}
     * annotation, or {@code 500 INTERNAL_SERVER_ERROR} if the exception class is not annotated.
     *
     * @param bre The exception to map
     * @return A ResponseEntity with the resolved status, message, message key and data
     */
    @ExceptionHandler(BusinessRuntimeException.class)
    protected ResponseEntity<Response<?>> handleBusinessRuntimeException(BusinessRuntimeException bre) {
        EXC_LOGGER.error(P_PRESENTATION_LAYER_EXCEPTION, bre.getLocalizedMessage(), bre);
        ResponseStatus annotation = bre.getClass().getAnnotation(ResponseStatus.class);
        HttpStatus status = annotation != null ? annotation.value() : HttpStatus.INTERNAL_SERVER_ERROR;
        return new ResponseEntity<>(Response.newBuilder()
                .withMessage(bre.getMessage())
                .withMessageKey(bre.getMessageKey())
                .withHttpStatus(String.valueOf(status.value()))
                .withObj(bre.getData())
                .build(),
                status
        );
    }

    /**
     * Map a {@link HttpBusinessException} onto a response with the HTTP status the exception carries.
     *
     * @param hbe The exception to map
     * @return A ResponseEntity with the exception's status and message
     */
    @ExceptionHandler(HttpBusinessException.class)
    protected ResponseEntity<Response<?>> handleHttpBusinessException(HttpBusinessException hbe) {
        EXC_LOGGER.error(P_PRESENTATION_LAYER_EXCEPTION, hbe.getLocalizedMessage(), hbe);
        return new ResponseEntity<>(Response.newBuilder()
                .withMessage(hbe.getMessage())
                .withHttpStatus(String.valueOf(hbe.getHttpStatus().value()))
                .build(),
                hbe.getHttpStatus()
        );
    }

    /**
     * Map a {@link TechnicalRuntimeException} onto a response. If the exception's cause is one of the known business exception types the
     * mapping is delegated to the corresponding handler method, otherwise the response has HTTP status {@code 502 BAD_GATEWAY}.
     *
     * @param tre The exception to map
     * @return A ResponseEntity with the resolved status, message, message key and data
     */
    @ExceptionHandler(TechnicalRuntimeException.class)
    protected ResponseEntity<Response<?>> handleTechnicalRuntimeException(TechnicalRuntimeException tre) {
        return switch (tre.getCause()) {
            case BehaviorAwareException bae -> handleBehaviorAwareException(bae);
            case BusinessRuntimeException bre -> handleBusinessRuntimeException(bre);
            case HttpBusinessException hbe -> handleHttpBusinessException(hbe);
            case null, default -> {
                EXC_LOGGER.error(P_PRESENTATION_LAYER_EXCEPTION, tre.getLocalizedMessage(), tre);
                yield new ResponseEntity<>(Response.newBuilder()
                        .withMessage(tre.getMessage())
                        .withMessageKey(tre.getMessageKey())
                        .withHttpStatus(String.valueOf(HttpStatus.BAD_GATEWAY.value()))
                        .withObj(tre.getData())
                        .build(),
                        HttpStatus.BAD_GATEWAY
                );
            }
        };
    }

    /**
     * Map an {@link IllegalArgumentException} onto a response with HTTP status {@code 400 BAD_REQUEST}.
     *
     * @param ex The exception to map
     * @return A ResponseEntity with status {@code 400 BAD_REQUEST}
     */
    @ExceptionHandler({IllegalArgumentException.class})
    public ResponseEntity<Response<?>> illegalArgumentException(IllegalArgumentException ex) {
        EXC_LOGGER.error(P_PRESENTATION_LAYER_EXCEPTION, ex.getLocalizedMessage(), ex);
        return new ResponseEntity<>(
                Response.newBuilder()
                        .withMessage(ex.getMessage())
                        .withMessageKey(ExceptionCodes.INVALID_PARAMETER_ERROR)
                        .withHttpStatus(String.valueOf(HttpStatus.BAD_REQUEST.value()))
                        .build(),
                HttpStatus.BAD_REQUEST
        );
    }

    /**
     * Map validation exceptions onto a response with HTTP status {@code 400 BAD_REQUEST} and a translated validation error message.
     *
     * @param e The exception to map
     * @return A ResponseEntity with status {@code 400 BAD_REQUEST}
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, ValidationException.class})
    protected ResponseEntity<Response<?>> handleValidationException(Exception e) {
        EXC_LOGGER.error(P_PRESENTATION_LAYER_EXCEPTION, e.getLocalizedMessage(), e);
        return new ResponseEntity<>(
                Response.newBuilder()
                        .withMessage(translate(ExceptionCodes.VALIDATION_ERROR))
                        .withMessageKey(ExceptionCodes.VALIDATION_ERROR)
                        .withHttpStatus(String.valueOf(HttpStatus.BAD_REQUEST.value()))
                        .build(),
                HttpStatus.BAD_REQUEST
        );
    }

    /**
     * Map a {@link ConstraintViolationException} onto a response with HTTP status {@code 400 BAD_REQUEST} and a translated validation
     * error message that contains the violated property paths.
     *
     * @param ex The exception to map
     * @return A ResponseEntity with status {@code 400 BAD_REQUEST}
     */
    @ExceptionHandler(ConstraintViolationException.class)
    protected ResponseEntity<Response<?>> handleConstraintViolationException(ConstraintViolationException ex) {
        var properties = ex.getConstraintViolations().stream().map(c -> c.getPropertyPath().toString()).toList();
        EXC_LOGGER.error(P_PRESENTATION_LAYER_EXCEPTION, ex.getLocalizedMessage(), ex);
        return new ResponseEntity<>(
                Response.newBuilder()
                        .withMessage(translate(ExceptionCodes.VALIDATION_ERROR, properties))
                        .withMessageKey(ExceptionCodes.VALIDATION_ERROR)
                        .withHttpStatus(String.valueOf(HttpStatus.BAD_REQUEST.value()))
                        .build(),
                HttpStatus.BAD_REQUEST
        );
    }

    /**
     * Fallback handler that maps any other {@link Exception} onto a response with HTTP status {@code 500 INTERNAL_SERVER_ERROR}.
     *
     * @param ex The exception to map
     * @return A ResponseEntity with status {@code 500 INTERNAL_SERVER_ERROR}
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<Response<?>> handleException(Exception ex) {
        EXC_LOGGER.error(P_PRESENTATION_LAYER_EXCEPTION, ex.getLocalizedMessage(), ex);
        return new ResponseEntity<>(Response.newBuilder()
                .withMessage(ex.getMessage())
                .withMessageKey(ExceptionCodes.TECHNICAL_RT_ERROR)
                .withHttpStatus(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()))
                .build(),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    /**
     * Translate the given message {@code key} into a message text using the configured {@code MessageSource} and the default locale.
     *
     * @param key The error code to search message text for
     * @param objects Any arguments that are passed into the message text
     * @return The translated message text
     */
    protected String translate(String key, Object... objects) {
        return messageSource.getMessage(key, objects, Locale.getDefault());
    }

    /**
     * Build a response object that signals a not-okay response with a given status {@code code}.
     *
     * @param <T> Some type extending the AbstractBase entity
     * @param code The status code to set as response code
     * @param msg The error message passed to the caller
     * @param msgKey The error message key passed to the caller
     * @param params A set of Serializable objects that are passed to the caller
     * @return A ResponseEntity with status {@code code}
     */
    protected <T extends Serializable> ResponseEntity<Response<T>> buildResponse(HttpStatus code, String msg, String msgKey, T... params) {
        Response result = Response.newBuilder().withMessage(msg).withMessageKey(msgKey).withHttpStatus(code.toString()).withObj(params).build();
        return new ResponseEntity<>(result, code);
    }

    /**
     * Build a response object that signals a not-okay response with a given status {@code code} and with given http headers.
     *
     * @param <T> Some type extending the AbstractBase entity
     * @param code The status code to set as response code
     * @param msg The error message passed to the caller
     * @param msgKey The error message key passed to the caller
     * @param headers The map of headers.
     * @param params A set of Serializable objects that are passed to the caller
     * @return A ResponseEntity with status {@code code}
     */
    protected <T extends Serializable> ResponseEntity<Response<T>> buildResponse(HttpStatus code, String msg, String msgKey, MultiValueMap<String, String> headers, T... params) {
        Response result = Response.newBuilder().withMessage(msg).withMessageKey(msgKey).withHttpStatus(code.toString()).withObj(params).build();
        return new ResponseEntity<>(result, headers, code);
    }

    /**
     * Build an response object that signals a success response to the caller.
     *
     * @param <T> Some type extending the AbstractBase entity
     * @param params A set of Serializable objects that are passed to the caller
     * @return A ResponseEntity with status {@link HttpStatus#OK}
     */
    protected <T extends Serializable> ResponseEntity<Response<T>> buildOKResponse(T... params) {
        return buildResponse(HttpStatus.OK, "", "", params);
    }

    /**
     * Build a response object that signals a not-okay response with a given status {@code code}.
     *
     * @param <T> Some type extending the AbstractBase entity
     * @param code The status code to set as response code
     * @param msg The error message passed to the caller
     * @param msgKey The error message key passed to the caller
     * @param params A set of Serializable objects that are passed to the caller
     * @return A ResponseEntity with status {@code code}
     */
    protected <T extends Serializable> ResponseEntity<Response<T>> buildNOKResponseWithKey(HttpStatus code, String msg, String msgKey, T... params) {
        return buildResponse(code, msg, msgKey, params);
    }

    /**
     * Build a response object that signals a not-okay response with a given status {@code code}.
     *
     * @param <T> Some type extending the AbstractBase entity
     * @param code The status code to set as response code
     * @param msg The error message passed to the caller
     * @param params A set of Serializable objects that are passed to the caller
     * @return A ResponseEntity with status {@code code}
     */
    protected <T extends Serializable> ResponseEntity<Response<T>> buildNOKResponse(HttpStatus code, String msg, T... params) {
        return buildResponse(code, msg, "", params);
    }

    /**
     * Append the ID of the object that was created to the original request URL and return it.
     *
     * @param req The HttpServletRequest object
     * @param objId The ID to append
     * @return The complete appended URL as String
     */
    protected String getLocationForCreatedResource(HttpServletRequest req, String objId) {
        return getLocationURIForCreatedResource(req, objId).toASCIIString();
    }

    /**
     * Append the ID of the object that was created to the original request URL and return it.
     *
     * @param req The HttpServletRequest object
     * @param objId The ID to append
     * @return The complete appended URL
     */
    protected URI getLocationURIForCreatedResource(HttpServletRequest req, String objId) {
        var protos = req.getHeader("x-forwarded-proto");
        var hosts = req.getHeader("x-forwarded-host");
        var url = new StringBuilder();
        if (protos != null && hosts != null) {
            var host = resolveFirstHost(hosts);
            var proto = resolveFirstProtocol(protos);
            url.append(proto).append(host);
        } else {
            url.append(req.getScheme().endsWith("://") ? req.getScheme() : req.getScheme() + "://")
                    .append(req.getServerName()).append(":")
                    .append(req.getServerPort());
        }
        var prefix = req.getHeader("x-forwarded-prefix");
        if (prefix != null) {
            url.append(prefix);
        }
        url.append(req.getRequestURI());
        url.append("/{objId}");
        return new UriTemplate(url.toString()).expand(objId);
    }

    private String resolveFirstHost(String hosts) {
        var parts = hosts.split(",");
        return parts.length == 1 ? hosts : parts[0];
    }

    private String resolveFirstProtocol(String protocols) {
        var parts = protocols.split(",");
        if (parts.length == 1) {
            return protocols.endsWith("://") ? protocols : protocols + "://";
        } else {
            return parts[0].endsWith("://") ? parts[0] : parts[0] + "://";
        }
    }
}
