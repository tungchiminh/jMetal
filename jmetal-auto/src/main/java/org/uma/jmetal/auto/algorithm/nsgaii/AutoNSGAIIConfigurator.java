package org.uma.jmetal.auto.algorithm.nsgaii;

import org.uma.jmetal.auto.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.auto.component.initialsolutionscreation.InitialSolutionsCreation;
import org.uma.jmetal.auto.component.initialsolutionscreation.impl.LatinHypercubeSamplingSolutionsCreation;
import org.uma.jmetal.auto.component.initialsolutionscreation.impl.RandomSolutionsCreation;
import org.uma.jmetal.auto.component.initialsolutionscreation.impl.ScatterSearchSolutionsCreation;
import org.uma.jmetal.auto.component.evaluation.Evaluation;
import org.uma.jmetal.auto.component.evaluation.impl.SequentialEvaluation;
import org.uma.jmetal.auto.component.replacement.Replacement;
import org.uma.jmetal.auto.component.replacement.impl.RankingAndDensityEstimatorReplacement;
import org.uma.jmetal.auto.component.selection.MatingPoolSelection;
import org.uma.jmetal.auto.component.selection.impl.DifferentialEvolutionMatingPoolSelection;
import org.uma.jmetal.auto.component.selection.impl.NaryTournamentMatingPoolSelection;
import org.uma.jmetal.auto.component.selection.impl.RandomMatingPoolSelection;
import org.uma.jmetal.auto.component.termination.Termination;
import org.uma.jmetal.auto.component.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.auto.component.variation.impl.DifferentialCrossoverVariation;
import org.uma.jmetal.auto.irace.parameter.crossover.CrossoverType;
import org.uma.jmetal.auto.irace.parameter.mutation.MutationType;
import org.uma.jmetal.auto.irace.parameter.repairstrategy.RepairStrategyType;
import org.uma.jmetal.auto.irace.parameter.selection.SelectionType;
import org.uma.jmetal.auto.irace.parameter.variation.VariationType;
import org.uma.jmetal.auto.util.densityestimator.DensityEstimator;
import org.uma.jmetal.auto.util.densityestimator.impl.CrowdingDistanceDensityEstimator;
import org.uma.jmetal.auto.util.observer.impl.ExternalArchiveObserver;
import org.uma.jmetal.auto.util.ranking.Ranking;
import org.uma.jmetal.auto.util.ranking.impl.DominanceRanking;
import org.uma.jmetal.auto.component.variation.Variation;
import org.uma.jmetal.auto.component.variation.impl.CrossoverAndMutationVariation;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.crossover.impl.BLXAlphaCrossover;
import org.uma.jmetal.operator.crossover.impl.DifferentialEvolutionCrossover;
import org.uma.jmetal.operator.crossover.impl.SBXCrossover;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.mutation.impl.PolynomialMutation;
import org.uma.jmetal.operator.mutation.impl.SimpleRandomMutation;
import org.uma.jmetal.operator.mutation.impl.UniformMutation;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.solution.util.RepairDoubleSolution;
import org.uma.jmetal.solution.util.impl.RepairDoubleSolutionWithBoundValue;
import org.uma.jmetal.solution.util.impl.RepairDoubleSolutionWithOppositeBoundValue;
import org.uma.jmetal.solution.util.impl.RepairDoubleSolutionWithRandomValue;
import org.uma.jmetal.util.ProblemUtils;
import org.uma.jmetal.util.archive.impl.CrowdingDistanceArchive;
import org.uma.jmetal.util.comparator.DominanceComparator;
import org.uma.jmetal.util.comparator.MultiComparator;
import picocli.CommandLine.Option;

import java.util.Arrays;
import java.util.List;

enum AlgorithmResult {
  population,
  externalArchive
}

enum CreateInitialSolutionsStrategyType {
  random,
  scatterSearch,
  latinHypercubeSampling
}

public class AutoNSGAIIConfigurator {
  /* Fixed parameters */
  int sizeOfTheFinalPopulation = 100;

  @Option(
      names = {"--algorithmResult"},
      description = "Algorithm result - population vs archive (default: ${DEFAULT-VALUE})")
  private AlgorithmResult algorithmResult = AlgorithmResult.population;

  @Option(
      names = {"--populationSize"},
      description = "Population Size (default: ${DEFAULT-VALUE})")
  private int populationSize = 100;

  @Option(
      names = {"--populationSizeWithArchive"},
      description = "Population Size when an archive is used (default: ${DEFAULT-VALUE})")
  private int populationSizeWithArchive = 40;

  @Option(
      names = {"-p", "--problemName"},
      required = true,
      description = "problem name})")
  private String problemName;

  @Option(
      names = {"-rf", "--referenceFront"},
      required = true,
      description = "reference front")
  private String referenceParetoFront;

