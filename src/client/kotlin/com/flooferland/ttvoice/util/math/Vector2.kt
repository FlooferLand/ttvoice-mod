package com.flooferland.ttvoice.util.math

sealed interface IVector<Vec> {
    operator fun plus(other: Vec): Vec
    operator fun minus(other: Vec): Vec
    operator fun times(other: Vec): Vec
    operator fun div(other: Vec): Vec
}

public class Vector2Int(val x: Int, val y: Int) : IVector<Vector2Int> {
    override fun plus(other: Vector2Int): Vector2Int {
        return Vector2Int(x + other.x, y + other.y)
    }
    override fun minus(other: Vector2Int): Vector2Int {
        return Vector2Int(x - other.x, y - other.y)
    }
    override fun times(other: Vector2Int): Vector2Int {
        return Vector2Int(x * other.x, y * other.y)
    }
    override fun div(other: Vector2Int): Vector2Int {
        return Vector2Int(x / other.x, y / other.y)
    }
    fun toFloat() : Vector2Float {
        return Vector2Float(x.toFloat(), y.toFloat())
    }
}

public class Vector2Float(val x: Float, val y: Float) : IVector<Vector2Float> {
    override fun plus(other: Vector2Float): Vector2Float {
        return Vector2Float(x + other.x, y + other.y)
    }
    override fun minus(other: Vector2Float): Vector2Float {
        return Vector2Float(x - other.x, y - other.y)
    }
    override fun times(other: Vector2Float): Vector2Float {
        return Vector2Float(x * other.x, y * other.y)
    }
    override fun div(other: Vector2Float): Vector2Float {
        return Vector2Float(x / other.x, y / other.y)
    }
    fun toInt() : Vector2Int {
        return Vector2Int(x.toInt(), y.toInt())
    }
}
