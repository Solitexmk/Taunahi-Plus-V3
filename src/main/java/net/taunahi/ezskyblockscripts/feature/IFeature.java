package net.taunahi.ezskyblockscripts.feature;

public interface IFeature {
    String getName();

    boolean isRunning();

    boolean shouldPauseMacroExecution();

    boolean shouldStartAtMacroStart();

    void start();

    void stop();

    void resetStatesAfterMacroDisabled();

    boolean isToggled();

    boolean shouldCheckForFailsafes();
}
