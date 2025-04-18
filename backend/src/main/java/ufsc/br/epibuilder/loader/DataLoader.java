package ufsc.br.epibuilder.loader;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ufsc.br.epibuilder.model.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import java.time.LocalDateTime;

@Component
@Order(1) // Define a ordem de execução se houver múltiplos runners
public class DataLoader implements CommandLineRunner {

    @PersistenceContext
    private EntityManager entityManager;

    private final PasswordEncoder passwordEncoder;

    public DataLoader(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Verifica se o carregamento de dados está desativado por argumento
        if (shouldSkipDataLoading(args)) {
            System.out.println("DataLoader: Carregamento de dados inicial desativado por argumento");
            return;
        }

        // Verifica se já existem usuários no banco
        Long userCount = (Long) entityManager.createQuery("SELECT COUNT(u) FROM User u").getSingleResult();

        if (userCount == 0) {
            System.out.println("DataLoader: Iniciando carregamento de dados iniciais...");
            loadInitialData();
            System.out.println("DataLoader: Dados iniciais carregados com sucesso!");
        } else {
            System.out.println("DataLoader: Banco já contém dados. Nenhum dado inicial foi carregado.");
        }
    }

    private boolean shouldSkipDataLoading(String... args) {
        for (String arg : args) {
            if (arg.equalsIgnoreCase("--skipDataLoader")) {
                return true;
            }
        }
        return false;
    }

    private void loadInitialData() {
        // Criar usuários
        User admin = createUser("Administrador do Sistema", "admin@epibuilder.com", "admin@123", Role.ADMIN);
        User regularUser = createUser("Usuário de Teste", "user@epibuilder.com", "user@123", Role.USER);

        // Criar tarefas de epitopos
        createSampleTasks(regularUser);
    }

    private void createSampleTasks(User user) {
        // Tarefa 1 - Predição
        EpitopeTaskData predictionTask = createEpitopeTaskData(
                "C:/epitope_tasks",
                LocalDateTime.now(),
                LocalDateTime.now(),
                user,
                "run-covid-001",
                ActionType.PREDICT,
                0.95,
                8,
                20);

        // Tarefa 2 - Análise
        EpitopeTaskData analysisTask = createEpitopeTaskData(
                "C:/epitope_tasks",
                LocalDateTime.now(),
                LocalDateTime.now(),
                user,
                "run-flu-002",
                ActionType.ANALYSIS,
                0.85,
                6,
                15);

        // Adicionar epitopos à tarefa de predição
        addSampleEpitopes(predictionTask);
    }

    private void addSampleEpitopes(EpitopeTaskData task) {
        // Epitopo 1
        Epitope epitope1 = createEpitope(
                "SGFRKMAFPS",
                0.92,
                task,
                "EPI_001",
                23,
                32,
                10,
                -0.35,
                6.8,
                1124.5,
                0.78,
                1,
                0,
                0.65,
                0.72,
                0.68,
                0.81,
                0.88);

        // Epitopo 2
        Epitope epitope2 = createEpitope(
                "VKNKCVNFNF",
                0.87,
                task,
                "EPI_002",
                45,
                53,
                9,
                -0.42,
                5.9,
                998.7,
                0.82,
                0,
                0,
                0.71,
                0.65,
                0.73,
                0.76,
                0.84);

        // Adiciona múltiplas topologias para o epitopo1
        createEpitopeTopology(
                "Dados de topologia BepiPred para EPI_001",
                epitope1,
                Method.BEPIPRED,
                0.55,
                0.89,
                92.5);

        createEpitopeTopology(
                "Dados de topologia TMHMM para EPI_001",
                epitope1,
                Method.CHOU_FASMAN,
                0.60,
                0.82,
                85.0);

        // Adiciona múltiplas topologias para o epitopo2
        createEpitopeTopology(
                "Dados de topologia ChouFasman para EPI_002",
                epitope2,
                Method.EMINI,
                0.60,
                0.85,
                88.3);

        createEpitopeTopology(
                "Dados de topologia SignalP para EPI_002",
                epitope2,
                Method.HYDROPATHY,
                0.45,
                0.78,
                90.2);
    }

    private User createUser(String name, String username, String password, Role role) {
        User user = new User();
        user.setName(name);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        entityManager.persist(user);
        return user;
    }

    private EpitopeTaskData createEpitopeTaskData(
            String absolutePath,
            LocalDateTime executionDate,
            LocalDateTime finishedDate,
            User user,
            String runName,
            ActionType action,
            Double bepipredThreshold,
            Integer minEpitopeLength,
            Integer maxEpitopeLength) {

        // Verifica se o usuário já existe
        // Cria o status da tarefa (exemplo: "PENDING")
        TaskStatus taskStatus = new TaskStatus();
        taskStatus.setStatus(Status.RUNNING); // Ou outro status inicial
        entityManager.persist(taskStatus);

        EpitopeTaskData task = new EpitopeTaskData();
        task.setAbsolutePath(absolutePath);
        task.setExecutionDate(executionDate);
        task.setFinishedDate(finishedDate);
        task.setUser(user);
        task.setRunName(runName);
        task.setAction(action);
        task.setBepipredThreshold(bepipredThreshold);
        task.setMinEpitopeLength(minEpitopeLength);
        task.setMaxEpitopeLength(maxEpitopeLength);
        task.setTaskStatus(taskStatus); // Associa o status
        entityManager.persist(task);
        // Configura a relação bidirecional
        taskStatus.setEpitopeTaskData(task);
        entityManager.merge(taskStatus);
        return task;
    }

    private Epitope createEpitope(
            String sequence,
            Double score,
            EpitopeTaskData epitopeTaskData,
            String epitope,
            Integer start,
            Integer end,
            Integer length,
            Double hydropathy,
            Double isoelectricPoint,
            Double molecularWeight,
            Double parker,
            Integer nGlyc,
            Integer nGlycCount,
            Double karplusSchulz,
            Double kolaskar,
            Double chouFosman,
            Double emini,
            Double bepiPred3) {
        Epitope e = new Epitope();
        e.setSequence(sequence);
        e.setScore(score);
        e.setEpitopeTaskData(epitopeTaskData);
        e.setEpitope(epitope);
        e.setStart(start);
        e.setEnd(end);
        e.setLength(length);
        e.setHydropathy(hydropathy);
        e.setIsoelectricPoint(isoelectricPoint);
        e.setMolecularWeight(molecularWeight);
        e.setParker(parker);
        e.setNGlyc(nGlyc);
        e.setNGlycCount(nGlycCount);
        e.setKarplusSchulz(karplusSchulz);
        e.setKolaskar(kolaskar);
        e.setChouFosman(chouFosman);
        e.setEmini(emini);
        e.setBepiPred3(bepiPred3);
        entityManager.persist(e);
        return e;
    }

    private EpitopeTopology createEpitopeTopology(
            String topologyData,
            Epitope epitope,
            Method method,
            Double threshold,
            Double avgScore,
            Double cover) {
        EpitopeTopology topology = new EpitopeTopology();
        topology.setTopologyData(topologyData);
        topology.setEpitope(epitope);
        topology.setMethod(method);
        topology.setThreshold(threshold);
        topology.setAvgScore(avgScore);
        topology.setCover(cover);

        // Primeiro persiste a topologia
        entityManager.persist(topology);

        // Depois configura a relação bidirecional
        if (epitope.getEpitopeTopologies() == null) {
            epitope.setEpitopeTopologies(new ArrayList<>());
        }
        epitope.getEpitopeTopologies().add(topology);

        // Atualiza o epitopo no banco
        entityManager.merge(epitope);

        return topology;
    }
}