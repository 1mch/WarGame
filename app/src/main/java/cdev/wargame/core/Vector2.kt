package cdev.wargame.core

import kotlin.math.sqrt

data class Vector2(var x: Float = 0f, var y: Float = 0f) {
    fun add(v: Vector2): Vector2 = Vector2(x + v.x, y + v.y)
    fun sub(v: Vector2): Vector2 = Vector2(x - v.x, y - v.y)
    fun mul(scalar: Float): Vector2 = Vector2(x * scalar, y * scalar)
    fun div(scalar: Float): Vector2 = if (scalar != 0f) Vector2(x / scalar, y / scalar) else Vector2(0f, 0f)
    
    fun magSq(): Float = x * x + y * y
    fun mag(): Float = sqrt(magSq())
    
    fun normalize(): Vector2 {
        val m = mag()
        return if (m > 0f) div(m) else Vector2(0f, 0f)
    }
    
    fun distanceTo(v: Vector2): Float = sub(v).mag()
    fun dot(v: Vector2): Float = x * v.x + y * v.y
    fun copy(): Vector2 = Vector2(x, y)
}
