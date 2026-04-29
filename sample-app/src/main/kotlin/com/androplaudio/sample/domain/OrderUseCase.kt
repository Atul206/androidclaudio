package com.androplaudio.sample.domain

import com.androplaudio.sample.data.ProductRepository

data class OrderResult(val orderId: String, val total: Double, val itemCount: Int, val success: Boolean)

class OrderUseCase(
    private val productRepository: ProductRepository,
    private val cartManager: CartManager,
) {

    fun placeOrder(customerName: String, paymentMethod: String): OrderResult {
        if (cartManager.isEmpty()) {
            return OrderResult(orderId = "", total = 0.0, itemCount = 0, success = false)
        }

        val items = cartManager.getItems()
        var total = 0.0
        var itemCount = 0

        for ((productId, qty) in items) {
            val product = productRepository.getProduct(productId) ?: continue
            if (!product.inStock) continue
            total += product.price * qty
            itemCount += qty
        }

        val orderId = "ORD-${System.currentTimeMillis()}"
        cartManager.clear()

        return OrderResult(orderId = orderId, total = total, itemCount = itemCount, success = true)
    }

    fun getOrderSummary(): String {
        val items = cartManager.getItems()
        if (items.isEmpty()) return "Cart is empty"

        val lines = items.entries.mapNotNull { (productId, qty) ->
            productRepository.getProduct(productId)?.let { p -> "${p.name} x$qty = ${"%.2f".format(p.price * qty)}" }
        }
        return lines.joinToString("\n")
    }

    fun applyDiscount(code: String): Double {
        return when (code.uppercase()) {
            "SAVE10" -> 0.10
            "SAVE20" -> 0.20
            "HALF" -> 0.50
            else -> 0.0
        }
    }
}
