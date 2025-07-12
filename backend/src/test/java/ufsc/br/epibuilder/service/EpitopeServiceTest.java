package ufsc.br.epibuilder.service;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ufsc.br.epibuilder.model.ActionType;
import ufsc.br.epibuilder.model.Epitope;
import ufsc.br.epibuilder.model.EpitopeTopology;
import ufsc.br.epibuilder.model.Method;
import ufsc.br.epibuilder.model.User;
import ufsc.br.epibuilder.repository.EpitopeRepository;
import ufsc.br.epibuilder.repository.EpitopeTaskDataRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.extension.ExtendWith;
import ufsc.br.epibuilder.model.EpitopeTaskData;

@ExtendWith(MockitoExtension.class)
public class EpitopeServiceTest {

    @Mock
    private EpitopeTaskDataRepository taskDataRepository;

    @Mock
    private EpitopeRepository epitopeRepository;

    @InjectMocks
    private EpitopeService epitopeService;

    @Test
    void testSaveEpitopeWithTopology() {
        User user = new User();
        user.setId(1L);

        EpitopeTaskData taskData = new EpitopeTaskData();
        taskData.setAbsolutePath("/caminho/para/o/arquivo");
        taskData.setRunName("Teste1");
        taskData.setActionType(ActionType.DEFAULT);
        taskData.setUser(user);

        EpitopeTopology topology = new EpitopeTopology();
        topology.setMethod(Method.BEPIPRED);
        topology.setAvgScore(0.9);
        topology.setCover(0.85);
        topology.setThreshold(0.5);
        topology.setTopologyData("BepiPred topology data");

        Epitope epitope = new Epitope();
        epitope.setEpitopeId("EPI_123");
        epitope.setEpitope("PEPTIDE");
        epitope.setStart(1);
        epitope.setEnd(7);
        epitope.setLength(7);
        epitope.setScore(0.85);
        epitope.setEpitopeTaskData(taskData);
        epitope.setEpitopeTopologies(List.of(topology));
        topology.setEpitope(epitope);

        when(epitopeRepository.save(any(Epitope.class))).thenAnswer(invocation -> {
            Epitope e = invocation.getArgument(0);
            e.setId(1L);
            return e;
        });

        Epitope savedEpitope = epitopeService.save(epitope);

        assertNotNull(savedEpitope.getId());
        assertNotNull(savedEpitope.getEpitopeTopologies());
        assertEquals(1, savedEpitope.getEpitopeTopologies().size());

        EpitopeTopology savedTopology = savedEpitope.getEpitopeTopologies().get(0);
        assertEquals(Method.BEPIPRED, savedTopology.getMethod());

        verify(epitopeRepository).save(any(Epitope.class));
    }
}