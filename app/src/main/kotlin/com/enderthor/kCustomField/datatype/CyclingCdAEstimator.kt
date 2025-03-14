package com.enderthor.kCustomField.datatype

import kotlin.math.atan
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.exp

class CyclingCdAEstimator(
    private val gravity: Double = 9.80665,
    private val slope:  Double,
    private val totalMass: Double,
    private val rollingResistanceCoefficient: Double,
    private val speed:  Double,
    private val powerLoss: Double,
    private val elevation: Double,
    private val surface: Double,
    private val power: Double
) {


    fun estimatedCdA(): Double {

        val slopeAngle = atan(slope)
        val sinAngle = sin(slopeAngle)
        val cosAngle = cos(slopeAngle)

        val gravityForce = gravity * sinAngle * totalMass
        val rollingResistanceForce = gravity * cosAngle * totalMass * rollingResistanceCoefficient * surface
        val dynamicRollingResistanceForce = 0.1 * cosAngle

        val powerOutput = power * (1 - powerLoss)
        val aerodynamicDragForce = powerOutput / speed - gravityForce -
                rollingResistanceForce - dynamicRollingResistanceForce

        val airDensity = 1.225 * exp(-0.00011856 * elevation)

        if (airDensity <= 0.0001) {
            return 0.0
        }

        val speedSquared = speed * speed

        return aerodynamicDragForce / (0.5 * airDensity * speedSquared)
    }

}