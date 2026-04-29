package com.androplaudio.sample.data

data class Product(val id: Int, val name: String, val price: Double, val inStock: Boolean)

class ProductRepository {

    private val products = mutableListOf(
        Product(1, "Kotlin Coroutines Book", 29.99, true),
        Product(2, "Android Architecture Guide", 34.99, true),
        Product(3, "Jetpack Compose in Action", 39.99, false),
    )

    fun getAllProducts(): List<Product> = products.toList()

    fun getProduct(id: Int): Product? = products.find { it.id == id }

    fun searchProducts(query: String): List<Product> =
        products.filter { it.name.contains(query, ignoreCase = true) }

    fun addProduct(name: String, price: Double): Product {
        val product = Product(id = products.size + 1, name = name, price = price, inStock = true)
        products.add(product)
        return product
    }

    fun updateStock(id: Int, inStock: Boolean): Boolean {
        val index = products.indexOfFirst { it.id == id }
        if (index == -1) return false
        products[index] = products[index].copy(inStock = inStock)
        return true
    }

    fun deleteProduct(id: Int): Boolean = products.removeIf { it.id == id }

    fun getProductCount(): Int = products.size

    fun getTotalInventoryValue(): Double =
        products.filter { it.inStock }.sumOf { it.price }
}
