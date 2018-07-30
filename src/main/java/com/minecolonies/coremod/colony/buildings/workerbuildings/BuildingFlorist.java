package com.minecolonies.coremod.colony.buildings.workerbuildings;

import com.minecolonies.coremod.colony.CitizenData;
import com.minecolonies.coremod.colony.Colony;
import com.minecolonies.coremod.colony.buildings.AbstractBuildingWorker;
import com.minecolonies.coremod.colony.jobs.AbstractJob;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

//Todo: implement this class
public class BuildingFlorist extends AbstractBuildingWorker
{
    /**
     * The abstract constructor of the building.
     *
     * @param c the colony
     * @param l the position
     */
    public BuildingFlorist(@NotNull final Colony c, final BlockPos l)
    {
        super(c, l);
    }

    @NotNull
    @Override
    public AbstractJob createJob(final CitizenData citizen)
    {
        return null;
    }

    @NotNull
    @Override
    public String getJobName()
    {
        return null;
    }

    @Override
    public String getSchematicName()
    {
        return null;
    }

    @Override
    public int getMaxBuildingLevel()
    {
        return 0;
    }
}