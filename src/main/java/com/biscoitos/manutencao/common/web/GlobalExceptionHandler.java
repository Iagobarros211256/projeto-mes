package com.biscoitos.manutencao.common.web;

import com.biscoitos.manutencao.common.exception.DadosInvalidosException;
import com.biscoitos.manutencao.common.exception.DuplicidadeException;
import com.biscoitos.manutencao.common.exception.EntidadeNaoEncontradaException;
import com.biscoitos.manutencao.common.exception.HorometroRetrocessoException;
import com.biscoitos.manutencao.estoque.service.exception.EstoqueInsuficienteException;
import com.biscoitos.manutencao.ordemservico.service.exception.OrdemServicoEmEstadoTerminalException;
import com.biscoitos.manutencao.ordemservico.service.exception.OrdemServicoNaoEmAndamentoException;
import com.biscoitos.manutencao.ordemservico.service.exception.TecnicoNaoAtribuidoException;
import com.biscoitos.manutencao.paradas.service.exception.IntervaloInvalidoException;
import com.biscoitos.manutencao.paradas.service.exception.ParadaEmAbertoException;
import com.biscoitos.manutencao.paradas.service.exception.ParadaJaEncerradaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntidadeNaoEncontradaException.class)
    public ResponseEntity<ErrorResponse> handleNaoEncontrado(EntidadeNaoEncontradaException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Não encontrado", ex.getMessage()));
    }

    @ExceptionHandler({
            ParadaEmAbertoException.class,
            DuplicidadeException.class,
            ParadaJaEncerradaException.class,
            // Sprint 3 — todas são "ação inválida pro estado atual da OS" (RN05-RN07),
            // mesma categoria semântica das exceptions de Parada acima.
            TecnicoNaoAtribuidoException.class,
            OrdemServicoNaoEmAndamentoException.class,
            OrdemServicoEmEstadoTerminalException.class,
            // Sprint 4 — RN09, mesma categoria: tentativa de ação inválida pro estado atual.
            HorometroRetrocessoException.class,
            // Sprint 5 — RN10, mesma categoria.
            EstoqueInsuficienteException.class
    })
    public ResponseEntity<ErrorResponse> handleConflito(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflito", ex.getMessage()));
    }

    @ExceptionHandler({IntervaloInvalidoException.class, DadosInvalidosException.class})
    public ResponseEntity<ErrorResponse> handleRequisicaoInvalida(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "Requisição inválida", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidacao(MethodArgumentNotValidException ex) {
        List<String> detalhes = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "Requisição inválida", "Um ou mais campos são inválidos", detalhes));
    }

    /**
     * Cobre, por exemplo, ?equipamentoId=abc (não é UUID) ou ?dataInicio=ontem (não é ISO-8601)
     * nos filtros da US08/US16. Sem isso, o Spring devolveria um 500 genérico em vez de um 400 claro.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTipoInvalido(MethodArgumentTypeMismatchException ex) {
        String mensagem = "Parâmetro '" + ex.getName() + "' com valor inválido: '" + ex.getValue() + "'";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "Requisição inválida", mensagem));
    }
}
