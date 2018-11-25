package com.minecolonies.coremod.entity.ai.basic;

import com.minecolonies.api.util.Log;
import com.minecolonies.coremod.entity.ai.util.AIState;
import com.minecolonies.coremod.entity.ai.util.AITarget;
import net.minecraft.entity.ai.EntityAIBase;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Basic Statemachine with Target registration and execution
 */
public abstract class AbstractStateMachineAI extends EntityAIBase
{

    private static final int MUTEX_MASK = 3;

    @NotNull
    private final Map<AIState, ArrayList<AITarget>> targetMap = new HashMap<>();

    /**
     * The current state the ai is in.
     * Used to compare to state matching targets.
     */
    private AIState state;

    /**
     * Tick counter for the AI
     */
    private int tickCount = 0;

    /**
     * Sets up some important stuff for every ai.
     */
    protected AbstractStateMachineAI()
    {
        super();

        setMutexBits(MUTEX_MASK);
        this.state = AIState.INIT;
        this.targetMap.put(AIState.INIT, new ArrayList<>());
        this.targetMap.put(AIState.AI_BLOCKING_PRIO, new ArrayList<>());
        this.targetMap.put(AIState.STATE_BLOCKING_PRIO, new ArrayList<>());
        this.targetMap.put(AIState.EVENT, new ArrayList<>());
    }

    /**
     * Register one target.
     *
     * @param target the target to register.
     */
    private void registerTarget(final AITarget target)
    {
        if (!targetMap.containsKey(target.getState()))
        {
            final ArrayList<AITarget> newList = new ArrayList<>();
            newList.add(target);
            targetMap.put(target.getState(), newList);
        }
        else
        {
            targetMap.get(target.getState()).add(target);
        }
    }

    /**
     * Unregisters an AI Target
     */
    protected final void unRegisterTarget(final AITarget target)
    {
        final ArrayList<AITarget> temp = new ArrayList<>(targetMap.get(target.getState()));
        temp.remove(target);
        targetMap.put(target.getState(), temp);
    }

    /**
     * Register all targets your ai needs.
     * They will be checked in the order of registration,
     * so sort them accordingly.
     *
     * @param targets a number of targets that need registration
     */
    protected final void registerTargets(final AITarget... targets)
    {
        Arrays.asList(targets).forEach(this::registerTarget);
    }

    /**
     * Updates the task.
     */
    @Override
    public final void updateTask()
    {

        tickCount++;
        if (tickCount >= AITarget.MAX_AI_TICKRATE + AITarget.MAX_AI_TICKRATE_VARIANT)
        {
            tickCount = 1;
        }

        // Check targets in order by priority
        if (!targetMap.get(AIState.AI_BLOCKING_PRIO).stream().anyMatch(this::checkOnTarget)
              && !targetMap.get(AIState.EVENT).stream().anyMatch(this::checkOnTarget)
              && !targetMap.get(AIState.STATE_BLOCKING_PRIO).stream().anyMatch(this::checkOnTarget))
        {
            targetMap.get(state).stream().anyMatch(this::checkOnTarget);
        }
    }

    /**
     * Made final to preserve behaviour:
     * Sets a bitmask telling which other tasks may not run concurrently. The test is a simple bitwise AND - if it
     * yields zero, the two tasks may run concurrently, if not - they must run exclusively from each other.
     *
     * @param mutexBits the bits to flag this with.
     */
    @Override
    public final void setMutexBits(final int mutexBits)
    {
        super.setMutexBits(mutexBits);
    }

    /**
     * Checks on one target to see if it has to be executed.
     * It first checks for the state of the ai.
     * If that matches it tests the predicate if the ai
     * wants to run the target.
     * And if that's a yes, runs the target.
     * Tester and target are both error-checked
     * to prevent minecraft from crashing on bad ai.
     *
     * @param target the target to check
     * @return true if this target worked and we should stop executing this tick
     */
    private boolean checkOnTarget(@NotNull final AITarget target)
    {
        // Check if the target should be run this Tick
        if (((tickCount + target.getTickOffset()) % target.getTickRate()) != 0)
        {
            return false;
        }

        try
        {
            if (!target.test())
            {
                return false;
            }
        }
        catch (final RuntimeException e)
        {
            Log.getLogger().warn("Condition check for target " + target + " threw an exception:", e);
            this.onException(e);
            return false;
        }
        return applyTarget(target);
    }

    /**
     * Handle an exception higher up.
     *
     * @param e The exception to be handled.
     */
    protected void onException(final RuntimeException e)
    {
    }

    /**
     * Continuation of checkOnTarget.
     * applies the target and changes the state.
     * if the state is null, execute more targets
     * and don't change state.
     *
     * @param target the target.
     * @return true if it worked.
     */
    private boolean applyTarget(@NotNull final AITarget target)
    {
        final AIState newState;
        try
        {
            newState = target.apply();
        }
        catch (final RuntimeException e)
        {
            Log.getLogger().warn("Action for target " + target + " threw an exception:", e);
            this.onException(e);
            return false;
        }
        if (newState != null)
        {
            if (target.shouldUnregister())
            {
                unRegisterTarget(target);
            }
            state = newState;
            return true;
        }
        return false;
    }

    /**
     * Get the current state the ai is in.
     *
     * @return The current AIState.
     */
    public final AIState getState()
    {
        return state;
    }

    /**
     * Check if it is okay to eat by checking if the current target is good to eat.
     *
     * @return true if so.
     */
    public boolean isOkayToEat()
    {
        if (targetMap.get(state) == null)
        {
            return false;
        }
        return targetMap.get(state)
                 .stream()
                 .anyMatch(AITarget::isOkayToEat);
    }

}
