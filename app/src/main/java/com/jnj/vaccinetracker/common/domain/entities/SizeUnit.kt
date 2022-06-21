package com.jnj.vaccinetracker.common.domain.entities

import kotlin.math.roundToLong

enum class SizeUnit {
    B {
        override fun toB(d: Double): Double {
            return d
        }

        override fun toKB(d: Double): Double {
            return d / (C1 / C0)
        }

        override fun toMB(d: Double): Double {
            return d / (C2 / C0)
        }

        override fun toGB(d: Double): Double {
            return d / (C3 / C0)
        }

        override fun convert(d: Double, u: SizeUnit): Double {
            return u.toB(d)
        }
    },
    KB {
        override fun toB(d: Double): Double {
            return x(d, C1 / C0, MAX / (C1 / C0))
        }

        override fun toKB(d: Double): Double {
            return d
        }

        override fun toMB(d: Double): Double {
            return d / (C2 / C1)
        }

        override fun toGB(d: Double): Double {
            return d / (C3 / C1)
        }

        override fun convert(d: Double, u: SizeUnit): Double {
            return u.toKB(d)
        }
    },
    MB {
        override fun toB(d: Double): Double {
            return x(d, C2 / C0, MAX / (C2 / C0))
        }

        override fun toKB(d: Double): Double {
            return x(d, C2 / C1, MAX / (C2 / C1))
        }

        override fun toMB(d: Double): Double {
            return d
        }

        override fun toGB(d: Double): Double {
            return d / (C3 / C2)
        }

        override fun convert(d: Double, u: SizeUnit): Double {
            return u.toMB(d)
        }
    },
    GB {
        override fun toB(d: Double): Double {
            return x(d, C3 / C0, MAX / (C3 / C0))
        }

        override fun toKB(d: Double): Double {
            return x(d, C3 / C1, MAX / (C3 / C1))
        }

        override fun toMB(d: Double): Double {
            return x(d, C3 / C2, MAX / (C3 / C2))
        }

        override fun toGB(d: Double): Double {
            return d
        }

        override fun convert(d: Double, u: SizeUnit): Double {
            return u.toGB(d)
        }
    };

    val label: String
        get() = name

    open fun toB(d: Double): Double {
        throw AbstractMethodError()
    }

    open fun toKB(d: Double): Double {
        throw AbstractMethodError()
    }

    open fun toMB(d: Double): Double {
        throw AbstractMethodError()
    }

    open fun toGB(d: Double): Double {
        throw AbstractMethodError()
    }

    open fun convert(d: Double, u: SizeUnit): Double {
        throw AbstractMethodError()
    }

    /**
     *
     * @param value
     * @return bytes
     */
    fun getSize(value: Double): Long {
        return SizeUnit.B.convert(value, this).roundToLong()
    }

    companion object {

        internal val MAX = java.lang.Double.MAX_VALUE

        internal val C0 = 1.0
        internal val C1 = C0 * 1024.0
        internal val C2 = C1 * 1024.0
        internal val C3 = C2 * 1024.0

        val setableUnits: Array<SizeUnit>
            get() = arrayOf(KB, MB, GB)

        /**
         * Scale d by m, checking for overflow. This has a short name to make above
         * code more readable.
         */
        internal fun x(d: Double, m: Double, over: Double): Double {
            if (d > over)
                return Double.MAX_VALUE
            return if (d < -over) Double.MIN_VALUE else d * m
        }
    }

}