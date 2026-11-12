package cdev.wargame.entities

import cdev.wargame.core.Vector2

abstract class Entity(
    open val id: String,
    open var position: Vector2,
    open val radius: Float,
    open var ownerId: Int = 0
)
