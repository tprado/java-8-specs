package j8spec.junit;

import j8spec.ExecutionPlan;
import j8spec.ItBlock;
import j8spec.J8Spec;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.InitializationError;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static java.util.Collections.unmodifiableList;
import static org.junit.runner.Description.createTestDescription;

public final class J8SpecRunner extends ParentRunner<ItBlock> {

    private final ExecutionPlan executionPlan;
    private final Map<ItBlock, Description> descriptions = new HashMap<>();
    private List<ItBlock> itBlocks;

    public J8SpecRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
        try {
            executionPlan = J8Spec.executionPlanFor(testClass);
        } catch (Exception e) {
            throw new InitializationError(e);
        }
    }

    @Override
    protected List<ItBlock> getChildren() {
        if (itBlocks == null) {
            itBlocks = unmodifiableList(executionPlan.allItBlocks());
        }
        return itBlocks;
    }

    @Override
    protected Description describeChild(ItBlock itBlock) {
        if (!descriptions.containsKey(itBlock)) {
            descriptions.put(
                itBlock,
                createTestDescription(executionPlan.specClass().getName(), buildChildName(itBlock))
            );
        }
        return descriptions.get(itBlock);
    }

    private String buildChildName(ItBlock itBlock) {
        List<String> name = new LinkedList<>();
        List<String> containerDescriptions = itBlock.containerDescriptions();
        name.add(itBlock.description());
        for (int i = 1; i < containerDescriptions.size(); i++) {
            name.add(containerDescriptions.get(i));
        }
        return String.join(", ", name);
    }

    @Override
    protected void runChild(ItBlock itBlock, RunNotifier notifier) {
        Description description = describeChild(itBlock);

        if (itBlock.shouldBeIgnored()) {
            notifier.fireTestIgnored(description);
            return;
        }

        notifier.fireTestStarted(description);
        try {
            itBlock.run();
        } catch (Throwable t) {
            notifier.fireTestFailure(new Failure(description, t));
        }
        notifier.fireTestFinished(description);
    }
}
