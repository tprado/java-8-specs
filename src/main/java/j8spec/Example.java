package j8spec;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

/**
 * Example ready to be executed.
 * @since 3.0.0
 */
public final class Example implements UnsafeBlock, Comparable<Example> {

    private final List<String> containerDescriptions;
    private final String description;
    private final List<BeforeHook> beforeHooks;
    private final UnsafeBlock block;
    private final Class<? extends Throwable> expectedException;
    private final Rank rank;

    static Example newExample(
        List<String> containerDescriptions,
        String description,
        List<BeforeHook> beforeHooks,
        UnsafeBlock block,
        Rank rank
    ) {
        return new Example(containerDescriptions, description, beforeHooks, block, null, rank);
    }

    static Example newExample(
        List<String> containerDescriptions,
        String description,
        List<BeforeHook> beforeHooks,
        UnsafeBlock block,
        Class<? extends Throwable> expectedException,
        Rank rank
    ) {
        return new Example(containerDescriptions, description, beforeHooks, block, expectedException, rank);
    }

    static Example newIgnoredExample(List<String> containerDescriptions, String description, Rank rank) {
        return newExample(containerDescriptions, description, emptyList(), NOOP, rank);
    }

    private Example(
        List<String> containerDescriptions,
        String description,
        List<BeforeHook> beforeHooks,
        UnsafeBlock block,
        Class<? extends Throwable> expectedException,
        Rank rank
    ) {
        this.containerDescriptions = unmodifiableList(containerDescriptions);
        this.description = description;
        this.beforeHooks = unmodifiableList(beforeHooks);
        this.block = block;
        this.expectedException = expectedException;
        this.rank = rank;
    }

    @Override
    public int compareTo(Example block) {
        return rank.compareTo(block.rank);
    }

    /**
     * Runs this block and associated setup code.
     * @since 2.0.0
     */
    @Override
    public void tryToExecute() throws Throwable {
        for (BeforeHook beforeHook : beforeHooks) {
            beforeHook.tryToExecute();
        }
        block.tryToExecute();
    }

    /**
     * @return textual description
     * @since 2.0.0
     */
    public String description() {
        return description;
    }

    /**
     * @return textual description of all outer "describe" blocks
     * @since 2.0.0
     */
    public List<String> containerDescriptions() {
        return containerDescriptions;
    }

    /**
     * @return <code>true</code> if this block should be ignored, <code>false</code> otherwise
     * @since 2.0.0
     */
    public boolean shouldBeIgnored() {
        return block == NOOP;
    }

    /**
     * @return exception class this block is expected to throw, <code>null</code> otherwise
     * @see #isExpectedToThrowAnException()
     * @since 2.0.0
     */
    public Class<? extends Throwable> expected() {
        return expectedException;
    }

    /**
     * @return <code>true</code> if this block is expected to throw an exception, <code>false</code> otherwise
     * @see #expected()
     * @since 2.0.0
     */
    public boolean isExpectedToThrowAnException() {
        return expectedException != null;
    }
}
