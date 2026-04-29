package com.androplaudio.sample

import android.app.Application
import com.androplaudio.sample.data.ProductRepository
import com.androplaudio.sample.domain.CartManager
import com.androplaudio.sample.domain.OrderUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

val appModule = module {
    single { ProductRepository() }
    single { CartManager() }
    single { OrderUseCase(get(), get()) }
}

class SampleApp : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@SampleApp)
            modules(appModule)
        }

        if (BuildConfig.DEBUG) {
            com.androplaudio.core.AndroClaudio.initialize(this)
        }
    }
}
