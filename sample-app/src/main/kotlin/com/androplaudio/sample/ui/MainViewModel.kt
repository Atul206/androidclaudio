package com.androplaudio.sample.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androplaudio.sample.data.Product
import com.androplaudio.sample.data.ProductRepository
import com.androplaudio.sample.domain.CartManager
import com.androplaudio.sample.domain.OrderUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MainViewModel : ViewModel(), KoinComponent {

    private val productRepository: ProductRepository by inject()
    private val cartManager: CartManager by inject()
    private val orderUseCase: OrderUseCase by inject()

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products

    private val _cartCount = MutableStateFlow(0)
    val cartCount: StateFlow<Int> = _cartCount

    private val _statusMessage = MutableStateFlow("Ready")
    val statusMessage: StateFlow<String> = _statusMessage

    init {
        loadProducts()
        // Collect cart changes from any source — UI tap or MCP call
        viewModelScope.launch {
            cartManager.cartFlow.collect { cartItems ->
                _cartCount.value = cartItems.values.sum()
            }
        }
    }

    private fun loadProducts() {
        viewModelScope.launch {
            _products.value = productRepository.getAllProducts()
        }
    }

    fun addToCart(productId: Int) {
        cartManager.addItem(productId)
        _statusMessage.value = "Added to cart (${cartManager.getItemCount()} items)"
    }

    fun placeOrder() {
        val result = orderUseCase.placeOrder("Demo User", "card")
        if (result.success) {
            _statusMessage.value = "Order ${result.orderId} placed! Total: ${"%.2f".format(result.total)}"
            loadProducts()
        } else {
            _statusMessage.value = "Cart is empty"
        }
    }

    fun refreshProducts() {
        loadProducts()
        _statusMessage.value = "Products: ${productRepository.getProductCount()} | Inventory value: ${"%.2f".format(productRepository.getTotalInventoryValue())}"
    }
}
