package com.jnj.vaccinetracker.common.data.models

import com.neurotec.biometrics.NEPosition

/**
 * @author druelens
 * @version 1
 */
enum class IrisPosition(val position: Int) {
    RIGHT(1), LEFT(2);

    val nePosition: NEPosition
        get() = when (this) {
            RIGHT -> NEPosition.RIGHT
            LEFT -> NEPosition.LEFT
        }
}

