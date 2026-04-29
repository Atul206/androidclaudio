package com.androplaudio.sample.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.androplaudio.sample.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeState()
    }

    private fun setupUI() {
        binding.btnRefresh.setOnClickListener { viewModel.refreshProducts() }
        binding.btnPlaceOrder.setOnClickListener { viewModel.placeOrder() }
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.products.collect { products ->
                val labels = products.map { p ->
                    "${p.name} — ${"%.2f".format(p.price)} ${if (p.inStock) "✓" else "✗"}"
                }
                val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_list_item_1, labels)
                binding.listProducts.adapter = adapter
                binding.listProducts.setOnItemClickListener { _, _, position, _ ->
                    viewModel.addToCart(products[position].id)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.cartCount.collect { count ->
                binding.tvCartCount.text = "Cart: $count items"
            }
        }

        lifecycleScope.launch {
            viewModel.statusMessage.collect { msg ->
                binding.tvStatus.text = msg
            }
        }
    }
}
