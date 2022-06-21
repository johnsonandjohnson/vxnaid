package com.jnj.vaccinetracker.robots.participantflow.irisscan

import com.jnj.vaccinetracker.robots.participantflow.irisscan.base.ParticipantFlowIrisScanRobotBase

fun participantFlowIrisScanRight(func: ParticipantFlowIrisScanRightRobot.() -> Unit) = ParticipantFlowIrisScanRightRobot().apply(func)

class ParticipantFlowIrisScanRightRobot : ParticipantFlowIrisScanRobotBase()