  /* Fixed components */
  Termination termination = new TerminationByEvaluations(25000);
  Evaluation<DoubleSolution> evaluation;
  Ranking<DoubleSolution> ranking = new DominanceRanking<>(new DominanceComparator<>());
  DensityEstimator<DoubleSolution> densityEstimator = new CrowdingDistanceDensityEstimator<>();
  MultiComparator<DoubleSolution> rankingAndCrowdingComparator =
      new MultiComparator<>(
          Arrays.asList(ranking.getSolutionComparator(), densityEstimator.getSolutionComparator()));

  Replacement<DoubleSolution> replacement =
      new RankingAndDensityEstimatorReplacement<>(ranking, densityEstimator);

  /* Configurable components*/
  /* Crossover */
  @Option(
      names = {"--crossover"},
      required = true,
      description = "Crossover: ${COMPLETION-CANDIDATES}")
  private CrossoverType crossoverType;

  @Option(
      names = {"--crossoverProbability"},
      description = "Crossover probability (default: ${DEFAULT-VALUE})")
  private double crossoverProbability = 0.9;

  @Option(
      names = {"--sbxCrossoverDistributionIndex"},
      description =
          "SBX crossover distribution index (default: ${DEFAULT-VALUE})")
  private double sbxCrossoverDistributionIndex = 0.20;

  @Option(
      names = {"--blxAlphaCrossoverAlphaValue"},
      description =
          "BLX-alpha crossover Alpha value (default: ${DEFAULT-VALUE})")
  private double blxAlphaCrossoverAlphaValue = 0.5;

  @Option(
      names = {"--crossoverRepairStrategy"},
      description = "Crossover repair strategy (default: ${DEFAULT-VALUE})")
  private RepairStrategyType crossoverRepairStrategy = RepairStrategyType.random;

  CrossoverOperator<DoubleSolution> crossover;

  /* Mutation */
  @Option(
      names = {"--mutation"},
      required = true,
      description = "Mutation: ${COMPLETION-CANDIDATES}")
  private MutationType mutationType;

  @Option(
      names = {"--mutationProbability"},
      description = "Mutation probability (default: ${DEFAULT-VALUE})")
  private double mutationProbability = 0.01;

  @Option(
      names = {"--polynomialMutationDistributionIndex"},
      description = "Polynomial Mutation Distribution Index (default: ${DEFAULT-VALUE})")
  private double polynomialMutationDistributionIndex = 0.20;

  @Option(
      names = {"--uniformMutationPerturbation"},
      description = "Uniform Mutation Perturbation (default: ${DEFAULT-VALUE})")
  private double uniformMutationPerturbation = 0.5;

  @Option(
      names = {"--mutationRepairStrategy"},
      description = "Mutation repair strategy (default: ${DEFAULT-VALUE})")
  private RepairStrategyType mutationRepairStrategy = RepairStrategyType.random;

  MutationOperator<DoubleSolution> mutation;

  @Option(
      names = {"--offspringPopulationSize"},
      description = "offspring population size (default: ${DEFAULT-VALUE})")
  private int offspringPopulationSize = populationSize;

  @Option(
      names = {"--createInitialSolutions"},
      required = true,
      description = "Create initial population: ${COMPLETION-CANDIDATES}")
  private CreateInitialSolutionsStrategyType createInitialSolutionsType =
      CreateInitialSolutionsStrategyType.random;

  InitialSolutionsCreation<DoubleSolution> createInitialSolutions;

  /* Variation */
  @Option(
      names = {"--variation"},
      required = true,
      description = "Variation: ${COMPLETION-CANDIDATES}")
  private VariationType variationType;

  @Option(
          names = {"--differentialEvolutionFValue"},
          description =
                  "Differential evolution F  value (default: ${DEFAULT-VALUE})")
  private double f = 0.5;

  @Option(
          names = {"--differentialEvolutionCRValue"},
          description =
                  "Differential evolution CR  value (default: ${DEFAULT-VALUE})")
  private double cr = 0.5;

  Variation<DoubleSolution> variation;

  /* Selection */
  @Option(
      names = {"--selection"},
      required = true,
      description = "Selection: ${COMPLETION-CANDIDATES}")
  private SelectionType selectionType;

  MatingPoolSelection<DoubleSolution> selection;

  @Option(
      names = {"--selectionTournamentSize"},
      description = "Selection: number of selected population")
  private int selectionTournamentSize = 2;

