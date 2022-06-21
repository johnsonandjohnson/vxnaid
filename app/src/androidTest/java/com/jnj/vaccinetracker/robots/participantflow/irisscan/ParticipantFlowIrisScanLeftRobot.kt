package com.jnj.vaccinetracker.robots.participantflow.irisscan

import com.jnj.vaccinetracker.robots.participantflow.irisscan.base.ParticipantFlowIrisScanRobotBase

fun participantFlowIrisScanLeft(func: ParticipantFlowIrisScanLeftRobot.() -> Unit) = ParticipantFlowIrisScanLeftRobot().apply(func)

class ParticipantFlowIrisScanLeftRobot : ParticipantFlowIrisScanRobotBase()