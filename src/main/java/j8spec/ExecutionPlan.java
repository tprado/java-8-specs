package j8spec;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static j8spec.BeforeBlock.newBeforeAllBlock;
import static j8spec.BeforeBlock.newBeforeEachBlock;
import static j8spec.ItBlock.newItBlock;
import static java.util.Collections.*;

public final class ExecutionPlan {

    private static final String LS = System.getProperty("line.separator");

    private final ExecutionPlan parent;
    private final String description;
    private final Runnable beforeAllBlock;
    private final Runnable beforeEachBlock;
    private final Map<String, Runnable> itBlocks;
    private final List<ExecutionPlan> plans = new LinkedList<>();
    private final Class<?> specClass;

    ExecutionPlan(
        Class<?> specClass,
        Runnable beforeAllBlock,
        Runnable beforeEachBlock,
        Map<String, Runnable> itBlocks
    ) {
        this.parent = null;
        this.specClass = specClass;
        this.description = specClass.getName();
        this.beforeAllBlock = beforeAllBlock;
        this.beforeEachBlock = beforeEachBlock;
        this.itBlocks = unmodifiableMap(itBlocks);
    }

    private ExecutionPlan(
        ExecutionPlan parent,
        String description,
        Runnable beforeAllBlock,
        Runnable beforeEachBlock,
        Map<String, Runnable> itBlocks
    ) {
        this.parent = parent;
        this.specClass = parent.specClass;
        this.description = description;
        this.beforeAllBlock = beforeAllBlock;
        this.beforeEachBlock = beforeEachBlock;
        this.itBlocks = unmodifiableMap(itBlocks);
    }

    ExecutionPlan newChildPlan(
        String description,
        Runnable beforeAllBlock,
        Runnable beforeEachBlock,
        Map<String, Runnable> itBlocks
    ) {
        ExecutionPlan plan = new ExecutionPlan(this, description, beforeAllBlock, beforeEachBlock, itBlocks);
        plans.add(plan);
        return plan;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        toString(sb, "");
        return sb.toString();
    }

    private void toString(StringBuilder sb, String indentation) {
        sb.append(indentation).append(description);

        for (Map.Entry<String, Runnable> behavior : itBlocks.entrySet()) {
            sb.append(LS).append(indentation).append("  ").append(behavior.getKey());
        }

        for (ExecutionPlan plan : plans) {
            sb.append(LS);
            plan.toString(sb, indentation + "  ");
        }
    }

    public Class<?> specClass() {
        return specClass;
    }

    public List<ItBlock> allItBlocks() {
        LinkedList<ItBlock> blocks = new LinkedList<>();
        collectItBlocks(blocks, new LinkedList<>());
        return blocks;
    }

    String description() {
        return description;
    }

    boolean hasItBlocks() {
        return !itBlocks.isEmpty();
    }

    List<ExecutionPlan> plans() {
        return new LinkedList<>(plans);
    }

    Runnable beforeAllBlock() {
        return beforeAllBlock;
    }

    Runnable beforeEachBlock() {
        return beforeEachBlock;
    }

    Runnable itBlock(String itBlockDescription) {
        return itBlocks.get(itBlockDescription);
    }

    private void collectItBlocks(List<ItBlock> blocks, List<BeforeBlock> parentBeforeAllBlocks) {
        if (this.beforeAllBlock != null) {
            parentBeforeAllBlocks.add(newBeforeAllBlock(this.beforeAllBlock));
        }

        List<BeforeBlock> beforeBlocks = new LinkedList<>();
        beforeBlocks.addAll(parentBeforeAllBlocks);
        beforeBlocks.addAll(collectBeforeEachBlocks());

        for (Map.Entry<String, Runnable> itBlock : itBlocks.entrySet()) {
            blocks.add(newItBlock(allContainerDescriptions(), itBlock.getKey(), beforeBlocks, itBlock.getValue()));
        }

        for (ExecutionPlan plan : plans) {
            plan.collectItBlocks(blocks, parentBeforeAllBlocks);
        }
    }

    private List<String> allContainerDescriptions() {
        List<String> containerDescriptions;

        if (isRootPlan()) {
            containerDescriptions = new LinkedList<>();
        } else {
            containerDescriptions = parent.allContainerDescriptions();
        }

        containerDescriptions.add(description);
        return containerDescriptions;
    }

    private List<BeforeBlock> collectBeforeEachBlocks() {
        List<BeforeBlock> beforeEachBlocks;

        if (isRootPlan()) {
            beforeEachBlocks = new LinkedList<>();
        } else {
            beforeEachBlocks = parent.collectBeforeEachBlocks();
        }

        if (beforeEachBlock != null) {
            beforeEachBlocks.add(newBeforeEachBlock(beforeEachBlock));
        }

        return beforeEachBlocks;
    }

    private boolean isRootPlan() {
        return parent == null;
    }
}
