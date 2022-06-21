package com.jnj.vaccinetracker.sync.p2p.domain.exceptions

import java.io.IOException

class SendMessageException(override val cause: Exception) : IOException()