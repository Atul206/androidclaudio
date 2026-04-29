package com.androplaudio.sample.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CartManager {

    private val items = mutableMapOf<Int, Int>() // productId -> quantity

    private val _cartFlow = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val cartFlow: StateFlow<Map<Int, Int>> = _cartFlow.asStateFlow()

    fun addItem(productId: Int, quantity: Int = 1) {
        items[productId] = (items[productId] ?: 0) + quantity
        _cartFlow.value = items.toMap()
    }

    fun removeItem(productId: Int) {
        items.remove(productId)
        _cartFlow.value = items.toMap()
    }

    fun updateQuantity(productId: Int, quantity: Int) {
        if (quantity <= 0) removeItem(productId) else items[productId] = quantity
        _cartFlow.value = items.toMap()
    }

    fun getItems(): Map<Int, Int> = items.toMap()

    fun getItemCount(): Int = items.values.sum()

    fun clear() {
        items.clear()
        _cartFlow.value = emptyMap()
    }

    fun isEmpty(): Boolean = items.isEmpty()

    fun containsProduct(productId: Int): Boolean = items.containsKey(productId)
}
