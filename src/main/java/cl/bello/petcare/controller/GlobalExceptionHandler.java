package cl.bello.petcare.controller;

import cl.bello.petcare.dto.ApiError;
import cl.bello.petcare.exception.ConflictoException;
import cl.bello.petcare.exception.RecursoNoEncontradoException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(RecursoNoEncontradoException.class)
	public ResponseEntity<ApiError> noEncontrado(
			RecursoNoEncontradoException error,
			HttpServletRequest request) {
		return respuesta(HttpStatus.NOT_FOUND, error.getMessage(), request, List.of());
	}

	@ExceptionHandler({ConflictoException.class, DataIntegrityViolationException.class,
			PessimisticLockingFailureException.class, OptimisticLockingFailureException.class})
	public ResponseEntity<ApiError> conflicto(Exception error, HttpServletRequest request) {
		String mensaje = error instanceof ConflictoException
				? error.getMessage()
				: "La operación entra en conflicto con el estado actual de los datos";
		return respuesta(HttpStatus.CONFLICT, mensaje, request, List.of());
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
			MethodArgumentNotValidException error,
			HttpHeaders headers,
			HttpStatusCode status,
			WebRequest request) {
		List<String> detalles = error.getBindingResult().getFieldErrors().stream()
				.map(campo -> campo.getField() + ": " + campo.getDefaultMessage())
				.toList();
		ApiError body = crearError(status, "La solicitud contiene datos inválidos", ruta(request), detalles);
		return handleExceptionInternal(error, body, headers, status, request);
	}

	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(
			HttpMessageNotReadableException error,
			HttpHeaders headers,
			HttpStatusCode status,
			WebRequest request) {
		ApiError body = crearError(status, "El cuerpo de la solicitud no tiene un formato JSON válido",
				ruta(request), List.of());
		return handleExceptionInternal(error, body, headers, status, request);
	}

	@ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class})
	public ResponseEntity<ApiError> solicitudInvalida(Exception error, HttpServletRequest request) {
		return respuesta(HttpStatus.BAD_REQUEST, error.getMessage(), request, List.of());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> inesperado(Exception error, HttpServletRequest request) {
		logger.error("Error inesperado al procesar " + request.getRequestURI(), error);
		return respuesta(
				HttpStatus.INTERNAL_SERVER_ERROR,
				"Ocurrió un error interno al procesar la solicitud",
				request,
				List.of());
	}

	@Override
	protected ResponseEntity<Object> createResponseEntity(
			Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		ApiError error = body instanceof ApiError apiError
				? apiError
				: crearError(status, mensajeHttp(status), ruta(request), List.of());
		return new ResponseEntity<>(error, headers, status);
	}

	private String mensajeHttp(HttpStatusCode status) {
		return switch (status.value()) {
			case 400 -> "La solicitud contiene parámetros inválidos";
			case 404 -> "El recurso solicitado no existe";
			case 405 -> "El método HTTP no está permitido para este recurso";
			case 406 -> "El formato de respuesta solicitado no está disponible";
			case 415 -> "El tipo de contenido de la solicitud no está soportado";
			default -> status.is5xxServerError()
					? "Ocurrió un error interno al procesar la solicitud"
					: "La solicitud no pudo ser procesada";
		};
	}

	private String ruta(WebRequest request) {
		return ((ServletWebRequest) request).getRequest().getRequestURI();
	}

	private ResponseEntity<ApiError> respuesta(
			HttpStatus estado,
			String mensaje,
			HttpServletRequest request,
			List<String> detalles) {
		return ResponseEntity.status(estado).body(crearError(estado, mensaje, request.getRequestURI(), detalles));
	}

	private ApiError crearError(HttpStatusCode estado, String mensaje, String ruta, List<String> detalles) {
		HttpStatus codigo = HttpStatus.resolve(estado.value());
		return new ApiError(
				LocalDateTime.now(),
				estado.value(),
				codigo == null ? "HTTP " + estado.value() : codigo.getReasonPhrase(),
				mensaje,
				ruta,
				detalles);
	}
}

