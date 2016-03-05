package j8spec;

import j8spec.annotation.DefinedOrder;
import j8spec.annotation.RandomOrder;

import java.util.LinkedList;
import java.util.List;

import static j8spec.BlockExecutionFlag.DEFAULT;

final class ExampleGroupDefinition implements BlockDefinition {

    private final ExampleGroupConfiguration config;
    private final Context<ExampleGroupDefinition> context;
    private final List<BlockDefinition> blockDefinitions = new LinkedList<>();
    private final List<BlockDefinition> hooks = new LinkedList<>();

    static ExampleGroupDefinition newExampleGroupDefinition(
        Class<?> specClass,
        Context<ExampleGroupDefinition> context
    ) {
        ExampleGroupConfiguration.Builder configBuilder = new ExampleGroupConfiguration.Builder()
            .description(specClass.getName())
            .executionFlag(DEFAULT);

        configureExecutionOrder(specClass, configBuilder);

        ExampleGroupDefinition group = new ExampleGroupDefinition(configBuilder.build(), context);
        context.switchTo(group);

        try {
            specClass.newInstance();
        } catch (J8SpecException e) {
            throw e;
        } catch (Exception e) {
            throw new SpecInitializationException("Failed to create instance of " + specClass + ".", e);
        }

        return group;
    }

    private static void configureExecutionOrder(Class<?> specClass, ExampleGroupConfiguration.Builder configBuilder) {
        if (specClass.isAnnotationPresent(DefinedOrder.class)) {
            configBuilder.definedOrder();
        } else {
            configBuilder.randomOrder();
            if (specClass.isAnnotationPresent(RandomOrder.class)) {
                configBuilder.seed(specClass.getAnnotation(RandomOrder.class).seed());
            }
        }
    }

    private ExampleGroupDefinition(ExampleGroupConfiguration config, Context<ExampleGroupDefinition> context) {
        this.config = config;
        this.context = context;
    }

    void addGroup(ExampleGroupConfiguration config, SafeBlock block) {
        ExampleGroupDefinition exampleGroupDefinition = new ExampleGroupDefinition(config, context);

        blockDefinitions.add(exampleGroupDefinition);

        context.switchTo(exampleGroupDefinition);
        block.execute();
        context.restore();
    }

    void addBeforeAll(UnsafeBlock beforeAllBlock) {
        hooks.add(new BeforeAllDefinition(beforeAllBlock));
    }

    void addBeforeEach(UnsafeBlock beforeEachBlock) {
        hooks.add(new BeforeEachDefinition(beforeEachBlock));
    }

    void addExample(ExampleConfiguration exampleConfig, UnsafeBlock block) {
        blockDefinitions.add(new ExampleDefinition(exampleConfig, block));
    }

    @Override
    public void accept(BlockDefinitionVisitor visitor) {
        visitor.startGroup(config);

        for (BlockDefinition blockDefinition : hooks) {
            blockDefinition.accept(visitor);
        }

        for (BlockDefinition blockDefinition : blockDefinitions) {
            blockDefinition.accept(visitor);
        }

        visitor.endGroup();
    }
}
