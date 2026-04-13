package com.postit.infrastructure.config;

import com.postit.shared.exception.EmailAlreadyExistsException;
import com.postit.shared.exception.PostitAccessDeniedException;
import com.postit.shared.exception.PostitNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PostitNotFoundException.class)
    public ProblemDetail handlePostitNotFoundException(PostitNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Post-it não encontrado");
        problemDetail.setType(URI.create("https://api.postits.local/errors/not-found"));
        return problemDetail;
    }

    @ExceptionHandler(PostitAccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handlePostitAccessDenied(
            PostitAccessDeniedException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        problem.setType(URI.create("https://api.postits.local/errors/forbidden"));
        problem.setTitle("Acesso negado");
        problem.setDetail("Você não tem permissão para acessar este recurso.");
        problem.setInstance(URI.create(request.getRequestURI()));
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ProblemDetail handleEmailAlreadyExistsException(EmailAlreadyExistsException ex) {
        // SEC-010: detail sanitizado — não revelar o email na resposta ao cliente.
        // A mensagem interna (ex.getMessage()) contém o email apenas para fins de logging interno.
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "Não foi possível completar o cadastro. Verifique os dados informados.");
        problemDetail.setTitle("Email já cadastrado");
        problemDetail.setType(URI.create("https://api.postits.local/errors/conflict"));
        return problemDetail;
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentialsException(BadCredentialsException ex) {
        // Mensagem genérica intencional — não revelar se o email existe ou não (prevenção de enumeração)
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, "Credenciais inválidas");
        problemDetail.setTitle("Não autorizado");
        problemDetail.setType(URI.create("https://api.postits.local/errors/unauthorized"));
        return problemDetail;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgumentException(IllegalArgumentException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Dados inválidos");
        problemDetail.setType(URI.create("https://api.postits.local/errors/bad-request"));
        return problemDetail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, details);
        problemDetail.setTitle("Erro de validação");
        problemDetail.setType(URI.create("https://api.postits.local/errors/validation"));
        return problemDetail;
    }
}
