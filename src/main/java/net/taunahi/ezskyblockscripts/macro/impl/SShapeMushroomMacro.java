package net.taunahi.ezskyblockscripts.macro.impl;

import cc.polyfrost.oneconfig.libs.universal.UMath;
import net.taunahi.ezskyblockscripts.config.TaunahiConfig;
import net.taunahi.ezskyblockscripts.handler.GameStateHandler;
import net.taunahi.ezskyblockscripts.handler.MacroHandler;
import net.taunahi.ezskyblockscripts.macro.AbstractMacro;
import net.taunahi.ezskyblockscripts.util.*;
import net.taunahi.ezskyblockscripts.util.helper.Rotation;
import net.taunahi.ezskyblockscripts.util.helper.RotationConfiguration;
import net.minecraft.util.EnumFacing;

import java.util.Optional;

public class SShapeMushroomMacro extends AbstractMacro {

    @Override
    public void onEnable() {
        super.onEnable();
        if (!TaunahiConfig.customPitch && !isRestoredState()) {
            setPitch((float) (Math.random() * 2 - 1)); // -1 - 1
        }
        if (!TaunahiConfig.customYaw && !isRestoredState()) {
            setYaw(AngleUtils.getClosestDiagonal());
            setClosest90Deg(Optional.of(AngleUtils.getClosest(mc.thePlayer.rotationYaw)));
        }
        if (MacroHandler.getInstance().isTeleporting()) return;
        setRestoredState(false);
        getRotation().easeTo(
                new RotationConfiguration(
                        new Rotation(getYaw(), getPitch()),
                        TaunahiConfig.getRandomRotationTime(), null
                ).easeOutBack(true)
        );
    }

    @Override
    public void actionAfterTeleport() {
        setPitch((float) (Math.random() * 2 - 1)); // -1 - 1
    }

    @Override
    public void doAfterRewarpRotation() {
        setPitch((float) (Math.random() * 2 - 1)); // -1 - 1
        setClosest90Deg(Optional.of((float) UMath.wrapAngleTo180(AngleUtils.getClosest(AngleUtils.getClosest() + 180))));
        setYaw(AngleUtils.getClosestDiagonal(getYaw() + 180));
    }

    @Override
    public void updateState() {
        if (getCurrentState() == null)
            changeState(State.NONE);
        switch (getCurrentState()) {
            case LEFT:
                if (GameStateHandler.getInstance().isRightWalkable()) {
                    changeState(State.RIGHT);
                } else if (!GameStateHandler.getInstance().isLeftWalkable()) {
                    changeState(State.LEFT);
                } else {
                    LogUtils.sendDebug("No direction found");
                    changeState(calculateDirection());
                }
                break;
            case RIGHT:
                if (GameStateHandler.getInstance().isLeftWalkable()) {
                    changeState(State.LEFT);
                } else if (!GameStateHandler.getInstance().isRightWalkable()) {
                    changeState(State.RIGHT);
                } else {
                    LogUtils.sendDebug("No direction found");
                    changeState(calculateDirection());
                }
                break;
            case DROPPING: {
                LogUtils.sendDebug("On Ground: " + mc.thePlayer.onGround);
                if (mc.thePlayer.onGround && Math.abs(getLayerY() - mc.thePlayer.getPosition().getY()) > 1.5) {
                    if (TaunahiConfig.rotateAfterDrop && !getRotation().isRotating()) {
                        LogUtils.sendDebug("Rotating 180");
                        getRotation().reset();
                        setYaw(AngleUtils.getClosest(getYaw() + 180));
                        setClosest90Deg(Optional.of(AngleUtils.getClosest(getYaw())));
                        getRotation().easeTo(
                                new RotationConfiguration(
                                        new Rotation(getYaw(), getPitch()),
                                        (long) (400 + Math.random() * 300), null
                                )
                        );
                    }
                    KeyBindUtils.stopMovement();
                    setLayerY(mc.thePlayer.getPosition().getY());
                    changeState(State.NONE);
                } else {
                    GameStateHandler.getInstance().scheduleNotMoving();
                }
                break;
            }
            case NONE:
                changeState(calculateDirection());
                break;
        }
    }

    @Override
    public void invokeState() {
        if (getCurrentState() == null) return;
        switch (getCurrentState()) {
            case RIGHT: {
                KeyBindUtils.holdThese(
                        TaunahiConfig.alwaysHoldW ? mc.gameSettings.keyBindForward : mushroom45DegreeSide() == LookDirection.LEFT ? mc.gameSettings.keyBindRight : mc.gameSettings.keyBindForward,
                        mc.gameSettings.keyBindAttack
                );
                break;
            }
            case LEFT: {
                KeyBindUtils.holdThese(
                        TaunahiConfig.alwaysHoldW ? mc.gameSettings.keyBindForward : mushroom45DegreeSide() == LookDirection.LEFT ? mc.gameSettings.keyBindForward : mc.gameSettings.keyBindLeft,
                        mc.gameSettings.keyBindAttack
                );
                break;
            }
            case DROPPING:
                if (mc.thePlayer.onGround && Math.abs(getLayerY() - mc.thePlayer.getPosition().getY()) <= 1.5) {
                    LogUtils.sendDebug("Dropping done, but didn't drop high enough to rotate!");
                    setLayerY(mc.thePlayer.getPosition().getY());
                    changeState(State.NONE);
                }
                break;
            case NONE: {
                LogUtils.sendDebug("No direction found");
                break;
            }
        }
    }

    private enum LookDirection {
        LEFT, RIGHT
    }

    private LookDirection mushroom45DegreeSide() {
        if (!getClosest90Deg().isPresent()) return LookDirection.LEFT;
        EnumFacing facing = PlayerUtils.getHorizontalFacing(getClosest90Deg().get());
        double yaw180 = UMath.wrapAngleTo180(getYaw());
        float difference = (float) (yaw180 - 90f);
        switch (facing) {
            case EAST:
                if (difference < -220) {
                    return LookDirection.LEFT;
                } else if (difference > -140) {
                    return LookDirection.RIGHT;
                } else {
                    return LookDirection.LEFT;
                }
            case WEST:
                if (difference < 40) {
                    return LookDirection.LEFT;
                } else if (difference > 40) {
                    return LookDirection.RIGHT;
                } else {
                    return LookDirection.LEFT;
                }
            case NORTH:
                if (difference > 40) {
                    return LookDirection.LEFT;
                } else if (difference < -40) {
                    return LookDirection.RIGHT;
                } else {
                    return LookDirection.LEFT;
                }
            case SOUTH:
                if (difference < -130) {
                    return LookDirection.LEFT;
                } else if (difference < -40) {
                    return LookDirection.RIGHT;
                } else {
                    return LookDirection.LEFT;
                }
            default:
                throw new IllegalStateException("Unexpected value: " + facing);
        }
    }


    @Override
    public State calculateDirection() {
        if (BlockUtils.rightCropIsReady()) {
            return State.RIGHT;
        } else if (BlockUtils.leftCropIsReady()) {
            return State.LEFT;
        }

        for (int i = 1; i < 180; i++) {
            if (!BlockUtils.canWalkThrough(BlockUtils.getRelativeBlockPos(i, 0, 0, getClosest90Deg().orElse(AngleUtils.getClosest())))) {
                return State.LEFT;
            }
            if (!BlockUtils.canWalkThrough(BlockUtils.getRelativeBlockPos(-i, 0, 0, getClosest90Deg().orElse(AngleUtils.getClosest()))))
                return State.RIGHT;
        }
        LogUtils.sendDebug("No direction found");
        return State.NONE;
    }
}
