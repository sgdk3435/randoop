package randoop.test;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import randoop.contract.ObjectContract;
import randoop.generation.ComponentManager;
import randoop.generation.ForwardGenerator;
import randoop.generation.SeedSequences;
import randoop.main.GenInputsAbstract;
import randoop.main.GenTests;
import randoop.main.OptionsCache;
import randoop.operation.ConcreteOperation;
import randoop.reflection.DefaultReflectionPredicate;
import randoop.reflection.ModelCollections;
import randoop.reflection.OperationExtractor;
import randoop.reflection.PublicVisibilityPredicate;
import randoop.reflection.ReflectionManager;
import randoop.reflection.TypedOperationManager;
import randoop.sequence.Sequence;
import randoop.sequence.SequenceExceptionError;
import randoop.test.treeadd.TreeAdd;
import randoop.test.treeadd.TreeNode;
import randoop.types.ConcreteType;
import randoop.util.MultiMap;
import randoop.util.ReflectionExecutor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ForwardExplorerTests2  {

  private static OptionsCache optionsCache;

   @BeforeClass
   public static void setup() {
     optionsCache = new OptionsCache();
     optionsCache.saveState();
   }

   @AfterClass
   public static void restore() {
     optionsCache.restoreState();
   }

  /**
   * The input scenario for this test results in the generation of a sequence
   * with repeated calls to a non-terminating method. If <code>--usethreads</code>
   * is set, the generator is not able to interrupt the executor, and will
   * never terminate.
   * Otherwise, a timeout exception will be thrown and the executor will throw an
   * exception, which since it is not the last statement is considered "flaky".
   */
  @Test
  public void test5() throws Exception {
    // means that timeout results in a flaky sequence exception
    GenInputsAbstract.repeat_heuristic = true;

    assert ReflectionExecutor.usethreads != false : "this test does not terminate if threads are not used";

    List<Class<?>> classes = new ArrayList<>();
    classes.add(TreeNode.class);
    classes.add(TreeAdd.class);

    System.out.println(classes);

    //SimpleExplorer exp = new SimpleExplorer(classes, Long.MAX_VALUE, 100);
    List<ConcreteOperation> model = getConcreteOperations(classes);
    assertTrue("model should not be empty", model.size() != 0);
    ComponentManager mgr = new ComponentManager(SeedSequences.defaultSeeds());
    ForwardGenerator exp = new ForwardGenerator(model, new LinkedHashSet<ConcreteOperation>(), Long.MAX_VALUE, 100, 100, mgr, null, null);
    exp.addTestCheckGenerator(createChecker(new LinkedHashSet<ObjectContract>()));

    // get a SequenceExceptionError when repeat_heuristic=true 
    try {
      exp.explore();
      //fail("expected timeout exception");
    } catch (SequenceExceptionError e) {
      assertEquals("should be timeout", e.getMessage(), "Exception thrown before end of sequence");
    }
    for (Sequence s : exp.getAllSequences()) {
      s.toCodeString();
    }
  }

  private static List<ConcreteOperation> getConcreteOperations(List<Class<?>> classes) {
    final List<ConcreteOperation> model = new ArrayList<>();
    TypedOperationManager operationManager = new TypedOperationManager(new ModelCollections() {
      @Override
      public void addConcreteOperation(ConcreteType declaringType, ConcreteOperation operation) {
        model.add(operation);
      }
    });
    ReflectionManager mgr = new ReflectionManager(new PublicVisibilityPredicate());
    mgr.add(new OperationExtractor(operationManager, new DefaultReflectionPredicate()));
    for (Class<?> c: classes) {
      mgr.apply(c);
    }
    return model;
  }

  private static TestCheckGenerator createChecker(Set<ObjectContract> contracts) {
    return (new GenTests()).createTestCheckGenerator(new PublicVisibilityPredicate(), contracts, new MultiMap<ConcreteType, ConcreteOperation>(), new LinkedHashSet<ConcreteOperation>());
  }
}
