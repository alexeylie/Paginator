package com.alexeyshmakov.paginator

import android.app.Application

class SampleApp : Application() {

    private var viewModel: ViewModel? = null

    override fun onCreate() {
        super.onCreate()
        viewModel = ViewModel.getModel()
    }
}