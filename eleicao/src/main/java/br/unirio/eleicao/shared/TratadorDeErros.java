package br.unirio.eleicao.shared;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class TratadorDeErros {
    @ExceptionHandler(CredenciaisInvalidasException.class)
    ResponseEntity<ErroResposta> e401(RuntimeException e){ return resp(401, e); }
    @ExceptionHandler(NaoEncontradoException.class)
    ResponseEntity<ErroResposta> e404(RuntimeException e){ return resp(404, e); }
    @ExceptionHandler({ConflitoException.class, DataIntegrityViolationException.class})
    ResponseEntity<ErroResposta> e409(RuntimeException e){ return resp(409, e); }
    @ExceptionHandler(RegraDeNegocioException.class)
    ResponseEntity<ErroResposta> e422(RuntimeException e){ return resp(422, e); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErroResposta> e400(MethodArgumentNotValidException e){
        return ResponseEntity.badRequest().body(new ErroResposta(400,
            e.getBindingResult().getFieldErrors().stream()
             .map(f -> f.getField()+": "+f.getDefaultMessage()).collect(Collectors.joining("; "))));
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErroResposta> corpoInvalido(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(new ErroResposta(400, "Corpo da requisição inválido"));
    }
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErroResposta> parametroInvalido(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest().body(new ErroResposta(400, "Parâmetro inválido: " + e.getName()));
    }
    @ExceptionHandler(AccessDeniedException.class)   // lançada pelos serviços (ex.: CA vendo outro cargo)
    ResponseEntity<ErroResposta> e403(RuntimeException e){ return resp(403, e); }

    private ResponseEntity<ErroResposta> resp(int s, Exception e){
        return ResponseEntity.status(s).body(new ErroResposta(s, e.getMessage()));
    }
}