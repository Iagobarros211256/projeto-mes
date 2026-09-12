package com.biscoitos.manutencao.ordemservico.service;

import com.biscoitos.manutencao.common.exception.DadosInvalidosException;
import com.biscoitos.manutencao.common.exception.EntidadeNaoEncontradaException;
import com.biscoitos.manutencao.core.domain.Equipamento;
import com.biscoitos.manutencao.core.domain.Usuario;
import com.biscoitos.manutencao.core.repository.EquipamentoRepository;
import com.biscoitos.manutencao.core.repository.UsuarioRepository;
import com.biscoitos.manutencao.core.service.AuditoriaService;
import com.biscoitos.manutencao.ordemservico.domain.OrdemServico;
import com.biscoitos.manutencao.ordemservico.domain.PrioridadeOrdemServico;
import com.biscoitos.manutencao.ordemservico.domain.StatusOrdemServico;
import com.biscoitos.manutencao.ordemservico.domain.TipoOrdemServico;
import com.biscoitos.manutencao.ordemservico.repository.OrdemServicoRepository;
import com.biscoitos.manutencao.ordemservico.service.exception.OrdemServicoEmEstadoTerminalException;
import com.biscoitos.manutencao.ordemservico.service.exception.OrdemServicoNaoEmAndamentoException;
import com.biscoitos.manutencao.ordemservico.service.exception.TecnicoNaoAtribuidoException;
import com.biscoitos.manutencao.ordemservico.web.dto.AbrirOrdemServicoRequest;
import com.biscoitos.manutencao.ordemservico.web.dto.ConcluirOrdemServicoRequest;
import com.biscoitos.manutencao.paradas.domain.Parada;
import com.biscoitos.manutencao.paradas.repository.ParadaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrdemServicoServiceTest {

    @Mock
    private OrdemServicoRepository ordemServicoRepository;
    @Mock
    private EquipamentoRepository equipamentoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ParadaRepository paradaRepository;
    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private OrdemServicoService ordemServicoService;

    @Test
    void deveRecusarAbrirSemEquipamentoNemParada() {
        AbrirOrdemServicoRequest request = new AbrirOrdemServicoRequest(
                null, null, "Título", "Descrição", TipoOrdemServico.CORRETIVA, PrioridadeOrdemServico.ALTA);

        assertThatThrownBy(() -> ordemServicoService.abrir(request, UUID.randomUUID()))
                .isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    void deveAbrirOsDiretoPorEquipamento() {
        UUID equipamentoId = UUID.randomUUID();
        UUID solicitanteId = UUID.randomUUID();

        when(equipamentoRepository.findById(equipamentoId))
                .thenReturn(Optional.of(Equipamento.builder().id(equipamentoId).nome("Forno 1").build()));
        when(usuarioRepository.findById(solicitanteId))
                .thenReturn(Optional.of(Usuario.builder().id(solicitanteId).nome("Supervisor").build()));
        when(ordemServicoRepository.save(any(OrdemServico.class))).thenAnswer(inv -> inv.getArgument(0));

        AbrirOrdemServicoRequest request = new AbrirOrdemServicoRequest(
                equipamentoId, null, "Correia rompida", null, TipoOrdemServico.CORRETIVA, PrioridadeOrdemServico.URGENTE);

        OrdemServico os = ordemServicoService.abrir(request, solicitanteId);

        assertThat(os.getStatus()).isEqualTo(StatusOrdemServico.ABERTA);
        assertThat(os.getEquipamento().getId()).isEqualTo(equipamentoId);
        assertThat(os.getDataAbertura()).isNotNull();
        verify(auditoriaService).registrar("ordem_servico", null, solicitanteId, "CREATE");
        // paradaRepository nunca deveria ser tocado nesse caminho (abertura sem parada de origem).
        verify(paradaRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void deveAbrirOsAPartirDeParadaEVincularOEquipamentoDaParada() {
        UUID paradaId = UUID.randomUUID();
        UUID equipamentoIdDaParada = UUID.randomUUID();
        UUID solicitanteId = UUID.randomUUID();

        Equipamento equipamentoDaParada = Equipamento.builder().id(equipamentoIdDaParada).nome("Misturadora").build();
        Parada parada = Parada.builder().id(paradaId).equipamento(equipamentoDaParada).build();

        when(paradaRepository.buscarComRelacionamentosPorId(paradaId)).thenReturn(Optional.of(parada));
        when(usuarioRepository.findById(solicitanteId))
                .thenReturn(Optional.of(Usuario.builder().id(solicitanteId).nome("Supervisor").build()));
        when(ordemServicoRepository.save(any(OrdemServico.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paradaRepository.save(any(Parada.class))).thenAnswer(inv -> inv.getArgument(0));

        // Note que equipamentoId NÃO é informado — precisa vir da parada.
        AbrirOrdemServicoRequest request = new AbrirOrdemServicoRequest(
                null, paradaId, "Investigar causa da parada", null,
                TipoOrdemServico.CORRETIVA, PrioridadeOrdemServico.ALTA);

        OrdemServico os = ordemServicoService.abrir(request, solicitanteId);

        assertThat(os.getEquipamento().getId()).isEqualTo(equipamentoIdDaParada);
        assertThat(parada.getOrdemServicoId()).isEqualTo(os.getId());
        verify(paradaRepository).save(parada);
    }

    @Test
    void deveRecusarIniciarExecucaoSemTecnicoAtribuido() {
        // RN05
        UUID osId = UUID.randomUUID();
        OrdemServico os = OrdemServico.builder().id(osId).status(StatusOrdemServico.ABERTA).build();

        when(ordemServicoRepository.buscarComRelacionamentosPorId(osId)).thenReturn(Optional.of(os));

        assertThatThrownBy(() -> ordemServicoService.iniciarExecucao(osId, UUID.randomUUID()))
                .isInstanceOf(TecnicoNaoAtribuidoException.class);
    }

    @Test
    void devePermitirIniciarExecucaoComTecnicoAtribuido() {
        UUID osId = UUID.randomUUID();
        Usuario tecnico = Usuario.builder().id(UUID.randomUUID()).nome("Técnico").build();
        OrdemServico os = OrdemServico.builder()
                .id(osId).status(StatusOrdemServico.ABERTA).tecnicoResponsavel(tecnico).build();

        when(ordemServicoRepository.buscarComRelacionamentosPorId(osId)).thenReturn(Optional.of(os));
        when(ordemServicoRepository.save(any(OrdemServico.class))).thenAnswer(inv -> inv.getArgument(0));

        OrdemServico iniciada = ordemServicoService.iniciarExecucao(osId, UUID.randomUUID());

        assertThat(iniciada.getStatus()).isEqualTo(StatusOrdemServico.EM_ANDAMENTO);
        assertThat(iniciada.getDataInicioExecucao()).isNotNull();
    }

    @Test
    void deveRecusarConcluirOsQueNaoEstaEmAndamento() {
        // RN06
        UUID osId = UUID.randomUUID();
        OrdemServico os = OrdemServico.builder().id(osId).status(StatusOrdemServico.ABERTA).build();

        when(ordemServicoRepository.buscarComRelacionamentosPorId(osId)).thenReturn(Optional.of(os));

        assertThatThrownBy(() -> ordemServicoService.concluir(
                osId, new ConcluirOrdemServicoRequest("feito"), UUID.randomUUID()))
                .isInstanceOf(OrdemServicoNaoEmAndamentoException.class);
    }

    @Test
    void deveConcluirOsEmAndamento() {
        UUID osId = UUID.randomUUID();
        OrdemServico os = OrdemServico.builder().id(osId).status(StatusOrdemServico.EM_ANDAMENTO).build();

        when(ordemServicoRepository.buscarComRelacionamentosPorId(osId)).thenReturn(Optional.of(os));
        when(ordemServicoRepository.save(any(OrdemServico.class))).thenAnswer(inv -> inv.getArgument(0));

        OrdemServico concluida = ordemServicoService.concluir(
                osId, new ConcluirOrdemServicoRequest("Rolamento trocado"), UUID.randomUUID());

        assertThat(concluida.getStatus()).isEqualTo(StatusOrdemServico.CONCLUIDA);
        assertThat(concluida.getObservacoesEncerramento()).isEqualTo("Rolamento trocado");
        assertThat(concluida.getDataConclusao()).isNotNull();
    }

    @Test
    void deveRecusarQualquerTransicaoEmOsJaConcluida() {
        // RN07 — testado via cancelar, mas a checagem é compartilhada por todas as transições.
        UUID osId = UUID.randomUUID();
        OrdemServico os = OrdemServico.builder().id(osId).status(StatusOrdemServico.CONCLUIDA).build();

        when(ordemServicoRepository.buscarComRelacionamentosPorId(osId)).thenReturn(Optional.of(os));

        assertThatThrownBy(() -> ordemServicoService.cancelar(osId, UUID.randomUUID()))
                .isInstanceOf(OrdemServicoEmEstadoTerminalException.class);
    }

    @Test
    void deveRecusarQualquerTransicaoEmOsJaCancelada() {
        UUID osId = UUID.randomUUID();
        OrdemServico os = OrdemServico.builder().id(osId).status(StatusOrdemServico.CANCELADA).build();

        when(ordemServicoRepository.buscarComRelacionamentosPorId(osId)).thenReturn(Optional.of(os));

        assertThatThrownBy(() -> ordemServicoService.atribuirTecnico(osId, UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(OrdemServicoEmEstadoTerminalException.class);
    }

    @Test
    void deveFalharAoAbrirComEquipamentoInexistente() {
        UUID equipamentoId = UUID.randomUUID();
        when(equipamentoRepository.findById(equipamentoId)).thenReturn(Optional.empty());

        AbrirOrdemServicoRequest request = new AbrirOrdemServicoRequest(
                equipamentoId, null, "Título", null, TipoOrdemServico.PREVENTIVA, PrioridadeOrdemServico.BAIXA);

        assertThatThrownBy(() -> ordemServicoService.abrir(request, UUID.randomUUID()))
                .isInstanceOf(EntidadeNaoEncontradaException.class);
    }
}