  public EvolutionaryAlgorithm<DoubleSolution> configureAndGetAlgorithm() {

    ExternalArchiveObserver<DoubleSolution> boundedArchiveObserver = null;

    DoubleProblem problem = getProblem();
    crossover = getCrossover();
    mutation = getMutation();
    createInitialSolutions = getCreateInitialSolutionStrategy();
    variation = getVariation();
    selection = getSelection();
    evaluation = new SequentialEvaluation<>(problem);

    if (algorithmResult.equals(AlgorithmResult.externalArchive)) {
      boundedArchiveObserver =
          new ExternalArchiveObserver<>(new CrowdingDistanceArchive<>(sizeOfTheFinalPopulation));
      evaluation.getObservable().register(boundedArchiveObserver);
      populationSize = populationSizeWithArchive;
    }

    class NSGAII extends EvolutionaryAlgorithm<DoubleSolution> {
      private ExternalArchiveObserver<DoubleSolution> archiveObserver;

      public NSGAII(
          String name,
          Evaluation<DoubleSolution> evaluation,
          InitialSolutionsCreation<DoubleSolution> createInitialSolutionList,
          Termination termination,
          MatingPoolSelection<DoubleSolution> selection,
          Variation<DoubleSolution> variation,
          Replacement<DoubleSolution> replacement,
          ExternalArchiveObserver<DoubleSolution> archiveObserver) {
        super(
            name,
            evaluation,
            createInitialSolutionList,
            termination,
            selection,
            variation,
            replacement);
        this.archiveObserver = archiveObserver;
      }

      @Override
      public List<DoubleSolution> getResult() {
        if (archiveObserver != null) {
          return archiveObserver.getArchive().getSolutionList();
        } else {
          return population;
        }
      }
    }

    NSGAII nsgaii =
        new NSGAII(
            "NSGAII",
            evaluation,
            createInitialSolutions,
            termination,
            selection,
            variation,
            replacement,
            boundedArchiveObserver);

    return nsgaii;
  }

  Variation<DoubleSolution> getVariation() {
    switch (variationType) {
      case crossoverAndMutationVariation:
        return new CrossoverAndMutationVariation<>(offspringPopulationSize, crossover, mutation);
      case DE:
        return new DifferentialCrossoverVariation(offspringPopulationSize, (DifferentialEvolutionCrossover)crossover, mutation);
      default:
        throw new RuntimeException(variationType + " is not a valid variation component");
    }
  }

  MatingPoolSelection<DoubleSolution> getSelection() {
    switch (selectionType) {
      case random:
        return new RandomMatingPoolSelection<>(variation.getMatingPoolSize());
      case tournament:
        return new NaryTournamentMatingPoolSelection<>(
            selectionTournamentSize, variation.getMatingPoolSize(), rankingAndCrowdingComparator);
      case DE:
        return new DifferentialEvolutionMatingPoolSelection(variation.getMatingPoolSize()) ;
      default:
        throw new RuntimeException(selectionType + " is not a valid selection operator");
    }
  }

  CrossoverOperator<DoubleSolution> getCrossover() {
    switch (crossoverType) {
      case SBX:
        return new SBXCrossover(
            crossoverProbability,
            sbxCrossoverDistributionIndex,
            getRepairDoubleSolutionStrategy(crossoverRepairStrategy));
      case BLX_ALPHA:
        return new BLXAlphaCrossover(
            crossoverProbability,
            blxAlphaCrossoverAlphaValue,
            getRepairDoubleSolutionStrategy(crossoverRepairStrategy));
      case DE:
        return new DifferentialEvolutionCrossover(cr, f, "rand/1/bin") ;
      default:
        throw new RuntimeException(crossoverType + " is not a valid crossover operator");
    }
  }

  MutationOperator<DoubleSolution> getMutation() {
    switch (mutationType) {
      case polynomial:
        return new PolynomialMutation(
            mutationProbability,
            polynomialMutationDistributionIndex,
            getRepairDoubleSolutionStrategy(mutationRepairStrategy));
      case uniform:
        return new UniformMutation(
            mutationProbability,
            uniformMutationPerturbation,
            getRepairDoubleSolutionStrategy(mutationRepairStrategy));
      case random:
        return new SimpleRandomMutation(mutationProbability) ;
      default:
        throw new RuntimeException(mutationType + " is not a valid mutation operator");
    }
  }

  RepairDoubleSolution getRepairDoubleSolutionStrategy(RepairStrategyType repairStrategyType) {
    switch (repairStrategyType) {
      case random:
        return new RepairDoubleSolutionWithRandomValue();
      case bounds:
        return new RepairDoubleSolutionWithBoundValue();
      case round:
        return new RepairDoubleSolutionWithOppositeBoundValue();
      default:
        throw new RuntimeException(
            repairStrategyType + " is not a valid repair double solution strategy");
    }
  }

  InitialSolutionsCreation<DoubleSolution> getCreateInitialSolutionStrategy() {
    switch (createInitialSolutionsType) {
      case random:
        return new RandomSolutionsCreation(getProblem(), populationSize);
      case scatterSearch:
        return new ScatterSearchSolutionsCreation(getProblem(), populationSize, 4);
      case latinHypercubeSampling:
        return new LatinHypercubeSamplingSolutionsCreation(getProblem(), populationSize);
      default:
        throw new RuntimeException(
            createInitialSolutions + " is not a valid initialization strategy");
    }
  }

  DoubleProblem getProblem() {
    return (DoubleProblem) ProblemUtils.<DoubleSolution>loadProblem(problemName);
  }

  String getReferenceParetoFront() {
    return "/pareto_fronts/" + referenceParetoFront;
  }
}
